/*SMARTFIRE: Satellite Mapping Automated Reanalysis Tool for Fire Incident REconciliation
Copyright (C) 2006-Present  USDA Forest Service AirFire Research Team and Sonoma Technology, Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package smartfire.func.fetch;

import com.sti.justice.gis.PolygonExtraction;
import com.sti.justice.util.StringUtil;
import com.sti.justice.util.ZipUtil;
import com.vividsolutions.jts.geom.MultiPolygon;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.func.Attribute;
import smartfire.func.UploadIngestMethod;
import smartfire.gis.GeometryBuilder;

/*
 * Burn severity thematic categories
 *   0 - outside fire perimeter
 *   1 - unburned to low severity
 *   2 - low severity
 *   3 - moderate severity
 *   4 - high severity
 *   5 - increased greenness
 *   6 - nodata/non-processing mask
 */

@MetaInfServices
public class MTBS_dNBR_UploadIngestMethod implements UploadIngestMethod {
    private static final Logger log = LoggerFactory.getLogger(smartfire.func.fetch.MTBS_dNBR_UploadIngestMethod.class);
    private static final double ACRES_PER_900_SQUARE_METERS = 0.222395;
    private static final String DEFAULT_BURN_SEVERITY_FILTER = "0,1,5,6"; // Make sure to update description text if changed.
    private static final String META_FIRE_ID_FIELD = "MTBS Fire ID:";
    private static final String META_FIRE_NAME_FIELD = "Fire Name (if known):";
    private static final String META_FIRE_DATE_FIELD = "Date of Fire:";
    private static final String META_FIRE_DATE_FORMAT = "MMMM dd, yyyy";
    private static final String META_THEMATIC_NBR_FIELD = "Thematic NBR;";
    private static final String META_THEMATIC_dNBR_FIELD = "Thematic dNBR;";
    private static final String META_FILE_SUFFIX = "_metadata.txt";
    private final Source source;
    private final GeometryBuilder geometryBuilder;
    private final String burnSeverityFilter;

    public MTBS_dNBR_UploadIngestMethod(Source source, GeometryBuilder geometryBuilder,
            @Attribute(name = "burnSeverityFilter",
            description = "Burn severity codes to ignore, separated by comma. Defaults to: \"0,1,5,6\" if left blank.") String burnSeverityFilter) {
        this.source = source;
        this.geometryBuilder = geometryBuilder;
        this.burnSeverityFilter = StringUtil.isEmpty(burnSeverityFilter) ? DEFAULT_BURN_SEVERITY_FILTER : burnSeverityFilter;
    }
    
    @Override
    public Collection<RawData> ingest(String filePath, DateTime dateTime) throws Exception {
        List<RawData> rdCollection = new ArrayList<RawData>();
        RawData result = new RawData();
        result.setSource(source);
        
        // Extract the tar files and store their paths in a list
        List<String> tarFiles = getTarGzipFiles(filePath);
        
        // Get metadata file
        File metaFile = getMetaFile(tarFiles);
        if(metaFile == null) {
            log.warn("Meta file not found in tar gzip file: {}; ignoring", filePath);
            return null;
        }
        
        // Get the abs path of extracted tar files
        String tarFilesPath = new File(tarFiles.get(0)).getParent(); // Any of the tar files will work
        log.debug("Tar contents extracted to temporary directory: {}", tarFilesPath);
        
        // Convert burn severity filter input to a list of values to ignore
        List<Integer> ignoreVals = parseBurnSeverityFilter();

        // Iterate over metadata file line by line and search for certain fields
        LineIterator metaFileIterator = FileUtils.lineIterator(metaFile, "UTF-8");
        String prevLine = null;
        boolean hasMTBSFireId = false;
        boolean hasFireName = false;
        boolean hasFireStartDate = false;
        boolean hasTiffFile = false;
        try {
            while (metaFileIterator.hasNext()) {
                String line = metaFileIterator.nextLine();
                // Find and add MTBS fire ID
                if(!hasMTBSFireId && line.startsWith(META_FIRE_ID_FIELD)) {
                    hasMTBSFireId = true;
                    String mtbsFireId = line.split(":")[1].trim();
                    result.put("MTBS_Fire_ID", mtbsFireId);
                }
                // Find and add fire name
                else if(!hasFireName && !StringUtil.isEmpty(source.getFireNameField()) && line.startsWith(META_FIRE_NAME_FIELD)) {
                    hasFireName = true;
                    String fireName = line.split(":")[1].trim();
                    result.put(source.getFireNameField(), fireName);
                }
                // Find and set the fire start date
                else if(!hasFireStartDate && line.startsWith(META_FIRE_DATE_FIELD)) {
                    hasFireStartDate = true;
                    String startDateStr = line.split(":")[1].trim();
                    DateTimeFormatter formatter = DateTimeFormat.forPattern(META_FIRE_DATE_FORMAT);
                    DateTime startDate = formatter.parseDateTime(startDateStr);
                    result.setStartDate(startDate);
                    result.setEndDate(startDate.plusDays(1).minusMillis(1));
                }
                // Find the raster geo tiff file and convert it into a MultiPolygon shape
                else if(!hasTiffFile && (line.startsWith(META_THEMATIC_dNBR_FIELD) || line.startsWith(META_THEMATIC_NBR_FIELD))) {
                    hasTiffFile = true;
                    String tiffName = prevLine;
                    File tiff = new File(tarFilesPath, tiffName);
                    if(!tiff.exists()) {
                        log.warn("Tiff file does not exist: {}; ignoring", tiff.getAbsolutePath());
                        return null;
                    }
                    PolygonExtraction polygonization = new PolygonExtraction(tiff, 0, true, ignoreVals);
                    MultiPolygon multiPoly = polygonization.getMultiPolygon(geometryBuilder.getCoordSysWKT());
                    result.setShape(multiPoly);
                    result.setArea(multiPoly.getArea());
                    for (Entry<Integer, Integer> vCount : polygonization.getValueCounts().entrySet()) {
                        String attName = "Burn_Severity_" + vCount.getKey().toString() + "_Acres";
                        String serverityAreaStr = Double.toString(ACRES_PER_900_SQUARE_METERS * vCount.getValue());
                        result.put(attName, serverityAreaStr);
                    }
                }
                // Stop reading metadata file if all required data has been found
                if(hasMTBSFireId && (hasFireName || StringUtil.isEmpty(source.getFireNameField())) && hasFireStartDate && hasTiffFile) {
                    break;
                }
                prevLine = line;
            }
        } finally {
            LineIterator.closeQuietly(metaFileIterator);
        }
        
        // Clean up the extracted tar contents.
        // NOTE: Extracted tar files will be abandond if ingest fails/returns before this point.
        //       Perhaps this is useful for debugging, but ultimatly we'd need to manually remove them.
        try {
            log.debug("Deleting extracted tar directory: {}", tarFilesPath);
            FileUtils.forceDelete(new File(tarFilesPath));
        } catch(IOException e) {
            log.warn(e.getMessage());
            log.warn("Directory was not deleted!");
        }
        
        rdCollection.add(result);
        return rdCollection;
    }
    
    private List<String> getTarGzipFiles(String tarPath) throws IOException {
        File tar = new File(tarPath);
        return ZipUtil.untarGzip(tar);
    }
    
    private File getMetaFile(List<String> filePaths) throws IOException {
        for(String filePath : filePaths) {
            if(filePath.endsWith(META_FILE_SUFFIX)) {
                return new File(filePath);
            }
        }
        return null;
    }
    
    private List<Integer> parseBurnSeverityFilter() {
        String[] ignoreValStrs = burnSeverityFilter.split(",");
        List<Integer> ignoreVals = new ArrayList<Integer>();
        for (String valStr : ignoreValStrs) {
            ignoreVals.add(Integer.parseInt(valStr));
        }
        return ignoreVals;
    }
}

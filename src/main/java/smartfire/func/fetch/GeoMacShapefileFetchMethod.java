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

import com.sti.justice.util.FileUtil;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.ScheduledFetch;
import smartfire.database.Source;
import smartfire.fileimport.ShapeFileParser;
import smartfire.func.FetchMethod;
import smartfire.gis.GeometryBuilder;

/**
 * FetchMethod for fetching GeoMac data in Shapefile format.
 */
@MetaInfServices(FetchMethod.class)
public class GeoMacShapefileFetchMethod extends AbstractFetchMethod {
    private static final Logger log = LoggerFactory.getLogger(GeoMacFetchMethod.class);
    private static final String FILE_SERVER = "rmgsc.cr.usgs.gov";
    private static final String FILE_PATH = "/outgoing/GeoMAC/current_year_fire_data/current_year_all_states/";
    private static final String FILENAME = "active_perimeters_dd83.zip";
    private final ScheduledFetch schedule;
    private final GeometryBuilder builder;

    public GeoMacShapefileFetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder) {
        this.schedule = scheduledFetch;
        this.builder = geometryBuilder;
    }

    @Override
    public Collection<RawData> fetch(Source source, DateTime dateTime) throws Exception {
        log.info("Parsing GeoMac Shapefile");
        
        URL url = new URL("http://" + FILE_SERVER + FILE_PATH + FILENAME);
        File tDir = FileUtil.createTempDir();
        File inputFile = new File(tDir, "tmp.zip");
        inputFile.deleteOnExit();
        FileUtils.copyURLToFile(url, inputFile);
        
        ShapeFileParser shapeFile = new ShapeFileParser(inputFile.getAbsolutePath(), builder.getCoordSysWKT());
        
        return new FetchResults(dateTime, shapeFile.getFieldNames(), shapeFile.getData());
    }

    @Override
    public Iterator<RawData> getFetchResultsIterator(DateTime fetchDate, String[] fieldNames,
            Iterator<Object[]> iter) {
        return new GeoMacFetchResultsIterator(fetchDate, fieldNames, iter);
    }

    private class GeoMacFetchResultsIterator extends AbstractFetchResultsIterator {
        public GeoMacFetchResultsIterator(DateTime fetchDate, String[] fieldNames, Iterator<Object[]> iter) {
            super(fetchDate, fieldNames, iter);
        }

        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());

            String isActive = ((String) row.get("ACTIVE")).toUpperCase();
            if (!isActive.equals("Y")) {
                log.warn("Fire is not active; Skipping.");
                return null;
            }
            
            com.vividsolutions.jts.geom.Geometry shape = (com.vividsolutions.jts.geom.Geometry) row.get("the_geom");
            if (shape.isEmpty()) {
                log.warn("No geometry data found; Skipping.");
                return null;
            }
            row.remove("the_geom");

            // Get area
            double area = shape.getArea();

            // Set result
            result.setShape(shape);
            result.setArea(area);
            result.setSource(schedule.getSource());

            // Set date time
            DateTime startDate = new DateTime(row.get("DATE_")).toDateMidnight().toDateTime();
            result.setStartDate(startDate);
            
            DateTime endDate = startDate.plusDays(1).minusMillis(1);
            result.setEndDate(endDate);

            for(String key : row.keySet()) {
                result.put(key, row.get(key).toString());
            }

            return result;
        }
    }
}

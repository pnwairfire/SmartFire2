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
package smartfire.export;

import com.google.common.collect.Lists;
import com.sti.justice.util.ZipUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.CRS;
import org.kohsuke.MetaInfServices;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import smartfire.ApplicationSettings;
import smartfire.SmartfireException;

@MetaInfServices(ExportMethod.class)
public class ShapefileTrimmedExportMethod extends ShapefileExportMethod implements ExportMethod {
    public ShapefileTrimmedExportMethod() {
        super("SHP Trimmed", "shp-trimmed");
    }

    @Override
    protected <T extends Exportable> File createShapefile(
            ApplicationSettings settings, String baseFileName, Iterable<T> records) {
        List<T> recordList = Lists.newArrayList(records);
        if(recordList.isEmpty()) {
            return null;
        }
        File folder = getTempFolder();
        try {
            String wkt = settings.getConfig().getCoordSysWKT();
            CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
            Map<String, Class<?>> memberMap = Exports.getMemberMapSpecialFields(recordList);
            SimpleFeatureType featureType = buildFeatureType(crs, recordList, memberMap);
            FeatureCollection<SimpleFeatureType, SimpleFeature> features = buildFeatures(settings.getGeometryBuilder(), featureType, recordList, memberMap);
            createShapefile(folder, baseFileName, featureType, features);
            // NOTE: We re-create the .prj file because GeoTools does it in a way
            // that the ESRI tools don't like.  This may not be an issue with
            // ArcGIS 10 but it seemed better to be backwards-compatible.
            File prjFile = new File(folder, baseFileName + ".prj");
            prjFile.delete();
            FileUtils.writeStringToFile(prjFile, wkt);
            return ZipUtil.zipUpFolder(folder);
        } catch(Exception e) {
            log.error("Unable to create shapefile.", e);
            throw new SmartfireException("Unable to create shapefile", e);
        } finally {
            try {
                log.debug("Deleting temporary folder {}", folder);
                FileUtils.deleteDirectory(folder);
            } catch(IOException e) {
                log.warn("Unable to delete temporary folder", e);
            }
        }
    }
}

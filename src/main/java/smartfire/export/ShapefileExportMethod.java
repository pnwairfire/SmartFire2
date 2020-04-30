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
import com.vividsolutions.jts.geom.Geometry;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.StaplerRequest;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import smartfire.ApplicationSettings;
import smartfire.SmartfireException;
import smartfire.gis.GeometryBuilder;


/**
 * ExportMethod for exporting Shapefiles.
 */
@MetaInfServices(ExportMethod.class)
public class ShapefileExportMethod extends AbstractExportMethod<Exportable> implements ExportMethod {

    public ShapefileExportMethod() {
        super("SHP", "shp", "/images/icons/shapefile-32x32.png", Exportable.class, "application/zip", ".zip");
    }
    
    public ShapefileExportMethod(String displayName, String slugName) {
        super(displayName, slugName, "/images/icons/shapefile-32x32.png", Exportable.class, "application/zip", ".zip");
    }

    @Override
    protected void performExport(
            StaplerRequest request,
            OutputStream out,
            ApplicationSettings appSettings,
            String exportFileName,
            List<Exportable> entities,
            DateTime startDate,
            DateTime endDate) throws IOException {

        File shapeFile = null;
        InputStream in = null;
        try {
            shapeFile = createShapefile(appSettings, exportFileName, entities);
            if (shapeFile != null) {
                in = new FileInputStream(shapeFile);
                IOUtils.copy(in, out);
            }
        } finally {
            if(in != null) {
                in.close();
            }
            if(out != null) {
                out.close();
            }

            // Cleanup shape file
            if(shapeFile != null) {
                shapeFile.delete();
            }
        }
    }

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
            Map<String, Class<?>> memberMap = Exports.getMemberMapAllFields(records);
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

    protected void createShapefile(File folder, String baseFileName, SimpleFeatureType featureType,
            FeatureCollection<SimpleFeatureType, SimpleFeature> features) throws Exception {

        File shapeFile = new File(folder, baseFileName + ".shp");

        // Create the ShapefileDataStore
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", shapeFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);
        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(featureType);

        // Write the features to the shapefile
        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = newDataStore.getFeatureSource(typeName);

        if(!(featureSource instanceof FeatureStore<?, ?>)) {
            throw new SmartfireException("File does not support read/write access");
        }

        FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) featureSource;

        featureStore.setTransaction(transaction);
        try {
            featureStore.addFeatures(features);
            transaction.commit();
        } catch(Exception e) {
            log.error("Problem creating shape file.", e);
            throw new SmartfireException("Problem creating file.", e);
        } finally {
            transaction.close();
            newDataStore.dispose();
        }
    }

    protected <T extends Exportable> FeatureCollection<SimpleFeatureType, SimpleFeature> buildFeatures(
            GeometryBuilder geometryBuilder, SimpleFeatureType featureType, List<T> records, Map<String, Class<?>> memberMap) {

        // Construct the FeatureCollection that will hold all the records
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = FeatureCollections.newCollection();
        DefaultFeatureCollection result = new DefaultFeatureCollection(fc);

        // Construct a builder for the FeatureType
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        List<String> attributeNames = Lists.newArrayList(memberMap.keySet());
        List<ExportRow> rows = Exports.getExportRows(geometryBuilder, records, attributeNames);

        // Create Feature objects
        for(ExportRow row : rows) {
            // Set the geometry field
            builder.set("shape", row.getExportShape());

            // Set other fields from the export member map
            for(String key : row.getHeadings()) {
                if(!"shape".equals(key)) {
                    builder.set(key, row.getExportMember(key));
                }
            }

            // Build the feature
            SimpleFeature feature = builder.buildFeature(null);

            // And add it to the result collection
            result.add(feature);
        }
        return result;
    }

    protected <T extends Exportable> SimpleFeatureType buildFeatureType(
            CoordinateReferenceSystem crs, List<T> records, Map<String, Class<?>> memberMap) throws Exception {

        if(records.isEmpty()) {
            throw new SmartfireException("Unable to export an empty list");
        }

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setCRS(crs);

        // Set the name from the class name
        T obj = records.get(0);
        builder.setName(obj.getClass().getSimpleName());

        // Get geometry class and verify it is a geometry.
        Class<?> geomClass = records.get(0).getShape().getClass();
        if(!Geometry.class.isAssignableFrom(geomClass)) {
            throw new SmartfireException("Unexpected shape class: " + geomClass.getName());
        }

        // Add geometry column
        builder.add("shape", geomClass);

        // Add exportable fields
        for(Map.Entry<String, Class<?>> entry : memberMap.entrySet()) {
            String key = entry.getKey();
            if(!"shape".equals(key)) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }

        return builder.buildFeatureType();
    }
}

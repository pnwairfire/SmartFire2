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
package smartfire.fileimport;

import com.google.common.collect.Lists;
import com.sti.justice.util.ZipUtil;
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.SmartfireException;
import smartfire.gis.CoordinateTransformer;

public class ShapeFileParser {
    private final Logger log = LoggerFactory.getLogger(ShapeFileParser.class);
    private final List<String> filePaths;
    private final CoordinateReferenceSystem destCRS;
    private final CoordinateReferenceSystem defaultSrcCRS; // Used if we are unable to parse CRS from shapefile
    private String[] fieldNames;
    private List<Object[]> data = Lists.newArrayList();

    public ShapeFileParser(String shapeFilePath, String destWkt) throws Exception {
        this(shapeFilePath, destWkt, null);
    }
    
    public ShapeFileParser(String shapeFilePath, String destWkt, String defaultSrcWkt) throws Exception {
        this.destCRS = CoordinateTransformer.getCRS(destWkt);
        this.filePaths = extractFiles(shapeFilePath);
        this.defaultSrcCRS = defaultSrcWkt != null ? CoordinateTransformer.getCRS(defaultSrcWkt) : null;
        parse();
    }
    
    private List<String> extractFiles(String filePath) throws Exception {
        // We might consider subclassing ShapeFileParser (such as ZipShapeFileParser for example) if we begin supporting file types that require additional logic. -AMC
        if (filePath.contains(".zip")) {
            return ZipUtil.unzip(new File(filePath));
        }
        if (filePath.contains(".tar.gz")) {
            return ZipUtil.untarGzip(new File(filePath));
        }
        throw new SmartfireException("File type unsupported: " + filePath);
    }

    private void parse() throws Exception {
        File file = getShapeFileURL();
        try {
            FileDataStore dataStore = FileDataStoreFinder.getDataStore(file);

            // we are now connected
            String[] typeNames = dataStore.getTypeNames();
            String typeName = typeNames[0];

            log.info("Reading shape file content: " + typeName);

            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
            FeatureIterator<SimpleFeature> iterator;

            List<String> fieldNameList = Lists.newArrayList();
            boolean fieldNamesFound = false;

            featureSource = dataStore.getFeatureSource(typeName);
            collection = featureSource.getFeatures();
            iterator = collection.features();
            try {
                while(iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();

                    // Setup Geometry Transformer
                    CoordinateReferenceSystem sourceCRS = feature.getFeatureType().getCoordinateReferenceSystem();
                    // If the source CRS was unable to be parsed, use the default CRS if we were given one
                    if (sourceCRS == null && defaultSrcCRS != null) {
                        sourceCRS = defaultSrcCRS;
                    }
                    CoordinateTransformer transformer = new CoordinateTransformer(sourceCRS, destCRS);

                    List<Object> featureData = Lists.newArrayList();
                    Collection<Property> properties = feature.getProperties();
                    for(Property p : properties) {

                        // Save field names
                        Name name = p.getName();
                        if(!fieldNamesFound) {
                            fieldNameList.add(name.toString());
                        }

                        // Get Feature attributes
                        Class<?> klass = p.getType().getBinding();
                        Object attribute = klass.cast(feature.getAttribute(name));

                        // Transform found geometery
                        if(attribute instanceof Geometry) {
                            attribute = transformer.transform((Geometry) attribute);
                        }

                        featureData.add(attribute);
                    }
                    data.add(featureData.toArray());

                    fieldNamesFound = true;
                }
            } finally {
                if(iterator != null) {
                    iterator.close();
                }
            }

            // Create field name list
            fieldNames = fieldNameList.toArray(new String[fieldNameList.size()]);

            deleteShapeFile();

        } catch(Exception e) {
            throw new SmartfireException("Problem parsing shape file.", e);
        }
    }

    public List<Object[]> getData() {
        return data;
    }

    public String[] getFieldNames() {
        return fieldNames;
    }

    private File getShapeFileURL() throws MalformedURLException {
        for(String path : filePaths) {
            if(path.contains(".shp")) {
                File file = new File(path);
                return file;
            }
        }
        throw new SmartfireException("Invalid shape file. Does not contain a .shp file.");
    }

    private void deleteShapeFile() {
        File f = new File(filePaths.get(0));
        File dir = f.getParentFile();
        deleteDir(dir);
    }

    public static boolean deleteDir(File dir) {
        if(dir.isDirectory()) {
            String[] children = dir.list();
            for(int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if(!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
}

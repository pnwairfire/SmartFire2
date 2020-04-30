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
package smartfire.util;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.SmartfireException;
import smartfire.gis.CoordinateTransformer;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.ShapeAttributes;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ShapefileUtil {
    private static final Logger log = LoggerFactory.getLogger(ShapefileUtil.class);

    private static Map<String, String> readFeatureAttributes(SimpleFeature feature) {
        SimpleFeatureType featureType = feature.getFeatureType();
        Map<String, String> result = Maps.newLinkedHashMap();
        int numAttrs = feature.getAttributeCount();
        for(int i = 0; i < numAttrs; i++) {
            AttributeDescriptor desc = featureType.getDescriptor(i);
            if(desc instanceof GeometryDescriptor) {
                continue;
            }
            String name = desc.getLocalName();
            Object obj = feature.getAttribute(i);
            String value = Functions.formatGeneral(obj);
            result.put(name, value);
        }
        return result;
    }

    public static SimpleFeatureSource openShapefile(String path) {
        FileDataStore dataStore = null;
        try {
            dataStore = FileDataStoreFinder.getDataStore(new File(path));
            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            return source;
        } catch(IOException ex) {
            if(dataStore != null) {
                dataStore.dispose();
            }
            throw new IllegalArgumentException("Unable to read shapefile from path \"" + path + "\"", ex);
        }
    }

    public static CoordinateTransformer makeCoordinateTransformer(GeometryBuilder geometryBuilder, CoordinateReferenceSystem sourceCRS) {
        String wkt = geometryBuilder.getCoordSysWKT();
        CoordinateReferenceSystem destCRS = CoordinateTransformer.getCRS(wkt);
        return new CoordinateTransformer(sourceCRS, destCRS);
    }

    public static ShapeAttributes readShapeFile(GeometryBuilder geometryBuilder, Geometry geom, String shapefileLocation) {
        SimpleFeatureSource featureSource = openShapefile(shapefileLocation);
//        log.debug("ShapeFile Opened Successfully.");
        try {
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
            SimpleFeatureType schema = featureSource.getSchema();
            String typeName = schema.getTypeName();
            String shapeColumnName = schema.getGeometryDescriptor().getLocalName();

            CoordinateReferenceSystem geomCRS = CoordinateTransformer.getCRS(geometryBuilder.getCoordSysWKT());
            CoordinateReferenceSystem fileCRS = schema.getCoordinateReferenceSystem();
            CoordinateTransformer xform = new CoordinateTransformer(geomCRS, fileCRS);

            Geometry projectedGeom = xform.transform(geom);
            double geomArea = projectedGeom.getArea();

            Expression shapeColumn = ff.property(shapeColumnName);
            Expression queryShape = ff.literal(projectedGeom);      
            Expression queryEnvelope = ff.literal(projectedGeom.getEnvelope());
            
            Filter envelopefilter = ff.intersects(shapeColumn, queryEnvelope);
            Filter shapefilter = ff.intersects(shapeColumn, queryShape);

            Query envelopeQuery = new Query(typeName, envelopefilter, Query.ALL_PROPERTIES);

            ShapeAttributes result = null;
//            log.debug("Querying features from ShapeFile.");
            SimpleFeatureCollection envelopeFeatures = featureSource.getFeatures(envelopeQuery);
            SimpleFeatureCollection features = envelopeFeatures.subCollection(shapefilter);
            SimpleFeatureIterator iter = features.features();
            try {
                while(iter.hasNext()) {
//                    log.debug("Reading feature from ShapeFile.");
                    SimpleFeature feature = iter.next();
                    Map<String, String> attrs = readFeatureAttributes(feature);

                    Geometry featureShape = (Geometry) feature.getDefaultGeometry();
                    Geometry intersection = projectedGeom.intersection(featureShape);
                    double intersectionArea = intersection.getArea();
                    double representativeFraction = intersectionArea / geomArea;

                    if(result == null || representativeFraction > result.getRepresentativeFraction()) {
                        result = new ShapeAttributes(attrs, geom, representativeFraction);
                    }
                }
            } finally {
                iter.close();
            }

            if(result == null) {
//                log.debug("No intersection found in the ShapeFile.");
                return ShapeAttributes.emptyIntersection(geom);
            }

            return result;
        } catch(IOException ex) {
            throw new SmartfireException("Error reading features from SummaryDataLayer", ex);
        } finally {
            featureSource.getDataStore().dispose();
        }
    }
}

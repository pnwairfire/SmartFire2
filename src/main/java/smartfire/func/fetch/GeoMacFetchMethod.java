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

import com.google.common.collect.Lists;
import de.micromata.opengis.kml.v_2_2_0.*;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.ScheduledFetch;
import smartfire.database.Source;
import smartfire.func.FetchMethod;
import smartfire.gis.CoordinateTransformer;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.Union;
import smartfire.gis.XYPoint;

/**
 * FetchMethod for fetching GeoMac data in KML format.
 */
@MetaInfServices(FetchMethod.class)
public class GeoMacFetchMethod extends AbstractFetchMethod {
    private static final Logger log = LoggerFactory.getLogger(GeoMacFetchMethod.class);
    private static final String FILE_SERVER = "rmgsc.cr.usgs.gov";
    private static final String FILE_PATH = "/outgoing/GeoMAC/";
    private static final String FILENAME = "ActiveFirePerimeters.kml";
    private final ScheduledFetch schedule;
    private final GeometryBuilder builder;

    public GeoMacFetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder) {
        this.schedule = scheduledFetch;
        this.builder = geometryBuilder;
    }

    @Override
    public Collection<RawData> fetch(Source source, DateTime dateTime) throws Exception {
        log.info("Fetching GeoMac KML data file via HTTP");
        URL url = new URL("http://" + FILE_SERVER + FILE_PATH + FILENAME);
        InputStream inputStream = url.openStream();

        log.info("Parsing GeoMac KML file");
        final Kml kml = Kml.unmarshal(inputStream);
        final Document document = (Document) kml.getFeature();
        final List<Feature> features = document.getFeature();

        // Get a list of all placemarks
        List<Placemark> placemarks = Lists.newArrayList();
        for(Feature feature : features) {
            if(feature instanceof Placemark) {
                placemarks.add((Placemark) feature);
            }
        }

        // Find placemarks that are only polygons or multigeometry
        // Convert them to native geometry types
        List<Object[]> geometries = Lists.newArrayList();
        for(Placemark placemark : placemarks) {
            Geometry geom = placemark.getGeometry();

            // Convert geometries to native polygons
            List<com.vividsolutions.jts.geom.Geometry> polygons = createNativePolygons(geom);

            // create native multipolygon
            com.vividsolutions.jts.geom.Geometry multiPolygon = null;
            if(!polygons.isEmpty()) {
                multiPolygon = Union.unionAll(polygons);
            }

            if(multiPolygon != null) {
                com.vividsolutions.jts.geom.Geometry[] geomArray = { multiPolygon };
                geometries.add(geomArray);
            }
        }

        String[] fields = { "shape" };
        return new FetchResults(dateTime, fields, geometries);
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

            // Get Shape
            com.vividsolutions.jts.geom.Geometry shape = (com.vividsolutions.jts.geom.Geometry) row.get("shape");
            if (shape.isEmpty()) {
                log.warn("No geometry data found; Skipping.");
                return null;
            }
            row.remove("shape");

            // Get area
            double area = shape.getArea();

            // Set result
            result.setShape(shape);
            result.setArea(area);
            result.setSource(schedule.getSource());

            // Set date time
            DateTime startDate = expectedDateTime;
            DateTime endDate = expectedDateTime.plusDays(1).minusMillis(1);

            result.setStartDate(startDate);
            result.setEndDate(endDate);

            for(String key : row.keySet()) {
                result.put(key, row.get(key).toString());
            }

            return result;
        }
    }

    private List<com.vividsolutions.jts.geom.Geometry> createNativePolygons(Geometry geom) {
        if(geom instanceof MultiGeometry) {
            MultiGeometry multiGeom = (MultiGeometry) geom;
            // Collect all polygons from the multipolygon and convert to native polygons
            List<com.vividsolutions.jts.geom.Geometry> polygons = Lists.newArrayList();
            for(Geometry geomPiece : multiGeom.getGeometry()) {
                if(geomPiece instanceof Polygon) {
                    List<com.vividsolutions.jts.geom.Geometry> newPolygons = createNativePolygon((Polygon) geomPiece);
                    for(com.vividsolutions.jts.geom.Geometry poly : newPolygons) {
                        polygons.add(poly);
                    }
                }
            }
            return polygons;
        } else if(geom instanceof Polygon) {
            Polygon polygon = (Polygon) geom;
            return createNativePolygon((Polygon) geom);
        }
        return Lists.newArrayList();
    }

    private com.vividsolutions.jts.geom.Geometry fixInvalidPolygon(com.vividsolutions.jts.geom.Geometry geom) {
        log.info("Found invalid polygon, buffering to fix it: {}", geom.toString());
        com.vividsolutions.jts.geom.Geometry newGeom = geom.buffer(0.0);
        if(newGeom instanceof com.vividsolutions.jts.geom.Polygon) {
            com.vividsolutions.jts.geom.Polygon poly = (com.vividsolutions.jts.geom.Polygon) newGeom;
            if(!poly.isValid()) {
                log.info("Found invalid polygon, failed to fix it: {}", geom.toString());
                return null;
            }
            return poly;
        } else if(newGeom instanceof com.vividsolutions.jts.geom.MultiPolygon) {
            com.vividsolutions.jts.geom.MultiPolygon multiPoly = (com.vividsolutions.jts.geom.MultiPolygon) newGeom;
            if(!multiPoly.isValid()) {
                log.info("Found invalid polygon, failed to fix it: {}", geom.toString());
                return null;
            }
            return multiPoly;
        }
        return null;
    }

    private List<com.vividsolutions.jts.geom.Geometry> createNativePolygon(Polygon polygon) {
        // Build exterior boundaries
        List<XYPoint> exteriorRing = transformPoints(polygon.getOuterBoundaryIs().getLinearRing().getCoordinates());

        // Build interior boundaries
        List<Boundary> interiorBoundaries = polygon.getInnerBoundaryIs();
        List<List<XYPoint>> interiorRings = Lists.newArrayList();
        for(Boundary boundary : interiorBoundaries) {
            interiorRings.add(transformPoints(boundary.getLinearRing().getCoordinates()));
        }

        // Build and fix polygons
        double scale = 10000.0; // Round to the nearest thousandth 
        com.vividsolutions.jts.geom.Geometry poly = builder.buildScaledPolygon(exteriorRing, interiorRings, scale);
        if(!poly.isValid()) {
            poly = fixInvalidPolygon(poly);
        }

        // Return list of polygons
        List<com.vividsolutions.jts.geom.Geometry> newPolygons = Lists.newArrayList();
        if(poly != null) {
            newPolygons.add(poly);
        }
        return newPolygons;
    }

    private List<XYPoint> transformPoints(List<Coordinate> coordinates) {
        CoordinateTransformer transformer = builder.newLonLatInputTransformer();
        List<XYPoint> points = Lists.newArrayList();
        for(Coordinate coord : coordinates) {
            points.add(transformer.transform(coord.getLongitude(), coord.getLatitude()));
        }
        return points;
    }
}

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
package smartfire.gis;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.PrecisionModel;
import java.util.List;
import smartfire.Config;

/**
 * Methods for constructing and manipulating geometric entities using
 * SMARTFIRE's native coordinate system.
 */
public class GeometryBuilder {
    public static final String WGS84 = "EPSG:4326";
    private final Config config;

    public GeometryBuilder(Config config) {
        this.config = config;
    }

    /**
     * Get Well-Known Text of the coordinate system currently in use.
     *
     * @return a string in WKT format
     */
    public String getCoordSysWKT() {
        return config.getCoordSysWKT();
    }

    /**
     * Builds a Point Geometry from x and y coordinates
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return a Geometry Point of the x and y coordinates.
     */
    public Point buildPoint(double x, double y) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate coord = new Coordinate(x, y);
        Point point = geometryFactory.createPoint(coord);
        return point;
    }

    /**
     * Transforms longitude and latitude coordinates and
     * builds a Point Geometry from them.
     *
     * @param lon longitude coordinate
     * @param lat latitude coordinate
     * @return a Geometry Point from the transformed longitude and latitude coordinates.
     */
    public Point buildPointFromLatLon(double lon, double lat) {
        CoordinateTransformer transformer = newLonLatInputTransformer();
        XYPoint coord = transformer.transform(lon, lat);
        return this.buildPoint(coord.getX(), coord.getY());
    }

    /**
     * Utility function to construct a rectangular Polygon object from an
     * Envelope.
     *
     * @param env the Envelope to convert
     * @return a rectangular Polygon
     */
    public Polygon envelopeToGeometry(Envelope env) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coords = new Coordinate[5];
        coords[0] = new Coordinate(env.getMinX(), env.getMinY());
        coords[1] = new Coordinate(env.getMinX(), env.getMaxY());
        coords[2] = new Coordinate(env.getMaxX(), env.getMaxY());
        coords[3] = new Coordinate(env.getMaxX(), env.getMinY());
        coords[4] = coords[0];
        LinearRing linearRing = geometryFactory.createLinearRing(coords);
        return geometryFactory.createPolygon(linearRing, null);
    }

    /**
     * Utility function to construct a Polygon from a linear ring of points
     * 
     * @param exteriorRing a list of Points that make the exterior ring of a polygon
     * @param interiorRings a list of lists containing interior rings of this polygon
     * @return a Polygon
     */
    public Polygon buildPolygon(List<XYPoint> exteriorRing, List<List<XYPoint>> interiorRings) {
        GeometryFactory geometryFactory = new GeometryFactory();

        // Create exterior linear ring
        LinearRing exteriorLinearRing = buildLinearRing(exteriorRing);
        
        // Create interior linear ring
        LinearRing[] interiorLinearRings = new LinearRing[interiorRings.size()];
        for(int i = 0; i < interiorRings.size(); i++) {
            interiorLinearRings[i] = buildLinearRing(interiorRings.get(i));
        }  

        return geometryFactory.createPolygon(exteriorLinearRing, interiorLinearRings);
    }

    /**
     * Utility function to construct a Linear Ring from a list of points
     * 
     * @param ring list of points that make up a linear ring.
     * @return a LinearRing
     */
    public LinearRing buildLinearRing(List<XYPoint> ring) {
        GeometryFactory geometryFactory = new GeometryFactory();
        
        Coordinate[] coords = new Coordinate[ring.size()];
        for(int i = 0; i < ring.size(); i++) {
            coords[i] = new Coordinate(ring.get(i).x, ring.get(i).y);
        }
        return geometryFactory.createLinearRing(coords);
    }

    /**
     * Construct a new CoordinateTransformer capable of transforming from
     * (longitude, latitude) points in WGS 84 to (x, y) points in SMARTFIRE's
     * coordinate system.
     *
     * @return a new CoordinateTransformer instance
     */
    public CoordinateTransformer newLonLatInputTransformer() {
        return new CoordinateTransformer(WGS84, this.config.getCoordSysWKT());
    }

    /**
     * Transforms point coordinates into a (longitude, latitude) point.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return an XYPoint where X is longitude and Y is latitude
     */
    public XYPoint buildLatLonFromPoint(double x, double y) {
        CoordinateTransformer transformer = newLonLatOutputTransformer();
        return transformer.transform(x, y);
    }

    /**
     * Construct a new CoordinateTransformer capable of transforming from
     * (x, y) points in SMARTFIRE's coordinate system to (longitude, latitude)
     * points in WGS 84.
     *
     * @return a new CoordinateTransformer instance
     */
    public CoordinateTransformer newLonLatOutputTransformer() {
        return new CoordinateTransformer(this.config.getCoordSysWKT(), WGS84);
    }
    
    
    public Polygon buildScaledPolygon(List<XYPoint> exteriorRing, List<List<XYPoint>> interiorRings, double scale) {
        
        PrecisionModel precision = new PrecisionModel(scale);
        
        GeometryFactory geometryFactory = new GeometryFactory(precision);

        // Create exterior linear ring
        LinearRing exteriorLinearRing = buildLinearRing(exteriorRing);
        
        // Create interior linear ring
        LinearRing[] interiorLinearRings = new LinearRing[interiorRings.size()];
        for(int i = 0; i < interiorRings.size(); i++) {
            interiorLinearRings[i] = buildLinearRing(interiorRings.get(i));
        }  

        return geometryFactory.createPolygon(exteriorLinearRing, interiorLinearRings);
    }
    
    
    
}

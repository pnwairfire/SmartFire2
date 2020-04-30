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

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import smartfire.SmartfireException;

public class CoordinateTransformer {
    static {
        // Force (x, y) -> (longitude, latitude) order, even when EPSG thinks
        // it should be (latitude, longitude).
        // See: http://docs.codehaus.org/display/GEOTDOC/The+axis+order+issue
        System.setProperty("org.geotools.referencing.forceXY", "true");
    }
    private static final String GOOGLE_MERCATOR_WKT = "PROJCS[\"WGS84 / Google Mercator\", GEOGCS[\"WGS 84\", DATUM[\"World Geodetic System 1984\", SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], AUTHORITY[\"EPSG\",\"6326\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AUTHORITY[\"EPSG\",\"4326\"]], PROJECTION[\"Mercator (1SP)\", AUTHORITY[\"EPSG\",\"9804\"]], PARAMETER[\"semi_major\", 6378137.0], PARAMETER[\"semi_minor\", 6378137.0], PARAMETER[\"latitude_of_origin\", 0.0], PARAMETER[\"central_meridian\", 0.0], PARAMETER[\"scale_factor\", 1.0], PARAMETER[\"false_easting\", 0.0], PARAMETER[\"false_northing\", 0.0], UNIT[\"m\", 1.0], AUTHORITY[\"EPSG\",\"900913\"]]";
    private final CoordinateReferenceSystem sourceCRS;
    private final CoordinateReferenceSystem destCRS;
    private final MathTransform transformer;

    public CoordinateTransformer(String sourceWkt, String destWkt) {
        this(getCRS(sourceWkt), getCRS(destWkt));
    }

    public CoordinateTransformer(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem destCRS) {
        this.sourceCRS = sourceCRS;
        this.destCRS = destCRS;

        try {
            this.transformer = CRS.findMathTransform(sourceCRS, destCRS, true);  // "lenient"
        } catch(FactoryException e) {
            throw new SmartfireException("Unable to create a MathTransform.", e);
        }
    }

    /**
     * Transform x, y into the coordinate system defined in destCRS
     *
     * @param x the x coordinate to be transformed
     * @param y the y coordinate to be transformed
     * @return a 2D Point that has been transformed into the defined coordinate.
     */
    public XYPoint transform(double x, double y) {
        double[] coord = buildCoordinate(x, y);
        try {
            this.transformer.transform(coord, 0, coord, 0, 1);
        } catch(TransformException e) {
            throw new SmartfireException("Error transforming from" + this.sourceCRS.getName() + " into coordinate system " + this.destCRS.getName(), e);
        }
        return new XYPoint(coord[0], coord[1]);
    }

    /**
     * Transform x, y into the coordinate system defined in destCRS
     *
     * @param point the x,y coordinates to be transformed
     * @return a 2D Point that has been transformed into the defined coordinate.
     */
    public XYPoint transform(XYPoint point) {
        return transform(point.getX(), point.getY());
    }

    /**
     * Transform a Geometry into the coordinate system defined in destCRS.
     * 
     * @param geom the geometry to transform
     * @return a Geometry representing the transformed shape
     */
    public Geometry transform(Geometry geom) {
        try {
            return JTS.transform(geom, transformer);
        } catch(TransformException e) {
            throw new SmartfireException("Error transforming from" + this.sourceCRS.getName() + " into coordinate system " + this.destCRS.getName(), e);
        }
    }

    /**
     * Adds Google Mercator as a possible coordinate system to transform with.
     *
     * @param srs SRS code to for coordinate system
     * @return the correct SRS code
     */
    private static String replaceUnsupportedSRS(String srs) {
        if("EPSG:900913".equals(srs)) {
            return GOOGLE_MERCATOR_WKT;
        }
        return srs;
    }

    /**
     * Get Coordinate Reference System from WKT
     *
     * @param wkt Well known Text for coordinate system.
     * @return the correct SRS code
     */
    public static CoordinateReferenceSystem getCRS(String wkt) {
        wkt = replaceUnsupportedSRS(wkt);

        try {
            if(wkt.startsWith("EPSG:")) {
                return CRS.decode(wkt);
            } else {
                return CRS.parseWKT(wkt);
            }
        } catch(FactoryException e) {
            throw new SmartfireException("Unable to find source coordinate system from the specifier: " + wkt, e);
        }
    }

    /**
     * Builds coordinate array for transform
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return and array of containing the coordinate values.
     */
    private static double[] buildCoordinate(double x, double y) {
        double[] xyz = new double[3];
        xyz[0] = x;
        xyz[1] = y;
        xyz[2] = 0;
        return xyz;
    }
}

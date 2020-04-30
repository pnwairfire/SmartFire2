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
package smartfire.layer;

import smartfire.database.SummaryDataLayer;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class ShapefileLayerReadingMethodTest extends ShapefileTest {
    public ShapefileLayerReadingMethodTest(String testName) {
        super(testName);
    }

    public void testReadAttributes() {
        // Petaluma, CA
        final double latitude = 38.25;
        final double longitude = -122.65;
        
        Point point = geometryBuilder.buildPointFromLatLon(longitude, latitude);
        Geometry shape = point.buffer(1000);

        SummaryDataLayer layer = new SummaryDataLayer();
        layer.setDataLocation(shapefilePath);

        LayerReadingMethod method = new ShapefileLayerReadingMethod();
        LayerAttributes attr = method.readAttributes(geometryBuilder, layer, shape);

        assertEquals(1.0, attr.getRepresentativeFraction());
        assertEquals(layer, attr.getSummaryDataLayer());
        assertEquals(shape, attr.getShape());
        assertEquals("California", attr.get("STATE_NAME"));
    }

    public void testEmptyIntersection() {
        // Somewhere over the Pacific Ocean
        final double latitude = 37;
        final double longitude = -123;

        Point point = geometryBuilder.buildPointFromLatLon(longitude, latitude);
        Geometry shape = point.buffer(1000);

        SummaryDataLayer layer = new SummaryDataLayer();
        layer.setDataLocation(shapefilePath);

        LayerReadingMethod method = new ShapefileLayerReadingMethod();
        LayerAttributes attr = method.readAttributes(geometryBuilder, layer, shape);

        assertNotNull(attr);
        assertEquals(0.0, attr.getRepresentativeFraction());
        assertEquals(layer, attr.getSummaryDataLayer());
        assertEquals(shape, attr.getShape());
        assertFalse(attr.containsKey("STATE_NAME"));
    }

    public void testReadBogusFile() {
        Point point = geometryBuilder.buildPointFromLatLon(-122.65, 38.25);
        Geometry shape = point.buffer(1000);

        SummaryDataLayer layer = new SummaryDataLayer();
        layer.setDataLocation("/path/that/does/not/exist.shp");

        LayerReadingMethod method = new ShapefileLayerReadingMethod();
        try {
            LayerAttributes attr = method.readAttributes(geometryBuilder, layer, shape);
            fail("Expected IllegalArgumentException, but instead got: " + attr);
        } catch(IllegalArgumentException ex) {
            // Expected
        }
    }

    public void testReadExtent() {
        // Petaluma, CA
        final double latitude = 38.25;
        final double longitude = -122.65;

        Point point = geometryBuilder.buildPointFromLatLon(longitude, latitude);

        LayerReadingMethod method = new ShapefileLayerReadingMethod();
        Geometry extent = method.readExtent(geometryBuilder, shapefilePath);

        assertNotNull(extent);
        assertTrue(extent.contains(point));
    }

    public void testOpenBogusFile() {
        LayerReadingMethod method = new ShapefileLayerReadingMethod();
        try {
            Geometry extent = method.readExtent(geometryBuilder, "/path/that/does/not/exist.shp");
            fail("Expected IllegalArgumentException, but instead got: " + extent);
        } catch(IllegalArgumentException ex) {
            // Expected
        }
    }

//    // Commented out because (a) the path is hardcoded below, and
//    //                       (b) even though it's pretty fast now, it still takes 9+ seconds on my machine
//    public void testReadingLargeShapefile() {
//        LayerReadingMethod method = new ShapefileLayerReadingMethod();
//        Geometry extent = method.readExtent(geometryBuilder, "C:\\Temp\\Smartfire\\SummaryData\\FIPS.shp");
//        //Geometry extent = method.readExtent(geometryBuilder, "C:\\Temp\\Smartfire\\SummaryData\\FCCS2_MultipartToSinglepart.shp");
//    }
    
}

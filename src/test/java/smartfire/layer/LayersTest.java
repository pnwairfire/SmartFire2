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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import java.util.Arrays;
import java.util.List;
import smartfire.SmartfireException;
import smartfire.database.SummaryDataLayer;
import smartfire.gis.Union;

public class LayersTest extends ShapefileTest {
    private static final String METHOD_NAME = ShapefileLayerReadingMethod.class.getName();

    public LayersTest(String testName) {
        super(testName);
    }

    public void testLayerReadingMethods() {
        List<String> methods = Layers.getLayerReadingMethods();
        assertTrue(methods.contains(METHOD_NAME));
    }

    public void testDetermineExtent() {
        // Petaluma, CA
        final double latitude = 38.25;
        final double longitude = -122.65;

        Point point = geometryBuilder.buildPointFromLatLon(longitude, latitude);

        Geometry extent = Layers.determineExtent(geometryBuilder, METHOD_NAME, shapefilePath);

        assertNotNull(extent);
        assertTrue(extent.contains(point));
    }

    public void testDetermineExtentWithInvalidLayerReadingMethod() {
        try {
            Geometry extent = Layers.determineExtent(geometryBuilder, "Bogus!", shapefilePath);
            fail("Expected SmartfireException, but instead got: " + extent);
        } catch(SmartfireException ex) {
            // Expected
        }
    }

    public void testReadAttributesFromInvalidLayerReadingMethod() {
        Point point = geometryBuilder.buildPointFromLatLon(-122.65, 38.25);
        Geometry shape = point.buffer(1000);

        SummaryDataLayer layer = new SummaryDataLayer();
        layer.setDataLocation("/bogus/path");
        layer.setLayerReadingMethod("Bogus!");
        try {
            LayerAttributes attr = Layers.readAttributes(geometryBuilder, layer, shape);
            fail("Expected SmartfireException, but instead got: " + attr);
        } catch(SmartfireException ex) {
            // Expected
        }
    }

    public void testReadAttributesFromOutsideExtent() {
        // Somewhere over the Pacific Ocean
        final double latitude = 37;
        final double longitude = -123;

        Point point = geometryBuilder.buildPointFromLatLon(longitude, latitude);
        Geometry shape = point.buffer(1000);

        SummaryDataLayer layer = new SummaryDataLayer();
        layer.setDataLocation(shapefilePath);
        layer.setLayerReadingMethod(METHOD_NAME);

        Geometry extent = Layers.determineExtent(geometryBuilder, METHOD_NAME, shapefilePath);
        assertFalse(extent.intersects(shape));
        layer.setExtent(extent);

        LayerAttributes attr = Layers.readAttributes(geometryBuilder, layer, shape);
        
        assertNotNull(attr);
        assertEquals(0.0, attr.getRepresentativeFraction());
        assertEquals(layer, attr.getSummaryDataLayer());
        assertEquals(shape, attr.getShape());
        assertFalse(attr.containsKey("STATE_NAME"));
    }

    public void testReadAttributesOverlappingExtent() {
        // Somewhere over the Pacific Ocean
        final double bogusLatitude = 37;
        final double bogusLongitude = -123;

        Point bogusPoint = geometryBuilder.buildPointFromLatLon(bogusLongitude, bogusLatitude);
        Geometry bogusShape = bogusPoint.buffer(1000);

        // Petaluma, CA
        final double stiLatitude = 38.25;
        final double stiLongitude = -122.65;

        Point stiPoint = geometryBuilder.buildPointFromLatLon(stiLongitude, stiLatitude);
        Geometry stiShape = stiPoint.buffer(1000);

        Geometry multiShape = Union.unionAll(Arrays.asList(bogusShape, stiShape));

        Geometry extent = Layers.determineExtent(geometryBuilder, METHOD_NAME, shapefilePath);

        assertTrue(extent.intersects(multiShape));
        assertFalse(extent.contains(multiShape));

        SummaryDataLayer layer = new SummaryDataLayer();
        layer.setDataLocation(shapefilePath);
        layer.setLayerReadingMethod(METHOD_NAME);
        layer.setExtent(extent);

        LayerAttributes attr = Layers.readAttributes(geometryBuilder, layer, multiShape);

        assertNotNull(attr);
        assertEquals(0.5, attr.getRepresentativeFraction(), 0.01);
        assertEquals("California", attr.get("STATE_NAME"));
    }
}

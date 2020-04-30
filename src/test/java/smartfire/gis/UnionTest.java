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

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;

public class UnionTest extends TestCase {
    private GeometryFactory factory;

    @Override
    protected void setUp() {
        factory = new GeometryFactory();
    }

    @Override
    protected void tearDown() throws Exception {
        factory = null;
    }

    public UnionTest(String testName) {
        super(testName);
    }

    public void testUnionSingleShape() {
        Geometry shape = circleAtPoint(10, 10, 5);
        List<Geometry> shapes = Arrays.asList(shape);
        Geometry result = Union.unionAll(shapes);
        assertSame(shape, result);
    }

    public void testUnionTwoShapes() {
        List<Geometry> shapes = Lists.newArrayList();
        shapes.add(circleAtPoint(10, 10, 5));
        shapes.add(circleAtPoint(10, 14, 5));
        Geometry geom = Union.unionAll(shapes);
        Envelope env = geom.getEnvelopeInternal();
        assertEquals(5.0, env.getMinX());
        assertEquals(15.0, env.getMaxX());
        assertEquals(5.0, env.getMinY());
        assertEquals(19.0, env.getMaxY());
    }

    public void testUnionEntities() {
        List<TestEntity> entities = Lists.newArrayList();
        entities.add(circleAtPointEntity(10, 10, 5));
        entities.add(circleAtPointEntity(10, 14, 5));
        Geometry geom = Union.unionAllShapes(entities);
        Envelope env = geom.getEnvelopeInternal();
        assertEquals(5.0, env.getMinX());
        assertEquals(15.0, env.getMaxX());
        assertEquals(5.0, env.getMinY());
        assertEquals(19.0, env.getMaxY());
    }

    public void testUnionEmptyListThrowsIllegalArgumentException() {
        Iterable<Geometry> emptyList = Collections.emptyList();
        try {
            Geometry result = Union.unionAll(emptyList);
            fail("Expected an exception, but instead got: " + result);
        } catch(IllegalArgumentException ex) {
            // Expected
        }
    }

    public void testUnionListWithNullGeometryThrowsIllegalArgumentException() {
        Iterable<TestEntity> geometries = Arrays.asList(new TestEntity(null));
        try {
            Geometry result = Union.unionAllShapes(geometries);
            fail("Expected an exception, but instead got: " + result);
        } catch(IllegalArgumentException ex) {
            // Expected
        }
    }

    public void testMultiPolygon() {
        List<Geometry> shapes = Lists.newArrayList();
        shapes.add(circleAtPoint(10, 10, 5));
        shapes.add(circleAtPoint(10, 20, 5));
        Geometry geom = Union.unionAll(shapes);
        assertEquals(MultiPolygon.class, geom.getClass());
        assertEquals(2, geom.getNumGeometries());
        MultiPolygon mp = Union.toMultiPolygon(geom);
        assertSame(geom, mp);
        Envelope env = geom.getEnvelopeInternal();
        assertEquals(5.0, env.getMinX());
        assertEquals(15.0, env.getMaxX());
        assertEquals(5.0, env.getMinY());
        assertEquals(25.0, env.getMaxY());
    }

    public void testPolygonAsMultiPolygon() {
        Polygon poly = (Polygon) circleAtPoint(10, 10, 5);
        MultiPolygon mp = Union.toMultiPolygon(poly);
        assertEquals(1, mp.getNumGeometries());
        assertEquals(poly.getNumPoints(), mp.getNumPoints());
        Polygon other = (Polygon) mp.getGeometryN(0);
        LineString polyRing = poly.getExteriorRing();
        LineString otherRing = other.getExteriorRing();
        int nPoints = polyRing.getNumPoints();
        for(int i = 0; i < nPoints; i++) {
            Coordinate polyCoord = polyRing.getCoordinateN(i);
            Coordinate otherCoord = otherRing.getCoordinateN(i);
            assertEquals(polyCoord, otherCoord);
        }
    }

    public void testNullToMultiPolygon() {
        MultiPolygon result = Union.toMultiPolygon(null);
        assertNull(result);
    }

    public void testToMultiPolygonOfNonPolygonThrowsException() {
        Coordinate coord = new Coordinate(10, 10);
        Geometry point = factory.createPoint(coord);
        try {
            Union.toMultiPolygon(point);
            fail("Expected ClassCastException from Union.toMultiPolygon(point)");
        } catch(ClassCastException e) {
            // OK, this was expected
        }
    }

    private Geometry circleAtPoint(double x, double y, double radius) {
        Coordinate coord = new Coordinate(x, y);
        Point point = factory.createPoint(coord);
        Geometry poly = point.buffer(radius);
        return poly;
    }

    private TestEntity circleAtPointEntity(double x, double y, double radius) {
        Geometry poly = circleAtPoint(x, y, radius);
        return new TestEntity(poly);
    }

    private static class TestEntity implements GeometryEntity {
        private final Geometry shape;

        public TestEntity(Geometry shape) {
            this.shape = shape;
        }

        @Override
        public Geometry getShape() {
            return shape;
        }

        @Override
        public String getShapeName() {
            return "Unknown";
        }
    }
}

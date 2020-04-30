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
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DissolveTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(DissolveTest.class);
    private GeometryFactory factory;

    public DissolveTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() {
        factory = new GeometryFactory();
    }

    @Override
    protected void tearDown() throws Exception {
        factory = null;
    }

    /**
     * Test that, if we start with two disjoint shapes, after Dissolving they
     * are still disjoint.
     */
    public void testTwoDisjointShapes() {
        List<TestEntity> entities = Arrays.asList(
                circleAtPoint(5, 5, 3),
                circleAtPoint(10, 10, 3));
        List<DissolvedEntity<TestEntity>> result = Dissolve.dissolve(entities);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getDerivedFromEntities().size());
        assertSame(entities.get(0), result.get(0).getDerivedFromEntities().get(0));
        assertEquals(1, result.get(1).getDerivedFromEntities().size());
        assertSame(entities.get(1), result.get(1).getDerivedFromEntities().get(0));
    }

    /**
     * Test that we can Dissolve two overlapping shapes into a single shape.
     */
    public void testTwoShapes() {
        List<TestEntity> entities = Arrays.asList(
                circleAtPoint(5, 5, 3),
                circleAtPoint(7, 7, 3));
        List<DissolvedEntity<TestEntity>> result = Dissolve.dissolve(entities);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getDerivedFromEntities().size());
        assertTrue(result.get(0).getDerivedFromEntities().contains(entities.get(0)));
        assertTrue(result.get(0).getDerivedFromEntities().contains(entities.get(1)));
    }

    /**
     * The basic idea of this test is that we create a grid (lattice) of
     * evenly-spaced little circles, and then for some of those circles we
     * also add several other shapes close enough to be touching or
     * overlapping.  After dissolving, all the clusters of shapes should
     * dissolve into single shapes, so we should be left with exactly one
     * shape for each grid cell.
     *
     * This is also a bit of a torture test for the dissolve algorithm,
     * to make sure it performs reasonably well over semi-realistic
     * data sets.
     */
    public void testLattice() {
        List<TestEntity> entities = Lists.newArrayList();
        final int SIZE = 600;
        final int STEP = 10;
        final int CLUSTER_INTERVAL = 10 * STEP;
        final int COUNT = (SIZE / STEP) * (SIZE / STEP);

        long started = System.nanoTime();
        for(int x = 0; x < SIZE; x += STEP) {
            for(int y = 0; y < SIZE; y += STEP) {
                entities.add(circleAtPoint(x, y, 3));
                if(x % CLUSTER_INTERVAL == 0 && y % CLUSTER_INTERVAL == 0) {
                    entities.add(circleAtPoint(x + 1, y + 1, 3));
                    entities.add(circleAtPoint(x + 1, y + 0, 3));
                    entities.add(circleAtPoint(x + 0, y + 1, 3));
                    entities.add(circleAtPoint(x - 1, y + 0, 3));
                }
            }
        }
        long elapsed = System.nanoTime() - started;
        log.debug("Lattice test created {} test shapes in {} seconds", entities.size(), (elapsed / 1000000000.0));

        List<DissolvedEntity<TestEntity>> result = Dissolve.dissolve(entities);
        assertEquals(COUNT, result.size());

        // OK, now we shuffle the inputs and make sure we can still come up
        // with the same output
        Collections.shuffle(entities);
        result = Dissolve.dissolve(entities);
        assertEquals(COUNT, result.size());
    }

    /**
     * Another dissolve test.  This time, we build a shape like an inverted V,
     * where we have SIZE elements along each leg, with the legs barely 
     * touching at the last point.  If the algorithm works as expected, the
     * legs will get assigned to two different buckets which will grow to meet
     * in the middle and which will then be merged into a single bucket in
     * pass 2 of the algorithm.
     */
    public void testMerge() {
        List<TestEntity> entities = Lists.newArrayList();
        final int SIZE = 1000;
        final int SPACING = 3;
        final int RADIUS = 3;
        final int HEIGHT = SIZE * SPACING;
        final int TOTAL_SHAPES = 1;

        long started = System.nanoTime();
        for(int y = 0; y < HEIGHT; y += SPACING) {
            int x1 = y;
            int x2 = (HEIGHT * 2) - y;
            entities.add(circleAtPoint(x1, y, RADIUS));
            entities.add(circleAtPoint(x2, y, RADIUS));
        }
        long elapsed = System.nanoTime() - started;
        log.debug("Merge test created {} test shapes in {} seconds", entities.size(), (elapsed / 1000000000.0));

        List<DissolvedEntity<TestEntity>> result = Dissolve.dissolve(entities);
        assertEquals(TOTAL_SHAPES, result.size());
    }

    private TestEntity circleAtPoint(double x, double y, double radius) {
        Coordinate coord = new Coordinate(x, y);
        Point point = factory.createPoint(coord);
        Geometry poly = point.buffer(radius);
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

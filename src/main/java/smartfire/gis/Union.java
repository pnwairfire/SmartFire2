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
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for unioning geometries.
 */
public class Union {
    private static final Logger log = LoggerFactory.getLogger(Union.class);
    private static final double SIMPLIFY_RESOLUTION = 10.0;
    private static final double PRECISION_SCALE = 8.0;
    
    private Union() { }
    
    public static Geometry union(Geometry... polys) {
        return unionAllImpl(Arrays.asList(polys));
    }

    /**
     * Union all the Geometry objects in the input into a single geometry.
     *
     * <p>This is a convenience method for creating a new Polygon (or possibly
     * MultiPolygon) from a collection of polygonal geometries.  This
     * implementation should be considerably faster than the naive method of
     * simply calling something like {@code a = a.union(b)} many many times.
     *
     * <p>If the input iterable contains zero geometries, this method will
     * return null.
     *
     * @param geometries an iterable of Geometry objects
     * @return a single unioned Geometry
     */
    public static Geometry unionAll(Iterable<? extends Geometry> geometries) {
        return unionAllImpl(Lists.newArrayList(geometries));
    }

    /**
     * Union all the geometries of all the GeometryEntity objects in the input
     * into a single geometry.
     *
     * <p>This is a convenience method for creating a new Polygon (or possibly
     * MultiPolygon) from a collection of polygonal geometries.  This
     * implementation should be considerably faster than the naive method of
     * simply calling something like {@code a = a.union(b)} many many times.
     *
     * <p>If the input iterable contains zero entities, this method will
     * return null.
     *
     * @param geometries an iterable of GeometryEntity objects
     * @return a single unioned Geometry
     */
    public static Geometry unionAllShapes(Iterable<? extends GeometryEntity> geometries) {
        List<Geometry> geomList = Lists.newArrayList();
        for(GeometryEntity entity : geometries) {
            Geometry geom = entity.getShape();
            if(geom == null) {
                throw new IllegalArgumentException("Geometry of " + entity + " is null");
            }
            geomList.add(geom);
        }
        return unionAllImpl(geomList);
    }

    private static Geometry unionAllImpl(List<Geometry> geomList) {
        int numGeoms = geomList.size();
        Geometry[] geoms = geomList.toArray(new Geometry[numGeoms]);
        if(numGeoms == 0) {
            throw new IllegalArgumentException("Cannot union zero geometries");
        }
        if(numGeoms == 1) {
            return geoms[0];
        }
        GeometryFactory factory = geoms[0].getFactory();
        GeometryCollection collection = factory.createGeometryCollection(geoms);
        Geometry result;
        try {
            result = collection.union();
        } catch (TopologyException e) {
            log.info("Unioning shapes produced a topology exception. Simplifying each of the shapes...");
            for (int i = 0; i < geoms.length; i++) {
                geoms[i] = TopologyPreservingSimplifier.simplify(geoms[i], SIMPLIFY_RESOLUTION);
            }
            collection = factory.createGeometryCollection(geoms);
            try {
                result = collection.union();
            } catch (TopologyException e2) {
                log.info("Topology exception still encountered.  Reducing precision of each shape...");
                for (int i = 0; i < geoms.length; i++) {
                    geoms[i] = GeometryPrecisionReducer.reduce(geoms[i], new PrecisionModel(PRECISION_SCALE));
                }
                collection = factory.createGeometryCollection(geoms);
                result = collection.union();
            }
        }
        return result;
    }

    /**
     * Convert a Geometry instance (possibly returned by {@link Union#unionAll(java.lang.Iterable)
     * unionAll} into a MultiPolygon instance.
     *
     * @param geom a Geometry instance; either a Polygon or a MultiPolygon
     * @return a MultiPolygon representing the same Geometry
     * @throws ClassCastException if the argument is not an instance of either
     *                            Polygon or MultiPolygon
     */
    public static MultiPolygon toMultiPolygon(Geometry geom) {
        if(geom == null) {
            return null;
        }
        if(geom instanceof MultiPolygon) {
            return (MultiPolygon) geom;
        }
        if(geom instanceof Polygon) {
            GeometryFactory factory = geom.getFactory();
            return factory.createMultiPolygon(new Polygon[] { (Polygon) geom });
        } else {
            throw new ClassCastException("Unable to convert from shape of type \"" +
                    geom.getClass().getSimpleName() + "\" to MultiPolygon");
        }
    }
}

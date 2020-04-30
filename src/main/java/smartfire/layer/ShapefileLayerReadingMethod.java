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

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import java.io.IOException;
import java.util.List;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.kohsuke.MetaInfServices;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.SummaryDataLayer;
import smartfire.gis.*;
import smartfire.util.ShapefileUtil;

/**
 * LayerReadingMethod implementation for reading data from Shapefile layers.
 */
@MetaInfServices
public class ShapefileLayerReadingMethod implements LayerReadingMethod {
    private static final Logger log = LoggerFactory.getLogger(ShapefileLayerReadingMethod.class);

    @Override
    public LayerAttributes readAttributes(GeometryBuilder geometryBuilder, SummaryDataLayer layer, Geometry geom) {
        ShapeAttributes shapeAttributes = ShapefileUtil.readShapeFile(geometryBuilder, geom, layer.getDataLocation());
        return new LayerAttributes(shapeAttributes, layer);
    }

    @Override
    public Geometry readExtent(GeometryBuilder geometryBuilder, String dataLocation) throws IllegalArgumentException {
        log.info("Reading extent geometry from: {}", dataLocation);
        SimpleFeatureSource source = ShapefileUtil.openShapefile(dataLocation);
        try {
            CoordinateReferenceSystem sourceCRS = source.getSchema().getCoordinateReferenceSystem();
            CoordinateTransformer xform = ShapefileUtil.makeCoordinateTransformer(geometryBuilder, sourceCRS);

            // We overload our Dissolve algorithm to do the heavy lifting here.
            // First, we read each feature and simplify any features smaller
            // than our SIMPLIFY_DISTANCE (which is hard-coded to 5km here),
            // and then we buffer outwards by the same amount (so as to fill
            // in any gaps that may have been created when the geometries were
            // simplified).
            //
            // Next, we wrap each geometry in a GeometryWrapper with no
            // payload (since we don't actually care about aggregating them).
            // Then Dissolve.dissolve() will efficiently group the geometries
            // into nearby spatial clusters and union them (where the union
            // operation is going to be much faster because it can operate on
            // a bunch of nearby polygons instead of randomly creating disjoint
            // multipolygons all over the place).  Then we just run another
            // union to merge all the unioned shapes into one big multipolygon.

            final int BATCH_SIZE = 10000;
            final double SIMPLIFY_DISTANCE = 5000;
            List<GeometryEntity> geoms = Lists.newArrayList();
            List<GeometryEntity> batch = Lists.newArrayList();
            int numFeatures = 0;
            long started = System.currentTimeMillis();
            SimpleFeatureIterator features = source.getFeatures().features();
            try {
                while(features.hasNext()) {
                    SimpleFeature feature = features.next();
                    numFeatures++;
                    Geometry geom = (Geometry) feature.getDefaultGeometry();
                    Geometry projected = xform.transform(geom);
                    Geometry simplified = DouglasPeuckerSimplifier.simplify(projected, SIMPLIFY_DISTANCE);
                    Geometry smoothed = simplified.buffer(SIMPLIFY_DISTANCE);
                    batch.add(GeometryWrapper.wrap(null, smoothed));

                    if(batch.size() > BATCH_SIZE) {
                        Geometry merged = Union.unionAllShapes(Dissolve.dissolve(batch));
                        geoms.add(GeometryWrapper.wrap(null, merged));
                        batch.clear();
                    }
                }
            } finally {
                features.close();
            }
            if(numFeatures == 0) {
                throw new IllegalArgumentException("Shapefile contains zero features: \"" + dataLocation + "\"");
            }
            geoms.addAll(batch);
            Geometry fullExtent = Union.unionAllShapes(Dissolve.dissolve(geoms));
            long elapsed = System.currentTimeMillis() - started;
            log.info("Computed extent geometry from {} features in {} seconds",
                    numFeatures, (elapsed / 1000));

            return fullExtent;
        } catch(IOException ex) {
            throw new IllegalArgumentException("Unable to read extent from shapefile", ex);
        } finally {
            source.getDataStore().dispose();
        }
    }
}

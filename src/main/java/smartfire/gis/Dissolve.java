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
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.queue.ProgressReporter;

/**
 * Implementation of dissolve algorithm.
 */
public class Dissolve {
    private static final Logger log = LoggerFactory.getLogger(Dissolve.class);

    private Dissolve() { }

    /**
     * Dissolves a collection of input geometries into distinct topological
     * subsets.
     *
     * <p>The basic algorithm is as follows: given a collection of
     * GeometryEntity instances, we collect them into <i>buckets</i>, such that,
     * within each bucket, every geometry <b>intersect</b>s at least one other
     * geometry in that bucket, and no geometry inside the bucket intersects
     * any geometry in another bucket.  The geometries inside each bucket
     * are then <b>union</b>ed to form a composite geometry (usually a
     * MultiPolygon).
     *
     * <p>The results are returned as a collection of {@link DissolvedEntity}
     * instances, each of which represents the contents of a single bucket.
     *
     * @param <T> the type of GeometryEntity on which to operate
     * @param inputEntities a collection of input GeometryEntity objects
     * @return a collection of DissolvedEntity objects
     */
    public static <T extends GeometryEntity> List<DissolvedEntity<T>> dissolve(Iterable<T> inputEntities) {
        return dissolve(inputEntities, new ProgressReporter());
    }

    /**
     * Dissolves a collection of input geometries into distinct topological
     * subsets.
     *
     * <p>See {@link dissolve(java.lang.Iterable)} for a description of the
     * algorithm.
     *
     * <p>This overload accepts a ProgressReporter object, which can be used
     * to provide feedback to the user as the dissolve operation proceeds.
     *
     * @param <T> the type of GeometryEntity on which to operate
     * @param inputEntities a collection of input GeometryEntity objects
     * @param progressReporter for providing progress feedback
     * @return a collection of DissolvedEntity objects
     */
    public static <T extends GeometryEntity> List<DissolvedEntity<T>> dissolve(
            Iterable<T> inputEntities, ProgressReporter progressReporter) {

        List<T> input = Lists.newArrayList(inputEntities);
        int size = input.size();

        String message = "Dissolving " + size + " input shapes";
        progressReporter.setProgress(0, message);
        log.debug(message);
        long started = System.nanoTime();

        List<Bucket<T>> buckets = Lists.newArrayListWithExpectedSize(size / 2);
        int percent = -1;
        int counter = 0;

        log.trace("Starting pass 1: separate into buckets");
        for(T entity : input) {
            counter++;
            int newPercent = (int) ((counter / (double) size) * 50);
            if(newPercent > percent) {
                percent = newPercent;
                progressReporter.setProgress(percent, "Dissolving: separate into buckets");
            }

            // Construct a new BucketItem for this entity
            BucketItem<T> item = new BucketItem<T>(entity);

            // Try to find a bucket of items where this item would fit.
            // Looping backwards may be a small performance boost if nearby
            // geometries tend to appear in groups in our input data.
            boolean found = false;
            for(int i = buckets.size() - 1; i >= 0; i--) {
                Bucket<T> bucket = buckets.get(i);
                if(bucket.intersectsAny(item)) {
                    bucket.add(item);
                    found = true;
                    break;
                }
            }

            // If we didn't find a bucket to put it in, create a new bucket
            if(!found) {
                Bucket<T> bucket = new Bucket<T>(item);
                buckets.add(bucket);
            }
        }
        log.trace("Completed pass 1; got {} buckets", buckets.size());

        log.trace("Starting pass 2: merge buckets if necessary");
        for(int i = 0; i < buckets.size(); i++) {
            int newPercent = (int) ((i / (double) buckets.size()) * 10) + 50;
            if(newPercent > percent) {
                progressReporter.setProgress(percent, "Dissolving: merge buckets if necessary");
            }

            Bucket<T> leftBucket = buckets.get(i);
            for(int j = i + 1; j < buckets.size(); j++) {
                Bucket<T> rightBucket = buckets.get(j);
                if(leftBucket.intersects(rightBucket)) {
                    leftBucket.absorb(rightBucket);
                    buckets.remove(j);
                    j--;
                }
            }
        }
        log.trace("Completed pass 2; got {} buckets", buckets.size());

        log.trace("Starting pass 3: union shapes");
        int resultSize = buckets.size();
        List<DissolvedEntity<T>> result = Lists.newArrayListWithCapacity(resultSize);
        counter = 0;
        for(Bucket<T> bucket : buckets) {
            counter++;
            int newPercent = (int) ((counter / (double) resultSize) * 40) + 60;
            if(newPercent > percent) {
                progressReporter.setProgress(percent, "Dissolving: union shapes");
            }

            result.add(dissolveBucket(bucket));
        }
        log.trace("Completed pass 3");

        long elapsed = System.nanoTime() - started;
        log.debug("Dissolved {} shapes into {} shapes in {} seconds",
                new Object[] { size, resultSize, (elapsed / 1000000000.0) });

        // At this point, all the shapes in the result list will be disjoint
        // from each other; we're done.
        progressReporter.setProgress(100, "Dissolve complete");
        return result;
    }

    /**
     * This method constructs a DissolvedEntity object, given a Bucket object.
     *
     * <p>The geometries from all the BucketItems in the given Bucket are
     * collected into a GeometryCollection and then <b>union</b>ed into a
     * single Geometry.  The entities (of generic type T) associated with
     * those BucketItems are gathered into a List.  Then that Geometry and
     * that List are used to construct a new DissolvedEntity.
     *
     * @param <T> the type of GeometryEntity being operated upon
     * @param bucket a bucket containing BucketItems
     * @return a DissolvedEntity representing this bucket
     */
    private static <T extends GeometryEntity> DissolvedEntity<T> dissolveBucket(Bucket<T> bucket) {
        GeometryFactory factory = bucket.items.get(0).geom.getFactory();
        int count = bucket.items.size();
        Geometry[] geoms = new Geometry[count];
        List<T> derivedFrom = Lists.newArrayListWithCapacity(count);
        for(int i = 0; i < count; i++) {
            BucketItem<T> item = bucket.items.get(i);
            geoms[i] = item.geom;
            derivedFrom.add(item.entity);
        }
        Geometry result = Union.union(geoms);
        return new DissolvedEntity<T>(result, derivedFrom);
    }

    /**
     * Represents a Bucket used in the Dissolve algorithm. Essentially, it
     * acts as a generic container for BucketItems, except that it also
     * maintains an Envelope representing the bounding box of all the
     * Geometries of all its BucketItems.  This class also provides a number
     * of utility methods for interacting with its items.
     *
     * @param <T> the type of GeometryEntity being operated upon
     */
    private static class Bucket<T extends GeometryEntity> {
        private final List<BucketItem<T>> items;
        private final Envelope envelope;

        private Bucket(BucketItem<T> item) {
            this.items = Lists.newArrayList();
            items.add(item);
            this.envelope = item.envelope;
        }

        private boolean intersects(Bucket<T> other) {
            // If the envelopes don't intersect, then none of the shapes
            // are going to intersect either.
            if(!envelope.intersects(other.envelope)) {
                return false;
            }
            
            // This is O(scary), but it's fast enough in practice...
            for(BucketItem<T> otherItem : other.items) {
                if(this.intersectsAny(otherItem)) {
                    return true;
                }
            }

            // If we couldn't find one, then the buckets don't intersect
            return false;
        }

        private void absorb(Bucket<T> other) {
            for(BucketItem<T> otherItem : other.items) {
                this.add(otherItem);
            }
        }

        private boolean intersectsAny(BucketItem<T> item) {
            // If the envelopes don't intersect, then none of the shapes
            // are going to intersect either.
            if(!envelope.intersects(item.envelope)) {
                return false;
            }

            // Try to find an item in this bucket that intersects with
            // the given item
            for(BucketItem<T> myItem : items) {
                if(myItem.geom.intersects(item.geom)) {
                    return true;
                }
            }

            // If we couldn't find one, then none of them intersect
            return false;
        }

        private void add(BucketItem<T> item) {
            items.add(item);
            envelope.expandToInclude(item.envelope);
        }
    }

    /**
     * Represents an item that can be stored in a Bucket.  Each item has three
     * fields: (1) a GeometryEntity (generically represented by the type T)
     * that represents a domain entity of interest; (2) a Geometry associated
     * with that GeometryEntity; and (3) the Envelope of that Geometry, to
     * allow for quick intersection filtering.
     *
     * @param <T> the type of GeometryEntity being operated upon
     */
    private static class BucketItem<T extends GeometryEntity> {
        private final T entity;
        private final Geometry geom;
        private final Envelope envelope;

        private BucketItem(T entity) {
            this.entity = entity;
            this.geom = entity.getShape();
            this.envelope = geom.getEnvelopeInternal();
        }
    }
}

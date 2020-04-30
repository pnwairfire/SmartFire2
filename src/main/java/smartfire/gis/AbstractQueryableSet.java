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

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.util.*;
import org.joda.time.DateTime;
import smartfire.database.AbstractDao;
import smartfire.database.QueryableEntity;

/**
 * Abstract base class for QueryableFireSet and QueryableEventSet.
 * 
 * @param <TEntity> the type of the entity; e.g. Event or Fire
 * @param <TKey> the type of the primary key of the entity; e.g. Integer
 * @param <TDao> the DAO class for the entity
 */
abstract class AbstractQueryableSet<TEntity extends QueryableEntity<TKey>, TKey, TDao extends AbstractDao<TEntity, TKey>> 
        extends AbstractSet<TEntity> {
    private final TDao dao;
    private final Set<TEntity> storage = new HashSet<TEntity>();
    private DateTime currentStart = null;
    private DateTime currentEnd = null;
    
    protected AbstractQueryableSet(TDao dao) {
        this.dao = dao;
    }

    protected abstract List<TEntity> fetchByDate(TDao dao, DateTime startDate, DateTime endDate);
    
    protected abstract TEntity mergeInternal(TDao dao, List<TEntity> toMerge);
    
    protected void prefetch(DateTime requestedStart, DateTime requestedEnd) {
        // if private storage is empty, then query the database and
        // and save to private storage.
        if(currentStart == null || currentEnd == null) {
            this.add(this.fetchByDate(dao, requestedStart, requestedEnd));
            currentStart = requestedStart;
            currentEnd = requestedEnd;
            return;
        }

        // If we don't have all the data then determine what data we need and
        // then query the database and save the results to private storage.
        boolean startDateNotInRange = currentStart.isAfter(requestedStart);
        boolean endDateNotInRange = currentEnd.isBefore(requestedEnd);
        if(startDateNotInRange || endDateNotInRange) {
            DateTime fetchStart = min(currentEnd, requestedStart);
            DateTime fetchEnd = max(currentStart, requestedEnd);
            this.add(this.fetchByDate(dao, fetchStart, fetchEnd));
            currentStart = min(currentStart, requestedStart);
            currentEnd = max(currentEnd, requestedEnd);
        }
    }
    
    /**
     * If any entities are associated with this set but not yet persisted to
     * the database, save them using the associated DAO object.  Returns the
     * number of new entities that were saved.
     * 
     * @return the number of new entities that were saved
     */
    public int saveNewEntities() {
        int counter = 0;
        for(TEntity record : storage) {
            if(record.getId() == null) {
                counter++;
                dao.save(record);
            }
        }
        return counter;
    }
    
    @Override
    public boolean add(TEntity record) {
        return storage.add(record);
    }

    public void add(Iterable<TEntity> newRecords) {
        for(TEntity fire : newRecords) {
            this.add(fire);
        }
    }

    public List<TEntity> getAssociated(Geometry intersectionShape, DateTime startDate, DateTime endDate) {
        Envelope intersectionShapeEnvelope = intersectionShape.getEnvelopeInternal();
        List<TEntity> associatedEntities = Lists.newArrayList();
        // Query for entities within the date range.
        List<TEntity> entities = this.getByDate(startDate, endDate);
        // Scan to find associated entities.
        for(TEntity entity : entities) {
            if(intersectionShapeEnvelope.intersects(entity.getShape().getEnvelopeInternal())
                && intersectionShape.intersects(entity.getShape())) {
                associatedEntities.add(entity);
            }
        }
        return associatedEntities;
    }

    public List<TEntity> getByDate(DateTime startDate, DateTime endDate) {
        prefetch(startDate, endDate);
        List<TEntity> entities = Lists.newArrayList();
        // Query the private storage for entities within the date range.
        for(TEntity entity : storage) {
            boolean startDateInRange = entity.getStartDateTime().isBefore(endDate) || entity.getStartDateTime().equals(endDate);
            boolean endDateInRange = entity.getEndDateTime().isAfter(startDate) || entity.getEndDateTime().equals(startDate);
            if(startDateInRange && endDateInRange) {
                entities.add(entity);
            }
        }
        return entities;
    }

    public List<TEntity> getMatching(Predicate<TEntity> predicate, DateTime startDate, DateTime endDate) {
        List<TEntity> associatedEntities = Lists.newArrayList();
        // Query for entities within the date range.
        List<TEntity> entities = this.getByDate(startDate, endDate);
        // Scan to find associated entities.
        for(TEntity entity : entities) {
            if(predicate.apply(entity)) {
                associatedEntities.add(entity);
            }
        }
        return associatedEntities;
    }

    @Override
    public Iterator<TEntity> iterator() {
        return storage.iterator();
    }
    
    public TEntity merge(Iterable<TEntity> toMerge) {
        List<TEntity> entities = Lists.newArrayList(toMerge);
        TEntity result = mergeInternal(dao, entities);
        this.removeAll(entities);
        this.add(result);
        return result;
    }

    public TEntity merge(TEntity... toMerge) {
        return merge(Arrays.asList(toMerge));
    }

    @Override
    public int size() {
        return storage.size();
    }
    
    private static DateTime min(DateTime dt1, DateTime dt2) {
        if(dt1.isBefore(dt2)) {
            return dt1;
        }
        return dt2;
    }

    private static DateTime max(DateTime dt1, DateTime dt2) {
        if(dt1.isAfter(dt2)) {
            return dt1;
        }
        return dt2;
    }
}

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
package smartfire.database;

import com.google.common.collect.Lists;
import java.util.*;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableDateTime;
import smartfire.SmartfireException;

public class FireDao extends AbstractDao<Fire, Integer> {
    public FireDao(DatabaseConnection conn) {
        super(Fire.class, conn);
    }

    /**
     * Gets all the Fire records for a source that occurred on a given date.
     *
     * @param source the data source for the Fire.
     * @param dt the date of interest
     * @return a list of Fire records
     */
    public List<Fire> getByDate(Source source, ReadableDateTime dt) {
        return getByDate(source, dt, dt);
    }

    /**
     * Gets all the Fire records that are available for a given source
     * and date range.
     *
     * @param source the source for the clump data.
     * @param start the start date of interest
     * @param end the end date of interest
     * @return a list of Fire records
     */
    public List<Fire> getByDate(Source source, ReadableDateTime start, ReadableDateTime end) {
        Date startDate = start.toDateTime().withZone(DateTimeZone.UTC).toDate();
        Date endDate = end.toDateTime().withZone(DateTimeZone.UTC).toDate();

        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Fire> cq = cb.createQuery(Fire.class);
        Root<Fire> fire = cq.from(Fire.class);

        Subquery<Integer> subquery = cq.subquery(Integer.class);
        Root<Fire> f = subquery.from(Fire.class);
        Join<Fire, Clump> clump = f.join(Fire_.clumps);

        Expression<Date> minStartDate = cb.least(clump.get(Clump_.startDate));
        Expression<Date> maxEndDate = cb.greatest(clump.get(Clump_.endDate));

        return em.createQuery(cq
                .select(fire)
                .where(
                    fire.get(Fire_.id).in(subquery
                        .select(f.get(Fire_.id))
                        .groupBy(f.get(Fire_.id))
                        .having(
                            cb.and(
                                cb.lessThanOrEqualTo(minStartDate, endDate),
                                cb.greaterThanOrEqualTo(maxEndDate, startDate)
                            )
                        )
                    ),
                    cb.equal(fire.get(Fire_.source), source)
                )).getResultList();
    }
    
    /**
     * Finds all the Fire instances in the database that are orphaned due to
     * having no associated Clumps.
     * 
     * @param source the Source of the fires to search
     * @return a list of Fire records
     */
    public List<Fire> findOrphanedFires(Source source) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Fire> cq = cb.createQuery(Fire.class);
        Root<Fire> fire = cq.from(Fire.class);

        Subquery<Integer> subquery = cq.subquery(Integer.class);
        Root<Fire> f = subquery.from(Fire.class);

        return em.createQuery(cq
                .select(fire)
                .where(
                    fire.get(Fire_.id).in(subquery
                        .select(f.get(Fire_.id))
                        .where(cb.equal(f.get(Fire_.source), source))
                        .groupBy(f.get(Fire_.id))
                        .having(
                            cb.isEmpty(f.get(Fire_.clumps))
                        )
                    )
                )).getResultList();
    }
    
    /**
     * Gets all the Fire records that are available for a given date range.
     *
     * @param start the start date of interest
     * @param end the end date of interest
     * @return a list of Fire records
     */
    public List<Fire> getByDate(ReadableDateTime start, ReadableDateTime end) {
        Date startDate = start.toDateTime().withZone(DateTimeZone.UTC).toDate();
        Date endDate = end.toDateTime().withZone(DateTimeZone.UTC).toDate();

        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Fire> cq = cb.createQuery(Fire.class);
        Root<Fire> fire = cq.from(Fire.class);

        Subquery<Integer> subquery = cq.subquery(Integer.class);
        Root<Fire> f = subquery.from(Fire.class);
        Join<Fire, Clump> clump = f.join(Fire_.clumps);

        Expression<Date> minStartDate = cb.least(clump.get(Clump_.startDate));
        Expression<Date> maxEndDate = cb.greatest(clump.get(Clump_.endDate));

        return em.createQuery(cq
                .select(fire)
                .where(
                    fire.get(Fire_.id).in(subquery
                        .select(f.get(Fire_.id))
                        .groupBy(f.get(Fire_.id))
                        .having(
                            cb.lessThanOrEqualTo(minStartDate, endDate),
                            cb.greaterThanOrEqualTo(maxEndDate, startDate)
                        )
                    )
                )).getResultList();
    }

    /**
     * Gets a Fire that is identified by its unique ID.
     *
     * @param uid the unique id of a Fire
     * @return the Fire that is identified by the unique ID, or null if no
     *         such Fire exists
     */
    public Fire getByUniqueID(String uid) {
        if(uid == null) {
            return null;
        }
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Fire> c = cb.createQuery(Fire.class);
        Root<Fire> r = c.from(Fire.class);
        TypedQuery<Fire> q = em.createQuery(
                c.where(
                cb.equal(r.get(Fire_.uniqueId), uid)));
        try {
            return q.getSingleResult();
        } catch(NoResultException e) {
            return null;
        }
    }

    /**
     * Merge a collection of Fire objects into a single Fire.
     *
     * <p>This method always creates a new Fire object. The result fire's
     * clumps are all the clumps associated with all the input fires, and
     * the result fire is associated with all the events that any of the
     * input fires were associated with.
     *
     * <p>The input fires are marked for removal from the database when the
     * transaction is committed.
     *
     * @param fires a collection of input fires
     * @return a single fire object representing the merger of all the input fires
     */
    public Fire merge(Iterable<Fire> fires) {
        Fire result = new Fire();

        Map<String, String> attrs = new HashMap<String, String>();
        List<Clump> clumps = new ArrayList<Clump>();

        double totalArea = 0;
        
        for(Fire fire : fires) {
            // Ensure that all the Fires come from the same Source
            if(result.getSource() == null) {
                result.setSource(fire.getSource());
            } else if(!result.getSource().equals(fire.getSource())) {
                throw new SmartfireException("Incompatible sources for merged Fire: "
                        + result.getSource().getName() + " and " + fire.getSource().getName());
            }

            // Copy all the attributes from this fire
            attrs.putAll(fire);

            // Copy the clumps
            clumps.addAll(fire.getClumps());
            
            // Sum the area
            totalArea += fire.getArea();

            // Update any associated events to associate with our new Fire object
            for(Event event : fire.getEvents()) {
                event.removeFire(fire);
                event.addFire(result);
            }

            // Schedule the input fire for deletion
            this.delete(fire);
        }

        result.putAll(attrs);
        result.addClumps(clumps);
        result.setArea(totalArea);

        return result;
    }

    /**
     * Gets all the Fire records that are available for a given source.
     *
     * @param source the source for the fire.
     * @return a list of fire records
     */
    public List<Fire> getBySource(Source source) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Fire> c = cb.createQuery(Fire.class);
        Root<Fire> r = c.from(Fire.class);
        TypedQuery<Fire> q = em.createQuery(
                c.where(cb.equal(r.get(Fire_.source), source)));
        return q.getResultList();
    }

    /**
     * Deletes all the Fire records that are available for a given source.
     *
     * @param source the source for the fire
     */
    public void deleteBySource(Source source) {
        for(Fire record : getBySource(source)) {
            delete(record);
        }
    }
    
    /**
     * Deletes all the Fire records in the database that are orphaned (that is,
     * that have zero associated Clumps).
     */
    public void deleteOrphanedFires(Source source) {
        for(Fire record : findOrphanedFires(source)) {
            delete(record);
        }
    }
    
    @Override
    public void delete(Fire fire) {
        EntityManager em = getEntityManager();
        for(Event event : Lists.newArrayList(fire.getEvents())) {
            event.removeFire(fire);
            fire.removeEvent(event);
            if(event.getFires().isEmpty()) {
                em.remove(event);
            }
        }
        em.remove(fire);
    }
}

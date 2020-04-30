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

import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableDateTime;

public class ClumpDao extends AbstractDao<Clump, Integer> {
    public ClumpDao(DatabaseConnection conn) {
        super(Clump.class, conn);
    }

    /**
     * Gets all the Clump records that are available for a given source and date.
     *
     * @param source the source for the clump data.
     * @param dt the date of interest
     * @return a list of Clump records
     */
    public List<Clump> getByDate(Source source, ReadableDateTime dt) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Clump> c = cb.createQuery(Clump.class);
        Root<Clump> r = c.from(Clump.class);
        Date date = dt.toDateTime().withZone(DateTimeZone.UTC).toDate();
        TypedQuery<Clump> q = em.createQuery(
                c.where(
                cb.and(cb.equal(r.get(Clump_.source), source),
                cb.and(
                cb.lessThanOrEqualTo(r.get(Clump_.startDate), date),
                cb.greaterThanOrEqualTo(r.get(Clump_.endDate), date)))));
        return q.getResultList();
    }

    /**
     * Gets all the Clump records that are available for a given source
     * and date range.
     *
     * @param source the source for the clump data.
     * @param start the start date of interest
     * @param end the end date of interest
     * @return a list of Clump records
     */
    public List<Clump> getByDate(Source source, ReadableDateTime start, ReadableDateTime end) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Clump> cq = cb.createQuery(Clump.class);
        Root<Clump> clump = cq.from(Clump.class);
        Date startDate = start.toDateTime().withZone(DateTimeZone.UTC).toDate();
        Date endDate = end.toDateTime().withZone(DateTimeZone.UTC).toDate();
        return em.createQuery(cq
                .select(clump)
                .where(
                    // clump.source == source
                    cb.equal(clump.get(Clump_.source), source),
                    
                    // and clump.startDate <= argument.endDate
                    cb.lessThanOrEqualTo(clump.get(Clump_.startDate), endDate),
                    
                    // and clump.endDate >= argument.startDate
                    cb.greaterThanOrEqualTo(clump.get(Clump_.endDate), startDate)
                
                )).getResultList();
    }

    /**
     * Deletes all the Clump records that are available for a given date.
     *
     * @param source the source for the clump data
     * @param start the start date of interest
     * @param end the end date of interest
     */
    public void deleteByDate(Source source, ReadableDateTime start, ReadableDateTime end) {
        for(Clump record : getByDate(source, start, end)) {
            delete(record);
        }
    }

    /**
     * Gets all the Clump records that are available for a given source.
     *
     * @param source the source for the clump.
     * @return a list of clump records
     */
    public List<Clump> getBySource(Source source) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Clump> c = cb.createQuery(Clump.class);
        Root<Clump> r = c.from(Clump.class);
        TypedQuery<Clump> q = em.createQuery(
                c.where(cb.equal(r.get(Clump_.source), source)));
        return q.getResultList();
    }

    /**
     * Deletes all the Clump records that are available for a given source.
     *
     * @param source the source for the clump
     */
    public void deleteBySource(Source source) {
        for(Clump record : getBySource(source)) {
            delete(record);
        }
    }

    @Override
    public void delete(Clump entity) {
        EntityManager em = getEntityManager();
        Fire fire = entity.getFire();
        if(fire != null) {
            fire.removeClump(entity);
            if(fire.getClumps().isEmpty()) {
                em.remove(fire);
            }
        }
        em.remove(entity);
    }
}

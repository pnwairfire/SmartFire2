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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.ReadableDateTime;

public class RawDataDao extends AbstractDao<RawData, Long> {
    public RawDataDao(DatabaseConnection conn) {
        super(RawData.class, conn);
    }

    /**
     * Gets all the RawData records that are available for a given source and date.
     *
     * @param source the source for the raw data.
     * @param dt the date of interest
     * @return a list of RawData records
     */
    public List<RawData> getByDate(Source source, ReadableDateTime dt) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<RawData> c = cb.createQuery(RawData.class);
        Root<RawData> r = c.from(RawData.class);
        Date date = dt.toDateTime().withZone(DateTimeZone.UTC).toDate();
        TypedQuery<RawData> q = em.createQuery(
                c.where(
                cb.and(cb.equal(r.get(RawData_.source), source),
                cb.and(
                cb.lessThanOrEqualTo(r.get(RawData_.startDate), date),
                cb.greaterThanOrEqualTo(r.get(RawData_.endDate), date)))));
        return q.getResultList();
    }

    /**
     * Gets all the RawData records that are available for a given source and date range.
     *
     * @param source the source for the clump data.
     * @param start the start date of interest
     * @param end the end date of interest
     * @return a list of RawData records
     */
    public List<RawData> getByDate(Source source, ReadableDateTime start, ReadableDateTime end) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<RawData> cq = cb.createQuery(RawData.class);
        Root<RawData> rawData = cq.from(RawData.class);
        Date startDate = start.toDateTime().withZone(DateTimeZone.UTC).toDate();
        Date endDate = end.toDateTime().withZone(DateTimeZone.UTC).toDate();
        return em.createQuery(cq.select(rawData).where(
                // rawData.source == source
                cb.equal(rawData.get(RawData_.source), source),
                // and rawData.startDate <= argument.endDate
                cb.lessThanOrEqualTo(rawData.get(RawData_.startDate), endDate),
                // and rawData.endDate >= argument.startDate
                cb.greaterThanOrEqualTo(rawData.get(RawData_.endDate), startDate))).getResultList();
    }

    /**
     * Gets all the RawData records that are available for a given source, date range, and attribute.
     *
     * @param source the source for the clump data.
     * @param start the start date of interest
     * @param end the end date of interest
     * @return a list of RawData records
     */
    public List<RawData> getByDateAndAttribute(Source source, ReadableDateTime start, ReadableDateTime end, String attrKey, Set<String> attrVals) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        
        CriteriaQuery<RawData> cq = cb.createQuery(RawData.class);
        Root<RawData> rawData = cq.from(RawData.class);
        
        Subquery<Long> sq = cq.subquery(Long.class);
        Root<DataAttribute> dataAttribute = sq.from(DataAttribute.class);
        Join<DataAttribute, RawData> sq_join = dataAttribute.join(DataAttribute_.rawData);
        
        Date startDate = start.toDateTime().withZone(DateTimeZone.UTC).toDate();
        Date endDate = end.toDateTime().withZone(DateTimeZone.UTC).toDate();
        
        return em.createQuery(cq.select(rawData).where(
            // rawData.source == source
            cb.equal(rawData.get(RawData_.source), source),
            // and rawData.startDate <= argument.endDate
            cb.lessThanOrEqualTo(rawData.get(RawData_.startDate), endDate),
            // and rawData.endDate >= argument.startDate
            cb.greaterThanOrEqualTo(rawData.get(RawData_.endDate), startDate),
            // and rawData.id IN DataAttribute WHERE
            cb.in(rawData.get(RawData_.id)).value(
                sq.select(sq_join.get(RawData_.id)).where(
                    // dataAttribute.name == argument.attrkey
                    cb.equal(dataAttribute.get(DataAttribute_.name), attrKey),
                    // and dataAttribute.attr_value IN argument.attrVals
                    dataAttribute.get(DataAttribute_.attrValue).in(attrVals)
                )
            )
        )).getResultList();
    }

    /**
     * Helper class for the getDataInterval() method.
     */
    private static class DataInterval {
        private final DateTime start;
        private final DateTime end;

        public DataInterval(Date start, Date end) {
            this.start = new DateTime(start);
            this.end = new DateTime(end);
        }

        public Interval asInterval() {
            return new Interval(start, end);
        }
    }

    /**
     * Returns a date/time Interval representing the earliest and latest dates
     * for which data exists in the database for the given Source.
     *
     * @param source the data Source of interest
     * @return a JodaTime Interval object
     */
    public Interval getDataInterval(Source source) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<DataInterval> cq = cb.createQuery(DataInterval.class);
        Root<RawData> rawData = cq.from(RawData.class);
        return em.createQuery(cq.select(cb.construct(DataInterval.class,
                cb.least(rawData.get(RawData_.startDate)),
                cb.greatest(rawData.get(RawData_.endDate)))).where(cb.equal(rawData.get(RawData_.source), source))).getSingleResult().asInterval();
    }

    /**
     * Deletes all the RawData records that are available for a given date.
     *
     * @param source the source for the raw data
     * @param startTime the start of the time period of interest
     * @param endTime the end of the time period of interest
     */
    public void deleteByDate(Source source, ReadableDateTime startTime, ReadableDateTime endTime) {
        for(RawData record : getByDate(source, startTime, endTime)) {
            delete(record);
        }
    }

    /**
     * Deletes all the RawData records that are available for a given date with a matching attribute value.
     *
     * @param source the source for the raw data
     * @param startTime the start of the time period of interest
     * @param endTime the end of the time period of interest
     */
    public void deleteByDateAndAttribute(Source source, ReadableDateTime startTime, ReadableDateTime endTime, Map<String, Set<String>> attributeFilter) {
        for (Entry<String, Set<String>> entry : attributeFilter.entrySet()) {
            for(RawData record : getByDateAndAttribute(source, startTime, endTime, entry.getKey(), entry.getValue())) {
                delete(record);
            }
        }
    }

    /**
     * Gets all the RawData records that are available for a given source.
     *
     * @param source the source for the raw data.
     * @return a list of RawData records
     */
    public List<RawData> getBySource(Source source) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<RawData> c = cb.createQuery(RawData.class);
        Root<RawData> r = c.from(RawData.class);
        TypedQuery<RawData> q = em.createQuery(
                c.where(cb.equal(r.get(RawData_.source), source)));
        return q.getResultList();
    }

    /**
     * Counts the number of RawData records for a given source.
     * 
     * @param source the source for the raw data.
     * @return a long that represents the number of data records for a given source.
     */
    public Long getDataCount(Source source) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<RawData> rawData = cq.from(RawData.class);
        cq.select(cb.count(rawData));
        cq.where(cb.equal(rawData.get(RawData_.source), source));
        return em.createQuery(cq).getSingleResult();
    }

    /**
     * Deletes all the RawData records that are available for a given source.
     *
     * @param source the source for the raw data
     */
    public void deleteBySource(Source source) {
        for(RawData record : getBySource(source)) {
            delete(record);
        }
    }

    @Override
    public void delete(RawData entity) {
        EntityManager em = getEntityManager();
        Clump clump = entity.getClump();
        if(clump != null) {
            clump.removeRawDataRecord(entity);
            if(clump.getRawData().isEmpty()) {
                em.remove(clump);
            }
        }
        em.remove(entity);
    }
}

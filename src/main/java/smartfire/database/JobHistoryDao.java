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
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import smartfire.queue.JobState;

public class JobHistoryDao extends AbstractDao<JobHistory, Integer> {
    public JobHistoryDao(DatabaseConnection conn) {
        super(JobHistory.class, conn);
    }

    /**
     * Gets a list of all JobHistory records.
     *
     * @return a list of all JobHistory records with most recent first.
     */
    @Override
    public List<JobHistory> getAll() {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<JobHistory> q = cb.createQuery(JobHistory.class);
        Root<JobHistory> from = q.from(JobHistory.class);
        q.orderBy(cb.desc(from.get("endDate")));
        TypedQuery<JobHistory> typedQuery = getEntityManager().createQuery(q.select(from));
        return typedQuery.getResultList();
    }
    
    public List<JobHistory> getRecentlyFinished() {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<JobHistory> cq = cb.createQuery(JobHistory.class);
        Root<JobHistory> jobHistory = cq.from(JobHistory.class);
        
        Date cutoffTime = new DateTime(DateTimeZone.UTC).minusMinutes(5).toDate();
        
        return em.createQuery(cq
                    .select(jobHistory)
                    .where(cb.greaterThanOrEqualTo(jobHistory.get(JobHistory_.endDate), cutoffTime))
                    .orderBy(cb.desc(jobHistory.get(JobHistory_.endDate)))
                ).getResultList();
    }

    public JobStats getJobStats(DateTime startDate, DateTime endDate) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<JobHistory> jobHistory = cq.from(JobHistory.class);
        Date start = startDate.withZone(DateTimeZone.UTC).toDate();
        Date end = endDate.withZone(DateTimeZone.UTC).toDate();

        Predicate predicate = cb.and(
                cb.greaterThanOrEqualTo(jobHistory.get(JobHistory_.endDate), start),
                cb.lessThanOrEqualTo(jobHistory.get(JobHistory_.endDate), end)
                );

        int totalJobs = em.createQuery(cq
                .select(cb.count(jobHistory))
                .where(predicate)
                ).getSingleResult().intValue();

        int successfulJobs = em.createQuery(cq
                .select(cb.count(jobHistory))
                .where(
                    predicate,
                    cb.equal(jobHistory.get(JobHistory_.status), JobState.SUCCESS.toString())
                )).getSingleResult().intValue();

        int failedJobs = em.createQuery(cq
                .select(cb.count(jobHistory))
                .where(
                    predicate,
                    cb.equal(jobHistory.get(JobHistory_.status), JobState.FAILURE.toString())
                )).getSingleResult().intValue();


        return new JobStats(totalJobs, successfulJobs, failedJobs);
    }
}

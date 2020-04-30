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

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class ScheduledFetchDao extends AbstractDao<ScheduledFetch, Integer> {
    public ScheduledFetchDao(DatabaseConnection conn) {
        super(ScheduledFetch.class, conn);
    }

    /**
     * Gets all ScheduledFetches that are automatic fetches
     */
    public List<ScheduledFetch> getAllAutomaticFetches() {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ScheduledFetch> c = cb.createQuery(ScheduledFetch.class);
        Root<ScheduledFetch> r = c.from(ScheduledFetch.class);
        TypedQuery<ScheduledFetch> q = em.createQuery(
                c.where(cb.and(cb.isNotNull(r.get(ScheduledFetch_.schedule)))));
        return q.getResultList();
    }
}

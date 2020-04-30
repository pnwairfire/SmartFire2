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
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class SummaryDataLayerDao extends AbstractDao<SummaryDataLayer, Integer> {
    public SummaryDataLayerDao(DatabaseConnection conn) {
        super(SummaryDataLayer.class, conn);
    }

    /**
     * Gets a Summary Data Layer that is identified by the slugified name.
     *
     * @param nameSlug the nameSlug of interest
     * @return a Data Layer that is identified with the nameSlug
     */
    public SummaryDataLayer getByNameSlug(String nameSlug) {
        if(nameSlug == null) {
            return null;
        }
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SummaryDataLayer> c = cb.createQuery(SummaryDataLayer.class);
        Root<SummaryDataLayer> r = c.from(SummaryDataLayer.class);
        TypedQuery<SummaryDataLayer> q = em.createQuery(
                c.where(
                cb.equal(r.get(SummaryDataLayer_.nameSlug), nameSlug)));

        try {
            return q.getSingleResult();
        } catch(Exception e) {
            return null;
        }
    }

    @Override
    public void delete(SummaryDataLayer layer) {
        for(ReconciliationStream stream : Lists.newArrayList(layer.getReconciliationStreams())) {
            stream.removeSummaryDataLayer(layer);
            layer.removeStream(stream);
        }
        super.delete(layer);
    }
}

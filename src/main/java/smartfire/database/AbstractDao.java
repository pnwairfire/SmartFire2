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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import org.hibernate.Session;
import org.hibernate.ejb.HibernateEntityManager;

public abstract class AbstractDao<T extends SfEntity<K>, K> {
    protected final Class<T> klass;
    protected final DatabaseConnection conn;

    public AbstractDao(Class<T> klass, DatabaseConnection conn) {
        this.klass = klass;
        this.conn = conn;
    }

    protected EntityManager getEntityManager() {
        return conn.getEntityManager();
    }

    protected Session getSession() {
        HibernateEntityManager hem = conn.getEntityManager().unwrap(HibernateEntityManager.class);
        return hem.getSession();
    }

    /**
     * Casts a list of arbitrary objects to a list of T.
     *
     * @param list the input list
     * @return a list with each member cast to the type of T
     */
    protected List<T> castList(List<?> list) {
        List<T> result = new ArrayList<T>(list.size());
        for(Object obj : list) {
            result.add(this.klass.cast(obj));
        }
        return Collections.checkedList(result, this.klass);
    }

    /**
     * Queries this entity using a JPA 1.0 query string.
     *
     * <p>Note: Whenever possible, we should instead use JPA 2.0 type-safe queries.
     * See http://www.ibm.com/developerworks/java/library/j-typesafejpa/
     * for more details.
     *
     * @param formatString a query string in JPQL format, optionally
     *                     containing formatting escapes as per the
     *                     {@link String#format(java.lang.String, java.lang.Object[]) String.format()}
     *                     method.
     * @param args optional arguments to fill in the formatting escapes
     * @return a list of matching entities, cast to the type of T
     */
    protected List<T> findEntities(String formatString, Object... args) {
        String ejbqlString = String.format(formatString, args);
        Query query = getEntityManager().createQuery(ejbqlString);
        List<?> list = query.getResultList();
        List<T> elist = castList(list);
        return elist;
    }

    /**
     * Gets a single entity, given its ID.
     *
     * @param id the primary key ID of the desired entity
     * @return the requested entity, or null if the entity does not exist
     */
    public T getById(K id) {
        return getEntityManager().find(this.klass, id);
    }

    /**
     * Gets a list of all entities in the database for this type.
     *
     * @return a list of all entities
     */
    public List<T> getAll() {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(klass);
        TypedQuery<T> typedQuery = getEntityManager().createQuery(q.select(q.from(klass)));
        return typedQuery.getResultList();
    }

    /**
     * Saves a new entity to the database by making it a persisted entity.
     *
     * If the entity is already persisted, this method does nothing.
     *
     * @param entity a newly created entity object
     */
    public void save(T entity) {
        K id = entity.getId();
        if(id == null) {
            getEntityManager().persist(entity);
        }
    }
    
    /**
     * Checks if an entity object is saved to the database.  (Or, more 
     * specifically, if it will be saved to the database at the next commit.)
     * 
     * @param entity the entity object in question
     * @return true if the object is being managed by the EntityManager
     */
    public boolean isSaved(T entity) {
        EntityManager em = getEntityManager();
        return em.contains(entity);
    }

    /**
     * Delete an existing entity from the database.
     *
     * @param entity an entity object
     */
    public void delete(T entity) {
        getEntityManager().remove(entity);
    }
}

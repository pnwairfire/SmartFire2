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
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.MultiPolygon;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.SmartfireException;
import smartfire.gis.Union;

public class EventDao extends AbstractDao<Event, Long> {
    private static final Logger log = LoggerFactory.getLogger(EventDao.class);
    private static final double SIMPLIFY_RESOLUTION = 10.0;
    
    public EventDao(DatabaseConnection conn) {
        super(Event.class, conn);
    }

    /**
     * Gets all the Event records that are available for a given 
     * ReconciliationStream and date range.
     *
     * @param stream the stream of Events of interest
     * @param start the start date of interest
     * @param end the end date of interest
     * @return a list of Event records
     */
    public List<Event> getByDate(ReconciliationStream stream, ReadableDateTime start, ReadableDateTime end) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> event = cq.from(Event.class);
        Date startDate = start.toDateTime().withZone(DateTimeZone.UTC).toDate();
        Date endDate = end.toDateTime().withZone(DateTimeZone.UTC).toDate();
        return em.createQuery(cq
                .select(event)
                .where(
                    // event.reconciliationStream == stream
                    cb.equal(event.get(Event_.reconciliationStream), stream),

                    // and event.startDate <= argument.endDate
                    cb.lessThanOrEqualTo(event.get(Event_.startDate), endDate),

                    // and event.endDate >= argument.startDate
                    cb.greaterThanOrEqualTo(event.get(Event_.endDate), startDate)
                
                )).getResultList();
    }
    
    /**
     * Get the largest fire events currently being tracked by the given 
     * ReconciliationStream. This version filters all events that ended more 
     * than 7 days ago, and any events with a detection probability less than
     * 75 percent.  See also {@link EventDao#getTopEvents(smartfire.database.ReconciliationStream, int, int, double)}.
     * 
     * @param stream the ReconciliationStream of interest
     * @param numFires the number of fires to return
     * @return a List of Event objects representing the top events
     */
    public List<Event> getTopEvents(ReconciliationStream stream, int numFires) {
        return getTopEvents(stream, numFires, 2, 0.75);
    }
    
    /**
     * Get the N largest likely fire events currently being tracked by the 
     * given ReconciliationStream.  This overload allows you to customize 
     * what you mean by "N", "likely", and "currently".
     * 
     * @param stream the ReconciliationStream of interest
     * @param numFires the number of fires to return
     * @param numBackwardDays the maximum number of days after a fire has 
     *                        ended that it will drop off the returned list
     * @param minProbability the minimum acceptable detection probability;
     *                       events with a lower probability are not returned
     * @return a List of Event objects representing the top events
     */
    public List<Event> getTopEvents(ReconciliationStream stream, int numFires, int numBackwardDays, double minProbability) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> event = cq.from(Event.class);
        Date cutoffDate = new DateTime(DateTimeZone.UTC).minusDays(numBackwardDays).toDate();
        return em.createQuery(cq
                    .select(event)
                    .where(
                        cb.greaterThanOrEqualTo(event.get(Event_.probability), minProbability),
                        cb.greaterThanOrEqualTo(event.get(Event_.endDate), cutoffDate),
                        cb.equal(event.get(Event_.reconciliationStream), stream)
                    )
                    .orderBy(cb.desc(event.get(Event_.totalArea)))
                )
                .setMaxResults(numFires)
                .getResultList();
    }

    /**
     * Gets an Event, identified by its unique ID.
     *
     * @param uid the unique id of an Event
     * @return the Event that is identified by the unique ID, or null if no
     *         such Event exists
     */
    public Event getByUniqueID(String uid) {
        if(uid == null) {
            return null;
        }
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Event> c = cb.createQuery(Event.class);
        Root<Event> r = c.from(Event.class);
        TypedQuery<Event> q = em.createQuery(
                c.where(
                cb.equal(r.get(Event_.uniqueId), uid)));
        try {
            return q.getSingleResult();
        } catch(NoResultException e) {
            return null;
        }
    }

    /**
     * Merge a collection of Event objects into a single Event.
     *
     * <p>This method always creates a new Fire object. The result events's
     * fires are all the fires associated with all the input events.
     *
     * <p>The input events are marked for removal from the database when the
     * transaction is committed.
     *
     * @param events a collection of input events
     * @return a single event object representing the merger of all the input events
     */
    public Event merge(Iterable<Event> events) {
        Event result = new Event();

        Map<String, String> attrs = Maps.newHashMap();
        List<Fire> fires = Lists.newArrayList();
        MultiPolygon poly = null;

        Event largestEvent = null;
        DateTime startDate = null;
        DateTime endDate = null;
        int numEvents = 0;

        for(Event event : events) {
            // Ensure that all the Events come from the same ReconciliationStream
            if(result.getReconciliationStream() == null) {
                result.setReconciliationStream(event.getReconciliationStream());
            } else if(!result.getReconciliationStream().equals(event.getReconciliationStream())) {
                throw new SmartfireException("Incompatible streams for merged Event: "
                        + result.getReconciliationStream().getName() + " and "
                        + event.getReconciliationStream().getName());
            }

            numEvents++;

            // Find the largest Event in the input set
            if(largestEvent == null || (event.getTotalArea() > largestEvent.getTotalArea())) {
                largestEvent = event;
            }

            // Find the min and max startDate and endDate
            if(startDate == null || (startDate.isAfter(event.getStartDateTime()))) {
                startDate = event.getStartDateTime();
            }
            if(endDate == null || (endDate.isAfter(event.getEndDateTime()))) {
                endDate = event.getEndDateTime();
            }

            // Union the shapes together
            if(poly == null) {
                poly = event.getShape();
            } else {
                poly = Union.toMultiPolygon(Union.union(poly, event.getShape()));
            }

            // Copy all the attributes from this event
            attrs.putAll(event);

            // Copy the Fires
            fires.addAll(event.getFires());

            // Schedule the input event for deletion
            this.delete(event);
        }

        if(numEvents == 0) {
            throw new IllegalArgumentException("Cannot merge an iterable containing zero events");
        }

        //result.setUniqueId(largestEvent.getUniqueId());
        result.setDisplayName(largestEvent.getDisplayName());
        result.setTotalArea(largestEvent.getTotalArea());
        result.setStartDate(startDate);
        result.setEndDate(endDate);
        result.setShape(poly);
        result.putAll(attrs);
        result.addFires(fires);

        // Copy EventDays from largestEvent
        List<EventDay> oldEventDays = Lists.newArrayList(largestEvent.getEventDays());
        List<EventDay> eventDays = Lists.newArrayListWithExpectedSize(oldEventDays.size());
        for(EventDay oldEventDay : oldEventDays) {
            EventDay eventDay = new EventDay();
            eventDay.setDailyArea(oldEventDay.getDailyArea());
            eventDay.setEventDate(oldEventDay.getEventDate());
            eventDays.add(eventDay);
        }
        result.setEventDays(eventDays);

        return result;
    }  
    
    @Override
    public void delete(Event event) {
        EntityManager em = getEntityManager();
        for(Fire fire : Lists.newArrayList(event.getFires())) {
            fire.removeEvent(event);
            event.removeFire(fire);
        }
        for(EventDay eventDay : Lists.newArrayList(event.getEventDays())) {
            eventDay.setEvent(null);
            em.remove(eventDay);
        }
        event.clearEventDays();
        super.delete(event);
    }
}

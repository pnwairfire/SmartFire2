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

import java.util.List;
import org.joda.time.DateTime;
import smartfire.database.Event;
import smartfire.database.EventDao;
import smartfire.database.ReconciliationStream;

/**
 * Represents a collection of Events, backed by an EventDao, with enhanced query
 * capabilities.  Subsequent queries are guaranteed to retrieve the same Event
 * instances, so that they can be mutated.
 */
public class QueryableEventSet extends AbstractQueryableSet<Event, Long, EventDao> {
    private final ReconciliationStream stream;

    public QueryableEventSet(EventDao eventDao, ReconciliationStream stream) {
        super(eventDao);
        this.stream = stream;
    }

    @Override
    protected List<Event> fetchByDate(EventDao dao, DateTime startDate, DateTime endDate) {
        return dao.getByDate(stream, startDate, endDate);
    }

    @Override
    protected Event mergeInternal(EventDao dao, List<Event> toMerge) {
        return dao.merge(toMerge);
    }
}

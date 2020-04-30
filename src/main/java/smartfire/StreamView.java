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
package smartfire;

import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import smartfire.database.DatabaseConnection;
import smartfire.database.Event;
import smartfire.database.ReconciliationStream;
import smartfire.database.Source;

public class StreamView extends DateIndexedView {
    private final ReconciliationStream stream;
    private final DatabaseConnection conn;

    public StreamView(Application app, ReconciliationStream stream) {
        super(app);
        this.conn = app.getAppSettings().getDatabaseConnection();
        this.stream = stream;
    }
    
    @Override
    protected Object getViewObjectByDate(DateTime startDate, DateTime endDate) {
        List<Event> events;
        String dateString;
        String urlString;
        if(endDate == null) {
            events = conn.getEvent().getByDate(stream, startDate, startDate.plusDays(1).minusMillis(1));
            dateString = startDate.toString("MMM d, yyyy");
            urlString = startDate.toString("yyyyMMdd") + "/";
        } else {
            events = conn.getEvent().getByDate(stream, startDate, endDate);
            dateString = startDate.toString("MMM d, yyyy") + " to " + endDate.toString("MMM d, yyyy");
            urlString = "range/?startDate=" + startDate.toString("yyyyMMdd") + "&endDate=" + endDate.toString("yyyyMMdd");
        }
        String individualLinkField = "unique_id";
        String individualLinkPrefix = "/events/";
        
        if(events == null) {
            return null;
        }
        
        return new ExportTableView<Event>(
                getApp(),
                Event.class,
                startDate,
                endDate,
                events,
                stream.getName() + " Fire Events",
                Arrays.asList("Streams", stream.getName(), dateString),
                Arrays.asList(
                    "/streams/",
                    "/streams/" + stream.getNameSlug() + "/",
                    "/streams/" + stream.getNameSlug() + "/" + urlString
                ),
                individualLinkField,
                individualLinkPrefix
                );
    }

    @Override
    protected DateTime getCurrentDate() {
        DateTime current = null;
        for(Source source : stream.getSources()) {
            if(current == null || current.isBefore(source.getLatestData())) {
                current = source.getLatestData();
            }
        }
        return current;
    }
    
    /*
     *  Support Methods
     */
    public ReconciliationStream getStream() {
        return stream;
    }
}

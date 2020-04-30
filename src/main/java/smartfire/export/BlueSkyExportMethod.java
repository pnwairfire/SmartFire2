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
package smartfire.export;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sti.justice.util.ZipUtil;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.StaplerRequest;
import smartfire.ApplicationSettings;
import smartfire.Config;
import smartfire.database.Clump;
import smartfire.database.Event;
import smartfire.database.EventSlice;
import smartfire.database.Fire;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.XYPoint;
import smartfire.util.Sorted;

/**
 * ExportMethod for exporting data to the BlueSky Framework.
 * 
 * Documented here: http://www.blueskyframework.org/framework/comma-separated-value-csv-files
 */
@MetaInfServices(ExportMethod.class)
public class BlueSkyExportMethod extends AbstractExportMethod<Event> implements ExportMethod {

    public BlueSkyExportMethod() {
        super("BlueSky-Zip", "blueskyzip", "/images/icons/blueskyfile-32x32.png", Event.class, "application/zip", ".zip");
    }
    
    public BlueSkyExportMethod(String displayName, String slugName, String iconPath, String contentType, String fileExtension) {
        super(displayName, slugName, iconPath, Event.class, contentType, fileExtension);
    }

    @Override
    protected void performExport(StaplerRequest request, OutputStream out, ApplicationSettings appSettings, String exportFileName,
            List<Event> events, DateTime startDate, DateTime endDate) throws IOException {
        File folder = getTempFolder();
        InputStream in = null;
        try {
            // Get files
            createBlueSkyFireEventsFile(folder, request.getRootPath(), events);
            createBlueSkyFireLocations(folder, request.getRootPath(), appSettings.getGeometryBuilder(), appSettings.getConfig(), events, startDate, endDate);

            File zipFile = ZipUtil.zipUpFolder(folder);

            in = new FileInputStream(zipFile);
            IOUtils.copy(in, out);
        } catch(Exception e) {
            log.error("Error exporting to bluesky format {}", e);
        } finally {
            if(in != null) {
                in.close();
            }
            if(out != null) {
                out.close();
            }

            try {
                log.debug("Deleting temporary folder {}", folder);
                FileUtils.deleteDirectory(folder);
            } catch(IOException e) {
                log.warn("Unable to delete temporary folder", e);
            }
        }
    }

    protected File createBlueSkyFireEventsFile(File folder, String contextPath, Iterable<Event> events) throws Exception {
        File fireEventsFile = new File(folder, "fire_events.csv");
        OutputStream stream = new FileOutputStream(fireEventsFile);
        Writer out = new OutputStreamWriter(stream);
        CSVWriter writer = new CSVWriter(out);

        // Add Headings
        List<String> headings = getFireEventsHeadings();
        String[] entriesOutput = new String[headings.size()];
        headings.toArray(entriesOutput);
        writer.writeNext(entriesOutput);

        String serverName = getServerName(contextPath);

        // Get stream name
        String streamName = events.iterator().next().getReconciliationStream().getName();
        
        // Add data attributes to CSV file
        for(Event event : events) {
            String[] outputValues = new String[headings.size()];
            
            String eventGUID = event.getUniqueId();

            outputValues[0] = "SF11E" + event.getId().toString();
            outputValues[1] = event.getDisplayName();
            outputValues[2] = eventGUID;
            outputValues[3] = streamName;
            outputValues[4] = serverName;
            
            // Build list of unique sources that make-up this event
            Set<String> sourceNames = Sets.newHashSet();
            for(Fire fire : event.getFires()) {
                for(Clump clump : fire.getClumps()) {
                    sourceNames.add(clump.getSource().getName());
                }
            }
            String sourceNamesSeperated = "";
            for(String name : sourceNames) {
                sourceNamesSeperated += name + ";";
            }
            
            outputValues[5] = sourceNamesSeperated;
            
            writer.writeNext(outputValues);
        }

        writer.close();
        out.close();

        return fireEventsFile;
    }

    protected File createBlueSkyFireLocations(File folder, String contextPath, GeometryBuilder geometryBuilder, Config config, Iterable<Event> events, DateTime startDate, DateTime endDate) throws Exception {
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMdd");
        DecimalFormat coordFormat = new DecimalFormat("#.###");

        File fireLocationsFile = new File(folder, "fire_locations.csv");
        OutputStream stream = new FileOutputStream(fireLocationsFile);
        Writer out = new OutputStreamWriter(stream);
        CSVWriter writer = new CSVWriter(out);

        // Add Headings
        List<String> headings = getFireLocationsHeadings();
        String[] entriesOutput = new String[headings.size()];
        headings.toArray(entriesOutput);
        writer.writeNext(entriesOutput);

        String serverName = getServerName(contextPath);
        
        // Get stream name
        String streamName = events.iterator().next().getReconciliationStream().getName();

        // Add data attributes to CSV file
        int i = 1;
        int size = Lists.newArrayList(events).size();
        for(Event event : events) {
            log.info("Processing event {} out of {}", i, size);
            i++;
            String eventId = "SF11E" + event.getId().toString();

            // Get event slice with greatest growth weight
            List<EventSlice> slices = event.getSlices();
            EventSlice slice = Sorted.by(slices, EventSlice.BY_GROWTH_WEIGHT_DESC).get(0);

            // Get event name and url
            String eventName = slice.getEvent().getDisplayName();
            String eventUrl = contextPath + "/events/" + slice.getEvent().getUniqueId();
            
            String eventGUID = event.getUniqueId();
            
            Set<LocalDate> dateSet = buildDateSet(startDate, endDate);

            // Build a list of fire locations
            FireLocationSet fireLocations = new FireLocationSet(event.getArea());
            for(Fire fire : slice.getFires()) {
                for(Clump clump : fire.getClumps()) {
                    Set<LocalDate> clumpDateSet = buildDateSet(clump.getStartDateTime(), clump.getEndDateTime());

                    String idPrefix = "SF11C" + clump.getId().toString();
                    double area = clump.getArea() / clumpDateSet.size();
                    XYPoint latLonPoint = geometryBuilder.buildLatLonFromPoint(clump.getExportPointX(), clump.getExportPointY());
                    String lat = coordFormat.format(latLonPoint.getY());
                    String lon = coordFormat.format(latLonPoint.getX());

                    int count = 0;
                    for(LocalDate date : clumpDateSet) {
                        // skip clump if not in date set for export
                        if(!dateSet.contains(date)) {
                            fireLocations.addArea(area);
                            continue;
                        }

                        String id = idPrefix + Integer.toString(clumpDateSet.hashCode()) + Integer.toString(count);
                        fireLocations.add(new FireLocation(id, area, date, lat, lon));

                        count++;
                    }
                }
            }
            
            for(FireLocation fireLocation : fireLocations) {

                String[] outputValues = new String[headings.size()];

                outputValues[0] = fireLocation.getId();
                outputValues[1] = eventId;

                outputValues[2] = eventName;
                outputValues[3] = eventUrl;

                outputValues[4] = fireLocation.getLatitude();
                outputValues[5] = fireLocation.getLongitude();

                outputValues[6] = fireLocation.getDate().toString(format);

                outputValues[7] = Double.toString(fireLocations.scaleLocationArea(fireLocation.getArea()));

                // Set fire type
                outputValues[8] = event.getFireType().toUpperCase();

                // Set server name
                outputValues[9] = serverName;

                // Set stream name
                outputValues[10] = streamName;

                // Set event GUID
                outputValues[11] = eventGUID;

                writer.writeNext(outputValues);
            }
        }

        writer.close();
        out.close();

        return fireLocationsFile;
    }

    protected List<String> getFireEventsHeadings() {
        List<String> headings = Lists.newArrayList();
        headings.add("id");
        headings.add("event_name");
        headings.add("event_guid");
        headings.add("stream_name");
        headings.add("server");
        headings.add("sources");
        return headings;
    }

    protected List<String> getFireLocationsHeadings() {
        List<String> headings = Lists.newArrayList();
        headings.add("id");
        headings.add("event_id");
        headings.add("event_name");
        headings.add("event_url");
        headings.add("latitude");
        headings.add("longitude");
        headings.add("date_time");
        headings.add("area");
        headings.add("type");
        headings.add("server");
        headings.add("stream_name");
        headings.add("event_guid");
        return headings;
    }
    
    public static String getServerName(String contextPath) throws MalformedURLException {
        URL contextURL = new URL(contextPath);
        String server = contextURL.getHost();
        
        int serverPort = contextURL.getPort();
        if(serverPort > -1) { // append port number if one exists
            server += ":" + Integer.toString(serverPort);
        }
        
        return server;
    }
    
    /**
     * Build a set of all the dates in the range to ensure we only get data for these select dates
     * 
     * @param startDate
     * @param endDate
     * @param format
     * @return a set of Strings representing all the dates within the range inclusive.
     */
    protected static Set<LocalDate> buildDateSet(DateTime startDate, DateTime endDate) {
        Set<LocalDate> dateSet = Sets.newHashSet();
        dateSet.add(startDate.toDateMidnight().toLocalDate());
        if(endDate != null) {
            DateTime start = startDate.toDateMidnight().toDateTime();
            int totalDays = Days.daysBetween(startDate.toDateMidnight(), endDate.toDateMidnight()).getDays();
            if (totalDays == 0) {
                return dateSet;
            }
            for(int days = 1; days <= totalDays; days++) {
                DateTime nextDate = start.plusDays(days);
                dateSet.add(nextDate.toLocalDate());
            }
        }
        return dateSet;
    }
}

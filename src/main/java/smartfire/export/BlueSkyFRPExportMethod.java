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
import java.io.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.MetaInfServices;
import smartfire.Config;
import smartfire.database.*;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.XYPoint;
import smartfire.util.Sorted;

/**
 * ExportMethod for exporting FRP data to the BlueSky Framework.  FRP values are acquired from raw data.
 */
@MetaInfServices(ExportMethod.class)
public class BlueSkyFRPExportMethod extends BlueSkyTextExportMethod implements ExportMethod {

    public BlueSkyFRPExportMethod() {
        super("BlueSky-FRP", "blueskyfrp", "/images/icons/blueskyfile-32x32.png", "text/csv", ".csv");
    }

    @Override
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
            String eventId = "SF2FRP" + event.getId().toString();

            // Get event slice with greatest growth weight (this will also dictate FRP)
            List<EventSlice> slices = event.getSlices();
            EventSlice slice = Sorted.by(slices, EventSlice.BY_GROWTH_WEIGHT_DESC).get(0);

            // Get event name and url
            String eventName = slice.getEvent().getDisplayName();
            String eventUrl = contextPath + "/events/" + slice.getEvent().getUniqueId();
            
            String eventGUID = event.getUniqueId();

            // Get summary data attributes
            String eventCountry = slice.getEvent().get("CNTRY");
            String eventState = slice.getEvent().get("STATE");
            String eventFIPS = slice.getEvent().get("FIPS");
            
            Set<LocalDate> dateSet = BlueSkyExportMethod.buildDateSet(startDate, endDate);

            // Build a list of fire locations
            FireLocationSet fireLocations = new FireLocationSet(event.getArea());
            for(Fire fire : slice.getFires()) {
                for(Clump clump : fire.getClumps()) {
                    Set<LocalDate> clumpDateSet = BlueSkyExportMethod.buildDateSet(clump.getStartDateTime(), clump.getEndDateTime());

                    String idPrefix = "SF2FRP" + clump.getId().toString();
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
                        FireLocation fireLocation = new FireLocation(id, area, date, lat, lon);
                        
                        // Calculate and set total FRP
                        String totalFRP = sumRawDataValues(clump, "frp");
                        fireLocation.setAttribute("frp", totalFRP);
                        fireLocations.add(fireLocation);
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

                // Use default date but add the tz offset if it exists.
                if(fireLocation.hasAttribute("Time Zone")) {
                    DateTimeZone timeZone = (DateTimeZone) fireLocation.getAttribute("Time Zone");
                    outputValues[6] = fireLocation.getDate().toDateTimeAtStartOfDay(timeZone).toString();
                } else {
                    outputValues[6] = fireLocation.getDate().toString(format);
                }

                outputValues[7] = Double.toString(fireLocations.scaleLocationArea(fireLocation.getArea()));

                // Set fire type
                outputValues[8] = event.getFireType().toUpperCase();
                
                // Set FRP 
                outputValues[9] = (String) fireLocation.getAttribute("frp");

                // add event summary data attributes
                outputValues[10] = eventCountry;
                outputValues[11] = eventState;
                outputValues[12] = eventFIPS;

                // Set server name
                outputValues[13] = serverName;

                // Set stream name
                outputValues[14] = streamName;

                // Set event GUID
                outputValues[15] = eventGUID;

                writer.writeNext(outputValues);
            }
        }

        writer.close();
        out.close();

        return fireLocationsFile;
    }
    
    private static String sumRawDataValues(Clump clump, String key) {
        double sum = 0.0;
        for(RawData data : clump.getRawData()) {
            // Get the value for the key or assume it to be 0.0
            if(data.containsKey(key)) {
                sum += Double.parseDouble(data.get(key));
            }
        }
        return Double.toString(sum);
    }

    @Override
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
        headings.add("frp");
        headings.add("country");
        headings.add("state");
        headings.add("fips");
        headings.add("server");
        headings.add("stream_name");
        headings.add("event_guid");
        return headings;
    }
}

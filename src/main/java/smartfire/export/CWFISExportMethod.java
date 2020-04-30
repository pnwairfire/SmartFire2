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
import java.util.Arrays;
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
 * ExportMethod for exporting data to the BlueSky Framework.
 */
@MetaInfServices(ExportMethod.class)
public class CWFISExportMethod extends BlueSkyTextExportMethod implements ExportMethod {

    public CWFISExportMethod() {
        super("BlueSky-CWFIS", "blueskycwfis", "/images/icons/blueskyfile-32x32.png", "text/csv", ".csv");
    }

    @Override
    protected File createBlueSkyFireLocations(File folder, String contextPath, GeometryBuilder geometryBuilder, Config config, Iterable<Event> events, DateTime startDate, DateTime endDate) throws
            Exception {
        DateTimeZone dtz = config.getDateTimeZone();

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

            // Get summary data attributes
            String eventCountry = slice.getEvent().get("country");
            String eventState = slice.getEvent().get("state");
            String eventFIPS = slice.getEvent().get("fips");
            String eventProvName = slice.getEvent().get("Prov_Name");
            
            Set<LocalDate> dateSet = BlueSkyExportMethod.buildDateSet(startDate, endDate);

            // Build a list of fire locations
            FireLocationSet fireLocations = new FireLocationSet(event.getArea());
            for(Fire fire : slice.getFires()) {
                for(Clump clump : fire.getClumps()) {
                    Set<LocalDate> clumpDateSet = BlueSkyExportMethod.buildDateSet(clump.getStartDateTime(), clump.getEndDateTime());

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
                        FireLocation fireLocation = new FireLocation(id, area, date, lat, lon);
                        
                        // Set fire location time zone, if provided
                        List<RawData> rawDataRecords = clump.getRawData();
                        if(!rawDataRecords.isEmpty()) {
                            RawData data = clump.getRawData().get(0);
                            if(data.containsKey("Time Zone")) {
                                DateTimeZone timeZone = getDateTimeZone(data.get("Time Zone"));
                                fireLocation.setAttribute("Time Zone", timeZone);
                            }
                        }
                        
                        // Calculate and set average flaming consumption
                        String avgFlamingConsumption = averageRawDataValues(clump, "consumption_flaming");
                        fireLocation.setAttribute("consumption_flaming", avgFlamingConsumption);
                        
                        // Calculate and set average smoldering consumption
                        String avgSmolderingConsumption = averageRawDataValues(clump, "consumption_smoldering");
                        fireLocation.setAttribute("consumption_smoldering", avgSmolderingConsumption);
                        
                        // Calculate and set average residual consumption
                        String avgResidualConsumption = averageRawDataValues(clump, "consumption_residual");
                        fireLocation.setAttribute("consumption_residual", avgResidualConsumption);
                        
                        // Calculate and set average duff consumption
                        String avgDuffConsumption = averageRawDataValues(clump, "consumption_duff");
                        fireLocation.setAttribute("consumption_duff", avgDuffConsumption);
                        
                        // Calculate and set hourly area values
                        String avgHourlyArea = averageHourlyValues(clump, "area");
                        fireLocation.setAttribute("hourly_area", avgHourlyArea);
                        
                        // Calculate and set hourly plume energy values
                        String avgHourlyEnergy = averageHourlyValues(clump, "heat");
                        fireLocation.setAttribute("hourly_heat", avgHourlyEnergy);
                        
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

                outputValues[9] = (String) fireLocation.getAttribute("consumption_flaming");

                outputValues[10] = (String) fireLocation.getAttribute("consumption_smoldering");

                outputValues[11] = (String) fireLocation.getAttribute("consumption_residual");

                outputValues[12] = (String) fireLocation.getAttribute("consumption_duff");

                // add event summary data attributes
                outputValues[13] = eventCountry;
                outputValues[14] = eventState;
                outputValues[15] = eventFIPS;
                outputValues[16] = eventProvName;

                // Set server name
                outputValues[17] = serverName;

                // Set stream name
                outputValues[18] = streamName;

                // Set event GUID
                outputValues[19] = eventGUID;
                
                outputValues[20] = (String) fireLocation.getAttribute("hourly_area");
                
                outputValues[21] = (String) fireLocation.getAttribute("hourly_heat");

                writer.writeNext(outputValues);
            }
        }

        writer.close();
        out.close();

        return fireLocationsFile;
    }
    
    private static DateTimeZone getDateTimeZone(String tzOffset) {
        // Determine offset based on time zone string
        int hourOffset;
        int minuteOffset = 0;
        if(tzOffset.contains(".")) { // handle timezones with a half-hour offset, such as Newfoundland
            String[] tzoneParts = tzOffset.split("\\.");
            hourOffset = Integer.parseInt(tzoneParts[0]);
            minuteOffset = Integer.parseInt(tzoneParts[1]) * 6; // convert to minutes
        } else {
            hourOffset = Integer.parseInt(tzOffset);
        }
        
        return DateTimeZone.forOffsetHoursMinutes(hourOffset, minuteOffset);
    }

    private static String averageRawDataValues(Clump clump, String key) {
        double average = 0.0;
        for(RawData data : clump.getRawData()) {
            // Get the value for the key or assume it to be 0.0
            if(data.containsKey(key)) {
                average += Double.parseDouble(data.get(key));
            }
        }
        average /= clump.getRawData().size();
        return Double.toString(average);
    }
    
    private static String averageHourlyValues(Clump clump, String key) {
        double[] averages = null;
        for(RawData data : clump.getRawData()) {
            if(data.containsKey(key)) {
                // Retrieve string value from raw data.  Skip if no data found.
                String hourlyValuesStr = (String) data.get(key);
                if (hourlyValuesStr == null || hourlyValuesStr.isEmpty()) {
                    continue;
                }
                
                // Break down the string into individual pieces
                String[] hourlyValues = hourlyValuesStr.split(";");

                // Initialize each hourly average value to 0
                if (averages == null) {
                    averages = new double[hourlyValues.length];
                    Arrays.fill(averages, 0);
                }
                
                // Skip if we encounter an unexpected number of hourly values
                if (averages.length != hourlyValues.length) {
                    continue;
                }
                
                // Sum up each hourly value
                int i = 0;
                for (String hourlyValue : hourlyValues) {
                    double average = Double.parseDouble(hourlyValue);
                    if (average > 0) {
                        averages[i] += average;
                    }
                    i++;
                }
            }
        }
        
        // Return empty string if we don't have hourly average values
        if (averages == null) {
            return "";
        }
        
        // Build a ';' delimited string of the hourly average values
        String averagesStr = "";
        for (double average : averages) {
            average /= clump.getRawData().size(); // calculate average
            averagesStr += String.valueOf(average) + ";";
        }
        averagesStr = averagesStr.substring(0, averagesStr.length()-1); // remove trailing ';'

        return averagesStr;
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
        headings.add("consumption_flaming");
        headings.add("consumption_smoldering");
        headings.add("consumption_residual");
        headings.add("consumption_duff");
        headings.add("country");
        headings.add("state");
        headings.add("fips");
        headings.add("ProvinceName");
        headings.add("server");
        headings.add("stream_name");
        headings.add("event_guid");
        headings.add("hourly_area");
        headings.add("hourly_heat");
        return headings;
    }
}

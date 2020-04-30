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

import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.mrbean.MrBeanModule;
import org.geotools.geojson.geom.GeometryJSON;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.StaplerRequest;
import smartfire.ApplicationSettings;
import smartfire.database.Clump;
import smartfire.database.Event;
import smartfire.database.EventSlice;
import smartfire.database.Fire;
import smartfire.database.FireDay;
import smartfire.database.RawData;
import smartfire.gis.CoordinateTransformer;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.XYPoint;
import smartfire.util.Sorted;

/**
 * ExportMethod for exporting summary data to the BlueSky Framework.
 */
@MetaInfServices(ExportMethod.class)
public class SummaryDataExportMethod extends AbstractExportMethod<Event> implements ExportMethod {
    private static final double ACRES_PER_SQ_METER = 0.000247105381;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss'Z");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final String IRWIN_GUID_FIELD = "IrwinID";
    private final ObjectMapper mapper;
    private final byte[] newline = "\n".getBytes();
    private boolean detailedMode = false;
    
    public SummaryDataExportMethod() {
        super("SummaryDataJson", "summarydatajson", "/images/icons/blueskyfile-32x32.png", Event.class, "application/json", ".json");
        
        mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.getJsonFactory().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        mapper.registerModule(new MrBeanModule());
    }
    
    private double roundVal(double val, int precision) {
        double factor = Math.pow(10, precision);
        return Math.round(val * factor) / factor;
    }
    
    private Map<String, Double> setCentroid(Event event, ApplicationSettings appSettings) {
        GeometryBuilder geometryBuilder = appSettings.getGeometryBuilder();
        XYPoint lonlat = geometryBuilder.buildLatLonFromPoint(event.getExportPointX(), event.getExportPointY());
        Map<String, Double> centroid = new LinkedHashMap<String, Double>();
        centroid.put("latitude", roundVal(lonlat.getY(), 5));
        centroid.put("longitude", roundVal(lonlat.getX(), 5));
        return centroid;
    }
    
    private Map<String, Object> setSourceSummary(Event event) {
        Map<String, Object> sourceSummaryMap = new LinkedHashMap<String, Object>();
        for (Fire fire : event.getFires()) {
            String sourceName = fire.getSource().getName();

            // Get source data if it already exists
            Map<String, Integer> sourceInfoMap = new LinkedHashMap<String, Integer>();
            if (sourceSummaryMap.containsKey(sourceName)) {
                sourceInfoMap = (Map) sourceSummaryMap.get(sourceName);
            }

            // Increment the current detects count by the number of raw data records associated with the fire
            int detects = sourceInfoMap.containsKey("detects") ? sourceInfoMap.get("detects") : 0;
            for (Clump clump : fire.getClumps()) {
                detects += clump.getRawData().size();
            }
            sourceInfoMap.put("detects", detects);
            sourceSummaryMap.put(sourceName, sourceInfoMap);
        }
        return sourceSummaryMap;
    }
    
    private Map<String, Object> setSourceData(Event event, ApplicationSettings appSettings) {
        GeometryBuilder geometryBuilder = appSettings.getGeometryBuilder();
        Map<String, Object> sourceDataMap = new LinkedHashMap<String, Object>();
        for (Fire fire : event.getFires()) {
            String sourceName = fire.getSource().getName();

            // Get source data if it already exists
            Map<String, Object> sourceInfoMap = new LinkedHashMap<String, Object>();
            if (sourceDataMap.containsKey(sourceName)) {
                sourceInfoMap = (Map) sourceDataMap.get(sourceName);
            }

            for (Clump clump : fire.getClumps()) {
                for (RawData rawData : clump.getRawData()) {
                    String startDateStr = rawData.getStartDateTime().toDateMidnight().toString(DATE_FORMAT);

                    // Get source date data if it already exists
                    Map<String, Object> sourceDateDataMap = new LinkedHashMap<String, Object>();
                    if (sourceInfoMap.containsKey(startDateStr)) {
                        sourceDateDataMap = (Map) sourceInfoMap.get(startDateStr);
                    }

                    // Increment the current day's detect count for each raw data record encountered sharing the same date
                    int detects = sourceDateDataMap.containsKey("detects") ? (Integer) sourceDateDataMap.get("detects") + 1 : 1;
                    sourceDateDataMap.put("detects", detects);

                    List<List<Double>> locations = new ArrayList<List<Double>>();
                    if (sourceDateDataMap.containsKey("locations")) {
                        locations = (List) sourceDateDataMap.get("locations");
                    }

                    XYPoint locationLonlat = geometryBuilder.buildLatLonFromPoint(rawData.getExportPointX(), rawData.getExportPointY());
                    List<Double> locationCentroid = new ArrayList<Double>(2);
                    locationCentroid.add(roundVal(locationLonlat.getY(), 5)); // Latitude
                    locationCentroid.add(roundVal(locationLonlat.getX(), 5)); // Longitude
                    locations.add(locationCentroid);
                    sourceDateDataMap.put("locations", locations);
                    sourceInfoMap.put(startDateStr, sourceDateDataMap);
                }
            }
            sourceDataMap.put(sourceName, sourceInfoMap);
        }
        return sourceDataMap;
    }
    
    private Map<String, Object> setFireData(Event event, ApplicationSettings appSettings) throws IOException {
        CoordinateTransformer transformer = appSettings.getGeometryBuilder().newLonLatOutputTransformer();
        GeometryJSON geojson = new GeometryJSON();
        Map<String, Object> fireDataMap = new LinkedHashMap<String, Object>();
        List<EventSlice> slices = event.getSlices();
        // Get fires from the source with the greatest size weight

        EventSlice sizeWeightedSlice = Sorted.by(slices, EventSlice.BY_SIZE_WEIGHT_DESC).get(0);
        double fireSize = 0;
        for(Fire fire : sizeWeightedSlice.getFires()) {
            fireSize += fire.getArea();
        }
        fireDataMap.put("size", roundVal(fireSize * ACRES_PER_SQ_METER, 2));

        fireDataMap.put("sizeUnit", "acres");
        fireDataMap.put("sizeSource", sizeWeightedSlice.getSource().getName());
        fireDataMap.put("dateTime", event.getEndDateTime().toString(DATE_TIME_FORMAT));

        // If requested, get geojson formatted event shape data
        if (detailedMode) {
            Geometry geometry = transformer.transform(event.getShape()); // Transforms coordinates into long/lat (x, y) points
            String shapeStr = geojson.toString(geometry); // Convert geometry to geojson string
            JsonNode shape = mapper.readTree(shapeStr); // Convert geojson string to json node tree
            fireDataMap.put("perimeter", shape);
        }

        Map<String, Object> fireDataSourcesMap = new LinkedHashMap<String, Object>();
        for (Fire fire : event.getFires()) {
            String sourceName = fire.getSource().getName();

            // Get source data if it already exists
            Map<String, Object> sourceInfo = new LinkedHashMap<String, Object>();
            if (fireDataSourcesMap.containsKey(sourceName)) {
                sourceInfo = (Map) fireDataSourcesMap.get(sourceName);
            }

            DateTime finalTime = fire.getEndDateTime();
            if (sourceInfo.containsKey("finalTime")) {
                DateTime sourceFinalTime = DATE_TIME_FORMAT.parseDateTime((String) sourceInfo.get("finalTime"));
                if (finalTime.isBefore(sourceFinalTime)) {
                    finalTime = sourceFinalTime;
                }
            }
            sourceInfo.put("finalTime", finalTime.toString(DATE_TIME_FORMAT));

            double finalSize = roundVal(fire.getArea() * ACRES_PER_SQ_METER, 2);
            if (sourceInfo.containsKey("finalSize")) {
                finalSize += (Double) sourceInfo.get("finalSize");
            }
            sourceInfo.put("finalSize", finalSize);
            sourceInfo.put("finalSizeUnit", "acres");

            fireDataSourcesMap.put(sourceName, sourceInfo);
        }
        fireDataMap.put("sources", fireDataSourcesMap);
        return fireDataMap;
    }
    
    private Map<String, Object> setFireGrowth(Event event) {
        Map<String, Object> growthMap = new LinkedHashMap<String, Object>();
        
        // Get event slice with greatest growth weight (this will also dictate FRP)
        List<EventSlice> slices = event.getSlices();
        EventSlice slice = Sorted.by(slices, EventSlice.BY_GROWTH_WEIGHT_DESC).get(0);
        growthMap.put("source", slice.getSource().getName());
        
        // Map sorted by date strings ascending
        SortedMap<String, Object> growthDatesMap = new TreeMap<String, Object>(
            new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    DateTime d1 = DATE_FORMAT.parseDateTime(o1);
                    DateTime d2 = DATE_FORMAT.parseDateTime(o2);
                    return d1.compareTo(d2);
                }
            }
        );
        double totalArea = event.getTotalArea();
        double sumArea = 0;
        for(Fire fire : slice.getFires()) {
            for(FireDay fireDay : fire.getFireDays()) {
                String date = fireDay.getDate().toString(DATE_FORMAT);
                double size = fireDay.getArea() * ACRES_PER_SQ_METER;
                
                sumArea += fireDay.getArea();
                
                HashMap<String, Object> dateDataMap = new LinkedHashMap<String, Object>();
                if (growthDatesMap.containsKey(date)) {
                    dateDataMap = (HashMap) growthDatesMap.get(date);
                }

                if (dateDataMap.containsKey("size")) {
                    size += (Double) dateDataMap.get("size");
                }
                dateDataMap.put("size", size);
                dateDataMap.put("sizeUnit", "acres");
                
                int locations = 0;
                for(Clump clump : fire.getClumps()) {
                    locations += clump.getRawData().size();
                }
                if (dateDataMap.containsKey("locations")) {
                    locations += (Integer) dateDataMap.get("locations");
                }
                dateDataMap.put("locations", locations);
                growthDatesMap.put(date, dateDataMap);
            }
        }
        
        double scaleFactor = totalArea / sumArea;
        
        for (String key : growthDatesMap.keySet()) {
            HashMap<String, Object> dateDataMap = (HashMap) growthDatesMap.get(key);
            double size = (Double) dateDataMap.get("size");
            size = roundVal(size * scaleFactor, 2);
            dateDataMap.put("size", size);
            growthDatesMap.put(key, dateDataMap);
        }
        
        growthMap.put("dates", growthDatesMap);
        return growthMap;
    }
    
    private Map<String, String> setFireAttributes(Event event) {
        Map<String, String> attributeDataMap = new LinkedHashMap<String, String>();
        for(Entry<String, String> entry : event.entrySet()) {
            attributeDataMap.put(entry.getKey(), entry.getValue());
        }
        return attributeDataMap;
    }
    
    @Override
    public String updateQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return "?detailed=false";
        }
        return queryString.concat("&detailed=false");
    }
    
    @Override
    protected void performExport(StaplerRequest request, OutputStream out, ApplicationSettings appSettings, String exportFileName,
            List<Event> events, DateTime startDate, DateTime endDate) throws IOException {
        File folder = getTempFolder();
        InputStream in = null;
        Map<String, String[]> requestParameters = request.getParameterMap();
        
        // If the "detailed" query request parameter was not given, return an error JSON object back to user.
        if (!requestParameters.containsKey("detailed")) {
            try {
                File smartfireEIFile = new File(folder, "summarydata.json");
                FileOutputStream fileOutputStream = new FileOutputStream(smartfireEIFile);
                
                Map<String, String> errorResponse = new HashMap<String, String>();
                errorResponse.put("error", "\"detailed\" must be defined in URL requst");
                errorResponse.put("example", request.getRequestURL().toString() + "?detailed=false");
                
                mapper.writeValue(fileOutputStream, errorResponse);
                fileOutputStream.write(newline);
                fileOutputStream.close();

                in = new FileInputStream(smartfireEIFile);
                IOUtils.copy(in, out);
            } catch(Exception e) {
                log.error("Error exporting to smartfire summary data format {}", e);
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
                return;
            }
        }
        
        detailedMode = Boolean.parseBoolean(requestParameters.get("detailed")[0]);
        
        try {
            File smartfireEIFile = new File(folder, "summarydata.json");
            FileOutputStream fileOutputStream = new FileOutputStream(smartfireEIFile);

            Map<String, Object> summaryDataMap = new LinkedHashMap<String, Object>();
            for(Event event : events) {
                String eventGuid = event.getUniqueId();
                Map<String, Object> eventDataMap = new LinkedHashMap<String, Object>();

                // Set general summary data information
                DateTime eventStartTime = event.getStartDateTime();
                DateTime eventEndTime = event.getEndDateTime();
                eventDataMap.put("startTime", eventStartTime.toString(DATE_TIME_FORMAT));
                eventDataMap.put("endTime", eventEndTime.toString(DATE_TIME_FORMAT)); // TBD: add logic for "current" value?
                eventDataMap.put("type", event.getFireType());
                eventDataMap.put("name", event.getDisplayName());
                eventDataMap.put("irwinGuid", event.get(IRWIN_GUID_FIELD));
                
                // Set event centroid
                Map<String, Double> centroid = setCentroid(event, appSettings);
                eventDataMap.put("centroid", centroid);
                
                // Set source summary object
                Map<String, Object> sourceSummaryMap = setSourceSummary(event);
                eventDataMap.put("sourceSummary", sourceSummaryMap);
                
                // set source data object, if requested
                if (detailedMode) {
                    Map<String, Object> sourceDataMap = setSourceData(event, appSettings);
                    eventDataMap.put("sourceData", sourceDataMap);
                }
                
                // set best fire data
                Map<String, Object> fireDataMap = setFireData(event, appSettings);
                eventDataMap.put("fireData", fireDataMap);
                
                // set growth object
                Map<String, Object> growthMap = setFireGrowth(event);
                eventDataMap.put("fireGrowth", growthMap);
                
                // set fire attributes, if requested
                if (detailedMode) {
                    Map<String, String> attributeDataMap = setFireAttributes(event);
                    eventDataMap.put("fireAttributes", attributeDataMap);
                }

                summaryDataMap.put(eventGuid, eventDataMap);
            }
            mapper.writeValue(fileOutputStream, summaryDataMap);
            fileOutputStream.write(newline);
            fileOutputStream.close();

            in = new FileInputStream(smartfireEIFile);
            IOUtils.copy(in, out);
        } catch(Exception e) {
            log.error("Error exporting to smartfire summary data format {}", e);
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
}

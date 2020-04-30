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
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.mrbean.MrBeanModule;
import org.geotools.geojson.geom.GeometryJSON;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.StaplerRequest;
import smartfire.ApplicationSettings;
import smartfire.database.Clump;
import smartfire.database.Event;
import smartfire.database.Fire;
import smartfire.gis.CoordinateTransformer;

/**
 * ExportMethod for exporting data to Smartfire EI.
 */
@MetaInfServices(ExportMethod.class)
public class SmartfireEIExportMethod extends AbstractExportMethod<Event> implements ExportMethod {
    private static final double ACRES_PER_SQ_METER = 0.000247105381;
    private final ObjectMapper mapper;
    private final byte[] newline = "\n".getBytes();

    public SmartfireEIExportMethod() {
        super("SmartfireEI", "smartfireei", "/images/icons/blueskyfile-32x32.png", Event.class, "application/json", ".json");

        mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.getJsonFactory().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        mapper.registerModule(new MrBeanModule());
    }

    @Override
    protected void performExport(StaplerRequest request, OutputStream out, ApplicationSettings appSettings, String exportFileName,
            List<Event> events, DateTime startDate, DateTime endDate) throws IOException {

        File folder = getTempFolder();
        InputStream in = null;
        CoordinateTransformer transformer = appSettings.getGeometryBuilder().newLonLatOutputTransformer();
        GeometryJSON geojson = new GeometryJSON();
        try {
            File smartfireEIFile = new File(folder, "smartfire_ei.json");
            FileOutputStream fileOutputStream = new FileOutputStream(smartfireEIFile);

            for(Event event : events) {
                // Holds data related to the event
                Map<String, Object> eventParams = new HashMap<String, Object>();

                // Collect all event field data
                Map<String, Object> fields = new HashMap<String, Object>();
                fields.put("display_name", event.getDisplayName());
                fields.put("start_date", event.getStartDate().toString());
                fields.put("end_date", event.getEndDate().toString());
                fields.put("total_area", event.getTotalArea() * ACRES_PER_SQ_METER);
                fields.put("fire_type", event.getFireType().toUpperCase());
                fields.put("probability", event.getProbability());
                eventParams.put("fields", fields);

                // Collect all event attribute data
                Map<String, Object> attributes = new HashMap<String, Object>();
                Set<Entry<String, String>> eventAttributes = event.entrySet();
                for(Entry<String, String> attribute : eventAttributes) {
                    String key = attribute.getKey();
                    String value = attribute.getValue();
                    attributes.put(key, value);
                }
                eventParams.put("attributes", attributes);

                // Get geojson formatted event shape data
                Geometry geometry = transformer.transform(event.getShape()); // Transforms coordinates into long/lat (x, y) points
                String shapeStr = geojson.toString(geometry); // Convert geometry to geojson string
                JsonNode shape = mapper.readTree(shapeStr); // Convert geojson string to json node tree
                eventParams.put("shape", shape);

                // Collect event sources data
                Map<String, Map<String, Integer>> sources = new HashMap<String, Map<String, Integer>>();
                Set<Fire> fires = event.getFires();
                for(Fire fire : fires) {
                    Map<String, Integer> sourceData = new HashMap<String, Integer>();
                    String sourceName = fire.getSource().getName();

                    // Get source data if it exists
                    if(sources.containsKey(sourceName)) {
                        sourceData = sources.get(sourceName);
                    }

                    // Get the daily detections of raw data for each clump of a fire
                    for(Clump clump : fire.getClumps()) {
                        int detects = clump.getRawData().size();
                        String clumpStartDateStr = clump.getStartDateTime().toString();

                        // If there is already a detects count for the given date, add it to the new detects count
                        if(sourceData.containsKey(clumpStartDateStr)) {
                            detects += sourceData.get(clumpStartDateStr);
                        }

                        sourceData.put(clumpStartDateStr, detects);
                    }
                    // Don't add source data map if no data was found within the specified date range
                    if(!sourceData.isEmpty()) {
                        sources.put(sourceName, sourceData);
                    }
                }
                eventParams.put("sources", sources);

                // Add combined event params into json event data
                Map<String, Object> eventData = new HashMap<String, Object>();
                eventData.put(event.getUniqueId(), eventParams);
                mapper.writeValue(fileOutputStream, eventData);
                fileOutputStream.write(newline);
            }
            fileOutputStream.close();

            in = new FileInputStream(smartfireEIFile);
            IOUtils.copy(in, out);
        } catch(Exception e) {
            log.error("Error exporting to smartfire_ei format {}", e);
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

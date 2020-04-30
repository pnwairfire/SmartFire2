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
package smartfire.func.fetch;

import com.vividsolutions.jts.geom.Point;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.ScheduledFetch;
import smartfire.database.Source;
import smartfire.fileimport.IRWINJSONParser;
import smartfire.func.Attribute;
import smartfire.func.FetchMethod;
import smartfire.gis.GeometryBuilder;
import smartfire.util.AreaUtil;
import smartfire.util.HTTPUtil;

@MetaInfServices(FetchMethod.class)
public class IRWINFetchMethod extends AbstractFetchMethod {
    private static final Logger log = LoggerFactory.getLogger(IRWINFetchMethod.class);
//    private static final String HTTP_TOKEN_URL = "https://irwinoat.doi.gov/arcgis/tokens/generateToken"; // Test
    private static final String HTTP_TOKEN_URL = "https://irwin.doi.gov/arcgis/tokens/generateToken"; // Production
    private static final String HTTP_TOKEN_USERNAME = "smartfire";
//    private static final String HTTP_TOKEN_PASSWORD = "JtAWxz3J7DWd"; // Test
    private static final String HTTP_TOKEN_PASSWORD = "ewPevQQNNkFK"; // Production
    private static final int HTTP_TOKEN_EXPIRATION = 5; // Token expiration in minutes
//    private static final String HTTP_INCIDENT_URL = "https://irwinoat.doi.gov/arcgis/rest/services/Irwin/MapServer/exts/Irwin/GetUpdates"; // Test
    private static final String HTTP_INCIDENT_URL = "https://irwin.doi.gov/arcgis/rest/services/Irwin/MapServer/exts/Irwin/GetUpdates"; // Production
    private static final DateTimeFormatter incidentDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z");
    private static final String IncidentFireTypeKind = "FI"; // IRWIN's classification of fire incidents
    private static final ObjectMapper mapper = new ObjectMapper();
    private final int maxBackwardDays;
    private final ScheduledFetch schedule;
    private final GeometryBuilder builder;
    
    public IRWINFetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder,
            @Attribute(name="maxBackwardDays",
            description="Maximum number of days backward in time to pull incident data since last fetch.  Enter a negative value to only pull current data.") Integer maxBackwardDays) {
        this.schedule = scheduledFetch;
        this.builder = geometryBuilder;
        this.maxBackwardDays = maxBackwardDays;
    }
    
    @Override
    public Collection<RawData> fetch(Source source, DateTime dateTime) throws Exception {
        log.info("Fetching IRWIN data HTTP");
        
        // Generate authentication token from IRWIN GenerateToken service
        log.info("Generating HTTP access token");
        String token;
        try {
            token = generateHTTPRequestToken();
        } catch (Exception e) {
            log.warn("Unable to retrieve HTTP access token; Aborting.", e);
            return Collections.emptyList();
        }
        
        // Pull incident data from IRWIN GetUpdates service
        log.info("Requesting IRWIN incident data");
        JsonNode incidents;
        try {
            incidents = getIncidentData(token, dateTime);
        } catch (Exception e) {
            log.warn("Unable to get list of incident ids; Aborting.", e);
            return Collections.emptyList();
        }
        int numIncidents = ((ArrayNode) incidents.get("incidents")).size();
        if (numIncidents == 0) {
            log.warn("No incident data was downloaded; Aborting.");
            return Collections.emptyList();
        }
        log.debug("Downloaded {} incidents.", numIncidents);
        
        // Parse json incident data into csv-like data
        IRWINJSONParser json;
        try {
            json = new IRWINJSONParser(incidents);
        } catch (IOException e) {
            log.warn("Unable to parse incident json data; Aborting.", e);
            return Collections.emptyList();
        }
        return new FetchResults(dateTime, json.getFieldNames(), json.getData());
    }
    
    private String generateHTTPRequestToken() throws IOException {
        Map<String, String> credentialRequest = new HashMap<String,String>();
        credentialRequest.put("username", HTTP_TOKEN_USERNAME);
        credentialRequest.put("password", HTTP_TOKEN_PASSWORD);
        credentialRequest.put("f", "json"); // Response format
        credentialRequest.put("expiration", Integer.toString(HTTP_TOKEN_EXPIRATION));
        JsonNode root = jsonPostRequest(HTTP_TOKEN_URL, credentialRequest);
        return root.get("token").asText();
    }
    
    private JsonNode getIncidentData(String token, DateTime dateTime) throws IOException {
        Map<String, String> incidentRequest = new HashMap<String,String>();
        DateTime fromDateTime = getFromDateTime(dateTime);
        log.info("Fetching incidents since {}.", fromDateTime.toString(incidentDateTimeFormatter));
        incidentRequest.put("token", token);
        incidentRequest.put("fromDateTime", fromDateTime.toString(incidentDateTimeFormatter));
        incidentRequest.put("f", "json"); // Response format
        return jsonPostRequest(HTTP_INCIDENT_URL, incidentRequest);
    }
    
    private JsonNode jsonPostRequest(String url, Map<String, String> postData) throws IOException {
        log.info("Submitting post request to {}", url);
        log.debug("Post data: {}", postData);
        String response = HTTPUtil.submitPostRequest(url, postData);
        return mapper.readTree(response);
    }
    
    private DateTime getFromDateTime(DateTime dateTime) {
        dateTime = dateTime.withZone(DateTimeZone.UTC);
        // If the fetch type is manual, then return the user submitted date at midnight
        if (schedule.getIsManual()) {
            log.debug("Manual fetch; Using user defined datetime at midnight.");
            return dateTime.toDateMidnight().toDateTime(DateTimeZone.UTC).minusDays(schedule.getDateOffset());
        }
        // Else return the last fetch time
        if (maxBackwardDays >= 0) {
            DateTime lastFetch = schedule.getLastFetch();
            DateTime now = new DateTime(DateTimeZone.UTC);
            Period period = new Period(lastFetch, now);
            // If last fetch time is beyond defined limit, then bring date closer so it's within the limit.
            if (period.getDays() > maxBackwardDays) {
                log.debug("Last fetch more than {} days ago; Truncating to date within range.", maxBackwardDays);
                return now.minusDays(maxBackwardDays);
            }
            log.debug("Using last fetch date.", lastFetch.toString(incidentDateTimeFormatter));
            return lastFetch;
        }
        // the day limit is disabled, so default to returning the current time
        log.debug("Ignoring last fetch time; Using time of fetch.", dateTime.toString(incidentDateTimeFormatter));
        return dateTime;
    }
    
    @Override
    public Iterator<RawData> getFetchResultsIterator(DateTime fetchDate, String[] fieldNames, Iterator<Object[]> iter) {
        return new IRWINFetchResultsIterator(fetchDate, fieldNames, iter);
    }
    
    private class IRWINFetchResultsIterator extends AbstractFetchResultsIterator {
        public IRWINFetchResultsIterator(DateTime fetchDate, String[] fieldNames, Iterator<Object[]> iter) {
            super(fetchDate, fieldNames, iter);
        }
        
        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());
            
            // Ignore incidents of unsupported types
            String incidentTypeKind = (String) row.get("IncidentTypeKind");
            if (!incidentTypeKind.equalsIgnoreCase(IncidentFireTypeKind)) {
                log.warn("Unsupported incident type found; Skipping.");
                return null;
            }
            
            // Set geometry
            Map<String, Object> shapeData = (Map) row.get("shapeData");
            if (shapeData.isEmpty()) {
                log.warn("No geometry data found; Skipping.");
                return null;
            }
            if (shapeData.containsKey("pointOfOrigin")) {
                Map<String, Double> pointOfOriginData = (Map) shapeData.get("pointOfOrigin");
                double lon = pointOfOriginData.get("x");
                double lat = pointOfOriginData.get("y");
                if(lon < -360 || lon > 360 || lat < -90 || lat > 90) {
                    log.warn("IRWIN record has invalid lon/lan: {}, {}; ignoring", lon, lat);
                    return null;
                }
                Point point = builder.buildPointFromLatLon(lon, lat);
                result.setShape(point);
            } else {
                log.warn("Unsupported geometry type found; Skipping.");
                return null;
            }
            row.remove("shapeData"); // Shape data is not a normal incident attribute field, so remove it
            
            // Set area
            Double acres;
            if (!isNull(row, "FinalAcres")) {
                acres = getDouble(row, "FinalAcres");
            } else if (!isNull(row, "CalculatedAcres")) {
                acres = getDouble(row, "CalculatedAcres");
            } else if (!isNull(row, "DailyAcres")) {
                acres = getDouble(row, "DailyAcres");
            } else if (!isNull(row, "InitialResponseAcres")) {
                acres = getDouble(row, "InitialResponseAcres");
            } else if (!isNull(row, "DiscoveryAcres")) {
                acres = getDouble(row, "DiscoveryAcres");
            } else {
                log.warn("Unable to find fire acres; Skipping.");
                return null;
            }
            double areaSqMeters = AreaUtil.acresToSquareMeters(acres);
            result.setArea(areaSqMeters);
            
            // Set source
            result.setSource(schedule.getSource());
            
            // Set start date time
            String start;
            if (!isNull(row, "FireDiscoveryDateTime")) {
                start = row.get("FireDiscoveryDateTime").toString();
            } else {
                log.warn("Unable to find start time; Skipping.");
                return null;
            }
            DateTime startDate = incidentDateTimeFormatter.parseDateTime(start);
            result.setStartDate(startDate);
            
            // Set end date time
            String end;
            if (!isNull(row, "FireOutDateTime")) {
                end = row.get("FireOutDateTime").toString();
            } else if (!isNull(row, "ContainmentDateTime")) {
                end = row.get("ContainmentDateTime").toString();
            } else if (!isNull(row, "ControlDateTime")) {
                end = row.get("ControlDateTime").toString();
            } else if (!isNull(row, "ICS209ReportDateTime")) {
                end = row.get("ICS209ReportDateTime").toString();
            } else if (!isNull(row, "ModifiedOnDateTime")) {
                end = row.get("ModifiedOnDateTime").toString();
            } else {
                log.warn("Unable to find end time; Skipping.");
                return null;
            }
            DateTime endDate = incidentDateTimeFormatter.parseDateTime(end);
            result.setEndDate(endDate);
            
            // Add all row fields to raw data
            for (String key : row.keySet()) {
                Object value = row.get(key);
                // Convert any null values to empty Strings
                if (value == null) {
                    value = "";
                }
                result.put(key, value.toString());
            }

            return result;
        }
        
    }
    
}

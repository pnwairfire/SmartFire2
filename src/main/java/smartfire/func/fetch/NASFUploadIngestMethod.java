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

import smartfire.fileimport.CSVParser;
import com.vividsolutions.jts.geom.Point;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.func.UploadIngestMethod;
import smartfire.func.Attribute;
import smartfire.gis.GeometryBuilder;
import smartfire.util.AreaUtil;

/**
 * The NASF Upload Ingest method (based on the Simple Data Upload Ingest method but with a couple data-specific QC checks).
 */
@MetaInfServices
public class NASFUploadIngestMethod implements UploadIngestMethod {
    private static final Logger log = LoggerFactory.getLogger(NASFUploadIngestMethod.class);
    private final Source source;
    private final GeometryBuilder geometryBuilder;
    private final String latitudeFieldName;
    private final String longitudeFieldName;
    private final String startDateFieldName;
    private final String areaBurnedFieldName;
    private final String endDateFieldName;
    private final String datePattern;

    public NASFUploadIngestMethod(Source source, GeometryBuilder geometryBuilder, 
            @Attribute(name = "latitudeFieldName",
                    description = "Name of the latitude field (must be decimal degrees).") String latitudeFieldName,
            @Attribute(name = "longitudeFieldName",
                    description = "Name of the longitude field (must be decimal degrees).") String longitudeFieldName,
            @Attribute(name = "startDateFieldName",
                    description = "Name of the start date field (format is M/D/Y).") String startDateFieldName,
            @Attribute(name = "areaBurnedFieldName", 
                    description = "Name of the area burned field (area must be in acres).") String areaBurnedFieldName,
            @Attribute(name = "endDateFieldName",
                    description = "Name of the end date field (leave blank for none).") String endDateFieldName,
            @Attribute(name = "datePattern",
                    description = "Date pattern (using Jave date formatting patterns)") String datePattern) {
        this.source = source;
        this.geometryBuilder = geometryBuilder;
        this.latitudeFieldName = latitudeFieldName;
        this.longitudeFieldName = longitudeFieldName;
        this.startDateFieldName = startDateFieldName;
        this.areaBurnedFieldName = areaBurnedFieldName;
        this.endDateFieldName = endDateFieldName;
        this.datePattern = datePattern;
    }
          
    @Override
    public Collection<RawData> ingest(String filePath, DateTime dateTime) throws Exception {
        CSVParser csv = new CSVParser(filePath);
        return new ResultsCollection(dateTime, csv.getFieldNames(), csv.getData());
    }

    private class ResultsCollection extends AbstractCollection<RawData> {
        private final DateTime date;
        private final String[] fieldNames;
        private final List<Object[]> data;

        public ResultsCollection(DateTime date, String[] fieldNames, List<Object[]> data) {
            this.date = date;
            this.fieldNames = fieldNames;
            this.data = data;
        }

        @Override
        public Iterator<RawData> iterator() {
            return new SimpleDataResultsIterator(date, fieldNames, data);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private class SimpleDataResultsIterator extends AbstractFetchResultsIterator {
        public SimpleDataResultsIterator(DateTime date, String[] fieldNames, List<Object[]> data) {
            super(date, fieldNames, data.iterator());
        }

        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());

            // Use string version of coordinations to check for sufficient precision (this also kills zeros and nulls)
            String decimals = "";
            String xCoordS = row.get(longitudeFieldName).toString();
            String yCoordS = row.get(latitudeFieldName).toString();
            StringTokenizer xTokens = new StringTokenizer(xCoordS, ".");
            StringTokenizer yTokens = new StringTokenizer(yCoordS, ".");
            if(xTokens.countTokens() == 2) {
                decimals = xTokens.nextToken();
                decimals = xTokens.nextToken();
                if(decimals.length() < 2) {
                    log.warn("Longitude has insufficient precision: {}; ignoring", xCoordS);
                    return null;                    
                }
            } else {
                log.warn("Longitude is null or has insufficient precision {}: ignoring", xCoordS);
                return null;
            }
            
            if(yTokens.countTokens() == 2) {
                decimals = yTokens.nextToken();
                decimals = yTokens.nextToken();
                if(decimals.length() < 2) {
                    log.warn("Latitude has insufficient precision: {}; ignoring", yCoordS);
                    return null;
                }
            } else {
                log.warn("Latitude is null or has insufficient precision {}: ignoring", yCoordS);
                return null;
            }

            
            // Get coordinates
            double xcoord = getDouble(row, longitudeFieldName);
            double ycoord = getDouble(row, latitudeFieldName);
            if(xcoord < -360 || xcoord > 360 || ycoord < -90 || ycoord > 90) {
                log.warn("Record has invalid lat/lon: {}, {}; ignoring", xcoord, ycoord);
                return null;
            }
            
            // Correct non-negative longitudes
            if(xcoord > 0) {
                double newXcoord = xcoord * -1;
                String newX = Double.toString(newXcoord);
                row.put(longitudeFieldName, newX);
                log.warn("Corrected non-negative longitude: {}", xcoord);
            }

            // Change (longitude, latitude) keys to (lon, lat)
            row = renameKey(row, longitudeFieldName, "lon");
            row = renameKey(row, latitudeFieldName, "lat");

            
            // Get area
            double area = getDouble(row, areaBurnedFieldName);
            if(area <= 0 ) {
                log.warn("Record has invalid area: {}; ignoring", area);
                return null;
            }
            double areaSqMeters;
            areaSqMeters = AreaUtil.acresToSquareMeters(area);

            // Build geometry
            Point point = geometryBuilder.buildPointFromLatLon(xcoord, ycoord);
            result.setShape(point);
            result.setArea(areaSqMeters);
            result.setSource(source);
            
            // Set start date time
            DateTime startDate = parsePatternDate((String) row.get(startDateFieldName), datePattern);
            if(startDate == null) {
               log.warn("Record has invalid start date: {}; ignoring", startDate);
                  return null;
              }

              // Determine End Date
              DateTime endDate;
              if(!"".equals(endDateFieldName)) {
                  endDate = parsePatternDate((String) row.get(endDateFieldName), datePattern);
                  if(endDate == null) {
                      log.warn("Record has invalid end date: {}; using start date", endDate);
                      endDate = startDate;
                  }
              } else {
                  endDate = startDate;
              }
            
//            // Set start date time
//            DateTime startDate = parseDate((String) row.get(startDateFieldName));
//            if(startDate == null) {
//                log.warn("Record has invalid start date: {}; ignoring", startDate);
//                return null;
//            }
//            
//
//            // Determine End Date
//            DateTime endDate;
//            if(!"".equals(endDateFieldName)) {
//                endDate = parseDate((String) row.get(endDateFieldName));
//                if(endDate == null) {
//                    log.warn("Record has invalid end date: {}; using start date", endDate);
//                    endDate = startDate;
//                }
//            } else {
//                endDate = startDate;
//            }

            result.setStartDate(startDate);
            result.setEndDate(endDate);

            for(String key : row.keySet()) {
                if(row.get(key) != null) {
                    result.put(key, row.get(key).toString());
                } else {
                    log.warn("Null value encountered for record key {}. Replacing with empty string", key);
                    result.put(key, "");
                }
            }

            return result;
        }
    }
    
    static DateTime parsePatternDate(String dateString, String datePattern) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern(datePattern);
        try {
            DateTime dt = fmt.parseDateTime(dateString);
            int month = dt.getMonthOfYear();
            int day = dt.getDayOfMonth();
            int year = dt.getYear();
            return new DateTime(year, month, day, 0, 0, 0, 0);
        } catch (IllegalArgumentException e) {
            log.warn("Date does not conform to specified pattern \"{}\"; ignoring", dateString);
            return null;
        }
        
    }
    
    static DateTime parseDate(String dateString) {
        String[] splitDate = dateString.split("/");
        if(splitDate.length != 3) {
            log.warn("Record has a badly formatted date \"{}\"; ignoring", dateString);
            return null;
            
        }
        int month = Integer.parseInt(splitDate[0]);
        int day = Integer.parseInt(splitDate[1]);
        int year = Integer.parseInt(splitDate[2]);
        return new DateTime(year, month, day, 0, 0, 0, 0);
    }
}

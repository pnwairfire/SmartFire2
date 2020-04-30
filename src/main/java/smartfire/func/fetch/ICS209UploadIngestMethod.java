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
import java.util.*;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.fileimport.CSVParser;
import smartfire.func.UploadIngestMethod;
import smartfire.gis.GeometryBuilder;
import smartfire.util.AreaUtil;

/**
 * The ICS209 Upload Ingest method.
 */
@MetaInfServices
public class ICS209UploadIngestMethod implements UploadIngestMethod {
    private static final Logger log = LoggerFactory.getLogger(ICS209UploadIngestMethod.class);
    private final Source source;
    private final GeometryBuilder geometryBuilder;

    public ICS209UploadIngestMethod(Source source, GeometryBuilder geometryBuilder) {
        this.source = source;
        this.geometryBuilder = geometryBuilder;
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
            return new ICS209ResultsIterator(date, fieldNames, data);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private class ICS209ResultsIterator extends AbstractFetchResultsIterator {
        public ICS209ResultsIterator(DateTime date, String[] fieldNames, List<Object[]> data) {
            super(date, fieldNames, data.iterator());
        }

        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());

            // Get coordinates
            double xcoord = getDouble(row, "longitude");
            double ycoord = getDouble(row, "latitude");
            if(xcoord < -360 || xcoord > 360 || ycoord < -90 || ycoord > 90) {
                log.warn("ICS209 record has invalid lat/lon: {}, {}; ignoring", xcoord, ycoord);
                return null;
            }

            // Change (longitude, latitude) keys to (lon, lat)
            row = renameKey(row, "longitude", "lon");
            row = renameKey(row, "latitude", "lat");

            // Remove records that are known to be not representative of a single fire (Texas does this)
            String county = (String) row.get("county");
            if (county.equalsIgnoreCase("Various")) {
                log.warn("ICS-209 record has county 'Various' and is not of a single fire; ignoring");
                return null;
            }
            
            // Get area
            double area = getDouble(row, "area");
            if(area < 0 ) {
                log.warn("ICS209 record has invalid area: {}; ignoring", area);
                return null;
            }
            String areaUnits = (String) row.get("area measurement");
            double areaSqMeters;
            if(areaUnits.equals("ACRES")) {
                areaSqMeters = AreaUtil.acresToSquareMeters(area);
            } else if(areaUnits.equals("SQ MILES")) {
                areaSqMeters = AreaUtil.squareMilesToSquareMeters(area);
            } else {
                log.warn("ICS209 record has unknown units for area: {}; ignoring", area);
                return null;
            }

            // Change area key to area_row
            row = renameKey(row, "area", "area_raw");

            // Build geometry
            Point point = geometryBuilder.buildPointFromLatLon(xcoord, ycoord);
            result.setShape(point);
            result.setArea(areaSqMeters);
            result.setSource(source);

            // Set date time
            DateTime startDate = ICS209FetchMethod.parseDate((String) row.get("start date"));
            DateTime reportDate = ICS209FetchMethod.parseDate((String) row.get("report_date"));

            if(reportDate == null) {
                return null;
            }
            
            if(startDate == null || startDate.isAfter(reportDate)) {
                startDate = reportDate;
            }

            // Determine End Date
            DateTime endDate;
            int fireDays = Days.daysBetween(startDate.toDateMidnight(), reportDate.toDateMidnight()).getDays();
            if((fireDays > 0) && ((area / fireDays) > 10)) {
                endDate = reportDate;
            } else {
                endDate = startDate;
            }

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
}

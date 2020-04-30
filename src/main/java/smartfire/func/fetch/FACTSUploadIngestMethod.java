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
 * The FACTS Upload Ingest method.
 */
@MetaInfServices
public class FACTSUploadIngestMethod implements UploadIngestMethod {
    private static final Logger log = LoggerFactory.getLogger(FACTSUploadIngestMethod.class);
    private final Source source;
    private final GeometryBuilder geometryBuilder;

    public FACTSUploadIngestMethod(Source source, GeometryBuilder geometryBuilder) {
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
            return new FACTSResultsIterator(date, fieldNames, data);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private class FACTSResultsIterator extends AbstractFetchResultsIterator {
        public FACTSResultsIterator(DateTime date, String[] fieldNames, List<Object[]> data) {
            super(date, fieldNames, data.iterator());
        }

        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());

            // Check for null values in lat lon values
            String lon = (String) row.get("LONGITUDE");
            String lat = (String) row.get("LATITUDE");
            if(lon.isEmpty() || lat.isEmpty()) {
                log.warn("FACTS record has invalid lat/lon: {}, {}; ignoring", lat, lon);
                return null;
            }

            // Get coordinates
            double xcoord = getDouble(row, "LONGITUDE");
            double ycoord = getDouble(row, "LATITUDE");
            if(xcoord < -360 || xcoord > 360 || ycoord < -90 || ycoord > 90) {
                log.warn("FACTS record has invalid lat/lon: {}, {}; ignoring", xcoord, ycoord);
                return null;
            }

            // Change (longitude, latitude) keys to (lon, lat)
            row = renameKey(row, "LONGITUDE", "lon");
            row = renameKey(row, "LATITUDE", "lat");

            // check for null values in area
            String areaString = (String) row.get("Acres Accomp");
            if(areaString.isEmpty()) {
                log.warn("FACTS record has invalid area: {}; ignoring", areaString);
                return null;
            }

            // Get area
            double area = getDouble(row, "Acres Accomp");
            double areaSqMeters = AreaUtil.acresToSquareMeters(area);

            // Change area key to area_raw
            row = renameKey(row, "Acres Accomp", "area_raw");

            // Build geometry
            Point point = geometryBuilder.buildPointFromLatLon(xcoord, ycoord);
            result.setShape(point);
            result.setArea(areaSqMeters);
            result.setSource(source);

            // Get start date time
            String dateAccomplished = (String) row.get("Date Accomplished");
            DateTime startDate = parseDate(dateAccomplished);
            if(startDate == null) {
                log.warn("FACTS record has a badly formatted date {}; ignoring", dateAccomplished);
                return null;
            }
            
            // Start date and End date are the same.
            result.setStartDate(startDate);
            result.setEndDate(startDate);

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

        private DateTime parseDate(String date) {
            String[] splitDate = date.split("-");
            if(splitDate.length != 3) {
                return null;
            }
            int year = Integer.parseInt(splitDate[0]);
            int month = Integer.parseInt(splitDate[1]);
            int day = Integer.parseInt(splitDate[2]);
            return new DateTime(year, month, day, 0, 0, 0, 0);
        }
    }
}

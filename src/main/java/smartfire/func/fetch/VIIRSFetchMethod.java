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

import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.ScheduledFetch;
import smartfire.database.Source;
import smartfire.fileimport.CSVParser;
import smartfire.func.FetchMethod;
import smartfire.gis.GeometryBuilder;

/**
 * FetchMethod for fetching VIIRS data in CSV format.
 */
@MetaInfServices(FetchMethod.class)
public class VIIRSFetchMethod extends AbstractFetchMethod {
    private static final Logger log = LoggerFactory.getLogger(VIIRSFetchMethod.class);
    private static final String FILE_SERVER = "viirsfire.geog.umd.edu";
    private static final String FILE_PATH = "/web_data/CONUS/";
    private static final double ASSUMED_FIRE_AREA_METERS = 0.0;
    private final ScheduledFetch schedule;
    private final GeometryBuilder geometryBuilder;

    public VIIRSFetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder) {
        this.schedule = scheduledFetch;
        this.geometryBuilder = geometryBuilder;
    }


    @Override
    public Collection<RawData> fetch(Source source, DateTime dateTime) throws Exception {
        log.info("Fetching VIIRS active fire data file via HTTP");
        //http://viirsfire.geog.umd.edu/web_data/CONUS/2015/01/04/NPP_VIIRS_20150104_AF.txt
        String fullName = FILE_SERVER + FILE_PATH + dateTime.toString("yyyy/MM/dd") + 
                "/NPP_VIIRS_" + dateTime.toString("yyyyMMdd") + "_AF.txt";
        URL url = new URL("http://" + fullName);
        InputStream inputStream = url.openStream();
        CSVParser csv = new CSVParser(inputStream);
        return new VIIRSFetchMethod.ResultsCollection(dateTime, csv.getFieldNames(), csv.getData());
    }

    @Override
    public Iterator<RawData> getFetchResultsIterator(DateTime fetchDate, String[] fieldNames,
            Iterator<Object[]> iter) {
        throw new UnsupportedOperationException("Not supported yet.");
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
            return new VIIRSFetchMethod.VIIRSResultsIterator(date, fieldNames, data);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private class VIIRSResultsIterator extends AbstractFetchResultsIterator {
        public VIIRSResultsIterator(DateTime date, String[] fieldNames, List<Object[]> data) {
            super(date, fieldNames, data.iterator());
        }

        //DateTimeFormatter fmt = DateTimeFormat.forPattern("M/d/YYYY h:m:s a");
        DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYYMMdd");
        
        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());

            double xcoord = getDouble(row, "longitude");
            double ycoord = getDouble(row, "latitude");
            if(xcoord < -360 || xcoord > 360 || ycoord < -90 || ycoord > 90) {
                log.warn("VIIRS record has invalid lat/lon: {}, {}; ignoring", xcoord, ycoord);
                return null;
            }

            com.vividsolutions.jts.geom.Point point = geometryBuilder.buildPointFromLatLon(xcoord, ycoord);
            result.setShape(point);
            result.setArea(ASSUMED_FIRE_AREA_METERS);
            result.setSource(schedule.getSource());

            // Find the start and end date for VIIRS record
            String date = (String) row.get("date");
            DateTime dt = fmt.parseDateTime(date);
            result.setStartDate(dt);
            result.setEndDate(dt.plusDays(1).minusMillis(1));      

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

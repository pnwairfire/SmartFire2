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

/**
 * The HMS Upload Ingest method.
 */
@MetaInfServices
public class HMSUploadIngestMethod implements UploadIngestMethod {
    private static final Logger log = LoggerFactory.getLogger(HMSUploadIngestMethod.class);
    private final Source source;
    private final GeometryBuilder geometryBuilder;
    private static final double ASSUMED_FIRE_AREA_METERS = 0.0;

    public HMSUploadIngestMethod(Source source, GeometryBuilder geometryBuilder) {
        this.source = source;
        this.geometryBuilder = geometryBuilder;
    }

    @Override
    public Collection<RawData> ingest(String filePath, DateTime dateTime) throws Exception {
        log.info("Opening CSV file for parsing: {}", filePath);
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
            return new HMSResultsIterator(date, fieldNames, data);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private class HMSResultsIterator extends AbstractFetchResultsIterator {
        public HMSResultsIterator(DateTime date, String[] fieldNames, List<Object[]> data) {
            super(date, fieldNames, data.iterator());
        }

        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());

            double xcoord = getDouble(row, "Lon");
            double ycoord = getDouble(row, "Lat");
            if(xcoord < -360 || xcoord > 360 || ycoord < -90 || ycoord > 90) {
                log.warn("HMS record has invalid lat/lon: {}, {}; ignoring", xcoord, ycoord);
                return null;
            }

            Point point = geometryBuilder.buildPointFromLatLon(xcoord, ycoord);
            result.setShape(point);
            result.setArea(ASSUMED_FIRE_AREA_METERS);
            result.setSource(source);

            // Find the start and end date for HMS record
            long yearDay = getLong(row, "YearDay");
            int year = (int) (yearDay / 1000);
            int dayOfYear = (int) (yearDay % 1000);
            long time = getLong(row, "Time");
            int hour = (int) (time / 100);
            int minute = (int) (time % 100);
            if(hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                log.warn("HMS record has unexpected time \"{}\"; ignoring", time);
                return null;
            }
            DateTime dt = new DateTime(year, 1, 1, hour, minute, 0, 0).plusDays(dayOfYear - 1).toDateMidnight().toDateTime();
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

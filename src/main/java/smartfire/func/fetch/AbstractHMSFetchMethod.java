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
import org.apache.commons.net.ftp.FTPClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.SmartfireException;
import smartfire.database.RawData;
import smartfire.database.ScheduledFetch;
import smartfire.database.Source;
import smartfire.func.FetchMethod;
import smartfire.gis.GeometryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Abstract HMS Fetch Method.
 *
 * FIXME: Rename references of FTP to HTTP
 */
public abstract class AbstractHMSFetchMethod extends AbstractFetchMethod implements FetchMethod {
    private static final Logger log = LoggerFactory.getLogger(AbstractHMSFetchMethod.class);
    // FIXME: These settings should be externalized to somewhere where it's easier to configure them
    private static final String BASE_URL = "https://satepsanone.nesdis.noaa.gov";
    private static final double ASSUMED_FIRE_AREA_METERS = 0.0;
    private final ScheduledFetch schedule;
    private final GeometryBuilder builder;

    public AbstractHMSFetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder) {
        this.schedule = scheduledFetch;
        this.builder = geometryBuilder;
    }

    public abstract String getFtpPath();

    public abstract String getFtpPathArchive();

    public abstract String getFinalFile(DateTime dateTime, String path);

    public abstract String getPrelimFile(DateTime dateTime, String path);

    public abstract HMSFileReader getFileReader();

    public abstract InputStream getFileStream(FTPClient ftp, String remoteFileName) throws Exception;

    public interface HMSFileReader {
        public abstract void open(InputStream fileStream) throws Exception;

        public abstract void close() throws Exception;

        public abstract Object getState();

        public abstract String[] getFieldNames();

        public abstract List<Object[]> getData();
    }

    @Override
    public Collection<RawData> fetch(Source source, DateTime dateTime) throws Exception {
        log.info("Fetching HMS data file via HTTP");
        InputStream fileStream = null;
        HMSFileReader reader = null;
        try {
            final String remoteFile;

            String finalFile = getFinalFile(dateTime, getFtpPath());
            String prelimFile = getPrelimFile(dateTime, getFtpPath());
            String archiveFile = getFinalFile(dateTime, getFtpPathArchive());

            if (hmsFileExists(finalFile)) {
                log.debug("Using final HMS data: " + BASE_URL.concat(finalFile));
                remoteFile = finalFile;
            } else if (hmsFileExists(prelimFile)) {
                log.debug("Using preliminary HMS data: " + BASE_URL.concat(prelimFile));
                remoteFile = prelimFile;
            } else if (hmsFileExists(archiveFile)) {
                log.debug("Using archive HMS data: " + BASE_URL.concat(archiveFile));
                remoteFile = archiveFile;
            } else {
                log.warn("HMS data are not available for the date: " + dateTime.toString("yyyy-MM-dd"));
                return Collections.emptyList();
            }

            fileStream = new URL(BASE_URL.concat(remoteFile)).openStream();

            reader = getFileReader();
            reader.open(fileStream);
            return new FetchResults(dateTime, reader.getFieldNames(), reader.getData());
        } finally {
            if(reader != null && reader.getState() != null) {
                reader.close();
            }
            if(fileStream != null) {
                fileStream.close();
            }
        }
    }

    @Override
    public Iterator<RawData> getFetchResultsIterator(DateTime fetchDate, String[] fieldNames, Iterator<Object[]> iter) {
        return new HMSFetchResultsIterator(fetchDate, fieldNames, iter);
    }

    private boolean hmsFileExists(String filepath) throws IOException {
        URL url = new URL(BASE_URL.concat(filepath));
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("HEAD");
        return con.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    private class HMSFetchResultsIterator extends AbstractFetchResultsIterator {
        public HMSFetchResultsIterator(DateTime fetchDate, String[] fieldNames, Iterator<Object[]> iter) {
            super(fetchDate, fieldNames, iter);
        }

        @Override
        protected Map<String, Object> getFields(Object[] row) {
            if(row.length == fieldNames.length + 1) {
                // Ugh. Fix the ugly bug in HMS's CSV file!
                String[] correctedFieldNames = new String[row.length];
                System.arraycopy(fieldNames, 0, correctedFieldNames, 0, fieldNames.length);
                String nextToLastHeader = fieldNames[fieldNames.length - 1];
                int lastSpaceIdx = nextToLastHeader.lastIndexOf(' ');
                if(lastSpaceIdx == -1) {
                    throw new SmartfireException("Unexpected last field header: " + nextToLastHeader);
                }
                String lastHeader = nextToLastHeader.substring(lastSpaceIdx + 1);
                nextToLastHeader = nextToLastHeader.substring(0, lastSpaceIdx);
                correctedFieldNames[fieldNames.length - 1] = nextToLastHeader;
                correctedFieldNames[fieldNames.length] = lastHeader;
                this.fieldNames = correctedFieldNames;
            } else if(row.length != fieldNames.length) {
                throw new SmartfireException("Assertion failure: field header does not match record");
            }
            Map<String, Object> fields = new LinkedHashMap<String, Object>();
            for(int i = 0; i < row.length; i++) {
                fields.put(fieldNames[i], row[i]);
            }
            return fields;
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

            Point point = builder.buildPointFromLatLon(xcoord, ycoord);
            result.setShape(point);
            result.setArea(ASSUMED_FIRE_AREA_METERS);
            result.setSource(schedule.getSource());

            result.setStartDate(expectedDateTime);
            result.setEndDate(expectedDateTime.plusDays(1).minusMillis(1));

            // Find the full DateTime for each HMS record
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
            DateTime dt = new DateTime(year, 1, 1, hour, minute, 0, 0, DateTimeZone.UTC).plusDays(dayOfYear - 1);
            if(!isDateTimeAcceptable(dt)) {
                log.warn("HMS record has unexpected yearday {} ({}); ignoring", yearDay, dt.toString("yyyy-MM-dd"));
                return null;
            }
            result.put("DateTime", dt.toString());

            for(String key : row.keySet()) {
                result.put(key, row.get(key).toString());
            }

            return result;
        }

        /*
         * For HMS data the date is acceptable if it is +/-1 whole day from the expected date.
         */
        private boolean isDateTimeAcceptable(DateTime result) {
            DateTime min = expectedDateTime.minusHours(24);
            DateTime max = expectedDateTime.plusHours(48);
            return (result.isAfter(min) && result.isBefore(max));
        }
    }
}

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

import au.com.bytecode.opencsv.CSVReader;
import com.vividsolutions.jts.geom.Point;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.ScheduledFetch;
import smartfire.database.Source;
import smartfire.fileimport.CSVParser;
import smartfire.func.FetchMethod;
import smartfire.func.Attribute;
import smartfire.gis.GeometryBuilder;
import smartfire.util.AreaUtil;

@MetaInfServices(FetchMethod.class)
public class ICS209FetchMethod extends AbstractFetchMethod {
    private static final Logger log = LoggerFactory.getLogger(ICS209FetchMethod.class);
    // FIXME: These settings should be externalized to somewhere where it's easier to configure them
    private static final String FTP_SERVER = "ftp2.fs.fed.us";
    private static final String FTP_USERNAME = "anonymous";
    private static final String FTP_PASSWORD = "";
    private static final String FTP_PATH = "/incoming/wo_fam/";
    private static final String FILENAME = "bluesky-info.txt";
    private final String dateFilter;
    private final ScheduledFetch schedule;
    private final GeometryBuilder builder;

    public ICS209FetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder,
            @Attribute(name = "DATE_FILTER",
            description = "Reports prior to this date (mm/dd/yyyy) will be excluded.") String dateFilter) {
        this.dateFilter = dateFilter;
        this.schedule = scheduledFetch;
        this.builder = geometryBuilder;
    }

    @Override
    public Collection<RawData> fetch(Source source, DateTime dateTime) throws Exception {
        log.info("Fetching ICS209 data file via FTP");
        FTPClient ftp = null;
        InputStream fileStream = null;
        CSVReader reader = null;
        try {
            ftp = new FTPClient();
            ftp.setRemoteVerificationEnabled(false);
            ftp.connect(FTP_SERVER);
            ftp.login(FTP_USERNAME, FTP_PASSWORD);
            ftp.enterLocalPassiveMode();

            // Verify ftp login
            log.info(ftp.getReplyString().trim());
            int reply = ftp.getReplyCode();
            if(!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                log.warn("FTP server refused connection.");
                return Collections.emptyList();
            }

            // Check ICS209 data is available
            // Note: changeWorkingDirectory() is required since directories are hidden
            if(!ftp.changeWorkingDirectory(FTP_PATH)) {
                log.warn("Unable to open directory {}; Aborting.", FTP_PATH);
                return Collections.emptyList();
            }

            // Get file stream of ICS209 data
            log.info("Opening ICS209 CSV file: {}", FILENAME);
            ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
            fileStream = ftp.retrieveFileStream(FILENAME);
            
            if(fileStream == null) {
                log.warn("ICS209 data are not available for the selected date.");
                return Collections.emptyList();
            }

            // Read CSV data
            CSVParser csv = new CSVParser(fileStream);

            return new FetchResults(dateTime, csv.getFieldNames(), csv.getData());
        } finally {
            if(reader != null) {
                reader.close();
            }
            if(fileStream != null) {
                fileStream.close();
            }
            if(ftp != null) {
                ftp.disconnect();
            }
        }
    }

    @Override
    public Iterator<RawData> getFetchResultsIterator(DateTime fetchDate, String[] fieldNames, Iterator<Object[]> iter) {
        return new ICS209FetchResultsIterator(fetchDate, fieldNames, iter);
    }

    private class ICS209FetchResultsIterator extends AbstractFetchResultsIterator {
        public ICS209FetchResultsIterator(DateTime fetchDate, String[] fieldNames, Iterator<Object[]> iter) {
            super(fetchDate, fieldNames, iter);
        }
        
        // Parse date filter
        DateTime allowedDate = parseDate(dateFilter);

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
            if(area <= 0) {
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
            Point point = builder.buildPointFromLatLon(xcoord, ycoord);
            result.setShape(point);
            result.setArea(areaSqMeters);
            result.setSource(schedule.getSource());

            // Set date time
            DateTime startDate = parseDate((String) row.get("start date"));
            DateTime reportDate = parseDate((String) row.get("report_date"));

            if(reportDate == null || reportDate.isAfterNow()) {
                return null;
            }

            if(startDate == null || startDate.isAfter(reportDate)) {
                startDate = reportDate;
            }
            
            // If start date is before allowable, exclude
            if (!dateFilter.equals("")){
                if(startDate.isBefore(allowedDate)){
                    log.warn("date is before date filter, excluding: {}", startDate);
                    return null;
                }
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
            result.setEndDate(endDate.plusDays(1).minusMillis(1));

            for(String key : row.keySet()) {
                result.put(key, row.get(key).toString());
            }

            return result;
        }
    }

    static DateTime parseDate(String dateString) {
        String[] splitDate = dateString.split("/");
        if(splitDate.length != 3) {
            log.warn("ICS209 record has a badly formatted date \"{}\"; ignoring", dateString);
            return null;
        }
        int month = Integer.parseInt(splitDate[0]);
        int day = Integer.parseInt(splitDate[1]);
        int year = Integer.parseInt(splitDate[2]);
        return new DateTime(year, month, day, 0, 0, 0, 0);
    }
}

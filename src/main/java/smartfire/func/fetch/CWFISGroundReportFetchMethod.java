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
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.ScheduledFetch;
import smartfire.database.Source;
import smartfire.fileimport.CSVParser;
import smartfire.func.Attribute;
import smartfire.func.FetchMethod;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.ShapeAttributes;
import smartfire.util.ShapefileUtil;

@MetaInfServices(FetchMethod.class)
public class CWFISGroundReportFetchMethod extends AbstractFetchMethod {
    private static final Logger log = LoggerFactory.getLogger(CWFISGroundReportFetchMethod.class);
    private static final double METERS_PER_HECTARE = 10000;
    private static final double METERS_PER_ACRE = 4046.85642;
    private static final double KG_PER_TON = 907.1847;
    private static final String FTP_SERVER = "ftp.nofc.cfs.nrcan.gc.ca";
    private static final String FTP_USERNAME = "anonymous";
    private static final String FTP_PASSWORD = "blueskyframework";
    private static final String FTP_PATH = "/pub/fire/misc/";
    private static final String FILENAME = "cfs_bluesky.csv";
    private final String timeZoneShapefile;
    private final String timeZoneAttributeName;
    private final String agencyName;
    private final double duffConsumptionRatio;
    private final double flamingConsumptionRatio;
    private final double consumption_flaming_default;
    private final double consumption_smoldering_default;
    private final double consumption_duff_default;
    private final double consumption_residual_default;
    private final ScheduledFetch schedule;
    private final GeometryBuilder builder;
    
    public CWFISGroundReportFetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder,
            @Attribute(name = "TIME_ZONE_SHAPEFILE",
            description = "Location of shapefile used to determine time zone.") String timeZoneShapefile,
            @Attribute(name = "TIME_ZONE_ATTRIBUTE_NAME",
            description = "Name of the attribute used to determine the time zone.") String timeZoneAttributeName,
            @Attribute(name = "REPORTING_AGENCY_NAME",
            description = "Name of the agency that reported the fire.") String agencyName,
            @Attribute(name = "DUFF_CONSUMPTION_RATIO",
            description = "Duff consumption ratio") Double duffConsumptionRatio,
            @Attribute(name = "FLAMING_CONSUMPTION_RATIO",
            description = "Flaming consumption ratio") Double flamingConsumptionRatio,
            @Attribute(name = "FLAMING_CONSUMPTION_DEFAULT",
            description = "Default flaming consumption value") Double consumption_flaming_default,
            @Attribute(name = "SMOLDERING_CONSUMPTION_DEFAULT",
            description = "Default smoldering consumption value") Double consumption_smoldering_default,
            @Attribute(name = "DUFF_CONSUMPTION_DEFAULT",
            description = "Default duff consumption value") Double consumption_duff_default,
            @Attribute(name = "RESIDUAL_CONSUMPTION_DEFAULT",
            description = "Default residual consumption value") Double consumption_residual_default) {
        this.timeZoneShapefile = timeZoneShapefile;
        this.timeZoneAttributeName = timeZoneAttributeName;
        this.agencyName = agencyName;
        this.schedule = scheduledFetch;
        this.builder = geometryBuilder;
        this.duffConsumptionRatio = duffConsumptionRatio;
        this.flamingConsumptionRatio = flamingConsumptionRatio;
        this.consumption_flaming_default = consumption_flaming_default;
        this.consumption_smoldering_default = consumption_smoldering_default;
        this.consumption_duff_default = consumption_duff_default;
        this.consumption_residual_default = consumption_residual_default;
    }
    
    @Override
    public Collection<RawData> fetch(Source source, DateTime dateTime) throws Exception {
        log.info("Fetching CWFIS ground reports data file via FTP");
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
            
            // Check CWFIS data is available
            // Note: changeWorkingDirectory() is required since directories are hidden
            if(!ftp.changeWorkingDirectory(FTP_PATH)) {
                log.warn("Unable to open directory {}; Aborting.", FTP_PATH);
                return Collections.emptyList();
            }

            // Get file stream of CWFIS data
            log.info("Opening CWFIS CSV file: {}", FILENAME);
            ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
            fileStream = ftp.retrieveFileStream(FILENAME);
            
            if(fileStream == null) {
                log.warn("CWFIS data are not available for the selected file: {}", FILENAME);
                return Collections.emptyList();
            }

            // Read CSV data
            CSVParser csv = new CSVParser(fileStream);
            // deal with date filter here?
            
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
        return new CWFISGroundReportFetchResultsIterator(fetchDate, fieldNames, iter);
    }
    
    public class CWFISGroundReportFetchResultsIterator extends AbstractFetchResultsIterator {
        public CWFISGroundReportFetchResultsIterator(DateTime fetchDate, String[] fieldNames, Iterator<Object[]> iter) {
            super(fetchDate, fieldNames, iter);
        }

        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

            // Filter for agency, if agencyName filter is set
            if (!agencyName.equals("")) {
                String rptAgency = (String) row.get("agency");
                if (rptAgency.equals("") || !agencyName.equalsIgnoreCase(rptAgency)) {
                    log.warn("Fire not reported by requested agency: {}; ignoring", agencyName);
                    return null;
                }
            }
            
            // Get coordinates
            double xcoord = getDouble(row, "lon");
            double ycoord = getDouble(row, "lat");
            if(xcoord < -360 || xcoord > 360 || ycoord < -90 || ycoord > 90) {
                log.warn("CWFIS record has invalid lat/lon: {}, {}; ignoring", xcoord, ycoord);
                return null;
            }
            
            // Get area
            double areaHectares = getDouble(row, "hectares");
            if(areaHectares < 0) {
                log.warn("CWFIS record has invalid area: {}; ignoring", areaHectares);
                return null;
            }
            double areaSqMeters = areaHectares * METERS_PER_HECTARE;

            // Build geometry
            Point point = builder.buildPointFromLatLon(xcoord, ycoord);
            result.setShape(point);
            result.setArea(areaSqMeters);
            result.setSource(schedule.getSource());
            
            // Raw first and last DateTimes from data as string in UTC
            String firstRepDate = (String) row.get("first_rep_date");
            String lastRepDate = (String) row.get("last_rep_date");
            String rptStartDate = (String) row.get("startdate");
            if((rptStartDate.equals("") & firstRepDate.equals("")) || lastRepDate.equals("")) {
                return null;
            }
            
            log.info("Raw first_rep_date: {}, Last report date: {}", firstRepDate, lastRepDate);
            log.info("Raw startdate: {}", rptStartDate);
            
            String beginDate = rptStartDate;
            if (beginDate.equals("") || beginDate == null) {
                beginDate = firstRepDate;
                log.warn("Using the first_rep_date as the start date");
            }

            // Parse into first/last DateTimes
            DateTime beginDateUTC = CWFISGroundReportFetchMethod.parseDateTime(beginDate, formatter);
            DateTime lastRepDateUTC = CWFISGroundReportFetchMethod.parseDateTime(lastRepDate, formatter);
            if(beginDateUTC == null || lastRepDateUTC == null) {
                return null;
            }
            log.info("Begin date: {}, Last report date: {}", beginDateUTC, lastRepDateUTC);
            
            // Determine time zone
            String tz = getTimeZoneFromShapeFile(point, xcoord);
            log.info("Time zone: {}", tz);
            result.put("Time Zone", tz);
            DateTimeZone dtZone = getDateTimeZone(tz);

            // Get local datetime from UTC date
            DateTime datetimeLocal = beginDateUTC.withZone(dtZone);
            DateTime dateLocal = datetimeLocal.toDateMidnight().toDateTime();
            log.info("Local date: {}", dateLocal);
            result.put("DateTime_local", dateLocal.toString());

            // Stored UTC date time in Raw Data table
            DateTime startDate = beginDateUTC.toDateMidnight().toDateTime();
            DateTime endDate = lastRepDateUTC.toDateMidnight().toDateTime();
            result.setStartDate(startDate);
            result.setEndDate(endDate.plusDays(1).minusMillis(1));
            log.info("Start date: {}, End date: {}", startDate, endDate);
            
            // set consumption here for each hotspot
            double consumption_flaming = consumption_flaming_default;
            double consumption_smoldering = consumption_smoldering_default;
            double consumption_duff = consumption_duff_default;
            double consumption_residual = consumption_residual_default;
            if(row.get("tfc") != null) {
                double tfc = java.lang.Double.parseDouble((String) row.get("tfc"));
                double tfc_meters = tfc * (METERS_PER_ACRE / KG_PER_TON);
                double ratio;
                if(row.get("sfc") != null && row.get("bfc") != null) {
                    double sfc = java.lang.Double.parseDouble((String) row.get("sfc"));
                    double bfc = java.lang.Double.parseDouble((String) row.get("bfc"));
                    double sfc_meters = sfc * (METERS_PER_ACRE / KG_PER_TON);
                    if(bfc == 0 || sfc == 0) {
                        log.info("Using default flaming ratio of {}.", flamingConsumptionRatio);
                        ratio = flamingConsumptionRatio;
                    } else {
                        ratio = sfc / bfc;
                    }
                    if(ratio > 1.0) {
                        ratio = 1.0;
                    }
                    consumption_flaming = (tfc_meters - sfc_meters) + (sfc_meters * ratio);
                } else {
                    // just use the default ratio
                    log.info("Using default flaming ratio of {}.", flamingConsumptionRatio);
                    ratio = flamingConsumptionRatio;
                    consumption_flaming = tfc_meters * ratio;
                }

                consumption_smoldering = tfc_meters - consumption_flaming;

                // duff consumption
                if(row.get("bfc") != null) {
                    double bfc = java.lang.Double.parseDouble((String) row.get("bfc"));
                    consumption_duff = bfc * (METERS_PER_ACRE / KG_PER_TON);
                } else {
                    // just use default ratio
                    log.info("Using default duff ratio of {}.", duffConsumptionRatio);
                    double duff_ratio = duffConsumptionRatio;
                    consumption_duff = tfc_meters * duff_ratio;
                }
            }
            // add to data attributes table
            result.put("consumption_flaming", Double.toString(consumption_flaming));
            result.put("consumption_smoldering", Double.toString(consumption_smoldering));
            result.put("consumption_duff", Double.toString(consumption_duff));
            result.put("consumption_residual", Double.toString(consumption_residual));
            
            for(String key : row.keySet()) {
                result.put(key, row.get(key).toString());
            }
            
            return result;
        }
        
    }
    
    private String getTimeZoneFromShapeFile(Geometry geom, double longitude) {
        ShapeAttributes shapeAttributes = ShapefileUtil.readShapeFile(builder, geom, timeZoneShapefile);
        Map<String, String> attr = shapeAttributes.getAttributes();
        if(attr.containsKey(timeZoneAttributeName)) {
            String timeZone = attr.get(timeZoneAttributeName);
            // what about daylight savings time
            log.info("Found time zone of {}.", timeZone);
            return timeZone;
        }
        // FIXME: Should make sure that hours offset is negative vs positive for hemisphere
        Integer hours_offset = (int) (longitude / (360 / 24));
        log.info("Unable to find a global time zone, using approximation.");
        return Integer.toString(hours_offset);
    }
    
    // Parse raw date string to UTC DateTime
    protected static DateTime parseDateTime(String dateTimeString, DateTimeFormatter formatter) {
        log.info("Date string: {}", dateTimeString);
        
        DateTime dateUTC;
        try {
            dateUTC = formatter.parseDateTime(dateTimeString);
        } catch(IllegalArgumentException e) {
            log.warn("CWFIS record has a badly formatted date \"{}\"; ignoring", dateTimeString);
            return null;
        }
        
        return dateUTC;
    }

    private static DateTimeZone getDateTimeZone(String tzOffset) {
        // Determine offset based on time zone string
        int hourOffset;
        int minuteOffset = 0;
        if(tzOffset.contains(".")) { // handle timezones with a half-hour offset, such as Newfoundland
            String[] tzoneParts = tzOffset.split("\\.");
            hourOffset = Integer.parseInt(tzoneParts[0]);
            minuteOffset = Integer.parseInt(tzoneParts[1]) * 6; // convert to minutes
        } else {
            hourOffset = Integer.parseInt(tzOffset);
        }
        
        return DateTimeZone.forOffsetHoursMinutes(hourOffset, minuteOffset);
    }
}

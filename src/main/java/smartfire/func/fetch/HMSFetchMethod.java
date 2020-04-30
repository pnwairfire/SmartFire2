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

import org.apache.commons.net.ftp.FTPClient;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.ScheduledFetch;
import smartfire.fileimport.CSVParser;
import smartfire.func.FetchMethod;
import smartfire.gis.GeometryBuilder;

import java.io.InputStream;
import java.util.List;

/**
 * FetchMethod for fetching HMS data in CSV format.
 *
 * FIXME: Rename references of FTP to HTTP
 */
@MetaInfServices(FetchMethod.class)
public class HMSFetchMethod extends AbstractHMSFetchMethod {
    private static final Logger log = LoggerFactory.getLogger(HMSFetchMethod.class);
    private static final String FTP_PATH = "/pub/FIRE/HMS/";
    private static final String FTP_PATH_ARCHIVE = "/pub/FIRE/HMS/TXT_ARCHIVE/";

    public HMSFetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder) {
        super(scheduledFetch, geometryBuilder);
    }

    @Override
    public String getFtpPath() {
        return FTP_PATH;
    }

    @Override
    public String getFtpPathArchive() {
        return FTP_PATH_ARCHIVE;
    }

    @Override
    public String getFinalFile(DateTime dateTime, String path) {
        return path + "hms" + dateTime.toString("yyyyMMdd") + ".txt";
    }

    @Override
    public String getPrelimFile(DateTime dateTime, String path) {
        return path + "hms" + dateTime.toString("yyyyMMdd") + ".prelim.txt";
    }

    @Override
    public HMSFileReader getFileReader() {
        return new HMSCVSFileReader();
    }

    @Override
    public InputStream getFileStream(FTPClient ftp, String remoteFileName) throws Exception {
        log.info("Opening HMS CSV file: {}", remoteFileName);
        ftp.setFileType(FTPClient.ASCII_FILE_TYPE);
        return ftp.retrieveFileStream(remoteFileName);
    }

    public class HMSCVSFileReader implements HMSFileReader {
        CSVParser csv = null;

        @Override
        public void open(InputStream fileStream) throws Exception {
            csv = new CSVParser(fileStream);
        }

        @Override
        public void close() throws Exception {
            return;
        }

        @Override
        public Object getState() {
            return csv;
        }

        @Override
        public String[] getFieldNames() {
            return csv.getFieldNames();
        }

        @Override
        public List<Object[]> getData() {
            return csv.getData();
        }
    }
}

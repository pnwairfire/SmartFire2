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

import com.svcon.jdbf.DBFReader;
import com.svcon.jdbf.JDBFException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.ScheduledFetch;
import smartfire.func.FetchMethod;
import smartfire.gis.GeometryBuilder;

/**
 * FetchMethod for fetching HMS data in DBF format.
 */
@MetaInfServices(FetchMethod.class)
public class HMSDBFFetchMethod extends AbstractHMSFetchMethod {
    private static final Logger log = LoggerFactory.getLogger(HMSDBFFetchMethod.class);
    private static final String FTP_PATH = "/FIRE/HMS/GIS/";
    private static final String FTP_PATH_ARCHIVE = "/FIRE/HMS/GIS/ARCHIVE/";

    public HMSDBFFetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder) {
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
        return path + "hms_fire" + dateTime.toString("yyyyMMdd") + ".dbf";
    }

    @Override
    public String getPrelimFile(DateTime dateTime, String path) {
        return path + "hms_fire" + dateTime.toString("yyyyMMdd") + ".prelim.dbf";
    }

    @Override
    public HMSFileReader getFileReader() {
        return new HMSDBFFileReader();
    }

    @Override
    public InputStream getFileStream(FTPClient ftp, String remoteFileName) throws Exception {
        log.info("Opening HMS DBF file: {}", remoteFileName);
        ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
        return ftp.retrieveFileStream(remoteFileName);
    }

    public class HMSDBFFileReader implements HMSFileReader {
        private DBFReader reader = null;

        @Override
        public void open(InputStream fileStream) throws Exception {
            if(reader != null) {
                reader.close();
            }
            reader = new DBFReader(fileStream);
        }

        @Override
        public void close() throws Exception {
            reader.close();
        }

        @Override
        public String[] getFieldNames() {
            String[] fieldNames = new String[reader.getFieldCount()];
            for(int i = 0; i < fieldNames.length; i++) {
                fieldNames[i] = reader.getField(i).getName();
            }
            return fieldNames;
        }

        @Override
        public List<Object[]> getData() {
            List<Object[]> data = new ArrayList<Object[]>();
            while(reader.hasNextRecord()) {
                try {
                    data.add(reader.nextRecord());
                } catch(JDBFException e) {
                    log.warn("Error reading DBF record", e);
                }
            }
            return data;
        }

        @Override
        public Object getState() {
            return reader;
        }
    }
}

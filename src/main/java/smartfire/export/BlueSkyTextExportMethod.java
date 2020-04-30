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
package smartfire.export;

import java.io.*;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.StaplerRequest;
import smartfire.ApplicationSettings;
import smartfire.database.Event;

@MetaInfServices(ExportMethod.class)
public class BlueSkyTextExportMethod extends BlueSkyExportMethod implements ExportMethod {

    public BlueSkyTextExportMethod() {
        super("BlueSky-CSV", "blueskycsv", "/images/icons/blueskyfile-32x32.png", "text/csv", ".csv");
    }
    
    public BlueSkyTextExportMethod(String displayName, String slugName, String iconPath, String contentType, String fileExtension) {
        super(displayName, slugName, iconPath, contentType, fileExtension);
    }

    @Override
    protected void performExport(StaplerRequest request, OutputStream out, ApplicationSettings appSettings, String exportFileName,
            List<Event> events, DateTime startDate, DateTime endDate) throws IOException {
        File folder = getTempFolder();
        File fireLocationsFile = null;
        InputStream in = null;
        try {
            fireLocationsFile = createBlueSkyFireLocations(folder, request.getRootPath(), appSettings.getGeometryBuilder(), appSettings.getConfig(), events, startDate, endDate);
            in = new FileInputStream(fireLocationsFile);
            IOUtils.copy(in, out);
        } catch(Exception e) {
            log.error("Error exporting to bluesky format {}", e);
        } finally {
            if(in != null) {
                in.close();
            }
            if(out != null) {
                out.close();
            }

            try {
                log.debug("Deleting temporary folder {}", folder);
                FileUtils.deleteDirectory(folder);
            } catch(IOException e) {
                log.warn("Unable to delete temporary folder", e);
            }
        }
    }
}

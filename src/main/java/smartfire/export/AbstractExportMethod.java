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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import org.joda.time.DateTime;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.ApplicationSettings;

/**
 * Abstract base class for ExportMethod implementations.
 * 
 * @param <T> the type of Exportable objects that this ExportMethod can handle
 */
public abstract class AbstractExportMethod<T extends Exportable> implements ExportMethod {
    protected final Logger log = LoggerFactory.getLogger(AbstractExportMethod.class);
    private final String displayName;
    private final String slugName;
    private final String iconPath;
    private final Class<T> exportableType;
    private final String contentType;
    private final String fileExtension;

    protected AbstractExportMethod(
            String displayName, 
            String slugName,
            String iconPath,
            Class<T> exportableType, 
            String contentType, 
            String fileExtension) {
        this.displayName = displayName;
        this.slugName = slugName;
        this.iconPath = iconPath;
        this.exportableType = exportableType;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }
    
    protected abstract void performExport(
            StaplerRequest request,
            OutputStream stream, 
            ApplicationSettings appSettings, 
            String exportFileName,
            List<T> entities,
            DateTime startDate,
            DateTime endDate
            ) throws IOException;

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getSlugName() {
        return slugName;
    }
    
    @Override
    public String updateQueryString(String queryString) {
        return queryString;
    }

    @Override
    public String getIconPath() {
        return iconPath;
    }

    @Override
    public Class<? extends Exportable> getExportableType() {
        return exportableType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getFileExtension() {
        return fileExtension;
    }
    
    protected File getTempFolder() {
        File sysTempDir = new File(System.getProperty("java.io.tmpdir"));
        String dirName = UUID.randomUUID().toString();
        File tempDir = new File(sysTempDir, dirName);
        log.debug("Creating new temporary directory {}", tempDir);
        tempDir.mkdir();
        return tempDir;
    }

    @Override
    public void exportToStream(
            StaplerRequest request,
            OutputStream stream, 
            ApplicationSettings appSettings,
            String exportFileName,
            List<Exportable> entities,
            DateTime startDate,
            DateTime endDate
            ) throws IOException {
        
        @SuppressWarnings("unchecked")
        List<T> castedList = (List<T>) entities;
        
        performExport(request, stream, appSettings, exportFileName, castedList, startDate, endDate);
    }
    
}

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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.joda.time.DateTime;
import org.kohsuke.stapler.StaplerRequest;
import smartfire.ApplicationSettings;

/**
 * Represents a mechanism for exporting Exportable entities.
 */
public interface ExportMethod {
    /**
     * Gets the display name that should be shown to the user.
     * 
     * @return a string representing the kind of file that is to be exported
     */
    String getDisplayName();
    
    /**
     * Gets a slug name that can be used to identify the URL for running this
     * export.
     * 
     * @return a string representing a URL part 
     */
    String getSlugName();
    
    /**
     * Gets the url query parameters that are required to run this export.
     * 
     * @return a string representing the non-optional query parameters with
     * their default values set.
     */
    String updateQueryString(String queryString);
    
    /**
     * Gets a path to an icon image representing the type of file produced by
     * this ExportMethod.  The image path should be relative to the webapp 
     * root and should refer to a 32x32 image.
     * 
     * @return a string representing the path to an icon
     */
    String getIconPath();
    
    /**
     * Gets the type of Exportable entities that can be exported by this
     * ExportMethod.
     * 
     * @return a Class object representing the type of Exportable entities.
     *         If this ExportMethod can export all kinds of Exportable
     *         entities, return Exportable.class here.
     */
    Class<? extends Exportable> getExportableType();
    
    /**
     * Gets the MIME content-type of the exported data.
     * 
     * @return a string in MIME format
     */
    String getContentType();
    
    /**
     * Gets an extension commonly appended to files of the exported type.
     * 
     * @return an extension string, including a period if needed.
     */
    String getFileExtension();
    
    /**
     * Export the given entities to the given OutputStream.
     * 
     * @param request for this resource
     * @param stream an OutputStream to write the result to
     * @param appSettings the current application settings
     * @param exportFileName the filename being exported (if needed)
     * @param entities the entities to export
     * @param startDate the start time that defines the entities
     * @param endDate  the end time that defines the entities
     * @throws IOException if an error occurs
     */
    void exportToStream(
            StaplerRequest request,
            OutputStream stream, 
            ApplicationSettings appSettings, 
            String exportFileName,
            List<Exportable> entities,
            DateTime startDate,
            DateTime endDate
            ) throws IOException;
}

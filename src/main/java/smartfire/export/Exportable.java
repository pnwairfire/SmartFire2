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

import java.util.Map;
import org.joda.time.DateTime;
import smartfire.gis.GeometryEntity;

/**
 * Interface for entities that are exportable, that is, that can be exported
 * into downloadable formats like CSV or KML.
 */
public interface Exportable extends GeometryEntity {
    /**
     * Gets a map of extra exportable members of this object, along with their
     * types.
     * 
     * Any normally exported members (latitude, longitude, start_date, 
     * end_date, and area) do not need to be included here, since they
     * will be generated automatically.
     *
     * @return a map of exportable members to their corresponding types
     */
    Map<String, Class<?>> getExtraExportMemberMap();

    /**
     * Gets a map of the basic members of this object, along with their
     * types.
     * 
     * @return a map of exportable members to their corresponding types
     */
    Map<String, Class<?>> getBasicMemberMap();
    
    /**
     * Fetches the value of an exportable member.
     *
     * @param name the name of the member to fetch
     * @return the value
     */
    Object getExtraExportMember(String name);
    
    /**
     * Returns the area associated with this entity.
     * 
     * @return the area, expressed in square meters
     */
    double getArea();
    
    /**
     * Returns the start date of this entity.
     * 
     * @return the start date
     */
    DateTime getStartDateTime();
    
    /**
     * Returns the end date of this entity.
     * 
     * @return the end date
     */
    DateTime getEndDateTime();

    /**
     * Gets the X coordinate of the point that should be used when exporting
     * this entity into an output format that only handles point data.
     *
     * @return the X coordinate
     */
    Double getExportPointX();

    /**
     * Gets the Y coordinate of the point that should be used when exporting
     * this entity into an output format that only handles point data.
     *
     * @return the Y coordinate
     */
    Double getExportPointY();
}

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
package smartfire.gis;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Interface representing an entity that has an associated geometric shape.
 */
public interface GeometryEntity {
    /**
     * Gets the geometric shape associated with this entity.
     *
     * @return a Geometry object
     */
    Geometry getShape();
    
    /**
     * Gets the description of the Geometry object
     *
     * @return a String representing the description of the Geometry object
     */
    String getShapeName();
}

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
package smartfire.layer;

import com.vividsolutions.jts.geom.Geometry;
import smartfire.database.SummaryDataLayer;
import smartfire.gis.GeometryBuilder;

/**
 * Represents a mechanism for reading summary data layer files.
 */
public interface LayerReadingMethod {
    /**
     * Reads attributes from a layer for the region matching a given Geometry.
     *
     * @param geometryBuilder the GeometryBuilder from the current application
     *                        settings, used to determine the coordinate
     *                        system to use
     * @param layer the SummaryDataLayer to read from
     * @param geom the Geometry to intersect with
     * @return a LayerAttributes object representing the result
     */
    LayerAttributes readAttributes(GeometryBuilder geometryBuilder, SummaryDataLayer layer, Geometry geom);

    /**
     * Attempts to read the maximum legal extent of data in a given data file.
     * If the dataLocation is not readable or does not correspond to a data
     * source that this LayerReadingMethod can read, then an
     * IllegalArgumentException will be thrown.
     *
     * @param geometryBuilder the GeometryBuilder from the current application
     *                        settings, used to determine the coordinate
     *                        system to use
     * @param dataLocation a String representing the location from which to
     *                     read the data layer data
     * @return a Geometry representing the maximum geographic extent for which
     *         data is available, transformed into the SMARTFIRE
     * @throws IllegalArgumentException if the dataLocation cannot be read
     */
    Geometry readExtent(GeometryBuilder geometryBuilder, String dataLocation) throws IllegalArgumentException;
}

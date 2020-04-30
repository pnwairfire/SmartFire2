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
package smartfire.util;

/**
 * Utility functions for converting area measurements between acres 
 * and square meters.
 */
public final class AreaUtil {
    private static final double ACRES_PER_SQUARE_METER = 0.000247105381;
    private static final double SQUARE_METERS_PER_ACRE = 4046.85642;
    private static final double SQUARE_METERS_PER_SQUARE_MILE = 2589988.11;

    private AreaUtil() {
    }

    /**
     * Convert an area as measured in acres into the equivalent area as
     * measured in square meters.
     * 
     * @param areaAcres the area, measured in acres
     * @return the area, measured in square meters
     */
    public static double acresToSquareMeters(double areaAcres) {
        return (areaAcres * SQUARE_METERS_PER_ACRE);
    }

    /**
     * Convert an area as measured in square miles into the equivalent area as
     * measured in square meters.
     * 
     * @param areaSqMiles the area, measured in square miles
     * @return the area, measured in square meters
     */
    public static double squareMilesToSquareMeters(double areaSqMiles) {
        return (areaSqMiles * SQUARE_METERS_PER_SQUARE_MILE);
    }

    /**
     * Convert an area as measured in square meters into the equivalent area
     * as measured in acres.
     * 
     * @param areaMeters the area, measured in square meters
     * @return the area, measured in acres
     */
    public static double squareMetersToAcres(double areaMeters) {
        return (areaMeters * ACRES_PER_SQUARE_METER);
    }
}

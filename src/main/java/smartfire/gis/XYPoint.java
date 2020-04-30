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

import java.awt.geom.Point2D;

/**
 * Simple XY Point implementation.
 *
 * Note: This class just inherits the implementation of
 * {@code java.awt.geom.Point2D.Double}.  This class mainly exists so that we
 * can add methods or other features in the future if desired, and to avoid
 * having to import a class from java.awt when we aren't actually using any
 * AWT features.
 */
public class XYPoint extends Point2D.Double {
    private static final long serialVersionUID = 1L;

    public XYPoint() {
        super();
    }

    public XYPoint(double x, double y) {
        super(x, y);
    }
}

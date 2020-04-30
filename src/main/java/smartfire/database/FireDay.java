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
package smartfire.database;

import com.vividsolutions.jts.geom.Geometry;
import org.joda.time.LocalDate;
import smartfire.gis.GeometryEntity;

/**
 * Represents the state of a given Fire on a particular date.
 */
public class FireDay implements GeometryEntity {
    private final Fire fire;
    private final LocalDate date;
    private final double area;
    private final Geometry shape;
    private final int numClumps;

    public FireDay(Fire fire, LocalDate date, double area, Geometry shape, int numClumps) {
        this.fire = fire;
        this.date = date;
        this.area = area;
        this.shape = shape;
        this.numClumps = numClumps;
    }

    public Fire getFire() {
        return fire;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public double getArea() {
        return area;
    }
    
    @Override
    public Geometry getShape() {
        return shape;
    }
    
    public int getNumClumps() {
        return numClumps;
    }
    
    @Override
    public String getShapeName() {
        return "";
    }
}

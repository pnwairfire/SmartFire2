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
import java.util.Collections;
import java.util.List;

/**
 * Represents a geometric entity created by the dissolve algorithm.
 *
 * @param <T> the type of Geometry entities that this entity was created from
 */
public class DissolvedEntity<T extends GeometryEntity> implements GeometryEntity {
    private final Geometry geom;
    private final List<T> derivedFrom;

    DissolvedEntity(Geometry geom, List<T> derivedFrom) {
        this.geom = geom;
        this.derivedFrom = Collections.unmodifiableList(derivedFrom);
    }

    @Override
    public Geometry getShape() {
        return geom;
    }

    @Override
    public String getShapeName() {
        return "Unknown";
    }

    public List<T> getDerivedFromEntities() {
        return derivedFrom;
    }
}

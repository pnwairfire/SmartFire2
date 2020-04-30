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

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import java.util.List;

/**
 * Represents a wrapper around a GeometryEntity that provides a different
 * shape.

 * @param <T> the type of GeometryEntity being wrapped
 */
public class GeometryWrapper<T extends GeometryEntity> implements GeometryEntity {
    private final T entity;
    private final Geometry shape;

    public GeometryWrapper(T entity, Geometry shape) {
        this.entity = entity;
        this.shape = shape;
    }

    public static <E extends GeometryEntity> GeometryWrapper<E> wrap(E entity, Geometry shape) {
        return new GeometryWrapper<E>(entity, shape);
    }

    public static <E extends GeometryEntity> List<E> unwrapAll(Iterable<GeometryWrapper<E>> wrappedList) {
        List<E> result = Lists.newArrayList();
        for(GeometryWrapper<E> wrapper : wrappedList) {
            result.add(wrapper.getEntity());
        }
        return result;
    }

    public T getEntity() {
        return entity;
    }

    @Override
    public Geometry getShape() {
        return shape;
    }

    @Override
    public String getShapeName() {
        return "Unknown";
    }
}

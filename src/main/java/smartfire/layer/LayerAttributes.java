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

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import java.util.Collections;
import java.util.Map;
import smartfire.database.SummaryDataLayer;
import smartfire.gis.GeometryEntity;
import smartfire.gis.ShapeAttributes;

/**
 * Represents a set of attributes returned for a given Geometry from a
 * LayerReadingMethod.  It functions as a String Map of attributes, with
 * some additional useful members.
 */
public class LayerAttributes
        extends ForwardingMap<String, String>
        implements Map<String, String>,
        GeometryEntity {
    private final Map<String, String> attributes;
    private final SummaryDataLayer layer;
    private final Geometry geometry;
    private final double representativeFraction;

    /**
     * Constructs a new LayerAttributes object.
     * 
     * @param attributes the attributes from the layer corresponding to the 
     *                   region represented by the given geometry
     * @param layer the SummaryDataLayer that was intersected with the Geometry
     * @param geometry the Geometry that was intersected with the layer
     * @param representativeFraction the fraction of the geometry that is
     *                               represented by the given attributes
     */
    public LayerAttributes(
            Map<String, String> attributes,
            SummaryDataLayer layer,
            Geometry geometry,
            double representativeFraction) {
        this.attributes = ImmutableMap.copyOf(attributes);
        this.layer = layer;
        this.geometry = geometry;
        this.representativeFraction = representativeFraction;
    }
    
    public LayerAttributes(ShapeAttributes shapeAttributes, SummaryDataLayer layer) {
        this.attributes = shapeAttributes.getAttributes();
        this.layer = layer;
        this.geometry = shapeAttributes.getShape();
        this.representativeFraction = shapeAttributes.getRepresentativeFraction();
    }

    public static LayerAttributes emptyIntersection(SummaryDataLayer layer, Geometry geometry) {
        Map<String, String> attributes = Collections.emptyMap();
        return new LayerAttributes(attributes, layer, geometry, 0.0);
    }

    @Override
    protected Map<String, String> delegate() {
        return attributes;
    }

    /**
     * Gets the geometric shape that was intersected with the given
     * SummaryDataLayer to produce these attributes.
     * 
     * @return the associated Geometry object
     */
    @Override
    public Geometry getShape() {
        return geometry;
    }

    @Override
    public String getShapeName() {
        return layer.getName();
    }

    /**
     * Returns the SummaryDataLayer that these attributes were extracted from.
     * 
     * @return the associated SummaryDataLayer object
     */
    public SummaryDataLayer getSummaryDataLayer() {
        return layer;
    }

    /**
     * Gets the fraction of area by which the associated Geometry object 
     * intersected the region in the SummaryDataLayer that supplied the 
     * associated Map of attributes.
     * 
     * <p>This can happen, for instance, if the requested Geometry intersects
     * several regions with different attributes.  If possible, data from all
     * the regions can be merged into a single set of attributes.  But in most
     * cases this is not easily possible, and so the attributes associated 
     * with the region that produces the greatest intersection with the 
     * requested Geometry should be used instead.  In this case, since the
     * attributes only represent some fraction of the overall event 
     * represented by the Geometry, this method will return that fraction as
     * a value between 0 and 1.
     * 
     * @return the fraction of the Geometry that these attributes apply to
     */
    public double getRepresentativeFraction() {
        return representativeFraction;
    }
}

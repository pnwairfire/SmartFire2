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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import smartfire.SmartfireException;
import smartfire.database.SummaryDataLayer;
import smartfire.gis.GeometryBuilder;

/**
 * Utility methods for working with SummaryDataLayer data.
 */
public final class Layers {
    private static final Map<String, LayerReadingMethod> layerReadingMethods;
    
    static {
        layerReadingMethods = Maps.newHashMap();
        ServiceLoader<LayerReadingMethod> serviceLoader = ServiceLoader.load(LayerReadingMethod.class);
        for(LayerReadingMethod method : serviceLoader) {
            layerReadingMethods.put(method.getClass().getName(), method);
        }
    }
    
    private Layers() { }
    
    /**
     * Read a LayerAttributes object containing String attributes from the 
     * given SummaryDataLayer for the region that intersects the given 
     * Geometry.
     * 
     * @param geometryBuilder the GeometryBuilder from the current application
     *                        settings, used to determine the coordinate
     *                        system to use
     * @param layer the SummaryDataLayer to read
     * @param geom the Geometry of interest
     * @return a LayerAttributes object
     */
    public static LayerAttributes readAttributes(GeometryBuilder geometryBuilder, SummaryDataLayer layer, Geometry geom) {
        LayerReadingMethod method = layerReadingMethods.get(layer.getLayerReadingMethod());
        if(method == null) {
            throw new SmartfireException("No such LayerReadingMethod \"" + layer.getLayerReadingMethod() + "\"");
        }

        if(!layer.getExtent().intersects(geom)) {
            return LayerAttributes.emptyIntersection(layer, geom);
        }

        return method.readAttributes(geometryBuilder, layer, geom);
    }

    /**
     * Determines the extent of available data in the given dataLocation, as
     * extracted by the named LayerReadingMethod.
     *
     * @param geometryBuilder the GeometryBuilder from the current application
     *                        settings, used to determine the coordinate
     *                        system to use
     * @param layerReadingMethod the name of the desired LayerReadingMethod
     * @param dataLocation the location (e.g. file path) of the data to read
     * @return a Geometry representing the extend of available data
     * @throws IllegalArgumentException if the dataLocation cannot be read
     */
    public static Geometry determineExtent(
            GeometryBuilder geometryBuilder,
            String layerReadingMethod,
            String dataLocation)
            throws IllegalArgumentException {
        LayerReadingMethod method = layerReadingMethods.get(layerReadingMethod);
        if(method == null) {
            throw new SmartfireException("No such LayerReadingMethod \"" + layerReadingMethod + "\"");
        }

        return method.readExtent(geometryBuilder, dataLocation);
    }
    
    /**
     * Get a list of Strings corresponding to the valid LayerReadingMethods.
     * 
     * @return the valid LayerReadingMethods
     */
    public static List<String> getLayerReadingMethods() {
        return ImmutableList.copyOf(layerReadingMethods.keySet());
    }
}

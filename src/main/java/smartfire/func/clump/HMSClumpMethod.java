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
package smartfire.func.clump;

import com.vividsolutions.jts.geom.Geometry;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.Clump;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.func.Attribute;
import smartfire.func.ClumpMethod;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.ShapeAttributes;
import smartfire.util.ShapefileUtil;

import java.util.Collection;
import java.util.Map;

/**
 * The HMS clumping method.
 */
@MetaInfServices
public class HMSClumpMethod implements ClumpMethod {
    private final double METERS_PER_ACRE = 4046.85642;
    private final double DEFAULT_AREA_PER_PIXEL = 100 * METERS_PER_ACRE; // 100 acres in meters
    private static final Logger log = LoggerFactory.getLogger(HMSClumpMethod.class);
    private final GeometryBuilder geometryBuilder;
    private final Source source;
    private final double clumpRadius;
    private final double pixelThreshold;
    private final String fireAreaShapeFile;
    private final String fireAreaAttributeName;

    public HMSClumpMethod(GeometryBuilder geometryBuilder, Source source,
            @Attribute(name = "clumpRadius",
            description = "Radius of the generated clump buffers (meters)") Double clumpRadius,
            @Attribute(name = "pixelThreshold",
            description = "Threshold at which to treat the area calculation as a \"small\" fire or a \"large\" fire (HMS detects)") Integer pixelThreshold,
            @Attribute(name = "fireAreaShapeFile",
            description = "Location of shapefile used to determine the fire area.") String fireAreaShapeFile,
            @Attribute(name = "fireAreaAttributeName",
            description = "Name of the attribute used to determine the fire area.") String fireAreaAttributeName) {
        this.geometryBuilder = geometryBuilder;
        this.source = source;
        this.clumpRadius = clumpRadius;
        this.pixelThreshold = pixelThreshold;
        this.fireAreaShapeFile = fireAreaShapeFile;
        this.fireAreaAttributeName = fireAreaAttributeName;
    }

    @Override
    public Collection<Clump> clump(Iterable<RawData> rawData) {
        DefaultClumpMethod clumpMethod = new DefaultClumpMethod(geometryBuilder, source, clumpRadius);
        Collection<Clump> result = clumpMethod.clump(rawData);
        for(Clump clump : result) {

            // Set area for each clump
            int pixelCount = clump.getRawData().size();
            double area = 0;
            if(pixelCount <= pixelThreshold) {
                double areaPerPixel = determineAreaPerPixel(clump.getShape());
                area = areaPerPixel * pixelCount;
            } else {
                area = clump.getShape().getArea();
            }
            clump.setArea(area);
        }
        return result;
    }
    
    private double determineAreaPerPixel(Geometry geom) {
        Map<String, String> attr = readShapeFile(geom);
        if(attr.containsKey(fireAreaAttributeName)) {
            String areaAcres = attr.get(fireAreaAttributeName);
            double areaMeters = Double.parseDouble(areaAcres) * METERS_PER_ACRE;
//            log.info("Found meters per pixel of {}.", areaMeters);
            return areaMeters;
        }
        return DEFAULT_AREA_PER_PIXEL;
    }

    private Map<String, String> readShapeFile(Geometry geom) {
        ShapeAttributes shapeAttributes = ShapefileUtil.readShapeFile(geometryBuilder, geom, fireAreaShapeFile);
        return shapeAttributes.getAttributes();
    }
}

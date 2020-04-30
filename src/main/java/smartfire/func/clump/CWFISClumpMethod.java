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

import java.util.Collection;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.Clump;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.func.Attribute;
import smartfire.func.ClumpMethod;
import smartfire.gis.GeometryBuilder;

/**
 * The CWFIS clumping method.
 */
@MetaInfServices
public class CWFISClumpMethod implements ClumpMethod {
    private static final Logger log = LoggerFactory.getLogger(CWFISClumpMethod.class);
    private final double METERS_PER_HECTARE = 10000;
    private final double clumpRadius;
    private final GeometryBuilder geometryBuilder;
    private final Source source;
    private final double pixelThreshold;

    public CWFISClumpMethod(GeometryBuilder geometryBuilder, Source source,
            @Attribute(name = "clumpRadius",
            description = "Radius of the generated clump buffers (meters)") Double clumpRadius,
            @Attribute(name = "pixelThreshold",
            description = "Threshold at which to treat the area calculation as a \"small\" fire or a \"large\" fire (CWFIS detects)") Integer pixelThreshold) {
        this.geometryBuilder = geometryBuilder;
        this.source = source;
        this.clumpRadius = clumpRadius;
        this.pixelThreshold = pixelThreshold;
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
                area = calculateEstimatedArea(clump.getRawData());
            } else {
                area = clump.getShape().getArea();
            }
            clump.setArea(area);
        }
        return result;
    }
    
    private double calculateEstimatedArea(Iterable<RawData> rawData) {
        double estAreaMeters = 0;
        for(RawData record : rawData) {
            String estAreaHectares = record.get("estarea");
            estAreaMeters += Double.parseDouble(estAreaHectares) * METERS_PER_HECTARE;
        }
        return estAreaMeters;
    }
}

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
package smartfire.func.assoc;

import org.kohsuke.MetaInfServices;
import smartfire.database.Clump;
import smartfire.database.Fire;
import smartfire.func.AssociationMethod;
import smartfire.func.Attribute;

/**
 * The CWFIS association method.
 */

@MetaInfServices(AssociationMethod.class)
public class CWFISAssociationMethod extends DefaultAssociationMethod {
    private final int pixelThreshold;
    
    public CWFISAssociationMethod(
            @Attribute(name="numForwardDays", 
                    description="Number of days forward in time to associate")
            Integer numForwardDays,
            @Attribute(name="numBackwardDays", 
                    description="Number of days backward in time to associate")
            Integer numBackwardDays,
            @Attribute(name="pixelThreshold",
                    description="Threshold at which to treat the area calculation as a \"small\" fire or a \"large\" fire (HMS detects)")
            Integer pixelThreshold,
            @Attribute(name="sizeThreshold", 
                    description="Threshold at which to treat this as a \"large\" fire instead of a \"small\" fire (square meters)")
            Double sizeThreshold,
            @Attribute(name="smallFireDistance", 
                    description="Association distance for \"small\" fires (meters)")
            Double smallFireDistance,
            @Attribute(name="largeFireDistance", 
                    description="Association distance for \"large\" fires (meters)")
            Double largeFireDistance
            ) {
        super(numForwardDays, numBackwardDays, sizeThreshold, smallFireDistance, largeFireDistance);
        this.pixelThreshold = pixelThreshold;
    }

    @Override
    protected double computeArea(Fire fire) {
        // Count All raw data records.
        int pixelCount = 0;
        double clumpArea = 0;
        for(Clump clump : fire.getClumps()) {
            pixelCount = pixelCount + clump.getRawData().size();
            clumpArea = clumpArea + clump.getArea();
        }
        
        if(pixelCount <= pixelThreshold) {
            return clumpArea;
        } else {
            return super.computeArea(fire);
        }
    }
}

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
package smartfire.func.firetype;

import com.google.common.collect.Maps;
import java.util.Map;
import org.kohsuke.MetaInfServices;
import smartfire.database.Clump;
import smartfire.database.Fire;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.func.FireTypeMethod;
import smartfire.func.Attribute;
import smartfire.gis.GeometryBuilder;

/**
 * The fire type method for FETS data (uses two fields to determine WF, Ag, Rx, or pile).
 */
@MetaInfServices
public class FETSFireTypeMethod implements FireTypeMethod {
    private final GeometryBuilder geometryBuilder;
    private final Source source;
    private final String sourceTypeField;
    private final String burnTypeField;

    public FETSFireTypeMethod(GeometryBuilder geometryBuilder, Source source,
            @Attribute(name = "sourceTypeField",
            description = "Fire type field (WF, Rx, Ag) for this source") String sourceTypeField,
            @Attribute(name = "burnTypeField",
            description = "Burn type field (broadcast, pile) for this source") String burnTypeField) {
        this.geometryBuilder = geometryBuilder;
        this.source = source;
        this.sourceTypeField = sourceTypeField;
        this.burnTypeField = burnTypeField;
    }

    @Override
    public String determineFireType(Fire fire) {
        // Count appearances of fireTypeField from the rawData
        Map<String, Integer> fireTypeCount = Maps.newHashMap();
        for(Clump clump : fire.getClumps()) {
            for(RawData rawData : clump.getRawData()) {
                String fireType = "";
                String burnType = "";
                String sourceType = "";
                if(rawData.containsKey(burnTypeField)) {
                    burnType = rawData.get(burnTypeField);
                }
                if(rawData.containsKey(sourceTypeField)) {
                    sourceType = rawData.get(sourceTypeField);
                }
                
                if("RX".equalsIgnoreCase(sourceType)) {
                    if("B".equals(burnType)) {
                        fireType = "RX";
                    } else {
                        fireType = "pile";
                    }                       
                } else {
                    fireType = sourceType;
                }
               
                if(!fireType.isEmpty()) {
                    if(fireTypeCount.containsKey(fireType)) {
                        fireTypeCount.put(fireType, fireTypeCount.get(fireType) + 1);
                    } else {
                        fireTypeCount.put(fireType, 1);
                    }
                }
            }
        }
        
        // Find the fire type which is most common
        String resultFireType = "NA";
        int resultCount = 0;
        for(String fireType : fireTypeCount.keySet()) {
            if(fireTypeCount.get(fireType) > resultCount) {
                resultFireType = fireType;
                resultCount = fireTypeCount.get(fireType);
            }
        }
        
        return resultFireType;
    }
}

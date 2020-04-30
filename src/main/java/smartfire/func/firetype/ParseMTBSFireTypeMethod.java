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
import smartfire.func.Attribute;
import smartfire.func.FireTypeMethod;
import smartfire.gis.GeometryBuilder;

/**
 * This method looks for specific stings within a shapefile attribute to assign type RX.
 * It is hard coded for MTBS, but should be generalized at some point.
 */
@MetaInfServices
public class ParseMTBSFireTypeMethod implements FireTypeMethod {
    private final GeometryBuilder geometryBuilder;
    private final Source source;
    private final String fireTypeField;

    public ParseMTBSFireTypeMethod(GeometryBuilder geometryBuilder, Source source,
            @Attribute(name = "fireTypeField",
            description = "Field that contains fire type info for this source") String fireTypeField) {
        this.geometryBuilder = geometryBuilder;
        this.source = source;
        this.fireTypeField = fireTypeField;
    }

    @Override
    public String determineFireType(Fire fire) {
        // Count appearances of fireTypeField from the rawData
        Map<String, Integer> fireTypeCount = Maps.newHashMap();
        for(Clump clump : fire.getClumps()) {
            for(RawData rawData : clump.getRawData()) {
                String fireType = "N/A";
                String attributeValue = "";
                if(rawData.containsKey(fireTypeField)) {
                    attributeValue = rawData.get(fireTypeField);
                    if(attributeValue.indexOf("RX") == -1 && attributeValue.indexOf("UNNAMED") == -1) {
                        fireType = "WF";
                    } else {
                        fireType = "RX";
                    }
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

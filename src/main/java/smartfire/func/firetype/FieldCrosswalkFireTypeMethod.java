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
import java.util.HashMap;
import org.kohsuke.MetaInfServices;
import smartfire.database.Clump;
import smartfire.database.Fire;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.func.FireTypeMethod;
import smartfire.func.Attribute;
import smartfire.gis.GeometryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default fire type method.
 */
@MetaInfServices
public class FieldCrosswalkFireTypeMethod implements FireTypeMethod {
    private static final Logger log = LoggerFactory.getLogger(FieldCrosswalkFireTypeMethod.class);
    private final GeometryBuilder geometryBuilder;
    private final Source source;
    private final String fireTypeField;
    private final Map<String, String> fireTypeMap;

    public FieldCrosswalkFireTypeMethod(GeometryBuilder geometryBuilder, Source source,
            @Attribute(name = "fireTypeField",
            description = "Fire type field for this source") String fireTypeField,
            @Attribute(name = "fireTypeCrosswalk",
            description = "List of strings for WF, RX, and pile. First code listed will be default.  Format WF:accident;lightning!RX:broadcast!pile:hand pile;machine")
            String fireTypeCrosswalk) {
        this.geometryBuilder = geometryBuilder;
        this.source = source;
        this.fireTypeField = fireTypeField;
        this.fireTypeMap = parseCrosswalk(fireTypeCrosswalk); // Decode the crosswalk string
    }

    @Override
    public String determineFireType(Fire fire) {
        // Count appearances of fireTypeField from the rawData
        Map<String, Integer> fireTypeCount = Maps.newHashMap();
        for(Clump clump : fire.getClumps()) {
            for(RawData rawData : clump.getRawData()) {
                String fireCode = "";
                String fireType = "";
                if(rawData.containsKey(fireTypeField)) {
                    fireCode = rawData.get(fireTypeField).toLowerCase().trim();
                    if(fireTypeMap.containsKey(fireCode)) {
                        fireType = fireTypeMap.get(fireCode);
                    } else {
                        log.warn("Record has uncoded fire type: {}; using default", fireCode);
                        fireType = fireTypeMap.get("default");
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
       
    //Format WF:accident;lightning!RX:broadcast!pile:hand pile;machine
    static Map<String, String> parseCrosswalk(String crosswalkString) {
        Map<String, String> typeCrosswalk = new HashMap<String, String>();
        String[] allTypes = crosswalkString.split("!");
        for(String fireTypes : allTypes) {
            String[] typeAndCodes = fireTypes.split(":");
            String fireType = typeAndCodes[0];
            String fireCodes[] = typeAndCodes[1].split(";");
            for(String fireCode : fireCodes) {
                typeCrosswalk.put(fireCode.toLowerCase(), fireType);
            }
        }
        String defaultType = crosswalkString.split(":")[0];
        typeCrosswalk.put("default", defaultType);

        log.debug("Parsed fireTypeCrosswalk mapping: {}", typeCrosswalk.toString());
        return typeCrosswalk;
    }
}

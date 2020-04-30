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
package smartfire.fileimport;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IRWINJSONParser extends AbstractParser {
    private final Logger log = LoggerFactory.getLogger(IRWINJSONParser.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public IRWINJSONParser(JsonNode jsonData) throws Exception {
        parse(jsonData);
    }

    private void parse(JsonNode jsonData) throws Exception  {
        log.info("Parsing Esri JSON data.");

        // Iterate through all incidents and collect their data
        ArrayNode incidents = (ArrayNode) jsonData.get("incidents");
        if (incidents.size() == 0) {
            fieldNames = new String[0];
            return;
        }
        for (JsonNode node : incidents) {
            Map<String, Object> attributesData = mapper.readValue(node.get("attributes"), Map.class);

            // Pull together incident field names
            Set<String> fields = new LinkedHashSet<String>(attributesData.keySet());
            fields.add("shapeData"); // Extra field to hold onto incident shape data
            fieldNames = fields.toArray(new String[fields.size()]);

            // Read shape data from json
            Map<String, Object> shapeData = new HashMap<String, Object>();
            if (node.has("pointOfOrigin")) { // TBD: Is this true only for Point geometries, or will it be true for other types as well?
                Map<String, Object> pointOfOriginData = mapper.readValue(node.get("pointOfOrigin"), Map.class);
                shapeData.put("pointOfOrigin", pointOfOriginData);
            }

            // Extract incident field data
            Object[] dataValues = new Object[fieldNames.length];
            int i = 0;
            for (String key : attributesData.keySet()) {
                dataValues[i] = attributesData.get(key);
                i++;
            }
            dataValues[i] = shapeData; // Slip incident shape data into extra field
            data.add(dataValues);
        }
    }
}

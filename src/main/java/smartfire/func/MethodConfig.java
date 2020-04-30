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
package smartfire.func;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents configuration information for a Method.
 */
public class MethodConfig {
    private final Map<String, MethodAttribute> attrs;
    
    public MethodConfig(List<MethodAttribute> attributes) {
        this.attrs = Maps.newHashMap();
        for(MethodAttribute attr : attributes) {
            this.attrs.put(attr.getName(), attr);
        }
    }
    
    public Set<String> getAttributeNames() {
        return attrs.keySet();
    }
    
    public void setAttribute(String attributeName, String attributeValue) {
        MethodAttribute attr = attrs.get(attributeName);
        if(attr != null) {
            attr.setValue(attributeValue);
        }
    }
    
    public String getAttributeDescription(String attributeName) {
        MethodAttribute attr = attrs.get(attributeName);
        if(attr == null) {
            return null;
        }
        return attr.getDescription();
    }
}

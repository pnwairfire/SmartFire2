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

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import junit.framework.TestCase;
import smartfire.database.Source;
import smartfire.func.AssociationMethod;
import smartfire.func.MethodConfig;
import smartfire.func.Methods;

public class DefaultAssociationMethodTest extends TestCase {
    private static final String METHOD_NAME = DefaultAssociationMethod.class.getName();
    
    public DefaultAssociationMethodTest(String testName) {
        super(testName);
    }

    public void testConfig() {
        MethodConfig config = Methods.getAssociationMethodConfig(METHOD_NAME);
        assertEquals(5, config.getAttributeNames().size());
        Set<String> expectedAttrNames = ImmutableSet.of(
                "numForwardDays",
                "numBackwardDays",
                "sizeThreshold",
                "smallFireDistance",
                "largeFireDistance");
        assertEquals(expectedAttrNames, config.getAttributeNames());
    }
    
    public void testConstruct() {
        Source source = new Source();
        source.setAssocMethod(METHOD_NAME);
        source.put("numForwardDays", "0");
        source.put("numBackwardDays", "2");
        source.put("sizeThreshold", "1767150");
        source.put("largeFireDistance", "750.0");
        source.put("smallFireDistance", "500.0");
        AssociationMethod method = Methods.newAssociationMethod(source);
        assertTrue(method instanceof DefaultAssociationMethod);
    }
}

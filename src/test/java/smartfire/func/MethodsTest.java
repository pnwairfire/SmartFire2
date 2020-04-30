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

import java.util.List;
import junit.framework.TestCase;

public class MethodsTest extends TestCase {
    public MethodsTest(String testName) {
        super(testName);
    }

    public void testFetchMethods() {
        List<String> methods = Methods.getFetchMethods();
        assertTrue(methods.contains("smartfire.func.fetch.HMSFetchMethod"));
        assertTrue(methods.contains("smartfire.func.fetch.ICS209FetchMethod"));
    }
    
    public void testClumpMethods() {
        List<String> methods = Methods.getClumpMethods();
        assertTrue(methods.contains("smartfire.func.clump.DefaultClumpMethod"));
    }
    
    public void testAssocMethods() {
        List<String> methods = Methods.getAssociationMethods();
        assertTrue(methods.contains("smartfire.func.assoc.DefaultAssociationMethod"));
    }
    
    public void testProbMethods() {
        List<String> methods = Methods.getProbabilityMethods();
        assertTrue(methods.contains("smartfire.func.probability.DefaultProbabilityMethod"));
    }
    
    public void testReconcileMethods() {
        List<String> methods = Methods.getReconciliationMethods();
        assertTrue(methods.contains("smartfire.func.reconcile.DefaultReconciliationMethod"));
    }
}

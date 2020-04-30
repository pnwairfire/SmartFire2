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
package smartfire.func.probability;

import smartfire.func.MethodConfig;
import smartfire.database.Fire;
import smartfire.database.DefaultWeighting;
import smartfire.func.ProbabilityMethod;
import smartfire.database.Source;
import junit.framework.TestCase;
import smartfire.func.Methods;
import static org.mockito.Mockito.*;

public class DefaultProbabilityMethodTest extends TestCase {
    private static final String METHOD_NAME = DefaultProbabilityMethod.class.getName();
    
    public DefaultProbabilityMethodTest(String testName) {
        super(testName);
    }

    public void testConstruct() {
        Source source = new Source();
        source.setProbabilityMethod(METHOD_NAME);
        ProbabilityMethod method = Methods.newProbabilityMethod(source);
        assertTrue(method instanceof DefaultProbabilityMethod);
    }
    
    public void testConfig() {
        MethodConfig config = Methods.getProbabilityMethodConfig(METHOD_NAME);
        assertTrue(config.getAttributeNames().isEmpty());
    }
    
    public void testProbabilityMethod() {
        Source source = new Source();
        source.setProbabilityMethod(METHOD_NAME);
        DefaultWeighting weighting = new DefaultWeighting();
        weighting.setFalseAlarmRate(0.2);
        source.setDefaultWeighting(weighting);
        
        ProbabilityMethod method = new DefaultProbabilityMethod(source);
        
        Fire fire = mock(Fire.class);
        assertEquals(0.8, method.calculateFireProbability(fire));
    }
}

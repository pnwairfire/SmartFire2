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
package smartfire.util;

import junit.framework.TestCase;

public class AreaUtilTest extends TestCase {
    private final double CLOSE_ENOUGH = 0.000001;
    
    public AreaUtilTest(String testName) {
        super(testName);
    }

    public void testRoundTrip() {
        final double VALUE = 100.0;
        double roundTrip1 = AreaUtil.acresToSquareMeters(AreaUtil.squareMetersToAcres(VALUE));
        assertEquals(VALUE, roundTrip1, CLOSE_ENOUGH);
        double roundTrip2 = AreaUtil.squareMetersToAcres(AreaUtil.acresToSquareMeters(VALUE));
        assertEquals(VALUE, roundTrip2, CLOSE_ENOUGH);
    }
}

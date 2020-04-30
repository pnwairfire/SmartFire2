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
package smartfire.func.clump;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import smartfire.Config;
import smartfire.database.Clump;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.func.ClumpMethod;
import smartfire.func.MethodConfig;
import smartfire.func.Methods;
import smartfire.gis.GeometryBuilder;

public class DefaultClumpMethodTest extends TestCase {
    private static final String METHOD_NAME = DefaultClumpMethod.class.getName();
    private GeometryBuilder builder;
    private DateTime startDate;
    private DateTime endDate;
    
    public DefaultClumpMethodTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        this.builder = new GeometryBuilder(new Config());
        this.startDate = new DateTime().toLocalDate().toDateTimeAtStartOfDay();
        this.endDate = startDate.plusDays(1).minusMillis(1);
    }

    public void testConfig() {
        MethodConfig config = Methods.getClumpMethodConfig(METHOD_NAME);
        assertEquals(1, config.getAttributeNames().size());
        
        String[] attrNames = config.getAttributeNames().toArray(new String[1]);
        assertEquals("clumpRadius", attrNames[0]);
    }
    
    public void testConstruct() {
        Source source = new Source();
        source.setClumpMethod(METHOD_NAME);
        source.put("clumpRadius", "800.0");
        ClumpMethod method = Methods.newClumpMethod(builder, source);
        assertTrue(method instanceof DefaultClumpMethod);
    }
    
    public void testClumpOneClump() {
        ClumpMethod method = new DefaultClumpMethod(builder, new Source(), 5.0);
        List<RawData> rawData = Lists.newArrayList();
        rawData.add(buildRawDataAt(10, 10));
        rawData.add(buildRawDataAt(10, 15));
        rawData.add(buildRawDataAt(15, 15));
        rawData.add(buildRawDataAt(15, 10));
        rawData.add(buildRawDataAt(20, 20));
        Collection<Clump> result = method.clump(rawData);
        Clump[] clumps = result.toArray(new Clump[0]);
        
        assertEquals(1, result.size());
        Clump clump = clumps[0];
        assertEquals(5, clump.getRawData().size());
        assertEquals(new HashSet<RawData>(rawData), new HashSet<RawData>(clump.getRawData()));
    }
    
    public void testClumpTwoClumps() {
        ClumpMethod method = new DefaultClumpMethod(builder, new Source(), 5.0);
        List<RawData> rawData = Lists.newArrayList();
        rawData.add(buildRawDataAt(10, 10));
        rawData.add(buildRawDataAt(10, 15));
        rawData.add(buildRawDataAt(15, 15));
        
        rawData.add(buildRawDataAt(25, 20));
        rawData.add(buildRawDataAt(30, 20));
        Collection<Clump> result = method.clump(rawData);
        Clump[] clumps = result.toArray(new Clump[0]);
        
        assertEquals(2, result.size());
        Clump clump1 = clumps[0];
        assertEquals(3, clump1.getRawData().size());
        
        Clump clump2 = clumps[1];
        assertEquals(2, clump2.getRawData().size());
    }
    
    public void testClumpDates() {
        ClumpMethod method = new DefaultClumpMethod(builder, new Source(), 5.0);
        List<RawData> rawData = Lists.newArrayList();
        
        DateTime earlierDate = startDate.minusDays(2);
        
        rawData.add(buildRawData(10, 15, earlierDate.plusDays(1)));
        rawData.add(buildRawData(10, 10, earlierDate));
        rawData.add(buildRawData(15, 15, startDate));
        Collection<Clump> result = method.clump(rawData);
        Clump[] clumps = result.toArray(new Clump[0]);
        
        assertEquals(1, result.size());
        Clump clump = clumps[0];
        assertEquals(3, clump.getRawData().size());
        assertEquals(earlierDate.withZone(DateTimeZone.UTC), clump.getStartDateTime());
        assertEquals(endDate.withZone(DateTimeZone.UTC), clump.getEndDateTime());
    }
    
    private RawData buildRawDataAt(double x, double y) {
        RawData result = new RawData();
        result.setShape(builder.buildPoint(x, y));
        result.setArea(0);
        result.setStartDate(startDate);
        result.setEndDate(endDate);
        return result;
    }
    
    private RawData buildRawData(double x, double y, DateTime dt) {
        RawData result = buildRawDataAt(x, y);
        result.setStartDate(dt.toLocalDate().toDateTimeAtStartOfDay());
        result.setEndDate(result.getStartDateTime().plusDays(1).minusMillis(1));
        return result;
    }
}

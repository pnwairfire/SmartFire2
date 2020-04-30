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
import com.vividsolutions.jts.geom.Polygon;
import java.util.Collection;
import java.util.List;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.Clump;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.func.ClumpMethod;
import smartfire.gis.GeometryBuilder;

/**
 * A ClumpMethod designed for basic point data (all fires are assumed unique and this method calculates a polygon).
 */
@MetaInfServices
public class SimpleDataClumpMethod implements ClumpMethod {
    private static final Logger log = LoggerFactory.getLogger(SimpleDataClumpMethod.class);
    private final GeometryBuilder geometryBuilder;
    private final Source source;

    public SimpleDataClumpMethod(GeometryBuilder geometryBuilder, Source source) {
        this.geometryBuilder = geometryBuilder;
        this.source = source;
    }

    @Override
    public Collection<Clump> clump(Iterable<RawData> rawData) {
        List<Clump> result = Lists.newArrayList();
        for(RawData rec : rawData) {

            Clump clump = new Clump();
            clump.setSource(source);

            // Compute a polygon for the clump.  We take the reported area
            // and calculate a circle of that area centered on the point.
            double radius = Math.sqrt(rec.getArea() / Math.PI);
            Polygon poly = (Polygon) rec.getShape().buffer(radius);
            clump.setShape(poly);

            clump.setArea(rec.getArea());
            clump.setStartDate(rec.getStartDateTime());
            clump.setEndDate(rec.getEndDateTime());

            clump.addRawDataRecord(rec);

            result.add(clump);
        }
        return result;
    }
}

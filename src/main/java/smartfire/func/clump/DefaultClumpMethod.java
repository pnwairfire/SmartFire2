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
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import java.util.Collection;
import java.util.List;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import smartfire.SmartfireException;
import smartfire.database.Clump;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.func.Attribute;
import smartfire.func.ClumpMethod;
import smartfire.gis.Dissolve;
import smartfire.gis.DissolvedEntity;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.GeometryWrapper;

/**
 * The default clumping method.
 */
@MetaInfServices
public final class DefaultClumpMethod implements ClumpMethod {
    private final GeometryBuilder geometryBuilder;
    private final Source source;
    private final double clumpRadius;

    public DefaultClumpMethod(GeometryBuilder geometryBuilder, Source source,
            @Attribute(name = "clumpRadius", description = "Radius of the generated clump buffers (meters)") Double clumpRadius) {
        this.geometryBuilder = geometryBuilder;
        this.source = source;
        this.clumpRadius = clumpRadius;
    }

    @Override
    public Collection<Clump> clump(Iterable<RawData> rawData) {
        double bufferRadius = this.clumpRadius;
        double areaPerPixel = Math.PI * (bufferRadius * bufferRadius);

        // First step: buffer all the RawData points
        List<GeometryWrapper<RawData>> bufferedData = Lists.newArrayList();
        for(RawData record : rawData) {
            Geometry bufferedShape = record.getShape().buffer(bufferRadius);
            GeometryWrapper<RawData> wrapper = GeometryWrapper.wrap(record, bufferedShape);
            bufferedData.add(wrapper);
        }

        // Second step: dissolve the buffered data
        List<DissolvedEntity<GeometryWrapper<RawData>>> dissolvedData = Dissolve.dissolve(bufferedData);

        // Third step: create Clump objects from the newly dissolved shapes
        List<Clump> result = Lists.newArrayList();
        for(DissolvedEntity<GeometryWrapper<RawData>> entity : dissolvedData) {
            Clump clump = new Clump();
            clump.setSource(source);

            Geometry shape = entity.getShape();
            if(shape instanceof Polygon) {
                clump.setShape((Polygon) shape);
            } else {
                throw new SmartfireException("Result of dissolve is not a polygon!");
            }

            List<RawData> derivedFrom = GeometryWrapper.unwrapAll(entity.getDerivedFromEntities());
            clump.addRawDataRecords(derivedFrom);

            // Figure out the start and end date by scanning through the RawData records
            DateTime startDate = null;
            DateTime endDate = null;
            for(RawData record : derivedFrom) {
                if(startDate == null) {
                    startDate = record.getStartDateTime();
                } else if(record.getStartDateTime().isBefore(startDate)) {
                    startDate = record.getStartDateTime();
                }
                if(endDate == null) {
                    endDate = record.getEndDateTime();
                } else if(record.getEndDateTime().isAfter(endDate)) {
                    endDate = record.getEndDateTime();
                }
            }
            clump.setStartDate(startDate);
            clump.setEndDate(endDate);

            // Figure out the area of this clump
            int numPixels = derivedFrom.size();
            double area = areaPerPixel * numPixels;
            clump.setArea(area);

            result.add(clump);
        }
        return result;
    }
}

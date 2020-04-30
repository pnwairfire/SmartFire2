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

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import java.util.List;
import java.util.Map;
import org.kohsuke.MetaInfServices;
import smartfire.database.Fire;
import smartfire.database.Source;
import smartfire.func.Attribute;
import smartfire.func.FireTypeMethod;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.ShapeAttributes;
import smartfire.util.ShapefileUtil;

/**
 * This fire type method uses a shapefile with two attributes.  One for ag fires (1 is ag),
 * and one for the wildfire season (given as a list of month numbers (e.g., 4,5,6).
 */
@MetaInfServices
public class TimePeriodPlusAgFireTypeMethod implements FireTypeMethod {
    private final GeometryBuilder geometryBuilder;
    private final Source source;
    private final String fireTypeShapeFile;
    private final String wildFireTimeFrameAttributeName;
    private final String agFireAttributeName;

    public TimePeriodPlusAgFireTypeMethod(GeometryBuilder geometryBuilder, Source source,
            @Attribute(name = "fireTypeShapeFile",
            description = "Location of shapefile used to determine the fire type.") String fireTypeShapeFile,
            @Attribute(name = "wildFireTimeFrameAttributeName",
            description = "Name of the attribute used to determine the fire type.") String wildFireTimeFrameAttributeName, 
            @Attribute(name = "agFireAttributeName",
            description = "Name of the attribute used to assign ag fire type.") String agFireAttributeName) {
        this.geometryBuilder = geometryBuilder;
        this.source = source;
        this.fireTypeShapeFile = fireTypeShapeFile;
        this.wildFireTimeFrameAttributeName = wildFireTimeFrameAttributeName;
        this.agFireAttributeName = agFireAttributeName;
    }

    @Override
    public String determineFireType(Fire fire) {
        int fireStartMonth = fire.getStartDateTime().getMonthOfYear();
        Map<String, String> attr = readShapeFile(fire.getShape());
        if(attr.containsKey(agFireAttributeName)) {
            String agFire = attr.get(agFireAttributeName);
            if (agFire.equals("1")) {
                return "Ag";
            }
        }
        if(attr.containsKey(wildFireTimeFrameAttributeName)) {
            String wildFireTimeFrame = attr.get(wildFireTimeFrameAttributeName);
            String[] rawWildFireMonths = wildFireTimeFrame.split(",");

            List<Integer> wildFireMonths = Lists.newArrayList();
            for(int i = 0; i < rawWildFireMonths.length; i++) {
                if(!rawWildFireMonths[i].isEmpty()) {
                    wildFireMonths.add(Integer.parseInt(rawWildFireMonths[i]));
                }
            }

            for(Integer month : wildFireMonths) {
                if(fireStartMonth == month) {
                    return "WF";
                }
            }
        }
        return "Rx";
    }

    private Map<String, String> readShapeFile(Geometry geom) {
        ShapeAttributes shapeAttributes = ShapefileUtil.readShapeFile(geometryBuilder, geom, fireTypeShapeFile);
        return shapeAttributes.getAttributes();
    }
}

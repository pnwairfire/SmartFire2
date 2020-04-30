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

import org.kohsuke.MetaInfServices;
import smartfire.database.Fire;
import smartfire.database.Source;
import smartfire.func.Attribute;
import smartfire.func.FireTypeMethod;
import smartfire.gis.GeometryBuilder;

/**
 * The default fire type method.
 */
@MetaInfServices
public class DefaultFireTypeMethod implements FireTypeMethod {
    private final GeometryBuilder geometryBuilder;
    private final Source source;
    private final String fireType;

    public DefaultFireTypeMethod(GeometryBuilder geometryBuilder, Source source,
            @Attribute(name = "fireType",
            description = "Fire type for this source") String fireType) {
        this.geometryBuilder = geometryBuilder;
        this.source = source;
        this.fireType = fireType;
    }

    @Override
    public String determineFireType(Fire fire) {
        return fireType;
    }
}

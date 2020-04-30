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

import java.util.Map;
import org.kohsuke.MetaInfServices;
import smartfire.database.Source;
import smartfire.func.BuiltInMethodsFactory;
import smartfire.func.ClumpMethod;
import smartfire.func.ClumpMethodFactory;
import smartfire.gis.GeometryBuilder;

/**
 * Factory for constructing built-in ClumpMethod instances.
 */
@MetaInfServices(ClumpMethodFactory.class)
public class BuiltInClumpMethods extends BuiltInMethodsFactory<ClumpMethod> 
        implements ClumpMethodFactory {

    public BuiltInClumpMethods() {
        super(ClumpMethod.class);
    }

    @Override
    public ClumpMethod newClumpMethod(String methodName, GeometryBuilder geometryBuilder, Source source) {
        Map<String, String> attributes = source;
        return this.construct(methodName, new Object[] { geometryBuilder, source }, attributes);
    }
}

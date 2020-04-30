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
package smartfire.func.fetch;

import com.google.common.collect.Maps;
import java.util.Map;
import org.kohsuke.MetaInfServices;
import smartfire.database.ScheduledFetch;
import smartfire.func.BuiltInMethodsFactory;
import smartfire.func.FetchMethod;
import smartfire.func.FetchMethodFactory;
import smartfire.gis.GeometryBuilder;

/**
 * Factory for constructing default FetchMethod instances.
 */
@MetaInfServices(FetchMethodFactory.class)
public class BuiltInFetchMethods extends BuiltInMethodsFactory<FetchMethod> 
        implements FetchMethodFactory {
    
    public BuiltInFetchMethods() {
        super(FetchMethod.class);
    }

    @Override
    public FetchMethod newFetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder) {
        // For backwards compatabilty, populate the attributes from both the 
        // Source and the ScheduledFetch.
        Map<String, String> attributes = Maps.newHashMap();
        attributes.putAll(scheduledFetch.getSource());
        attributes.putAll(scheduledFetch);
        
        return this.construct(scheduledFetch.getFetchMethod(),
                new Object[] { scheduledFetch, geometryBuilder }, 
                attributes);
    }
}

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
package smartfire.func.assoc;

import java.util.Map;
import org.kohsuke.MetaInfServices;
import smartfire.database.Source;
import smartfire.func.AssociationMethod;
import smartfire.func.AssociationMethodFactory;
import smartfire.func.BuiltInMethodsFactory;

/**
 * Factory for constructing built-in AssociationMethod instances.
 */
@MetaInfServices(AssociationMethodFactory.class)
public class BuiltInAssociationMethods extends BuiltInMethodsFactory<AssociationMethod>
        implements AssociationMethodFactory {

    public BuiltInAssociationMethods() {
        super(AssociationMethod.class);
    }
    
    @Override
    public AssociationMethod newAssociationMethod(String methodName, Source source) {
        Map<String, String> attributes = source;
        return this.construct(methodName, new Object[] { source }, attributes);
    }
}

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

import java.util.List;
import java.util.Map;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.Clump;
import smartfire.database.Fire;
import smartfire.database.RawData;
import smartfire.func.AssociationMethod;
import smartfire.gis.QueryableFireSet;

/**
 * An AssociationMethod designed for FACTS data.
 */
@MetaInfServices
public class FACTSAssociationMethod implements AssociationMethod {
    private static final Logger log = LoggerFactory.getLogger(ICS209AssociationMethod.class);

    public FACTSAssociationMethod() {
    }

    @Override
    public void associate(Clump clump, QueryableFireSet fireSet) {
        // Association method is essentially a non-operation method.

        // Get all Raw Data for each clump. Ensure clump has raw data associated with it.
        List<RawData> rawData = clump.getRawData();
        if(rawData == null || rawData.isEmpty()) {
            log.warn("Clump #{} does not have any associated RawData; ignoring",
                    clump.getId());
            return;
        }
        RawData factsReport = rawData.get(0);

        // Create a clump for each fire
        Fire result = new Fire();
        result.setSource(clump.getSource());
        fireSet.add(result);

        // Add this clump to the set of clumps associated with the given Fire
        result.addClump(clump);
        
        // Set area
        result.setArea(factsReport.getArea());

        // Merge FACTS data attributes into Fire attributes
        for(Map.Entry<String, String> entry : factsReport.entrySet()) {
            if(!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
    }
}

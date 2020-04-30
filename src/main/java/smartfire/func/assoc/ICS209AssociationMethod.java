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

import com.google.common.base.Predicate;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.Clump;
import smartfire.database.Fire;
import smartfire.database.RawData;
import smartfire.func.AssociationMethod;
import smartfire.func.Attribute;
import smartfire.gis.QueryableFireSet;

/**
 * An AssociationMethod designed for ICS-209 data.
 */
@MetaInfServices
public class ICS209AssociationMethod implements AssociationMethod {
    private static final Logger log = LoggerFactory.getLogger(ICS209AssociationMethod.class);
    private final int numForwardDays;
    private final int numBackwardDays;
    private final String associationField;
    
    public ICS209AssociationMethod(
            @Attribute(name="numForwardDays", 
                    description="Number of days forward in time to associate")
            Integer numForwardDays,
            @Attribute(name="numBackwardDays", 
                    description="Number of days backward in time to associate")
            Integer numBackwardDays,
            @Attribute(name="associationField",
                    description="The name of the field of the ICS-209 report that should be used to associate records")
            String associationField
            ) {
        this.numForwardDays = numForwardDays;
        this.numBackwardDays = numBackwardDays;
        this.associationField = associationField;
    }
    
    @Override
    public void associate(Clump clump, QueryableFireSet fireSet) {
        // Figure out how close (in time and in space) a fire would have to be
        // to this clump to be considered associated.  This distance may depend
        // on the clump itself.  (In this class it's a pretty dead simple
        // formula, but we expect that there may be subclasses of this class
        // that want to calculate the distance differently.
        Period forwardPeriod = Period.days(numForwardDays);
        Period backwardPeriod = Period.days(numBackwardDays);

        DateTime startDate = clump.getStartDateTime().minus(backwardPeriod);
        DateTime endDate = clump.getEndDateTime().plus(forwardPeriod);
        
        List<RawData> rawData = clump.getRawData();
        if(rawData == null || rawData.isEmpty()) {
            log.warn("Clump #{} does not have any associated RawData; ignoring",
                    clump.getId());
            return;
        }
        RawData icsReport = rawData.get(0);
        
        String associationFieldValue = icsReport.get(associationField);
        
        if(associationFieldValue == null) {
            log.warn("Unable to find a value for field \"{}\" on RawData #{}; ignoring",
                    associationField, icsReport.getId());
            return;
        }
        
        final String incidentIdentifier = associationFieldValue.trim();
        
        Predicate<Fire> predicate = new Predicate<Fire>() {
            @Override
            public boolean apply(Fire fire) {
                String value = fire.get(associationField);
                if(value == null) {
                    return false;
                }
                return incidentIdentifier.equalsIgnoreCase(value.trim());
            }
        };
        
        List<Fire> fires = fireSet.getMatching(predicate, startDate, endDate);

        final Fire result;
        switch(fires.size()) {
            case 0:
                // OK, we didn't find any Fires that associate with this
                // clump, so we need to create a new one and associate with it.
                result = newFireFromClump(clump);
                result.setArea(clump.getArea());
                fireSet.add(result);
                break;

            case 1:
                // OK, we found exactly one Fire to associate with.  Then
                // that's our fire, and we need to associate with it.
                result = fires.get(0);
                break;

            default:
                // OK, there is more than one Fire that should associate with
                // this clump.  So let's merge all those fires into a single
                // one, and then associate with it.
                result = fireSet.merge(fires);
                break;
        }

        DateTime previousEndDate = result.getEndDateTime();
        
        // Add this clump to the set of clumps associated with the given Fire
        result.addClump(clump);

        // If this report is newer than our previous latest report, then use
        // its area instead of the previous area.
        if(icsReport.getEndDateTime().isAfter(previousEndDate)) {
            result.setArea(icsReport.getArea());
        }
        
        // Merge ICS data attributes into Fire attributes
        for(Map.Entry<String, String> entry : icsReport.entrySet()) {
            if(!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            } else if(icsReport.getEndDateTime().isAfter(previousEndDate)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private Fire newFireFromClump(Clump clump) {
        Fire result = new Fire();
        result.setSource(clump.getSource());
        return result;
    }
}

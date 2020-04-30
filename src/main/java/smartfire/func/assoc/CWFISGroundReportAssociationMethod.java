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

import com.vividsolutions.jts.geom.Geometry;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

@MetaInfServices(AssociationMethod.class)
public class CWFISGroundReportAssociationMethod implements AssociationMethod {
    private static final Logger log = LoggerFactory.getLogger(CWFISGroundReportAssociationMethod.class);
    private final double fireDistance; 
    private final int numForwardDays;
    private final int numBackwardDays;
    private final int numRepeatedDays;
    
    public CWFISGroundReportAssociationMethod(
            @Attribute(name="numForwardDays",
                    description="Number of days forward in time to associate") Integer numForwardDays,
            @Attribute(name="numBackwardDays", 
                    description="Number of days backward in time to associate") Integer numBackwardDays,
            @Attribute(name="fireDistance",
                    description="Association distance for fires (meters)") Double fireDistance,
            @Attribute(name="numRepeatedDays",
                    description="Number of days to allow consecutive reports of unchanged fire areas") Integer numRepeatedDays) {
        this.numForwardDays = numForwardDays;
        this.numBackwardDays = numBackwardDays;
        this.fireDistance = fireDistance;
        this.numRepeatedDays = numRepeatedDays;
    }

    @Override
    public void associate(Clump clump, QueryableFireSet fireSet) {
        double bufferDistance = this.fireDistance;
        Period forwardPeriod = Period.days(numForwardDays);
        Period backwardPeriod = Period.days(numBackwardDays);
        
        DateTime startDate = clump.getStartDateTime().minus(backwardPeriod);
        DateTime endDate = clump.getEndDateTime().plus(forwardPeriod);
        Geometry queryShape = clump.getShape().buffer(bufferDistance);
        
        List<RawData> rawData = clump.getRawData();
        if(rawData == null || rawData.isEmpty()) {
            log.warn("Clump #{} does not have any associated RawData; ignoring",
                    clump.getId());
            return;
        }
        RawData report = rawData.get(0);
        
        List<Fire> fires = fireSet.getAssociated(queryShape, startDate, endDate);
        
        final Fire result;
        boolean newFire = false;
        switch(fires.size()) {
            case 0:
                // OK, we didn't find any Fires that associate with this
                // clump, so we need to create a new one and associate with it.
                result = newFireFromClump(clump);
                result.setArea(clump.getArea());
                fireSet.add(result);
                result.addClump(clump);
                newFire = true;
                break;

            case 1:
                // OK, we found exactly one Fire to associate with.  Then
                // that's our fire, and we need to associate with it.
                result = fires.get(0);
                break;

            default:
                // OK, there is more than one Fire that should associate with
                // this clump.  We will try to find the fire with the same firename,
                // and then associate with it.
                Fire resultFire = null;
                for(Fire singleFire : fires){
                    if (singleFire.getDisplayName().equals(report.get("firename"))){
                        resultFire = singleFire;
                        break;
                    }
                }
                result = resultFire;
                break;
                
        }
        
        // if we could not find a single fire to associate, quit.
        if (result == null) {
           log.warn("Can't associate Clump #{} with single fire report; ignoring",
           clump.getId());
           return;
        }
                        
        int numDaysSameArea = 0;
        double fireArea = result.getArea();
        Set<Clump> fireClumps = result.getClumps();
        
        if (newFire == false){        
        // Gets the number of days the latest fire area has been reported by counting the number of clumps
        // that share the same area as the fire.
            for(Clump fireClump : fireClumps) {
                double fireClumpArea = fireClump.getArea();

                if(fireClumpArea == fireArea) {
                    numDaysSameArea++;
                }
            }
        }
        
        // If this clump reports the same burn area as the fire, and the fire area has not changed over 
        // a set time, ignore the clump
        if(clump.getArea() <= result.getArea() & numDaysSameArea > numRepeatedDays) {
            log.warn("Clump #{} reporting unchanging area; ignoring",
                    clump.getId());
            return;
        }
        
        DateTime previousEndDate = result.getEndDateTime();
        
        // Add this clump to the set of clumps associated with the given Fire
        //result.addClump(clump);

        // If this report is newer than our previous latest report, then add it
        // and use its area instead of the previous area.
        if(report.getEndDateTime().isAfter(previousEndDate) & newFire == false) {
            
            // remove previous clumps
            for(Clump fireClump : fireClumps) {
                result.removeClump(fireClump);
            }
            
            // Add this clump to the set of clumps associated with the given Fire
            result.addClump(clump);
            
            result.setArea(report.getArea());
        }
        
        // Merge CWFIS data attributes into Fire attributes
        for(Map.Entry<String, String> entry : report.entrySet()) {
            if(!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            } else if(report.getEndDateTime().isAfter(previousEndDate)) {
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

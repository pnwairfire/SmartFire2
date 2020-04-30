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
import java.util.Iterator;
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

/**
 * The daily area association method.  Use for sources with no spatial info, but daily area burned info.
 */
@MetaInfServices
public class DailyAreaAssociationMethod implements AssociationMethod {
    private static final Logger log = LoggerFactory.getLogger(DailyAreaAssociationMethod.class);
    private final int numForwardDays;
    private final int numBackwardDays;
    private final double sizeThreshold;
    private final double smallFireDistance;
    private final double largeFireDistance;

    /* TODO: simplify this to a single association distance and no size threshold*/
    public DailyAreaAssociationMethod(
            @Attribute(name="numForwardDays", 
                    description="Number of days forward in time to associate")
            Integer numForwardDays,
            @Attribute(name="numBackwardDays", 
                    description="Number of days backward in time to associate")
            Integer numBackwardDays,
            @Attribute(name="sizeThreshold", 
                    description="Threshold at which to treat this as a \"large\" fire instead of a \"small\" fire (square meters)")
            Double sizeThreshold,
            @Attribute(name="smallFireDistance", 
                    description="Association distance for \"small\" fires (meters)")
            Double smallFireDistance,
            @Attribute(name="largeFireDistance", 
                    description="Association distance for \"large\" fires (meters)")
            Double largeFireDistance
            ) {
        this.numForwardDays = numForwardDays;
        this.numBackwardDays = numBackwardDays;
        this.sizeThreshold = sizeThreshold;
        this.smallFireDistance = smallFireDistance;
        this.largeFireDistance = largeFireDistance;
    }

    @Override
    public void associate(Clump clump, QueryableFireSet fireSet) {
        // Figure out how close (in time and in space) a fire would have to be
        // to this clump to be considered associated.  This distance may depend
        // on the clump itself.  (In this class it's a pretty dead simple
        // formula, but we expect that there may be subclasses of this class
        // that want to calculate the distance differently.
        double bufferDistance = getFireAssociationDistance(clump);
        Period forwardPeriod = Period.days(getNumForwardDays(clump));
        Period backwardPeriod = Period.days(getNumBackwardDays(clump));

        Geometry queryShape = clump.getShape().buffer(bufferDistance);
        DateTime startDate = clump.getStartDateTime().minus(backwardPeriod);
        DateTime endDate = clump.getEndDateTime().plus(forwardPeriod);
        
        List<RawData> rawData = clump.getRawData();
        if(rawData == null || rawData.isEmpty()) {
            log.warn("Clump #{} does not have any associated RawData; ignoring",
                    clump.getId());
            return;
        }
        RawData sourceData = rawData.get(0);
        
        List<Fire> fires = fireSet.getAssociated(queryShape, startDate, endDate);

        final Fire result;
        switch(fires.size()) {
            case 0:
                // OK, we didn't find any Fires that associate with this
                // clump, so we need to create a new one and return that.
                result = newFireFromClump(clump);
                fireSet.add(result);
                break;

            case 1:
                // OK, we found exactly one Fire to associate with.  Then
                // that's our fire, and we need to return it.
                result = fires.get(0);
                break;

            default:
                // OK, there is more than one Fire that should associate with
                // this clump.  So let's merge all those fires into a single
                // one, and then associate with it.
                result = fireSet.merge(fires);
                break;
        }

        // Add this clump to the set of clumps associated with the given Fire
        result.addClump(clump);
        
        // Compute the area of the result Fire
        result.setArea(computeArea(result));
        
        // Merge source data attributes into Fire attributes
        for(Map.Entry<String, String> entry : sourceData.entrySet()) {
            if(!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
    }

    protected double computeArea(Fire fire) {
        // Compute area as the sum of all clump areas.
        Set<Clump> clumps = fire.getClumps();
        Iterator<Clump> it = clumps.iterator();
        double totalArea = 0;
        while(it.hasNext()) {
            Clump aClump = it.next();
            totalArea += aClump.getArea();
        }
        return totalArea;
    }
    
    protected Fire newFireFromClump(Clump clump) {
        Fire result = new Fire();
        result.setSource(clump.getSource());
        return result;
    }
    
    protected double getFireAssociationDistance(Clump clump) {
        double area = clump.getArea();
        if(area > this.sizeThreshold) {
            return this.largeFireDistance;
        }
        return this.smallFireDistance;
    }

    protected int getNumForwardDays(Clump clump) {
        return this.numForwardDays;
    }

    protected int getNumBackwardDays(Clump clump) {
        return this.numBackwardDays;
    }
}

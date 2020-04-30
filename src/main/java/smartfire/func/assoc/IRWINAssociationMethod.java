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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.Clump;
import smartfire.database.Fire;
import smartfire.database.RawData;
import smartfire.func.AssociationMethod;
import smartfire.gis.QueryableFireSet;

@MetaInfServices(AssociationMethod.class)
public class IRWINAssociationMethod implements AssociationMethod {
    private static final Logger log = LoggerFactory.getLogger(IRWINAssociationMethod.class);
    private static final DateTimeFormatter incidentDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z");
    private final String incidentGUIDField = "IrwinID";
    private final String isComplexField = "IsComplex";
    private final String inConflictField = "InConflict";
    private final String isActiveField = "IsActive";
    private final String falseAlarmCategory = "FA";
    private final String incidentTypeCategoryField = "IncidentTypeCategory";
    private final String incidentModifiedTimeField = "ModifiedOnDateTime";
    private final DateTime epoch = new DateTime(0);
    private final DateTime endDateTime = epoch.plusYears(100);
    

    @Override
    public void associate(Clump clump, QueryableFireSet fireSet) {
        // Get and ensure Raw Data for the clump
        List<RawData> rawData = clump.getRawData();
        if(rawData == null || rawData.isEmpty()) {
            log.warn("Clump #{} does not have any associated RawData; ignoring", clump.getId());
            return;
        }
        RawData report = rawData.get(0);
        
        // Get Association field, validate, and clean it
        String associationFieldValue = report.get(incidentGUIDField);
        if(associationFieldValue == null) {
            log.warn("Unable to find a value for field \"{}\" on RawData #{}; ignoring", incidentGUIDField, report.getId());
            return;
        }
        final String incidentIdentifier = associationFieldValue.trim();
        
        // Build fire set query predicate where fires with matching GUIDs are associated
        Predicate<Fire> predicate = new Predicate<Fire>() {
            @Override
            public boolean apply(Fire fire) {
                String value = fire.get(incidentGUIDField);
                if(value == null) {
                    return false;
                }
                return incidentIdentifier.equalsIgnoreCase(value.trim());
            }
        };
        
        // Get associated fires by predicate
        List<Fire> fires = fireSet.getMatching(predicate, epoch, endDateTime);
        
        // If an incident is complex, conflicted, not active, or a false alarm ignore it and remove any related fires
//        if (report.get(isComplexField).equalsIgnoreCase("true") ||
          if (report.get(inConflictField).equalsIgnoreCase("true") ||
                  report.get(isActiveField).equalsIgnoreCase("false") ||
                  report.get(incidentTypeCategoryField).equalsIgnoreCase(falseAlarmCategory)) {
            log.warn("RawData #{} incident is invalid; Clearing out {} associated fires", report.getId(), fires.size());
            removeFires(fireSet, fires);
            return;
        }
        
        final Fire result;
        switch(fires.size()) {
            case 0:
                // OK, we didn't find any Fires that associate with this
                // clump, so we need to create a new one and associate with it.
                result = newFireFromClump(clump);
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

        // Add this clump to the set of clumps associated with the given Fire
        result.addClump(clump);
        result.setArea(result.getShape().getArea());
        
        // Merge the most recent IRWIN data attributes into Fire attributes
        if (result.isEmpty() || isMoreRecent(report, result)) {
            result.clear();
            for(Map.Entry<String, String> entry : report.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
    }
    
    private void removeFires(QueryableFireSet fireSet, List<Fire> fires) {
        for (Fire fire : fires) {
            fire.disassociateAllClumps();
            fireSet.remove(fire);
        }
    }
    
    private Fire newFireFromClump(Clump clump) {
        Fire result = new Fire();
        result.setSource(clump.getSource());
        return result;
    }
    
    private boolean isMoreRecent(RawData raw, Fire fire) {
        // If fire has no modified time, consider raw data more recent.
        if (!fire.containsKey(incidentModifiedTimeField)) {
            return true;
        }
        // If fire has a modified time, but not raw data, consider raw data older.
        if (!raw.containsKey(incidentModifiedTimeField)) {
            return false;
        }
        // True if raw data modified time is after fire modified time.
        DateTime rawModifiedTime = incidentDateTimeFormatter.parseDateTime(raw.get(incidentModifiedTimeField));
        DateTime fireModifiedTime = incidentDateTimeFormatter.parseDateTime(fire.get(incidentModifiedTimeField));
        return rawModifiedTime.isAfter(fireModifiedTime);
    }
}

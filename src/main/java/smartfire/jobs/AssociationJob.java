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
package smartfire.jobs;

import java.util.Collection;
import org.joda.time.DateTime;
import smartfire.database.*;
import smartfire.func.AssociationMethod;
import smartfire.func.FireTypeMethod;
import smartfire.func.Methods;
import smartfire.func.ProbabilityMethod;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.QueryableFireSet;
import smartfire.queue.Job;
import smartfire.queue.ProgressReporter;

/**
 * The <b>Association</b> Job.  For a description of this job, see the 
 * appropriate section under the "SMARTFIRE 2.0 Job Chain" specification in
 * the SMARTFIRE 2.0 software design document (STI-910050-TM2).
 */
public class AssociationJob implements Job {
    private final GeometryBuilder geometryBuilder;
    private final ClumpDao clumpDao;
    private final FireDao fireDao;
    private final Source source;
    private final DateTime startTime;
    private final DateTime endTime;

    /**
     * Constructs a new AssociationJob.
     *
     * @param geometryBuilder the geometry builder for SF2
     * @param clumpDao the DAO for the Clump table
     * @param fireDao the DAO for the Fire table
     * @param source the current input Source
     * @param startTime the start DateTime of data to associate
     * @param endTime the end DateTime of data to associate
     */
    AssociationJob(GeometryBuilder geometryBuilder, ClumpDao clumpDao, FireDao fireDao, Source source, DateTime startTime, DateTime endTime) {
        this.geometryBuilder = geometryBuilder;
        this.clumpDao = clumpDao;
        this.fireDao = fireDao;
        this.source = source;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public void execute(ProgressReporter progressReporter) throws Exception {
        progressReporter.setProgress(5, "Deleting orphaned fires");
        fireDao.deleteOrphanedFires(source);

        progressReporter.setProgress(10, "Setting up association");
        AssociationMethod assocMethod = Methods.newAssociationMethod(source);
        ProbabilityMethod probMethod = Methods.newProbabilityMethod(source);
        FireTypeMethod fireTypeMethod = Methods.newFireTypeMethod(geometryBuilder, source);
        Collection<Clump> clumps = clumpDao.getByDate(source, startTime, endTime);
        QueryableFireSet fireSet = new QueryableFireSet(fireDao, source);

        progressReporter.setProgress(30, "Creating fires");
        final int PROGRESS_START = 30;
        final int PROGRESS_MULTIPLIER = 65;
        int percentProgress = PROGRESS_START;
        int counter = 0;
        int numRecords = clumps.size();
        for(Clump clump : clumps) {
            assocMethod.associate(clump, fireSet);

            // Find the fire that the Clump ended up associated with
            Fire associatedFire = clump.getFire();
            if(associatedFire != null) {
                // Determine the probability for the fire 
                double probability = probMethod.calculateFireProbability(associatedFire);
                associatedFire.setProbability(probability);
                
                // Determine the fire type for the fire
                String fireType = fireTypeMethod.determineFireType(associatedFire);
                if(fireType.isEmpty()) {
                    fireType = "NA";
                }
                associatedFire.setFireType(fireType);
            }

            // Update progress
            counter++;
            int newProgress = (int) (PROGRESS_START + (counter / (double) numRecords) * PROGRESS_MULTIPLIER);
            if(newProgress > percentProgress) {
                percentProgress = newProgress;
                progressReporter.setProgress(percentProgress, "Associating clump " + counter + " of " + numRecords);
            }
        }

        // Save any newly created fires
        int numCreated = fireSet.saveNewEntities();

        if(numCreated == 0) {
            progressReporter.setProgress(100, "Zero fires created");
        } else {
            progressReporter.setProgress(100, "Successfully associated "
                    + numRecords + " clumps into "
                    + numCreated + " fires");
        }

    }

    public DateTime getEndTime() {
        return endTime;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public Source getSource() {
        return source;
    }

    @Override
    public boolean isEquivalentTo(Job other) {
        AssociationJob otherAssoc;
        if(other instanceof AssociationJob) {
            otherAssoc = (AssociationJob) other;
        } else {
            return false;
        }
        boolean sameSource = source.getId() == otherAssoc.source.getId();
        boolean dateContained = !(otherAssoc.startTime.isAfter(startTime) || otherAssoc.endTime.isBefore(endTime));
        return (sameSource && dateContained);
    }

    @Override
    public boolean isConflictingWith(Job other) {
        if(other instanceof AssociationJob) {
            AssociationJob otherAssoc = (AssociationJob) other;
            boolean sameSource = source.getId() == otherAssoc.source.getId();
            boolean datePartlyContained = !(otherAssoc.startTime.isAfter(endTime) || otherAssoc.endTime.isBefore(startTime));
            return (sameSource && datePartlyContained);
        } else if(other instanceof ReconciliationJob) {
            ReconciliationJob otherRec = (ReconciliationJob) other;
            if(!otherRec.getStreamSources().contains(getSource())) {
                return false;
            }

            boolean datePartlyContained = !(otherRec.getStartTime().isAfter(endTime) || otherRec.getEndTime().isBefore(startTime));
            return datePartlyContained;
        }
        return false;
    }
}

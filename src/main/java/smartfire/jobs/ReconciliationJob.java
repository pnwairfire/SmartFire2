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

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import smartfire.database.*;
import smartfire.func.Methods;
import smartfire.func.ReconciliationMethod;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.QueryableEventSet;
import smartfire.queue.Job;
import smartfire.queue.ProgressReporter;

/**
 * The <b>Reconciliation</b> Job.  For a description of this job, see the 
 * appropriate section under the "SMARTFIRE 2.0 Job Chain" specification in
 * the SMARTFIRE 2.0 software design document (STI-910050-TM2).
 */
public class ReconciliationJob implements Job {
    private final GeometryBuilder geometryBuilder;
    private final FireDao fireDao;
    private final EventDao eventDao;
    private final ReconciliationStreamDao streamDao;
    private final Integer reconciliationStreamId;
    private final DateTime startTime;
    private final DateTime endTime;

    /**
     * Constructs a new ReconciliationJob.
     * 
     * @param fireDao the DAO for the Fire table
     * @param eventDao the DAO for the Event table
     * @param stream the ReconciliationStream that this job will reconcile for
     * @param startTime the start DateTime of the data to reconcile
     * @param endTime the end DateTime of the data to reconcile
     */
    ReconciliationJob(
            GeometryBuilder geometryBuilder,
            FireDao fireDao,
            EventDao eventDao,
            ReconciliationStreamDao streamDao,
            ReconciliationStream stream,
            DateTime startTime,
            DateTime endTime) {
        this.geometryBuilder = geometryBuilder;
        this.fireDao = fireDao;
        this.eventDao = eventDao;
        this.streamDao = streamDao;
        this.reconciliationStreamId = stream.getId();
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public void execute(ProgressReporter progressReporter) throws Exception {

        progressReporter.setProgress(10, "Setting up reconciliation");
        ReconciliationStream stream = streamDao.getById(reconciliationStreamId);
        ReconciliationMethod method = Methods.newReconciliationMethod(geometryBuilder, stream, startTime, endTime);

        // Get all fires for every source in the reconciliation stream
        List<Fire> fires = Lists.newArrayList();
        for(Source source : stream.getSources()) {
            fires.addAll(fireDao.getByDate(source, startTime, endTime));
        }
        
        QueryableEventSet eventSet = new QueryableEventSet(eventDao, stream);

        progressReporter.setProgress(30, "Reconciling fires");
        final int PROGRESS_START = 30;
        final int PROGRESS_MULTIPLIER = 65;
        int percentProgress = PROGRESS_START;
        int counter = 0;
        int numRecords = fires.size();
        for(Fire fire : fires) {
            // Ensure we are only working with Fires with valid shapes.
            if(fire.getClumps().isEmpty()) {
                continue;
            }

            // Reconcile this Fire
            method.reconcile(fire, eventSet);

            // Update progress
            counter++;
            int newProgress = (int) (PROGRESS_START + (counter / (double) numRecords) * PROGRESS_MULTIPLIER);
            if(newProgress > percentProgress) {
                percentProgress = newProgress;
                progressReporter.setProgress(percentProgress, "Reconciling fire " + counter + " of " + numRecords);
            }
        }

        // Save any newly created events
        int numCreated = eventSet.saveNewEntities();

        if(numRecords == 0) {
            progressReporter.setProgress(100, "Zero events created");
        } else {
            progressReporter.setProgress(100, "Successfully reconciled " + numRecords
                    + " fires, creating " + numCreated + " new events");
        }
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public Set<Source> getStreamSources() {
        return new HashSet<Source>(streamDao.getById(reconciliationStreamId).getSources());
    }

    @Override
    public boolean isEquivalentTo(Job other) {
        ReconciliationJob otherRec;
        if(other instanceof ReconciliationJob) {
            otherRec = (ReconciliationJob) other;
        } else {
            return false;
        }
        boolean sameStream = (this.reconciliationStreamId == otherRec.reconciliationStreamId);
        boolean dateContained = !(otherRec.startTime.isAfter(startTime) || otherRec.endTime.isBefore(endTime));
        return (sameStream && dateContained);
    }

    @Override
    public boolean isConflictingWith(Job other) {
        if(other instanceof ReconciliationJob) {
            ReconciliationJob otherRec = (ReconciliationJob) other;
            boolean sameStream = (this.reconciliationStreamId == otherRec.reconciliationStreamId);
            boolean datePartlyContained = !(otherRec.startTime.isAfter(endTime) || otherRec.endTime.isBefore(startTime));
            return (sameStream && datePartlyContained);
        } else if(other instanceof AssociationJob) {
            AssociationJob otherAssoc = (AssociationJob) other;
            if(!getStreamSources().contains(otherAssoc.getSource())) {
                return false;
            }

            boolean datePartlyContained = !(otherAssoc.getStartTime().isAfter(endTime) || otherAssoc.getEndTime().isBefore(startTime));
            return datePartlyContained;
        }
        return false;
    }
}

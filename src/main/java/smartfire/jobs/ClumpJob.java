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
import java.util.Collection;
import java.util.List;
import org.joda.time.DateTime;
import smartfire.database.*;
import smartfire.func.ClumpMethod;
import smartfire.func.Methods;
import smartfire.gis.GeometryBuilder;
import smartfire.queue.Job;
import smartfire.queue.ProgressReporter;

/**
 * The <b>Clump</b> Job.  For a description of this job, see the appropriate
 * section under the "SMARTFIRE 2.0 Job Chain" specification in the SMARTFIRE
 * 2.0 software design document (STI-910050-TM2).
 */
public class ClumpJob implements Job {
    private final GeometryBuilder geometryBuilder;
    private final RawDataDao rawDataDao;
    private final ClumpDao clumpDao;
    private final Source source;
    private final DateTime startTime;
    private final DateTime endTime;

    /**
     * Constructs a new ClumpJob.
     *
     * @param rawDataDao the DAO for the RawData table
     * @param clumpDao the DAO for the Clump table
     * @param source the current input Source
     * @param startTime the start DateTime of data to clump
     * @param endTime the end DateTime of data to clump
     */
    ClumpJob(GeometryBuilder geometryBuilder, RawDataDao rawDataDao, ClumpDao clumpDao, Source source, DateTime startTime, DateTime endTime) {
        this.geometryBuilder = geometryBuilder;
        this.rawDataDao = rawDataDao;
        this.clumpDao = clumpDao;
        this.source = source;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public void execute(ProgressReporter progressReporter) throws Exception {
        progressReporter.setProgress(25, "Clumping raw data");

        // The records from the RawData table for the given Source and
        // time range will be read, and fed to the ClumpMethod associated with
        // the current Source as input. 
        List<RawData> rawData = rawDataDao.getByDate(source, startTime, endTime);

        // Filter out any RawData records that already have Clumps
        List<RawData> toBeClumped = Lists.newArrayListWithExpectedSize(rawData.size());
        for(RawData record : rawData) {
            if(record.getClump() == null) {
                toBeClumped.add(record);
            }
        }

        // Run the ClumpMethod
        ClumpMethod method = Methods.newClumpMethod(geometryBuilder, source);
        Collection<Clump> clumpData = method.clump(toBeClumped);

        // Any records the ClumpMethod creates will be inserted into the Clump
        // table, and relationship records will be inserted linking Clumps to
        // their associated RawData record(s).

        final int PROGRESS_START = 25;
        final int PROGRESS_MULTIPLIER = 70;
        int percentProgress = PROGRESS_START;
        int counter = 0;
        int numRecords = clumpData.size();

        // Insert clumps into the Clump table.
        for(Clump clump : clumpData) {
            if(clump != null) {
                clumpDao.save(clump);
            }

            // Update progress
            counter++;
            int newProgress = (int) (PROGRESS_START + (counter / (double) numRecords) * PROGRESS_MULTIPLIER);
            if(newProgress > percentProgress) {
                percentProgress = newProgress;
                progressReporter.setProgress(percentProgress, "Clumping record " + counter + " of " + numRecords);
            }
        }

        progressReporter.setProgress(95, "Finishing job");

        if(numRecords == 0) {
            progressReporter.setProgress(100, "Zero clumps created");
        } else {
            progressReporter.setProgress(100, "Successfully created " + numRecords + " clumps");
        }
    }

    @Override
    public boolean isEquivalentTo(Job other) {
        ClumpJob otherClumpJob;
        if(other instanceof ClumpJob) {
            otherClumpJob = (ClumpJob) other;
        } else {
            return false;
        }
        boolean sameSource = source.getId() == otherClumpJob.source.getId();
        boolean dateContained = !(otherClumpJob.startTime.isAfter(startTime) || otherClumpJob.endTime.isBefore(endTime));
        return (sameSource && dateContained);
    }

    @Override
    public boolean isConflictingWith(Job other) {
        ClumpJob otherClumpJob;
        if(other instanceof ClumpJob) {
            otherClumpJob = (ClumpJob) other;
        } else {
            return false;
        }
        boolean sameSource = source.getId() == otherClumpJob.source.getId();
        boolean datePartlyContained = !(otherClumpJob.startTime.isAfter(endTime) || otherClumpJob.endTime.isBefore(startTime));
        return (sameSource && datePartlyContained);
    }
}

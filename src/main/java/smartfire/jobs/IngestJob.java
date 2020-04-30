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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.Days;
import smartfire.SmartfireException;
import smartfire.config.SmartfireConfig;
import smartfire.database.RawData;
import smartfire.database.RawDataDao;
import smartfire.database.Source;
import smartfire.database.Source.DataPolicy;
import smartfire.database.SourceDao;
import smartfire.func.Methods;
import smartfire.func.UploadIngestMethod;
import smartfire.gis.GeometryBuilder;
import smartfire.queue.DataAcquireJob;
import smartfire.queue.Job;
import smartfire.queue.ProgressReporter;

/**
 * The <b>Ingest</b> Job.  This job is responsible for handling the uploading of data
 * similar to the Fetch job.
 */
public class IngestJob implements DataAcquireJob {
    private final JobChain jobChain;
    private final String filePath;
    private final RawDataDao dao;
    private final Source source;
    private final SourceDao sourceDao;
    private final GeometryBuilder geometryBuilder;
    private final DateTime fetchTime;
    private final boolean runReconciliation;
    private final boolean useMaxBackwardDays;

    /**
     * Constructs a new ingestJob.
     *
     * @param jobChain the JobChain this IngestJob is associated with
     * @param filePath the path of the uploaded file associated with the ingest of raw data
     * @param dao the DAO for the RawData table
     * @param source the current input Source
     * @param sourceDao the DAO for the Source table
     * @param geometryBuilder used to build geometry objects from the raw data
     * @param fetchTime the DateTime of data to fetch
     * @param runReconciliation flags that defines if reconciliation jobs should be scheduled after the upload
     */
    public IngestJob(JobChain jobChain, String filePath, RawDataDao dao, Source source, SourceDao sourceDao,
            GeometryBuilder geometryBuilder, DateTime fetchTime, boolean runReconciliation, boolean useMaxBackwardDays) {
        this.jobChain = jobChain;
        this.filePath = filePath;
        this.dao = dao;
        this.source = source;
        this.sourceDao = sourceDao;
        this.geometryBuilder = geometryBuilder;
        this.fetchTime = fetchTime;
        this.runReconciliation = runReconciliation;
        this.useMaxBackwardDays = useMaxBackwardDays;
    }

    @Override
    public void execute(ProgressReporter progressReporter) throws Exception {
        progressReporter.setProgress(10, "Ingesting records");

        // Execute the FetchMethod associated with this ScheduledFetch.
        UploadIngestMethod method = Methods.newUploadIngestMethod(source, geometryBuilder);

        // Collect data records
        Collection<RawData> fetchedData = method.ingest(filePath, fetchTime);

        // Find earliest and lastest times
        progressReporter.setProgress(40, "Determining date range for data");
        DateTime earliestTime = null;
        DateTime latestTime = null;
        List<RawData> filteredFetchedData = new ArrayList<RawData>();
        for(RawData record : fetchedData) {
            if(record != null) {
                DateTime recordStartDate = record.getStartDateTime();
                DateTime recordEndDate = record.getEndDateTime();
                
                // If the record ends earlier than the max allowed backward days, skip the record
                if (useMaxBackwardDays && exceedsMaxBackwardDays(recordEndDate, fetchTime)) {
                    // Skip this record
                    continue;
                }
                
                if(earliestTime == null) {
                    earliestTime = recordStartDate;
                } else if(earliestTime.isAfter(recordStartDate)) {
                    earliestTime = recordStartDate;
                }
                if(latestTime == null) {
                    latestTime = recordEndDate;
                } else if(latestTime.isBefore(recordEndDate)) {
                    latestTime = recordEndDate;
                }
                filteredFetchedData.add(record);
            }
        }
        
        if (earliestTime != null) {
            earliestTime = adjustStartDate(earliestTime, fetchTime);
        }

        // Clear any existing RawData records associated with the time period
        // about to be fetched from the RawData table.  By cascade rules, any
        // existing relationship records relating RawData records to Clump
        // records that were previously created will be deleted as well.
        // Deletion only occurs if the dataset is a REPLACE dataset.
        if(source.getNewDataPolicy().equals(DataPolicy.REPLACE)) {
            progressReporter.setProgress(50, "Cleaning existing raw data records");
            dao.deleteByDate(source, earliestTime, latestTime);
        } else if(source.getNewDataPolicy().equals(DataPolicy.APPEND)) {
            progressReporter.setProgress(75, "Skipping delete step. Source is set to APPEND");
        } else {
            throw new SmartfireException("Unrecognized Data Policy in IngestJob.");
        }

        // Insert any records it fetches into the RawData table.
        final int PROGRESS_START = 75;
        final int PROGRESS_MULTIPLIER = 20;
        int percentProgress = PROGRESS_START;
        int numRecords = filteredFetchedData.size();
        int counter = 0;
        int saved = 0;
        for(RawData record : filteredFetchedData) {
            if(record != null) {
                dao.save(record);
                saved++;

                // Update progress
                counter++;
                int newProgress = (int) (PROGRESS_START + (counter / (double) numRecords) * PROGRESS_MULTIPLIER);
                if(newProgress > percentProgress) {
                    percentProgress = newProgress;
                    progressReporter.setProgress(percentProgress, "Ingesting record " + counter + " of " + numRecords);
                }
            }
        }

        progressReporter.setProgress(95, "Finishing job");

        if(saved > 0) {
            jobChain.scheduleDownstreamJobs(source, earliestTime, latestTime, runReconciliation);
        }

        // TODO: Release the exclusive write lock on the RawData table.

        if(numRecords == 0) {
            progressReporter.setProgress(100, "Data unavailable");
        } else {
            // set lastest data.
            Source mySource = sourceDao.getById(source.getId());
            mySource.setLatestData(new DateTime(latestTime.getYear(), latestTime.getMonthOfYear(), latestTime.getDayOfMonth(), 0, 0, 0, 0));
            sourceDao.save(mySource);
            progressReporter.setProgress(100, "Successfully Ingested " + saved + " records");
        }
        
        // Delete temp uploaded file
        File file = new File(filePath);
        file.delete();
    }

    @Override
    public boolean isEquivalentTo(Job other) {
        DataAcquireJob otherJob;
        if(other instanceof DataAcquireJob) {
            otherJob = (DataAcquireJob) other;
        } else {
            return false;
        }
        if(source.getId() == otherJob.getSource().getId() && otherJob.getFetchTime().equals(fetchTime)) {
            return true;
        }
        return false;
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public DateTime getFetchTime() {
        return fetchTime;
    }

    @Override
    public boolean isConflictingWith(Job other) {
        return false;
    }
    
    private boolean exceedsMaxBackwardDays(DateTime startDate, DateTime endDate) {
        DateTime adjustedStartDate = adjustStartDate(startDate, endDate);
        return !startDate.isEqual(adjustedStartDate);
    }
    
    private DateTime adjustStartDate(DateTime startDate, DateTime endDate) {
        String value = SmartfireConfig.get("maxNumBackwardDays");
        int maxNumBackwardDays = !value.isEmpty() ? Integer.parseInt(value) : -1;
        if (maxNumBackwardDays > 0) {
            int numReconciliationDays = Days.daysBetween(startDate.toDateMidnight(), endDate.toDateMidnight()).getDays();
            if(numReconciliationDays > maxNumBackwardDays) {
                return endDate.minusDays(maxNumBackwardDays);
            }
        }
        return startDate;
    }
}

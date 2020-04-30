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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.Days;
import smartfire.SmartfireException;
import smartfire.config.SmartfireConfig;
import smartfire.database.Source.DataPolicy;
import smartfire.database.*;
import smartfire.func.FetchMethod;
import smartfire.func.Methods;
import smartfire.gis.GeometryBuilder;
import smartfire.queue.DataAcquireJob;
import smartfire.queue.Job;
import smartfire.queue.ProgressReporter;

/**
 * The <b>Fetch</b> Job.  For a description of this job, see the appropriate
 * section under the "SMARTFIRE 2.0 Job Chain" specification in the SMARTFIRE
 * 2.0 software design document (STI-910050-TM2).
 */
public class FetchJob implements DataAcquireJob {
    private final JobChain jobChain;
    private final RawDataDao dao;
    private final Source source;
    private final SourceDao sourceDao;
    private final ScheduledFetch fetch;
    private final ScheduledFetchDao fetchDao;
    private final GeometryBuilder geometryBuilder;
    private final DateTime fetchTime;
    private final Boolean useMaxBackwardDays;
    private final int RETRY_LIMIT = 2;
    private final int RETRY_WAIT_SECONDS = 30;

    /**
     * Constructs a new FetchJob.
     *
     * @param jobChain the JobChain this FetchJob is associated with
     * @param dao the DAO for the RawData table
     * @param source the current input Source
     * @param sourceDao the DAO for the Source table
     * @param fetch ScheduledFetch used to acquire data
     * @param fetchDao the DAO for the ScheduledFetch table
     * @param geometryBuilder used to build geometry objects from the raw data
     * @param fetchTime the DateTime of data to fetch
     * @param useMaxBackwardDays limit number of days backward in time to process
     */
    FetchJob(JobChain jobChain, RawDataDao dao, Source source, SourceDao sourceDao,
            ScheduledFetch fetch, ScheduledFetchDao fetchDao, GeometryBuilder geometryBuilder, DateTime fetchTime, Boolean useMaxBackwardDays) {
        this.jobChain = jobChain;
        this.dao = dao;
        this.source = source;
        this.sourceDao = sourceDao;
        this.fetch = fetch;
        this.fetchDao = fetchDao;
        this.geometryBuilder = geometryBuilder;
        this.fetchTime = fetchTime;
        this.useMaxBackwardDays = useMaxBackwardDays;
    }

    @Override
    public void execute(ProgressReporter progressReporter) throws Exception {
        // TODO: Acquire an exclusive write lock on the RawData table
        // for the Source and time period about to be fetched.

        progressReporter.setProgress(10, "Fetching records");

        // Execute the FetchMethod associated with this ScheduledFetch.
        FetchMethod method = Methods.newFetchMethod(fetch, geometryBuilder);

        // Collect data records
        boolean success = false;
        int retryAttempt = 1;
        Collection<RawData> fetchedData = Collections.emptyList();
        while(!success) {
            try {
                fetchedData = method.fetch(source, fetchTime);
                success = true;
            } catch(IOException e) {
                if(retryAttempt > RETRY_LIMIT) {
                    throw e;
                }
                progressReporter.setProgress(10, "Fetch failure " + retryAttempt + ". Retrying in " + RETRY_WAIT_SECONDS + " seconds.");
                Thread.sleep(RETRY_WAIT_SECONDS * 1000);
            }
            if(retryAttempt > RETRY_LIMIT) {
                // Should never happen
                throw new SmartfireException("Reached retry limit on Fetch method.");
            }
            retryAttempt++;
        }

        // Set last fetch
        ScheduledFetch myFetch = fetchDao.getById(fetch.getId());
        myFetch.setLastFetch();
        fetchDao.save(myFetch);

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
        
        // Adjust earliest time so that it's within the max number of allowed days
        if (earliestTime != null) {
            earliestTime = adjustStartDate(earliestTime, fetchTime);
        }

        // Clear any existing RawData records associated with the time period
        // about to be fetched from the RawData table.  By cascade rules, any
        // existing relationship records relating RawData records to Clump
        // records that were previously created will be deleted as well.
        // Deletion only occurs if the dataset is a REPLACE dataset.
        if(source.getNewDataPolicy().equals(DataPolicy.REPLACE)) {
            if (earliestTime != null && latestTime != null) {
                progressReporter.setProgress(50, "Cleaning existing raw data records");
                dao.deleteByDate(source, earliestTime, latestTime);
            } else {
                progressReporter.setProgress(75, "Skipping delete step. Unable to determine start/end times.");
            }
        } else if(source.getNewDataPolicy().equals(DataPolicy.IRWIN_REPLACE)) {
            if (earliestTime != null && latestTime != null) {
                progressReporter.setProgress(50, "Cleaning existing raw data records");
                Map<String, Set<String>> attributeFilter = new HashMap<String, Set<String>>();
                attributeFilter.put("IrwinID", new HashSet<String>());
                for(RawData record : filteredFetchedData) {
                    if (record != null && record.containsKey("IrwinID")) {
                        attributeFilter.get("IrwinID").add(record.get("IrwinID"));
                    }
                }
                dao.deleteByDateAndAttribute(source, earliestTime, latestTime, attributeFilter);
            } else {
                progressReporter.setProgress(75, "Skipping delete step. Unable to determine start/end times.");
            }
        } else if(source.getNewDataPolicy().equals(DataPolicy.APPEND)) {
            progressReporter.setProgress(75, "Skipping delete step. Source is set to APPEND");
        } else {
            throw new SmartfireException("Unrecognized Data Policy in FetchJob.");
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
            jobChain.scheduleDownstreamJobs(source, earliestTime, latestTime);
        }

        // TODO: Release the exclusive write lock on the RawData table.

        if(numRecords == 0) {
            progressReporter.setProgress(100, "Data unavailable");
        } else {
            // set lastest data.
            Source mySource = sourceDao.getById(source.getId());
            mySource.setLatestData(new DateTime(latestTime.getYear(), latestTime.getMonthOfYear(), latestTime.getDayOfMonth(), 0, 0, 0, 0));
            sourceDao.save(mySource);
            progressReporter.setProgress(100, "Successfully fetched " + saved + " records");
        }
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

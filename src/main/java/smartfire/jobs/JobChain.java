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
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import smartfire.ApplicationSettings;
import smartfire.Granularity;
import smartfire.config.SmartfireConfig;
import smartfire.database.*;
import smartfire.gis.GeometryBuilder;
import smartfire.queue.DataAcquireJob;
import smartfire.queue.JobQueue;
import smartfire.queue.QueuedJob;

/**
 * Utility class for triggering the whole chain of SMARTFIRE jobs.
 */
public class JobChain {
    private final JobQueue queue;
    private final DatabaseConnection conn;
    private final GeometryBuilder geometryBuilder;
    private final DateTime fetchTime;
    private final DateTimeZone timeZone;
    private volatile QueuedJob queuedDataAcquireJob;
    private final List<QueuedJob> queuedClumpJobs = Lists.newArrayList();
    private final List<QueuedJob> queuedAssocJobs = Lists.newArrayList();
    private final List<QueuedJob> queuedReconcileJobs = Lists.newArrayList();

    private JobChain(ApplicationSettings settings, DateTime fetchTime, int dateOffset) {
        this.queue = settings.getJobQueue();
        this.conn = settings.getDatabaseConnection();
        this.geometryBuilder = settings.getGeometryBuilder();
        this.fetchTime = fetchTime.minusDays(dateOffset);
        this.timeZone = settings.getConfig().getDateTimeZone();
    }

    /**
     * Schedule a chain of SMARTFIRE jobs.  This method implements the
     * mechanism of triggering a ScheduledFetch, either manually or according
     * to its schedule, and any subsequent jobs as well.
     *
     * @param settings the current application settings
     * @param scheduledFetch the ScheduledFetch currently being triggered
     * @param fetchTime the date/time of data to fetch
     */
    public static void schedule(ApplicationSettings settings, ScheduledFetch scheduledFetch, DateTime fetchTime, boolean useMaxBackwardDays) {
        JobChain jobChain = new JobChain(settings, fetchTime, scheduledFetch.getDateOffset());
        jobChain.scheduleFetchJob(scheduledFetch.getSource(), scheduledFetch, useMaxBackwardDays);
    }

    /**
     * Schedule a chain of SMARTFIRE jobs.  This method implements the
     * mechanism of triggering an Upload Ingest and any subsequent jobs as well.
     *
     * @param settings the current application settings
     * @param source the Source of the data to be ingested
     * @param filePath the path of the file to be processed by the ingest job.
     * @param ingestTime the date/time of data to be ingested
     */
    public static void schedule(ApplicationSettings settings, Source source, String filePath, DateTime ingestTime, boolean runReconciliation, boolean useMaxBackwardDays) {
        JobChain jobChain = new JobChain(settings, ingestTime, 0);
        jobChain.scheduleIngestJob(source, filePath, runReconciliation, useMaxBackwardDays);
    }

    /**
     * Schedule a Reconciliation SMARTFIRE jobs.  This method implements the
     * mechanism of triggering a Reconciliation.
     *
     * @param settings the current application settings
     * @param stream the ReconciliationStream to schedule
     * @param startDate the start of the date range to reconcile
     * @param endDate the end of the date range to reconcile
     */
    public static void schedule(ApplicationSettings settings, ReconciliationStream stream, DateTime startDate, DateTime endDate) {
        boolean useMaxBackwardDays = false; // Manual scheduling of reconciliaton, don't enforce backward day limit
        JobChain jobChain = new JobChain(settings, startDate, 0);
        jobChain.scheduleReconciliationJob(stream, startDate, endDate, useMaxBackwardDays);
    }
    
    private void scheduleReconciliationJob(ReconciliationStream stream, DateTime startDate, DateTime endDate) {
        boolean useMaxBackwardDays = true; // Automated scheduling of reconciliation, do enforce backward day limit
        this.scheduleReconciliationJob(stream, startDate, endDate, useMaxBackwardDays);
    }

    /**
     * Schedule a new Reconciliation Job; the final step in the chain.
     */
    private void scheduleReconciliationJob(ReconciliationStream stream, DateTime startDate, DateTime endDate, boolean useMaxBackwardDays) {
        String recJobName = String.format("Reconcile %s %s",
                stream.getName(), describeTimeInterval(startDate, endDate));
        
        ReconciliationJob job = new ReconciliationJob(
                geometryBuilder,
                conn.getFire(),
                conn.getEvent(),
                conn.getReconciliationStream(),
                stream,
                startDate,
                endDate);

        List<QueuedJob> recJobDependencies = Lists.newArrayList(queuedAssocJobs);
        recJobDependencies.addAll(this.queuedReconcileJobs);
        QueuedJob[] recJobDependenciesArray = recJobDependencies.toArray(
                new QueuedJob[recJobDependencies.size()]);

        QueuedJob queuedRecJob = queue.enqueueIfNoneEquivalent(job, recJobName,
                recJobDependenciesArray);

        this.queuedReconcileJobs.add(queuedRecJob);
    }

    /**
     * Schedule a new IngestJob; the first step in the chain.
     */
    private void scheduleIngestJob(Source source, String filePath, boolean runReconciliation, boolean useMaxBackwardDays) {
        String sourceName = source.getName();
        String dateString = fetchTime.toString("yyyy-MM-dd");
        String jobName = String.format("%s: Upload Ingest for %s", sourceName, dateString);
        DataAcquireJob ingestJob = new IngestJob(this, filePath, conn.getRawData(), source, conn.getSource(), geometryBuilder, fetchTime, runReconciliation, useMaxBackwardDays);
        this.queuedDataAcquireJob = queue.enqueueIfNoneEquivalent(ingestJob, jobName);
    }

    /**
     * Schedule a new FetchJob; the first step in the chain.
     */
    private void scheduleFetchJob(Source source, ScheduledFetch fetch, boolean useMaxBackwardDays) {
        String sourceName = source.getName();
        String fetchDateString = fetchTime.toString("yyyy-MM-dd");
        String fetchJobName = String.format("%s: Fetch %s for %s",
                fetch.getName(), sourceName, fetchDateString);
        DataAcquireJob fetchJob = new FetchJob(this, conn.getRawData(), source, conn.getSource(), fetch, conn.getScheduledFetch(), geometryBuilder, fetchTime, useMaxBackwardDays);
        this.queuedDataAcquireJob = queue.enqueueIfNoneEquivalent(fetchJob, fetchJobName);
    }

    /**
     * Schedule downstream job(s) to execute after fetching has determined the
     * range of affected dates.
     *
     * @param source data source to use for the downstream jobs
     * @param earliestTime the earliest RawData time fetched
     * @param latestTime the latest RawData time fetched
     */
    synchronized void scheduleDownstreamJobs(Source source, DateTime earliestTime, DateTime latestTime) {
        scheduleDownstreamJobs(source, earliestTime, latestTime, true);
    }

    /**
     * Schedule downstream job(s) to execute after fetching has determined the
     * range of affected dates.
     *
     * @param source data source to use for the downstream jobs
     * @param earliestTime the earliest RawData time fetched
     * @param latestTime the latest RawData time fetched
     * @param runReconciliation flag that defines if reconciliation jobs should be scheduled
     */
    synchronized void scheduleDownstreamJobs(Source source, DateTime earliestTime, DateTime latestTime, boolean runReconciliation) {
        String sourceName = source.getName();

        Granularity granularity = source.getGranularity();

        // First loop: create Clump and Association Jobs
        for(Interval interval : granularity.getIntervals(earliestTime, latestTime, timeZone)) {
            DateTime startTime = interval.getStart();
            DateTime endTime = interval.getEnd();

            String clumpJobName = String.format("Clump %s %s",
                    sourceName, describeTimeInterval(startTime, endTime));

            // The Clump job; the second step in the chain
            ClumpJob clumpJob = new ClumpJob(
                    geometryBuilder,
                    conn.getRawData(),
                    conn.getClump(),
                    source,
                    startTime,
                    endTime);

            QueuedJob queuedClumpJob = queue.enqueueIfNoneEquivalent(clumpJob, clumpJobName, queuedDataAcquireJob);
            this.queuedClumpJobs.add(queuedClumpJob);

            String assocJobName = String.format("Associate %s %s",
                    sourceName, describeTimeInterval(startTime, endTime));

            // The Association job; the third step in the chain
            AssociationJob assocJob = new AssociationJob(
                    geometryBuilder,
                    conn.getClump(),
                    conn.getFire(),
                    source,
                    startTime,
                    endTime);

            List<QueuedJob> assocJobDependencies = Lists.newArrayList();
            assocJobDependencies.add(queuedClumpJob);
            assocJobDependencies.addAll(queuedAssocJobs);
            QueuedJob[] assocJobDependenciesArray = assocJobDependencies.toArray(
                    new QueuedJob[assocJobDependencies.size()]);

            QueuedJob queuedAssocJob = queue.enqueueIfNoneEquivalent(assocJob, assocJobName,
                    assocJobDependenciesArray);

            this.queuedAssocJobs.add(queuedAssocJob);
        }

        // Second loop: create ReconciliationJobs
        if(runReconciliation) { // Reconcile if run reconciliation is enabled during uploading. Default is true.
            for(ReconciliationStream stream : conn.getReconciliationStream().getAll()) {
                if(stream.autoReconcile()) { // Reconcile if auto reconcile is enabled
                    ReconciliationWeighting weighting = stream.getWeightingForSource(source);
                    if(weighting == null) {
                        // If this source isn't enabled for this stream, skip it
                        continue;
                    }
                    this.scheduleReconciliationJob(stream, earliestTime, latestTime);
                }
            }
        }
    }

    private static String describeTimeInterval(DateTime startTime, DateTime endTime) {
        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();
        if(startDate.equals(endDate)) {
            return String.format("for %s", startDate);
        }
        return String.format("between %s and %s", startDate, endDate);
    }
}

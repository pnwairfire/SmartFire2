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
package smartfire.queue;

import com.google.common.collect.Maps;
import it.sauronsoftware.cron4j.Scheduler;
import java.util.HashMap;
import org.joda.time.DateTimeZone;
import smartfire.ApplicationSettings;
import smartfire.SmartfireException;
import smartfire.database.ReconciliationStream;
import smartfire.database.ScheduledFetch;

/**
 * Schedules and Handles fetch jobs.
 */
public class JobScheduler {
    private final Scheduler scheduler;
    private final HashMap<String, String> scheduledJobs;

    /**
     * Constructs a new JobScheduler.
     *
     * <p>Note: this constructor takes a DateTimeZone so that it can
     * ensure that the jobs are scheduled for the system's default time zone.
     *
     * @param tz time zone to run scheduled jobs in.
     */
    public JobScheduler(DateTimeZone tz) {
        this();
        scheduler.setTimeZone(tz.toTimeZone());
    }

    /**
     * Constructs a new JobScheduler.
     */
    public JobScheduler() {
        this.scheduler = new Scheduler();
        this.scheduledJobs = Maps.newHashMap();
    }

    /**
     * Set scheduler time zone for running scheduled jobs.
     *
     * @param tz time zone to run scheduled jobs in .
     */
    public void setTimeZone(DateTimeZone tz) {
        scheduler.setTimeZone(tz.toTimeZone());
    }

    /**
     *  Start the scheduling system.
     */
    public void start() {
        scheduler.start();
    }

    /**
     * Schedule a fetch job to run.
     *
     * @param appSettings Settings for the application. Used for getting a job queue.
     * @param fetch Fetch to be scheduled.
     */
    public void schedule(ApplicationSettings appSettings, ScheduledFetch fetch) {
        if(!this.scheduler.isStarted()) {
            throw new SmartfireException("Error: Scheduling job before the job scheduler has been started.");
        }

        Integer fetchId = fetch.getId();
        SchedulableJob schedulableJob = new SchedulableFetchJob(appSettings, fetch);
        String id = this.scheduler.schedule(fetch.getSchedule(), schedulableJob);
        scheduledJobs.put("Fetch_" + fetchId, id);
    }
    
    public void schedule(ApplicationSettings appSettings, ReconciliationStream stream) {
        if(!this.scheduler.isStarted()) {
            throw new SmartfireException("Error: Scheduling job before the job scheduler has been started.");
        }
        
        Integer streamId = stream.getId();
        SchedulableJob schedulableJob = new SchedulableReconciliationJob(appSettings, stream);
        String id = this.scheduler.schedule(stream.getSchedule(), schedulableJob);
        scheduledJobs.put("Stream_" + streamId, id);
    }

    /**
     * Deschedule a fetch.
     *
     * @param fetch Fetch to be descheduled.
     */
    public void deschedule(ScheduledFetch fetch) {
        this.deschedule("Fetch_" + fetch.getId());
    }
    
    public void deschedule(ReconciliationStream stream) {
        this.deschedule("Stream_" + stream.getId());
    }

    /**
     * Deschedule a fetch with the particular fetch id.
     *
     * @param jobId Job id to be descheduled.
     */
    public void deschedule(String jobId) {
        if(!this.scheduledJobs.containsKey(jobId)) {
            throw new SmartfireException("Error: Attempt to deschedule nonexistent job with id: " + jobId);
        }
        this.scheduler.deschedule(this.scheduledJobs.get(jobId));
        this.scheduledJobs.remove(jobId);
    }

    /**
     * Stop the scheduling system.
     */
    public void stop() {
        scheduler.stop();
    }
}

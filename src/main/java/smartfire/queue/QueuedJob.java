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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.JobHistory;

public final class QueuedJob {
    private final JobQueue queue;
    private final Job job;
    private final String name;
    private final DateTime submitted;
    private final ProgressReporter progressReporter;
    private final List<QueuedJob> upstreamJobs;
    private final List<QueuedJob> downstreamJobs;
    final ReentrantLock lock = new ReentrantLock();
    private volatile JobState state;
    private volatile DateTime started;
    private volatile DateTime completed;

    QueuedJob(JobQueue queue, Job job, String name) {
        this.queue = queue;
        this.job = job;
        this.name = name;
        this.submitted = new DateTime();
        Logger log = LoggerFactory.getLogger(job.getClass());
        this.progressReporter = new ProgressReporter(log);
        this.upstreamJobs = new CopyOnWriteArrayList<QueuedJob>();
        this.downstreamJobs = new CopyOnWriteArrayList<QueuedJob>();
        this.state = JobState.INITIALIZING;
    }

    /**
     * Returns a JobInfo object describing the current execution status
     * of this job.
     *
     * @return the current execution status of this job
     */
    public JobInfo getJobInfo() {
        return new JobInfo(
                job,
                name,
                state,
                progressReporter.getProgress(),
                submitted,
                started,
                completed);
    }

    /**
     * Returns the other jobs that this job depends on.
     *
     * @return a list of QueuedJob objects
     */
    public List<QueuedJob> getUpstreamJobs() {
        return Collections.unmodifiableList(upstreamJobs);
    }

    /**
     * Returns the other jobs that depend on this job.
     *
     * @return a list of QueuedJob objects
     */
    public List<QueuedJob> getDownstreamJobs() {
        return Collections.unmodifiableList(downstreamJobs);
    }

    /**
     * Adds a new dependency on an upstream job.
     *
     * @param other a new job that is an upstream dependency of this one
     */
    /* package */ void addUpstreamJob(QueuedJob other) {
        this.upstreamJobs.add(other);
        other.downstreamJobs.add(this);
        if(other.isFinished()) {
            notifyUpstreamJobCompleted(other);
        }
    }

    /* package */ void notifyUpstreamJobCompleted(QueuedJob other) {
        upstreamJobs.remove(other);
        if(other.state == JobState.SUCCESS) {
            if(started == null && isReadyToRun()) {
                queue.submit(this);
            }
        } else {
            this.state = JobState.FAILURE;
        }
    }

    public JobHistory createJobHistory() {
        JobHistory jobHistory = new JobHistory();
        jobHistory.setStartDate(this.getStarted());
        jobHistory.setEndDate(this.getCompleted());
        jobHistory.setName(this.getName());
        jobHistory.setStatus(this.getJobInfo().getState().name());
        jobHistory.setType(this.getJob().getClass().getName());
        jobHistory.setFinalStatus(this.getProgressReporter().getProgress().getCurrentStatus());
        return jobHistory;
    }

    /**
     * Returns true if this job is ready to run; that is, if all of its
     * upstream dependencies have completed successfully.
     *
     * @return
     */
    public boolean isReadyToRun() {
        return (state == JobState.WAITING && upstreamJobs.isEmpty());
    }

    /**
     * Returns true if this job has finished; that is, if it has completed,
     * either successfully or unsuccessfully.
     *
     * @return
     */
    public boolean isFinished() {
        return (state == JobState.SUCCESS || state == JobState.FAILURE);
    }

    public Job getJob() {
        return job;
    }

    public String getName() {
        return name;
    }

    DateTime getSubmitted() {
        return submitted;
    }

    ProgressReporter getProgressReporter() {
        return progressReporter;
    }

    public JobState getState() {
        return state;
    }

    void setState(JobState state) {
        this.state = state;
    }

    DateTime getStarted() {
        return started;
    }

    void setStarted(DateTime started) {
        this.started = started;
    }

    DateTime getCompleted() {
        return completed;
    }

    void setCompleted(DateTime completed) {
        this.completed = completed;
    }
}

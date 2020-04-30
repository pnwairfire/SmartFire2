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
import java.util.HashSet;
import java.util.Set;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.DatabaseConnection;
import smartfire.database.JobHistory;

final class RunnableJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(RunnableJob.class);
    private static final Set<QueuedJob> runningJobs = Collections.synchronizedSet(new HashSet<QueuedJob>());
    private final DatabaseConnection conn;
    private final QueuedJob queuedJob;

    RunnableJob(DatabaseConnection conn, QueuedJob queuedJob) {
        this.conn = conn;
        this.queuedJob = queuedJob;
    }

    private QueuedJob findAnyExecutingConflictingJob() {
        synchronized(runningJobs) {
            for(QueuedJob runningJob : runningJobs) {
                if(this.queuedJob.getJob().isConflictingWith(runningJob.getJob())) {
                    return runningJob;
                }
            }
        }
        return null;
    }

    @Override
    public void run() {
        queuedJob.lock.lock();
        try {
            if(!queuedJob.isReadyToRun()) {
                log.debug("Ready to execute job {}, but it is not ready to run; abort", queuedJob.getName());
                return;
            }
            try {
                if(conn != null) {
                    conn.beginTransaction();
                }
                QueuedJob otherJob = findAnyExecutingConflictingJob();
                if(otherJob != null) {
                    log.debug("Waiting to execute job {} until job {} is done", queuedJob.getName(), otherJob.getName());
                    queuedJob.addUpstreamJob(otherJob);
                    return;
                }
            } catch(Exception ex) {
                log.warn("Ignoring exception that occurred while attempting to find jobs that conflict with {}", queuedJob.getName());
                log.debug("Ignored exception is:", ex);
                if(conn != null) {
                    conn.rollbackOnly();
                }
            } finally {
                if(conn != null) {
                    conn.resolveTransaction();
                }
            }
            runningJobs.add(this.queuedJob);
            queuedJob.setState(JobState.RUNNING);
            queuedJob.setStarted(new DateTime());
            JobState result = JobState.FAILURE;
            try {
                if(conn != null) {
                    conn.beginTransaction();
                }
                queuedJob.getJob().execute(queuedJob.getProgressReporter());
                result = JobState.SUCCESS;
            } catch(Exception ex) {
                log.debug("Exception while executing job \"" + queuedJob.getName() + "\"", ex);
                result = JobState.FAILURE;
                if(conn != null) {
                    conn.rollbackOnly();
                }
                queuedJob.getProgressReporter().setProgress(100,
                        "Exception while executing job: " + ex.getMessage());
            } finally {
                ProgressReporter pr = queuedJob.getProgressReporter();
                String message = pr.getProgress().getCurrentStatus();
                if(conn != null) {
                    pr.setProgress(100, "Flushing changes to database");
                    try {
                        conn.resolveTransaction();
                    } catch(Exception ex) {
                        log.debug("Exception while finalizing transaction for job \"" + queuedJob.getName() + "\"", ex);
                        message = "Exception while finalizing transaction: " + ex.getMessage();
                        result = JobState.FAILURE;
                    }
                }

                runningJobs.remove(this.queuedJob);
                pr.setProgress(100, message);
                queuedJob.setCompleted(new DateTime());
                queuedJob.setState(result);

                // Save the JobHistory in a separate transaction (since if we had
                // an error, the transaction for the job would have been rolled
                // back anyway).
                if(conn != null) {
                    conn.beginTransaction();
                    JobHistory jobHistory = queuedJob.createJobHistory();
                    conn.getJobHistory().save(jobHistory);
                    conn.resolveTransaction();
                }

                for(QueuedJob downstreamJob : queuedJob.getDownstreamJobs()) {
                    downstreamJob.notifyUpstreamJobCompleted(queuedJob);
                }
            }
        } finally {
            queuedJob.lock.unlock();
        }
    }
}

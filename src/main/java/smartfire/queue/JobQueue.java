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

import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import com.sti.justice.concurrent.DisposableExecutorService;
import com.sti.justice.concurrent.NamedThreadFactory;
import com.sti.justice.Validator;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.DatabaseConnection;

/**
 * Represents a queue of executing jobs.
 */
public class JobQueue {
    private static final Logger log = LoggerFactory.getLogger(JobQueue.class);
    private final DisposableExecutorService pool;
    private final List<WeakReference<QueuedJob>> jobs = new CopyOnWriteArrayList<WeakReference<QueuedJob>>();
    private final DatabaseConnection conn;

    /**
     * Constructs a new JobQueue.  The queue will be sized to take advantage of
     * all the CPUs available to the current runtime.
     */
    public JobQueue() {
        this(null, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Constructs a new JobQueue.  The queue will be sized to take advantage of
     * all the CPUs available to the current runtime.
     *
     * <p>Note: this constructor takes a DatabaseConnection so that it can
     * ensure that database transactions are properly handled for all the jobs
     * that are run in this queue.
     *
     * @param conn the current database connection
     */
    public JobQueue(DatabaseConnection conn) {
        this(conn, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Constructs a new JobQueue, which will use the given number of threads
     * to schedule jobs.
     * 
     * <p>Note: this constructor takes a DatabaseConnection so that it can 
     * ensure that database transactions are properly handled for all the jobs
     * that are run in this queue.
     *
     * @param conn the current database connection
     * @param nThreads the number of threads to use
     */
    public JobQueue(DatabaseConnection conn, int nThreads) {
        this.conn = conn;
        this.pool = NamedThreadFactory.newFixedThreadPool("SMARTFIRE", nThreads);
    }

    /**
     * Disposes any resources associated with this JobQueue.
     */
    public void dispose() {
        pool.dispose();
        jobs.clear();
    }

    /**
     * Adds a new job to this JobQueue, with a default name and with no
     * upstream jobs.
     *
     * @param job the Job to execute
     * @return a QueuedJob object for tracking the newly enqueued job
     */
    public QueuedJob enqueue(Job job) {
        Validator.notNull(job, "job");
        return enqueue(job, job.getClass().getName(), new QueuedJob[0]);
    }

    /**
     * Adds a new job to this JobQueue with the given name and given upstream
     * Jobs.
     *
     * @param job the Job to execute
     * @param name a name for this Job
     * @param upstreamJobs array of upstream QueuedJobs that this job depends on;
     *                     it may be null
     * @return a QueuedJob object for tracking the newly enqueued job
     */
    public QueuedJob enqueue(Job job, String name, QueuedJob... upstreamJobs) {
        Validator.notNull(job, "job");
        Validator.notNull(name, "name");
        QueuedJob queuedJob = new QueuedJob(this, job, name);
        enqueueInternal(queuedJob, upstreamJobs);
        return queuedJob;
    }

    /**
     * Adds a new job to this JobQueue, as long as it is not equivalent to 
     * any job that is currently pending.
     *
     * @param job the Job to execute
     * @param name a name for this Job
     * @param upstreamJobs array of upstream QueuedJobs that this job depends
     *                     on; it may be null
     * @return a QueuedJob object for tracking the enqueued job; if an
     *         equivalent job is already queued, it will be returned instead
     */
    public QueuedJob enqueueIfNoneEquivalent(Job job, String name, QueuedJob... upstreamJobs) {
        Validator.notNull(job, "job");
        Validator.notNull(name, "name");
        QueuedJob queuedJob = new QueuedJob(this, job, name);
        for(QueuedJob existing : getQueuedJobs()) {
            boolean isEquivalent = existing.getJob().isEquivalentTo(job);
            boolean isWaiting = (existing.getState() == JobState.WAITING);
            if(isWaiting && isEquivalent && existing.lock.tryLock()) {
                try {
                    log.debug("Ignoring new job {} because it is equivalent to existing job {}",
                            name, existing.getName());

                    // OK, we found an existing job we can use.  But before we
                    // return it, update its upstream job list.
                    if(upstreamJobs != null) {
                        for(QueuedJob upstreamJob : upstreamJobs) {
                            existing.addUpstreamJob(upstreamJob);
                        }
                    }
                    return existing;
                } finally {
                    existing.lock.unlock();
                }
            } else if(isEquivalent && existing.getState() == JobState.RUNNING) {
                log.debug("Existing job {} is already running; marking it as upstream of new job {}",
                        existing.getName(), name);
                upstreamJobs = ObjectArrays.concat(upstreamJobs, existing);
            }
        }
        enqueueInternal(queuedJob, upstreamJobs);
        return queuedJob;
    }

    private void enqueueInternal(QueuedJob queuedJob, QueuedJob[] upstreamJobs) {
        if(upstreamJobs != null) {
            for(QueuedJob upstreamJob : upstreamJobs) {
                queuedJob.addUpstreamJob(upstreamJob);
            }
        }
        jobs.add(new WeakReference<QueuedJob>(queuedJob));
        queuedJob.setState(JobState.WAITING);
        if(queuedJob.isReadyToRun()) {
            submit(queuedJob);
        } else {
            log.debug("Job {} is not yet ready to run", queuedJob.getName());
        }
    }

    /* package */ void submit(QueuedJob queuedJob) {
        log.debug("Adding job {} to the ThreadPool queue", queuedJob.getName());
        pool.execute(new RunnableJob(conn, queuedJob));
    }

    /**
     * Get a JobInfo object for each Job that is currently executing or queued
     * to execute.
     *
     * @return a collection of JobInfo instances
     */
    public List<JobInfo> getJobsInfo() {
        List<JobInfo> result = Lists.newArrayList();
        for(QueuedJob queuedJob : getQueuedJobs()) {
            result.add(queuedJob.getJobInfo());
        }
        return result;
    }

    /**
     * Get a QueuedJob object for each Job that is currently executing or
     * queued to execute.
     *
     * @return a collection of QueuedJob instances
     */
    public List<QueuedJob> getQueuedJobs() {
        List<QueuedJob> result = Lists.newArrayList();
        List<WeakReference<QueuedJob>> toBeRemoved = Lists.newArrayList();
        for(WeakReference<QueuedJob> ref : jobs) {
            QueuedJob queuedJob = ref.get();
            if(queuedJob == null) {
                toBeRemoved.add(ref);
            } else {
                result.add(queuedJob);
            }
        }
        jobs.removeAll(toBeRemoved);
        return result;
    }
}

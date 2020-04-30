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

import java.util.concurrent.atomic.AtomicBoolean;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobQueueTest extends TestCase {
    private final Logger log = LoggerFactory.getLogger(JobQueueTest.class);

    public JobQueueTest(String testName) {
        super(testName);
    }

    public void testExecuteJob() throws Exception {
        final String JOB_NAME = "TestJob";
        final AtomicBoolean flag = new AtomicBoolean(false);
        final Job job = new Job() {
            @Override
            public void execute(ProgressReporter progressReporter) throws Exception {
                for(int i = 0; i < 10; i++) {
                    Thread.sleep(10);
                    progressReporter.setProgress(i);
                }
                flag.set(true);
            }

            @Override
            public boolean isEquivalentTo(Job other) {
                return false;
            }

            @Override
            public boolean isConflictingWith(Job other) {
                return false;
            }
        };
        JobQueue queue = new JobQueue();
        long started = System.currentTimeMillis();
        QueuedJob queuedJob = queue.enqueue(job, JOB_NAME);
        while(!queuedJob.isFinished()) {
            long elapsed = System.currentTimeMillis() - started;
            for(JobInfo jobInfo : queue.getJobsInfo()) {
                if(JOB_NAME.equals(jobInfo.getName())) {
                    JobState state = jobInfo.getState();
                    log.debug("Job is in state {} after {} millis", state, elapsed);
                    if(state == JobState.FAILURE) {
                        fail("Failure running Job");
                    }
                }
            }
            Thread.sleep(20);
        }
        assertTrue(flag.get());
        JobState state = queuedJob.getState();
        log.debug("Job is in state {} after completion", state);
        assertEquals(JobState.SUCCESS, state);
        queue.dispose();
    }

    public void testJobDependencies() throws Exception {
        final String job1Name = "JOB1";
        final AtomicBoolean flag1 = new AtomicBoolean(false);
        final Job job1 = new Job() {
            @Override
            public void execute(ProgressReporter progressReporter) throws Exception {
                Thread.sleep(50);
                flag1.set(true);
                Thread.sleep(50);
            }

            @Override
            public boolean isEquivalentTo(Job other) {
                return false;
            }

            @Override
            public boolean isConflictingWith(Job other) {
                return false;
            }
        };
        final String job2Name = "JOB2";
        final AtomicBoolean flag2 = new AtomicBoolean(false);
        final Job job2 = new Job() {
            @Override
            public void execute(ProgressReporter progressReporter) throws Exception {
                Thread.sleep(50);
                if(!flag1.get()) {
                    throw new RuntimeException("Unexpected flag1 value!");
                }
                flag2.set(true);
                Thread.sleep(50);
            }

            @Override
            public boolean isEquivalentTo(Job other) {
                return false;
            }

            @Override
            public boolean isConflictingWith(Job other) {
                return false;
            }
        };

        JobQueue queue = new JobQueue();
        long started = System.currentTimeMillis();

        // Start a bunch of dummy jobs
        int numDummyJobs = (Runtime.getRuntime().availableProcessors() * 2);
        for(int i = 0; i < numDummyJobs; i++) {
            final int n = i + 1;
            queue.enqueue(new Job() {
                @Override
                public void execute(ProgressReporter progressReporter) throws Exception {
                    log.debug("Running dummy job {}", n);
                    Thread.sleep(10);
                }

                @Override
                public boolean isEquivalentTo(Job other) {
                    return false;
                }

                @Override
                public boolean isConflictingWith(Job other) {
                    return false;
                }
            }, "DummyJob" + n);
        }

        QueuedJob queuedJob1 = queue.enqueue(job1, job1Name);
        QueuedJob queuedJob2 = queue.enqueue(job2, job2Name, queuedJob1);

        while(!(queuedJob1.isFinished() && queuedJob2.isFinished())) {
            long elapsed = System.currentTimeMillis() - started;
            JobState state = queuedJob1.getState();
            log.debug("Job1 is in state {} after {} millis", state, elapsed);
            if(state == JobState.FAILURE) {
                fail("Failure running Job1");
            }
            state = queuedJob2.getState();
            log.debug("Job2 is in state {} after {} millis", state, elapsed);
            if(state == JobState.FAILURE) {
                fail("Failure running Job2");
            }
            Thread.sleep(20);
        }

        assertTrue(flag1.get());
        assertTrue(flag2.get());

        JobState state = queuedJob1.getState();
        log.debug("Job1 is in state {} after completion", state);
        assertEquals(JobState.SUCCESS, state);
        state = queuedJob2.getState();
        log.debug("Job2 is in state {} after completion", state);
        assertEquals(JobState.SUCCESS, state);

        queue.dispose();
    }
}

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

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Represents information about a Job currently in the JobQueue.
 */
@ExportedBean(defaultVisibility=2)
public class JobInfo {
    private final Job job;
    private final String name;
    private final JobState state;
    private final Progress progress;
    private final DateTime submitted;
    private final DateTime started;
    private final DateTime completed;

    JobInfo(Job job, String name, JobState state, Progress progress, DateTime submitted, DateTime started, DateTime completed) {
        this.job = job;
        this.name = name;
        this.state = state;
        this.progress = progress;
        this.submitted = submitted;
        this.started = started;
        this.completed = completed;
    }

    public Job getJob() {
        return job;
    }

    @Exported
    public String getName() {
        return name;
    }

    @Exported
    public Progress getProgress() {
        return progress;
    }

    @Exported
    public JobState getState() {
        return state;
    }

    public DateTime getSubmittedTime() {
        return submitted;
    }

    public DateTime getStartedTime() {
        return started;
    }

    public DateTime getCompletedTime() {
        return completed;
    }

    public Interval getElapsedTime() {
        if(started == null) {
            return null;
        }
        final DateTime endTime;
        if(completed == null) {
            endTime = new DateTime();
        } else {
            endTime = completed;
        }
        return new Interval(started, endTime);
    }

    public Period getElapsedPeriod() {
        if(started == null) {
            return null;
        }
        return getElapsedTime().toPeriod();
    }

    @Exported(name="elapsedTime")
    public String getElapsedString() {
        if(started == null) {
            return "";
        }
        PeriodFormatter formatter = PeriodFormat.getDefault();
        return getElapsedPeriod().toString(formatter);
    }
}

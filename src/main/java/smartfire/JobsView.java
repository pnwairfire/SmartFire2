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
package smartfire;

import com.google.common.collect.Lists;
import java.util.List;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import smartfire.database.DatabaseConnection;
import smartfire.database.JobHistory;
import smartfire.queue.JobInfo;
import smartfire.queue.JobState;

/**
 * JobsView Dashboard and related pages.
 */
@ExportedBean
public class JobsView {
    private final ApplicationSettings appSettings;
    private final DatabaseConnection conn;

    public JobsView(ApplicationSettings appSettings, DatabaseConnection conn) {
        this.appSettings = appSettings;
        this.conn = conn;
    }

    public Api getApi() {
        return new Api(this);
    }
    
    /*
     * Support methods
     */

    @Exported(name="runningJobs")
    public List<JobInfo> getRunningJobs() {
        List<JobInfo> jobInfo = appSettings.getJobQueue().getJobsInfo();
        List<JobInfo> result = Lists.newArrayList();
        for(JobInfo info : jobInfo) {
            JobState status = info.getState();
            if(status == JobState.RUNNING || status == JobState.WAITING) {
                result.add(info);
            }
        }
        return result;
    }

    @Exported(name="recentlyFinishedJobs")
    public List<JobHistory> getRecentlyFinishedJobs() {
        return conn.getJobHistory().getRecentlyFinished();
    }

    public List<JobHistory> getJobHistory() {
        return this.conn.getJobHistory().getAll();
    }

    public boolean areAnyJobsPending() {
        return !getRunningJobs().isEmpty();
    }

    public int getRefreshInterval() {
        return (areAnyJobsPending() ? 1 : 0);
    }    
}

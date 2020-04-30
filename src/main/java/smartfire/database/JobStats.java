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
package smartfire.database;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Represents statistics from the JobHistory table.
 */
@ExportedBean(defaultVisibility=10)
public class JobStats {
    private int totalJobs;
    private int successfulJobs;
    private int failedJobs;

    JobStats(int totalJobs, int successfulJobs, int failedJobs) {
        this.totalJobs = totalJobs;
        this.successfulJobs = successfulJobs;
        this.failedJobs = failedJobs;
    }

    @Exported
    public int getFailedJobs() {
        return failedJobs;
    }

    @Exported
    public int getSuccessfulJobs() {
        return successfulJobs;
    }

    @Exported
    public int getTotalJobs() {
        return totalJobs;
    }
}

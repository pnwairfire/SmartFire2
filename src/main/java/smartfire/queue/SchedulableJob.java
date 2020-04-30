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
import smartfire.ApplicationSettings;

/**
 * Represents a SMARTFIRE Job that can be scheduled by a Scheduler
 */
public abstract class SchedulableJob implements Runnable {
    protected final ApplicationSettings appSettings;

    /**
     * Constructs a new SchedulableJob. To be added to the job queue
     * by the job chain
     *
     * @param appSettings application settings
     */
    public SchedulableJob(ApplicationSettings appSettings) {
        this.appSettings = appSettings;
    }

    @Override
    public abstract void run(); // Adds job to the job queue.
    
    protected DateTime currentDate() {
        return new DateTime(appSettings.getConfig().getDateTimeZone());
    }
}

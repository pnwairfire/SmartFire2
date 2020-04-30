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

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Represents the progress of an operation.
 */
@ExportedBean(defaultVisibility=10)
public final class Progress {
    private final int percentProgress;
    private final String currentStatus;

    /**
     * Constructs a new Progress object, with the given percentage of progress
     * and no associated message.
     *
     * @param percentProgress percent of completion of the current operation
     */
    public Progress(int percentProgress) {
        this.percentProgress = percentProgress;
        this.currentStatus = null;
    }

    /**
     * Constructs a new Progress object, with the given percentage of progress
     * and the given message.
     *
     * @param percentProgress percent of completion of the current operation
     * @param statusMessage a user-friendly status message
     */
    public Progress(int percentProgress, String statusMessage) {
        this.percentProgress = percentProgress;
        this.currentStatus = statusMessage;
    }

    /**
     * Returns the percent complete of the current operation, where zero
     * indicates that the operation has not yet started and 100 represents
     * completion.
     *
     * @return percent of completion of the current operation
     */
    @Exported
    public int getPercentProgress() {
        return percentProgress;
    }

    /**
     * Returns a string representing a user-friendly description of the
     * current status.  If no such string is available, returns the
     * empty string.
     *
     * @return a user-friendly status message
     */
    @Exported
    public String getCurrentStatus() {
        if(currentStatus == null) {
            return "";
        }
        return currentStatus;
    }
}

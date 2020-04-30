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

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

/**
 * Utility class for tracking the progress of an operation in one thread
 * from another thread.
 */
public class ProgressReporter {
    private final AtomicReference<Progress> progress;
    private final Logger log;

    /**
     * Constructs a new ProgressReporter with an initial state of 0% complete.
     */
    public ProgressReporter() {
        this(null);
    }
    
    /**
     * Constructs a new ProgressReporter with an initial state of 0% complete.
     * This ProgressReporter will also write progress messages to the given 
     * Logger.
     * 
     * @param log SLF4J Logger to write progress messages to
     */
    public ProgressReporter(Logger log) {
        this.progress = new AtomicReference<Progress>(new Progress(0));
        this.log = log;
    }

    /**
     * Set the progress status to the given percentage.
     *
     * @param percentProgress percent complete of the current operation
     */
    public void setProgress(int percentProgress) {
        progress.set(new Progress(percentProgress));
    }

    /**
     * Set the progress status to the given percentage, along with the given
     * user-friendly status message.
     *
     * @param percentProgress percent complete of current operation
     * @param currentStatus a user-friendly status message
     */
    public void setProgress(int percentProgress, String currentStatus) {
        if(log != null) {
            log.debug(percentProgress + "% : " + currentStatus);
        }
        progress.set(new Progress(percentProgress, currentStatus));
    }

    /**
     * Gets the current progress.
     *
     * @return the current progress
     */
    public Progress getProgress() {
        return progress.get();
    }
}

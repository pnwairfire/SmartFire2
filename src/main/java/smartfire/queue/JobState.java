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

/**
 * Represents the state of a Job's execution
 */
public enum JobState {
    /**
     * The job is still in the process of being constructed.
     */
    INITIALIZING,
    /**
     * The job has not yet started running.  It may be waiting in an execution
     * queue, or it may not yet have been added to an execution queue.
     */
    WAITING,
    /**
     * The job is currently running.
     */
    RUNNING,
    /**
     * The job has completed successfully.
     */
    SUCCESS,
    /**
     * The job has failed.
     */
    FAILURE
}

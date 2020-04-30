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
 * Represents a SMARTFIRE Job that can be triggered, executed, and monitored.
 */
public interface Job {
    /**
     * Executes the operation defined by this Job.
     *
     * @param progressReporter object for reporting execution progress
     * @throws Exception if the job fails for any reason
     */
    void execute(ProgressReporter progressReporter) throws Exception;

    /**
     * Determines whether this job is equivalent to another job.
     *
     * <p>JobQueue can use this method to determine whether a newly scheduled
     * job can be ignored because it is equivalent to another job that is
     * already scheduled to execute.
     *
     * @param other another Job instance
     * @return true, if the other Job would perform the same work as this Job;
     *         false, otherwise
     */
    boolean isEquivalentTo(Job other);
    
    /**
     * Determines whether this job conflicts with another job.
     *
     * <p>JobQueue can use this method to determine whether a newly scheduled
     * job can be ignored because it conflicts with another job that is
     * already scheduled to execute.
     *
     * @param other another Job instance
     * @return true, if the other Job would conflict with the work of this Job;
     *         false, otherwise
     */
    boolean isConflictingWith(Job other);
}

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

import java.io.File;
import smartfire.database.DatabaseConnection;
import smartfire.gis.GeometryBuilder;
import smartfire.queue.JobQueue;
import smartfire.queue.JobScheduler;

/**
 * Stores application settings for passing onto views within the application.
 */
public class ApplicationSettings {
    private final File homeDir;
    private final Config config;
    private final VersionInfo version;
    private final DatabaseConnection conn;
    private final JobQueue jobQueue;
    private final JobScheduler scheduler;
    private final GeometryBuilder geometryBuilder;

    ApplicationSettings(File homeDir, Config config, DatabaseConnection conn, VersionInfo version, JobQueue jobQueue, JobScheduler scheduler, GeometryBuilder geometryBuilder) {
        this.homeDir = homeDir;
        this.config = config;
        this.conn = conn;
        this.version = version;
        this.jobQueue = jobQueue;
        this.scheduler = scheduler;
        this.geometryBuilder = geometryBuilder;
    }

    public File getHomeDir() {
        return this.homeDir;
    }

    public Config getConfig() {
        return this.config;
    }

    public DatabaseConnection getDatabaseConnection() {
        return this.conn;
    }

    public VersionInfo getVersionInfo() {
        return this.version;
    }

    public JobQueue getJobQueue() {
        return this.jobQueue;
    }

    public JobScheduler getScheduler() {
        return this.scheduler;
    }

    public GeometryBuilder getGeometryBuilder() {
        return this.geometryBuilder;
    }
}

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

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the URL entrypoint. This is the application that is loaded if
 * the database is improperly configured. This limits the SMARTFIRE
 * functionality to configuring the database and checking the version.
 */
public class InvalidDatabaseApp implements Application {
    private static final Logger log = LoggerFactory.getLogger(InvalidDatabaseApp.class);
    private final ApplicationSettings appSettings;
    private final Config config;
    private boolean configSuccess;

    InvalidDatabaseApp(ApplicationSettings appSettings) {
        this.appSettings = appSettings;
        this.config = this.appSettings.getConfig();
        this.configSuccess = false;
    }

    public Object getDynamic(String urlPiece, StaplerRequest request, StaplerResponse response) throws Exception {
        if(this.configSuccess) {
            response.forward(this, "success", request);
        }
        return this;
    }

    public Config getConfig() {
        return appSettings.getConfig();
    }

    @Override
    public VersionInfo getVersion() {
        return appSettings.getVersionInfo();
    }

    @Override
    public ApplicationSettings getAppSettings() {
        return this.appSettings;
    }


    public void doSaveConfig(StaplerRequest req, StaplerResponse res) throws Exception {
        try {
            req.bindParameters(this.config);
            this.config.save(appSettings.getHomeDir());
            this.configSuccess = true;
        } catch(Exception e) {
            throw new SmartfireException("Unable to save SMARTFIRE configuration!");
        }
        res.forward(this, "configuration", req);
    }
}

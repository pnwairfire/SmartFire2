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
package smartfire.admin;

import java.util.List;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import smartfire.Application;
import smartfire.ApplicationSettings;
import smartfire.Config;
import smartfire.ModelView;
import smartfire.security.User;

public class Users extends ModelView {
    private final ApplicationSettings appSettings;
    private final Config config;
    private String message;

    Users(Application app) {
        super(app);
        this.appSettings = getAppSettings();
        this.config = appSettings.getConfig();
    }

    public void doCreate(StaplerRequest req, StaplerResponse res) throws Exception {
        String username = req.getParameter("username");
        String passwordRaw = req.getParameter("password");
        User user = User.newUser(username, passwordRaw);
        config.addUser(user);
        config.save(appSettings.getHomeDir());
        message = "User \"" + username + "\" successfully created.";
        res.forward(this, "index", req);
    }

    public void doDelete(StaplerRequest req, StaplerResponse res) throws Exception {
        String username = req.getParameter("username");
        config.deleteUser(username);
        config.save(appSettings.getHomeDir());
        message = "User \"" + username + "\" successfully deleted.";
        res.forward(this, "index", req);
    }
    
    public List<User> getUsers() {
        return config.getUsers();
    }

    public String getMessage() {
        return message;
    }
}

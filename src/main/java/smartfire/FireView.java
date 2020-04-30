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
import smartfire.database.DatabaseConnection;
import smartfire.database.Fire;
import smartfire.database.Source;

public class FireView extends ModelView {
    private final ApplicationSettings appSettings;
    private final DatabaseConnection conn;

    FireView(Application app, Source source) {
        super(app);
        this.appSettings = getAppSettings();
        this.conn = this.appSettings.getDatabaseConnection();
    }

    /*
     *  Views
     */
    public Object getDynamic(String urlPiece, StaplerRequest request, StaplerResponse response) throws Exception {
        Fire fire = this.conn.getFire().getByUniqueID(urlPiece);
        if(fire == null) {
            return null;
        }
        
        return new IndividualFireView(getApp(), fire);
    }
}

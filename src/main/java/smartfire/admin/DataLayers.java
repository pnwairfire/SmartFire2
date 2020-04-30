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
import smartfire.ModelView;
import smartfire.database.DatabaseConnection;
import smartfire.database.SummaryDataLayer;
import smartfire.layer.Layers;

public class DataLayers extends ModelView {
    private final ApplicationSettings appSettings;
    private final DatabaseConnection conn;

    DataLayers(Application app) {
        super(app);
        this.appSettings = getAppSettings();
        this.conn = this.appSettings.getDatabaseConnection();
    }
    /*
     *  Views
     */

    public Object getDynamic(String urlPiece, StaplerRequest request, StaplerResponse response) throws Exception {
        SummaryDataLayer layer = this.conn.getSummaryDataLayer().getByNameSlug(urlPiece);
        if(layer == null) {
            return null;
        }

        return new DataLayerView(getApp(), layer);
    }

    /*
     *  Support Methods
     */
    public List<String> getLayerReadingMethods() {
        return Layers.getLayerReadingMethods();
    }

    public List<SummaryDataLayer> getDataLayers() {
        return this.conn.getSummaryDataLayer().getAll();
    }
}

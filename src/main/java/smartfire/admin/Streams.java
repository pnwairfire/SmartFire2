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

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import smartfire.Application;
import smartfire.ApplicationSettings;
import smartfire.ModelView;
import smartfire.database.DatabaseConnection;
import smartfire.database.ReconciliationStream;
import smartfire.database.Source;
import smartfire.database.SummaryDataLayer;
import smartfire.func.MethodConfig;
import smartfire.func.Methods;

public class Streams extends ModelView {
    private final ApplicationSettings appSettings;
    private final DatabaseConnection conn;

    Streams(Application app) {
        super(app);
        this.appSettings = getAppSettings();
        this.conn = this.appSettings.getDatabaseConnection();
    }

    /*
     *  Views
     */
    public Object getDynamic(String urlPiece, StaplerRequest request, StaplerResponse response) throws Exception {
        ReconciliationStream stream = this.conn.getReconciliationStream().getByNameSlug(urlPiece);
        if(stream == null) {
            return null;
        }

        return new StreamView(getApp(), stream);
    }

    /*
     *  Support Methods
     */
    public List<String> getReconciliationMethods() {
        return Methods.getReconciliationMethods();
    }

    public List<Source> getSources() {
        return this.conn.getSource().getAll();
    }
    
    public List<SummaryDataLayer> getLayers() {
        return this.conn.getSummaryDataLayer().getAll();
    }

    public List<ReconciliationStream> getStreams() {
        return this.conn.getReconciliationStream().getAll();
    }

    public Map<String, Map<String, String>> getAllMethodAttributes() {
        Map<String, Map<String, String>> methodAttributes = Maps.newHashMap();
        for(String method : getReconciliationMethods()) {
            MethodConfig reconciliationMethodConfig = Methods.getReconciliationMethodConfig(method);
            methodAttributes.put(method, Methods.getMethodAttributes(reconciliationMethodConfig));
        }
        return methodAttributes;
    }
}

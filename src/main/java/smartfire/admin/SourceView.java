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
import smartfire.Application;
import smartfire.ApplicationSettings;
import smartfire.Granularity;
import smartfire.ModelView;
import smartfire.database.DatabaseConnection;
import smartfire.database.GeometryType;
import smartfire.database.Source;
import smartfire.database.Source.DataPolicy;
import smartfire.func.MethodConfig;
import smartfire.func.Methods;

public class SourceView extends ModelView {
    private final ApplicationSettings appSettings;
    private final DatabaseConnection conn;
    private final Source source;

    SourceView(Application app, Source source) {
        super(app);
        this.appSettings = getAppSettings();
        this.conn = this.appSettings.getDatabaseConnection();
        this.source = source;
    }

    /*
     *  Views
     */
    public FetchView getFetch() {
        return new FetchView(getApp(), source);
    }

    /*
     *  Support Methods
     */
    public Source getSource() {
        return this.source;
    }

    public List<String> getFetchMethods() {
        return Methods.getFetchMethods();
    }

    public DataPolicy[] getDataPolicies() {
        return DataPolicy.values();
    }

    public Granularity[] getGranularityTypes() {
        return Granularity.values();
    }

    public GeometryType[] getGeometryTypes() {
        return GeometryType.values();
    }

    public List<String> getClumpMethods() {
        return Methods.getClumpMethods();
    }

    public List<String> getAssociationMethods() {
        return Methods.getAssociationMethods();
    }

    public List<String> getProbabilityMethods() {
        return Methods.getProbabilityMethods();
    }

    public List<String> getFireTypeMethods() {
        return Methods.getFireTypeMethods();
    }

    public List<String> getIngestMethods() {
        return Methods.getUploadIngestMethods();
    }

    public Map<String, Map<String, String>> getAllMethodAttributes() {
        Map<String, Map<String, String>> methodAttributes = Maps.newHashMap();
        for(String method : getFetchMethods()) {
            MethodConfig fetchMethodConfig = Methods.getFetchMethodConfig(method);
            methodAttributes.put(method, Methods.getMethodAttributes(fetchMethodConfig));
        }
        for(String method : getIngestMethods()) {
            MethodConfig uploadIngestMethodConfig = Methods.getUploadIngestMethodConfig(method);
            methodAttributes.put(method, Methods.getMethodAttributes(uploadIngestMethodConfig));
        }
        for(String method : getClumpMethods()) {
            MethodConfig clumpMethodConfig = Methods.getClumpMethodConfig(method);
            methodAttributes.put(method, Methods.getMethodAttributes(clumpMethodConfig));
        }
        for(String method : getAssociationMethods()) {
            MethodConfig associationMethodConfig = Methods.getAssociationMethodConfig(method);
            methodAttributes.put(method, Methods.getMethodAttributes(associationMethodConfig));
        }
        for(String method : getProbabilityMethods()) {
            MethodConfig probabilityMethodConfig = Methods.getProbabilityMethodConfig(method);
            methodAttributes.put(method, Methods.getMethodAttributes(probabilityMethodConfig));
        }
        for(String method : getFireTypeMethods()) {
            MethodConfig fireTypeMethodConfig = Methods.getFireTypeMethodConfig(method);
            methodAttributes.put(method, Methods.getMethodAttributes(fireTypeMethodConfig));
        }
        return methodAttributes;
    }
}

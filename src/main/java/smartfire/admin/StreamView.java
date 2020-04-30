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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import smartfire.Application;
import smartfire.ApplicationSettings;
import smartfire.ModelView;
import smartfire.database.*;
import smartfire.func.MethodConfig;
import smartfire.func.Methods;

public class StreamView extends ModelView {
    private final ApplicationSettings appSettings;
    private final DatabaseConnection conn;
    private final ReconciliationStream stream;

    StreamView(Application app, ReconciliationStream stream) {
        super(app);
        this.appSettings = getAppSettings();
        this.conn = this.appSettings.getDatabaseConnection();
        this.stream = stream;
    }

    /*
     *  Views
     */

    /*
     *  Support Methods
     */
    public ReconciliationStream getStream() {
        return this.stream;
    }

    public List<String> getReconciliationMethods() {
        return Methods.getReconciliationMethods();
    }
    
    public List<Source> getUnusedSources() {
        // Get used sources
        Map<String, ReconciliationWeighting> usedSources = Maps.newHashMap();
        for(ReconciliationWeighting weighting : stream.getReconciliationWeightings()) {
            String sourceNameSlug = weighting.getSource().getNameSlug();
            usedSources.put(sourceNameSlug, weighting);
        }
        
        // Find unused sources.
        List<Source> unusedSources = Lists.newArrayList();
        List<Source> sources = this.conn.getSource().getAll();
        for(Source source : sources) {
            if(!usedSources.containsKey(source.getNameSlug())) {
                unusedSources.add(source);
            }
        }
        
        return unusedSources;
    }
    
    public List<SummaryDataLayer> getUnusedLayers() {
        // Get used Summary Data layers
        Map<String, SummaryDataLayer> usedLayers = Maps.newHashMap();
        for(SummaryDataLayer layer : stream.getSummaryDataLayers()) {
            String layerNameSlug = layer.getNameSlug();
            usedLayers.put(layerNameSlug, layer);
        }
        
        // Find unused summary data layers
        List<SummaryDataLayer> unusedLayers = Lists.newArrayList();
        List<SummaryDataLayer> layers = this.conn.getSummaryDataLayer().getAll();
        for(SummaryDataLayer layer : layers) {
            if(!usedLayers.containsKey(layer.getNameSlug())) {
                unusedLayers.add(layer);
            }
        }
        
        return unusedLayers;
    }
    
    public String getSchedule() {
        return stream.getSchedule();
    }
    
    public boolean getIsScheduled() {
        return stream.getIsScheduled();
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

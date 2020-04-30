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
import com.sti.justice.util.StringUtil;
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.joda.time.DateTime;
import org.joda.time.IllegalFieldValueException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.*;
import smartfire.config.SmartfireConfig;
import smartfire.database.*;
import smartfire.export.ExportXML;
import smartfire.func.MethodConfig;
import smartfire.func.Methods;
import smartfire.jobs.JobChain;
import smartfire.layer.Layers;

/**
 * Root of administrative pages.
 */
public class Admin extends ModelView {
    private static final Logger log = LoggerFactory.getLogger(Admin.class);
    private final ApplicationSettings appSettings;
    private final Config config;
    private final DatabaseConnection conn;
    private String message;

    public Admin(Application app) {
        super(app);
        this.appSettings = getAppSettings();
        this.config = this.appSettings.getConfig();
        this.conn = this.appSettings.getDatabaseConnection();
    }

    /*
     *  Views
     */
    public Sources getSources() {
        return new Sources(getApp());
    }

    public Streams getStreams() {
        return new Streams(getApp());
    }

    public DataLayers getDatalayers() {
        return new DataLayers(getApp());
    }

    public Users getUsers() {
        return new Users(getApp());
    }

    public void doSaveConfig(StaplerRequest req, StaplerResponse res) throws Exception {
        try {
            req.bindParameters(this.config);

            Integer numThreads = this.config.getNumThreads();
            try {
                numThreads = Integer.parseInt(req.getParameter("numThreads").trim());
            } catch(Exception e) {
            }
            this.config.setNumThreads(numThreads);

            this.config.save(appSettings.getHomeDir());
            this.message = "SMARTFIRE configuration saved successfully. Please Restart the server for the changes to take place.";
        } catch(Exception e) {
            this.message = "Error configuring SMARTFIRE.";
        }
        res.forward(this, "index", req);
    }
    
    public void doSaveGlobalConfig(StaplerRequest req, StaplerResponse res) throws Exception {
        // Set maxNumBackwardDays
        SmartfireConfig.set("maxNumBackwardDays", req.getParameter("maxNumBackwardDays"));

        // Persist updated configurations
        SmartfireConfig.saveAll(this.conn);

        this.message = "SMARTFIRE global configurations saved successfully.";
        res.forward(this, "index", req);
    }

    public void doExportSources(StaplerRequest req, StaplerResponse res) throws Exception {
        List<Source> sources = this.conn.getSource().getAll();

        ExportXML exportXML = new ExportXML();
        exportXML.saveSources(sources, appSettings.getHomeDir(), "sources.xml");

        this.message = "Sources exported successfully.";
        res.forward(this, "index", req);
    }

    @SuppressWarnings("unchecked")
    public void doImportSources(StaplerRequest req, StaplerResponse res) throws Exception {
        ExportXML exportXML = new ExportXML();
        List<Source> sources = exportXML.readSources(appSettings.getHomeDir(), "sources.xml");

        int i = 1;
        for(Source source : sources) {
            DefaultWeighting weighting = source.getDefaultWeighting();
            weighting.setSource(source);

            // Temporarily Set Name Slug
            source.setNameSlug("Temp" + new DateTime().getMillis() + "-" + i);

            this.conn.getSource().save(source);
            i++;
        }

        for(Source source : sources) {
            // Slugify name
            String nameSlug = StringUtil.slugify(source.getName());
            if(this.conn.getSource().getByNameSlug(nameSlug) != null) {
                nameSlug += "-" + source.getId();
            }
            source.setNameSlug(nameSlug);
        }

        this.message = "Sources imported successfully.";
        res.forward(this, "index", req);
    }

    public void doSaveSource(StaplerRequest req, StaplerResponse res) throws Exception {
        // Set source values
        Source source = new Source();
        source = setSource(source, req);

        // Set default weighting
        DefaultWeighting defaultWeighting = new DefaultWeighting();
        req.bindParameters(defaultWeighting);
        defaultWeighting.setSource(source);
        source.setDefaultWeighting(defaultWeighting);

        // Temporarily Set Name Slug
        source.setNameSlug("Temp");

        // Build fetch
        // Initial fetch is constructed as a manual fetch which does not require a scheduler.
        // If fetchMethod is None then DO NOT construct a ScheduledFetch
        String fetchMethod = req.getParameter("fetchMethod");
        if(!fetchMethod.equals("None")) {
            ScheduledFetch fetch = new ScheduledFetch();
            fetch = this.setScheduledFetch(fetch, source.getName(), fetchMethod);
            fetch.setSource(source);
            this.conn.getScheduledFetch().save(fetch);

            // Set fetch method attributes
            MethodConfig fetchMethodConfig = Methods.getFetchMethodConfig(fetch.getFetchMethod());
            for(String name : fetchMethodConfig.getAttributeNames()) {
                fetch.put(name, req.getParameter(fetch.getFetchMethod() + '.' + name));
            }
        } else {
            this.conn.getSource().save(source);
        }

        // Slugify name
        String nameSlug = StringUtil.slugify(source.getName());
        if(this.conn.getSource().getByNameSlug(nameSlug) != null) {
            nameSlug += "-" + source.getId();
        }
        source.setNameSlug(nameSlug);

        this.message = "Source (" + source.getName() + ") has been saved.";
        res.forward(this, "index", req);
    }

    public void doSaveLayer(StaplerRequest req, StaplerResponse res) throws Exception {
        // Set Summary Data Layer values
        SummaryDataLayer layer = new SummaryDataLayer();
        layer.setNameSlug("temp");

        layer.setName(req.getParameter("name"));

        // Data Location
        String dataLocation = req.getParameter("dataLocation");
        layer.setDataLocation(dataLocation);

        // Layer Reading Method
        String layerReadingMethod = req.getParameter("layerReadingMethod");
        layer.setLayerReadingMethod(layerReadingMethod);

        // Determine Extent
        Geometry extent = Layers.determineExtent(appSettings.getGeometryBuilder(), layerReadingMethod, dataLocation);
        layer.setExtent(extent);

        // set startDate and endDate
        DateTime startDate = parseDate(req.getParameter("startDate"));
        layer.setStartDate(startDate);
        DateTime endDate = parseDate(req.getParameter("endDate"));
        layer.setEndDate(endDate);

        this.conn.getSummaryDataLayer().save(layer);

        // Slugify name
        String nameSlug = StringUtil.slugify(layer.getName());
        if(this.conn.getSummaryDataLayer().getByNameSlug(nameSlug) != null) {
            nameSlug += "-" + layer.getId();
        }
        layer.setNameSlug(nameSlug);

        // Forward with a success message.
        this.message = "Data Layer (" + layer.getName() + ") has been saved.";
        res.forward(this, "index", req);
    }

    public void doEditLayer(StaplerRequest req, StaplerResponse res) throws Exception {
        Integer layerId;
        try {
            layerId = Integer.parseInt(req.getParameter("layerId").trim());
        } catch(NumberFormatException e) {
            this.message = "Failed to edit summary data layer.";
            res.forward(this, "index", req);
            return;
        }
        SummaryDataLayer layer = this.conn.getSummaryDataLayer().getById(layerId);

        layer.setName(req.getParameter("name"));

        // Data Location
        String dataLocation = req.getParameter("dataLocation");
        layer.setDataLocation(dataLocation);

        // Layer Reading Method
        String layerReadingMethod = req.getParameter("layerReadingMethod");
        layer.setLayerReadingMethod(layerReadingMethod);

        // Determine Extent
        Geometry extent = Layers.determineExtent(appSettings.getGeometryBuilder(), layerReadingMethod, dataLocation);
        layer.setExtent(extent);

        // set startDate and endDate
        DateTime startDate = parseDate(req.getParameter("startDate"));
        layer.setStartDate(startDate);
        DateTime endDate = parseDate(req.getParameter("endDate"));
        layer.setEndDate(endDate);

        this.conn.getSummaryDataLayer().save(layer);

        // Forward with a success message.
        this.message = "Data Layer (" + layer.getName() + ") has been saved.";
        res.forward(this, "index", req);
    }

    public void doSaveStream(StaplerRequest req, StaplerResponse res) throws Exception {
        // Set Reconciliation Stream values
        ReconciliationStream stream = new ReconciliationStream();
        stream.setNameSlug("temp");
        
        stream = this.setReconciliationStream(stream, req);

        // default value for auto reconciliation
        stream.setAutoReconcile(false);

        // for each checked source define a new ReconciliationWeighting with the given value.
        List<ReconciliationWeighting> recWeightingList = Lists.newArrayList();
        @SuppressWarnings("unchecked")
        Map<String, Object> paramMap = req.getParameterMap();
        for(String param : paramMap.keySet()) {
            // Check for auto reconcile
            if(param.equals("autoReconcile")) {
                stream.setAutoReconcile(true);
            }

            // If a checked source is found create a new reconciliation weighting and 
            // connection stream and source to it.
            boolean isSourceChecked = param.substring(param.lastIndexOf("-") + 1).equals("sourceselect");
            if(isSourceChecked) {
                // get source
                String sourceSlug = param.substring(0, param.lastIndexOf("-"));
                Source source = this.conn.getSource().getByNameSlug(sourceSlug);

                // create new reconciliation weighting
                ReconciliationWeighting recWeighting = new ReconciliationWeighting();
                recWeighting = setReconciliationWeighting(req, recWeighting, sourceSlug);
                recWeighting.setReconciliationStream(stream);
                recWeighting.setSource(source);
                this.conn.getReconciliationWeighting().save(recWeighting);

                recWeightingList.add(recWeighting);
            }

            // If a checked data layer is found, add it to the stream.
            boolean isLayerChecked = param.substring(param.lastIndexOf("-") + 1).equals("layerselect");
            if(isLayerChecked) {
                // get summary data layer and set it
                String layerSlug = param.substring(0, param.lastIndexOf("-"));
                SummaryDataLayer layer = this.conn.getSummaryDataLayer().getByNameSlug(layerSlug);
                stream.addSummaryDataLayer(layer);
            }
        }

        this.conn.getReconciliationStream().save(stream);

        // Set reconciliation method attributes
        MethodConfig reconciliationMethodConfig = Methods.getReconciliationMethodConfig(stream.getReconciliationMethod());
        for(String name : reconciliationMethodConfig.getAttributeNames()) {
            stream.put(name, req.getParameter(stream.getReconciliationMethod() + '.' + name));
        }

        // Slugify name
        String nameSlug = StringUtil.slugify(stream.getName());
        if(this.conn.getReconciliationStream().getByNameSlug(nameSlug) != null) {
            nameSlug += "-" + stream.getId();
        }
        stream.setNameSlug(nameSlug);
        
        // If the stream has been given a schedule, add it to the scheduler
        if (stream.getIsScheduled()) {
            this.appSettings.getScheduler().schedule(appSettings, stream);
        }

        // Forward with a success message.
        this.message = "Stream (" + stream.getName() + ") has been saved.";
        res.forward(this, "index", req);
    }

    public void doEditStream(StaplerRequest req, StaplerResponse res) throws Exception {
        Integer streamId;
        try {
            streamId = Integer.parseInt(req.getParameter("streamId").trim());
        } catch(NumberFormatException e) {
            this.message = "Failed to edit stream.";
            res.forward(this, "index", req);
            return;
        }
        ReconciliationStream stream = this.conn.getReconciliationStream().getById(streamId);
        
        // If stream is already scheduled, unschedule it.  We will be adding the schedule back if applicable.
        if (stream.getIsScheduled()) {
            this.appSettings.getScheduler().deschedule(stream);
        }
        
        stream = this.setReconciliationStream(stream, req);

        // default value for auto reconciliation
        boolean isAutoReconcile = false;

        // Get a map of Stream Weightings to determine which ones to delete.
        Map<String, ReconciliationWeighting> unusedSources = getStreamWeightingMap(stream);

        // New summary data layers
        List<SummaryDataLayer> newLayers = Lists.newArrayList();

        List<ReconciliationWeighting> recWeightingList = Lists.newArrayList();
        @SuppressWarnings("unchecked")
        Map<String, Object> paramMap = req.getParameterMap();
        for(String param : paramMap.keySet()) {
            // Check for auto reconcile
            if(param.equals("autoReconcile")) {
                isAutoReconcile = true;
            }

            // Set sources and weightings for the stream
            boolean isSourceChecked = param.substring(param.lastIndexOf("-") + 1).equals("sourceselect");
            if(isSourceChecked) {
                String sourceSlug = param.substring(0, param.lastIndexOf("-"));

                // change values because reconciliation weighting already exists.
                if(unusedSources.containsKey(sourceSlug)) {
                    ReconciliationWeighting recWeighting = unusedSources.get(sourceSlug);
                    recWeighting = setReconciliationWeighting(req, recWeighting, sourceSlug);
                    this.conn.getReconciliationWeighting().save(recWeighting);

                    recWeightingList.add(recWeighting);

                    // Remove it from map.
                    unusedSources.remove(sourceSlug);
                } else {
                    // Create new reconciliation weighting for new source.
                    Source source = this.conn.getSource().getByNameSlug(sourceSlug);

                    ReconciliationWeighting recWeighting = new ReconciliationWeighting();
                    recWeighting = setReconciliationWeighting(req, recWeighting, sourceSlug);
                    recWeighting.setReconciliationStream(stream);
                    recWeighting.setSource(source);
                    this.conn.getReconciliationWeighting().save(recWeighting);

                    recWeightingList.add(recWeighting);
                }
            }

            // Set Summary data layers for the stream
            boolean isLayerChecked = param.substring(param.lastIndexOf("-") + 1).equals("layerselect");
            if(isLayerChecked) {
                String layerSlug = param.substring(0, param.lastIndexOf("-"));
                SummaryDataLayer layer = this.conn.getSummaryDataLayer().getByNameSlug(layerSlug);
                newLayers.add(layer);
            }
        }

        // Add Summary Data Layers
        stream.setSummaryDataLayers(newLayers);

        // Delete unchecked weightings.
        for(String sourceSlug : unusedSources.keySet()) {
            ReconciliationWeighting weighting = unusedSources.get(sourceSlug);
            this.conn.getReconciliationWeighting().delete(weighting);
        }
        
        // Set autoreconcile
        stream.setAutoReconcile(isAutoReconcile);

        stream.setReconciliationWeightings(recWeightingList);
        this.conn.getReconciliationStream().save(stream);

        // Set reconciliation method attributes
        MethodConfig reconciliationMethodConfig = Methods.getReconciliationMethodConfig(stream.getReconciliationMethod());
        for(String name : reconciliationMethodConfig.getAttributeNames()) {
            stream.put(name, req.getParameter(stream.getReconciliationMethod() + '.' + name));
        }
        
        // If a stream has a schedule, add it to the scheduler
        if (stream.getIsScheduled()) {
            this.appSettings.getScheduler().schedule(appSettings, stream);
        }

        // Forward with a success message.
        this.message = "Stream (" + stream.getName() + ") has been saved.";
        res.forward(this, "index", req);
    }

    public void doEditSource(StaplerRequest req, StaplerResponse res) throws Exception {
        Integer sourceId;
        try {
            sourceId = Integer.parseInt(req.getParameter("sourceId").trim());
        } catch(NumberFormatException e) {
            this.message = "Failed to edit source.";
            res.forward(this, "index", req);
            return;
        }
        Source source = this.conn.getSource().getById(sourceId);
        source = setSource(source, req);
        req.bindParameters(source.getDefaultWeighting());
        this.conn.getSource().save(source);

        this.message = "Source (" + source.getName() + ") has been saved.";
        res.forward(this, "index", req);
    }

    public void doSaveScheduledFetch(StaplerRequest req, StaplerResponse res) throws Exception {
        Integer sourceId;
        try {
            sourceId = Integer.parseInt(req.getParameter("sourceId").trim());
        } catch(NumberFormatException e) {
            this.message = "Failed to add new Scheduled Fetch.";
            res.forward(this, "index", req);
            return;
        }
        Source source = this.conn.getSource().getById(sourceId);

        ScheduledFetch fetch = new ScheduledFetch();
        fetch = this.setScheduledFetch(fetch, req);
        fetch.setSource(source);
        this.conn.getScheduledFetch().save(fetch);

        // Set fetch method attributes
        MethodConfig fetchMethodConfig = Methods.getFetchMethodConfig(fetch.getFetchMethod());
        for(String name : fetchMethodConfig.getAttributeNames()) {
            fetch.put(name, req.getParameter(fetch.getFetchMethod() + '.' + name));
        }

        // Add job to job chain/queue if not a manual fetch
        if(!fetch.getIsManual()) {
            appSettings.getScheduler().schedule(appSettings, fetch);
        }

        this.message = "Scheduled Fetch (" + fetch.getName() + ") has been saved.";
        res.forward(this, "index", req);
    }

    public void doEditFetch(StaplerRequest req, StaplerResponse res) throws Exception {
        Integer fetchId;
        try {
            fetchId = Integer.parseInt(req.getParameter("fetchId").trim());
        } catch(NumberFormatException e) {
            throw new SmartfireException("Cannot parse fetch id");
        }
        ScheduledFetch fetch = this.conn.getScheduledFetch().getById(fetchId);

        if(!fetch.getIsManual()) {
            this.appSettings.getScheduler().deschedule(fetch);
        }

        fetch = this.setScheduledFetch(fetch, req);
        this.conn.getScheduledFetch().save(fetch);

        // Set fetch method attributes
        MethodConfig fetchMethodConfig = Methods.getFetchMethodConfig(fetch.getFetchMethod());
        for(String name : fetchMethodConfig.getAttributeNames()) {
            fetch.put(name, req.getParameter(fetch.getFetchMethod() + '.' + name));
        }

        // Add job to job chain/queue if not a manual fetch
        if(!fetch.getIsManual()) {
            appSettings.getScheduler().schedule(appSettings, fetch);
        }

        res.sendRedirect2("sources");
    }

    public void doDeleteFetch(StaplerRequest req, StaplerResponse res) throws Exception {
        Integer fetchId;
        try {
            fetchId = Integer.parseInt(req.getParameter("fetchId").trim());
        } catch(NumberFormatException e) {
            throw new SmartfireException("Cannot parse fetch id");
        }
        ScheduledFetch fetch = this.conn.getScheduledFetch().getById(fetchId);

        if(!fetch.getIsManual()) {
            this.appSettings.getScheduler().deschedule(fetch);
        }

        this.conn.getScheduledFetch().delete(fetch);

        res.sendRedirect2("sources");
    }

    public void doDeleteSource(StaplerRequest req, StaplerResponse res) throws Exception {
        Integer sourceId;
        try {
            sourceId = Integer.parseInt(req.getParameter("sourceId").trim());
        } catch(NumberFormatException e) {
            throw new SmartfireException("Cannot parse source id");
        }
        Source source = this.conn.getSource().getById(sourceId);

        // descedule all fetches associated with the source.
        for(ScheduledFetch fetch : source.getScheduledFetches()) {
            if(!fetch.getIsManual()) {
                this.appSettings.getScheduler().deschedule(fetch);
            }
        }

        // Delete all data records first.
        this.conn.getRawData().deleteBySource(source);
        this.conn.getClump().deleteBySource(source);
        this.conn.getFire().deleteBySource(source);

        // delete source
        this.conn.getSource().delete(source);

        res.sendRedirect2("sources");
    }

    public void doFetchRawData(StaplerRequest req, StaplerResponse res) throws Exception {
        Integer fetchId;
        try {
            fetchId = Integer.parseInt(req.getParameter("fetchId").trim());
        } catch(NumberFormatException e) {
            throw new SmartfireException("Cannot parse fetch id");
        }
        ScheduledFetch fetch = this.conn.getScheduledFetch().getById(fetchId);

        // Check to ensure fetch is manual
        if(!fetch.getIsManual()) {
            return;
        }

        // Get date
        DateTime date = parseDate(req.getParameter("date"));

        JobChain.schedule(appSettings, fetch, date, true); // TBD: dynamically set useMaxBackwardDays

        res.sendRedirect2("../jobs");
    }

    public void doReconcileData(StaplerRequest req, StaplerResponse res) throws Exception {
        Integer streamId;
        try {
            streamId = Integer.parseInt(req.getParameter("streamId").trim());
        } catch(NumberFormatException e) {
            throw new SmartfireException("Cannot parse stream id");
        }
        ReconciliationStream stream = this.conn.getReconciliationStream().getById(streamId);

        // Get startDate and endDate
        DateTime startDate = parseDate(req.getParameter("startDate"));
        DateTime endDate = parseDate(req.getParameter("endDate"));

        JobChain.schedule(appSettings, stream, startDate, endDate);

        res.sendRedirect2("../jobs");
    }
    
    private FileItem findFileItem(List<Object> fileItems, String fieldName) {
        for (Object fItemObj : fileItems) {
            FileItem fItem = (FileItem)fItemObj;
            if (fItem.isFormField() && fItem.getFieldName().equalsIgnoreCase(fieldName))
                return fItem;
        }
        return null;        
    }
    
    private boolean checkRunReconciliation(List<Object> fileItems) {
        FileItem found = findFileItem(fileItems, "run-reconciliation");
        return (found == null || StringUtil.isEmpty(found.getString())) ? false : found.getString().equals("on");
    }
    
    private Source parseSource(List<Object> fileItems) {
        FileItem found = findFileItem(fileItems, "sourceId");
        if (found == null)
            throw new SmartfireException("corrupt file upload request");
        Integer sourceId = Integer.parseInt(found.getString().trim());
        return conn.getSource().getById(sourceId);       
    }

    public void doUploadFile(StaplerRequest req, StaplerResponse res) throws Exception {
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());

        @SuppressWarnings("unchecked")
        List<Object> fileItems = upload.parseRequest(req);

        Source source = parseSource(fileItems);
        
        Boolean runReconciliation = checkRunReconciliation(fileItems);
        Iterator<Object> fileItemIter = fileItems.iterator();
        
        while (fileItemIter.hasNext()) {
            FileItem fileItem = (FileItem)fileItemIter.next();
            if (fileItem.isFormField())
                continue;
            if (StringUtil.isEmpty(fileItem.getName()))
                continue;
            
            File file = File.createTempFile(fileItem.getName(), null);
            fileItem.write(file);
            fileItem.delete();
            
            JobChain.schedule(appSettings, source, file.getPath(), new DateTime(config.getDateTimeZone()), runReconciliation, true); // TBD: dynamically set useMaxBackwardDays
            // this is a kludge because JobChain appears to be having trouble scheduling jobs in quick succession --
            // -- the database transaction commit is failing otherwise.
            if (fileItemIter.hasNext())
                Thread.sleep(1000);
        }

        res.sendRedirect2("../jobs");
    }

    public void doDeleteOrphanedFires(StaplerRequest req, StaplerResponse res) throws Exception {
        for(Source source : conn.getSource().getAll()) {
            conn.getFire().deleteOrphanedFires(source);
        }
        res.sendRedirect2("orphanedFires");
    }

    /*
     *  Support Methods
     */
    public Config getConfig() {
        return this.config;
    }
    
    public String getGlobalConfig(String key) {
        return SmartfireConfig.get(key);
    }

    public String getMessage() {
        return message;
    }

    public int getNumOrphanedFires() {
        int total = 0;
        for(Source source : conn.getSource().getAll()) {
            total += conn.getFire().findOrphanedFires(source).size();
        }
        return total;
    }

    ScheduledFetch setScheduledFetch(ScheduledFetch fetch, StaplerRequest req) {
        fetch.setName(req.getParameter("fetchName"));
        fetch.setFetchMethod(req.getParameter("fetchMethod"));
        fetch.setSchedule(req.getParameter("fetchCron"));
        fetch.setDateOffset(Integer.parseInt(req.getParameter("dateOffset")));

        // Set fetch method attributes
        MethodConfig fetchMethodConfig = Methods.getFetchMethodConfig(fetch.getFetchMethod());
        for(String name : fetchMethodConfig.getAttributeNames()) {
            fetch.put(name, req.getParameter(fetch.getFetchMethod() + '.' + name));
        }

        return fetch;
    }

    ScheduledFetch setScheduledFetch(ScheduledFetch fetch, String sourceName, String fetchMethod) {
        fetch.setName(sourceName + " Manual Fetch");
        fetch.setFetchMethod(fetchMethod);
        fetch.setDateOffset(0);
        fetch.setSchedule(null); // set to manual

        return fetch;
    }
    
    ReconciliationStream setReconciliationStream(ReconciliationStream stream, StaplerRequest req) {
        req.bindParameters(stream);
        stream.setSchedule(req.getParameter("reconciliationCron"));
        
        return stream;
    }

    private Source setSource(Source source, StaplerRequest req) {
        req.bindParameters(source);
        String ingestMethod = req.getParameter("ingestMethod");
        if(ingestMethod.equals("None")) {
            source.setIngestMethod(null);
        }
        source.setNewDataPolicy(req.getParameter("newDataPolicy")); // bindParameters() cannot handle the enum
        source.setGeometryType(req.getParameter("geometryType")); // bindParameters() cannot handle the enum
        source.setGranularity(Granularity.valueOf(req.getParameter("granularityValue"))); // bindParameters() cannot handle the enum

        // Set upload ingest method attributes
        if(source.getIngestMethod() != null) {
            MethodConfig uploadIngestMethodConfig = Methods.getUploadIngestMethodConfig(source.getIngestMethod());
            for(String name : uploadIngestMethodConfig.getAttributeNames()) {
                source.put(name, req.getParameter(source.getIngestMethod() + '.' + name));
            }
        }

        // Set clump method attributes
        MethodConfig clumpMethodConfig = Methods.getClumpMethodConfig(source.getClumpMethod());
        for(String name : clumpMethodConfig.getAttributeNames()) {
            source.put(name, req.getParameter(source.getClumpMethod() + '.' + name));
        }

        // Set association method attributes
        MethodConfig associationMethodConfig = Methods.getAssociationMethodConfig(source.getAssocMethod());
        for(String name : associationMethodConfig.getAttributeNames()) {
            source.put(name, req.getParameter(source.getAssocMethod() + '.' + name));
        }

        // Set probability method attributes
        MethodConfig probabilityMethodConfig = Methods.getProbabilityMethodConfig(source.getProbabilityMethod());
        for(String name : probabilityMethodConfig.getAttributeNames()) {
            source.put(name, req.getParameter(source.getProbabilityMethod() + '.' + name));
        }
        
        // Set fire type method attributes
        MethodConfig fireTypeMethodConfig = Methods.getFireTypeMethodConfig(source.getFireTypeMethod());
        for(String name : fireTypeMethodConfig.getAttributeNames()) {
            source.put(name, req.getParameter(source.getFireTypeMethod() + '.' + name));
        }

        return source;
    }

    private boolean isValidDate(String date) {
        // Date format should be YYYYMMDD
        if(date.length() != 8) {
            return false;
        }
        for(int i = 0; i < date.length(); i++) {
            if(!Character.isDigit(date.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private DateTime parseDate(String dateInput) {
        if(!dateInput.isEmpty() && isValidDate(dateInput)) {
            int year = Integer.parseInt(dateInput.substring(0, 4));
            int month = Integer.parseInt(dateInput.substring(4, 6));
            int day = Integer.parseInt(dateInput.substring(6));
            try {
                return new DateTime(year, month, day, 0, 0, 0, 0, config.getDateTimeZone());
            } catch(IllegalFieldValueException e) {
                return new DateTime(this.config.getDateTimeZone());
            }
        } else {
            return new DateTime(this.config.getDateTimeZone());
        }
    }

    private Map<String, ReconciliationWeighting> getStreamWeightingMap(ReconciliationStream stream) {
        Map<String, ReconciliationWeighting> usedSources = Maps.newHashMap();
        for(ReconciliationWeighting weighting : stream.getReconciliationWeightings()) {
            String sourceNameSlug = weighting.getSource().getNameSlug();
            usedSources.put(sourceNameSlug, weighting);
        }
        return usedSources;
    }

    private ReconciliationWeighting setReconciliationWeighting(StaplerRequest req, ReconciliationWeighting recWeighting, String sourceSlug) {
        recWeighting.setDetectionRate(Double.parseDouble(req.getParameter(sourceSlug + "-detectionRate")));
        recWeighting.setFalseAlarmRate(Double.parseDouble(req.getParameter(sourceSlug + "-falseAlarmRate")));
        recWeighting.setGrowthWeight(Double.parseDouble(req.getParameter(sourceSlug + "-growthWeight")));
        recWeighting.setLocationWeight(Double.parseDouble(req.getParameter(sourceSlug + "-locationWeight")));
        recWeighting.setShapeWeight(Double.parseDouble(req.getParameter(sourceSlug + "-shapeWeight")));
        recWeighting.setSizeWeight(Double.parseDouble(req.getParameter(sourceSlug + "-sizeWeight")));
        recWeighting.setNameWeight(Double.parseDouble(req.getParameter(sourceSlug + "-nameWeight")));
        recWeighting.setLocationUncertainty(Double.parseDouble(req.getParameter(sourceSlug + "-locationUncertainty")));
        recWeighting.setStartDateUncertainty(Integer.parseInt(req.getParameter(sourceSlug + "-startDateUncertainty")));
        recWeighting.setEndDateUncertainty(Integer.parseInt(req.getParameter(sourceSlug + "-endDateUncertainty")));
        recWeighting.setTypeWeight(Double.parseDouble(req.getParameter(sourceSlug + "-typeWeight")));
        return recWeighting;
    }
}

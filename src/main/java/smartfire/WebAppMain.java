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

import java.beans.Introspector;
import java.io.File;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContextEvent;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.AbstractAuthorityFactory;
import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.util.WeakCollectionCleaner;
import org.kohsuke.stapler.MetaClass;
import org.kohsuke.stapler.framework.AbstractWebAppMain;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import smartfire.config.SmartfireConfig;
import smartfire.database.DatabaseConnection;
import smartfire.database.ReconciliationStream;
import smartfire.database.ScheduledFetch;
import smartfire.gis.GeometryBuilder;
import smartfire.queue.JobQueue;
import smartfire.queue.JobScheduler;

/**
 * ServletContextListener that is used to start up and shut down the
 * application.
 */
public class WebAppMain extends AbstractWebAppMain<SmartfireApp> {
    private static final Logger log = LoggerFactory.getLogger(WebAppMain.class);

    public WebAppMain() {
        super(SmartfireApp.class);
    }

    @Override
    protected String getApplicationName() {
        return "Smartfire";
    }

    @Override
    protected Object createApplication() throws Exception {
        log.debug("Initializing SMARTFIRE\n-------------------------------------------------------------------------------");

        VersionInfo version = new VersionInfo();
        log.info("Loading SMARTFIRE version {}", version.getDisplayVersion());
        
        SLF4JBridgeHandler.install();

        if(version.isLocalVersion()) {
            log.debug("Detected local version, running with stapler.trace enabled");
            // For debugging purposes only
            System.setProperty("stapler.trace", "true");
            MetaClass.NO_CACHE = true;
        }

        File homeDir = getHomeDir();

        Config config = Config.buildConfig(homeDir);

        DatabaseConnection conn = new DatabaseConnection(config);
       
        JobQueue jobQueue = new JobQueue(conn, config.getNumThreads());

        // Start job scheduler
        JobScheduler scheduler = new JobScheduler(config.getDateTimeZone());
        scheduler.start();

        GeometryBuilder geometryBuilder = new GeometryBuilder(config);

        ApplicationSettings appSettings = new ApplicationSettings(homeDir, config, conn, version, jobQueue, scheduler, geometryBuilder);
        SmartfireConfig.fromDatabaseConnection(conn); // Load global system db configurations
        
        // Decide Application to launch
        if(!conn.checkDataSource()) {
            // Application to launch without valid db configuration.
            return new InvalidDatabaseApp(appSettings);
        }

        // Schedule all automatic fetch methods
        conn.beginTransaction();
        List<ScheduledFetch> fetches = conn.getScheduledFetch().getAllAutomaticFetches();
        for(ScheduledFetch fetch : fetches) {
            appSettings.getScheduler().schedule(appSettings, fetch);
        }
        conn.resolveTransaction();
        
        // Schedule all automatic reconciliation methods
        conn.beginTransaction();
        List<ReconciliationStream> streams = conn.getReconciliationStream().getAllAutomaticReconciliations();
        for(ReconciliationStream stream : streams) {
            appSettings.getScheduler().schedule(appSettings, stream);
        }
        conn.resolveTransaction();
        
        return new SmartfireApp(appSettings);
    }

    @Override
    protected void cleanUp(SmartfireApp app) {
        app.getAppSettings().getScheduler().stop();
        app.getConnection().disconnect();
        app.getAppSettings().getJobQueue().dispose();   
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        super.contextDestroyed(event);
        cleanUpGeoTools();
        unregisterJdbcDrivers();
        Introspector.flushCaches();
        SLF4JBridgeHandler.uninstall();
        log.debug("SMARTFIRE shutdown complete");
    }
    
    private void cleanUpGeoTools() {
        log.debug("Shutting down GeoTools");
        disposeAuthorityFactories(ReferencingFactoryFinder.getCoordinateOperationAuthorityFactories(null));
        disposeAuthorityFactories(ReferencingFactoryFinder.getCRSAuthorityFactories(null));
        disposeAuthorityFactories(ReferencingFactoryFinder.getCSAuthorityFactories(null));
        WeakCollectionCleaner.DEFAULT.exit();
        DeferredAuthorityFactory.exit();
        CRS.reset("all");
        ReferencingFactoryFinder.reset();
        log.debug("GeoTools shutdown complete");
    }
    
    private void disposeAuthorityFactories(Set<? extends AuthorityFactory> factories) {
        String lineSeparator = System.getProperty("line.separator", "\n");
        for(AuthorityFactory factory : factories) {
            if(factory instanceof AbstractAuthorityFactory) {
                String factoryName = factory.toString();
                int endOfFirstLine = factoryName.indexOf(lineSeparator);
                if(endOfFirstLine > 0) {
                    factoryName = factoryName.substring(0, endOfFirstLine);
                }
                log.debug("Disposing GeoTools authority factory {}", factoryName);
                try {
                    ((AbstractAuthorityFactory) factory).dispose();
                } catch(FactoryException ex) {
                    log.warn("Error occurred while disposing of factory", ex);
                }
            }
        }
    }

    private void unregisterJdbcDrivers() {
        List<Driver> drivers = Collections.list(DriverManager.getDrivers());
        for(Driver driver : drivers) {
            log.debug("Unregistering JDBC driver {}", driver);
            try {
                DriverManager.deregisterDriver(driver);
            } catch(Exception ex) {
                log.warn("Unable to deregister driver: " + driver.toString(), ex);
            }
        }
    }
}

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
package smartfire.database;

import com.googlecode.flyway.core.Flyway;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.sql.DataSource;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.Config;
import smartfire.SmartfireException;

/**
 * Represents a connection to the SMARTFIRE database.
 */
public final class DatabaseConnection {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String JDBC_DRIVER_CLASS = "org.postgresql.Driver";
    private static final String PERSISTENCE_UNIT = "smartfire";
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final EntityManagerFactory factory;
    private final DataSource dataSource;
    private final boolean initOkay;
    private final boolean validateOkay;
    private final ThreadLocal<EntityManager> registry = new ThreadLocal<EntityManager>();
    private final ThreadLocal<EntityTransaction> transReg = new ThreadLocal<EntityTransaction>();
    private final ThreadLocal<Boolean> rollbackReg = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public DatabaseConnection(Config config) {
        this.host = config.getDatabaseHost();
        this.port = config.getDatabasePort();
        this.database = config.getDatabaseName();
        this.username = config.getDatabaseUsername();
        this.password = config.getDatabasePassword();

        // Create DataSource object
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setServerName(host);
        source.setPortNumber(port);
        source.setDatabaseName(database);
        source.setUser(username);
        source.setPassword(password);
        this.dataSource = source;

        // Check data source
        Connection conn = null;
        boolean okay = false;
        try {
            conn = source.getConnection();
            conn.getMetaData();
            okay = true;
        } catch(SQLException e) {
            log.warn("Error while connecting to datasource", e);
            okay = false;
        } finally {
            if(conn != null) {
                try {
                    conn.close();
                } catch(SQLException e) {
                    log.warn("Error while closing connection", e);
                }
            }
        }
        this.initOkay = okay;
        if(!okay) {
            factory = null;
            this.validateOkay = false;
            return;
        }

        // Use Flyway to make sure the database is up to date
        Flyway flyway = new Flyway();
        flyway.setBaseDir("/database/");
        flyway.setDataSource(dataSource);
        
        if(flyway.status() == null) {
            flyway.init();
        }
        flyway.migrate();


        // Initialize EntityManagerFactory
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("javax.persistence.jdbc.driver", JDBC_DRIVER_CLASS);
        props.put("javax.persistence.jdbc.url", getJdbcUrl());
        props.put("javax.persistence.jdbc.user", username);
        props.put("javax.persistence.jdbc.password", password);
        Ejb3Configuration cfg = new Ejb3Configuration();
        cfg.configure(PERSISTENCE_UNIT, props);
        cfg.setDataSource(dataSource);
        factory = cfg.buildEntityManagerFactory();

        conn = null;
        boolean isValid = false;
        try {
            conn = dataSource.getConnection();
            Dialect dialect = Dialect.getDialect(cfg.getProperties());
            DatabaseMetadata metadata = new DatabaseMetadata(conn, dialect);
            cfg.getHibernateConfiguration().validateSchema(dialect, metadata);
            isValid = true;
        } catch(Exception ex) {
            log.error("Error validating database schema", ex);
            isValid = false;
        } finally {
            this.validateOkay = isValid;
            if(conn != null) {
                try {
                    conn.close();
                } catch(Exception ex) {
                }
            }
        }
    }

    public boolean checkDataSource() {
        return initOkay && validateOkay;
    }

    public String getJdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    public void disconnect() {
        if(factory.isOpen()) {
            factory.close();
        }
    }

    public void beginTransaction() {
        EntityManager em = registry.get();
        if(em == null) {
            registry.set(factory.createEntityManager());
            rollbackReg.set(false);
            em = getEntityManager();
        }
        em.setFlushMode(FlushModeType.COMMIT);
        EntityTransaction trans = em.getTransaction();
        transReg.set(trans);
        trans.begin();
    }

    public void rollbackOnly() {
        rollbackReg.set(true);
        getEntityManager().clear();
    }

    public EntityManager getEntityManager() {
        EntityManager em = registry.get();
        if(em == null) {
            throw new SmartfireException("Cannot get an EntityManager before the transaction has been initialized in the current thread");
        }
        return em;
    }

    private void commitTransaction() {
        EntityTransaction trn = transReg.get();
        trn.commit();
    }

    private void rollbackTransaction() {
        log.debug("Rolling back transaction");
        EntityTransaction trn = transReg.get();
        trn.rollback();
    }

    public void resolveTransaction() {
        EntityManager em = getEntityManager();
        try {
            if(rollbackReg.get()) {
                rollbackTransaction();
            } else {
                commitTransaction();
            }
        } finally {
            em.close();
            registry.remove();
            rollbackReg.remove();
            transReg.remove();
        }
    }

    public ClumpDao getClump() {
        return new ClumpDao(this);
    }

    public DefaultWeightingDao getDefaultWeighting() {
        return new DefaultWeightingDao(this);
    }

    public EventDao getEvent() {
        return new EventDao(this);
    }

    public EventDayDao getEventDay() {
        return new EventDayDao(this);
    }

    public FireDao getFire() {
        return new FireDao(this);
    }

    public RawDataDao getRawData() {
        return new RawDataDao(this);
    }

    public ReconciliationStreamDao getReconciliationStream() {
        return new ReconciliationStreamDao(this);
    }

    public ReconciliationWeightingDao getReconciliationWeighting() {
        return new ReconciliationWeightingDao(this);
    }

    public JobHistoryDao getJobHistory() {
        return new JobHistoryDao(this);
    }

    public ScheduledFetchDao getScheduledFetch() {
        return new ScheduledFetchDao(this);
    }

    public SourceDao getSource() {
        return new SourceDao(this);
    }

    public SummaryDataLayerDao getSummaryDataLayer() {
        return new SummaryDataLayerDao(this);
    }
    
    public SystemConfigDao getSystemConfig() {
        return new SystemConfigDao(this);
    }
}

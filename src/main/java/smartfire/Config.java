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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.security.User;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * General configuration for SMARTFIRE.
 */
@XStreamAlias("smartfire")
public class Config {
    private static transient final Logger log = LoggerFactory.getLogger(Config.class);
    private static transient final XStream xstream = new XStream();

    /**
     * Constructs a new Config with default values for all fields.
     */
    public Config() {
    }

    static {
        xstream.processAnnotations(Config.class);
    }
    //
    // Actual configuration fields
    //
    private String databaseHost = "localhost";
    private Integer databasePort = 5432;
    private String databaseName = "smartfiredb";
    private String databaseUsername = "postgres";
    private String databasePassword = "admin";
    private Integer numThreads = Runtime.getRuntime().availableProcessors();
    private String coordSysWKT = "PROJCS[\"North_America_Albers_Equal_Area_Conic\",GEOGCS[\"GCS_North_American_1983\",DATUM[\"D_North_American_1983\",SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Albers\"],PARAMETER[\"False_Easting\",0.0],PARAMETER[\"False_Northing\",0.0],PARAMETER[\"Central_Meridian\",-96.0],PARAMETER[\"Standard_Parallel_1\",20.0],PARAMETER[\"Standard_Parallel_2\",60.0],PARAMETER[\"Latitude_Of_Origin\",40.0],UNIT[\"Meter\",1.0]]";
    private String timeZone = "America/Los_Angeles";
    private String realtimeStreamNameSlug = "realtime";
    @XStreamImplicit(itemFieldName="user")
    private List<User> users = Lists.newArrayList(User.newUser("admin", "admin"));
    private transient Map<String, User> userMap = null;

    //
    // Getters and setters for configuration fields
    //
    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String tz) {
        this.timeZone = tz;
    }

    public DateTimeZone getDateTimeZone() {
        return DateTimeZone.forID(timeZone);
    }

    public String getCoordSysWKT() {
        return coordSysWKT;
    }

    public void setCoordSysWKT(String coordSysWKT) {
        this.coordSysWKT = coordSysWKT;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(Integer numThreads) {
        this.numThreads = numThreads;
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public void setDatabaseHost(String databaseHost) {
        this.databaseHost = databaseHost;
    }

    public int getDatabasePort() {
        return databasePort;
    }

    public void setDatabasePort(Integer databasePort) {
        this.databasePort = databasePort;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public void setDatabaseUsername(String databaseUsername) {
        this.databaseUsername = databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public String getRealtimeStreamNameSlug() {
        return realtimeStreamNameSlug;
    }

    public void setRealtimeStreamNameSlug(String realtimeStream) {
        this.realtimeStreamNameSlug = realtimeStream;
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public Map<String, User> getUserMap() {
        if(userMap == null) {
            userMap = Maps.newConcurrentMap();
            for(User user : users) {
                userMap.put(user.getUsername(), user);
            }
        }
        return Collections.unmodifiableMap(userMap);
    }

    public void addUser(User user) {
        users.add(user);
        if(userMap != null) {
            userMap.put(user.getUsername(), user);
        }
    }

    public void deleteUser(String username) {
        User user = getUserMap().get(username);
        if(user != null) {
            users.remove(user);
            userMap.remove(username);
        }
    }

    //
    // Other utility methods
    //
    public static File getXmlFile(File folder) {
        return new File(folder, "smartfire.xml");
    }

    private boolean fixCorruptConfig() {
        boolean configCorrupt = false;
        if(this.databaseHost == null) {
            this.databaseHost = new Config().getDatabaseHost();
            configCorrupt = true;
        }
        if(this.databaseName == null) {
            this.databaseName = new Config().getDatabaseName();
            configCorrupt = true;
        }
        if(this.databasePassword == null) {
            this.databasePassword = new Config().getDatabasePassword();
            configCorrupt = true;
        }
        if(this.databasePort == null) {
            this.databasePort = new Config().getDatabasePort();
            configCorrupt = true;
        }
        if(this.databaseUsername == null) {
            this.databaseUsername = new Config().getDatabaseUsername();
            configCorrupt = true;
        }
        if(this.numThreads == null) {
            this.numThreads = new Config().getNumThreads();
            configCorrupt = true;
        }
        if(this.coordSysWKT == null) {
            this.coordSysWKT = new Config().getCoordSysWKT();
            configCorrupt = true;
        }
        if(this.timeZone == null) {
            this.timeZone = new Config().getTimeZone();
            configCorrupt = true;
        }
        if(this.realtimeStreamNameSlug == null) {
            this.realtimeStreamNameSlug = new Config().getRealtimeStreamNameSlug();
            configCorrupt = true;
        }
        if(this.users == null) {
            this.users = Lists.newArrayList(new Config().getUsers());
            configCorrupt = true;
        }

        return configCorrupt;
    }

    /**
     * Reads configuration from the given XML file.
     *
     * @param xmlFile the configuration file
     * @return a Config instance
     * @throws IOException
     */
    private static Config readConfig(File xmlFile) throws IOException {
        log.info("Reading SmartfireConfig from file: {}", xmlFile);
        InputStream stream = new BufferedInputStream(new FileInputStream(xmlFile));
        try {
            Config config = (Config) xstream.fromXML(stream);
            return config;
        } finally {
            stream.close();
        }
    }

    /**
     * Tries to build a new config instance from a config file.
     * Creates a new instance with default values if config doesn't exist.
     *
     * @param xmlFileFolder the folder location of the configuration file
     * @return a Config instance
     * @throws IOException
     */
    public static Config buildConfig(File xmlFileFolder) throws IOException {
        File configFile = Config.getXmlFile(xmlFileFolder);
        if(configFile.canRead()) {
            try {
                Config config = Config.readConfig(configFile);

                // Check for corrupt config file and fix
                boolean configCorrupt = config.fixCorruptConfig();
                if(configCorrupt) {
                    config.save(xmlFileFolder);
                }

                return config;
            } catch(Exception e) {
                log.warn("Unable to read configuration from smartfire.xml; using default configuration", e);
                return new Config();
            }
        } else {
            return new Config();
        }
    }

    /**
     * Saves configuration to the given XML file.
     *
     * @param xmlFileFolder the folder location of the configuration file
     * @throws IOException
     */
    public void save(File xmlFileFolder) throws IOException {
        File xmlFile = getXmlFile(xmlFileFolder);
        log.info("Saving SmartfireConfig to file: {}", xmlFile);
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(xmlFile));
        try {
            xstream.toXML(this, stream);
        } finally {
            stream.close();
        }
    }
}

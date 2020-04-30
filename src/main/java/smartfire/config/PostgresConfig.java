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
package smartfire.config;

import java.util.HashMap;
import java.util.Map;
import smartfire.SmartfireException;
import smartfire.database.DatabaseConnection;
import smartfire.database.SystemConfig;

public class PostgresConfig implements RewritableAppConfig<String, String, String> {
    private final Map<String, SystemConfig> systemConfig;
    
    public PostgresConfig(Map<String, SystemConfig> systemConfig) {
        this.systemConfig = systemConfig;
    }
    
    @Override
    public String get(String key) {
        if (systemConfig.containsKey(key)) {
            return systemConfig.get(key).getConfigValue();
        }
        return "";
    }
    
    @Override
    public void set(String key, String value) {
        if (systemConfig.containsKey(key)) {
            systemConfig.get(key).setConfigValue(value);
        } else {
            SystemConfig sc = new SystemConfig();
            sc.setName(key);
            sc.setConfigValue(value);
            systemConfig.put(key, sc);
        }
    }
    
    public static PostgresConfig createInstanceFromPostgres(DatabaseConnection conn) {
        Map<String, SystemConfig> systemConfig = new HashMap<String, SystemConfig>();
        boolean isActive = hasActiveTransaction(conn);
        try {
            if (!isActive) {
                conn.beginTransaction();
            }
            for (SystemConfig sc : conn.getSystemConfig().getAll()) {
                conn.getEntityManager().detach(sc);
                systemConfig.put(sc.getName(), sc);
            }
        } finally {
            if (!isActive) {
                conn.resolveTransaction();
            }
        }
        return new PostgresConfig(systemConfig);
    }
    
    public static void writeInstanceToPostgres(DatabaseConnection conn, PostgresConfig config) {
        boolean isActive = hasActiveTransaction(conn);
        try {
            if (!isActive) {
                conn.beginTransaction();
            }
            for (SystemConfig sc : config.systemConfig.values()) {
                if (sc.getId() != null) {
                    conn.getEntityManager().merge(sc);
                }
                conn.getSystemConfig().save(sc);
            }
        } finally {
            if (!isActive) {
                conn.resolveTransaction();
            }
        }
    }
    
    private static boolean hasActiveTransaction(DatabaseConnection conn) {
        try {
            return conn.getEntityManager().getTransaction().isActive();
        } catch (SmartfireException e) {
            return false;
        }
    }
}

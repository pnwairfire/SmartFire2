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

import smartfire.database.DatabaseConnection;

public class SmartfireConfig {
    
    private SmartfireConfig() {}
    
    private static RewritableAppConfig<String, String, String> config;
    
    public static void fromDatabaseConnection(DatabaseConnection conn) {
        config = PostgresConfig.createInstanceFromPostgres(conn);
    }
    
    public static String get(String key) {
        return config.get(key);
    }
    
    public static void set(String key, String value) {
        config.set(key, value);
    }
    
    public static void saveAll(DatabaseConnection conn) {
        PostgresConfig.writeInstanceToPostgres(conn, (PostgresConfig) config);
    }
}

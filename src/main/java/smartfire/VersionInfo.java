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

import java.io.IOException;
import java.util.Properties;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Class to hold information about the currently running version of the
 * SMARTFIRE application.
 */
public class VersionInfo {
    private String version;
    private String revision;
    private String buildNumber;
    private String buildTimestamp;
    private String computerName;
    private String hostName;

    public VersionInfo() {
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("/smartfire.properties"));
        } catch(IOException e) {
            throw new SmartfireException("Unexpected error reading properties file!", e);
        }
        version = props.getProperty("smartfire.version");
        revision = props.getProperty("smartfire.revisionNumber");
        buildNumber = props.getProperty("smartfire.buildNumber");
        buildTimestamp = props.getProperty("smartfire.buildTimestamp");
        computerName = props.getProperty("smartfire.computerName");
        hostName = props.getProperty("smartfire.hostName");
    }

    public String getDisplayVersion() {
        String theVersion = getVersion();
        if(buildNumber.startsWith("$")) {
            String buildMachine = getBuildMachine();
            if(buildMachine.equals("Unknown")) {
                return theVersion + "." + revision + ".local";
            }
            return theVersion + "." + revision + "." + buildMachine;
        }
        return theVersion + "." + revision + "." + buildNumber;
    }

    public String getVersion() {
        String theVersion = version;
        if(theVersion.endsWith("-SNAPSHOT")) {
            theVersion = theVersion.substring(0, theVersion.length() - "-SNAPSHOT".length());
        }
        return theVersion;
    }

    public String getFullVersion() {
        return version;
    }

    public String getRevision() {
        return revision;
    }

    public String getBuildNumber() {
        if(buildNumber.startsWith("$")) {
            return "Unknown";
        }
        return buildNumber;
    }

    public boolean isLocalVersion() {
        return buildNumber.startsWith("$");
    }

    public String getBuildTimestamp() {
        if(buildTimestamp.startsWith("$")) {
            return "Unknown";
        }
        return buildTimestamp;
    }

    public String getFormattedBuildTimestamp() {
        if(buildTimestamp.startsWith("$")) {
            return "Unknown";
        }
        try {
            DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMdd-HHmm");
            DateTime timestamp = format.parseDateTime(buildTimestamp);
            return timestamp.toString("MMMM d, yyyy HH:mm");
        } catch(Exception e) {
            return buildTimestamp;
        }
    }

    public String getBuildMachine() {
        if(computerName.startsWith("$")) {
            if(hostName.startsWith("$")) {
                return "Unknown";
            }
            return hostName;
        }
        return computerName;
    }
}

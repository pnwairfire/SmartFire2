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
package smartfire.util;

import java.text.DecimalFormat;
import org.apache.commons.lang.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.ReadableDateTime;
import org.kohsuke.stapler.Stapler;
import smartfire.Application;
import smartfire.VersionInfo;
import smartfire.database.Event;

/**
 * Utility functions used in views.
 */
public class Functions {
    private final Application myApp;

    public Functions() {
        // Look up our current Smartfire instance from Stapler
        Object app = Stapler.getCurrent().getWebApp().getApp();
        if(app instanceof Application) {
            myApp = (Application) app;
        } else {
            myApp = null;
        }
    }

    public String getResourcePath() {
        if(myApp.getVersion().isLocalVersion()) {
            return "";
        }
        return "/static/" + myApp.getVersion().getRevision();
    }

    public VersionInfo getVersion() {
        return myApp.getVersion();
    }

    public static String appendIfNotNull(String text, String suffix, String nullText) {
        return text == null ? nullText : text + suffix;
    }

    public static String appendUrlPiece(String url, String urlpiece) {
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        if(!url.endsWith("/")) {
            sb.append('/');
        }
        sb.append(urlpiece);
        return sb.toString();
    }

    public String convertDate(DateTime dt, String format) {
        DateTime dtz = dt.withZone(myApp.getAppSettings().getConfig().getDateTimeZone());
        return dtz.toString(format);
    }

    public String formatLongDate(DateTime dt) {
        return convertDate(dt, "EEEE, MMM d, yyyy");
    }

    public String formatShortDate(DateTime dt) {
        return convertDate(dt, "yyyy-MM-dd");
    }

    public String formatInterval(Interval interval) {
        return formatShortDate(interval.getStart()) + " to "
                + formatShortDate(interval.getEnd());
    }

    public String formatAcres(Double area) {
        if(area == null) {
            return "Unknown";
        }
        double acres = AreaUtil.squareMetersToAcres(area);
        DecimalFormat format = new DecimalFormat("#,###");
        return format.format(acres) + " acres";
    }

    public static String formatPercent(Double prob) {
        if(prob == null) {
            return "Unknown";
        }
        int percent = (int) Math.round(prob * 100);
        return percent + "%";
    }

    public static String formatLocation(Event event) {
        String county = event.get("CNTY");
        String state = event.get("STATE");
        String country = event.get("CNTRY");

        if(country == null) {
            String location = event.get("location");

            if(location != null) {
                return location;
            }

            return "Unknown";
        }

        if("USA".equals(country)) {
            return WordUtils.capitalizeFully(String.format("%s County, %s", county, state));
        } else if("CANADA".equals(country)) {
            return WordUtils.capitalizeFully(String.format("%s, %s, %s", county, state, country));
        } else if("MEXICO".equals(country)) {
            return WordUtils.capitalizeFully(String.format("%s, %s, %s", county, state, country));            
        } else {
            return WordUtils.capitalizeFully(String.format("%s", country));
        }
    }

    public static String formatGeneral(Object obj) {
        if(obj == null) {
            return "";
        } else if(obj instanceof String) {
            return (String) obj;
        } else if(obj instanceof Number) {
            DecimalFormat format = new DecimalFormat("#.##");
            return format.format(obj);
        } else if(obj instanceof java.util.Date || obj instanceof ReadableDateTime) {
            DateTime date = new DateTime(obj);
            if(date.getMillisOfDay() == 0) {
                return date.toString("yyyy-MM-dd");
            }
            // FIXME: Shouldn't we be converting to the local time zone here?  But we're in a static context so we don't have access to an AppSettings object
            return date.toString();
        } else {
            return obj.toString();
        }
    }
}

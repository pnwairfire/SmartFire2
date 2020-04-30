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

import java.util.ArrayList;
import org.joda.time.DateTime;
import org.joda.time.IllegalFieldValueException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Base class for views that can be indexed by date.
 */
abstract class DateIndexedView extends ModelView {
    private final Config config;

    protected DateIndexedView(Application app) {
        super(app);
        this.config = app.getAppSettings().getConfig();
    }

    /**
     * Child classes must override this to perform the actual lookup of
     * the view object, given the DateTimes parsed from query string
     * in a range lookup.
     * 
     * @param start the start DateTime parsed from the query string.
     * @param end the end DateTime parsed from the query string.
     * @return an object that will be used by Stapler for the view
     */
    protected abstract Object getViewObjectByDate(DateTime startDate, DateTime endDate);

    /**
     * Child classes must override this to provide the date that the "current"
     * URL maps to.
     * 
     * @return a DateTime representing the most current data available
     */
    protected abstract DateTime getCurrentDate();

    /*
     *  Views
     */
    public Object getDynamic(String urlPiece, StaplerRequest request, StaplerResponse response) throws Exception {
        if(!isValidDate(urlPiece)) {
            return null;
        }
        if(urlPiece.equals("range")) {
            // Get date range
            String startDate = request.getParameter("startDate");
            if(!isValidDate(startDate)) {
                return null;
            }
            String endDate = request.getParameter("endDate");
            if(!isValidDate(endDate)) {
                return null;
            }

            // Parse dates
            DateTime start = parseDate(startDate);
            DateTime end = parseDate(endDate).plusDays(1).minusMillis(1);

            return getViewObjectByDate(start, end);
        } else {
            DateTime dt = null;
            if(hasSpecialDate(urlPiece)) {
                dt = getSpecialDate(urlPiece);
            } else {
                dt = parseDate(urlPiece);
            }
            if(dt == null) {
                return null;
            }
            return getViewObjectByDate(dt, null);
        }
    }

    public void doFindDate(StaplerRequest request, StaplerResponse response) throws Exception {
        String date = request.getParameter("date");
        response.sendRedirect2(date);
    }

    /*
     * Helper methods
     */
    private DateTime getSpecialDate(String dateStr) {
        if(dateStr.equals("current")) {
            DateTime currentDate = getCurrentDate();
            if(currentDate == null) {
                currentDate = new DateTime();
            }
            return currentDate.toDateTime(config.getDateTimeZone()).toDateMidnight().toDateTime();
        } else if(dateStr.equals("today")) {
            return new DateTime(config.getDateTimeZone()).toDateMidnight().toDateTime();
        } else if(dateStr.equals("tomorrow")) {
            return new DateTime(config.getDateTimeZone()).plusDays(1).toDateMidnight().toDateTime();
        } else if(dateStr.equals("yesterday")) {
            return new DateTime(config.getDateTimeZone()).minusDays(1).toDateMidnight().toDateTime();
        } else {
            throw new SmartfireException("Cannot parse date information: " + dateStr);
        }
    }

    private DateTime parseDate(String dateStr) {
        int year = Integer.parseInt(dateStr.substring(0, 4));
        int month = Integer.parseInt(dateStr.substring(4, 6));
        int day = Integer.parseInt(dateStr.substring(6));
        try {
            return new DateTime(year, month, day, 0, 0, 0, 0, config.getDateTimeZone()).toDateMidnight().toDateTime();
        } catch(IllegalFieldValueException e) {
            return null;
        }
    }

    private boolean hasSpecialDate(String date) {
        ArrayList<String> specialDates = new ArrayList<String>();
        specialDates.add("current");
        specialDates.add("today");
        specialDates.add("tomorrow");
        specialDates.add("yesterday");
        specialDates.add("range");
        if(specialDates.contains(date.toLowerCase())) {
            return true;
        }
        return false;
    }

    private boolean isValidDate(String date) {
        if(date == null) {
            return false;
        }
        if(hasSpecialDate(date)) {
            return true;
        }
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
}

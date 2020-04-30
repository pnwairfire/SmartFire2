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
import java.util.ArrayList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

public enum Granularity {
    ONE_DAY("1 Day") {
        @Override
        public DateTime findStartDate(DateTime dt) {
            return dt.toDateMidnight().toDateTime();
        }
    
        @Override
        protected DateTime findNextDate(DateTime dt) {
            return dt.plusDays(1);
        }
    },
    ONE_WEEK("1 Week") {
        @Override
        public DateTime findStartDate(DateTime dt) {
            // Sunday is considered the last day of the week in Joda time.
            // Logic handles this by returning the Sunday of the week and then
            // subtracting a week to get the valid Sunday. If it is already
            // Sunday then just return that day.
            if(dt.getDayOfWeek() == DateTimeConstants.SUNDAY) {
                return dt.toDateMidnight().toDateTime();
            }
            return dt.toDateMidnight().withDayOfWeek(DateTimeConstants.SUNDAY).minusWeeks(1).toDateTime();
        }
    
        @Override
        protected DateTime findNextDate(DateTime dt) {
            return dt.plusWeeks(1);
        }
    },
    ONE_MONTH("1 Month") {
        @Override
        public DateTime findStartDate(DateTime dt) {
            return dt.toDateMidnight().withDayOfMonth(1).toDateTime();
        }
    
        @Override
        protected DateTime findNextDate(DateTime dt) {
            return dt.plusMonths(1);
        }
    },
    ONE_YEAR("1 Year") {
        @Override
        public DateTime findStartDate(DateTime dt) {
            return dt.toDateMidnight().withDayOfYear(1).toDateTime();
        }
    
        @Override
        protected DateTime findNextDate(DateTime dt) {
            return dt.plusYears(1);
        }
    };

    private Granularity(String readableName) {
        this.readableName = readableName;
    }
    private final String readableName;

    @Override
    public String toString() {
        return this.readableName;
    }

    /**
     * Find the start date of a time granularity. This corresponds to the
     * beginning of a day, the first day of a week, first day of a month, or
     * first day of the year.
     * 
     * NOTE: the dt parameter must already be in the correct time zone.
     *
     * @param dt DateTime value to determine the start date from.
     * @return a DateTime that corresponds with the start date of the time granularity.
     */
    public abstract DateTime findStartDate(DateTime dt);
    
    protected abstract DateTime findNextDate(DateTime dt);

    /**
     * Gets an iterable of intervals that are the size of the time granularity.
     *
     * @param st start time of the interval
     * @param end end time of the interval
     * @param zone the time zone in which to perform the calculation
     * @return an iterable of intervals that are the size of the time granularity.
     */
    public Iterable<Interval> getIntervals(DateTime st, DateTime end, DateTimeZone zone) {
        ArrayList<Interval> dateInterval = Lists.newArrayList();
        DateTime start = findStartDate(st.withZone(zone));
        DateTime next;
        while(!start.isAfter(end)) {
            next = findNextDate(start);
            dateInterval.add(new Interval(start, next.minusMillis(1)));
            start = next;
        }
        return dateInterval;
    }
}

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
import java.io.Writer;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import smartfire.database.DatabaseConnection;
import smartfire.database.JobStats;
import smartfire.database.Source;

/**
 * Basic status displays.
 */
@ExportedBean
public class Status {
    private final DatabaseConnection conn;
    private final JobStats stats;
    private final DateTime startDate;
    private final DateTime endDate;

    Status(DatabaseConnection conn) {
        this.conn = conn;
        this.endDate = new DateTime(DateTimeZone.UTC);
        this.startDate = endDate.minusDays(1);
        this.stats = conn.getJobHistory().getJobStats(startDate, endDate);
    }

    @WebMethod(name="status.txt")
    public void doSmartfirePrj(StaplerRequest request, StaplerResponse response) throws Exception {
        response.setHeader("Content-type", "text/plain");
        Writer writer = null;
        try {
            writer = response.getCompressedWriter(request);
            writer.write(getStatus());
        } finally {
            if(writer != null) {
                writer.close();
            }
        }
    }

    public Api getApi() {
        return new Api(this);
    }

    @Exported
    public String getStatus() {
        boolean ranToday = (stats.getTotalJobs() > 0);
        boolean failedToday = (stats.getFailedJobs() > 0);
        boolean succeededToday = (stats.getSuccessfulJobs() > 0);

        if(!ranToday) {
            return "LAGGING";
        }

        if(!failedToday) {
            return "NORMAL";
        }
        
        if(succeededToday) {
            return "PARTIAL";
        }

        return "FAILING";
    }

    @Exported
    public JobStats getStats() {
        return stats;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    @Exported(name="startDate")
    public String getStartDateString() {
        return startDate.toString();
    }

    public DateTime getEndDate() {
        return endDate;
    }

    @Exported(name="endDate")
    public String getEndDateString() {
        return endDate.toString();
    }

    @Exported
    public List<SourceStatus> getSources() {
        List<SourceStatus> result = Lists.newArrayList();
        for(Source source : conn.getSource().getAll()) {
            result.add(new SourceStatus(conn, source));
        }
        return result;
    }

    @ExportedBean(defaultVisibility=2)
    public static class SourceStatus {
        private final Source source;
        private final Interval dataInterval;
        private final Long dataCount;

        private SourceStatus(DatabaseConnection conn, Source source) {
            this.source = source;
            this.dataInterval = conn.getRawData().getDataInterval(source);
            this.dataCount = conn.getRawData().getDataCount(source);
        }

        @Exported
        public String getName() {
            return source.getName();
        }

        public DateTime getEarliestDataDate() {
            return dataInterval.getStart();
        }

        @Exported(name="earliestDataDate")
        public String getEarliestDataDateString() {
            return getEarliestDataDate().toString();
        }

        public DateTime getLatestDataDate() {
            return dataInterval.getEnd();
        }

        @Exported(name="latestDataDate")
        public String getLatestDataDateString() {
            return getLatestDataDate().toString();
        }
        
        public Long getDataCount() {
            return dataCount;
        }
    }
}

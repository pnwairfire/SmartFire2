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

import java.io.Writer;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.admin.Admin;
import smartfire.database.DatabaseConnection;
import smartfire.database.Event;
import smartfire.database.ReconciliationStream;
import smartfire.database.Source;

/**
 * Represents the URL entrypoint.  This is the object that Stapler will use as
 * the foundation of the URL hierarchy.
 */
public class SmartfireApp implements Application {
    private static final Logger log = LoggerFactory.getLogger(SmartfireApp.class);
    private final ApplicationSettings appSettings;
    private final DatabaseConnection conn;

    SmartfireApp(ApplicationSettings appSettings) {
        this.appSettings = appSettings;
        this.conn = appSettings.getDatabaseConnection();
    }

    public void doAdmin(StaplerRequest req, StaplerResponse res) throws Exception {
        if(req.isUserInRole("authenticated")) {
            Admin admin = new Admin(this);
            res.forward(admin, req.getRestOfPath(), req);
        } else {
            HttpSession session = req.getSession();
            session.setAttribute("loginFrom", req.getOriginalRequestURI());
            res.sendRedirect2(req.getContextPath() + "/login");
        }
    }

    public void doLogin(StaplerRequest req, StaplerResponse res) throws Exception {
        // When we get a login form posted back, the SecurityFilter will take
        // care of logging us in, so we just need to check whether we have a
        // valid user or not.
        if(req.isUserInRole("authenticated")) {
            HttpSession session = req.getSession();
            String intendedPath = (String) session.getAttribute("loginFrom");
            if(intendedPath != null) {
                session.removeAttribute("loginFrom");
                res.sendRedirect2(intendedPath);
            } else {
                res.sendRedirect2(req.getContextPath() + "/");
            }
        } else {
            res.forward(this, "loginForm", req);
        }
    }

    public void doLogout(StaplerRequest req, StaplerResponse res) throws Exception {
        req.getSession().invalidate();
        res.forward(this, "loggedOut", req);
    }

    public Data getData() {
        return new Data(this);
    }

    public JobsView getJobs() {
        return new JobsView(appSettings, conn);
    }
    
    public Events getEvents() {
        return new Events(this);
    }

    public Status getStatus() {
        return new Status(conn);
    }
    
    public Streams getStreams() {
        return new Streams(this);
    }

    Config getConfig() {
        return appSettings.getConfig();
    }

    DatabaseConnection getConnection() {
        return conn;
    }

    @Override
    public ApplicationSettings getAppSettings() {
        return this.appSettings;
    }

    @Override
    public VersionInfo getVersion() {
        return appSettings.getVersionInfo();
    }

    @WebMethod(name="smartfire.prj")
    public void doSmartfirePrj(StaplerRequest request, StaplerResponse response) throws Exception {
        response.setHeader("Content-type", "text/plain");
        response.setHeader("Content-disposition", "attachment;filename=smartfire.prj");
        Writer writer = null;
        try {
            writer = response.getCompressedWriter(request);
            writer.write(appSettings.getConfig().getCoordSysWKT());
        } finally {
            if(writer != null) {
                writer.close();
            }
        }
    }

    /*
     * Support functions
     */

    public List<Source> getAllSources() {
        return conn.getSource().getAll();
    }

    public List<ReconciliationStream> getAllStreams() {
        return conn.getReconciliationStream().getAll();
    }

    public Interval getDataInterval(Source source) {
        return conn.getRawData().getDataInterval(source);
    }
    
    public Long getDataCount(Source source) {
        return conn.getRawData().getDataCount(source);
    }

    public Interval getStreamInterval(ReconciliationStream stream) {
        DateTime start = null;
        DateTime end = null;
        for(Source source : stream.getSources()) {
            Interval interval = conn.getRawData().getDataInterval(source);
            if(start == null || start.isAfter(interval.getStart())) {
                start = interval.getStart();
            }
            if(end == null || end.isBefore(interval.getEnd())) {
                end = interval.getEnd();
            }
        }
        return new Interval(start, end);
    }

    public ReconciliationStream getRealtimeStream() {
        String realtimeStreamNameSlug = appSettings.getConfig().getRealtimeStreamNameSlug();
        return conn.getReconciliationStream().getByNameSlug(realtimeStreamNameSlug);
    }

    public List<Event> getTopEvents() {
        ReconciliationStream stream = getRealtimeStream();
        if(stream == null) {
            return Collections.emptyList();
        }
        return conn.getEvent().getTopEvents(stream, 10);
    }
}

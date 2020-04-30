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
import javax.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.DatabaseConnection;

/**
 * Filter to run before and after every servlet request, to manage our
 * Hibernate sessions for us.
 */
public class PersistenceFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(PersistenceFilter.class);
    private ServletContext context;

    @Override
    public void init(FilterConfig fc) throws ServletException {
        context = fc.getServletContext();
    }

    @Override
    public void destroy() {
        // No-op
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        
        // Look up our current Smartfire instance from the ServletContext
        Object app = context.getAttribute("app");
        if(app instanceof SmartfireApp) {
            DatabaseConnection conn = ((SmartfireApp) app).getConnection();
            conn.beginTransaction();
            boolean success = false;
            try {
                chain.doFilter(req, resp);
                success = true;
            } finally {
                if(!success) {
                    conn.rollbackOnly();
                }
                conn.resolveTransaction();
            }
        } else {
            chain.doFilter(req, resp);
        }
        org.geotools.referencing.CRS.cleanupThreadLocals();
        cleanUpStaplerThreadLocal();
    }

    private void cleanUpStaplerThreadLocal() {
        try {
            Class<?> klass = org.kohsuke.stapler.Stapler.class;
            java.lang.reflect.Field field = klass.getDeclaredField("HTTP_DATE_FORMAT");
            field.setAccessible(true);
            Object value = field.get(null);
            ThreadLocal<?> threadLocal = (ThreadLocal<?>) value;
            threadLocal.remove();
        } catch(Exception e) {
            log.debug("Unable to clear Stapler ThreadLocal field", e);
        }
    }
}

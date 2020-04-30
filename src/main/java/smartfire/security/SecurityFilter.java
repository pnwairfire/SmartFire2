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
package smartfire.security;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.Config;
import smartfire.SmartfireApp;

/**
 * Filter to handle authentication information.
 */
public class SecurityFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);
    private ServletContext context;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.context = filterConfig.getServletContext();
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpSession session = req.getSession();

        // Look up our current Smartfire instance from the ServletContext
        Object app = context.getAttribute("app");
        if(app instanceof SmartfireApp) {
            Config config = ((SmartfireApp) app).getAppSettings().getConfig();

            String username = request.getParameter("j_username");
            String passwordRaw = request.getParameter("j_password");

            if(username != null && passwordRaw != null) {
                log.debug("Got authentication request for username \"{}\"", username);
                
                User user = config.getUserMap().get(username);
                if(user == null) {
                    log.debug("Login Failure: No such user \"{}\"", username);
                } else if(!user.matchesPassword(passwordRaw)) {
                    log.debug("Login Failure: Password does not match for user \"{}\"", username);
                } else {
                    log.debug("Login Success! Authenticated password for user \"{}\"", username);
                    session.setAttribute("user", user);
                }
            }

            ServletRequest chainRequest = request;

            User user = (User) session.getAttribute("user");
            if(user != null) {
                chainRequest = new UserRoleRequestWrapper(req, user);
            }

            chain.doFilter(chainRequest, response);
        }
    }
}

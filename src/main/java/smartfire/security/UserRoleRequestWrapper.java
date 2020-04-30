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

import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * A wrapper for the HttpServletRequest that overrides the getUserPrincipal
 * and isUserInRole methods.
 *
 * Based on code by Travis Hein from: http://www.coderanch.com/t/466744/Servlets/java/Set-user-principal-filter
 */
public class UserRoleRequestWrapper extends HttpServletRequestWrapper {
    private final User user;
    private final HttpServletRequest request;

    public UserRoleRequestWrapper(HttpServletRequest request, User user) {
        super(request);
        this.user = user;
        this.request = request;
    }

    @Override
    public Principal getUserPrincipal() {
        return new SmartfireUserPrincipal(user);
    }

    @Override
    public boolean isUserInRole(String role) {
        return user.hasRole(role);
    }
}

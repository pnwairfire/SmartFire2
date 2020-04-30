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

import com.sti.justice.security.Hasher;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a user with access to the SMARTFIRE web application.
 */
@XStreamAlias("user")
public class User {
    private final String username;
    private final String password;
    @XStreamImplicit(itemFieldName="role")
    private final Set<String> roles;

    private User(String username, String password, Set<String> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    private User() {
        this.username = "postgres";
        this.password = "admin";
        this.roles = Collections.emptySet();
    }

    public static User newUser(String username, String passwordRaw) {
        Set<String> roleSet = Collections.emptySet();
        return new User(username, hashPassword(username, passwordRaw), roleSet);
    }

    private static String hashPassword(String username, String passwordRaw) {
        return Hasher.generateMD5Hash(username + passwordRaw);
    }

    public String getUsername() {
        return username;
    }

    public boolean matchesPassword(String passwordRaw) {
        return password.equals(hashPassword(username, passwordRaw));
    }

    public boolean hasRole(String role) {
        if("authenticated".equals(role)) {
            return true;
        }
        return roles.contains(role);
    }
}

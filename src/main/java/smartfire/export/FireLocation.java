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
package smartfire.export;

import java.util.HashMap;
import java.util.Map;
import org.joda.time.LocalDate;

public class FireLocation {
    private final String id;
    private final double area;
    private final LocalDate date;
    private final String latitude;
    private final String longitude;
    private final Map<String, Object> attributes;

    public FireLocation(String id, double area, LocalDate date, String latitude, String longitude) {
        this.id = id;
        this.area = area;
        this.date = date;
        this.latitude = latitude;
        this.longitude = longitude;
        this.attributes = new HashMap<String, Object>();
    }

    public double getArea() {
        return area;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getId() {
        return id;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
    
    public boolean hasAttribute(String key) {
        return this.attributes.containsKey(key);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }
    
    public void setAttribute(String key, Object obj) {
        this.attributes.put(key, obj);
    }
    
    @Override
    public int hashCode() {
        String hash = date.toString() + '-' + latitude + '-' + longitude;
        return hash.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final FireLocation other = (FireLocation) obj;
        if((this.date == null) ? (other.date != null) : !this.date.equals(other.date)) {
            return false;
        }
        if((this.latitude == null) ? (other.latitude != null) : !this.latitude.equals(other.latitude)) {
            return false;
        }
        if((this.longitude == null) ? (other.longitude != null) : !this.longitude.equals(other.longitude)) {
            return false;
        }
        return true;
    }
}

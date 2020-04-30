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

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FireLocationSet implements Iterable<FireLocation>, Set<FireLocation> {
    private static final double ACRES_PER_SQ_METER = 0.000247105381;
    private final Map<Integer, FireLocation> fireLocations;
    private double totalArea;
    private final double eventTotalArea;

    public FireLocationSet(double eventTotalArea) {
        this.eventTotalArea = eventTotalArea;
        this.fireLocations = Maps.newHashMap();
        this.totalArea = 0.0;
    }

    public double scaleLocationArea(double locationArea) {
        double scaledClumpArea = 0.0;
        if (totalArea > 0.0) {
            scaledClumpArea = (locationArea / totalArea) * eventTotalArea;
        }
        return scaledClumpArea * ACRES_PER_SQ_METER;
    }

    @Override
    public Iterator<FireLocation> iterator() {
        return fireLocations.values().iterator();
    }

    @Override
    public int size() {
        return fireLocations.size();
    }

    @Override
    public boolean isEmpty() {
        return fireLocations.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if(o instanceof Integer) {
            return fireLocations.containsKey((Integer) o);
        }
        return false;
    }

    @Override
    public Object[] toArray() {
        return fireLocations.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return fireLocations.values().toArray(a);
    }
    
    public void addArea(double area) {
        totalArea += area;
    }

    /**
     * Add new fire location. If there is a fire location with the same datetime and location (lat/lon) then keep the one with the largest area.
     *
     * @param location
     * @return
     */
    @Override
    public boolean add(FireLocation location) {
        int locationHash = location.hashCode();
        if(!fireLocations.containsKey(location.hashCode())) {
            fireLocations.put(locationHash, location);
            addArea(location.getArea());
            return true;
        } else {
            FireLocation currentLocation = fireLocations.get(locationHash);
            if(location.getArea() > currentLocation.getArea()) {
                fireLocations.put(locationHash, location);
                addArea(location.getArea() - currentLocation.getArea()); // Area difference
            }
            return false;
        }
    }

    @Override
    public boolean remove(Object o) {
        if(o instanceof FireLocation) {
            FireLocation removalObj = (FireLocation) o;
            FireLocation removed = fireLocations.remove(removalObj.hashCode());
            if(removed == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean addAll(Collection<? extends FireLocation> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        fireLocations.clear();
    }
}

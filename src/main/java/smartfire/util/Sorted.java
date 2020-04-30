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
package smartfire.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class for returning sorted views of existing data.
 * 
 * @see com.google.common.collect.Ordering
 * @see java.util.Comparator
 */
public final class Sorted {
    private Sorted() { }
    
    private static <T> List<T> defensiveCopy(Iterable<T> items) {
        List<T> result = new ArrayList<T>();
        for(T item : items) {
            result.add(item);
        }
        return result;
    }
    
    /**
     * Returns a new list that contains the elements from items, ordered
     * according to their natural ordering.  The objects must implement
     * {@link Comparable}.
     * 
     * @param <T> the type of elements in the list
     * @param items the items to sort
     * @return a new list with the elements sorted in natural order
     */
    public static <T extends Comparable<T>> List<T> naturally(Iterable<T> items) {
        List<T> result = defensiveCopy(items);
        Collections.sort(result);
        return result;
    }
    
    /**
     * Returns a new list that contains the elements from items, ordered 
     * according to the given comparator.
     * 
     * @param <T> the type of items in the list
     * @param items the items to sort
     * @param comparator a comparator expressing the desired ordering
     * @return a new list with the elements sorted in the desired ordering
     */
    public static <T> List<T> by(Iterable<T> items, Comparator<T> comparator) {
        List<T> result = defensiveCopy(items);
        Collections.sort(result, comparator);
        return result;
    }
}

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
package smartfire.func.fetch;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;

public abstract class AbstractFetchResultsIterator implements Iterator<RawData> {
    private static final Logger log = LoggerFactory.getLogger(AbstractFetchResultsIterator.class);
    protected final DateTime expectedDateTime;
    protected /*final*/ String[] fieldNames;
    protected final Iterator<Object[]> iter;

    public AbstractFetchResultsIterator(DateTime fetchDate, String[] fieldNames, Iterator<Object[]> iter) {
        this.expectedDateTime = fetchDate.toDateMidnight().toDateTime();
        this.fieldNames = fieldNames;
        this.iter = iter;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    protected Map<String, Object> getFields(Object[] row) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for(int i = 0; i < row.length; i++) {
            fields.put(fieldNames[i], row[i]);
        }
        return fields;
    }

    @Override
    public abstract RawData next();

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

    public static double getDouble(Map<String, Object> row, String fieldName) {
        Object value = row.get(fieldName);
        if(value == null) {
            return -9999; // Should we throw an exception instead?
        }
        if(value instanceof Double) {
            return ((Double) value).doubleValue();
        }
        if(value instanceof String) {
            String strValue = (String) value;
            if(strValue.isEmpty()) {
                return -9999;
            }
            return Double.valueOf(strValue);
        }
        log.warn("Unexpected type for {}: {}", fieldName, value.getClass());
        return -9999;
    }

    public static long getLong(Map<String, Object> row, String fieldName) {
        Object value = row.get(fieldName);
        if(value == null) {
            return -9999; // Should we throw an exception instead?
        }
        if(value instanceof Long) {
            return ((Long) value).longValue();
        }
        if(value instanceof String) {
            return Long.valueOf((String) value);
        }
        log.warn("Unexpected type for {}: {}", fieldName, value.getClass());
        return -9999;
    }
    
    public static boolean isNull(Map<String, Object> row, String fieldName) {
        Object value = row.get(fieldName);
        return value == null || value.equals("null");
    }

    public static Map<String, Object> renameKey(Map<String, Object> row, String oldKey, String newKey) {
        if(row.containsKey(oldKey)) {
            row.put(newKey, row.get(oldKey));
            row.remove(oldKey);
        }
        return row;
    }
}

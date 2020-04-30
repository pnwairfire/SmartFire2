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

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import org.joda.time.DateTime;
import smartfire.database.RawData;
import smartfire.func.FetchMethod;

abstract class AbstractFetchMethod implements FetchMethod {
    public abstract Iterator<RawData> getFetchResultsIterator(DateTime fetchDate, String[] fieldNames, Iterator<Object[]> iter);

    protected class FetchResults extends AbstractCollection<RawData> {
        private final DateTime fetchDate;
        private final String[] fieldNames;
        private final List<Object[]> data;

        public FetchResults(DateTime fetchDate, String[] fieldNames, List<Object[]> data) {
            this.fetchDate = fetchDate;
            this.fieldNames = fieldNames;
            this.data = data;
        }

        @Override
        public Iterator<RawData> iterator() {
            return getFetchResultsIterator(fetchDate, fieldNames, data.iterator());
        }

        @Override
        public int size() {
            return data.size();
        }
    }
}

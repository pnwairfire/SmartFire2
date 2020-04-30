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
package smartfire.gis;

import java.util.List;
import org.joda.time.DateTime;
import smartfire.database.Fire;
import smartfire.database.FireDao;
import smartfire.database.Source;

/**
 * Represents a collection of Fires, backed by a FireDao, with enhanced query
 * capabilities.  Subsequent queries are guaranteed to retrieve the same Fire
 * instances, so that they can be mutated.
 */
public class QueryableFireSet extends AbstractQueryableSet<Fire, Integer, FireDao> {
    private final Source source;
    
    public QueryableFireSet(FireDao fireDao, Source source) {
        super(fireDao);
        this.source = source;
    }

    public Source getSource() {
        return source;
    }
    
    @Override
    protected List<Fire> fetchByDate(FireDao dao, DateTime startDate, DateTime endDate) {
        return dao.getByDate(source, startDate, endDate);
    }

    @Override
    protected Fire mergeInternal(FireDao dao, List<Fire> toMerge) {
        return dao.merge(toMerge);
    }
}

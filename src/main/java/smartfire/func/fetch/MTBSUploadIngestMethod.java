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

import com.vividsolutions.jts.geom.MultiPolygon;
import java.util.*;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.fileimport.ShapeFileParser;
import smartfire.func.UploadIngestMethod;
import smartfire.gis.GeometryBuilder;

/**
 * The MTBS Upload Ingest method.
 */
@MetaInfServices
public class MTBSUploadIngestMethod implements UploadIngestMethod {
    private static final Logger log = LoggerFactory.getLogger(MTBSUploadIngestMethod.class);
    private String fireNameField; // Name of the fire name field on the source.
    private String associationField; // Name of the association field on the source MTBSAssociationMethod.
    private String fireTypeField; // Name of the fire type field on the source ParseMTBSFireTypeMethod.
    private final Source source;
    private final GeometryBuilder geometryBuilder;

    public MTBSUploadIngestMethod(Source source, GeometryBuilder geometryBuilder) {
        this.source = source;
        this.geometryBuilder = geometryBuilder;
        this.fireNameField = source.getFireNameField();
        this.associationField = source.getSourceAttributes().get("associationField").getAttrValue();
        this.fireTypeField = source.getSourceAttributes().get("fireTypeField").getAttrValue();

    }

    @Override
    public Collection<RawData> ingest(String filePath, DateTime dateTime) throws Exception {
        ShapeFileParser shapeFile = new ShapeFileParser(filePath, geometryBuilder.getCoordSysWKT());
        return new ResultsCollection(dateTime, shapeFile.getFieldNames(), shapeFile.getData());
    }

    private class ResultsCollection extends AbstractCollection<RawData> {
        private final DateTime date;
        private final String[] fieldNames;
        private final List<Object[]> data;

        public ResultsCollection(DateTime date, String[] fieldNames, List<Object[]> data) {
            this.date = date;
            this.fieldNames = fieldNames;
            this.data = data;
        }

        @Override
        public Iterator<RawData> iterator() {
            return new MTBSResultsIterator(date, fieldNames, data);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private class MTBSResultsIterator extends AbstractFetchResultsIterator {
        public MTBSResultsIterator(DateTime date, String[] fieldNames, List<Object[]> data) {
            super(date, fieldNames, data.iterator());
        }

        @Override
        public RawData next() {
            RawData result = new RawData();
            /*
             * SF-289:
             *   Fixes issue with arbitrary field capitalization.
             *   Converts all keys to lowercase, and 'get' call become case insensitive.
             */
            Map<String, Object> row = new CaseInsensitiveMap(getFields(iter.next()));

            // Get area
            double area = getDouble(row, "Area");

            // Get Shape
            MultiPolygon shape = (MultiPolygon) row.get("the_geom");

            // Remove shape from attributes.
            row.remove("the_geom");

            // Build geometry
            result.setShape(shape);
            result.setArea(area);
            result.setSource(source);

            // Get start date time
            int year = (Integer) row.get("Year");
            int month = (Integer) row.get("StartMonth");
            if(month == 0 || month > 12) {
                month = 1;
            }
            int day = (Integer) row.get("StartDay");
            if(day == 0 || day > 31) {
                day = 1;
            }
            DateTime startDate = new DateTime(year, month, day, 0, 0, 0, 0);

            // End date same as start date
            DateTime endDate = startDate.plusDays(1).minusMillis(1);

            // set start and end dates
            result.setStartDate(startDate);
            result.setEndDate(endDate);

            /* 
             * SF-289:
             *   Preserve the casing of the fields set on the source so as not to break historically uploaded data.
             *   Use the lowercase fields for everything else.
             */
            for(String key : row.keySet()) {
                if(key.equals(fireNameField.toLowerCase())) {
                    result.put(fireNameField, row.get(key).toString());
                }
                else if(key.equals(associationField.toLowerCase())) {
                    result.put(associationField, row.get(key).toString());
                }
                else if(key.equals(fireTypeField.toLowerCase())) {
                    result.put(fireTypeField, row.get(key).toString());
                }
                else if(row.get(key) != null) {
                    result.put(key, row.get(key).toString());
                } else {
                    log.warn("Null value encountered for record key {}. Replacing with empty string", key);
                    result.put(key, "");
                }
            }

            return result;
        }
    }
}

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
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.RawData;
import smartfire.database.Source;
import smartfire.fileimport.ShapeFileParser;
import smartfire.func.Attribute;
import smartfire.func.UploadIngestMethod;
import smartfire.gis.GeometryBuilder;
import smartfire.util.AreaUtil;

/**
 * The MTBS Upload Ingest method.
 */
@MetaInfServices
public class PolygonUploadIngestMethod implements UploadIngestMethod {
    private static final Logger log = LoggerFactory.getLogger(PolygonUploadIngestMethod.class);
    private final Source source;
    private final GeometryBuilder geometryBuilder;
    private final String startDateFieldName;
    private final String areaBurnedFieldName;
    private final String endDateFieldName;
    private final String datePattern;

    public PolygonUploadIngestMethod(Source source, GeometryBuilder geometryBuilder,
            @Attribute(name = "startDateFieldName",
                    description = "Name of the start date field (format is M/D/Y).") String startDateFieldName,
            @Attribute(name = "areaBurnedFieldName", 
                    description = "Name of the area burned field (area must be in acres).") String areaBurnedFieldName,
            @Attribute(name = "endDateFieldName",
                    description = "Name of the end date field (leave blank for none).") String endDateFieldName,
            @Attribute(name = "datePattern",
                    description = "Date pattern (using Jave date formatting patterns)") String datePattern) {
        this.source = source;
        this.geometryBuilder = geometryBuilder;
        this.startDateFieldName = startDateFieldName;
        this.areaBurnedFieldName = areaBurnedFieldName;
        this.endDateFieldName = endDateFieldName;
        this.datePattern = datePattern;
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
            return new PolygonResultsIterator(date, fieldNames, data);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private class PolygonResultsIterator extends AbstractFetchResultsIterator {
        public PolygonResultsIterator(DateTime date, String[] fieldNames, List<Object[]> data) {
            super(date, fieldNames, data.iterator());
        }

        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());

            // Get area
            double area = getDouble(row, areaBurnedFieldName);
            if(area <= 0 ) {
                log.warn("Record has invalid area: {}; ignoring", area);
                return null;
            }
            double areaSqMeters;
            areaSqMeters = AreaUtil.acresToSquareMeters(area);

            // Get Shape
            MultiPolygon shape = (MultiPolygon) row.get("the_geom");

            // Remove shape from attributes.
            row.remove("the_geom");

            // Build geometry
            result.setShape(shape);
            result.setArea(areaSqMeters);
            result.setSource(source);
            
            // Set start date time
            DateTime startDate = parsePatternDate((String) row.get(startDateFieldName), datePattern);
            if(startDate == null) {
               log.warn("Record has invalid start date: {}; ignoring", startDate);
                  return null;
              }

              // Determine End Date
              DateTime endDate;
              if(!"".equals(endDateFieldName)) {
                  endDate = parsePatternDate((String) row.get(endDateFieldName), datePattern);
                  if(endDate == null) {
                      log.warn("Record has invalid end date: {}; using start date", endDate);
                      endDate = startDate;
                  }
              } else {
                  endDate = startDate;
              }

            // set start and end dates
            result.setStartDate(startDate);
            result.setEndDate(endDate);

            for(String key : row.keySet()) {
                if(row.get(key) != null) {
                    result.put(key, row.get(key).toString());
                } else {
                    log.warn("Null value encountered for record key {}. Replacing with empty string", key);
                    result.put(key, "");
                }
            }

            return result;
        }
    }
    
        static DateTime parsePatternDate(String dateString, String datePattern) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern(datePattern);
        try {
            DateTime dt = fmt.parseDateTime(dateString);
            int month = dt.getMonthOfYear();
            int day = dt.getDayOfMonth();
            int year = dt.getYear();
            return new DateTime(year, month, day, 0, 0, 0, 0);
        } catch (IllegalArgumentException e) {
            log.warn("Date does not conform to specified pattern \"{}\"; ignoring", dateString);
            return null;
        }
        
    }
}

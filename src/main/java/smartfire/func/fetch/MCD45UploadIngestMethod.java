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

import com.google.common.primitives.Ints;
import com.vividsolutions.jts.geom.MultiPolygon;
import java.io.File;
import java.util.*;
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
 * The MCD45 (MODIS Burn Scar) Upload Ingest method for shapefiles from UMD.
 */
@MetaInfServices(UploadIngestMethod.class)
public class MCD45UploadIngestMethod implements UploadIngestMethod {
    private static final Logger log = LoggerFactory.getLogger(MCD45UploadIngestMethod.class);
    private final String defaultCoordSysWKT = "GEOGCS[\"\",DATUM[\"D_unknown\",SPHEROID[\"Unknown\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]]"; // Copy of MCD45monthly.A2014091.Win03.051.burndate.prj WKT with fix to make it parse without error
    private final Source source;
    private final GeometryBuilder geometryBuilder;
    private String fileName;

    public MCD45UploadIngestMethod(Source source, GeometryBuilder geometryBuilder) {
        this.source = source;
        this.geometryBuilder = geometryBuilder;
    }

    @Override
    public Collection<RawData> ingest(String filePath, DateTime dateTime) throws Exception {
        setFilename(filePath);
        ShapeFileParser shapeFile = new ShapeFileParser(filePath, geometryBuilder.getCoordSysWKT(), defaultCoordSysWKT);
        return new ResultsCollection(dateTime, shapeFile.getFieldNames(), shapeFile.getData());
    }
    
    private void setFilename(String filePath) {
        File file = new File(filePath);
        fileName = file.getName();
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
            return new MCD45ResultsIterator(date, fieldNames, data);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private class MCD45ResultsIterator extends AbstractFetchResultsIterator {
        public MCD45ResultsIterator(DateTime date, String[] fieldNames, List<Object[]> data) {
            super(date, fieldNames, data.iterator());
        }

        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());

            // Get Shape
            MultiPolygon shape = (MultiPolygon) row.get("the_geom");

            // Remove shape from attributes.
            row.remove("the_geom");

            // Build geometry
            result.setShape(shape);
            result.setArea(0.0);  //no area field in raw data
            result.setSource(source);

            // Get start date time (stored in BurnDate as a Julian day)
            DateTime startDate;
            try {
                int julDay = parseJulianDay(row.get("BurnDate"));
                int year = parseYearFromFileName();
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year); ///need a way to pass this (do parameters show up at upload?
                calendar.set(Calendar.DAY_OF_YEAR, julDay);
                startDate = new DateTime(calendar);
            } catch(Exception e) {
                log.warn("Unable to determine start date; ignoring");
                return null;
            }

            // End date same as start date
            DateTime endDate = startDate.plusDays(1).minusMillis(1);

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
        
        // This handles an issue where the BurnDate attribute returns the Julian date as a Long instead of an Integer
        private int parseJulianDay(Object burnDate) {
            try {
                return (Integer) burnDate;
            } catch (ClassCastException e) {
                // If for some reason BurnDate is not an Integer, try casting it from a Long
                return Ints.checkedCast((Long) burnDate);
            }
        }
        
        private int parseYearFromFileName() {
            String burnDate = fileName.split("\\.")[1]; // MCD45monthly.A2014091.Win03.051.burndate.shapefiles -> A2014091
            return Integer.parseInt(burnDate.substring(1, 5)); // A2014091 -> 2014
        }
    }
}

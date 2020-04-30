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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
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
import smartfire.util.AreaUtil;

/**
 * The GeoMac Upload Ingest method.
 */
@MetaInfServices
public class GeoMacUploadIngestMethod implements UploadIngestMethod {
    private static final Logger log = LoggerFactory.getLogger(GeoMacUploadIngestMethod.class);
    private final Source source;
    private final GeometryBuilder geometryBuilder;

    public GeoMacUploadIngestMethod(Source source, GeometryBuilder geometryBuilder) {
        this.source = source;
        this.geometryBuilder = geometryBuilder;
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
            return new GeoMacResultsIterator(date, fieldNames, data);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private class GeoMacResultsIterator extends AbstractFetchResultsIterator {
        public GeoMacResultsIterator(DateTime date, String[] fieldNames, List<Object[]> data) {
            super(date, fieldNames, data.iterator());
        }

        @Override
        public RawData next() {
            RawData result = new RawData();
            Map<String, Object> row = getFields(iter.next());

            // Get area
            double areaAcres = getDouble(row, "ACRES");
            double area = AreaUtil.acresToSquareMeters(areaAcres);

            // Get Shape
            double scale = 10000.0; // Round to the nearest thousandth 
            MultiPolygon shape = reduceMultiPolygonPrecision((MultiPolygon) row.get("the_geom"), scale);

            // Remove shape from attributes.
            row.remove("the_geom");

            // Build geometry
            result.setShape(shape);
            result.setArea(area);
            result.setSource(source);

            // Get start date time
            Date date = (Date) row.get("DATE_");
            DateTime startDate = new DateTime(date);
            DateTime endDate = startDate;

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
    
    private static MultiPolygon reduceMultiPolygonPrecision(MultiPolygon multiPoly, double scale) {
        PrecisionModel precision = new PrecisionModel(scale);
        Geometry reducedGeom = com.vividsolutions.jts.precision.GeometryPrecisionReducer.reduce((Geometry) multiPoly, precision);
        if(reducedGeom instanceof Polygon) { // Convert Polygon into Multi
            GeometryFactory geometryFactory = new GeometryFactory();
            Polygon[] polygonArray = new Polygon[]{(Polygon) reducedGeom};
            reducedGeom = new MultiPolygon(polygonArray, geometryFactory);
        }
        return (MultiPolygon) reducedGeom;
    }
}

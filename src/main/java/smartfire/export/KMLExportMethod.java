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

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import de.micromata.opengis.kml.v_2_2_0.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.StaplerRequest;
import smartfire.ApplicationSettings;
import smartfire.SmartfireException;
import smartfire.gis.CoordinateTransformer;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.GeometryEntity;
import smartfire.gis.XYPoint;

/**
 * ExportMethod for exporting as KML data.
 */
@MetaInfServices(ExportMethod.class)
public class KMLExportMethod extends AbstractExportMethod<Exportable> implements ExportMethod {
    private static final DecimalFormat df = new DecimalFormat("#.##");

    public KMLExportMethod() {
        super("KML", "kml", "/images/icons/kmlfile-32x32.png", Exportable.class, "application/vnd.google-earth.kml+xml", ".kml");
    }

    @Override
    protected void performExport(
            StaplerRequest request,
            OutputStream stream, 
            ApplicationSettings appSettings,
            String exportFileName,
            List<Exportable> entities,
            DateTime startDate,
            DateTime endDate
            ) throws IOException {
        createKml(stream, appSettings.getGeometryBuilder(), exportFileName, entities);
    } 

    public static void createKml(OutputStream out, GeometryBuilder geometryBuilder,
            String folderName, Iterable<? extends Exportable> records) throws FileNotFoundException {

        // Set up KML file
        final Kml kml = new Kml();
        final Document document = kml.createAndSetDocument();
        
        final Style fire = document.createAndAddStyle().withId("fire");
        fire.createAndSetIconStyle().withColor("FFE22B8A").withScale(0.2)
            .createAndSetIcon().withHref("http://www.getbluesky.org/images/drawSquare.png"); // FIXME: Hard coded icon
        
        final Style poly = document.createAndAddStyle().withId("poly");
        poly.createAndSetLineStyle()
            .withColor("CC4C4CA6");
        poly.createAndSetPolyStyle()
            .withColor("CC4C4CA6")
            .withFill(true)
            .withOutline(true);
        
        final Folder folder = document.createAndAddFolder();
        folder.setName(folderName);

        // Add placemarks to KML file
        Iterable<Placemark> placemarks = buildPlacemarks(geometryBuilder, records);
        for(Placemark placemark : placemarks) {
            folder.addToFeature(placemark);
        }
        
        kml.marshal(out);
    }

    public static Iterable<Placemark> buildPlacemarks(GeometryBuilder geometryBuilder,
            Iterable<? extends GeometryEntity> entities) {
        CoordinateTransformer lonLatTransformer = geometryBuilder.newLonLatOutputTransformer();
        return new PlacemarkIterable(lonLatTransformer, entities);
    }

    private static class PlacemarkIterable implements Iterable<Placemark> {
        private final CoordinateTransformer lonLatTransformer;
        private final Iterable<? extends GeometryEntity> entities;

        public PlacemarkIterable(CoordinateTransformer lonLatTransformer,
                Iterable<? extends GeometryEntity> entities) {
            this.lonLatTransformer = lonLatTransformer;
            this.entities = entities;
        }

        @Override
        public Iterator<Placemark> iterator() {
            return new PlacemarkIterator(lonLatTransformer, entities.iterator());
        }
    }

    private static class PlacemarkIterator implements Iterator<Placemark> {
        private final CoordinateTransformer lonLatTransformer;
        private final Iterator<? extends GeometryEntity> iter;

        public PlacemarkIterator(CoordinateTransformer lonLatTransformer,
                Iterator<? extends GeometryEntity> iter) {
            this.lonLatTransformer = lonLatTransformer;
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Placemark next() {
            GeometryEntity entity = iter.next();
            return buildPlacemark(lonLatTransformer, entity);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    private static Placemark buildPlacemark(CoordinateTransformer lonLatTransformer, GeometryEntity entity) {
        Geometry geom = entity.getShape();
        if(geom instanceof Point) {
            return buildPointPlacemark(lonLatTransformer, (Point) geom, entity.getShapeName());
        }
        if(geom instanceof MultiPolygon) {
            return buildMultiPolygonPlacemark(lonLatTransformer, (MultiPolygon) geom, entity.getShapeName());
        }
        if(geom instanceof com.vividsolutions.jts.geom.Polygon) {
            return buildPolygonPlacemark(lonLatTransformer, (com.vividsolutions.jts.geom.Polygon) geom, entity.getShapeName());
        }

        throw new SmartfireException("Error exporting to KML: unable to construct KML for \""
                + geom.getGeometryType() + "\" geometry");
    }

    private static Placemark buildPointPlacemark(CoordinateTransformer xform, Point point, String name) {
        XYPoint lonlat = xform.transform(point.getX(), point.getY());
        double lon = lonlat.getX();
        double lat = lonlat.getY();
        
        // Set name if empty
        String fullname = name;
        if(fullname.isEmpty()) {
            fullname = "(" + df.format(lat) + ", " + df.format(lon) + ")";
        }
        
        // Put point in kml
        Placemark placemark = new Placemark();
        placemark
                .withName(fullname)
                .withOpen(Boolean.FALSE)
                .withStyleUrl("#fire")
                .createAndSetPoint()
                .addToCoordinates(lon, lat);
        return placemark;
    }

    private static Placemark buildMultiPolygonPlacemark(CoordinateTransformer xform, MultiPolygon mp, String name) {
        XYPoint lonlat = xform.transform(mp.getCentroid().getX(), mp.getCentroid().getY());
        double lon = lonlat.getX();
        double lat = lonlat.getY();
        
        // Set name if empty
        String fullname = name;
        if(fullname.isEmpty()) {
            fullname = "(" + df.format(lat) + ", " + df.format(lon) + ")";
        }
        
        final Placemark placemark = new Placemark();
        placemark.setStyleUrl("#poly");
        MultiGeometry multi = convertMultiPolygon(mp, xform);
        placemark.withName(fullname);
        placemark.setGeometry(multi);
        return placemark;
    }

    private static Placemark buildPolygonPlacemark(CoordinateTransformer xform, com.vividsolutions.jts.geom.Polygon poly, String name) {
        XYPoint lonlat = xform.transform(poly.getCentroid().getX(), poly.getCentroid().getY());
        double lon = lonlat.getX();
        double lat = lonlat.getY();
        
        // Set name if empty
        String fullname = name;
        if(fullname.isEmpty()) {
            fullname = "(" + df.format(lat) + ", " + df.format(lon) + ")";
        }
        
        final Placemark placemark = new Placemark();
        placemark.setStyleUrl("#poly");
        final Polygon polygon = convertPolygon(xform, poly);
        placemark.withName(fullname);
        placemark.setGeometry(polygon);
        return placemark;
    }

    private static MultiGeometry convertMultiPolygon(MultiPolygon mp, CoordinateTransformer xform) {
        final MultiGeometry multi = new MultiGeometry();
        final int numPolygons = mp.getNumGeometries();
        for(int i = 0; i < numPolygons; i++) {
            final com.vividsolutions.jts.geom.Polygon poly =
                    (com.vividsolutions.jts.geom.Polygon) mp.getGeometryN(i);
            Polygon polygon = convertPolygon(xform, poly);
            multi.addToGeometry(polygon);
        }
        return multi;
    }

    private static Polygon convertPolygon(CoordinateTransformer xform, com.vividsolutions.jts.geom.Polygon poly) {
        final Polygon result = new Polygon();
        result.setExtrude(true);
        result.setAltitudeMode(AltitudeMode.CLAMP_TO_GROUND);

        final Boundary outerBoundary = new Boundary();
        final LinearRing outerLinearRing = traceRing(xform, poly.getExteriorRing());
        outerBoundary.setLinearRing(outerLinearRing);
        result.setOuterBoundaryIs(outerBoundary);

        final int numInteriorRings = poly.getNumInteriorRing();
        for(int i = 0; i < numInteriorRings; i++) {
            final Boundary innerBoundary = new Boundary();
            final LinearRing ring = traceRing(xform, poly.getInteriorRingN(i));
            innerBoundary.setLinearRing(ring);
            result.addToInnerBoundaryIs(innerBoundary);
        }

        return result;
    }

    private static LinearRing traceRing(CoordinateTransformer xform, LineString line) {
        final List<Coordinate> coords = Lists.newArrayList();
        for(com.vividsolutions.jts.geom.Coordinate coord : line.getCoordinates()) {
            XYPoint point = xform.transform(coord.x, coord.y);
            double lat = point.getY();
            double lon = point.getX();
            coords.add(new Coordinate(lon, lat));
        }
        
        final LinearRing result = new LinearRing();
        result.setCoordinates(coords);
        return result;
    }
}

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
import com.vividsolutions.jts.geom.Geometry;
import java.text.DecimalFormat;
import java.util.*;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.XYPoint;
import smartfire.util.AreaUtil;
import smartfire.util.Functions;

/**
 * Represents a single row of exported data.
 */
public class ExportRow implements Iterable<String> {
    private final DecimalFormat coordFormat = new DecimalFormat("#.###");
    private final List<String> attributeNames;
    private final Exportable entity;
    private final XYPoint latLonPoint;
    
    public static final Comparator<ExportRow> BY_AREA_DESC = new Comparator<ExportRow>() {
        @Override
        public int compare(ExportRow a, ExportRow b) {
            return Double.compare(b.entity.getArea(), a.entity.getArea());
        }
    };
    
    ExportRow(GeometryBuilder geometryBuilder, List<String> attributeNames, Exportable entity) {
        this.entity = entity;
        this.attributeNames = Collections.unmodifiableList(attributeNames);
        this.latLonPoint = geometryBuilder.buildLatLonFromPoint(
                    entity.getExportPointX(), entity.getExportPointY());
    }
    
    public List<String> getHeadings() {
        return attributeNames;
    }
    
    public Geometry getExportShape() {
        return entity.getShape();
    }
    
    public Object getExportMember(String key) {
        if("area_acres".equals(key)) {
            return Long.valueOf(Math.round(AreaUtil.squareMetersToAcres(entity.getArea())));
        } else if("area_meters".equals(key)) {
            return Long.valueOf(Math.round(entity.getArea()));
        } else if("latitude".equals(key)) {
            return coordFormat.format(latLonPoint.getY());
        } else if("longitude".equals(key)) {
            return coordFormat.format(latLonPoint.getX());
        } else if("start_date".equals(key)) {
            return entity.getStartDateTime().toDate();
        } else if("end_date".equals(key)) {
            return entity.getEndDateTime().toDate();
        } else {
            return entity.getExtraExportMember(key);
        }
    }
    
    public String getFormattedMember(String key) {
        return Functions.formatGeneral(getExportMember(key));
    }
    
    public Map<String, Object> getExportedValues() {
        Map<String, Object> result = Maps.newLinkedHashMap();
        for(String key : attributeNames) {
            result.put(key, getExportMember(key));
        }
        return result;
    }
    
    public String[] toArray() {
        String[] result = new String[attributeNames.size()];
        this.copyToArray(result);
        return result;
    }
    
    public void copyToArray(String[] result) {
        if(result.length != attributeNames.size()) {
            throw new IllegalArgumentException("Incorrect array size");
        }
        for(int i = 0; i < attributeNames.size(); i++) {
            result[i] = getFormattedMember(attributeNames.get(i));
        }
    }

    @Override
    public Iterator<String> iterator() {
        return Arrays.asList(this.toArray()).iterator();
    }    
}

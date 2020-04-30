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
package smartfire.database;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import java.io.Serializable;
import java.util.Map.Entry;
import java.util.*;
import javax.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import smartfire.export.Exportable;
import smartfire.gis.GeometryEntity;

/**
 * Represents a raw datum read from a given Source, prior to any processing by
 * the SMARTFIRE algorithm.
 */
@Entity
@Table(name = "raw_data")
public class RawData extends AbstractMap<String, String> 
        implements SfEntity<Long>, 
                   Map<String, String>, 
                   Exportable, 
                   GeometryEntity, 
                   Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="raw_data_seq_gen")
    @SequenceGenerator(name="raw_data_seq_gen", sequenceName="raw_data_seq")
    private Long id;

    @Column(name = "shape", nullable = false)
    @Type(type = "org.hibernatespatial.GeometryUserType")
    private Geometry shape;

    @Column(name = "area", nullable = false)
    private double area;

    @Column(name = "start_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name = "end_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date endDate;

    @ManyToOne(optional=false)
    private Source source;

    @ManyToOne(optional=true)
    @JoinColumn(name="clump_id", insertable=false, updatable=false)
    private Clump clump;

    @Fetch(FetchMode.JOIN)
    @OneToMany(cascade = { CascadeType.ALL })
    @JoinColumn(name = "rawdata_id")
    @MapKey(name = "name")
    private Map<String, DataAttribute> dataAttributes;

    public RawData() {
        dataAttributes = new LinkedHashMap<String, DataAttribute>();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean containsKey(Object key) {
        if(!(key instanceof String)) {
            return false;
        }
        return dataAttributes.containsKey((String) key);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Set<Entry<String, String>> result = new LinkedHashSet<Entry<String, String>>();
        for(DataAttribute attr : dataAttributes.values()) {
            result.add(new SimpleEntry<String, String>(attr.getName(), attr.getAttrValue()));
        }
        return result;
    }

    @Override
    public String get(Object key) {
        if(!(key instanceof String)) {
            return null;
        }
        DataAttribute attribute = dataAttributes.get((String) key);
        if(attribute == null) {
            return null;
        }
        return attribute.getAttrValue();
    }

    @Override
    public String put(String key, String value) {
        DataAttribute attr = dataAttributes.get(key);
        if(attr == null) {
            attr = new DataAttribute();
            attr.setName(key);
            attr.setAttrValue(value);
            dataAttributes.put(key, attr);
            return null;
        } else {
            String oldValue = attr.getAttrValue();
            attr.setAttrValue(value);
            return oldValue;
        }
    }

    @Override
    public String remove(Object key) {
        if(!(key instanceof String)) {
            return null;
        }
        DataAttribute attr = dataAttributes.remove((String) key);
        if(attr != null) {
            return attr.getAttrValue();
        }
        return null;
    }

    @Override
    public int size() {
        return dataAttributes.size();
    }

    @Override
    public Double getExportPointY() {
        if(this.getShape().getNumPoints() != 1) {
            return this.getShape().getCentroid().getY();
        }
        return this.getShape().getCoordinate().y;
    }

    @Override
    public Double getExportPointX() {
        if(this.getShape().getNumPoints() != 1) {
            return this.getShape().getCentroid().getX();
        }
        return this.getShape().getCoordinate().x;
    }

    @Override
    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public Clump getClump() {
        return clump;
    }

    void associateWithClump(Clump clump) {
        this.clump = clump;
    }

    public Map<String, DataAttribute> getDataAttributes() {
        return dataAttributes;
    }

    public void setDataAttributes(Map<String, DataAttribute> dataAttributes) {
        this.dataAttributes = dataAttributes;
    }

    @Override
    public DateTime getEndDateTime() {
        return new DateTime(endDate, DateTimeZone.UTC);
    }

    public void setEndDate(DateTime endDate) {
        this.endDate = endDate.withZone(DateTimeZone.UTC).toDate();
    }

    @Override
    public Geometry getShape() {
        return shape;
    }

    public void setShape(Geometry shape) {
        this.shape = shape;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    @Override
    public DateTime getStartDateTime() {
        return new DateTime(startDate, DateTimeZone.UTC);
    }

    public void setStartDate(DateTime startDate) {
        this.startDate = startDate.withZone(DateTimeZone.UTC).toDate();
    }

    @Override
    public Map<String, Class<?>> getBasicMemberMap() {
        return Collections.emptyMap();
    }
    
    @Override
    public Map<String, Class<?>> getExtraExportMemberMap() {
        Map<String, Class<?>> result = Maps.newHashMap();
        for(String attributeName : this.keySet()) {
            result.put(attributeName, String.class);
        }        
        return result;
    }

    @Override
    public Object getExtraExportMember(String name) {
        return this.get(name);
    }
    
    @Override
    public String getShapeName() {
        return "";
    }
}

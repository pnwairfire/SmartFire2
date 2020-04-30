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

import com.vividsolutions.jts.geom.Geometry;
import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import smartfire.export.Exportable;
import smartfire.gis.GeometryEntity;

@Entity
@Table(name = "clump")
public class Clump implements SfEntity<Integer>, Exportable, GeometryEntity, Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="clump_seq_gen")
    @SequenceGenerator(name="clump_seq_gen", sequenceName="clump_seq")
    private Integer id;

    @Column(name = "shape", nullable = false)
    @Type(type = "org.hibernatespatial.GeometryUserType")
    private Geometry shape;

    @Column(name = "area", nullable = false)
    private double area;

    @Column(name = "start_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date startDate;

    @Column(name = "end_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date endDate;

    @ManyToOne(optional=false)
    private Source source;

    @ManyToOne(optional=true)
    @JoinColumn(name="fire_id", insertable=false, updatable=false)
    private Fire fire;

    @OneToMany(cascade=CascadeType.ALL)
    @JoinColumn(name = "clump_id")
    private List<RawData> rawData;

    public Clump() {
        this.rawData = new ArrayList<RawData>();
    }

    @Override
    public Integer getId() {
        return id;
    }
    
    @Override
    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    @Override
    public DateTime getStartDateTime() {
        return new DateTime(startDate, DateTimeZone.UTC);
    }

    public void setStartDate(DateTime startDate) {
        this.startDate = startDate.withZone(DateTimeZone.UTC).toDate();
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

    public Fire getFire() {
        return fire;
    }

    void associateWithFire(Fire fire) {
        this.fire = fire;
    }

    public List<RawData> getRawData() {
        return Collections.unmodifiableList(rawData);
    }

    public void addRawDataRecord(RawData rawDataRecord) {
        this.rawData.add(rawDataRecord);
        rawDataRecord.associateWithClump(this);
    }

    public void addRawDataRecords(Iterable<RawData> rawData) {
        for(RawData record : rawData) {
            addRawDataRecord(record);
        }
    }

    void removeRawDataRecord(RawData rawDataRecord) {
        this.rawData.remove(rawDataRecord);
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    @Override
    public Map<String, Class<?>> getBasicMemberMap() {
        return Collections.emptyMap();
    }
    
    @Override
    public Map<String, Class<?>> getExtraExportMemberMap() {
        return Collections.emptyMap();
    }

    @Override
    public Object getExtraExportMember(String name) {
        return null;
    }

    @Override
    public Double getExportPointX() {
        return this.getShape().getCentroid().getX();
    }
    
    @Override
    public Double getExportPointY() {
        return this.getShape().getCentroid().getY();
    }
    
    @Override
    public String getShapeName() {
        return "";
    }
}

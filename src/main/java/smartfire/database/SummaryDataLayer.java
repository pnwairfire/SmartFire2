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
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import javax.persistence.*;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

@Entity
@Table(name = "summary_data_layer")
public class SummaryDataLayer implements SfEntity<Integer>, Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="summary_data_layer_seq_gen")
    @SequenceGenerator(name="summary_data_layer_seq_gen", sequenceName="summary_data_layer_seq")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "name_slug", nullable = true, length = 100)
    private String nameSlug;

    @Column(name = "extent", nullable = false)
    @Type(type = "org.hibernatespatial.GeometryUserType")
    private Geometry extent;

    @Column(name = "start_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date startDate;

    @Column(name = "end_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date endDate;

    @Column(name = "data_location", nullable = false, length = 100)
    private String dataLocation;
    
    @Column(name="layer_reading_method", nullable=false, length=100)
    private String layerReadingMethod;

    @ManyToMany(mappedBy = "summaryDataLayers")
    private Set<ReconciliationStream> reconciliationStreams;

    @Override
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getDataLocation() {
        return dataLocation;
    }

    public void setDataLocation(String dataLocation) {
        this.dataLocation = dataLocation;
    }

    public Geometry getExtent() {
        return extent;
    }

    public void setExtent(Geometry extent) {
        this.extent = extent;
    }

    public String getStartDate() {
        return new DateTime(startDate, DateTimeZone.UTC).toString("yyyyMMdd");
    }

    public void setStartDate(DateTime startDate) {
        this.startDate = startDate.withZone(DateTimeZone.UTC).toDate();
    }

    public String getEndDate() {
        return new DateTime(endDate, DateTimeZone.UTC).toString("yyyyMMdd");
    }

    public void setEndDate(DateTime endDate) {
        this.endDate = endDate.withZone(DateTimeZone.UTC).toDate();
    }

    public String getLayerReadingMethod() {
        return layerReadingMethod;
    }

    public void setLayerReadingMethod(String layerReadingMethod) {
        this.layerReadingMethod = layerReadingMethod;
    }

    public String getNameSlug() {
        return nameSlug;
    }

    public void setNameSlug(String nameSlug) {
        this.nameSlug = nameSlug;
    }

    public Set<ReconciliationStream> getReconciliationStreams() {
        return Collections.unmodifiableSet(reconciliationStreams);
    }

    void associateWithStream(ReconciliationStream stream) {
        if(!this.reconciliationStreams.contains(stream)) {
            this.reconciliationStreams.add(stream);
        }
    }

    void removeStream(ReconciliationStream stream) {
        this.reconciliationStreams.remove(stream);
    }
}

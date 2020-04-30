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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.MultiPolygon;
import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import smartfire.export.Exportable;
import smartfire.gis.GeometryEntity;

@Entity
@Table(name = "event")
public class Event extends AbstractMap<String, String>
        implements SfEntity<Long>, 
                   Map<String, String>, 
                   Exportable, 
                   GeometryEntity, 
                   QueryableEntity<Long>,
                   Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="event_seq_gen")
    @SequenceGenerator(name="event_seq_gen", sequenceName="event_seq")
    private Long id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "unique_id", nullable = false, length = 100)
    private String uniqueId;

    @Column(name = "start_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date startDate;

    @Column(name = "end_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date endDate;

    @Column(name = "total_area", nullable = false)
    private double totalArea;
    
    @Column(name = "fire_type", nullable = false)
    private String fireType;

    @Column(name = "outline_shape", nullable = false)
    @Type(type = "org.hibernatespatial.GeometryUserType")
    private MultiPolygon outlineShape;

    @Column(name = "probability", nullable = false)
    private double probability;
    
    @Column(name = "create_date", nullable = false)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date createDate;

    @ManyToOne
    private ReconciliationStream reconciliationStream;

    @OneToMany(cascade = { CascadeType.ALL })
    @JoinColumn(name = "event_id")
    private Set<EventDay> eventDays;

    @ManyToMany
    @JoinTable(
        name = "event_fires",
        joinColumns = { @JoinColumn(name = "event_id") },
        inverseJoinColumns = { @JoinColumn(name = "fire_id") })
    private Set<Fire> fires;

    @LazyCollection(org.hibernate.annotations.LazyCollectionOption.EXTRA)
    @Fetch(FetchMode.JOIN)
    @OneToMany(cascade = { CascadeType.ALL })
    @JoinColumn(name = "event_id")
    @MapKey(name = "attrName")
    private Map<String, EventAttribute> eventAttributes;
    
    public Event() {
        this.uniqueId = UUID.randomUUID().toString();
        this.eventAttributes = new LinkedHashMap<String, EventAttribute>();
        this.fires = new HashSet<Fire>();
        this.eventDays = new HashSet<EventDay>();
        this.createDate = new Date();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final Event other = (Event) obj;
        if(this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        if((this.uniqueId == null) ? (other.uniqueId != null) : !this.uniqueId.equals(other.uniqueId)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 37 * hash + (this.uniqueId != null ? this.uniqueId.hashCode() : 0);
        return hash;
    }
    
    @Override
    public Long getId() {
        return id;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public DateTime getStartDateTime() {
        return new DateTime(startDate, DateTimeZone.UTC);
    }
    
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(DateTime startDate) {
        this.startDate = startDate.withZone(DateTimeZone.UTC).toDate();
    }

    @Override
    public DateTime getEndDateTime() {
        return new DateTime(endDate, DateTimeZone.UTC);
    }
    
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(DateTime endDate) {
        this.endDate = endDate.withZone(DateTimeZone.UTC).toDate();
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public ReconciliationStream getReconciliationStream() {
        return reconciliationStream;
    }

    public void setReconciliationStream(ReconciliationStream reconciliationStream) {
        this.reconciliationStream = reconciliationStream;
    }

    public String getFireType() {
        return fireType;
    }

    public void setFireType(String fireType) {
        this.fireType = fireType;
    }
    
    @Override
    public double getArea() {
        return totalArea;
    }

    public double getTotalArea() {
        return totalArea;
    }

    public void setTotalArea(double totalArea) {
        this.totalArea = totalArea;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public MultiPolygon getShape() {
        return outlineShape;
    }
    
    @Override
    public Envelope getShapeEnvelope() {
        return outlineShape.getEnvelopeInternal();
    }

    public void setShape(MultiPolygon outlineShape) {
        this.outlineShape = outlineShape;
    }

    public Set<EventDay> getEventDays() {
        return Collections.unmodifiableSet(eventDays);
    }
    
    public void setEventDays(Iterable<EventDay> eventDays) {
        this.clearEventDays();
        for(EventDay eventDay : eventDays) {
            if(eventDay.getEvent() != null) {
                throw new IllegalArgumentException("Can't re-parent an EventDay!");
            }
            this.eventDays.add(eventDay);
        }
    }

    void clearEventDays() {
        for(EventDay eventDay : this.eventDays) {
            eventDay.setEvent(null);
        }
        this.eventDays.clear();
    }

    public Set<Fire> getFires() {
        return Collections.unmodifiableSet(fires);
    }
    
    public void addFire(Fire fire) {
        this.fires.add(fire);
        fire.associateWithEvent(this);
    }
    
    public void addFires(Iterable<Fire> fires) {
        for(Fire fire : fires) {
            addFire(fire);
        }
    }
    
    void removeFire(Fire fire) {
        this.fires.remove(fire);
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Attributes map (implementation of Map interface)
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean containsKey(Object key) {
        if(!(key instanceof String)) {
            return false;
        }
        return eventAttributes.containsKey((String) key);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Set<Entry<String, String>> result = new LinkedHashSet<Entry<String, String>>();
        for(EventAttribute attr : eventAttributes.values()) {
            result.add(new SimpleEntry<String, String>(attr.getName(), attr.getAttrValue()));
        }
        return result;
    }

    @Override
    public String get(Object key) {
        if(!(key instanceof String)) {
            return null;
        }
        EventAttribute attr = eventAttributes.get((String) key);
        if(attr == null) {
            return null;
        }
        return attr.getAttrValue();
    }

    @Override
    public String put(String key, String value) {
        EventAttribute attr = eventAttributes.get(key);
        if(attr == null) {
            attr = new EventAttribute();
            attr.setName(key);
            attr.setAttrValue(value);
            eventAttributes.put(key, attr);
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
        EventAttribute attr = eventAttributes.remove((String) key);
        if(attr != null) {
            return attr.getAttrValue();
        }
        return null;
    }

    @Override
    public int size() {
        return eventAttributes.size();
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Exportable interface
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Map<String, Class<?>> getBasicMemberMap() {
        Map<String, Class<?>> result = Maps.newHashMap();
        result.put("display_name", String.class);
        result.put("unique_id", String.class);
        result.put("probability", Double.class);
        result.put("date_created", Date.class);
        result.put("fire_type", String.class);
        
        return result;
    }
    
    @Override
    public Map<String, Class<?>> getExtraExportMemberMap() {
        Map<String, Class<?>> result = getBasicMemberMap();
        
        for(String attributeName : this.keySet()) {
            result.put(attributeName, String.class);
        }
        
        return result;
    }

    @Override
    public Object getExtraExportMember(String name) {
        if("display_name".equals(name)) {
            return this.displayName;
        } else if("unique_id".equals(name)) {
            return this.uniqueId;
        } else if("probability".equals(name)) {
            return this.probability;
        } else if("date_created".equals(name)) {
            return this.createDate;
        } else if("fire_type".equals(name)) {
            return this.fireType;
        } else {
            return this.get(name);
        }
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
        return this.getDisplayName();
    }
    
    public void setWeightingSourceName(String weighting, Source source) {
        this.setWeightingSourceName(weighting, source.getName());
    }
    
    public void setWeightingSourceName(String weighting, String sourceName) {
        this.put("sf2_" + weighting + "_source", sourceName);
    }
    
    public String getWeightingSourceName(String weighting) {
        return this.get("sf2_" + weighting + "_source");
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // EventSlice
    ///////////////////////////////////////////////////////////////////////////
    
    public List<EventSlice> getSlices() {
        // FIXME - SF-292
        ReconciliationStream stream = getReconciliationStream();
        List<ReconciliationWeighting> weights = stream.getReconciliationWeightings();        
        SetMultimap<Source, Fire> lookup = HashMultimap.create();
        for(Fire fire : getFires()) {
            if(!fire.getClumps().isEmpty()) {
                lookup.put(fire.getSource(), fire);
            }
        }
        
        List<EventSlice> result = Lists.newArrayListWithExpectedSize(weights.size());
        for(ReconciliationWeighting weight : weights) {
            Source source = weight.getSource();
            Set<Fire> sourceFires = lookup.get(source);
            if(!sourceFires.isEmpty()) {
                result.add(new EventSlice(this, source, weight, sourceFires));
            }
        }
        
        return result;
    }
}

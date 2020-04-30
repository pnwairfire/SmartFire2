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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.*;
import javax.persistence.*;
import org.apache.commons.lang.WordUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.SmartfireException;
import smartfire.export.Exportable;
import smartfire.gis.GeometryEntity;
import smartfire.gis.Union;

@Entity
@Table(name = "fire")
public class Fire extends AbstractMap<String, String>
        implements SfEntity<Integer>, 
                   Map<String, String>, 
                   Exportable, 
                   GeometryEntity,
                   QueryableEntity<Integer>,
                   Serializable {
    private static final long serialVersionUID = 1L;

    public static final Comparator<Fire> BY_SIZE_DESC = new Comparator<Fire>() {
        @Override
        public int compare(Fire a, Fire b) {
            return Double.compare(
                    b.getArea(),
                    a.getArea());
        }
    };

    public static final Comparator<Fire> BY_SIZE_ASC = new Comparator<Fire>() {
        @Override
        public int compare(Fire a, Fire b) {
            return Double.compare(
                    a.getArea(),
                    b.getArea());
        }
    };
    
    public static final Comparator<Fire> BY_START_DATE_ASC = new Comparator<Fire>() {
        @Override
        public int compare(Fire a, Fire b) {
            return a.getStartDateTime().compareTo(b.getStartDateTime());
        }
    };
    
    public static final String UNKNOWN_FIRE_NAME = "Unknown Fire";
    public static final String PRESCRIBED_FIRE_NAME = "Unnamed Prescribed Fire";
    private static final Logger log = LoggerFactory.getLogger(Fire.class);

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="fire_seq_gen")
    @SequenceGenerator(name="fire_seq_gen", sequenceName="fire_seq")
    private Integer id;

    @Column(name = "unique_id", nullable = false, length = 100)
    private String uniqueId;

    @Column(name = "probability", nullable = true)
    private Double probability;

    @Column(name = "area", nullable = false)
    private double area;
    
    @Column(name = "fire_type", nullable = false)
    private String fireType;
    
    @OneToMany()
    @JoinColumn(name = "fire_id")
    private Set<Clump> clumps;

    @ManyToMany(mappedBy = "fires")
    private Set<Event> events;

    @ManyToOne(optional=false)
    private Source source;

    @LazyCollection(org.hibernate.annotations.LazyCollectionOption.EXTRA)
    @Fetch(FetchMode.JOIN)
    @OneToMany(cascade = { CascadeType.ALL })
    @JoinColumn(name = "fire_id")
    @MapKey(name = "name")
    private Map<String, FireAttribute> fireAttributes;
    
    @Transient
    private MultiPolygon shape;

    @Transient
    private Envelope envelope;

    @Transient
    private DateTime startDate;
    
    @Transient
    private DateTime endDate;

    @Transient
    private List<FireDay> fireDays;
    
    public Fire() {
        this.uniqueId = UUID.randomUUID().toString();
        this.fireAttributes = new LinkedHashMap<String, FireAttribute>();
        this.clumps = new HashSet<Clump>();
        this.events = new HashSet<Event>();
        this.shape = null;
        this.startDate = null;
        this.endDate = null;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final Fire other = (Fire) obj;
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
        int hash = 3;
        hash = 47 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 47 * hash + (this.uniqueId != null ? this.uniqueId.hashCode() : 0);
        return hash;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

    public String getFireType() {
        return fireType;
    }

    public void setFireType(String fireType) {
        this.fireType = fireType;
    }
    
    @Override
    public double getArea() {
        return this.area;
    }
    
    public void setArea(double area) {
        this.area = area;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Set<Clump> getClumps() {
        return Collections.unmodifiableSet(clumps);
    }

    public void addClump(Clump clump) {
        this.clumps.add(clump);
        clump.associateWithFire(this);
        updateCachedClumpData(clump);
    }

    public void addClumps(Iterable<Clump> clumps) {
        for(Clump clump : clumps) {
            addClump(clump);
        }
    }

    public void removeClump(Clump clump) {
        this.clumps.remove(clump);
        invalidateCachedClumpData();
    }

    public void disassociateAllClumps() {
        // Disassociate & remove clumps from the fire
        for (Clump clump : this.clumps) {
            clump.associateWithFire(null);
        }
        this.clumps.clear();
        invalidateCachedClumpData();
    }

    public Set<Event> getEvents() {
        return Collections.unmodifiableSet(events);
    }
    
    void associateWithEvent(Event event) {
        if(!this.events.contains(event)) {
            this.events.add(event);
        }
    }
    
    void removeEvent(Event event) {
        this.events.remove(event);
    }
    
    public String getDisplayName() {
        String fireNameField = this.getSource().getFireNameField();
        
        String[] names = fireNameField.split(";");
        String fullName = "";
        
        for(String rawName : names) {
            String retrievedName = this.get(rawName);
            if(retrievedName != null && !retrievedName.isEmpty()) {
                if(!fullName.isEmpty()) {
                    fullName += " - ";
                }
                fullName += retrievedName;
            }
        }
        
        if(fullName.isEmpty()) {
            if(this.get("Planned Initiation Date") != null) {
                return PRESCRIBED_FIRE_NAME;
            }
            return UNKNOWN_FIRE_NAME;
        }
        
        String lowerName = fullName.toLowerCase();
        if(!(lowerName.contains("fire") || lowerName.contains("complex"))) {
            fullName = fullName + " Fire";
        }
        return WordUtils.capitalizeFully(fullName);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Attributes map (implementation of Map interface)
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean containsKey(Object key) {
        if(!(key instanceof String)) {
            return false;
        }
        return fireAttributes.containsKey((String) key);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Set<Entry<String, String>> result = new LinkedHashSet<Entry<String, String>>();
        for(FireAttribute attr : fireAttributes.values()) {
            result.add(new SimpleEntry<String, String>(attr.getName(), attr.getAttrValue()));
        }
        return result;
    }

    @Override
    public String get(Object key) {
        if(!(key instanceof String)) {
            return null;
        }
        FireAttribute attr = fireAttributes.get((String) key);
        if(attr == null) {
            return null;
        }
        return attr.getAttrValue();
    }

    @Override
    public String put(String key, String value) {
        FireAttribute attr = fireAttributes.get(key);
        if(attr == null) {
            attr = new FireAttribute();
            attr.setName(key);
            attr.setAttrValue(value);
            fireAttributes.put(key, attr);
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
        FireAttribute attr = fireAttributes.remove((String) key);
        if(attr != null) {
            return attr.getAttrValue();
        }
        return null;
    }
    
    @Override
    public void clear() {
        fireAttributes.clear();
    }

    @Override
    public int size() {
        return fireAttributes.size();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Data summarized from Clumps
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public MultiPolygon getShape() {
        // Union clumps if shape is null.
        if(this.shape == null) {
            Geometry geom = Union.unionAllShapes(clumps);
            try {
                this.shape = Union.toMultiPolygon(geom);
            } catch (ClassCastException e) {
                log.error("{} geometry type returned from clump union; Expected Polygon or MultiPolygon.", geom.getGeometryType());
                throw new SmartfireException("Non-Polygon geometry type returned from clump union.", e);
            }
            this.envelope = this.shape.getEnvelopeInternal();
        }
        return this.shape;
    }

    @Override
    public Envelope getShapeEnvelope() {
        if(this.envelope == null) {
            getShape();
        }
        return this.envelope;
    }

    private void updateCachedClumpData(Clump newClump) {
        if(this.shape != null) {
            this.shape = Union.toMultiPolygon(Union.union(this.shape, newClump.getShape()));
            this.envelope = this.shape.getEnvelopeInternal();
        }
        if(this.startDate != null && this.startDate.isAfter(newClump.getStartDateTime())) {
            this.startDate = newClump.getStartDateTime();
        }
        if(this.endDate != null && this.endDate.isBefore(newClump.getEndDateTime())) {
            this.endDate = newClump.getEndDateTime();
        }
    }

    private void invalidateCachedClumpData() {
        this.shape = null;
        this.envelope = null;
        this.startDate = null;
        this.endDate = null;
        this.fireDays = null;
    }

    @Override
    public DateTime getStartDateTime() {
        if(this.startDate == null) {
            computeClumpSummaryProperties();
        }
        return this.startDate;
    }

    @Override
    public DateTime getEndDateTime() {
        if(this.endDate == null) {
            computeClumpSummaryProperties();
        }
        return this.endDate;
    }

    private void computeClumpSummaryProperties() {
        DateTime min = null;
        DateTime max = null;
        for(Clump clump : this.clumps) {
            if(min == null || min.isAfter(clump.getStartDateTime())) {
                min = clump.getStartDateTime();
            }
            if(max == null || max.isBefore(clump.getEndDateTime())) {
                max = clump.getEndDateTime();
            }
        }
        this.startDate = min;
        this.endDate = max;
    }
    
    public List<FireDay> getFireDays() {
        if(this.fireDays == null) {
            if(this.startDate == null) {
                computeClumpSummaryProperties();
            }

            SortedMap<LocalDate, List<Clump>> intervalClumps = new TreeMap<LocalDate, List<Clump>>();

            for(Clump clump : clumps) {
                LocalDate dt = clump.getStartDateTime().toLocalDate();
                LocalDate end = clump.getEndDateTime().toLocalDate();
                while(!dt.isAfter(end)) {                
                    List<Clump> clumpList = intervalClumps.get(dt);
                    if(clumpList == null) {
                        clumpList = Lists.newArrayList();
                        intervalClumps.put(dt, clumpList);
                    }
                    clumpList.add(clump);
                    dt = dt.plusDays(1);
                }
            }

            List<FireDay> result = Lists.newArrayList();

            for(Map.Entry<LocalDate, List<Clump>> entry : intervalClumps.entrySet()) {
                LocalDate date = entry.getKey();
                List<Clump> fireDayClumps = entry.getValue();

                Geometry fireDayShape = Union.unionAllShapes(fireDayClumps);
                double fireDayArea = fireDayShape.getArea();

                result.add(new FireDay(this, date, fireDayArea, fireDayShape, fireDayClumps.size()));
            }

            this.fireDays = Collections.unmodifiableList(result);
        }
        return this.fireDays;
    }
    
    @Override
    public String getShapeName() {
        return this.getUniqueId();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Exportable interface
    ///////////////////////////////////////////////////////////////////////////
    
    @Override
    public Map<String, Class<?>> getBasicMemberMap() {
        Map<String, Class<?>> result = Maps.newHashMap();
        result.put("display_name", String.class);
        result.put("probability", Double.class);
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
        if(name == null) return null;
        if(name.equals("display_name")) {
            return this.getDisplayName();
        } else if(name.equals("probability")) {
            return this.probability;
        } else if(name.equals("unique_id")) {
            return this.uniqueId;
        } else if(name.equals("fire_type")) {
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
}

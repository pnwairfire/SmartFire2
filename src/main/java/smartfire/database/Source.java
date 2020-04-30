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

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.io.Serializable;
import java.util.Map.Entry;
import java.util.*;
import javax.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import smartfire.Granularity;

/**
 * Represents a Source of data that is read into the SMARTFIRE database.
 */
@Entity
@Table(name = "source")
public class Source extends AbstractMap<String, String>
        implements SfEntity<Integer>, Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum DataPolicy {REPLACE, APPEND, IRWIN_REPLACE};

    @XStreamOmitField
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="source_seq_gen")
    @SequenceGenerator(name="source_seq_gen", sequenceName="source_seq")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @XStreamOmitField
    @Column(name = "name_slug", nullable = true, length = 100)
    private String nameSlug;

    @Enumerated(EnumType.STRING)
    @Column(name = "geometry_type", nullable = false, length = 100)
    private GeometryType geometryType;

    @Column(name = "ingest_method", nullable = true, length = 100)
    private String ingestMethod;
    
    @Column(name = "clump_method", nullable = false, length = 100)
    private String clumpMethod;

    @Column(name = "assoc_method", nullable = false, length = 100)
    private String assocMethod;

    @Column(name = "probability_method", nullable = false, length = 100)
    private String probabilityMethod;
    
    @Column(name = "fire_type_method", nullable = false, length = 100)
    private String fireTypeMethod;

    @Column(name = "granularity", nullable = false, length = 100)
    private String granularity;

    @Column(name = "fire_name_field", nullable = true, length = 100)
    private String fireNameField;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "new_data_policy", nullable = false, length = 100)
    private DataPolicy newDataPolicy;
    
    @XStreamOmitField
    @Column(name = "latest_data", nullable = true)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date latestData;

    @XStreamOmitField
    @OneToMany(cascade = { CascadeType.ALL })
    @JoinColumn(name = "source_id")
    private List<ReconciliationWeighting> reconciliationWeighting;

    @XStreamOmitField
    @OneToMany(cascade = { CascadeType.ALL })
    @JoinColumn(name="source_id")
    private List<ScheduledFetch> scheduledFetch;

	@OneToOne(fetch = FetchType.LAZY, mappedBy = "source", cascade = {CascadeType.ALL})
    private DefaultWeighting defaultWeighting;

    @Fetch(FetchMode.JOIN)
    @OneToMany(cascade = { CascadeType.ALL })
    @JoinColumn(name = "source_id")
    @MapKey(name = "name")
    private Map<String, SourceAttribute> sourceAttributes;
    
    public static final String UNKNOWN_SOURCE_NAME = "N/A";

    public Source() {
        sourceAttributes = new LinkedHashMap<String, SourceAttribute>();
    }

    public Source(String name) {
        this.name = name;
        this.sourceAttributes = new LinkedHashMap<String, SourceAttribute>();
        this.assocMethod = "";
        this.clumpMethod = "";
        this.probabilityMethod = "";
        this.geometryType = GeometryType.POINT;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final Source other = (Source) obj;
        if(this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }
    
    @Override
    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameSlug() {
        return this.nameSlug;
    }

    public void setNameSlug(String nameSlug) {
        this.nameSlug = nameSlug;
    }

    public GeometryType getGeometryType() {
        return this.geometryType;
    }

    public void setGeometryType(String geometryType) {
        this.geometryType = GeometryType.valueOf(geometryType);
    }

    public String getClumpMethod() {
        return this.clumpMethod;
    }

    public void setClumpMethod(String clumpMethod) {
        this.clumpMethod = clumpMethod;
    }

    public DateTime getLatestData() {
        return new DateTime(latestData).withZone(DateTimeZone.UTC);
    }

    public void setLatestData(DateTime date) {
        Date newDate = date.withZone(DateTimeZone.UTC).toDate();
        if(latestData == null) {
            this.latestData = newDate;
        } else if(latestData.before(newDate)) {
            this.latestData = newDate;
        }
    }
    
    public String getAssocMethod() {
        return this.assocMethod;
    }

    public void setAssocMethod(String assocMethod) {
        this.assocMethod = assocMethod;
    }

    public String getProbabilityMethod() {
        return this.probabilityMethod;
    }

    public void setProbabilityMethod(String probabilityMethod) {
        this.probabilityMethod = probabilityMethod;
    }

    public String getFireTypeMethod() {
        return fireTypeMethod;
    }

    public void setFireTypeMethod(String fireTypeMethod) {
        this.fireTypeMethod = fireTypeMethod;
    }

    public List<ScheduledFetch> getScheduledFetches() {
        return this.scheduledFetch;
    }

    public Map<String, SourceAttribute> getSourceAttributes() {
        return sourceAttributes;
    }

    public void setSourceAttributes(Map<String, SourceAttribute> sourceAttributes) {
        this.sourceAttributes = sourceAttributes;
    }

    public DataPolicy getNewDataPolicy() {
        return newDataPolicy;
    }

    public void setNewDataPolicy(String newDataPolicy) {
        this.newDataPolicy = DataPolicy.valueOf(newDataPolicy);
    }

    public Granularity getGranularity() {
        return Granularity.valueOf(granularity);
    }

    public void setGranularity(Granularity granularity) {
        this.granularity = granularity.name();
    }

    public Period getGranularityPeriod() {
        PeriodFormatter periodFormat = new PeriodFormatterBuilder()
                .appendYears()
                .appendSuffix(" Year", " Years")
                .appendMonths()
                .appendSuffix(" Month" , " Months")
                .appendWeeks()
                .appendSuffix(" Week", " Weeks")
                .appendDays()
                .appendSuffix(" Day", " Days")
                .toFormatter();
        return periodFormat.parsePeriod(getGranularity().toString());
    }

    public DefaultWeighting getDefaultWeighting() {
        return defaultWeighting;
    }

    public void setDefaultWeighting(DefaultWeighting defaultWeighting) {
        this.defaultWeighting = defaultWeighting;
    }

    public String getFireNameField() {
        return fireNameField;
    }

    public void setFireNameField(String fireNameField) {
        this.fireNameField = fireNameField;
    }
    
    public String getIngestMethod() {
        return this.ingestMethod;
    }
    
    public void setIngestMethod(String ingestMethod) {
        this.ingestMethod = ingestMethod;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Attributes map (implementation of Map interface)
    ///////////////////////////////////////////////////////////////////////////
    
    @Override
    public boolean containsKey(Object key) {
        if(!(key instanceof String)) {
            return false;
        }
        return sourceAttributes.containsKey((String) key);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Set<Entry<String, String>> result = new LinkedHashSet<Entry<String, String>>();
        for(SourceAttribute attr : sourceAttributes.values()) {
            result.add(new SimpleEntry<String, String>(attr.getName(), attr.getAttrValue()));
        }
        return result;
    }

    @Override
    public String get(Object key) {
        if(!(key instanceof String)) {
            return null;
        }
        SourceAttribute attribute = sourceAttributes.get((String) key);
        if(attribute == null) {
            return null;
        }
        return attribute.getAttrValue();
    }

    @Override
    public String put(String key, String value) {
        SourceAttribute attr = sourceAttributes.get(key);
        if(attr == null) {
            attr = new SourceAttribute();
            attr.setName(key);
            attr.setAttrValue(value);
            sourceAttributes.put(key, attr);
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
        SourceAttribute attr = sourceAttributes.remove((String) key);
        if(attr != null) {
            return attr.getAttrValue();
        }
        return null;
    }

    @Override
    public int size() {
        return sourceAttributes.size();
    }
}

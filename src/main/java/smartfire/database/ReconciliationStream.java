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
import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "reconciliation_stream")
public class ReconciliationStream extends AbstractMap<String, String>
        implements SfEntity<Integer>, 
                   Map<String, String>,
                   Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="reconciliation_stream_seq_gen")
    @SequenceGenerator(name="reconciliation_stream_seq_gen", sequenceName="reconciliation_stream_seq")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "name_slug", nullable = true, length = 100)
    private String nameSlug;
    
    @Column(name = "auto_reconcile", nullable = false)
    private Boolean autoReconcile;

    @Column(name = "reconciliation_method", nullable = false, length = 100)
    private String reconciliationMethod;
    
    @Column(name = "schedule", nullable = true, length = 100)
    private String schedule;

    @OneToMany(cascade = { CascadeType.ALL })
    @JoinColumn(name = "reconciliationstream_id")
    private List<ReconciliationWeighting> reconciliationWeighting;

    @Fetch(FetchMode.JOIN)
    @OneToMany(cascade = { CascadeType.ALL })
    @JoinColumn(name = "reconciliation_stream_id")
    @MapKey(name = "attrName")
    private Map<String, StreamAttribute> streamAttributes;

    @ManyToMany
    @JoinTable(
        name = "reconciliation_stream_summary_data_layers",
        joinColumns = { @JoinColumn(name = "reconciliation_stream_id") },
        inverseJoinColumns = { @JoinColumn(name = "summary_data_layer_id") })
    private Set<SummaryDataLayer> summaryDataLayers;
    
    public ReconciliationStream() {
        this.streamAttributes = new LinkedHashMap<String, StreamAttribute>();
        this.reconciliationWeighting = new ArrayList<ReconciliationWeighting>();
        this.summaryDataLayers = new HashSet<SummaryDataLayer>();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final ReconciliationStream other = (ReconciliationStream) obj;
        if(this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }
    
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

    public String getNameSlug() {
        return nameSlug;
    }

    public void setNameSlug(String nameSlug) {
        this.nameSlug = nameSlug;
    }

    public String getReconciliationMethod() {
        return reconciliationMethod;
    }

    public void setReconciliationMethod(String reconciliationMethod) {
        this.reconciliationMethod = reconciliationMethod;
    }
    
    public String getSchedule() {
        return this.schedule;
    }
    
    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }
    
    public boolean getIsScheduled() {
        if(this.schedule == null) {
            return false;
        }
        return true;
    }
    
    public List<Source> getSources() {
        List<Source> result = Lists.newArrayList();
        for(ReconciliationWeighting weight : reconciliationWeighting) {
            result.add(weight.getSource());
        }
        return result;
    }

    public List<ReconciliationWeighting> getReconciliationWeightings() {
        return reconciliationWeighting;
    }
    
    public void setReconciliationWeightings(List<ReconciliationWeighting> weightings) {
        reconciliationWeighting = weightings;
    }
    
    public ReconciliationWeighting getWeightingForSource(Source source) {
        for(ReconciliationWeighting weighting : reconciliationWeighting) {
            if(weighting.getSource().equals(source)) {
                return weighting;
            }
        }
        return null;
    }

    public Set<SummaryDataLayer> getSummaryDataLayers() {
        return Collections.unmodifiableSet(summaryDataLayers);
    }

    public void addSummaryDataLayer(SummaryDataLayer layer) {
        this.summaryDataLayers.add(layer);
        layer.associateWithStream(this);
    }

    public void setSummaryDataLayers(Iterable<SummaryDataLayer> newLayers) {
        this.summaryDataLayers = new HashSet<SummaryDataLayer>();
        for(SummaryDataLayer layer : newLayers) {
            addSummaryDataLayer(layer);
        }
    }

    void removeSummaryDataLayer(SummaryDataLayer layer) {
        this.summaryDataLayers.remove(layer);
    }

    public Boolean autoReconcile() {
        return autoReconcile;
    }

    public void setAutoReconcile(Boolean autoReconcile) {
        this.autoReconcile = autoReconcile;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Attributes map (implementation of Map interface)
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean containsKey(Object key) {
        if(!(key instanceof String)) {
            return false;
        }
        return streamAttributes.containsKey((String) key);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Set<Entry<String, String>> result = new LinkedHashSet<Entry<String, String>>();
        for(StreamAttribute attr : streamAttributes.values()) {
            result.add(new SimpleEntry<String, String>(attr.getName(), attr.getAttrValue()));
        }
        return result;
    }

    @Override
    public String get(Object key) {
        if(!(key instanceof String)) {
            return null;
        }
        StreamAttribute attr = streamAttributes.get((String) key);
        if(attr == null) {
            return null;
        }
        return attr.getAttrValue();
    }

    @Override
    public String put(String key, String value) {
        StreamAttribute attr = streamAttributes.get(key);
        if(attr == null) {
            attr = new StreamAttribute();
            attr.setName(key);
            attr.setAttrValue(value);
            streamAttributes.put(key, attr);
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
        StreamAttribute attr = streamAttributes.remove((String) key);
        if(attr != null) {
            return attr.getAttrValue();
        }
        return null;
    }

    @Override
    public int size() {
        return streamAttributes.size();
    }
    
    public Map<String, StreamAttribute> getStreamAttributes() {
        return streamAttributes;
    }

    public void setStreamAttributes(Map<String, StreamAttribute> streamAttributes) {
        this.streamAttributes = streamAttributes;
    }
}

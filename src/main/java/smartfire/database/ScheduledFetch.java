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

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

@Entity
@Table(name = "scheduled_fetch")
public class ScheduledFetch extends AbstractMap<String, String>
        implements SfEntity<Integer>, Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="scheduled_fetch_seq_gen")
    @SequenceGenerator(name="scheduled_fetch_seq_gen", sequenceName="scheduled_fetch_seq")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "schedule", nullable = true, length = 100)
    private String schedule;

    @Column(name = "fetch_method", nullable = false, length = 100)
    private String fetchMethod;

    @Column(name = "last_fetch", nullable = true)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date lastFetch;
    
    @Column(name = "date_offset", nullable = false)
    private Integer dateOffset;

    @ManyToOne(cascade = { CascadeType.PERSIST })
    @JoinColumn(name="source_id", insertable=true, updatable=true)
    private Source source;

    @Fetch(FetchMode.JOIN)
    @OneToMany(cascade = { CascadeType.ALL })
    @JoinColumn(name = "fetch_id")
    @MapKey(name = "name")
    private Map<String, FetchAttribute> fetchAttributes;

    public ScheduledFetch() {
        fetchAttributes = new LinkedHashMap<String, FetchAttribute>();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchedule() {
        return this.schedule;
    }

    public String getFetchType() {
        if(this.schedule == null) {
            return "Manual Fetch";
        } else {
            return "Scheduled Fetch";
        }
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public boolean getIsManual() {
        if(this.schedule == null) {
            return true;
        } else {
            return false;
        }
    }

    public String getFetchMethod() {
        return this.fetchMethod;
    }

    public void setFetchMethod(String fetchMethod) {
        this.fetchMethod = fetchMethod;
    }

    public DateTime getLastFetch() {
        return new DateTime(lastFetch).withZone(DateTimeZone.UTC);
    }

    public void setLastFetch() {
        this.lastFetch = new DateTime(DateTimeZone.UTC).toDate();
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Map<String, FetchAttribute> getFetchAttributes() {
        return fetchAttributes;
    }

    public void setFetchAttributes(Map<String, FetchAttribute> fetchAttributes) {
        this.fetchAttributes = fetchAttributes;
    }

    public Integer getDateOffset() {
        return dateOffset;
    }

    public void setDateOffset(Integer dateOffset) {
        this.dateOffset = dateOffset;
    }
    

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public boolean containsKey(Object key) {
        if(!(key instanceof String)) {
            return false;
        }
        return fetchAttributes.containsKey((String) key);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Set<Entry<String, String>> result = new LinkedHashSet<Entry<String, String>>();
        for(FetchAttribute attr : fetchAttributes.values()) {
            result.add(new SimpleEntry<String, String>(attr.getName(), attr.getAttrValue()));
        }
        return result;
    }

    @Override
    public String get(Object key) {
        if(!(key instanceof String)) {
            return null;
        }
        FetchAttribute attribute = fetchAttributes.get((String) key);
        if(attribute == null) {
            return null;
        }
        return attribute.getAttrValue();
    }

    @Override
    public String put(String key, String value) {
        FetchAttribute attr = fetchAttributes.get(key);
        if(attr == null) {
            attr = new FetchAttribute();
            attr.setName(key);
            attr.setAttrValue(value);
            fetchAttributes.put(key, attr);
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
        FetchAttribute attr = fetchAttributes.remove((String) key);
        if(attr != null) {
            return attr.getAttrValue();
        }
        return null;
    }

    @Override
    public int size() {
        return fetchAttributes.size();
    }
}

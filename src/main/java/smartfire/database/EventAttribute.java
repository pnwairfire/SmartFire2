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
import javax.persistence.*;

@Entity
@Table(name = "event_attribute")
public class EventAttribute implements SfEntity<Long>, Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="event_attribute_seq_gen")
    @SequenceGenerator(name="event_attribute_seq_gen", sequenceName="event_attribute_seq")
    private Long id;

    @Column(name = "attr_name", nullable = false, length = 100)
    private String attrName;

    @Column(name = "attr_value", nullable = false)
    private String attrValue;

    @ManyToOne(cascade={CascadeType.ALL})
    @JoinColumn(name="event_id", insertable=false, updatable=false)
    private Event event;

    @Override
    public Long getId() {
        return id;
    }

    Event getEvent() {
        return event;
    }

    void setEvent(Event event) {
        this.event = event;
    }

    public String getName() {
        return attrName;
    }

    public void setName(String name) {
        this.attrName = name;
    }
    
    public String getAttrValue() {
        return attrValue;
    }

    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }
}

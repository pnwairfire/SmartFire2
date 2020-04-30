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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.io.Serializable;
import javax.persistence.*;

@Entity
@XStreamAlias("sourceAttribute")
@Table(name = "source_attribute")
public class SourceAttribute implements SfEntity<Integer>, Serializable {
    private static final long serialVersionUID = 1L;

    @XStreamOmitField
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="source_attribute_seq_gen")
    @SequenceGenerator(name="source_attribute_seq_gen", sequenceName="source_attribute_seq")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "attr_value", nullable = false, length = 100)
    private String attrValue;

    @XStreamOmitField
    @ManyToOne(cascade={CascadeType.ALL})
    @JoinColumn(name="source_id", insertable=false, updatable=false)
    Source source;

    @Override
    public Integer getId() {
        return id;
    }

    public String getAttrValue() {
        return attrValue;
    }

    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

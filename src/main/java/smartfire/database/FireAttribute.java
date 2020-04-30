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
@Table(name = "fire_attribute")
public class FireAttribute implements SfEntity<Integer>, Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="fire_attribute_seq_gen")
    @SequenceGenerator(name="fire_attribute_seq_gen", sequenceName="fire_attribute_seq")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "attr_value", nullable = false)
    private String attrValue;

    @ManyToOne(cascade={CascadeType.ALL})
    Fire fire;

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

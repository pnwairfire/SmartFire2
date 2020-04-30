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
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@XStreamAlias("defaultWeighting")
@Table(name = "default_weighting")
public class DefaultWeighting implements SfEntity<Integer>, Serializable, Weighting {
    private static final long serialVersionUID = 1L;

    @XStreamOmitField
    @GenericGenerator(  name = "generator",
                        strategy = "foreign",
                        parameters = @Parameter(name = "property", value = "source"))
	@Id
	@GeneratedValue(generator = "generator")
	@Column(name = "id", unique = true, nullable = false)
    private Integer id;

    @Column(name = DETECTION_RATE, nullable = false)
    private double detectionRate;

    @Column(name = FALSE_ALARM_RATE, nullable = false)
    private double falseAlarmRate;

    @Column(name = LOCATION_WEIGHT, nullable = false)
    private double locationWeight;

    @Column(name = SIZE_WEIGHT, nullable = false)
    private double sizeWeight;

    @Column(name = SHAPE_WEIGHT, nullable = false)
    private double shapeWeight;

    @Column(name = GROWTH_WEIGHT, nullable = false)
    private double growthWeight;
    
    @Column(name = LOCATION_UNCERTAINTY, nullable = false)
    private double locationUncertainty;
    
    @Column(name = START_DATE_UNCERTAINTY, nullable = false)
    private int startDateUncertainty;
    
    @Column(name = END_DATE_UNCERTAINTY, nullable = false)
    private int endDateUncertainty;
    
    @Column(name = NAME_WEIGHT, nullable = false)
    private double nameWeight;
    
    @Column(name = TYPE_WEIGHT, nullable = false)
    private double typeWeight;

    @XStreamOmitField
	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
    private Source source;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }
    
    @Override
    public double getDetectionRate() {
        return detectionRate;
    }

    @Override
    public void setDetectionRate(double detectionRate) {
        this.detectionRate = detectionRate;
    }

    @Override
    public double getFalseAlarmRate() {
        return falseAlarmRate;
    }

    @Override
    public void setFalseAlarmRate(double falseAlarmRate) {
        this.falseAlarmRate = falseAlarmRate;
    }

    @Override
    public double getGrowthWeight() {
        return growthWeight;
    }

    @Override
    public void setGrowthWeight(double growthWeight) {
        this.growthWeight = growthWeight;
    }
    
    @Override
    public double getLocationWeight() {
        return locationWeight;
    }

    @Override
    public void setLocationWeight(double locationWeight) {
        this.locationWeight = locationWeight;
    }

    @Override
    public double getShapeWeight() {
        return shapeWeight;
    }

    @Override
    public void setShapeWeight(double shapeWeight) {
        this.shapeWeight = shapeWeight;
    }

    @Override
    public double getSizeWeight() {
        return sizeWeight;
    }

    @Override
    public void setSizeWeight(double sizeWeight) {
        this.sizeWeight = sizeWeight;
    }

    @Override
    public int getEndDateUncertainty() {
        return endDateUncertainty;
    }

    @Override
    public void setEndDateUncertainty(int endDateUncertainty) {
        this.endDateUncertainty = endDateUncertainty;
    }

    @Override
    public double getLocationUncertainty() {
        return locationUncertainty;
    }

    @Override
    public void setLocationUncertainty(double locationUncertainty) {
        this.locationUncertainty = locationUncertainty;
    }

    @Override
    public double getNameWeight() {
        return nameWeight;
    }

    @Override
    public void setNameWeight(double nameWeight) {
        this.nameWeight = nameWeight;
    }

    public double getTypeWeight() {
        return typeWeight;
    }

    public void setTypeWeight(double typeWeight) {
        this.typeWeight = typeWeight;
    }

    @Override
    public int getStartDateUncertainty() {
        return startDateUncertainty;
    }

    @Override
    public void setStartDateUncertainty(int startDateUncertainty) {
        this.startDateUncertainty = startDateUncertainty;
    }
    
    
}

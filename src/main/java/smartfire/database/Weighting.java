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

/**
 * Interface common to both DefaultWeighting and ReconciliationWeighting
 */
public interface Weighting {
    public final static String DETECTION_RATE = "detection_rate";
    public final static String FALSE_ALARM_RATE = "false_alarm_rate";
    public final static String GROWTH_WEIGHT = "growth_weight";
    public final static String LOCATION_WEIGHT = "location_weight";
    public final static String SHAPE_WEIGHT = "shape_weight";
    public final static String SIZE_WEIGHT = "size_weight";
    public final static String LOCATION_UNCERTAINTY = "location_uncertainty";
    public final static String START_DATE_UNCERTAINTY = "start_date_uncertainty";
    public final static String END_DATE_UNCERTAINTY = "end_date_uncertainty";
    public final static String NAME_WEIGHT = "name_weight";
    public final static String TYPE_WEIGHT = "type_weight";

    double getDetectionRate();

    double getFalseAlarmRate();

    double getGrowthWeight();

    double getLocationWeight();

    double getShapeWeight();

    double getSizeWeight();

    double getLocationUncertainty();

    int getStartDateUncertainty();

    int getEndDateUncertainty();

    double getNameWeight();

    Source getSource();

    void setDetectionRate(double detectionRate);

    void setFalseAlarmRate(double falseAlarmRate);

    void setGrowthWeight(double growthWeight);

    void setLocationWeight(double locationWeight);

    void setShapeWeight(double shapeWeight);

    void setSizeWeight(double sizeWeight);

    void setLocationUncertainty(double locationUncertainty);

    void setStartDateUncertainty(int startDateUncertainty);

    void setEndDateUncertainty(int endDateUncertainty);

    void setNameWeight(double nameWeight);
}

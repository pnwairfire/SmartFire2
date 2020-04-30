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

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import smartfire.util.Sorted;

/**
 * Represents a "slice" of an Event, as perceived by a given Source.  Since
 * the Event is associated with a particular ReconciliationStream, and an
 * EventSlice is associated with an Event and a Source, we can also access
 * the applicable ReconciliationWeightings here.
 */
public class EventSlice {
    private final Event event;
    private final Source source;
    private final ReconciliationWeighting weights;
    private final List<Fire> fires;

    /**
     * A Comparator for sorting EventSlices by the detectionRate of their
     * ReconciliationWeightings in descending order (that is, the slice
     * with the highest detectionRate sorts first).
     */
    public static final Comparator<EventSlice> BY_DETECTION_RATE_DESC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            return Double.compare(
                    b.getWeights().getDetectionRate(), 
                    a.getWeights().getDetectionRate());
        }
    };
    
    /**
     * A Comparator for sorting EventSlices by the falseAlarmWeight of their
     * ReconciliationWeightings in descending order (that is, the slice
     * with the highest falseAlarmWeight sorts first).
     */
    public static final Comparator<EventSlice> BY_FALSE_ALARM_RATE_DESC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            return Double.compare(
                    b.getWeights().getFalseAlarmRate(), 
                    a.getWeights().getFalseAlarmRate());
        }
    };
    
    /**
     * A Comparator for sorting EventSlices by the growthWeight of their
     * ReconciliationWeightings in descending order (that is, the slice
     * with the highest growthWeight sorts first).
     */
    public static final Comparator<EventSlice> BY_GROWTH_WEIGHT_DESC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            return Double.compare(
                    b.getWeights().getGrowthWeight(), 
                    a.getWeights().getGrowthWeight());
        }
    };
    
    /**
     * A Comparator for sorting EventSlices by the locationWeight of their
     * ReconciliationWeightings in descending order (that is, the slice
     * with the highest locationWeight sorts first).
     */
    public static final Comparator<EventSlice> BY_LOCATION_WEIGHT_DESC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            return Double.compare(
                    b.getWeights().getLocationWeight(), 
                    a.getWeights().getLocationWeight());
        }
    };
    
    /**
     * A Comparator for sorting EventSlices by the shapeWeight of their
     * ReconciliationWeightings in descending order (that is, the slice
     * with the highest shapeWeight sorts first).
     */
    public static final Comparator<EventSlice> BY_SHAPE_WEIGHT_DESC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            return Double.compare(
                    b.getWeights().getShapeWeight(), 
                    a.getWeights().getShapeWeight());
        }
    };
    
    /**
     * A Comparator for sorting EventSlices by the typeWeight of their
     * ReconciliationWeightings in descending order (that is, the slice
     * with the highest typeWeight sorts first).
     */
    public static final Comparator<EventSlice> BY_TYPE_WEIGHT_DESC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            return Double.compare(
                    b.getWeights().getTypeWeight(), 
                    a.getWeights().getTypeWeight());
        }
    };
    
    /**
     * A Comparator for sorting EventSlices by the sizeWeight of their
     * ReconciliationWeightings in descending order (that is, the slice
     * with the highest sizeWeight sorts first).
     */
    public static final Comparator<EventSlice> BY_SIZE_WEIGHT_DESC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            return Double.compare(
                    b.getWeights().getSizeWeight(), 
                    a.getWeights().getSizeWeight());
        }
    };
    
    /**
     * A Comparator for sorting EventSlices by the startDateUncertainty of their
     * ReconciliationWeightings in ascending order (that is, the slice
     * with the lowest startDateUncertainty sorts first).
     */
    public static final Comparator<EventSlice> BY_START_DATE_ASC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            int aUncert = a.getWeights().getStartDateUncertainty();
            int bUncert = b.getWeights().getStartDateUncertainty();
            return Integer.signum(aUncert - bUncert);
        }
    };
    
    /**
     * A Comparator for sorting EventSlices by the endDateUncertainty of their
     * ReconciliationWeightings in ascending order (that is, the slice
     * with the lowest endDateUncertainty sorts first).
     */
    public static final Comparator<EventSlice> BY_END_DATE_ASC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            int aUncert = a.getWeights().getEndDateUncertainty();
            int bUncert = b.getWeights().getEndDateUncertainty();
            return Integer.signum(aUncert - bUncert);
        }
    };
    
    /**
     * A Comparator for sorting EventSlices by the nameWeight of their
     * ReconciliationWeightings in descending order (that is, the slice
     * with the highest nameWeight sorts first).
     */
    public static final Comparator<EventSlice> BY_NAME_WEIGHT_DESC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            return Double.compare(
                    b.getWeights().getNameWeight(), 
                    a.getWeights().getNameWeight());
        }
    };
    
    /**
     * A Comparator for sorting EventSlices by the nameWeight of their
     * ReconciliationWeightings in ascending order (that is, the slice
     * with the lowest nameWeight sorts first).
     */
    public static final Comparator<EventSlice> BY_NAME_WEIGHT_ASC = new Comparator<EventSlice>() {
        @Override
        public int compare(EventSlice a, EventSlice b) {
            return Double.compare(
                    a.getWeights().getNameWeight(), 
                    b.getWeights().getNameWeight());
        }
    };
    
    /**
     * Constructs a new EventSlice.
     * 
     * @param event the associated Event
     * @param source the associated Source
     * @param weights the associated ReconciliationWeighting
     * @param fires the associated Fires
     */
    EventSlice(Event event, Source source, ReconciliationWeighting weights, Set<Fire> fires) {
        this.event = event;
        this.source = source;
        this.weights = weights;
        this.fires = Sorted.by(fires, Fire.BY_SIZE_DESC);
    }

    /**
     * Gets the Event this slice is associated with.
     * 
     * @return the associated Event
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Gets the Fires that are associated with this EventSlice.
     * 
     * @return a list of Fires, sorted by size in descending order
     */
    public List<Fire> getFires() {
        return fires;
    }

    /**
     * Gets the Source that this slice is associated with.
     * 
     * @return the associated Source
     */
    public Source getSource() {
        return source;
    }

    /**
     * Gets the ReconciliationWeightings that apply to this slice.
     * 
     * @return the associated ReconciliationWeighting object
     */
    public ReconciliationWeighting getWeights() {
        return weights;
    }
}

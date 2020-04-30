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
package smartfire.func.reconcile;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.database.*;
import smartfire.func.Attribute;
import smartfire.func.ReconciliationMethod;
import smartfire.gis.GeometryBuilder;
import smartfire.gis.QueryableEventSet;
import smartfire.gis.Union;
import smartfire.layer.LayerAttributes;
import smartfire.layer.Layers;
import smartfire.util.Functions;
import smartfire.util.Sorted;

/**
 * The default reconciliation method.
 */
@MetaInfServices
public class DefaultReconciliationMethod implements ReconciliationMethod {
    private static final Logger log = LoggerFactory.getLogger(DefaultReconciliationMethod.class);
    private final GeometryBuilder geometryBuilder;
    private final ReconciliationStream stream;
    private final String WILDFIRE = "WF";
    private final int MULTIPOLYGON_COMPLEXITY_BUFFERING_THRESHOLD = 200;
    private final int maxNumBackwardDays;
    private final DateTime reconciliationStartTime;
    private final DateTime reconciliationEndTime;

    public DefaultReconciliationMethod(GeometryBuilder geometryBuilder, ReconciliationStream stream,
            DateTime reconciliationStartTime, DateTime reconciliationEndTime,
            @Attribute(name = "maxNumBackwardDays",
            description = "Maximum number of days allowed backward in time to reconcile.  Enter blank or negative value to disable.") String maxNumBackwardDays) {
        this.geometryBuilder = geometryBuilder;
        this.stream = stream;
        this.reconciliationStartTime = reconciliationStartTime;
        this.reconciliationEndTime = reconciliationEndTime;
        this.maxNumBackwardDays = !maxNumBackwardDays.isEmpty() ? Integer.parseInt(maxNumBackwardDays) : -1; // If blank, then disable feature by setting to negative value
    }

    @Override
    public void reconcile(Fire fire, QueryableEventSet eventSet) {
        ReconciliationWeighting weights = stream.getWeightingForSource(fire.getSource());
        if(weights == null) {
            log.warn("Reconciliation Weightings are NULL for Source: {}. Skipping fire.", fire.getSource().getName());
            return;
        }
        log.info("Processing fire: " + fire.getUniqueId());

        // Multiply by 1000 to convert from km to meters, and divide by two to
        // convert into a symmetric buffer distance (e.g. uncertainty of 10 km
        // means plus or minus 5000 meters).
        double bufferDistance = weights.getLocationUncertainty() * 1000 / 2;
        Period forwardPeriod = Period.days(weights.getEndDateUncertainty());
        Period backwardPeriod = Period.days(weights.getStartDateUncertainty());

        Geometry queryShape;
        int numIndependentPolys = fire.getShape().getNumGeometries();
        if(numIndependentPolys > MULTIPOLYGON_COMPLEXITY_BUFFERING_THRESHOLD) {
            queryShape = fire.getShape();
            log.info("This geometry has exceeded the multipolygon complexity threshold with {} independent polygons. Using unbuffered shape.", numIndependentPolys);
        } else {
            queryShape = fire.getShape().buffer(bufferDistance);
        }

        DateTime startDate = fire.getStartDateTime().minus(backwardPeriod);
        DateTime endDate = fire.getEndDateTime().plus(forwardPeriod);
        
        List<Event> events = eventSet.getAssociated(queryShape, startDate, endDate);

        final Event result;
        switch(events.size()) {
            case 0:
                result = new Event();
                break;

            case 1:
                result = events.get(0);
                break;

            default:
                result = eventSet.merge(events);
                break;
        }
        result.addFire(fire);
        result.setReconciliationStream(stream);

        populateEventFields(result);

        eventSet.add(result);
    }

    protected void populateEventFields(Event event) {
        List<EventSlice> slices = event.getSlices();

        event.setShape(determineOutlineShape(event, slices));
        associateSummaryData(event);
        event.setDisplayName(determineDisplayName(event, slices));
        event.setStartDate(determineStartDate(event, slices));
        event.setEndDate(determineEndDate(event, slices));
        event.setTotalArea(determineTotalArea(event, slices));
        event.setProbability(determineProbability(event, slices));
        event.setFireType(determineFireType(event, slices));
        event.setEventDays(determineEventDays(event, slices));
        setEventAttributes(event, slices);
    }

    protected String determineDisplayName(Event event, List<EventSlice> slices) {
        for(EventSlice slice : Sorted.by(slices, EventSlice.BY_NAME_WEIGHT_DESC)) {
            String name = Fire.UNKNOWN_FIRE_NAME;
            DateTime endDate = null;
            for(Fire fire : slice.getFires()) {
                if(endDate == null) {
                    endDate = fire.getEndDateTime();
                    name = fire.getDisplayName();
                } else if(endDate.isBefore(fire.getEndDateTime())) {
                    endDate = fire.getEndDateTime();
                    name = fire.getDisplayName();
                }
            }
            if(!name.equals(Fire.UNKNOWN_FIRE_NAME)) {
                event.setWeightingSourceName(Weighting.NAME_WEIGHT, slice.getSource());
                return name;
            }
        }

        event.setWeightingSourceName(Weighting.NAME_WEIGHT, Source.UNKNOWN_SOURCE_NAME);
        String location = Functions.formatLocation(event);
        if("Unknown".equals(location)) {
            return Fire.UNKNOWN_FIRE_NAME;
        }

        return "Unnamed fire in " + location;
    }

    protected DateTime determineStartDate(Event event, List<EventSlice> slices) {
        // 
        EventSlice tempSlice = Sorted.by(slices, EventSlice.BY_START_DATE_ASC).get(0);
        event.setWeightingSourceName(Weighting.START_DATE_UNCERTAINTY, tempSlice.getSource());
        
        // Finds the earliest possible fire without considering start date uncertainty
        DateTime startDate = null;
        for(EventSlice slice : slices) {
            for(Fire fire : slice.getFires()) {
                if(startDate == null) {
                    startDate = fire.getStartDateTime();
                } else if(startDate.isAfter(fire.getStartDateTime())) {
                    startDate = fire.getStartDateTime();
                }
            }
        }
        return startDate;
    }

    protected DateTime determineEndDate(Event event, List<EventSlice> slices) {
        EventSlice slice = Sorted.by(slices, EventSlice.BY_END_DATE_ASC).get(0);
        event.setWeightingSourceName(Weighting.END_DATE_UNCERTAINTY, slice.getSource());
        DateTime endDate = null;
        for(Fire fire : slice.getFires()) {
            if(endDate == null) {
                endDate = fire.getEndDateTime();
            } else if(endDate.isBefore(fire.getEndDateTime())) {
                endDate = fire.getEndDateTime();
            }
        }
        return endDate;
    }

    protected double determineTotalArea(Event event, List<EventSlice> slices) {
        EventSlice slice = Sorted.by(slices, EventSlice.BY_SIZE_WEIGHT_DESC).get(0);
        event.setWeightingSourceName(Weighting.SIZE_WEIGHT, slice.getSource());
        double totalArea = 0;
        for(Fire fire : slice.getFires()) {
            totalArea += fire.getArea();
        }
        return totalArea;
    }

    protected MultiPolygon determineOutlineShape(Event event, List<EventSlice> slices) {
        EventSlice slice = Sorted.by(slices, EventSlice.BY_SHAPE_WEIGHT_DESC).get(0);
        event.setWeightingSourceName(Weighting.SHAPE_WEIGHT, slice.getSource());
        Geometry geom = Union.unionAllShapes(slice.getFires());
        return Union.toMultiPolygon(geom);
    }

    protected double determineProbability(Event event, List<EventSlice> slices) {
        // 1 - (1 - 0.9)*(1 - 0.8) = 0.98
        double inverseTotal = 1.0;
        for(Fire fire : event.getFires()) {
            Double fireProb = fire.getProbability();
            if(fireProb != null) {
                double inverse = (1.0 - fireProb);
                inverseTotal *= inverse;
            }
        }
        return (1.0 - inverseTotal);
    }

    protected String determineFireType(Event event, List<EventSlice> slices) {
        EventSlice slice = Sorted.by(slices, EventSlice.BY_TYPE_WEIGHT_DESC).get(0);
        event.setWeightingSourceName(Weighting.TYPE_WEIGHT, slice.getSource());
        String fireType = "";
        for(Fire fire : slice.getFires()) {
            fireType = fire.getFireType();
            
            // If fire is EVER predicted to be wildfire than that should be the defining fire type.
            if(fireType.equals(WILDFIRE)) {
               return fireType; 
            }
        }
        return fireType;
    }

    protected List<EventDay> determineEventDays(Event event, List<EventSlice> slices) {
        EventSlice slice = Sorted.by(slices, EventSlice.BY_GROWTH_WEIGHT_DESC).get(0);
        event.setWeightingSourceName(Weighting.GROWTH_WEIGHT, slice.getSource());
        double totalArea = event.getTotalArea();

        double sumArea = 0;
        SortedMap<LocalDate, List<FireDay>> dateFires = new TreeMap<LocalDate, List<FireDay>>();
        for(Fire fire : slice.getFires()) {
            for(FireDay fireDay : fire.getFireDays()) {
                LocalDate date = fireDay.getDate();
                List<FireDay> fireDayList = dateFires.get(date);
                if(fireDayList == null) {
                    fireDayList = Lists.newArrayList();
                    dateFires.put(date, fireDayList);
                }
                fireDayList.add(fireDay);
                sumArea += fireDay.getArea();
            }
        }

        double scaleFactor = totalArea / sumArea;

        List<EventDay> result = Lists.newArrayList();

        for(Map.Entry<LocalDate, List<FireDay>> entry : dateFires.entrySet()) {
            LocalDate date = entry.getKey();
            List<FireDay> fireDays = entry.getValue();

            double fireDayArea = 0;
            for(FireDay fireDay : fireDays) {
                fireDayArea += fireDay.getArea();
            }

            double eventDayArea = fireDayArea * scaleFactor;

            EventDay eventDay = new EventDay();
            eventDay.setEventDate(date);
            eventDay.setDailyArea(eventDayArea);
            result.add(eventDay);
        }

        return result;
    }

    protected void setEventAttributes(Event event, List<EventSlice> slices) {
        Map<String, String> attrs = Maps.newHashMap();

        for(EventSlice slice : Sorted.by(slices, EventSlice.BY_NAME_WEIGHT_ASC)) {
            for(Fire fire : Sorted.by(slice.getFires(), Fire.BY_SIZE_ASC)) {
                attrs.putAll(fire);
            }
        }

        event.clear();
        event.putAll(attrs);
    }

    protected void associateSummaryData(Event event) {
        Geometry eventShape = event.getShape();
        for(SummaryDataLayer layer : stream.getSummaryDataLayers()) {
            LayerAttributes attributes = Layers.readAttributes(geometryBuilder, layer, eventShape);
            if(attributes.getRepresentativeFraction() > 0) {
                associateLayerAttributes(event, attributes);
            }
        }
    }

    protected void associateLayerAttributes(Event event, LayerAttributes attributes) {
        String repFractionName = attributes.getSummaryDataLayer().getName()
                + "_representative_fraction";
        String repFractionValue = Functions.formatPercent(attributes.getRepresentativeFraction());
        event.putAll(attributes);
        event.put(repFractionName, repFractionValue);
    }
}

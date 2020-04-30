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
package smartfire;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import smartfire.database.*;
import smartfire.gis.Render;
import smartfire.gis.XYPoint;
import smartfire.util.Functions;

@ExportedBean
public class IndividualEventView extends ModelView {
    private final Event event;
    private final List<EventSlice> slices;
    private final List<EventDay> eventDays;
    private final ReconciliationStream stream;
    private final String latitude;
    private final String longitude;
    private final String location;
    private final String additionalLocation;

    IndividualEventView(Application app, Event event) {
        super(app);
        this.event = event;
        this.slices = event.getSlices();
        this.eventDays = Lists.newArrayList(event.getEventDays());
        this.stream = event.getReconciliationStream();
        ApplicationSettings appSettings = app.getAppSettings();
        
        Collections.sort(eventDays, EventDay.BY_DATE_ASC);
        
        // Get calculated lat lon
        DecimalFormat coordFormat = new DecimalFormat("#.###");
        XYPoint point = appSettings.getGeometryBuilder().buildLatLonFromPoint(
                event.getExportPointX(), event.getExportPointY());
        this.latitude = coordFormat.format(point.getY());
        this.longitude = coordFormat.format(point.getX());

        this.location = Functions.formatLocation(event);
        String loc = event.get("location");
        if(this.location.equals(loc)) {
            this.additionalLocation = null;
        } else {
            this.additionalLocation = loc;
        }
    }
    
    /*
     * Views
     */
    @WebMethod(name="shape.png")
    public void doShapePng(StaplerRequest request, StaplerResponse response) throws Exception {
        response.setHeader("Content-type", "image/png");
        OutputStream out = null;
        try {
            out = response.getCompressedOutputStream(request);
            BufferedImage image = Render.drawScaledShape(300, 300, event.getShape());
            ImageIO.write(image, "png", out);
        } finally {
            if(out != null) {
                out.close();
            }
        }
    }

    @WebMethod(name="thumbnail.png")
    public void doThumbnailPng(StaplerRequest request, StaplerResponse response) throws Exception {
        response.setHeader("Content-type", "image/png");
        OutputStream out = null;
        try {
            out = response.getCompressedOutputStream(request);
            BufferedImage image = Render.drawScaledShape(32, 32, event.getShape());
            ImageIO.write(image, "png", out);
        } finally {
            if(out != null) {
                out.close();
            }
        }
    }

    public Api getApi() {
        return new Api(this);
    }

    /*
     *  Support Methods
     */
    public Event getEvent() {
        return event;
    }

    @Exported
    public String getLocation() {
        return location;
    }

    public boolean hasAdditionalLocation() {
        return (additionalLocation != null);
    }

    @Exported
    public String getAdditionalLocation() {
        return additionalLocation;
    }

    @Exported
    public String getContainment() {
        String percentContained = event.get("percent contained");
        if(percentContained != null) {
            return percentContained + "% contained";
        }
        return "Unknown";
    }
    
    public List<Fire> getFires() {
        List<Fire> result = Lists.newArrayList(event.getFires());
        Collections.sort(result, Fire.BY_START_DATE_ASC);
        return result;
    }

    public ReconciliationStream getStream() {
        return stream;
    }

    @Exported
    public String getEventName() {
        return event.getDisplayName();
    }

    @Exported(inline=true)
    public List<EventDay> getEventDays() {
        return eventDays;
    }

    public List<EventSlice> getSlices() {
        return slices;
    }
    
    public List<Source> getSources() {
        List<Source> result = Lists.newArrayList();
        for(EventSlice slice : slices) {
            result.add(slice.getSource());
        }
        return result;
    }

    @Exported
    public String getLatitude() {
        return latitude;
    }

    @Exported
    public String getLongitude() {
        return longitude;
    }

    @Exported
    public String getShapeWkt() {
        return event.getShape().toText();
    }

    @Exported(name="attributes")
    public Map<String, String> getAttributeMap() {
        Map<String, String> result = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
        for(String key : event.getExtraExportMemberMap().keySet()) {
            Object value = event.getExtraExportMember(key);
            if(value == null) {
                result.put(key, "");
            } else {
                result.put(key, Functions.formatGeneral(value));
            }
        }
        return result;
    }
}

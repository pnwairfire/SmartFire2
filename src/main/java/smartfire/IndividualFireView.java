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

import com.google.common.collect.Maps;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import smartfire.database.Fire;
import smartfire.database.FireDay;
import smartfire.database.Source;
import smartfire.gis.Render;
import smartfire.gis.XYPoint;
import smartfire.util.Functions;

public class IndividualFireView extends ModelView {
    private final ApplicationSettings appSettings;
    private final Fire fire;
    private final String latitude;
    private final String longitude;
    
    IndividualFireView(Application app, Fire fire) {
        super(app);
        this.appSettings = getAppSettings();
        this.fire = fire;
        
        // Get calculated lat lon
        DecimalFormat coordFormat = new DecimalFormat("#.###");
        XYPoint point = appSettings.getGeometryBuilder().buildLatLonFromPoint(
                fire.getExportPointX(), fire.getExportPointY());
        this.latitude = coordFormat.format(point.getY());
        this.longitude = coordFormat.format(point.getX());
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
            BufferedImage image = Render.drawScaledShape(300, 300, fire.getShape());
            ImageIO.write(image, "png", out);
        } finally {
            if(out != null) {
                out.close();
            }
        }
    }
    
    /*
     *  Support Methods
     */
    public Source getSource() {
        return fire.getSource();
    }
    
    public Fire getFire() {
        return fire;
    }
    
    public List<FireDay> getFireDays() {
        return fire.getFireDays();
    }
    
    public String getFireName() {
        return fire.getDisplayName();
    }
    
    public String getLatitude() {
        return latitude;
    }
    
    public String getLongitude() {
        return longitude;
    }
    
    public Map<String, String> getAttributeMap() {
        Map<String, String> result = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
        for(String key : fire.getExtraExportMemberMap().keySet()) {
            Object value = fire.getExtraExportMember(key);
            if(value == null) {
                result.put(key, "");
            } else {
                result.put(key, Functions.formatGeneral(value));
            }
        }
        return result;
    }
}

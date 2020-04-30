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
package smartfire.admin;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import smartfire.Application;
import smartfire.ApplicationSettings;
import smartfire.ModelView;
import smartfire.database.DatabaseConnection;
import smartfire.database.SummaryDataLayer;
import smartfire.gis.Render;
import smartfire.layer.Layers;

public class DataLayerView extends ModelView {
    private final ApplicationSettings appSettings;
    private final DatabaseConnection conn;
    private final SummaryDataLayer layer;

    DataLayerView(Application app, SummaryDataLayer layer) {
        super(app);
        this.appSettings = getAppSettings();
        this.conn = this.appSettings.getDatabaseConnection();
        this.layer = layer;
    }
    /*
     *  Views
     */

    public void doShapeImg(StaplerRequest request, StaplerResponse response) throws Exception {
        response.setHeader("Content-type", "image/png");
        OutputStream out = null;
        try {
            out = response.getCompressedOutputStream(request);
            BufferedImage image = Render.drawScaledShape(300, 300, layer.getExtent());
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
    public SummaryDataLayer getLayer() {
        return this.layer;
    }

    public List<String> getLayerReadingMethods() {
        return Layers.getLayerReadingMethods();
    }
}

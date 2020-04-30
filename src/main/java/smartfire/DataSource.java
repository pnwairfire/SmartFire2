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

import org.joda.time.Interval;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import smartfire.database.*;

@ExportedBean
public class DataSource extends ModelView {
    private final DatabaseConnection conn;
    private final Source source;
    private final Interval dataInterval;

    DataSource(Application app, Source source) {
        super(app);
        this.conn = app.getAppSettings().getDatabaseConnection();
        this.source = source;
        this.dataInterval = conn.getRawData().getDataInterval(source);
    }   

    /*
     *  Views
     */

    public DataView<RawData> getRaw() {
        return new DataView<RawData>(getApp(), this.source, RawData.class);
    }

    public DataView<Clump> getClump() {
        return new DataView<Clump>(getApp(), this.source, Clump.class);
    }

    public DataView<Fire> getFires() {
        return new DataView<Fire>(getApp(), this.source, Fire.class);
    }

    public FireView getFire() {
        return new FireView(getApp(), this.source);
    }

    /*
     * API
     */

    public Api getApi() {
        return new Api(this);
    }

    @Exported
    public String getName() {
        return source.getName();
    }

    @Exported
    public String getEarliestDataDate() {
        return dataInterval.getStart().toString();
    }

    @Exported
    public String getLatestDataDate() {
        return dataInterval.getEnd().toString();
    }

    /*
     *  Support Methods
     */

    public Source getSource() {
        return source;
    }
}

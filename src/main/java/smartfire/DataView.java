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

import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import smartfire.database.*;
import smartfire.export.Exportable;

public class DataView<T extends Exportable> extends DateIndexedView {
    private final Class<T> klass;
    private final ApplicationSettings appSettings;
    private final DatabaseConnection conn;
    private Source source;
    private final String dataType;
    private final String urlDataType;

    DataView(Application app, Source source, Class<T> klass) {
        super(app);
        this.klass = klass;
        this.appSettings = app.getAppSettings();
        this.conn = this.appSettings.getDatabaseConnection();
        this.source = source;

        if(RawData.class == klass) {
            dataType = "Raw Data";
            urlDataType = "raw";
        } else if(Clump.class == klass) {
            dataType = "Clump Data";
            urlDataType = "clump";
        } else if(Fire.class == klass) {
            dataType = "Fire Data";
            urlDataType = "fires";
        } else {
            dataType = "Data";
            urlDataType = "";
        }
    }

    @Override
    protected Object getViewObjectByDate(DateTime startDate, DateTime endDate) {
        String individualLinkField = null;
        String individualLinkPrefix = null;
        List<? extends Exportable> dataList = null;
        if(endDate == null) {
            if(RawData.class == klass) {
                dataList = this.conn.getRawData().getByDate(source, startDate);
            } else if(Clump.class == klass) {
                dataList = this.conn.getClump().getByDate(source, startDate);
            } else if(Fire.class == klass) {
                dataList = this.conn.getFire().getByDate(source, startDate);
                individualLinkField = "unique_id";
                individualLinkPrefix = "/data/" + source.getNameSlug() + "/fire/";
            }
        } else {
            if(RawData.class == klass) {
                dataList = this.conn.getRawData().getByDate(source, startDate, endDate);
            } else if(Clump.class == klass) {
                dataList = this.conn.getClump().getByDate(source, startDate, endDate);
            } else if(Fire.class == klass) {
                dataList = this.conn.getFire().getByDate(source, startDate, endDate);
                individualLinkField = "unique_id";
                individualLinkPrefix = "/data/" + source.getNameSlug() + "/fire/";
            }
        }

        if(dataList == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<T> entities = (List<T>) dataList;
        
        // Get display string for dates
        String dateString;
        String urlString;
        if(endDate != null) {
            dateString = startDate.toString("MMM d, yyyy") + " to " + endDate.toString("MMM d, yyyy");
            urlString = "range/?startDate=" + startDate.toString("yyyyMMdd") + "&endDate=" + endDate.toString("yyyyMMdd");
        } else {
            dateString = startDate.toString("MMM d, yyyy");
            urlString = startDate.toString("yyyyMMdd") + "/";
        }

        return new ExportTableView<T>(
                getApp(),
                klass,
                startDate,
                endDate,
                entities,
                source.getName() + " " + dataType,
                Arrays.asList("Data", source.getName(), dataType, dateString),
                Arrays.asList(
                "/data/",
                "/data/" + source.getNameSlug() + "/",
                "/data/" + source.getNameSlug() + "/" + urlDataType + "/",
                "/data/" + source.getNameSlug() + "/" + urlDataType + "/" + urlString),
                individualLinkField,
                individualLinkPrefix);
    }

    @Override
    protected DateTime getCurrentDate() {
        return source.getLatestData();
    }

    /*
     *  Support Methods
     */
    public String getDataType() {
        return this.dataType;
    }

    public String getUrlDataType() {
        return this.urlDataType;
    }

    public Source getSource() {
        return source;
    }
}

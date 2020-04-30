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

import com.sti.justice.util.StringUtil;
import java.util.List;
import org.joda.time.DateTime;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import smartfire.export.ExportMethod;
import smartfire.export.ExportRow;
import smartfire.export.Exportable;
import smartfire.export.Exports;

public class ExportTableView<T extends Exportable> extends ModelView {
    private final ApplicationSettings appSettings;
    private final List<T> dataList;
    private final Class<T> klass;
    private final DateTime startDate;
    private final DateTime endDate;
    private final String title;
    private final List<String> breadcrumbNames;
    private final List<String> breadcrumbUrls;
    private final List<String> attributeHeadings;
    private final String individualLinkField;
    private final String individualLinkPrefix;

    ExportTableView(
            Application app,
            Class<T> klass,
            DateTime startDate,
            DateTime endDate,
            List<T> dataList,
            String title,
            List<String> breadcrumbNames,
            List<String> breadcrumbUrls,
            String individualLinkField,
            String individualLinkPrefix) {
        super(app);
        this.appSettings = app.getAppSettings();
        this.klass = klass;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dataList = dataList;
        this.title = title;
        this.breadcrumbNames = breadcrumbNames;
        this.breadcrumbUrls = breadcrumbUrls;

        this.attributeHeadings = Exports.getAllHeadings(dataList);
        this.individualLinkField = individualLinkField;
        this.individualLinkPrefix = individualLinkPrefix;
    }

    /*
     *  Views
     */
    public ExportTableView<T> getHtml() {
        return this;
    }

    public void doDynamic(StaplerRequest request, StaplerResponse response) throws Exception {
        String urlPiece = request.getRestOfPath();
        if(urlPiece.startsWith("/")) {
            urlPiece = urlPiece.substring(1);
        }
        Exports.handleDynamicRequest(
                urlPiece,
                klass,
                dataList,
                startDate,
                endDate,
                appSettings,
                getFileName(),
                request,
                response);
    }

    /*
     *  Support Methods
     */
    public String getTitle() {
        return title;
    }

    public List<ExportMethod> getExportMethods() {
        return Exports.getExportMethodsForType(klass);
    }
    
    public String getQueryString() {
        return "?startDate=" + startDate.toString("yyyyMMdd") + "&endDate=" + endDate.toString("yyyyMMdd");
    }

    public List<String> getBreadcrumbNames() {
        return breadcrumbNames;
    }

    public List<String> getBreadcrumbUrls() {
        return breadcrumbUrls;
    }

    public boolean isLinkable() {
        return (individualLinkField != null);
    }

    public List<String> getAttributeHeadings() {
        return attributeHeadings;
    }

    public List<ExportRow> getExportRows() {
        return Exports.getExportRows(appSettings.getGeometryBuilder(), dataList);
    }

    public String getLinkUrl(ExportRow row) {
        return individualLinkPrefix + row.getExportMember(individualLinkField);
    }

    public String getDataDate() {
        if(endDate != null) {
            return startDate.toString("MMM d, yyyy") + " to " + endDate.toString("MMM d, yyyy");
        } else {
            return startDate.toString("MMM d, yyyy");
        }
    }

    private String getFileName() {
        if(endDate != null) {
            return StringUtil.slugify(title) + startDate.toString("yyyyMMdd") + "-" + endDate.toString("yyyyMMdd");
        } else {
            return StringUtil.slugify(title) + startDate.toString("yyyyMMdd");
        }
    }
}

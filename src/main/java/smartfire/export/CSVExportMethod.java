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
package smartfire.export;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import org.joda.time.DateTime;
import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.StaplerRequest;
import smartfire.ApplicationSettings;
import smartfire.gis.GeometryBuilder;

/**
 * ExportMethod for exporting as CSV files.
 */
@MetaInfServices(ExportMethod.class)
public class CSVExportMethod extends AbstractExportMethod<Exportable> implements ExportMethod {

    public CSVExportMethod() {
        super("CSV", "csv", "/images/icons/csvfile-32x32.png", Exportable.class, "text/csv", ".csv");
    }

    @Override
    public void performExport(
            StaplerRequest request,
            OutputStream stream, 
            ApplicationSettings appSettings,
            String exportFileName,
            List<Exportable> records,
            DateTime startDate,
            DateTime endDate
            ) throws IOException {
        GeometryBuilder geometryBuilder = appSettings.getGeometryBuilder();
        Writer out = new OutputStreamWriter(stream);
        CSVWriter writer = new CSVWriter(out);
        List<? extends Exportable> recordList = Lists.newArrayList(records);
        
        // Add Headings to CSV file
        List<String> headings = Exports.getAllHeadings(records);
        String[] entriesOutput = new String[headings.size()];
        headings.toArray(entriesOutput);
        writer.writeNext(entriesOutput);

        // Add data attributes to CSV file
        List<ExportRow> rows = Exports.getExportRows(geometryBuilder, recordList);
        for(ExportRow row : rows) {
            row.copyToArray(entriesOutput);
            writer.writeNext(entriesOutput);
        }
        
        writer.close();
        out.close();
    }
}

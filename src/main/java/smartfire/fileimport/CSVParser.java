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
package smartfire.fileimport;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

public class CSVParser {
    private final Logger log = LoggerFactory.getLogger(CSVParser.class);
    private String[] fieldNames;
    private List<Object[]> data;

    public CSVParser(InputStream fileStream) throws Exception {
        log.info("Parsing CSV file.");
        parse(fileStream);
    }

    public CSVParser(String filePath) throws Exception {
        this(new FileInputStream(new File(filePath)));
    }

    private void parse(InputStream fileStream) throws Exception {
        CSVReader reader = null;
        try {
            // Read CSV data
            reader = new CSVReader(new InputStreamReader(fileStream));
            log.info("CSV file successfully read");

            // Get CSV Headings
            fieldNames = null;
            try {
                fieldNames = reader.readNext();
            } catch(IOException e) {
                log.warn("Unable to read CSV file.", e);
            }
            fieldNames = trimRow(fieldNames);
            log.info("Parsed Headings in CSV file.");

            // Get CSV Data
            data = Lists.newArrayList();
            int count = 1;
            String[] nextLine;
            try {
                while((nextLine = reader.readNext()) != null) {
//                    log.info("Parsing line {} in CSV file.", count);
                    nextLine = trimRow(nextLine);
                    data.add(nextLine);
                    count = count + 1;
                }
            } catch(IOException e) {
                log.warn("Unable to read CSV file. Line: " + count, e);
            }
            log.info("Parsed all rows in CSV file.");
        } catch(Exception e) {
            log.warn("Issue parsing the CSV file.", e);
        } finally {
            if(reader != null) {
                reader.close();
            }
            if(fileStream != null) {
                fileStream.close();
            }
        }
    }

    public static String[] trimRow(String[] row) {
        String[] trimmed = new String[row.length];
        for(int i = 0; i < row.length; i++) {
            trimmed[i] = row[i].trim();
        }
        return trimmed;
    }

    public List<Object[]> getData() {
        return data;
    }

    public String[] getFieldNames() {
        return fieldNames;
    }
}

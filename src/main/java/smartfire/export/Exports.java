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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.joda.time.DateTime;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import smartfire.ApplicationSettings;
import smartfire.SmartfireException;
import smartfire.gis.GeometryBuilder;

/**
 * Static factory for ExportMethod instances.
 */
public final class Exports {
    private static final Map<String, ExportMethod> methodsBySlugName;
    private static final Map<String, ExportMethod> methodsByDisplayName;
    
    static {
        methodsBySlugName = Maps.newHashMap();
        methodsByDisplayName = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
        ServiceLoader<ExportMethod> serviceLoader = ServiceLoader.load(ExportMethod.class);
        for(ExportMethod method : serviceLoader) {
            methodsBySlugName.put(method.getSlugName(), method);
            methodsByDisplayName.put(method.getDisplayName(), method);
        }
    }
    
    private Exports() { }
    
    public static <T extends Exportable> void handleDynamicRequest(
            String urlPiece, 
            Class<T> entityClass, 
            List<T> entities,
            DateTime startDate,
            DateTime endDate,
            ApplicationSettings appSettings,
            String fileBaseName,
            StaplerRequest request, 
            StaplerResponse response
            ) throws IOException {
        
        ExportMethod method = methodsBySlugName.get(urlPiece);
        Class<? extends Exportable> exportableType = method.getExportableType();
        if(!exportableType.isAssignableFrom(entityClass)) {
            throw new SmartfireException("Unable to export data of type " + entityClass.getName()
                    + " using ExportMethod " + method.getDisplayName());
        }
        
        @SuppressWarnings("unchecked")
        List<Exportable> downcastList = (List<Exportable>) Collections.unmodifiableList(entities);
        
        response.setHeader("Content-type", method.getContentType());
        String attachmentFileName = fileBaseName + method.getFileExtension();
        response.setHeader("Content-disposition", "attachment;filename=" + attachmentFileName);
        OutputStream out = null;
        try {
            out = response.getCompressedOutputStream(request);
            method.exportToStream(request, out, appSettings, attachmentFileName, downcastList, startDate, endDate);
        } finally {
            if(out != null) {
                out.close();
            }
        }
    }
    
    public static Map<String, Class<?>> getMemberMapSpecialFields(Iterable<? extends Exportable> records) {
        Map<String, Class<?>> result = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
        
        List<? extends Exportable> recordList = Lists.newArrayList(records);
        if (!recordList.isEmpty()) {
            result.putAll(recordList.get(0).getBasicMemberMap());
        }
        
        result.put("area_acres", Long.class);
        result.put("area_meters", Long.class);
        result.put("latitude", String.class);
        result.put("longitude", String.class);
        result.put("start_date", Date.class);
        result.put("end_date", Date.class);
        
        return result;
    }
    
    public static Map<String, Class<?>> getMemberMapAllFields(Iterable<? extends Exportable> records) {
        // Use a TreeMap so the fields are sorted
        Map<String, Class<?>> result = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);

        // Union all members to ensure no missing fields.
        for(Exportable record : records) {
            result.putAll(record.getExtraExportMemberMap());
        }

        // Add special-case some added fields
        result.putAll(getMemberMapSpecialFields(records));
        
        // Ignore existence of attribute called 'shape' to ensure that shape data doesn't 
        // get passed along as an attribute field.
        result.remove("shape");
        
        return result;
    }

    public static List<String> getAllHeadings(Iterable<? extends Exportable> records) {
        return Lists.newArrayList(getMemberMapAllFields(records).keySet());
    }
    
    public static List<ExportRow> getExportRows(GeometryBuilder geometryBuilder, Iterable<? extends Exportable> records, List<String> attributeNames) {
        List<? extends Exportable> entities = Lists.newArrayList(records);
        List<ExportRow> result = Lists.newArrayListWithExpectedSize(entities.size());
        for(Exportable entity : entities) {
            result.add(new ExportRow(geometryBuilder, attributeNames, entity));
        }
        Collections.sort(result, ExportRow.BY_AREA_DESC);
        return result;
    }
    
    public static List<ExportRow> getExportRows(GeometryBuilder geometryBuilder, Iterable<? extends Exportable> records) {
        List<? extends Exportable> entities = Lists.newArrayList(records);
        List<String> attributeNames = getAllHeadings(entities);
        return getExportRows(geometryBuilder, records, attributeNames);
    }
    
    public static Map<String, Object> getExportedValues(Exportable entity) {
        Map<String, Object> result = Maps.newLinkedHashMap();
        for(String key : entity.getExtraExportMemberMap().keySet()) {
            result.put(key, entity.getExtraExportMember(key));
        }
        return result;
    }
    
    public static List<ExportMethod> getExportMethods() {
        return Lists.newArrayList(methodsByDisplayName.values());
    }
    
    public static List<ExportMethod> getExportMethodsForType(Class<? extends Exportable> entityClass) {
        List<ExportMethod> result = Lists.newArrayList();
        for(ExportMethod method : methodsByDisplayName.values()) {
            Class<? extends Exportable> exportableType = method.getExportableType();
            if(exportableType.isAssignableFrom(entityClass)) {
                result.add(method);
            }
        }
        return result;
    }
}

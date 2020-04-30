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
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.*;
import java.util.List;
import smartfire.database.Source;

public class ExportXML {
    public ExportXML() {
    }

    public void saveSources(List<Source> sources, File dir, String filename) throws Exception {
        XStream xstream = new XStream();
        xstream.autodetectAnnotations(true);
        xstream.addDefaultImplementation(
                org.hibernate.collection.PersistentList.class, java.util.List.class);
        xstream.addDefaultImplementation(
                org.hibernate.collection.PersistentMap.class, java.util.Map.class);
        xstream.addDefaultImplementation(
                org.hibernate.collection.PersistentSet.class, java.util.Set.class);
        Mapper mapper = xstream.getMapper();
        xstream.registerConverter(new HibernateCollectionConverter(mapper));
        xstream.registerConverter(new HibernateMapConverter(mapper));

        File xmlFile = new File(dir, filename);
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(xmlFile));
        try {
            xstream.toXML(sources, stream);
        } finally {
            stream.close();
        }
    }

    public List<Source> readSources(File dir, String filename) throws Exception {
        XStream xstream = new XStream();
        xstream.autodetectAnnotations(true);
        Mapper mapper = xstream.getMapper();
        xstream.registerConverter(new HibernateCollectionConverter(mapper));
        xstream.registerConverter(new HibernateMapConverter(mapper));

        File xmlFile = new File(dir, filename);
        InputStream stream = new BufferedInputStream(new FileInputStream(xmlFile));
        List<Source> sources = Lists.newArrayList();
        try {
            sources = (List<Source>) xstream.fromXML(stream);
        } finally {
            stream.close();
        }
        return sources;
    }

    class HibernateMapConverter extends MapConverter {
        HibernateMapConverter(Mapper mapper) {
            super(mapper);
        }

        public boolean canConvert(Class type) {
            return super.canConvert(type)
                    || org.hibernate.collection.PersistentMap.class.equals(type);
        }
    }

    class HibernateCollectionConverter extends CollectionConverter {
        HibernateCollectionConverter(Mapper mapper) {
            super(mapper);
        }

        public boolean canConvert(Class type) {
            return super.canConvert(type)
                    || org.hibernate.collection.PersistentList.class.equals(type)
                    || org.hibernate.collection.PersistentSet.class.equals(type);
        }
    }
}

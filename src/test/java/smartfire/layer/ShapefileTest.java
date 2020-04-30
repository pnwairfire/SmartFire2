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
package smartfire.layer;

import com.sti.justice.util.ZipUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import smartfire.Config;
import smartfire.gis.GeometryBuilder;

/**
 * Abstract test case that sets up a working directory with a shapefile.
 */
public abstract class ShapefileTest extends TestCase {
    protected File zipFolder;
    protected File shapefileFolder;
    protected String shapefilePath;
    protected GeometryBuilder geometryBuilder;

    public ShapefileTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        this.zipFolder = new File(System.getProperty("java.io.tmpdir") + File.separatorChar + UUID.randomUUID());
        if(!zipFolder.exists()) {
            zipFolder.mkdir();
        }
        File zipFile = new File(zipFolder, "States_USA.zip");
        InputStream in = null;
        OutputStream out = null;
        try {
            in = ClassLoader.getSystemResourceAsStream("States_USA.zip");
            out = new FileOutputStream(zipFile);
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
        List<String> shapefilePaths = ZipUtil.unzip(zipFile);
        for(String path : shapefilePaths) {
            if(path.endsWith(".shp")) {
                this.shapefilePath = path;
            }
        }
        this.shapefileFolder = new File(shapefilePath).getParentFile();
        this.geometryBuilder = new GeometryBuilder(new Config());
    }

    @Override
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(zipFolder);
        FileUtils.deleteDirectory(shapefileFolder);
    }
}

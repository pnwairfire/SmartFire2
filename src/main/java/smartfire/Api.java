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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.xml.transform.stream.StreamResult;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Flavor;
import org.kohsuke.stapler.export.ModelBuilder;
import org.kohsuke.stapler.export.SchemaGenerator;

/**
 * Modeled after the hudson.model.Api class in Hudson/Jenkins.
 */
public class Api {
    private final Object bean;

    public Api(Object bean) {
        this.bean = bean;
    }

    public void doXml(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        response.serveExposedBean(request, bean, Flavor.XML);
    }

    public void doJson(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        response.serveExposedBean(request, bean, Flavor.JSON);
    }

    public void doPython(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        response.serveExposedBean(request, bean, Flavor.PYTHON);
    }

    public void doSchema(StaplerRequest request, StaplerResponse response) throws IOException, ServletException {
        response.setContentType("application/xml");
        StreamResult result = new StreamResult(response.getOutputStream());
        new SchemaGenerator(new ModelBuilder().get(bean.getClass())).generateSchema(result);
        result.getOutputStream().close();
    }
}

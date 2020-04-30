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
package smartfire.func;

import smartfire.database.Source;
import smartfire.gis.GeometryBuilder;

/**
 * Factory for constructing UploadIngestMethods.
 */
public interface UploadIngestMethodFactory extends MethodFactory {
    /**
     * Constructs a new UploadIngestMethod implementation object.
     *
     * @param methodName name of upload ingest method to construct
     * @param source the corresponding source entity
     * @param geometryBuilder the GeometryBuilder that the FetchMethod should
     *                        use for constructing new Geometry objects
     * @return a new FetchMethod instance
     */
    UploadIngestMethod newUploadIngestMethod(String methodName, Source source, GeometryBuilder geometryBuilder);
}

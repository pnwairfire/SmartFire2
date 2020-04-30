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

/**
 * Generic MethodFactory interface; base interface of all *MethodFactory
 * interfaces.
 */
public interface MethodFactory {
    /**
     * Returns a list of all Method implementations available from this
     * MethodFactory.
     *
     * @return a collection of Strings representing the different Method
     *         implementations available
     */
    Iterable<String> getAllMethods();
    
    /**
     * Returns a MethodConfig object representing the attributes of the
     * given Method that can be configured.
     * 
     * @param methodName a String representing the desired Method implementation
     * @return a MethodConfig object
     */
    MethodConfig getMethodConfig(String methodName);
}

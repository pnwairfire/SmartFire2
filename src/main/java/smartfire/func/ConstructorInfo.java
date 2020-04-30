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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartfire.SmartfireException;

/**
 * Represents information about a constructor for a Method interface object.
 */
class ConstructorInfo<T> {
    private static final Logger log = LoggerFactory.getLogger(ConstructorInfo.class);
    private final Class<T> methodInterface;
    private final Class<? extends T> klass;
    private final Constructor<?> cons;
    private final int numParams;
    private final Class<?>[] paramTypes;
    private final Attribute[] paramAttribs;
    private final Map<String, Integer> paramLookup;
    private final Object[] paramValues;

    ConstructorInfo(Class<T> methodInterface,
            Class<? extends T> klass) {
        this.methodInterface = methodInterface;
        this.klass = klass;
        Constructor<?>[] constructors = klass.getConstructors();
        if(constructors.length != 1) {
            throw new SmartfireException("Unexpected number of public constructors for class " + klass.getName() + "; expected 1, but found " + constructors.length);
        }
        this.cons = constructors[0];
        this.paramTypes = cons.getParameterTypes();
        this.numParams = this.paramTypes.length;
        this.paramAttribs = new Attribute[numParams];
        this.paramValues = new Object[numParams];
        this.paramLookup = Maps.newHashMap();
        Annotation[][] annos = cons.getParameterAnnotations();
        for(int i = 0; i < numParams; i++) {
            Annotation[] paramAnnos = annos[i];
            for(int j = 0; j < paramAnnos.length; j++) {
                if(paramAnnos[j] instanceof Attribute) {
                    Attribute attr = (Attribute) paramAnnos[j];
                    this.paramAttribs[i] = attr;
                    this.paramLookup.put(attr.name(), Integer.valueOf(i));
                    break;
                }
            }
        }
    }

    List<Attribute> getAttributes() {
        List<Attribute> result = Lists.newArrayList();
        for(int i = 0; i < numParams; i++) {
            if(paramAttribs[i] != null) {
                result.add(paramAttribs[i]);
            }
        }
        return result;
    }

    List<String> getAttributeNames() {
        return Lists.newArrayList(paramLookup.keySet());
    }

    String getAttributeDescription(String attributeName) {
        Integer idx = paramLookup.get(attributeName);
        if(idx == null) {
            return null;
        }
        return paramAttribs[idx].description();
    }

    void setAttributeValue(String attributeName, String attributeValue) {
        Integer idx = paramLookup.get(attributeName);
        if(idx == null) {
            return;
        }
        if(paramTypes[idx] == String.class) {
            // If the parameter type is String, then let's just use that
            paramValues[idx] = attributeValue;
        } else {
            // If the attribute isn't a String directly, try to construct
            // the appropriate type from the String, if we can.
            try {
                Constructor<?> valueCons = paramTypes[idx].getConstructor(new Class<?>[] { String.class });
                Object obj = valueCons.newInstance(attributeValue);
                paramValues[idx] = obj;
            } catch(Exception ex) {
                throw new SmartfireException(String.format(
                        "Unable to convert argument #%d value \"%s\" to type \"%s\""
                        , idx, attributeValue, paramTypes[idx]), ex);
            }
        }
    }

    void setAttributeValues(Map<String, String> values) {
        for(Map.Entry<String, String> entry : values.entrySet()) {
            setAttributeValue(entry.getKey(), entry.getValue());
        }
    }

    void setWellKnownArgValues(Object[] wellKnownArgs) {
        int numArgs = Math.min(this.numParams, wellKnownArgs.length);
        for(int i = 0; i < numArgs; i++) {
            if(paramAttribs[i] != null) {
                log.debug("Got {} well-known args for {}, but encountered an @Attribute arg at position {}",
                        new Object[] { wellKnownArgs.length, klass, i });
                break;
            }
            Object arg = wellKnownArgs[i];
            if(paramTypes[i].isInstance(arg)) {
                paramValues[i] = arg;
            } else {
                throw new SmartfireException(String.format("Argument #%d has type \"%s\", which cannot accept a value of type \"%s\"", i, paramTypes[i], arg.getClass()));
            }
        }
    }

    T construct() {
        for(int i = 0; i < numParams; i++) {
            if(paramValues[i] == null) {
                if(paramAttribs[i] != null) {
                    throw new SmartfireException(String.format(
                            "Unable to construct Method implementation because attribute \"%s\" was not provided",
                            paramAttribs[i].name()));
                } else {
                    throw new SmartfireException(String.format(
                            "Unable to construct Method implementation because parameter #%d (of type %s) was not provided",
                            i, paramTypes[i]));
                }
            }
        }
        try {
            Object method = cons.newInstance(paramValues);
            return methodInterface.cast(method);
        } catch(Exception ex) {
            throw new SmartfireException("Unable to construct Method implementation object", ex);
        }
    }
}

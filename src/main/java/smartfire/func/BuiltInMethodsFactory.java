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
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for built-in MethodFactory implementations.
 * 
 * @param <TMethod>  the type of the method interface, e.g. FetchMethod.class
 */
public abstract class BuiltInMethodsFactory<TMethod> implements MethodFactory {
    private static final Logger log = LoggerFactory.getLogger(BuiltInMethodsFactory.class);
    private final Class<TMethod> methodInterface;
    private final Map<String, Class<? extends TMethod>> methodClasses;
    
    /**
     * Helper function to discover implementations (like java.util.ServiceLoader)
     * but without actually instantiating the classes.  This allows us to use
     * our ConstructorInfo class to inject constructor arguments.
     * 
     * <p>This implementation is based on a sample implementation of 
     * ServiceLoader which I adapted to this purpose.  Credit goes to
     * user "erickson" on StackOverflow: 
     * http://stackoverflow.com/questions/251336/is-something-similar-to-serviceloader-in-java-1-5/251691#251691
     * 
     * @param <T> the type of the interface that we want implementations of
     * @param methodInterface a Class object representing the interface
     * @return a set of Class objects representing possible implementations
     */
    private static <T> Set<Class<? extends T>> findImplementationsOf(Class<T> methodInterface) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> e = loader.getResources("META-INF/services/" + methodInterface.getName());
            Set<Class<? extends T>> result = Sets.newHashSet();
            while(e.hasMoreElements()) {
                URL url = e.nextElement();
                InputStream is = url.openStream();
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line;
                    while((line = r.readLine()) != null) {
                        int comment = line.indexOf('#');
                        if(comment >= 0) {
                            line = line.substring(0, comment);
                        }
                        String name = line.trim();
                        if(name.isEmpty()) {
                            continue;
                        }
                        Class<?> clazz = Class.forName(name, true, loader);
                        Class<? extends T> impl = clazz.asSubclass(methodInterface);
                        result.add(impl);
                    }
                } finally {
                    is.close();
                }
            }
            
            return result;
        } catch(Exception ex) {
            log.warn("Error while discovering method implementations", ex);
            return Collections.emptySet();
        }
    }
    
    protected BuiltInMethodsFactory(Class<TMethod> methodInterface) {
        this.methodInterface = methodInterface;
        this.methodClasses = Maps.newHashMap();
        
        Set<Class<? extends TMethod>> methods = findImplementationsOf(methodInterface);
        if(methods.isEmpty()) {
            log.warn("Unable to find any classes that implement {}", methodInterface);
        }
        for(Class<? extends TMethod> klass : methods) {
            if(klass.isInterface() || Modifier.isAbstract(klass.getModifiers())) {
                log.debug("Ignoring implementation class {} because it is abstract",
                        klass);
               continue; 
            }
            int numConstructors = klass.getConstructors().length;
            if(numConstructors != 1) {
                log.debug("Ignoring implementation class {} because it has {} public constructors",
                        klass, numConstructors);
                continue;
            }
            log.debug("Found {} implementation: {}", methodInterface, klass);
            this.methodClasses.put(klass.getName(), klass);
        }
    }
    
    protected void registerClass(Class<? extends TMethod> klass) {
        if(this.methodClasses.containsKey(klass.getName())) {
            log.debug("Ignoring already registered implementation {}", klass);
        } else {
            log.debug("Explicitly registering {} implementation: {}", methodInterface, klass);
            this.methodClasses.put(klass.getName(), klass);
        }
    }
    
    protected List<String> getClassNames() {
        return Lists.newArrayList(this.methodClasses.keySet());
    }
    
    @Override
    public Iterable<String> getAllMethods() {
        return getClassNames();
    }

    @Override
    public MethodConfig getMethodConfig(String methodName) {
        Class<? extends TMethod> klass = methodClasses.get(methodName);
        ConstructorInfo<TMethod> info = new ConstructorInfo<TMethod>(methodInterface, klass);
        List<MethodAttribute> attrs = Lists.newArrayList();
        for(Attribute attr : info.getAttributes()) {
            attrs.add(new MethodAttribute(attr.name(), attr.description()));
        }
        return new MethodConfig(attrs);
    }
    
    protected TMethod construct(String methodName, Object[] wellKnownArgs, Map<String, String> attribs) {
        Class<? extends TMethod> klass = methodClasses.get(methodName);
        ConstructorInfo<TMethod> info = new ConstructorInfo<TMethod>(methodInterface, klass);
        info.setWellKnownArgValues(wellKnownArgs);
        info.setAttributeValues(attribs);
        return info.construct();
    }
}

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
import java.util.*;
import org.joda.time.DateTime;
import smartfire.database.ReconciliationStream;
import smartfire.database.ScheduledFetch;
import smartfire.database.Source;
import smartfire.gis.GeometryBuilder;

/**
 * Static utility class for looking up Method instances.
 */
public final class Methods {
    private Methods() { }

    private static final MethodRegistry<AssociationMethodFactory> assocMethods = newRegistry(AssociationMethodFactory.class);
    private static final MethodRegistry<ClumpMethodFactory> clumpMethods = newRegistry(ClumpMethodFactory.class);
    private static final MethodRegistry<FetchMethodFactory> fetchMethods = newRegistry(FetchMethodFactory.class);
    private static final MethodRegistry<ProbabilityMethodFactory> probMethods = newRegistry(ProbabilityMethodFactory.class);
    private static final MethodRegistry<FireTypeMethodFactory> fireTypeMethods = newRegistry(FireTypeMethodFactory.class);
    private static final MethodRegistry<ReconciliationMethodFactory> recMethods = newRegistry(ReconciliationMethodFactory.class);
    private static final MethodRegistry<UploadIngestMethodFactory> ingestMethods = newRegistry(UploadIngestMethodFactory.class);

    /**
     * Returns a list of all AssociationMethod implementations available.
     *
     * @return a collection of Strings representing the different
     *         AssociationMethod implementations available
     */
    public static List<String> getAssociationMethods() {
        return assocMethods.getMethods();
    }

    /**
     * Constructs a new AssociationMethod implementation object corresponding
     * to the given implementation name.
     *
     * @param source the corresponding input Source entity
     * @return a new AssociationMethod instance
     */
    public static AssociationMethod newAssociationMethod(Source source) {
        String methodName = source.getAssocMethod();
        AssociationMethodFactory factory = assocMethods.getMethodFactory(methodName);
        return factory.newAssociationMethod(methodName, source);
    }
    
    /**
     * Returns a MethodConfig object representing the configurable attributes
     * of the named AssociationMethod.
     * 
     * @param methodName the name of the AssociationMethod of interest
     * @return a MethodConfig object
     */
    public static MethodConfig getAssociationMethodConfig(String methodName) {
        AssociationMethodFactory factory = assocMethods.getMethodFactory(methodName);
        return factory.getMethodConfig(methodName);
    }

    /**
     * Returns a list of all ClumpMethod implementations available.
     *
     * @return a collection of Strings representing the different
     *         ClumpMethod implementations available
     */
    public static List<String> getClumpMethods() {
        return clumpMethods.getMethods();
    }

    /**
     * Constructs a new ClumpMethod implementation object corresponding
     * to the given implementation name.
     *
     * @param geometryBuilder 
     * @param source the corresponding input Source entity
     * @return a new ClumpMethod instance
     */
    public static ClumpMethod newClumpMethod(GeometryBuilder geometryBuilder, Source source) {
        String methodName = source.getClumpMethod();
        ClumpMethodFactory factory = clumpMethods.getMethodFactory(methodName);
        return factory.newClumpMethod(methodName, geometryBuilder, source);
    }
    
    /**
     * Returns a MethodConfig object representing the configurable attributes
     * of the named ClumpMethod.
     * 
     * @param methodName the name of the ClumpMethod of interest
     * @return a MethodConfig object
     */
    public static MethodConfig getClumpMethodConfig(String methodName) {
        ClumpMethodFactory factory = clumpMethods.getMethodFactory(methodName);
        return factory.getMethodConfig(methodName);
    }
    
        /**
     * Returns a list of all UploadIngestMethod implementations available.
     *
     * @return a collection of Strings representing the different
     *         UploadIngestMethod implementations available
     */
    public static List<String> getUploadIngestMethods() {
        return ingestMethods.getMethods();
    }

    /**
     * Constructs a new UploadIngestMethod implementation object corresponding
     * to the given implementation name.
     *
     * @param source the corresponding input Source entity
     * @param geometryBuilder the GeometryBuilder that the UploadIngestMethod should
     *                        use for constructing new Geometry objects
     * @return a new AssociationMethod instance
     */
    public static UploadIngestMethod newUploadIngestMethod(Source source, GeometryBuilder geometryBuilder) {
        String methodName = source.getIngestMethod();
        UploadIngestMethodFactory factory = ingestMethods.getMethodFactory(methodName);
        return factory.newUploadIngestMethod(methodName, source, geometryBuilder);
    }
    
    /**
     * Returns a MethodConfig object representing the configurable attributes
     * of the named UploadIngestMethod.
     * 
     * @param methodName the name of the UploadIngestMethod of interest
     * @return a MethodConfig object
     */
    public static MethodConfig getUploadIngestMethodConfig(String methodName) {
        UploadIngestMethodFactory factory = ingestMethods.getMethodFactory(methodName);
        return factory.getMethodConfig(methodName);
    }

    /**
     * Returns a list of all FetchMethod implementations available.
     *
     * @return a collection of Strings representing the different
     *         FetchMethod implementations available
     */
    public static List<String> getFetchMethods() {
        return fetchMethods.getMethods();
    }

    /**
     * Constructs a new FetchMethod implementation object for implementing
     * the given ScheduledFetch.
     *
     * @param scheduledFetch the corresponding ScheduledFetch entity
     * @param geometryBuilder the GeometryBuilder that the FetchMethod should
     *                        use for constructing new Geometry objects
     * @return a new FetchMethod instance
     */
    public static FetchMethod newFetchMethod(ScheduledFetch scheduledFetch, GeometryBuilder geometryBuilder) {
        String methodName = scheduledFetch.getFetchMethod();
        FetchMethodFactory factory = fetchMethods.getMethodFactory(methodName);
        return factory.newFetchMethod(scheduledFetch, geometryBuilder);
    }
    
    /**
     * Returns a MethodConfig object representing the configurable attributes
     * of the named FetchMethod.
     * 
     * @param methodName the name of the FetchMethod of interest
     * @return a MethodConfig object
     */
    public static MethodConfig getFetchMethodConfig(String methodName) {
        FetchMethodFactory factory = fetchMethods.getMethodFactory(methodName);
        return factory.getMethodConfig(methodName);
    }

    /**
     * Returns a list of all ProbabilityMethod implementations available.
     *
     * @return a collection of Strings representing the different
     *         ProbabilityMethod implementations available
     */
    public static List<String> getProbabilityMethods() {
        return probMethods.getMethods();
    }

    /**
     * Constructs a new ProbabilityMethod implementation object corresponding
     * to the given implementation name.
     *
     * @param source the corresponding input Source entity
     * @return a new ProbabilityMethod instance
     */
    public static ProbabilityMethod newProbabilityMethod(Source source) {
        String methodName = source.getProbabilityMethod();
        ProbabilityMethodFactory factory = probMethods.getMethodFactory(methodName);
        return factory.newProbabilityMethod(methodName, source);
    }
    
    /**
     * Returns a MethodConfig object representing the configurable attributes
     * of the named ProbabilityMethod.
     * 
     * @param methodName the name of the ProbabilityMethod of interest
     * @return a MethodConfig object
     */
    public static MethodConfig getProbabilityMethodConfig(String methodName) {
        ProbabilityMethodFactory factory = probMethods.getMethodFactory(methodName);
        return factory.getMethodConfig(methodName);
    }
    
    /**
     * Returns a list of all FireTypeMethod implementations available.
     *
     * @return a collection of Strings representing the different
     *         FireTypeMethod implementations available
     */
    public static List<String> getFireTypeMethods() {
        return fireTypeMethods.getMethods();
    }

    /**
     * Constructs a new FireTypeMethod implementation object corresponding
     * to the given implementation name.
     *
     * @param geometryBuilder 
     * @param source the corresponding input Source entity
     * @return a new FireTypeMethod instance
     */
    public static FireTypeMethod newFireTypeMethod(GeometryBuilder geometryBuilder, Source source) {
        String methodName = source.getFireTypeMethod();
        FireTypeMethodFactory factory = fireTypeMethods.getMethodFactory(methodName);
        return factory.newFireTypeMethod(methodName, geometryBuilder, source);
    }
    
    /**
     * Returns a MethodConfig object representing the configurable attributes
     * of the named ProbabilityMethod.
     * 
     * @param methodName the name of the ProbabilityMethod of interest
     * @return a MethodConfig object
     */
    public static MethodConfig getFireTypeMethodConfig(String methodName) {
        FireTypeMethodFactory factory = fireTypeMethods.getMethodFactory(methodName);
        return factory.getMethodConfig(methodName);
    }


    /**
     * Returns a list of all ReconciliationMethod implementations available.
     *
     * @return a collection of Strings representing the different
     *         ReconciliationMethod implementations available
     */
    public static List<String> getReconciliationMethods() {
        return recMethods.getMethods();
    }

    /**
     * Constructs a new ReconciliationMethod implementation object corresponding
     * to the given implementation name.
     *
     * @param geometryBuilder the GeometryBuilder from the application settings
     * @param stream the current ReconciliationStream entity
     * @return a new ReconciliationMethod instance
     */

    public static ReconciliationMethod newReconciliationMethod(
            GeometryBuilder geometryBuilder, ReconciliationStream stream,
            DateTime startTime, DateTime endTime) {
        String methodName = stream.getReconciliationMethod();
        ReconciliationMethodFactory factory = recMethods.getMethodFactory(methodName);
        return factory.newReconciliationMethod(methodName, geometryBuilder, stream, startTime, endTime);
    }
    
    
    /**
     * Returns a MethodConfig object representing the configurable attributes
     * of the named ReconciliationMethod.
     * 
     * @param methodName the name of the ReconciliationMethod of interest
     * @return a MethodConfig object
     */
    public static MethodConfig getReconciliationMethodConfig(String methodName) {
        ReconciliationMethodFactory factory = recMethods.getMethodFactory(methodName);
        return factory.getMethodConfig(methodName);
    }

    /**
     * Returns a map of all method attributes and their description
     * for a MethodConfig.
     *
     * @param methodConfig attribute configuration for a method
     * @return a new map of attributes and their descriptions
     */
    public static Map<String, String> getMethodAttributes(MethodConfig methodConfig) {
        Map<String, String> attributes = Maps.newHashMap();
        for(String attrib : methodConfig.getAttributeNames()) {
            attributes.put(attrib, methodConfig.getAttributeDescription(attrib));
        }
        return attributes;
    }

    private static <F extends MethodFactory> MethodRegistry<F> newRegistry(Class<F> factoryInterface) {
        return new MethodRegistry<F>(factoryInterface);
    }

    private static class MethodRegistry<T extends MethodFactory> {
        private final Map<String, T> methods = new HashMap<String, T>();

        private MethodRegistry(Class<T> factoryInterface) {
            ServiceLoader<T> serviceLoader = ServiceLoader.load(factoryInterface);
            for(T factory : serviceLoader) {
                for(String method : factory.getAllMethods()) {
                    methods.put(method, factory);
                }
            }
        }

        public List<String> getMethods() {
            List<String> result = Lists.newArrayList(methods.keySet());
            Collections.sort(result);
            return Collections.unmodifiableList(result);
        }

        public T getMethodFactory(String methodName) {
            return methods.get(methodName);
        }
    }
}

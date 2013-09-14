package org.xwiki.contrib.xwikifs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Map with references.
 *
 * This is a map implementation that allows to put references in the values.
 *
 * A reference is a string of the form "-> id"
 *
 * The map can be serialized and deserialized using the Yaml format: in this case references will be read/written from
 * external files that have the name of the reference id. The id can also have the form of a relative path causing the
 * reference to be read/written in the corresponding subdirectory allowing flexibility in organising the references.
 *
 * @version $Id$
 */
public class MapWithReferences implements Map
{
    /**
     * The map that contains the raw data.
     */
    protected Map wrappedMap;

    /**
     * A mapping from the reference id to the actual value.
     */
    protected Map<String, String> references;

    /**
     * Constructor.
     */
    public MapWithReferences()
    {
        wrappedMap = new LinkedHashMap();
        references = new LinkedHashMap<String, String>();
    }

    @Override public int size()
    {
        return wrappedMap.size();
    }

    @Override public boolean isEmpty()
    {
        return wrappedMap.isEmpty();
    }

    @Override public boolean containsKey(Object key)
    {
        return wrappedMap.containsKey(key);
    }

    @Override public boolean containsValue(Object value)
    {
        return wrappedMap.containsValue(value);
    }

    @Override public Object get(Object key)
    {
        Object value = wrappedMap.get(key);

        if (value instanceof String) {
            String string = (String) value;
            if (isReference(string)) {
                String referenceId = getReferenceId(string);

                return references.get(referenceId);
            }
        }

        return value;
    }

    @Override public Object put(Object key, Object value)
    {
        Object currentValue = wrappedMap.get(key);

        if (currentValue instanceof String) {
            String string = (String) currentValue;

            if (isReference(string) && (value instanceof String)) {
                String id = getReferenceId(string);

                return references.put(id, (String) value);
            }
        }

        return wrappedMap.put(key, value);
    }

    /**
     * Store a value as a referenced value.
     *
     * @param key the key.
     * @param referenceId the reference id.
     * @param value the referenced value.
     * @return the previous referenced value.
     */
    public Object putReference(Object key, String referenceId, String value)
    {
        wrappedMap.put(key, String.format("-> %s", referenceId));

        return references.put(referenceId, value);
    }

    @Override public Object remove(Object key)
    {
        Object value = wrappedMap.get(key);

        if (value instanceof String) {
            String string = (String) value;
            if (isReference(string)) {
                String referenceId = getReferenceId(string);

                references.remove(referenceId);
            }
        }

        return wrappedMap.remove(key);
    }

    @Override public void putAll(Map m)
    {
        for (Object object : m.keySet()) {
            put(object, m.get(object));
        }
    }

    @Override public void clear()
    {
        wrappedMap.clear();
        references.clear();
    }

    @Override public Set keySet()
    {
        return wrappedMap.keySet();
    }

    @Override public Collection values()
    {
        List result = new ArrayList();

        for (Object key : wrappedMap.keySet()) {
            result.add(get(key));
        }

        return result;
    }

    @Override public Set<Entry> entrySet()
    {
        return wrappedMap.entrySet();
    }

    /**
     * Check if a string is a reference.
     *
     * @param string a string.
     * @return true if the string is a reference.
     */
    protected static boolean isReference(String string)
    {
        return string.trim().startsWith("->");
    }

    /**
     * Parses a reference string and return the corresponding id
     *
     * @param string a string.
     * @return the reference id.
     */
    protected static String getReferenceId(String string)
    {
        return string.substring(string.lastIndexOf("->") + 2).trim();
    }

    /**
     * @return a list of all the reference ids that are present in the map.
     */
    public List<String> getDeclaredReferences()
    {
        List<String> result = new LinkedList<String>();
        for (Object value : wrappedMap.values()) {
            if (value instanceof String) {
                String string = (String) value;
                if (isReference(string)) {
                    result.add(string);
                }
            }
        }

        return result;
    }

    /**
     * @return the mapping between reference ids and the corresponding values.
     */
    public Map<String, String> getReferences()
    {
        return references;
    }

    @Override public String toString()
    {
        return wrappedMap.toString();
    }

    /**
     * Read a map with references from a YAML file
     *
     * @param file the file to be read.
     * @return the map with references.
     */
    public static MapWithReferences fromYaml(File file) throws IOException
    {
        DumperOptions opts = new DumperOptions();
        opts.setCanonical(false);
        opts.setPrettyFlow(true);
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);

        Yaml yaml = new Yaml(opts);
        FileInputStream in = new FileInputStream(file);
        Map data = (Map) yaml.load(in);
        in.close();

        return fromMap(file.getParentFile(), data);
    }

    /**
     * Convert a standard map to a map with references.
     *
     * @param baseDir the base dir for resolving references.
     * @param map the map to be converted.
     */
    protected static MapWithReferences fromMap(File baseDir, Map map) throws IOException
    {
        MapWithReferences result = new MapWithReferences();

        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof String) {
                String string = (String) value;
                if (isReference(string)) {
                    String id = getReferenceId(string);

                    result.putReference(key, id, IOUtils.toString(new FileInputStream(new File(baseDir, id))));
                } else {
                    result.put(key, value);
                }
            } else if (value instanceof Map) {
                result.put(key, fromMap(baseDir, (Map) value));
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * Write the map with references to a YAML file.
     *
     * @param file the output file.
     */
    public void toYaml(File file) throws Exception
    {
        DumperOptions opts = new DumperOptions();
        opts.setCanonical(false);
        opts.setPrettyFlow(true);
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);

        Yaml yaml = new Yaml(opts);

        FileOutputStream out = new FileOutputStream(file);
        IOUtils.write(yaml.dump(wrappedMap), out);
        out.flush();
        out.close();

        Map<String, String> references = collectReferences(this);

        File parentFile = file.getParentFile();
        for (String key : references.keySet()) {
            File outputFile = new File(parentFile, key);
            outputFile.getParentFile().mkdirs();

            out = new FileOutputStream(outputFile);
            IOUtils.write(references.get(key), out);
            out.flush();
            out.close();
        }
    }

    /**
     * Recursively traverse the map with references in order to collect all the reference ids and their corresponding
     * values.
     *
     * @param mapWithReferences the root map with references.
     * @return a mapping with all the reference ids to their corresponding values.
     */
    protected Map<String, String> collectReferences(MapWithReferences mapWithReferences)
    {
        Map<String, String> result = new LinkedHashMap<String, String>();

        result.putAll(mapWithReferences.getReferences());

        for (Object key : mapWithReferences.keySet()) {
            Object value = mapWithReferences.get(key);
            if (value instanceof MapWithReferences) {
                result.putAll(collectReferences((MapWithReferences) value));
            }
        }

        return result;
    }
}

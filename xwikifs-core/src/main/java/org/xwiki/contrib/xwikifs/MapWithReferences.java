package org.xwiki.contrib.xwikifs;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

/**
 * MapWithReferences.
 *
 * This Map implementation wraps a map that can contain "-> file" values. When such a value is found then the actual
 * value is read from the referenced file. Files references are relative to a base directory passed to the constructor.
 *
 * @version $Id$
 */
public class MapWithReferences implements Map
{
    /**
     * The base directory from where all references are looked for.
     */
    private final File baseDir;

    /**
     * The wrapped map.
     */
    protected Map map;

    /**
     * @param baseDir the base directory from where all references are looked for.
     * @param map the wrapped map.
     */
    public MapWithReferences(File baseDir, Map map)
    {
        this.map = map;
        this.baseDir = baseDir;
    }

    @Override public int size()
    {
        return map.size();
    }

    @Override public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @Override public boolean containsKey(Object key)
    {
        return map.containsKey(key);
    }

    @Override public boolean containsValue(Object value)
    {
        return map.containsValue(value);
    }

    @Override public Object get(Object key)
    {

        Object value = map.get(key);

        if (value instanceof Map) {
            /* If the value is a map, we need to wrap it in a MapWithReferences in order to make the reference mechanism
             work. */
            return new MapWithReferences(baseDir, (Map) value);
        } else if (value instanceof String) {
            /* If the value if a String, check if it is a reference and, if it is so, return the content read
            from the referenced file. */
            String string = (String) value;
            if (string.trim().startsWith("->")) {
                String reference = string.substring(string.lastIndexOf("->") + 2).trim();

                try {
                    return IOUtils.toString(new FileInputStream(new File(baseDir, reference)));
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }

        return value;
    }

    @Override public Object put(Object key, Object value)
    {
        return map.put(key, value);
    }

    @Override public Object remove(Object key)
    {
        return map.remove(key);
    }

    @Override public void putAll(Map m)
    {
        map.putAll(m);
    }

    @Override public void clear()
    {
        map.clear();
    }

    @Override public Set keySet()
    {
        return map.keySet();
    }

    @Override public Collection values()
    {
        return map.values();
    }

    @Override public Set<Entry> entrySet()
    {
        return map.entrySet();
    }
}

package org.xwiki.contrib.xwikifs.model;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.contrib.xwikifs.MapWithReferences;
import org.yaml.snakeyaml.Yaml;

/**
 * XWikiClass.
 *
 * @version $Id$
 */
public class XWikiClass
{
    private MapWithReferences data;

    protected XWikiClass()
    {
    }

    public static XWikiClass createFromFile(File target) throws Exception
    {
        if (!(target.exists() && target.isFile())) {
            throw new IllegalArgumentException(String.format("%s doesn't exist or is not a regular file", target));
        }

        XWikiClass result = new XWikiClass();

        Yaml yaml = new Yaml();
        Map yamlMap = (Map) yaml.load(new FileInputStream(target));
        result.data = new MapWithReferences(target.getParentFile(), yamlMap);

        return result;
    }

    public String getName()
    {
        return (String) data.get("name");
    }

    public Map<String, String> getData()
    {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Object key : data.keySet()) {
            if (!"properties".equals(key)) {
                Object value = data.get(key);
                if (value != null) {
                    result.put((String) key, value.toString());
                }
            }
        }

        return result;
    }

    public Map<String, Map<String, String>> getProperties()
    {
        return (Map<String, Map<String, String>>) data.get("properties");
    }

    public Map getPropertyAttributes(String property)
    {
        Map properties = (Map) data.get("properties");
        if (properties != null) {
            return (Map) properties.get(property);
        }

        return null;
    }
}

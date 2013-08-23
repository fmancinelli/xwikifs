package org.xwiki.contrib.xwikifs.model;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Set;

import org.xwiki.contrib.xwikifs.Constants;
import org.xwiki.contrib.xwikifs.MapWithReferences;
import org.yaml.snakeyaml.Yaml;

/**
 * XWikiObject.
 *
 * @version $Id$
 */
public class XWikiObject
{
    private XWikiDocument xwikiDocument;

    private XWikiClass xwikiClass;

    private String className;

    private int number;

    private MapWithReferences properties;

    protected XWikiObject()
    {
    }

    public static XWikiObject createFromFile(XWikiDocument xwikiDocument, File target) throws Exception
    {
        if (!(target.exists() && target.isFile())) {
            throw new IllegalArgumentException(String.format("%s doesn't exist or is not a regular file"));
        }

        XWikiObject result = new XWikiObject();

        result.xwikiDocument = xwikiDocument;

        String objectName = target.getName();

        /* Remove extension if there. */
        if (objectName.endsWith(Constants.OBJECT_FILE_EXTENSION)) {
            objectName = objectName.substring(0, objectName.lastIndexOf(Constants.OBJECT_FILE_EXTENSION));
        }

        String[] parts = objectName.split("-", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Filename must be in the form of Space.Class-Number");
        }

        if (!parts[0].contains(".")) {
            throw new IllegalArgumentException("Filename doesn't contain a valid Space.Class reference");
        }

        result.className = parts[0];
        result.number = Integer.parseInt(parts[1]);

        File xwikiClassFile =
                new File(new File(target.getParentFile(), Constants.CLASSINFO_DIRECTORY_NAME),
                        String.format("%s.xwc", result.className));
        if (!xwikiClassFile.exists()) {
            throw new IllegalArgumentException(
                    String.format("Class file for class %s doesn't exist at %s", result.className, xwikiClassFile));
        }

        result.xwikiClass = XWikiClass.createFromFile(xwikiClassFile);

        Yaml yaml = new Yaml();
        Map yamlMap = (Map) yaml.load(new FileInputStream(target));
        result.properties = new MapWithReferences(target.getParentFile(), yamlMap);

        /* Check that every property in the object has a corresponding description in the class file. */
        for (Object key : result.properties.keySet()) {
            if (result.xwikiClass.getPropertyAttributes((String) key) == null) {
                throw new IllegalArgumentException(
                        String.format("Property %s is not described in class %s", key, result.xwikiClass.getName()));
            }
            ;
        }

        return result;
    }

    public String getClassName()
    {
        return className;
    }

    public int getNumber()
    {
        return number;
    }

    public XWikiDocument getXWikiDocument()
    {
        return xwikiDocument;
    }

    public XWikiClass getXWikiClass()
    {
        return xwikiClass;
    }

    public Set<String> getProperties()
    {
        return (Set<String>) properties.keySet();
    }

    public String getProperty(String property)
    {
        return (String) properties.get(property);
    }
}

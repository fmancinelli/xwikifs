package org.xwiki.contrib.xwikifs.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.contrib.xwikifs.Constants;
import org.xwiki.contrib.xwikifs.MapWithReferences;
import org.yaml.snakeyaml.Yaml;

/**
 * XWikiDocument.
 *
 * @version $Id$
 */
public class XWikiDocument
{
    private String space;

    private String name;

    private MapWithReferences data;

    private XWikiClass xwikiClass;

    private List<XWikiObject> objects;

    private List<File> attachments;

    private long lastModified;

    protected XWikiDocument()
    {
    }

    public static XWikiDocument createFromDirectory(File target) throws Exception
    {
        if (!(target.exists() && target.isDirectory())) {
            throw new IllegalArgumentException(String.format("%s doesn't exist or is not a directory.", target));
        }

        /* Create a document whose id is based on the directory name */
        String directoryName = target.getName();
        String[] directoryNameParts = directoryName.split("\\.", 2);
        if (directoryNameParts.length != 2) {
            throw new IllegalArgumentException(
                    String.format("%s is not a valid XWiki Document identifier", directoryName));
        }

        XWikiDocument result = new XWikiDocument();

        result.space = directoryNameParts[0];
        result.name = directoryNameParts[1];

        if (result.space.isEmpty() || result.name.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("%s is not a valid XWiki Document identifier", directoryName));
        }

        result.lastModified = target.lastModified();

        /* Parse the content. */
        File documentFile = new File(target, Constants.DOCUMENT_FILE_NAME);
        if (documentFile.exists()) {
            result.data = MapWithReferences.fromYaml(documentFile);
        } else {
            result.data = new MapWithReferences();
        }

        /* Parse the class if it exists. */
        File classFile = new File(target, Constants.CLASS_FILE_NAME);
        if (classFile.exists() && classFile.isFile()) {
            result.xwikiClass = XWikiClass.createFromFile(classFile);
        }

        /* Find objects */
        File objectsDir = new File(target, Constants.OBJECTS_DIRECTORY_NAME);
        if (objectsDir.exists()) {
            File[] objectFiles = objectsDir.listFiles(new FilenameFilter()
            {
                @Override public boolean accept(File dir, String name)
                {
                    return name.endsWith(Constants.OBJECT_FILE_EXTENSION);
                }
            });

            result.objects = new ArrayList<XWikiObject>();
            for (File objectFile : objectFiles) {
                result.objects.add(XWikiObject.createFromFile(result, objectFile));
            }
        }

        /* Find attachments */
        File attachmentsDir = new File(target, Constants.ATTACHMENTS_DIRECTORY_NAME);
        if (attachmentsDir.exists()) {
            result.attachments = Arrays.asList(attachmentsDir.listFiles());
        }

        return result;
    }

    public String getSpace()
    {
        return space;
    }

    public String getName()
    {
        return name;
    }

    public String getData(String key)
    {
        Object value = data.get(key);

        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }

    public List<XWikiObject> getObjects()
    {
        return objects;
    }

    public XWikiClass getXWikiClass()
    {
        return xwikiClass;
    }

    public List<File> getAttachments()
    {
        return attachments;
    }

    public long getLastModified()
    {
        return lastModified;
    }
}

package org.xwiki.contrib.xwikifs;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.xwiki.contrib.xwikifs.model.XWikiClass;
import org.xwiki.contrib.xwikifs.model.XWikiDocument;
import org.xwiki.contrib.xwikifs.model.XWikiObject;

/**
 * XARUtils.
 *
 * @version $Id$
 */
public class XARUtils
{
    private static final String DEFAULT_AUTHOR = "xwiki:XWiki.Admin";

    private static final String DEFAULT_VERSION = "1.1";

    public static Document getPackageDocument(List<XWikiDocument> xwikiDocuments)
    {
        Document packageDocument = DocumentHelper.createDocument();

        Element root = packageDocument.addElement("package");
        Element infos = root.addElement("infos");
        infos.addElement("name");
        infos.addElement("description");
        infos.addElement("licence");
        infos.addElement("author");
        infos.addElement("extensionId");
        infos.addElement("version");
        infos.addElement("backupPack").addText("true");

        Element files = root.addElement("files");
        for (XWikiDocument xwikiDocument : xwikiDocuments) {
            files.addElement("file").addAttribute("language", "").addAttribute("defaultAction", "0")
                    .addText(String.format("%s.%s", xwikiDocument.getSpace(), xwikiDocument.getName()));
        }

        return packageDocument;
    }

    public static Document getXWikiDocumentXML(XWikiDocument xwikiDocument) throws Exception
    {
        return getXWikiDocumentXML(xwikiDocument, new HashMap<String, String>());
    }

    public static Document getXWikiDocumentXML(XWikiDocument xwikiDocument, Map<String, String> overrides)
            throws Exception
    {
        Document result = DocumentHelper.createDocument();

        Element root = result.addElement("xwikidoc");

        root.addElement("web").addText(xwikiDocument.getSpace());
        root.addElement("name").addText(xwikiDocument.getName());

        addElement(root, "language", xwikiDocument.getData("language"), overrides.get("language"));

        addElement(root, "defaultLanguage", overrides.get("defaultLanguage"), null);
        root.addElement("translations").addText("0"); //TODO: Handle translations

        addElement(root, "parent", xwikiDocument.getData("parent"), overrides.get("parent"));

        addElement(root, "creator", overrides.get("creator"), DEFAULT_AUTHOR);
        addElement(root, "author", overrides.get("author"), DEFAULT_AUTHOR);
        addElement(root, "contentAuthor", overrides.get("contentAuthor"), DEFAULT_AUTHOR);

        addElement(root, "customClass", overrides.get("customClass"), null);

        root.addElement("creationDate").addText(String.format("%d", xwikiDocument.getLastModified()));
        root.addElement("date").addText(String.format("%d", xwikiDocument.getLastModified()));
        root.addElement("contentUpdateDate").addText(String.format("%d", xwikiDocument.getLastModified()));

        root.addElement("version").addText(DEFAULT_VERSION);

        addElement(root, "title", xwikiDocument.getData("title"), overrides.get("title"));

        addElement(root, "defaultTemplate", overrides.get("defaultTemplate"), null);
        addElement(root, "validationScript", overrides.get("validationScript"), null);
        addElement(root, "comment", overrides.get("comment"), null);
        root.addElement("minorEdit").addText("false");

        addElement(root, "syntaxId", xwikiDocument.getData("syntax"), "xwiki/2.1");
        addElement(root, "hidden", xwikiDocument.getData("hidden"), "false");

        /* Add class */
        XWikiClass xwikiClass = xwikiDocument.getXWikiClass();
        if(xwikiClass != null) {
            Document xwikiClassDocument = XARUtils.getXWikiClassXML(xwikiClass);
            root.add(xwikiClassDocument.getRootElement());
        }

        /* Add objects */
        List<XWikiObject> xwikiObjects = xwikiDocument.getObjects();
        if (xwikiObjects != null) {
            for (XWikiObject xwikiObject : xwikiObjects) {
                Document objectDocument = XARUtils.getXWikiObjectXML(xwikiObject);
                root.add(objectDocument.getRootElement());
            }
        }

        /* Add attachments */
        List<File> attachments = xwikiDocument.getAttachments();
        if (attachments != null) {
            for (File attachmentFile : attachments) {
                Element attachment = root.addElement("attachment");
                attachment.addElement("filename").addText(attachmentFile.getName());
                attachment.addElement("filesize").addText(String.format("%d", attachmentFile.length()));
                attachment.addElement("author").addText("xwiki:XWiki.Admin");
                attachment.addElement("date").addText(String.format("%d", attachmentFile.lastModified()));
                attachment.addElement("version").addText("1.1");
                attachment.addElement("comment");
                attachment.addElement("content")
                        .addText(Base64.encodeBase64String(IOUtils.toByteArray(new FileInputStream(attachmentFile))));
            }
        }

        addElement(root, "content", xwikiDocument.getData("content"), "");

        return result;
    }

    private static Document getXWikiObjectXML(XWikiObject xwikiObject)
    {
        Document result = DocumentHelper.createDocument();

        Element root = result.addElement("object");

        XWikiClass xwikiClass = xwikiObject.getXWikiClass();
        if (xwikiClass != null) {
            root.add(getXWikiClassXML(xwikiClass).getRootElement());
        }

        root.addElement("name").addText(
                String.format("%s.%s", xwikiObject.getXWikiDocument().getSpace(),
                        xwikiObject.getXWikiDocument().getName()));
        root.addElement("className").addText(xwikiObject.getClassName());
        root.addElement("number").addText(String.format("%d", xwikiObject.getNumber()));

        addElement(root, "guid", xwikiObject.getProperty("guid"), UUID.randomUUID().toString());

        for (String property : xwikiObject.getProperties()) {
            if (!"guid".equals(property)) {
                Element propertyElement = root.addElement("property");
                propertyElement.addElement(property).addText(xwikiObject.getProperty(property).toString());
            }
        }

        return result;
    }

    private static Document getXWikiClassXML(XWikiClass xwikiClass)
    {
        Document result = DocumentHelper.createDocument();

        Element root = result.addElement("class");

        Map<String, String> data = xwikiClass.getData();

        for (Object key : data.keySet()) {
            Object value = data.get(key);
            if (value != null) {
                root.addElement(key.toString()).addText(data.get(key).toString());
            } else {
                root.addElement(key.toString());
            }
        }

        Map<String, Map<String, String>> properties = xwikiClass.getProperties();
        for (String key : properties.keySet()) {
            Element propertyElement = root.addElement(key);
            Map<String, String> propertyAttributes = properties.get(key);
            for (String attribute : propertyAttributes.keySet()) {
                Object value = propertyAttributes.get(attribute);
                if (value != null) {
                    propertyElement.addElement(attribute).addText(value.toString());
                } else {
                    propertyElement.addElement(attribute);
                }
            }
        }

        return result;
    }

    /**
     * Add an XML element with text to a parent.
     *
     * @param parent the parent XML element.
     * @param name the XML element name.
     * @param text the text of the XML element (can be null).
     * @param defaultText the default text (can be null)
     */
    protected static void addElement(Element parent, String name, String text, String defaultText)
    {
        if (text != null) {
            parent.addElement(name).addText(text);
        } else {
            if (defaultText != null) {
                parent.addElement(name).addText(defaultText);
            } else {
                parent.addElement(name);
            }
        }
    }
}

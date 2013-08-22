package org.xwiki.contrib.xwikifs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.xwiki.contrib.xwikifs.model.XWikiDocument;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * XWikiFS.
 *
 * @version $Id$
 */
public class XWikiFS
{
    protected static final Logger logger = Logger.getLogger(XWikiFS.class.getName());

    private final File target;

    public XWikiFS(File target)
    {
        if (!(target.exists() && target.isDirectory())) {
            throw new IllegalArgumentException(String.format("%s doesn't exist or is not a directory."));
        }

        this.target = target;

        logger.info(String.format("XWikiFS initialized at %s", target));
    }

    public void reformat() throws Exception
    {
        reformat(target);
    }

    private void reformat(File root) throws Exception
    {
        for (File file : root.listFiles()) {
            if (file.isDirectory()) {
                reformat(file);
            } else {
                String fileName = file.getName();
                if (fileName.endsWith(Constants.DOCUMENT_FILE_EXTENSION) ||
                        fileName.endsWith(Constants.OBJECT_FILE_EXTENSION) ||
                        fileName.endsWith(Constants.CLASS_FILE_EXTENSION))
                {
                    logger.info(String.format("Reformatting %s", file));
                    reformatYaml(file);
                }
            }
        }
    }

    private void reformatYaml(File file) throws Exception
    {
        DumperOptions opts = new DumperOptions();
        opts.setCanonical(false);
        opts.setPrettyFlow(true);
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);

        Yaml yaml = new Yaml(opts);
        FileInputStream in = new FileInputStream(file);
        Object data = yaml.load(in);
        in.close();

        FileOutputStream out = new FileOutputStream(file);
        IOUtils.write(yaml.dump(data), out);
        out.flush();
        out.close();
    }

    public void writeXAR(OutputStream os) throws Exception
    {
        List<XWikiDocument> xwikiDocuments = new ArrayList<XWikiDocument>();
        File[] documentDirectories = target.listFiles(new FilenameFilter()
        {
            @Override public boolean accept(File dir, String name)
            {
                File file = new File(dir, name);
                return file.isDirectory() && name.contains(".");
            }
        });

        for (File documentDirectory : documentDirectories) {
            xwikiDocuments.add(XWikiDocument.createFromDirectory(documentDirectory));
        }

        logger.info("Building XAR...");

        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(os);

            OutputFormat outputFormat = OutputFormat.createPrettyPrint();
            outputFormat.setExpandEmptyElements(true);
            XMLWriter xmlWriter = new XMLWriter(zos, outputFormat);

            for (XWikiDocument xwikiDocument : xwikiDocuments) {
                ZipEntry entry =
                        new ZipEntry(String.format("%s/%s.xml", xwikiDocument.getSpace(), xwikiDocument.getName()));
                zos.putNextEntry(entry);

                xmlWriter.write(XARUtils.getXWikiDocumentXML(xwikiDocument));
                zos.closeEntry();

                logger.info(String.format("Added %s.%s", xwikiDocument.getSpace(), xwikiDocument.getName()));
            }

            ZipEntry entry = new ZipEntry("package.xml");
            zos.putNextEntry(entry);
            xmlWriter.write(XARUtils.getPackageDocument(xwikiDocuments));
            zos.closeEntry();

            logger.info("Added package.xml");
        } finally {
            if (zos != null) {
                zos.flush();
                zos.close();
            }
        }
    }
}

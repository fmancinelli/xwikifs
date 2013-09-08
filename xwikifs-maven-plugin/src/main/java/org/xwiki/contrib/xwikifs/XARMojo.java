package org.xwiki.contrib.xwikifs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * XAR Mojo
 */
@Mojo(name = "xar", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class XARMojo extends AbstractMojo
{
    @Component
    protected MavenProject project;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    protected File outputDirectory;

    public void execute() throws MojoExecutionException
    {
        Log log = getLog();
        File resourcesDirectory = getResourcesDirectory();

        XWikiFS xwikiFS = new XWikiFS(resourcesDirectory);

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        File outputFile = new File(outputDirectory,
                String.format("%s.xar", project.getArtifactId()));

        log.info(String.format("Writing XAR to %s...", outputFile));

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(outputFile);
            xwikiFS.writeXAR(out);
        } catch (Exception e) {
            logStackTrace(e, log);
            throw new MojoExecutionException("Error while writing XAR", e);
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    log.error("Unable to close outputstream", e);
                }
            }
        }

        project.getArtifact().setFile(outputFile);
    }

    protected File getResourcesDirectory()
    {
        return new File(String.format("%s/src/main/resources", project.getBasedir().getAbsolutePath())
                .replace("/", File.separator));
    }

    protected void logStackTrace(Throwable t, Log log)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        log.error(sw.toString());
    }
}

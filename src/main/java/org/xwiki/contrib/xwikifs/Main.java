package org.xwiki.contrib.xwikifs;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Hello world!
 */
public class Main
{
    private static final String ROOT_OPTION_NAME = "root";

    private static final String XAR_DEFAULT_NAME = "output.xar";

    private static final String XAR_ACTION = "xar";

    private static final String REFORMAT_ACTION = "reformat";

    private static Options options;

    public static void main(String[] args) throws Exception
    {
        options = new Options();
        options.addOption(new Option(ROOT_OPTION_NAME, true, "XWikiFS root"));

        CommandLineParser commandLineParser = new BasicParser();
        CommandLine commandLine = commandLineParser.parse(options, args);

        File root = new File(System.getProperty("user.dir"));
        if (commandLine.hasOption(ROOT_OPTION_NAME)) {
            root = new File(commandLine.getOptionValue(ROOT_OPTION_NAME));
        }


        if (commandLine.getArgs().length == 0) {
            printHelp();
        } else {
            XWikiFS xwikiFS = new XWikiFS(root);

            if (XAR_ACTION.equals(commandLine.getArgs()[0])) {
                File xarFile = new File(root, XAR_DEFAULT_NAME);
                if (commandLine.getArgs().length == 2) {
                    xarFile = new File(commandLine.getArgs()[1]);
                }

                xwikiFS.writeXAR(new FileOutputStream(xarFile));
            } else if (REFORMAT_ACTION.equals(commandLine.getArgs()[0])) {
                xwikiFS.reformat();
            } else {
                printHelp();
            }
        }
    }

    private static void printHelp()
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("xwikifs [options...] action [params...]", null, options,
                String.format("\nActions:\n%x [target]\n%s", XAR_ACTION, REFORMAT_ACTION));
    }
}

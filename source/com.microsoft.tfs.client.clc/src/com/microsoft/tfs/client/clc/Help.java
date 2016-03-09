// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import java.io.File;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;

import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.UnknownCommandException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.commands.ScriptCommand;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.product.CoreVersionInfo;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.util.Check;

/**
 *         Provides help for commands.
 */
public abstract class Help {
    private static int _overallIndent = 2;

    private static CommandsMap commandsMap;
    private static OptionsMap optionsMap;

    /**
     * The Help class is a static utility class, but it needs to know which
     * commands and options are in use (since VC and WIT use different sets of
     * commands and options). This static init method must be called before any
     * other methods on Help are called.
     *
     * @param commandsMap
     *        the commands map being used
     * @param optionsMap
     *        the options map being used
     */
    public static void init(final CommandsMap commandsMap, final OptionsMap optionsMap) {
        Check.notNull(commandsMap, "commandsMap"); //$NON-NLS-1$
        Check.notNull(optionsMap, "optionsMap"); //$NON-NLS-1$

        Help.commandsMap = commandsMap;
        Help.optionsMap = optionsMap;
    }

    /**
     * Prints the exit status codes to the given display so the user knows the
     * exit conventions for the CLC.
     *
     * @param display
     *        the display to print the exit status codes to.
     */
    public static void showExitCodes(final Display display) {
        Check.notNull(display, "display"); //$NON-NLS-1$

        printHeader(display);

        display.printLine(Messages.getString("Help.ExitStatusCodesHeader")); //$NON-NLS-1$
        display.printLine(""); //$NON-NLS-1$

        printExitCode(display, ExitCode.SUCCESS, Messages.getString("Help.ExitCodeSuccess")); //$NON-NLS-1$
        printExitCode(display, ExitCode.PARTIAL_SUCCESS, Messages.getString("Help.ExitCodePartialSuccess")); //$NON-NLS-1$
        printExitCode(display, ExitCode.UNRECOGNIZED_COMMAND, Messages.getString("Help.ExitCodeUnrecognizedCommand")); //$NON-NLS-1$
        printExitCode(display, ExitCode.NOT_ATTEMPTED, Messages.getString("Help.ExitCodeNotAttempted")); //$NON-NLS-1$
        printExitCode(display, ExitCode.FAILURE, Messages.getString("Help.ExitCodeFailure")); //$NON-NLS-1$
    }

    private static void printExitCode(final Display display, final int code, final String description) {
        /*
         * Only show first three digits of exit status code, which is OK because
         * it's a signed byte on most operating systems, and we'll never use
         * that many values.
         */
        String numberPart = Integer.toString(code) + "   "; //$NON-NLS-1$
        numberPart = numberPart.substring(0, 3);
        display.printLine("    " + numberPart + " " + description); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Show help information for the command with the given alias. If the alias
     * is null, show general usage information. This method is an alternative to
     * the method show(Command, Display) when only a command alias and not an
     * instance of a Command is available.
     *
     * @param commandAlias
     *        the alias of the command to show usage for, or null
     * @param display
     *        the display object to write help text to (not null)
     * @throws UnknownCommandException
     *         if commandAlias is not null and is not a valid alias
     */
    public static void show(final String commandAlias, final Display display) throws UnknownCommandException {
        Check.notNull(commandsMap, "commandsMap"); //$NON-NLS-1$

        Command c = null;

        if (commandAlias != null) {
            c = commandsMap.findCommand(commandAlias);
            if (c == null) {
                throw new UnknownCommandException(commandAlias);
            }
        }

        Help.show(c, display);
    }

    /**
     * Show help information for the given command. If the command is null, show
     * general usage information.
     *
     * @param display
     *        the display object to write the help text to (not null).
     * @param c
     *        the command to show help for. Pass null to show general usage
     *        information.
     */
    public static void show(final Command c, final Display display) {
        Check.notNull(display, "display"); //$NON-NLS-1$

        if (c == null) {
            showGenericHelp(display);
        } else {
            showHelpForCommand(c, display);
        }
    }

    private static void showGenericHelp(final Display display) {
        Check.notNull(commandsMap, "commandsMap"); //$NON-NLS-1$
        Check.notNull(optionsMap, "optionsMap"); //$NON-NLS-1$

        printHeader(display);
        display.printLine(Messages.getString("Help.AvailableCommandsAndTheirOptions")); //$NON-NLS-1$
        display.printLine(""); //$NON-NLS-1$

        final Map map = commandsMap.getCommandsToCanonicalNamesMap();

        final TextOutputTable table = new TextOutputTable(display.getWidth());
        table.setColumns(new Column[] {
            new Column(Messages.getString("Help.Command"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("Help.Arguments"), Sizing.EXPAND) //$NON-NLS-1$
        });
        table.setWrapColumnText(true);
        table.setHeadingsVisible(false);
        table.setOverallIndent(_overallIndent);

        for (final Iterator i = map.values().iterator(); i.hasNext();) {
            final String name = (String) i.next();
            final Command c = commandsMap.findCommand(name);
            c.setMatchedAlias(name);

            if ((c instanceof ScriptCommand) == false) {
                // Add a row to the table for each syntax string.
                final String[] syntaxStrings = c.getSyntaxStrings(optionsMap);

                for (int j = 0; j < syntaxStrings.length; j++) {
                    table.addRow(new String[] {
                        c.getCanonicalName(),
                        syntaxStrings[j]
                    });
                }
            }
        }

        // Print the table.
        table.sort(new HelpRowComparator());
        table.print(display.getPrintStream());

        display.printLine(""); //$NON-NLS-1$
        display.printLine(Messages.getString("Help.OptionsAcceptedByMostCommands")); //$NON-NLS-1$
        display.printLine(""); //$NON-NLS-1$

        final Class[] globalOptions = commandsMap.getGlobalOptions();
        final Map optionMap = optionsMap.getOptionsToCanonicalNamesMap();

        table.clearRows();

        for (int i = 0; i < globalOptions.length; i++) {
            final String canonicalName = (String) optionMap.get(globalOptions[i]);
            final Option o = optionsMap.instantiateOption(canonicalName);

            if (canonicalName == null || canonicalName == null) {
                final String messageFormat = Messages.getString("Help.NoOptionsMapEntryContactMicrosoftFormat"); //$NON-NLS-1$
                final String message =
                    MessageFormat.format(messageFormat, globalOptions[i].getName(), Application.VENDOR_NAME);

                throw new RuntimeException(message);
            }

            o.setMatchedAlias(canonicalName);
            table.addRow(new String[] {
                // Show the non-command-bound syntax string
                o.getSyntaxString()
            });
        }

        table.sort(new HelpRowComparator());
        table.print(display.getPrintStream());

        printFooter(display);
    }

    private static void showHelpForCommand(final Command c, final Display display) {
        /*
         * We use the matched alias of the command here because that's what the
         * user typed.
         */
        printHeader(display);

        // Use this table with these settings for this help run.
        final TextOutputTable table = new TextOutputTable(display.getWidth());
        table.setWrapColumnText(true);
        table.setHeadingsVisible(false);
        table.setOverallIndent(_overallIndent);

        // Print general help using a table to get word-wrap.
        display.printLine(
            MessageFormat.format(Messages.getString("Help.CommandNameHeaderFormat"), c.getCanonicalName())); //$NON-NLS-1$
        display.printLine(""); //$NON-NLS-1$

        table.setColumns(new Column[] {
            new Column("", Sizing.EXPAND) //$NON-NLS-1$
        });

        /*
         * Add all the free-form help paragraphs.
         */
        final String[] paragraphs = c.getCommandHelpText();
        if (paragraphs != null) {
            for (int i = 0; i < paragraphs.length; i++) {
                if (i > 0) {
                    table.addRow(new String[] {
                        "" //$NON-NLS-1$
                    });
                }

                table.addRow(new String[] {
                    paragraphs[i]
                });
            }
        }

        if (c instanceof ScriptCommand) {
            table.addRow(new String[] {
                "" //$NON-NLS-1$
            });
            table.addRow(new String[] {
                Messages.getString("Help.NoteThisCommandProvidedForScripts") //$NON-NLS-1$
            });
        }

        table.print(display.getPrintStream());
        display.printLine(""); //$NON-NLS-1$

        /*
         * Aliases.
         */

        final String[] aliases = commandsMap.getAliasesForCommand(c);

        if (aliases.length > 1) {
            final StringBuffer sb = new StringBuffer();
            for (int i = 0; i < aliases.length; i++) {
                if (i > 0) {
                    sb.append(" "); //$NON-NLS-1$
                }
                sb.append(aliases[i]);
            }

            display.printLine(MessageFormat.format(Messages.getString("Help.CommandAliasesFormat"), sb.toString())); //$NON-NLS-1$
            display.printLine(""); //$NON-NLS-1$
        }

        /*
         * Option sets.
         */

        display.printLine(Messages.getString("Help.ValidOptionSets")); //$NON-NLS-1$
        display.printLine(""); //$NON-NLS-1$

        table.clear();
        table.setColumns(new Column[] {
            new Column("", Sizing.TIGHT), //$NON-NLS-1$
            new Column("", Sizing.EXPAND) //$NON-NLS-1$
        });

        // Add a row to the table for each syntax string.
        final String[] syntaxStrings = c.getSyntaxStrings(optionsMap);
        for (int j = 0; j < syntaxStrings.length; j++) {
            table.addRow(new String[] {
                c.getCanonicalName(),
                syntaxStrings[j]
            });
        }

        // Print the table.
        table.sort(new HelpRowComparator());
        table.print(display.getPrintStream());

        display.printLine(""); //$NON-NLS-1$
        /*
         * No footer, because it just explains how to use this syntax.
         */
    }

    private static void printHeader(final Display display) {
        display.printLine(MessageFormat.format(
            Messages.getString("Help.ProductNameAndVersionHeaderFormat"), //$NON-NLS-1$
            ProductInformation.getCurrent().getFamilyShortName(),
            ProductInformation.getCurrent().getProductFullName(),
            CoreVersionInfo.getMajorVersion(),
            CoreVersionInfo.getMinorVersion(),
            CoreVersionInfo.getServiceVersion(),
            CoreVersionInfo.getBuildVersion()));

        display.printLine(""); //$NON-NLS-1$

        // PREVIEW NOTICE
        // display.printLine("This preview release is for evaluation purposes
        // only.");
        // display.printLine("It should not be used in a production
        // environment.");
        // display.printLine("Microsoft is not liable for any damages arising
        // from the
        // use");
        // display.printLine("of this product preview.");
        // display.printLine("This preview will expire on April 30, 2007.");
        // display.printLine("");
    }

    private static void printFooter(final Display display) {
        display.printLine(""); //$NON-NLS-1$
        String optionsString = ""; //$NON-NLS-1$
        for (int i = 0; i < OptionsMap.getSupportedOptionPrefixes().length; i++) {
            optionsString = optionsString + OptionsMap.getSupportedOptionPrefixes()[i] + " "; //$NON-NLS-1$
        }

        display.printLine(
            MessageFormat.format(Messages.getString("Help.OptionsMayBeStartedWithFormat"), optionsString)); //$NON-NLS-1$

        display.printLine(""); //$NON-NLS-1$

        display.printLine(MessageFormat.format(
            Messages.getString("Help.ForGeneralHelpFormat"), //$NON-NLS-1$
            File.separator));

        display.printLine(Messages.getString("Help.ForSpecificHelpLine1")); //$NON-NLS-1$
        display.printLine(Messages.getString("Help.ForSpecificHelpLine2")); //$NON-NLS-1$
    }
}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.externaltools.internal.PlistHandler;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolException;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolValidator;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * <p>
 * Describes an external program and its required arguments. On Mac OS X,
 * automatically resolves commands that are application bundles to the correct
 * executable program.
 * </p>
 * <p>
 * The argument strings given to the constructors should contain some
 * substitution strings, like "%1", "%2", etc. Derived classes declare which
 * substitutions are forbidden and required for their kind of tool. No
 * validation is done during construction. See {@link ExternalToolValidator}.
 * </p>
 * <p>
 * The command and argument strings are always compared case-sensitive (for
 * {@link #equals(Object)}, etc.) even if the current platform doesn't work that
 * way.
 * </p>
 *
 * @see ExternalToolValidator
 * @see WindowsStyleArgumentTokenizer
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class ExternalTool {
    private static final Log log = LogFactory.getLog(ExternalTool.class);

    /**
     * The name for the memento property that holds the command and arguments.
     */
    private static final String ORIGINAL_COMMAND_AND_ARGUMENTS_MEMENTO_NAME = "commandAndArguments"; //$NON-NLS-1$

    private final String originalCommandAndArguments;

    private final String command;
    private final String[] arguments;

    /**
     * Creates an external tool that runs the given command with the given
     * arguments. Parses the command and arguments from the given string using
     * Windows style tokenizer (@see {@link WindowsStyleArgumentTokenizer}). The
     * command is the first token parsed, and every other token is a new
     * argument.
     *
     * @param commandAndArguments
     *        a string containing the command that runs this external tool and
     *        all its arguments (with special substitutions ("%1", etc.)
     *        embedded) (must not be <code>null</code> or empty)
     * @throws ExternalToolException
     *         if the arguments could not be tokenized (for example, invalid
     *         quoting)
     */
    public ExternalTool(final String commandAndArguments) throws ExternalToolException {
        Check.notNullOrEmpty(commandAndArguments, "commandAndArguments"); //$NON-NLS-1$

        originalCommandAndArguments = commandAndArguments;

        final String[] tokens = WindowsStyleArgumentTokenizer.tokenizeArguments(commandAndArguments);

        if (tokens.length == 0 || tokens[0] == null || tokens[0].length() == 0) {
            throw new ExternalToolException(
                MessageFormat.format(
                    Messages.getString("ExternalTool.CommandCouldNotBeParsedFromStringFormat"), //$NON-NLS-1$
                    commandAndArguments));
        }

        command = tokens[0];

        if (tokens.length > 1) {
            arguments = new String[tokens.length - 1];
            System.arraycopy(tokens, 1, arguments, 0, tokens.length - 1);
        } else {
            arguments = new String[0];
        }
    }

    /**
     * Gets the command (executable) portion of the external command. Does not
     * resolve app bundles (eg, on OS X) and is thus suitable for display but
     * not for execution.
     *
     * @return the user-specified path of the command to execute
     */
    public final String getOriginalCommand() {
        return command;
    }

    /**
     * Gets the command (executable) portion of the external command. Mutates
     * the command for OS X. If the command ends in ".app", we assume it to be
     * an app bundle (which is actually a directory on OS X) and append the
     * portion to the executable file.
     *
     * @return the path to the command to execute
     */
    public final String getCommand() {
        // transform for MacOS X -- we need to read the app bundle's plist in
        // order to determine the actual embedded executable name
        if (Platform.isCurrentPlatform(Platform.MAC_OS_X) && command.endsWith(".app")) //$NON-NLS-1$
        {
            final String macCommand = getMacCommand(command);

            if (macCommand != null) {
                return macCommand;
            }
        }

        return command;
    }

    /**
     * Gets the arguments as individual strings, unquoted and unescaped from the
     * original arguments given during construction.
     *
     * @return the arguments as individual strings
     */
    public final String[] getArguments() {
        final String[] ret = new String[arguments.length];
        System.arraycopy(arguments, 0, ret, 0, arguments.length);
        return ret;
    }

    /**
     * Gets the original command and arguments string, before they were
     * tokenized and unquoted.
     *
     * @return the original command and arguments string, before tokenization.
     */
    public final String getOriginalCommandAndArguments() {
        return originalCommandAndArguments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getOriginalCommandAndArguments();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o instanceof ExternalTool == false) {
            return false;
        }

        final ExternalTool other = (ExternalTool) o;

        /*
         * Case-sensitive compare of command and arguments.
         */
        return command.equals(other.command) == false && Arrays.equals(arguments, other.arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + command.hashCode();
        result = result * 37 + Arrays.hashCode(arguments);

        return result;
    }

    /**
     * Saves this tool's state to the given {@link Memento}, which should have a
     * name (of the caller's choice) but no other data.
     *
     * @param memento
     *        the {@link Memento} to save this tool's state to (must not be
     *        <code>null</code>)
     */
    public void saveToMemento(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        memento.putString(ORIGINAL_COMMAND_AND_ARGUMENTS_MEMENTO_NAME, originalCommandAndArguments);
    }

    /**
     * Loads tool state from the given {@link Memento}, which can have any name.
     *
     * @param memento
     *        the {@link Memento} to load state from (must not be
     *        <code>null</code>)
     */
    public static ExternalTool loadFromMemento(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        final String commandAndArguments = memento.getString(ORIGINAL_COMMAND_AND_ARGUMENTS_MEMENTO_NAME);

        if (commandAndArguments == null) {
            return null;
        }

        return new ExternalTool(commandAndArguments);
    }

    /**
     * <p>
     * Given a path to an application bundle (ie, Mac ".app" directory),
     * determine the path to the bundled executable. For example, given
     * /Applications/Camino.app, return
     * /Applications/Camino.app/Contents/MacOS/Camino.
     * </p>
     *
     * @param appBundle
     *        path to the Mac .app bundle (must not be <code>null</code>)
     * @return the fully-qualified executable suitable for exec(), or null if
     *         the path is not a valid mac application bundle
     */
    private static String getMacCommand(final String appBundle) {
        Check.notNull(appBundle, "appBundle"); //$NON-NLS-1$

        Map plistDict;
        final String plistPath = appBundle + "/Contents/Info.plist"; //$NON-NLS-1$

        final File plistFile = new File(plistPath);
        if (!plistFile.exists() || !plistFile.canRead()) {
            return null;
        }

        try {
            final FileInputStream plistStream = new FileInputStream(plistFile);

            final SAXParser plistParser = SAXParserFactory.newInstance().newSAXParser();
            final PlistHandler plistHandler = new PlistHandler();
            plistParser.parse(plistStream, plistHandler);

            final Object plist = plistHandler.getPlist();

            if (!(plist instanceof Map)) {
                log.error(MessageFormat.format("Plist {0} does not contain dict", plistPath)); //$NON-NLS-1$
                return null;
            }

            plistDict = (Map) plist;
        } catch (final IOException e) {
            log.error(MessageFormat.format("Could not read plist {0}", plistPath), e); //$NON-NLS-1$
            return null;
        } catch (final ParserConfigurationException e) {
            log.error(MessageFormat.format("Could not parse plist {0}", plistPath), e); //$NON-NLS-1$
            return null;
        } catch (final SAXException e) {
            log.error(MessageFormat.format("Could not parse plist {0}", plistPath), e); //$NON-NLS-1$
            return null;
        }

        final Object executable = plistDict.get("CFBundleExecutable"); //$NON-NLS-1$
        if (executable == null || !(executable instanceof String)) {
            log.error(MessageFormat.format("Plist {0} contains no string entry for CFBundleExecutable", plistPath)); //$NON-NLS-1$
            return null;
        }

        return appBundle + "/Contents/MacOS/" + (String) executable; //$NON-NLS-1$
    }
}

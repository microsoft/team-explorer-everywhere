// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException.LicenseExceptionType;
import com.microsoft.tfs.client.clc.exceptions.UnknownCommandException;
import com.microsoft.tfs.client.clc.exceptions.UnknownOptionException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.options.shared.OptionContinueOnError;
import com.microsoft.tfs.client.clc.options.shared.OptionExitCode;
import com.microsoft.tfs.client.clc.options.shared.OptionHelp;
import com.microsoft.tfs.client.clc.options.shared.OptionOutputSeparator;
import com.microsoft.tfs.client.clc.telemetry.CLCTelemetryHelper;
import com.microsoft.tfs.console.application.AbstractConsoleApplication;
import com.microsoft.tfs.console.display.ConsoleDisplay;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.console.input.ConsoleInput;
import com.microsoft.tfs.console.input.Input;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.exceptions.TFSFederatedAuthException;
import com.microsoft.tfs.core.httpclient.auth.AuthenticationSecurityException;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.core.product.ProductName;
import com.microsoft.tfs.core.telemetry.TfsTelemetryHelper;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringHelpers;
import com.microsoft.tfs.util.tasks.CanceledException;

/**
 *         The entry point for the command-line client. Instances of this class
 *         should not be used by multiple threads.
 */
public abstract class Application implements AbstractConsoleApplication {
    private static final Log log = LogFactory.getLog(Application.class);

    public static final String VENDOR_NAME = Messages.getString("Application.VendorName"); //$NON-NLS-1$

    private Display display = new ConsoleDisplay(false);
    private Input input = new ConsoleInput();

    private OptionsMap optionsMap;
    private CommandsMap commandsMap;

    /**
     * Called by the base Application class to create an OptionsMap. This method
     * will be called once and the result will be cached by the base Application
     * class.
     *
     * @return the OptionsMap specific for this client
     */
    protected abstract OptionsMap createOptionsMap();

    /**
     * Called by the base Application class to create a CommandsMap. This method
     * will be called once and the result will be cached by the base Application
     * class.
     *
     * @return the CommandsMap specific for this client
     */
    protected abstract CommandsMap createCommandsMap();

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.console.application.AbstractConsoleApplication#
     * getDisplay ()
     */
    @Override
    public Display getDisplay() {
        return display;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.console.application.AbstractConsoleApplication#
     * setDisplay (com.microsoft.tfs.console.Display)
     */
    @Override
    public void setDisplay(final Display display) {
        Check.notNull(display, "display"); //$NON-NLS-1$
        this.display = display;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.console.application.AbstractConsoleApplication#getInput
     * ()
     */
    @Override
    public Input getInput() {
        return input;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.console.application.AbstractConsoleApplication#setInput
     * (com.microsoft.tfs.console.input.Input)
     */
    @Override
    public void setInput(final Input input) {
        Check.notNull(input, "input"); //$NON-NLS-1$
        this.input = input;
    }

    /**
     * Does almost all of the work of the command-line client. This method is
     * invoked from the static main() method, and also recursively when a
     * command file is encountered.
     *
     *
     * @param args
     *        the command-line arguments as passed into the process by the Java
     *        virtual machine (or in that style, but parsed from a command
     *        file).
     * @return the status code to exit the process with.
     */
    @Override
    public int run(final String[] args) {
        /*
         * Set up the running product configuration.
         */
        ProductInformation.initialize(ProductName.CLC);

        TfsTelemetryHelper.sendSessionBegins();
        final int ret = run(args, false);
        TfsTelemetryHelper.sendSessionEnds();

        return ret;
    }

    /**
     * Does almost all of the work of the command-line client. This method is
     * invoked from the static main() method, and also recursively when a
     * command file is encountered.
     *
     * @param args
     *        the command-line arguments as passed into the process by the Java
     *        virtual machine (or in that style, but parsed from a command
     *        file).
     * @param recursiveCall
     *        true if this method was called recursively from itself, false
     *        otherwise.
     * @return the status code to exit the process with.
     */
    private int run(final String[] args, final boolean recursiveCall) {
        log.debug("Entering CLC application"); //$NON-NLS-1$
        log.debug("Command line: "); //$NON-NLS-1$
        for (int i = 0; i < args.length; i++) {
            final int p = args[i].toLowerCase().indexOf("login:"); //$NON-NLS-1$
            if (p < 0) {
                log.debug("     args[" + i + "]: " + args[i]); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                log.debug("     args[" + i + "]: " + args[i].substring(0, p + 6) + "*******"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

        }
        /*
         * create and cache the options map and commands map we also set the
         * maps on the static Help class so the Help class always has access to
         * them
         */
        optionsMap = createOptionsMap();
        commandsMap = createCommandsMap();
        Help.init(commandsMap, optionsMap);

        Command c = null;
        int ret = ExitCode.UNKNOWN;
        boolean printExitCode = false;

        try {
            final String[] tokens = args.clone();

            log.debug("Parse and prepare arguments."); //$NON-NLS-1$
            /*
             * Check to see if the first argument is a file containing commands.
             * Don't allow this if we're being called recursively.
             */
            if (recursiveCall == false && tokens.length > 0 && tokens[0].startsWith("@")) //$NON-NLS-1$
            {
                /*
                 * Search all the arguments for the "continue on error" and
                 * output separator options.
                 */
                boolean continueOnError = false;
                String outputSeparator = null;
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i] == null || tokens[i].length() == 0) {
                        continue;
                    }

                    try {
                        final Option o = optionsMap.findOption(tokens[i]);

                        if (o instanceof OptionContinueOnError) {
                            continueOnError = true;
                        } else if (o instanceof OptionOutputSeparator) {
                            outputSeparator = ((OptionOutputSeparator) o).getValue();
                        }
                    } catch (final Exception e) {
                        // Ignore.
                    }
                }

                try {
                    ret = runCommandFile(tokens, continueOnError, outputSeparator);
                } catch (final FileNotFoundException e) {
                    final String messageFormat = Messages.getString("Application.CommandFileCoundNotBeFoundFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, tokens[0].substring(1));
                    display.printErrorLine(message);
                } catch (final IOException e) {
                    final String messageFormat = Messages.getString("Application.ErrorReadingFromCommandFileFormat"); //$NON-NLS-1$
                    final String message =
                        MessageFormat.format(messageFormat, tokens[0].substring(1), e.getLocalizedMessage());
                    log.warn(message, e);
                    display.printErrorLine(message);
                }

                return ret;
            }

            final ArrayList<Option> options = new ArrayList<Option>();
            final ArrayList<String> freeArguments = new ArrayList<String>();

            /*
             * Parse all the args into a command, its options, and free
             * arguments.
             */
            c = parseTokens(args, options, freeArguments);

            /*
             * Set the display on the command as soon as possible, so it can
             * write errors/messages.
             */
            if (c != null) {
                c.setInput(input);
                c.setDisplay(display);
            }

            /*
             * Search for the help option anywhere in the command line.
             * Microsoft's client does this for user convenience. Also look for
             * the exit code option while we're searching.
             */
            boolean foundHelpOption = false;
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i) instanceof OptionHelp) {
                    foundHelpOption = true;
                }

                if (options.get(i) instanceof OptionExitCode) {
                    printExitCode = true;
                }
            }

            if (tokens.length == 0 || c == null || foundHelpOption) {
                Help.show(c, display);

                /*
                 * Always exit with success to match Microsoft's CLC.
                 */
                return ExitCode.SUCCESS;
            }

            c.setOptions(options.toArray(new Option[0]), commandsMap.getGlobalOptions());
            c.setFreeArguments(freeArguments.toArray(new String[0]));

            log.debug("Execute the command implementation."); //$NON-NLS-1$

            c.run();

            log.debug("Close the command: Flush any remaining notifications and remove the manager"); //$NON-NLS-1$
            c.close();

            ret = c.getExitCode();
        } catch (final CanceledException e) {
            getDisplay().printErrorLine(""); //$NON-NLS-1$
            getDisplay().printErrorLine(Messages.getString("Application.CommandCanceled")); //$NON-NLS-1$

            ret = ExitCode.FAILURE;
        } catch (final InputValidationException e) {
            final String messageFormat = Messages.getString("Application.AnInputValidationErrorOccurredFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
            display.printErrorLine(message);

            ret = ExitCode.FAILURE;
        } catch (final ArgumentException e) {
            /*
             * Argument exceptions happen when the user supplies an incorrect
             * command, misspelled options, the wrong option values, is missing
             * an option, or other similar error. We should show the help to the
             * user in this case.
             */
            final String messageFormat = Messages.getString("Application.AnArgumentErrorOccurredFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
            display.printErrorLine(message);

            ret = ExitCode.FAILURE;
        } catch (final IllegalArgumentException e) {
            /*
             * Same as above but returned by some low level Core or Common
             * classes, e.g. HttpHost.
             */
            TfsTelemetryHelper.sendException(e);

            final String messageFormat = Messages.getString("Application.AnArgumentErrorOccurredFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
            display.printErrorLine(message);

            ret = ExitCode.FAILURE;
        } catch (final CLCException e) {
            /*
             * CLCExceptions have messages that are meaningful to users.
             */
            final String messageFormat = Messages.getString("Application.AClientErrorOccurredFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
            log.error(message, e);
            display.printErrorLine(message);

            ret = ExitCode.FAILURE;
        } catch (final MalformedURLException e) {
            final String messageFormat = Messages.getString("Application.StringCouldNotBeConvertedToURLFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
            log.info(message, e);
            display.printErrorLine(message);
            ret = ExitCode.FAILURE;
        } catch (final LicenseException e) {
            final String messageFormat = Messages.getString("Application.LicenseErrorFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
            log.error(message, e);
            display.printErrorLine(message);

            if (e.getType() == LicenseExceptionType.EULA) {
                display.printErrorLine(Messages.getString("Application.RunTfEulaToAccept")); //$NON-NLS-1$
            } else {
                display.printErrorLine(Messages.getString("Application.RunTfProductkeyToInstall")); //$NON-NLS-1$
            }

            ret = ExitCode.FAILURE;
        } catch (final AuthenticationSecurityException e) {
            /*
             * Thrown when using insecure credentials over an insecure channel.
             */
            log.error(e);
            display.printErrorLine(e.getLocalizedMessage());
            ret = ExitCode.FAILURE;
        } catch (final TFSFederatedAuthException e) {
            /*
             * FederatedAuthenticationException is thrown when
             * DefaultFederatedAuthenticationHandler decided not to try to auth,
             * which only happens because the username and/or password weren't
             * available.
             */
            final String message = Messages.getString("Command.FedAuthRequiresUsernamePassword"); //$NON-NLS-1$
            log.error(message, e);
            display.printErrorLine(message);
            ret = ExitCode.FAILURE;
        } catch (final ProxyException e) {
            final String message = MessageFormat.format(
                Messages.getString("Application.ProblemContactingServerFormat"), //$NON-NLS-1$
                e.getLocalizedMessage());
            log.error(message, e);
            display.printErrorLine(message);
            ret = ExitCode.FAILURE;
        } catch (final TECoreException e) {
            /*
             * The most basic core exception class. All lower level (SOAP)
             * exceptions are wrapped in these.
             */
            TfsTelemetryHelper.sendException(e);

            final String messageFormat = Messages.getString("Application.AnErrorOccurredFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
            log.error(message, e);
            display.printErrorLine(message);
            ret = ExitCode.FAILURE;
        } catch (final Throwable e) {
            TfsTelemetryHelper.sendException(new Exception("Unexpected exception.", e)); //$NON-NLS-1$

            log.error("Unexpected exception: ", e); //$NON-NLS-1$
        }

        // If the exit code never got set, set it to 0.
        if (ret == ExitCode.UNKNOWN) {
            ret = ExitCode.SUCCESS;
        }

        if (printExitCode) {
            final String messageFormat = Messages.getString("Application.ExitCodeFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(ret));
            display.printLine(message);
            display.printLine(""); //$NON-NLS-1$
        }

        CLCTelemetryHelper.sendCommandFinishedEvent(c, ret);
        log.debug("Leaving CLC application"); //$NON-NLS-1$

        return ret;
    }

    /**
     * Parse the tokens into a command, any options, and free arguments. If the
     * command is not present (or is not recognized), returns null.
     *
     * @param tokens
     *        the tokens to parse (command-line arguments, not including the
     *        program being exeucted). Not null.
     * @param foundOptions
     *        an allocated ArrayList the found options will be stored in.
     * @param foundFreeArguments
     *        an allocated ArrayList the found free arguments will be stored in.
     * @return the command that was parsed from the tokens, null if none was
     *         found or recognized.
     * @throws UnknownOptionException
     *         if an unknown option is encountered.
     * @throws UnknownCommandException
     *         if an unknown command is encountered.
     * @throws InvalidOptionValueException
     *         if an invalid value was passed to an option.
     */
    private Command parseTokens(
        final String[] tokens,
        final ArrayList<Option> foundOptions,
        final ArrayList<String> foundFreeArguments)
            throws UnknownOptionException,
                UnknownCommandException,
                InvalidOptionValueException {
        Command c = null;

        for (int i = 0; i < tokens.length; i++) {
            final String token = tokens[i];

            if (token == null || token.length() == 0) {
                continue;
            }

            /*
             * If this token looks like an option...
             */
            boolean startsWithOptionCharacter = false;
            if (token.length() >= 2) {
                for (int j = 0; j < OptionsMap.getSupportedOptionPrefixes().length; j++) {
                    if (token.charAt(0) == OptionsMap.getSupportedOptionPrefixes()[j]) {
                        startsWithOptionCharacter = true;
                        break;
                    }
                }
            }

            if (startsWithOptionCharacter) {
                final Option o = optionsMap.findOption(token);

                if (o == null) {
                    throw new UnknownOptionException(token);
                }

                foundOptions.add(o);
                continue;
            }

            /*
             * We didn't parse it as an option, or command file trigger, so it
             * is the command (unless we've already found the command).
             */
            if (c == null) {
                final Command possibleCommand = commandsMap.findCommand(token);

                if (possibleCommand == null) {
                    throw new UnknownCommandException(token);
                }

                c = possibleCommand;
                continue;
            }

            /*
             * The remaining items must be free arguments.
             */
            foundFreeArguments.add(token);
        }

        return c;
    }

    /**
     * Run commands from a file (including standard input) using the extra
     * parameters as defaults for each command.
     *
     * @param commandLineArguments
     *        the arguments from the command line, the first of which must be a
     *        string that starts with the \@ symbol. If this string is 1
     *        character long (contains only the \@ character), commands are read
     *        from standard input. If the string is longer, the remaining
     *        characters compose the name of the file to read commands from.
     *        Additional array elements are substituted (positional arguments in
     *        the style "%1", "%2", etc.) in the command lines read.
     * @param continueOnError
     *        true if execution should continue after a command returns a
     *        non-success, false if execution should stop in that case
     * @param outputSeparator
     *        if non-null, this string is printed as a new line between every
     *        command run from the command file. If null, no separator line is
     *        printed between commands.
     * @throws FileNotFoundException
     *         if the command file named in the first token cannot be found.
     * @throws IOException
     *         if an error occurred reading from the command file.
     * @return the status code to exit the process with.
     */
    private int runCommandFile(
        final String[] commandLineArguments,
        final boolean continueOnError,
        final String outputSeparator) throws FileNotFoundException, IOException {
        if (commandLineArguments == null || commandLineArguments.length == 0) {
            return ExitCode.FAILURE;
        }

        InputStreamReader isr = null;
        BufferedReader br = null;

        int ret = ExitCode.UNKNOWN;

        try {
            /*
             * commandLineArguments[0] will contain the command file. Other
             * tokens are positional arguments for the lines in the command
             * file.
             *
             * If commandLineArguments[0] is simply the character "@" (no file
             * name), we should read from standard input.
             */
            if (commandLineArguments[0].equals("@")) //$NON-NLS-1$
            {
                isr = new InputStreamReader(input.getInputStream());
            } else {
                isr = new InputStreamReader(new FileInputStream(commandLineArguments[0].substring(1)));
            }

            br = new BufferedReader(isr);

            String line = null;
            while ((line = br.readLine()) != null) {
                // Break this line into tokens we'll run the usual way.
                final String[] conventionalJavaArgs = tokenizeCommandFileLine(line, commandLineArguments);

                // Null means nothing to do on this line (comment, etc.).
                if (conventionalJavaArgs == null) {
                    continue;
                }

                final int thisRet = run(conventionalJavaArgs, true);

                /*
                 * Print the output separator if the user desired it.
                 */
                if (outputSeparator != null) {
                    getDisplay().printLine(outputSeparator);
                    getDisplay().printErrorLine(outputSeparator);

                    /*
                     * Flush the streams in case they're being piped somewhere
                     * that might buffer (REXX wrapper on z/OS?).
                     */
                    getDisplay().getPrintStream().flush();
                    getDisplay().getErrorPrintStream().flush();
                }

                /*
                 * If this command didn't succeed, we might have to exit early.
                 */
                if (thisRet != ExitCode.SUCCESS) {
                    /*
                     * This wasn't successful, so stop processing if
                     * continueOnError was not set or if the command itself
                     * tells us to stop processing now ("exit" command).
                     */
                    if (continueOnError == false || thisRet == ExitCode.SUCCESS_BUT_STOP_NOW) {
                        ret = thisRet;
                        break;
                    }

                    /*
                     * Continue processing, but use partial success.
                     */
                    ret = ExitCode.PARTIAL_SUCCESS;
                } else {
                    /*
                     * Set the success code, but only if there wasn't a previous
                     * partial success.
                     */
                    if (ret != ExitCode.PARTIAL_SUCCESS) {
                        ret = ExitCode.SUCCESS;
                    }
                }
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (final IOException e) {
                }
            }
        }

        // The script stopped executing, so use the offical return value.
        if (ret == ExitCode.SUCCESS_BUT_STOP_NOW) {
            ret = ExitCode.SUCCESS;
        }

        return ret;
    }

    /**
     * Parse the arguments from a command file line. When we're invoked from a
     * shell on Unix, the shell does the argument parsing (on Windows, the JVM
     * does the parsing). When we read from a command file, we have to parse in
     * a way similar to Microsoft's parsing rules.
     *
     * From Microsoft's code: [" The only recognized "special" sequence is that
     * a pair of double quotes inside a quoted string is translated to a single
     * double quote. Other double quote characters are used to bound arguments.
     * Examples: abc -> abc; a bc -> a, bc; "a bc" -> a bc; "a""bc" -> a"bc;
     * """a""" -> "a" "]
     *
     * @param line
     *        the command line to parse (not null).
     * @param positionalArguments
     *        the original command line's arguments, which will be substituted
     *        for instances of "%1", "%2", etc. in the given line.
     * @return the parsed tokens, null if no command at all was found on this
     *         line.
     */
    private String[] tokenizeCommandFileLine(String line, final String[] positionalArguments) {
        Check.notNull(line, "line"); //$NON-NLS-1$
        Check.notNull(positionalArguments, "positionalArguments"); //$NON-NLS-1$

        // Skip blank lines and whitespace-only lines.
        if (line.trim().length() == 0) {
            return null;
        }

        // We always skip leading whitespace.
        line = StringHelpers.trimBegin(line);

        // Skip lines that begin with a comment indicator
        if (line.toLowerCase().startsWith("rem ") || line.toLowerCase().startsWith("#")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return null;
        }

        /*
         * Expand any positional parameter variables in the command line to the
         * values at that index in the positionalArguments array.
         *
         * Start with index at 1, not 0, because we don't expand %0.
         */
        for (int i = 1; i < positionalArguments.length; i++) {
            if (positionalArguments[i] != null) {
                line = replace(line, "%" + i, positionalArguments[i]); //$NON-NLS-1$
            }
        }

        final ArrayList<String> tokens = new ArrayList<String>();

        int i = 0;
        char c;
        boolean insideQuote = false;

        while (i < line.length()) {
            // Eat any whitespace.
            for (; i < line.length(); ++i) {
                if (line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                    break;
                }
            }

            if (i < line.length()) {
                // Collect the argument.
                final StringBuffer thisToken = new StringBuffer();

                for (; i < line.length(); ++i) {
                    c = line.charAt(i);
                    if (c == '"') {
                        // Two double-quotes inside quotes are smashed into one
                        // double-quote.
                        if (insideQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                            ++i;
                        } else {
                            // Flip the quote state but don't add the quote to
                            // the
                            // token.
                            insideQuote = !insideQuote;
                            continue;
                        }
                    } else if (!insideQuote && (c == ' ' || c == '\t')) {
                        // Whitespace outside of quotes is end of token.
                        break;
                    }

                    thisToken.append(c);
                }

                tokens.add(thisToken.toString());
            }
        }

        return tokens.toArray(new String[0]);
    }

    /**
     * Quick hack of a String.replace method for Java 1.4
     *
     * @param source
     *        The string you want to search
     * @param pattern
     *        The pattern you would like to match
     * @param replace
     *        The string you want to replace the pattern with
     * @return the source string with all occurances of the patterm replaced
     */
    private String replace(final String source, final String pattern, final String replace) {
        if (source != null) {
            final int len = pattern.length();
            final StringBuffer sb = new StringBuffer();

            int found = -1;
            int start = 0;

            while ((found = source.indexOf(pattern, start)) != -1) {
                sb.append(source.substring(start, found));
                sb.append(replace);
                start = found + len;
            }

            sb.append(source.substring(start));

            return sb.toString();
        }

        return ""; //$NON-NLS-1$
    }

}

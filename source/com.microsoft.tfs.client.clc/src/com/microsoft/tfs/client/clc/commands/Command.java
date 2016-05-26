// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.commands;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.activation.ActivationException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.CLCConnectionAdvisor;
import com.microsoft.tfs.client.clc.CommandsMap;
import com.microsoft.tfs.client.clc.EnvironmentVariables;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.CannotFindWorkspaceException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException.LicenseExceptionType;
import com.microsoft.tfs.client.clc.exceptions.MissingRequiredOptionException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.options.shared.OptionCollection;
import com.microsoft.tfs.client.clc.options.shared.OptionLogin;
import com.microsoft.tfs.client.clc.options.shared.OptionServer;
import com.microsoft.tfs.client.clc.prompt.Prompt;
import com.microsoft.tfs.client.clc.vc.CLCPathWatcherFactory;
import com.microsoft.tfs.client.clc.vc.Main;
import com.microsoft.tfs.client.clc.vc.MatchedFileArgument;
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionNoPrompt;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;
import com.microsoft.tfs.client.clc.xml.CommonXMLNames;
import com.microsoft.tfs.client.clc.xml.SimpleXMLWriter;
import com.microsoft.tfs.client.common.license.LicenseManager;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.console.display.NullDisplay;
import com.microsoft.tfs.console.input.Input;
import com.microsoft.tfs.console.input.NullInput;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionHelper;
import com.microsoft.tfs.core.clients.versioncontrol.events.BeforeCheckinListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.BeforeShelveListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.DestroyEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.DestroyListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.MergingEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.MergingListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.NewPendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.UndonePendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.DownloadProxyException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.NullPathWatcherFactory;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.SeverityType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.credentials.CredentialsManagerFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.JwtCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.pendingcheckin.CheckinNoteFailure;
import com.microsoft.tfs.core.util.CredentialsUtils;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.util.notifications.MessageWindowNotificationManager;
import com.microsoft.tfs.jni.NTLMEngine;
import com.microsoft.tfs.jni.NegotiateEngine;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Closable;
import com.microsoft.tfs.util.NewlineUtils;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

/**
 * The base class for other command classes in the command-line client.
 *
 * This class is NOT guaranteed thread-safe.
 */
public abstract class Command
    implements BeforeCheckinListener, CheckinListener, NonFatalErrorListener, UndonePendingChangeListener,
    NewPendingChangeListener, BeforeShelveListener, DestroyListener, ConflictListener, GetListener, MergingListener,
    ConflictResolvedListener, Closable {
    private static final int SUMMARY_THRESHOLD = 10;

    protected static final Log log = LogFactory.getLog(Command.class);
    private TFSTeamProjectCollection collection = null;

    public final TFSTeamProjectCollection getCollection() {
        return collection;
    }

    public static final DefaultPersistenceStoreProvider CLC_PERSISTENCE_PROVIDER =
        DefaultPersistenceStoreProvider.INSTANCE;

    /**
     * This command's canonical name.
     */
    private String canonicalName = ""; //$NON-NLS-1$

    /**
     * The alias under which this option was invoked.
     */
    private String alias;

    /**
     * We save this so we can reconstruct command-lines exactly as the user
     * typed them.
     */
    private String userText;

    private Option[] options;
    private String[] freeArguments;

    /**
     * The display object this command will use to relay output to the user.
     */
    private Display display = new NullDisplay();

    /**
     * The input object this command will use to read input from the user.
     */
    private Input input = new NullInput();

    /**
     * The exit code for this command. If never set with
     * {@link #setExitCode(int)} while the command runs,
     * {@link ExitCode#UNKNOWN} is converted to {@link ExitCode#SUCCESS}.
     */
    private int exitCode = ExitCode.UNKNOWN;

    /**
     * Used when printing out pending changes, so we can keep track of the last
     * directory we printed and only print file names until the directory
     * changes.
     */
    private String lastPendingChangeDirectory;

    /**
     * When non-null, XML is written to the display instead of the normal format
     * for things like pending changes.
     */
    private SimpleXMLWriter xmlWriter = null;

    /**
     * We use this to keep track of the last server or local path that was part
     * of an onGet() event, so we can print short file names.
     */
    private String lastGetDirectory = null;

    private final List<GetEvent> getWarningList = new ArrayList<GetEvent>();
    private final List<MergingEvent> mergeWarningList = new ArrayList<MergingEvent>();

    /*
     * Non-fatal events come in two flavors, warnings and errors. We need to
     * keep track of the counts separately for use in the summary output.
     */
    private final List<NonFatalErrorMessage> nonFatalMessageList = new ArrayList<NonFatalErrorMessage>();
    private int numNonFatalErrors;
    private int numNonFatalWarnings;

    /*
     * Cache connections to the servers by URI and credentials; in batch mode,
     * we can reuse these connections to avoid spinning up new ones. We maintain
     * a map of URIs to all the connections built for that URI so that we can
     * match to the credentials. We cannot simply map the credentials because
     * they can change over time, after map insertion.
     */
    private static final Map<URI, List<TFSTeamProjectCollection>> connections =
        new HashMap<URI, List<TFSTeamProjectCollection>>();

    class NonFatalErrorMessage {
        private final boolean error;
        private final String message;

        public NonFatalErrorMessage(final boolean error, final String message) {
            this.error = error;
            this.message = message;
        }

        public boolean isError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    };

    /**
     * NonFatalError events may be fired from threads other than the main thread
     * and may be fired concurrently. We use this lock to protect the list and
     * counters related to NonFatalError handling.
     */
    private final Object nonFatalErrorLock = new Object();

    /**
     * Set by {@link #initializeClient(VersionControlClient)} on platforms where
     * cross-process notifications are supported.
     */
    private MessageWindowNotificationManager notificationManager;

    /**
     * Set by {@link #initializeClient(VersionControlClient)} because
     * {@link #close()} needs it to do its work properly.
     */
    private VersionControlClient client;
    private Workstation workstation;

    /**
     * Zero argument constructor so we can create new instances of this class
     * dynamically.
     */
    public Command() {
    }

    /**
     * Execute the command. Be sure to call setOptions() and setFreeArguments()
     * (if any were supplied) or this method will probably fail.
     * <p>
     * The method may call {@link #setExitCode(int)} during its execution.
     *
     * @throws InvalidFreeArgumentException
     *         if a required argument is missing.
     */
    public abstract void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException;

    /**
     * Gets the option profiles this command supports. Options in the global
     * option set of the {@link CommandsMap} used by ths application (vc, wit,
     * etc.) do not need to be listed by extending classes.
     *
     * @return an array of option profiles that this command class supports (not
     *         including global options), an empty array if no options are
     *         supported.
     */
    public abstract AcceptedOptionSet[] getSupportedOptionSets();

    /**
     * Gets the free-form text help for this command, each string representing a
     * single paragraph (without explicit line breaks).
     *
     * @return the free form text help for this command, each string a paragraph
     *         without any line breaks.
     */
    public abstract String[] getCommandHelpText();

    /**
     * @param userText
     *        the string the user typed to invoke this command (not necessarily
     *        the canonical name). Not null or empty.
     */
    public void setUserText(final String userText) {
        Check.notNullOrEmpty(userText, "userText"); //$NON-NLS-1$
        this.userText = userText;
    }

    /**
     * @return the string the user typed to invoke this command (not necessarily
     *         the canonical name).
     */
    public final String getUserText() {
        return userText;
    }

    /**
     * @param alias
     *        the alias that was matched to create this command (not null or
     *        empty).
     */
    public final void setMatchedAlias(final String alias) {
        Check.notNullOrEmpty(alias, "alias"); //$NON-NLS-1$
        this.alias = alias;
    }

    /**
     * @return the alias that was matched to create this command.
     */
    public final String getMatchedAlias() {
        return alias;
    }

    /**
     * @param canonicalName
     *        this command's canonical name (not null or empty).
     */
    public final void setCanonicalName(final String canonicalName) {
        Check.notNullOrEmpty(canonicalName, "name"); //$NON-NLS-1$
        this.canonicalName = canonicalName;
    }

    /**
     * @return this command's canonical name.
     */
    public final String getCanonicalName() {
        return canonicalName;
    }

    /**
     * Call this method for every command you create, or the command defaults to
     * a {@link NullDisplay} which the user won't appreciate.
     *
     * @param display
     *        the display to use for all output for this command (not null).
     */
    public final void setDisplay(final Display display) {
        Check.notNull(display, "display"); //$NON-NLS-1$
        this.display = display;
    }

    /**
     * @return the display that this command uses for all output.
     */
    public final Display getDisplay() {
        return display;
    }

    /**
     * Call this method for every command you create, or the command defaults to
     * a {@link NullInput} which the user won't appreciate.
     *
     * @param input
     *        the class to use for all input for this command (not null).
     */
    public final void setInput(final Input input) {
        Check.notNull(input, "input"); //$NON-NLS-1$
        this.input = input;
    }

    /**
     * @return the class that this command uses for all input.
     */
    public final Input getInput() {
        return input;
    }

    /**
     * Sets the options that the user wants set for this command instance.
     *
     * @param setOptions
     *        the options to set for this command (not null).
     * @param validGlobalOptions
     *        the valid global options to accept for this command (not null).
     * @throws InvalidOptionException
     *         if one of the given options is not appropriate for this command.
     */
    public final void setOptions(final Option[] setOptions, final Class[] validGlobalOptions)
        throws InvalidOptionException {
        Check.notNull(setOptions, "options"); //$NON-NLS-1$
        Check.notNull(validGlobalOptions, "globalOptions"); //$NON-NLS-1$

        final ArrayList allValidOptions = new ArrayList();

        final AcceptedOptionSet[] profiles = getSupportedOptionSets();
        for (int i = 0; i < profiles.length; i++) {
            allValidOptions.addAll(Arrays.asList(profiles[i].getOptionalOptions()));
            allValidOptions.addAll(Arrays.asList(profiles[i].getRequiredOptions()));
        }

        allValidOptions.addAll(Arrays.asList(validGlobalOptions));

        /*
         * Make sure each of the set options is in the array of all valid
         * options.
         */
        for (int i = 0; i < setOptions.length; i++) {
            boolean foundInValidOptions = false;
            for (int j = 0; j < allValidOptions.size(); j++) {
                if (allValidOptions.get(j).equals(setOptions[i].getClass())) {
                    foundInValidOptions = true;
                    break;
                }
            }

            if (!foundInValidOptions) {
                final String messageFormat = Messages.getString("Command.CommandDoesNotSupportOptionFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, getMatchedAlias(), setOptions[i].toString());

                throw new InvalidOptionException(message);
            }
        }

        options = setOptions;
    }

    /**
     * @return the options that the user wants set for this command instance.
     */
    public Option[] getOptions() {
        return options;
    }

    /**
     * @param args
     *        the free arguments the user supplied for this command (not null).
     */
    public void setFreeArguments(final String[] args) {
        Check.notNull(args, "args"); //$NON-NLS-1$
        freeArguments = args;
    }

    /**
     * @return the free arguments the user supplied for this command.
     */
    public String[] getFreeArguments() {
        return freeArguments;
    }

    public int getExitCode() {
        return exitCode;
    }

    /**
     * Set the exit code for the commands. Setting the exit code multiple times
     * composes in interesting ways here. If all exitCodes match, the result is
     * the value. If the new setting is greater, keep the new one (that way if
     * 100 happens at the end, it wins). Otherwise, if different, the result is
     * {@link ExitCode#PARTIAL_SUCCESS}.
     *
     * @param exitCode
     *        Exit code to set
     */
    public void setExitCode(final int exitCode) {
        if (this.exitCode == ExitCode.UNKNOWN || exitCode > this.exitCode) {
            this.exitCode = exitCode;
        } else if (exitCode != this.exitCode) {
            this.exitCode = ExitCode.PARTIAL_SUCCESS;
        }
    }

    /**
     * Gets the subset of the free arguments that begin with the given index.
     *
     * @param firstLocalPathArgumentIndex
     *        the index of the first free argument that will be included in the
     *        returned set (must be >= 0 and <= to the last index of the free
     *        arguments).
     * @return the subset of the free arguments in the original order starting
     *         with the given index.
     */
    public String[] getLastFreeArguments(final int firstLocalPathArgumentIndex) {
        final String[] allFreeArguments = getFreeArguments();

        final List subList =
            Arrays.asList(allFreeArguments).subList(firstLocalPathArgumentIndex, allFreeArguments.length);

        return (String[]) subList.toArray(new String[subList.size()]);
    }

    /**
     * Finds all options that the user set for this command that match the given
     * class.
     *
     * @param optionClass
     *        the class to search for option types that match (not null).
     * @return the array of options the user set on this command that have the
     *         given type.
     */
    public Option[] findAllOptionTypes(final Class optionClass) {
        Check.notNull(optionClass, "optionClass"); //$NON-NLS-1$

        return findAllOptionTypes(new Class[] {
            optionClass
        });
    }

    /**
     * Finds all options that the user set for this command that match the given
     * classes
     *
     * @param optionClass
     *        the classes to search for option types that match (not null).
     * @return the array of options the user set on this command that have the
     *         given type.
     */
    public Option[] findAllOptionTypes(final Class[] optionClasses) {
        Check.notNull(optionClasses, "optionClasses"); //$NON-NLS-1$

        /* Store in a List to ensure that we get them back in the given order */
        final Set<Option> optionsSet = new HashSet<Option>();
        final List<Option> optionsList = new ArrayList<Option>();

        for (int i = 0; i < options.length; i++) {
            for (int j = 0; j < optionClasses.length; j++) {
                if (options[i].getClass().equals(optionClasses[j])) {
                    optionsSet.add(options[i]);
                    optionsList.add(options[i]);

                    break;
                }
            }
        }

        return optionsList.toArray(new Option[optionsList.size()]);
    }

    /**
     * Finds the first option that the user set for this command that matches
     * the given class.
     *
     * @param optionClass
     *        the class to search for an option type that matches (not null).
     * @return the first option the user set on this command that has the given
     *         type, or null if no matching option was found
     */
    public Option findOptionType(final Class optionClass) {
        Check.notNull(optionClass, "optionClass"); //$NON-NLS-1$

        for (int i = 0; i < options.length; i++) {
            if (options[i].getClass().equals(optionClass)) {
                return options[i];
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getMatchedAlias();
    }

    /**
     * Get the syntax helper string for each option profile supported by this
     * command. The syntax strings do NOT include this command's name.
     *
     * @param optionsMap
     *        the options map for this Command (not null).
     * @return a command-line help syntax string for each option profile
     *         supported by this command, not including the command name in the
     *         strings.
     */
    public final String[] getSyntaxStrings(final OptionsMap optionsMap) {
        Check.notNull(optionsMap, "optionsMap"); //$NON-NLS-1$

        final ArrayList<String> ret = new ArrayList<String>();

        final AcceptedOptionSet[] profiles = getSupportedOptionSets();

        for (int i = 0; i < profiles.length; i++) {
            final AcceptedOptionSet profile = profiles[i];

            if (profile == null) {
                continue;
            }

            final StringBuffer sb = new StringBuffer();
            final Class[] optionalOptions = profile.getOptionalOptions();
            final Class[] requiredOptions = profile.getRequiredOptions();

            // Add the required options first, without the brackets.
            for (int j = 0; j < requiredOptions.length; j++) {
                if (sb.length() > 0) {
                    sb.append(" "); //$NON-NLS-1$
                }

                if (requiredOptions[j] == null) {
                    final String messageFormat = "parsed null required option at index {0} , skipping"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, Integer.toString(j));
                    log.error(message);
                    continue;
                }

                final Option o = instantiateOptionForSyntaxString(requiredOptions[j], optionsMap);

                if (o != null) {
                    // Get the command-bound string first
                    String s = o.getSyntaxString(getClass());
                    if (s == null) {
                        // Fallback to the non-command-bound string
                        s = o.getSyntaxString();
                    }

                    sb.append(s);
                }
            }

            // Now do the optional options.
            for (int j = 0; j < optionalOptions.length; j++) {
                if (sb.length() > 0) {
                    sb.append(" "); //$NON-NLS-1$
                }

                if (optionalOptions[j] == null) {
                    final String messageFormat = "parsed null optional option at index {0} , skipping"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, Integer.toString(j));
                    log.error(message);
                    continue;
                }

                final Option o = instantiateOptionForSyntaxString(optionalOptions[j], optionsMap);

                if (o != null) {
                    sb.append("["); //$NON-NLS-1$
                    sb.append(o.getSyntaxString());
                    sb.append("]"); //$NON-NLS-1$
                }
            }

            // Do free arguments.
            final String freeArgumentsSyntax = profile.getFreeArgumentsSyntax();
            if (!StringUtil.isNullOrEmpty(freeArgumentsSyntax)) {
                if (sb.length() > 0) {
                    sb.append(" "); //$NON-NLS-1$
                }

                sb.append(freeArgumentsSyntax);
            }

            // Add the string buffer's contents to the return list.
            ret.add(sb.toString());
        }

        return ret.toArray(new String[ret.size()]);
    }

    /**
     * Creates an instance of an option of the given type that must exist in the
     * given options map.
     *
     * @param optionClass
     *        the class of the option type to create (not null).
     * @param optionsMap
     *        the map that contains the option (not null).
     * @return the instance of the option requested.
     */
    private Option instantiateOptionForSyntaxString(final Class optionClass, final OptionsMap optionsMap) {
        Check.notNull(optionClass, "optionClass"); //$NON-NLS-1$
        Check.notNull(optionsMap, "optionsMap"); //$NON-NLS-1$

        // Look up the canonical name of the option by the class.
        final String canonicalName = (String) optionsMap.getOptionsToCanonicalNamesMap().get(optionClass);

        if (canonicalName == null) {
            final String messageFormat = Messages.getString("Command.OptionNotRegisteredFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, optionClass.toString(), Main.VENDOR_NAME);

            log.error(message);
            throw new RuntimeException(message);
        }

        final Option option = optionsMap.instantiateOption(canonicalName);
        option.setMatchedAlias(canonicalName);

        return option;
    }

    /**
     * Finds the single locally cached workspace that contains the given list of
     * items. Only workspaces on this computer can match.
     *
     * @param items
     *        the list of items that have to belong to a single cached workspace
     *        (not null).
     * @return the workspace found that contains the all the given items.
     * @throws CannotFindWorkspaceException
     *         if the local client cache's lock file could not be created.
     *         InvalidFreeArgumentException if the specified items do not belong
     *         to a single workspace
     */
    protected final WorkspaceInfo findSingleCachedWorkspace(final QualifiedItem[] items)
        throws InvalidFreeArgumentException,
            CannotFindWorkspaceException {
        Check.notNull(items, "items"); //$NON-NLS-1$

        final String[] canonicalPaths = new String[items.length];
        int idx = 0;

        for (final QualifiedItem item : items) {
            canonicalPaths[idx++] = item.getPath();
        }

        return findSingleCachedWorkspaceImpl(canonicalPaths);
    }

    /**
     * Finds the single locally cached workspace that contains the given list of
     * paths. Only workspaces on this computer can match.
     *
     * @param paths
     *        the list of path that have to belong to a single cached workspace
     *        (not null).
     * @return the workspace found that contains the all the given paths.
     * @throws CannotFindWorkspaceException
     *         if the local client cache's lock file could not be created.
     *         InvalidFreeArgumentException if the specified paths do not belong
     *         to a single workspace
     */
    protected final WorkspaceInfo findSingleCachedWorkspace(final String[] paths)
        throws InvalidFreeArgumentException,
            CannotFindWorkspaceException {
        Check.notNull(paths, "paths"); //$NON-NLS-1$

        final String[] canonicalPaths = new String[paths.length];
        int idx = 0;

        for (final String path : paths) {
            if (ServerPath.isServerPath(path)) {
                canonicalPaths[idx++] = ServerPath.canonicalize(path);
            } else {
                canonicalPaths[idx++] = LocalPath.canonicalize(path);
            }
        }

        return findSingleCachedWorkspaceImpl(canonicalPaths);
    }

    private final WorkspaceInfo findSingleCachedWorkspaceImpl(final String[] paths)
        throws InvalidFreeArgumentException,
            CannotFindWorkspaceException {
        Check.notNull(paths, "paths"); //$NON-NLS-1$

        WorkspaceInfo singleWorkspace = null;
        WorkspaceInfo mappedWorkspaceForCurrentWorkingDirectory = null;

        for (final String path : paths) {
            Check.notNull(path, "path"); //$NON-NLS-1$

            final WorkspaceInfo mappedWorkspace;

            if (ServerPath.isServerPath(path)) {
                if (mappedWorkspaceForCurrentWorkingDirectory == null) {
                    /*
                     * See if the current directory is mapped in a locally
                     * cached workspace.
                     */
                    final File currentDirectory =
                        new File(LocalPath.canonicalize(LocalPath.getCurrentWorkingDirectory()));
                    mappedWorkspaceForCurrentWorkingDirectory =
                        findCachedWorkspaceForPath(currentDirectory.getAbsolutePath());
                }

                mappedWorkspace = mappedWorkspaceForCurrentWorkingDirectory;
            } else {
                /*
                 * See if any locally cached workspace contains this path.
                 */
                mappedWorkspace = findCachedWorkspaceForPath(path);
            }

            if (mappedWorkspace != null) {
                /*
                 * Save this workspace to a method variable to we can ensure all
                 * of the arguments the user supplied are in the same workspace.
                 */
                if (singleWorkspace == null) {
                    singleWorkspace = mappedWorkspace;
                } else if (!singleWorkspace.equals(mappedWorkspace)) {
                    throw new InvalidFreeArgumentException(
                        Messages.getString("Command.AllItemsMustResideSingleWorkspace")); //$NON-NLS-1$
                }
            } else {
                /*
                 * TODO Implement this condition. We should search all mapped
                 * workspaces for sub-folders that intersect with the recursive
                 * expansion of the item supplied by the user, and get those
                 * folders.
                 */
                throw new InvalidFreeArgumentException(
                    MessageFormat.format(
                        Messages.getString("Command.UnableToDetermineWorkspaceFormat"), //$NON-NLS-1$
                        OptionsMap.getPreferredOptionPrefix()));
            }
        }

        return singleWorkspace;
    }

    /**
     * Finds the locally cached workspace that contains the given local path.
     * Only workspaces on this computer can match. null is returned if no
     * matching workspace is found.
     *
     * @param localPath
     *        the local path (file or folder) to find in all locally cached
     *        workspaces (not null).
     * @return the first workspace found that contains the given path, null if
     *         no matching workspace is found.
     * @throws CannotFindWorkspaceException
     *         if the local client cache's lock file could not be created.
     */
    protected final WorkspaceInfo findCachedWorkspaceForPath(final String localPath)
        throws CannotFindWorkspaceException {
        if (localPath != null) {
            if (!ServerPath.isServerPath(localPath)
                && Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).isMapped(localPath)) {
                return Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).getLocalWorkspaceInfo(localPath);
            }
        }

        return null;
    }

    /**
     * Determines which cached workspace (if any) matches the workspace command
     * line option, or the free arguments, or the current directory (in that
     * order). Throws if no match is found.
     * <p>
     * All free arguments are considered local paths and can cause a cached
     * workspace that has matching working folder mappings to be returned. Call
     * {@link #determineCachedWorkspace(String[])} or
     * {@link #determineCachedWorkspace(String[], boolean)} and supply your own
     * local path arguments if you desire finer control.
     *
     * @return the cached workspace that matches the command line options or
     *         current working directory.
     * @throws CannotFindWorkspaceException
     *         if the workspace specified on the command line cannot be found in
     *         the local client cache, or if no cached workspaces map to the
     *         current working directory.
     * @throws InvalidOptionValueException
     *         if the server option was given but the option value can't be
     *         parsed as a URI.
     * @throws InvalidOptionException
     *         if the server and collection options are both specified
     */
    protected final WorkspaceInfo determineCachedWorkspace()
        throws CannotFindWorkspaceException,
            InvalidOptionValueException,
            InvalidOptionException {
        return determineCachedWorkspace(null, false);
    }

    /**
     * Determines which cached workspace (if any) matches the workspace command
     * line option, or the free arguments, or the current directory (in that
     * order). Throws if no match is found.
     *
     * @param pathFreeArguments
     *        an array of all the free arguments that are local paths that can
     *        matched against cached workspace working folder mappings to find a
     *        matching cached workspace. If null or empty, no path-based search
     *        for a cached workspace is performed.
     *
     * @return the cached workspace that matches the command line options or
     *         current working directory.
     * @throws CannotFindWorkspaceException
     *         if the workspace specified on the command line cannot be found in
     *         the local client cache, or if no cached workspaces map to the
     *         current working directory.
     * @throws InvalidOptionValueException
     *         if the server option was given but the option value can't be
     *         parsed as a URI.
     * @throws InvalidOptionException
     *         if the server and collection options are both specified
     */
    protected final WorkspaceInfo determineCachedWorkspace(final String[] pathFreeArguments)
        throws CannotFindWorkspaceException,
            InvalidOptionValueException,
            InvalidOptionException {
        return determineCachedWorkspace(pathFreeArguments, false);
    }

    /**
     * Determines which cached workspace (if any) matches the workspace command
     * line option, or the free arguments, or the current directory (in that
     * order). Throws if no match is found. Only cached workspaces that reside
     * on the current computer can match.
     *
     * @param pathFreeArguments
     *        an array of all the free arguments that are local paths that can
     *        matched against cached workspace working folder mappings to find a
     *        matching cached workspace. If null or empty, no path-based search
     *        for a cached workspace is performed.
     * @param ignoreWorkspaceOptionValue
     *        if true, the any workspace option supplied on the command-line is
     *        ignored, and only the free arguments and current directory are
     *        checked to determine the cached workspace. This is probably only
     *        useful for the CLC for commands where the /workspace option is
     *        used to set the workspace name to query on, but should not cause a
     *        cached workspace to be present for it.
     * @return the cached workspace that matches the command line options or
     *         current working directory.
     * @throws CannotFindWorkspaceException
     *         if the workspace specified on the command line cannot be found in
     *         the local client cache, or if no cached workspaces map to the
     *         current working directory.
     * @throws InvalidOptionValueException
     *         if the server option was given but the option value can't be
     *         parsed as a URI.
     * @throws InvalidOptionException
     *         if the server and collection options are both specified
     */
    protected final WorkspaceInfo determineCachedWorkspace(
        final String[] pathFreeArguments,
        final boolean ignoreWorkspaceOptionValue)
        throws CannotFindWorkspaceException,
            InvalidOptionValueException,
            InvalidOptionException {
        WorkspaceInfo cachedWorkspace = null;

        /*
         * See if the user specified it on the command line.
         */
        final OptionWorkspace optionWorkspace = (OptionWorkspace) findOptionType(OptionWorkspace.class);
        if (!ignoreWorkspaceOptionValue && optionWorkspace != null) {
            final WorkspaceSpec spec;
            try {
                spec = WorkspaceSpec.parse(optionWorkspace.getValue(), null);
            } catch (final WorkspaceSpecParseException e) {
                throw new CannotFindWorkspaceException(e.getMessage());
            }

            /*
             * The user may have specified the server option to qualify an
             * ambiguous workspace (same name, owner, domain, different server).
             */
            final URI serverURI;
            final OptionCollection collectionOption = getCollectionOption();
            if (collectionOption != null) {
                serverURI = collectionOption.getURI();
            } else {
                serverURI = null;
            }

            final WorkspaceInfo[] all = findLocalWorkspaces(serverURI, spec.getName(), spec.getOwner());

            if (all == null || all.length == 0) {
                final String messageFormat = Messages.getString("Command.WorkspaceNotFoundInCacheFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, spec.toString());
                throw new CannotFindWorkspaceException(message);
            } else if (all.length > 1 && spec.getOwner() == null) {
                final String messageFormat = Messages.getString("Command.WorkspaceNameMatchesMoreThanOneFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, spec.toString());
                throw new CannotFindWorkspaceException(message);
            } else if (all.length > 1) {
                // Workspace owner was specified, still ambiguous.
                final String messageFormat =
                    Messages.getString("Command.WorkspaceNameAndOwnerMatchesMoreThanOneFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, spec.getName(), spec.getOwner());
                throw new CannotFindWorkspaceException(message);
            }

            cachedWorkspace = all[0];
        } else {
            /*
             * No workspace option was specified, or the parameter was set so we
             * ignored it, so look at the free arguments for a local file name,
             * and try to find that mapped in some locally cached workspace. In
             * this mode of operation (no workspace option given), the CLC will
             * never hit the server to find a workspace. It must already be in
             * the cache.
             */

            /*
             * If the pathFreeArguments option is null, treat all free arguments
             * as paths.
             */
            final String[] pathsToSearch = (pathFreeArguments == null) ? getFreeArguments() : pathFreeArguments;

            for (int i = 0; i < pathsToSearch.length; i++) {
                String path = pathsToSearch[i];

                /*
                 * A path can be a version spec, which is a path with a version
                 * appended. Run the string through the version spec class to
                 * get only the file part.
                 */
                try {
                    final VersionedFileSpec spec =
                        VersionedFileSpec.parse(path, VersionControlConstants.AUTHENTICATED_USER, true);

                    if (spec.getItem() != null) {
                        path = spec.getItem();
                    }
                } catch (final VersionSpecParseException e) {
                    // Ignore.

                } catch (final LabelSpecParseException e) {
                    // Ignore.
                }

                /*
                 * Canonicalize the path so relative local paths can be used.
                 * It's possible a free argument is not a path, so we just skip
                 * those if there's not cached workspace that matched it as a
                 * path.
                 */
                path = ItemPath.canonicalize(path);

                /*
                 * We can only reliably search for mapped local paths, because a
                 * server path is likely to be mapped multiple places.
                 */
                if (!ServerPath.isServerPath(path)) {
                    cachedWorkspace = findCachedWorkspaceForPath(path);

                    if (cachedWorkspace != null) {
                        break;
                    }
                }
            }

            if (cachedWorkspace == null) {
                /*
                 * We couldn't find it using the free arguments (maybe they
                 * weren't full paths). Try the current directory.
                 */
                final File currentDirectory = new File(LocalPath.getCurrentWorkingDirectory());
                cachedWorkspace = findCachedWorkspaceForPath(currentDirectory.getAbsolutePath());
            }

            if (cachedWorkspace == null) {
                throw new CannotFindWorkspaceException(Messages.getString("Command.WorkspaceCouldNotBeDetermined")); //$NON-NLS-1$
            }
        }

        Check.notNull(cachedWorkspace, "cachedWorkspace"); //$NON-NLS-1$
        return cachedWorkspace;
    }

    /**
     * Searches the local workspace cache for workspaces that match.
     *
     * @param serverURI
     *        the server {@link URI} to match (<code>null</code> matches all)
     * @param name
     *        the name to match (must not be <code>null</code>)
     * @param owner
     *        the owner to match (<code>null</code> matches all)
     * @return the workspaces found, possibly an empty list
     */
    private WorkspaceInfo[] findLocalWorkspaces(final URI serverURI, final String name, final String owner) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        final WorkspaceInfo[] infos = Workstation.getCurrent(CLC_PERSISTENCE_PROVIDER).getAllLocalWorkspaceInfo();

        if (infos == null || infos.length == 0) {
            return new WorkspaceInfo[0];
        }

        final List<WorkspaceInfo> matches = new ArrayList<WorkspaceInfo>();

        for (final WorkspaceInfo info : infos) {
            if ((serverURI == null || Workspace.matchServerURI(serverURI, info.getServerURI()))
                && Workspace.matchName(name, info.getName())
                && (owner == null || info.ownerNameMatches(owner))) {
                matches.add(info);
            }
        }

        return matches.toArray(new WorkspaceInfo[matches.size()]);
    }

    /**
     * Throw an exception if the the specified array of paths contain a local
     * file path that does not have a workspace mapping.
     *
     * @param pathFreeArguments
     *        an array of server and/or local path names
     * @throws CannotFindWorkspaceException
     *         thrown by findCacheWorkspaceForPath
     * @throws CLCException
     *         if a local path does not have a workspace mapping.
     */
    protected void throwIfContainsUnmappedLocalPath(final String[] pathFreeArguments)
        throws CannotFindWorkspaceException,
            CLCException {
        if (pathFreeArguments != null) {
            for (final String path : pathFreeArguments) {
                if (!ServerPath.isServerPath(path)) {
                    if (findCachedWorkspaceForPath(path) == null) {
                        final String messageFormat = Messages.getString("CommandDir.NoWorkingFolderMappingFormat"); //$NON-NLS-1$
                        throw new CLCException(MessageFormat.format(messageFormat, path));
                    }
                }
            }
        }
    }

    /**
     * Gets the {@link OptionCollection} (possibly an instance of
     * {@link OptionServer}) the user specified, or null if neither was
     * specified. If both were specified, reports an error.
     *
     * @return the {@link OptionCollection} (possibly an instance of
     *         {@link OptionServer}) specified by the user, or null if neither
     *         was specified
     * @throws InvalidOptionException
     *         if {@link OptionCollection} and {@link OptionServer} were both
     *         specified
     */
    protected OptionCollection getCollectionOption() throws InvalidOptionException {
        final OptionServer serverOption = (OptionServer) findOptionType(OptionServer.class);
        final OptionCollection collectionOption = (OptionCollection) findOptionType(OptionCollection.class);

        if (serverOption != null && collectionOption != null) {
            final String messageFormat = Messages.getString("Command.OptionCannotBeUsedWithOptionFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, serverOption.getMatchedAlias(), collectionOption.getMatchedAlias());
            throw new InvalidOptionException(message);
        }

        if (serverOption != null) {
            return serverOption;
        }

        return collectionOption;
    }

    /**
     * Creates a new {@link TFSTeamProjectCollection} object and initialize it
     * with the given arguments and options given to this command.
     * <p>
     * When searching for a cached workspace to use for the connection, this
     * method treats all free arguments as local paths. Use a method that takes
     * a list of local paths for finer control.
     *
     * @return a new TFSConnection object, initialized for the user's arguments,
     *         and already connected to the server.
     * @throws TFSException
     *         if the server returned an error when the repository properties
     *         were refreshed.
     * @throws MissingRequiredOptionException
     *         when a required option is missing (TODO remove the capability to
     *         throw this exception type when the options are really optional,
     *         and not required).
     * @throws CannotFindWorkspaceException
     *         when the workspace was not specified as an option, and cannot be
     *         determined from any argument paths.
     * @throws MalformedURLException
     *         if a valid URL could not be constructed for this TFS object.
     * @throws CLCException
     *         when the login credentials could not be found in the cache and
     *         were not supplied as an option.
     * @throws ActivationException
     */
    protected final TFSTeamProjectCollection createConnection()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException {
        return createConnection(null, null, false, false);
    }

    /**
     * Creates a new {@link TFSTeamProjectCollection} object and initialize it
     * with the given arguments and options given to this command.
     * <p>
     * When searching for a cached workspace to use for the connection, this
     * method treats all free arguments as local paths. Use a method that takes
     * a list of local paths for finer control.
     *
     * @param ignoreWorkspaceDetectionFailure
     *        if true, this method will not throw an exception when no workspace
     *        is detected, but the server option is required, or an exception is
     *        thrown. If false, an exception is thrown if a workspace is not
     *        detected.
     * @return a new TFSConnection object, initialized for the user's arguments,
     *         and already connected to the server.
     * @throws CannotFindWorkspaceException
     *         when the workspace was not specified as an option, and cannot be
     *         determined from any argument paths.
     * @throws MalformedURLException
     *         if a valid URL could not be constructed for this TFS object.
     * @throws CLCException
     *         when the login credentials could not be found in the cache and
     *         were not supplied as an option.
     * @throws ActivationException
     */
    protected final TFSTeamProjectCollection createConnection(final boolean ignoreWorkspaceDetectionFailure)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException {
        return createConnection(null, null, ignoreWorkspaceDetectionFailure, false);
    }

    /**
     * Creates a new {@link TFSTeamProjectCollection} object and initialize it
     * with the given arguments and options given to this command.
     *
     * @param pathFreeArguments
     *        an array of all the free arguments that are local paths for this
     *        command, so they can be searched if no profile or workspace option
     *        was given (or they are not checked because of other parameter
     *        values). If null, all free arguments are matched as local paths.
     *        If an empty array, no path-based search for a cached workspace is
     *        performed.
     *
     * @return a new TFSConnection object, initialized for the user's arguments,
     *         and already connected to the server.
     * @throws TFSException
     *         if the server returned an error when the repository properties
     *         were refreshed.
     * @throws MissingRequiredOptionException
     *         when a required option is missing (TODO remove the capability to
     *         throw this exception type when the options are really optional,
     *         and not required).
     * @throws CannotFindWorkspaceException
     *         when the workspace was not specified as an option, and cannot be
     *         determined from any argument paths.
     * @throws MalformedURLException
     *         if a valid URL could not be constructed for this TFS object.
     * @throws CLCException
     *         when the login credentials could not be found in the cache and
     *         were not supplied as an option.
     * @throws ActivationException
     */
    protected final TFSTeamProjectCollection createConnection(final String[] pathFreeArguments)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException {
        return createConnection(null, pathFreeArguments, false, false);
    }

    /**
     * Creates a new {@link TFSTeamProjectCollection} object and initialize it
     * with the given arguments and options given to this command.
     *
     * @param pathFreeArguments
     *        an array of all the free arguments that are local paths for this
     *        command, so they can be searched if no profile or workspace option
     *        was given (or they are not checked because of other parameter
     *        values). If null, all free arguments are matched as local paths.
     *        If an empty array, no path-based search for a cached workspace is
     *        performed.
     * @param ignoreWorkspaceDetectionFailure
     *        if true, this method will not throw an exception when no workspace
     *        is detected, but the server option is required, or an exception is
     *        thrown. If false, an exception is thrown if a workspace is not
     *        detected.
     * @return a new TFSConnection object, initialized for the user's arguments,
     *         and already connected to the server.
     * @throws CannotFindWorkspaceException
     *         when the workspace was not specified as an option, and cannot be
     *         determined from any argument paths.
     * @throws MalformedURLException
     *         if a valid URL could not be constructed for this TFS object.
     * @throws CLCException
     *         when the login credentials could not be found in the cache and
     *         were not supplied as an option.
     * @throws ActivationException
     */
    protected final TFSTeamProjectCollection createConnection(
        final String[] pathFreeArguments,
        final boolean ignoreWorkspaceDetectionFailure)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException {
        return createConnection(null, pathFreeArguments, ignoreWorkspaceDetectionFailure, false);
    }

    /**
     * Creates a new {@link TFSTeamProjectCollection} object and initialize it
     * with the given arguments and options given to this command.
     * <p>
     * The first param (ignoreWorkspaceAutoDetectionFailure) is a little
     * complicated. The option controls whether the exception is thrown if and
     * only if an explicit workspace option is not given by the user and the
     * automatic detection fails. The exception is always thrown if the
     * workspace option value was given and it can't be used. This option is set
     * for commands like history, label, properties, status, workspace, etc.
     * These commands don't (always) require a local workspace.
     * <p>
     * When searching for a cached workspace to use for the connection, this
     * method treats all free arguments as local paths. Use a method that takes
     * a list of local paths for finer control.
     *
     * @param ignoreWorkspaceAutoDetectionFailure
     *        if true, this method will not throw an exception when no workspace
     *        is detected and no workspace option was given, but the server
     *        option is required, or an exception is thrown. If false, an
     *        exception is thrown if a workspace is not detected. Useful for
     *        commands that don't require a workspace.
     * @param ignoreWorkspaceOptionValue
     *        if true, the any workspace option supplied on the command-line is
     *        ignored, and only the free arguments and current directory are
     *        checked to determine the cached workspace. This is probably only
     *        useful for the CLC for commands where the /workspace option is
     *        used to set the workspace name to query on, but should not cause a
     *        cached workspace to be present for it.
     * @return a new TFSConnection object, initialized for the user's arguments,
     *         and already connected to the server.
     * @throws CannotFindWorkspaceException
     *         when the workspace was not specified as an option, or the
     *         workspace option matched multiple or no workspace, or the
     *         workspace cannot be determined from any argument paths.
     * @throws MalformedURLException
     *         if a valid URL could not be constructed for this TFS object.
     * @throws CLCException
     *         when the login credentials could not be found in the cache and
     *         were not supplied as an option.
     * @throws ActivationException
     */
    protected final TFSTeamProjectCollection createConnection(
        final boolean ignoreWorkspaceAutoDetectionFailure,
        final boolean ignoreWorkspaceOptionValue)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException {
        return createConnection(null, null, ignoreWorkspaceAutoDetectionFailure, ignoreWorkspaceOptionValue);
    }

    /**
     * Creates a new {@link TFSTeamProjectCollection} object and initialize it
     * with the given arguments and options given to this command.
     * <p>
     * The first param (ignoreWorkspaceAutoDetectionFailure) is a little
     * complicated. The option controls whether the exception is thrown if and
     * only if an explicit workspace option is not given by the user and the
     * automatic detection fails. The exception is always thrown if the
     * workspace option value was given and it can't be used. This option is set
     * for commands like history, label, properties, status, workspace, etc.
     * These commands don't (always) require a local workspace.
     *
     * @param pathFreeArguments
     *        an array of all the free arguments that are local paths for this
     *        command, so they can be searched if no profile or workspace option
     *        was given (or they are not checked because of other parameter
     *        values). If null, all free arguments are matched as local paths.
     *        If an empty array, no path-based search for a cached workspace is
     *        performed.
     * @param ignoreWorkspaceAutoDetectionFailure
     *        if true, this method will not throw an exception when no workspace
     *        is detected and no workspace option was given, but the server
     *        option is required, or an exception is thrown. If false, an
     *        exception is thrown if a workspace is not detected. Useful for
     *        commands that don't require a workspace.
     * @param ignoreWorkspaceOptionValue
     *        if true, the any workspace option supplied on the command-line is
     *        ignored, and only the free arguments and current directory are
     *        checked to determine the cached workspace. This is probably only
     *        useful for the CLC for commands where the /workspace option is
     *        used to set the workspace name to query on, but should not cause a
     *        cached workspace to be present for it.
     * @return a new TFSConnection object, initialized for the user's arguments,
     *         and already connected to the server.
     * @throws CannotFindWorkspaceException
     *         when the workspace was not specified as an option, or the
     *         workspace option matched multiple or no workspace, or the
     *         workspace cannot be determined from any argument paths.
     * @throws MalformedURLException
     *         if a valid URL could not be constructed for this TFS object.
     * @throws CLCException
     *         when the login credentials could not be found in the cache and
     *         were not supplied as an option.
     * @throws ActivationException
     */
    protected final TFSTeamProjectCollection createConnection(
        final String[] pathFreeArguments,
        final boolean ignoreWorkspaceAutoDetectionFailure,
        final boolean ignoreWorkspaceOptionValue)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException {
        return createConnection(
            null,
            pathFreeArguments,
            ignoreWorkspaceAutoDetectionFailure,
            ignoreWorkspaceOptionValue);
    }

    /**
     * Creates a new {@link TFSTeamProjectCollection} object and initialize it
     * with the given arguments and options given to this command.
     * <p>
     * The ignoreWorkspaceAutoDetectionFailure parameter is a little
     * complicated. The option controls whether the exception is thrown if and
     * only if an explicit workspace option is not given by the user and the
     * automatic detection fails. The exception is always thrown if the
     * workspace option value was given and it can't be used. This option is set
     * for commands like history, label, properties, status, workspace, etc.
     * These commands don't (always) require a local workspace.
     *
     * @param uri
     *        the connection uri to use. If null, one is built from the
     *        environment and options provided by the user. If non-null, the
     *        profile is cloned and the clone is modified by the method.
     * @param pathFreeArguments
     *        an array of all the free arguments that are local paths for this
     *        command, so they can be searched if no profile or workspace option
     *        was given (or they are not checked because of other parameter
     *        values). If null or empty, no path-based search for a cached
     *        workspace is performed.
     * @param ignoreWorkspaceAutoDetectionFailure
     *        if true, this method will not throw an exception when no workspace
     *        is detected and no workspace option was given, but the server
     *        option is required, or an exception is thrown. If false, an
     *        exception is thrown if a workspace is not detected. Useful for
     *        commands that don't require a workspace.
     * @param ignoreWorkspaceOptionValue
     *        if true, the any workspace option supplied on the command-line is
     *        ignored, and only the free arguments and current directory are
     *        checked to determine the cached workspace. This is probably only
     *        useful for the CLC for commands where the /workspace option is
     *        used to set the workspace name to query on, but should not cause a
     *        cached workspace to be present for it.
     * @return a new TFSConnection object, initialized for the user's arguments,
     *         and already connected to the server.
     * @throws CannotFindWorkspaceException
     *         when the workspace was not specified as an option, or the
     *         workspace option matched multiple or no workspace, or the
     *         workspace cannot be determined from any argument paths.
     * @throws MalformedURLException
     *         if a valid URL could not be constructed for this TFS object.
     * @throws CLCException
     *         when the login credentials could not be found in the cache and
     *         were not supplied as an option.
     * @throws LicenseException
     *         when a licensing error occurs (the EULA is not accepted or a
     *         product id is not installed)
     */
    protected final TFSTeamProjectCollection createConnection(
        URI serverURI,
        final String[] pathFreeArguments,
        final boolean ignoreWorkspaceAutoDetectionFailure,
        final boolean ignoreWorkspaceOptionValue)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException {
        Option o = null;

        /* Test for EULA acceptance / product id installation */
        testLicense();

        /*
         * If we don't yet have a profile (no profile option), find a cached
         * workspace and use its profile.
         */
        if (serverURI == null) {
            WorkspaceInfo cachedWorkspace = null;

            try {
                cachedWorkspace = determineCachedWorkspace(pathFreeArguments, ignoreWorkspaceOptionValue);
            } catch (final CannotFindWorkspaceException e) {
                /*
                 * Since we had an error loading the cached workspace, throw if
                 * any of:
                 *
                 * 1. The option was given and failed to load.
                 *
                 * 2. The option wasn't given but the user doesn't want to
                 * ignore auto detect failures.
                 */
                if (findOptionType(OptionWorkspace.class) != null || !ignoreWorkspaceAutoDetectionFailure) {
                    throw e;
                }
            }

            if (cachedWorkspace != null) {
                serverURI = cachedWorkspace.getServerURI();
            }
        }

        /*
         * Now override any cached settings with properties from other
         * command-line options. These are not transient properties, they should
         * be saved in the workspace cache for future sessions. Transient
         * properties would not be saved at all, leaving missing attributes in
         * the cached workspaces.
         */

        if ((o = getCollectionOption()) != null) {
            serverURI = getCollectionOption().getURI();
        }

        /*
         * If we have no server URL by now, we should error. This check should
         * happen before the credentials are validated so we don't prompt the
         * user for data, then throw for lack of server URL.
         */
        if (serverURI == null) {
            throw new CLCException(Messages.getString("Command.CouldNotDetermineCollectionURL")); //$NON-NLS-1$
        }

        Credentials credentials = null;

        final CredentialsManager credentialsManager = CredentialsManagerFactory.getCredentialsManager(
            CLC_PERSISTENCE_PROVIDER,
            usePersistanceCredentialsManager());
        final CachedCredentials cachedCredentials = credentialsManager.getCredentials(serverURI);

        if ((o = findOptionType(OptionLogin.class)) != null) {
            credentials = ((OptionLogin) o).getType() == OptionLogin.LoginType.USERNAME_PASSWORD
                ? new UsernamePasswordCredentials(((OptionLogin) o).getUsername(), ((OptionLogin) o).getPassword())
                : new JwtCredentials(((OptionLogin) o).getToken());
        }
        /*
         * If the user has saved a username (regardless of whether they have a
         * saved password), then use that information.
         */
        else if (cachedCredentials != null
            && cachedCredentials.getUsername() != null
            && cachedCredentials.getUsername().length() > 0) {
            credentials =
                new UsernamePasswordCredentials(cachedCredentials.getUsername(), cachedCredentials.getPassword());
        } else {
            /*
             * For TFS instance, validate will test whether Kerberos is
             * available and the user has a ticket and prompt for
             * username/pass/domain if not.
             * 
             * For VSTS instance, do interactive auth if allow prompt
             */
            credentials = ServerURIUtils.isHosted(serverURI) ? null : new DefaultNTCredentials();
        }

        /*
         * This method checks the credentials we've read already, prompts if
         * allowed for missing ones, or lets defaults kick in.
         */
        log.debug("credentialsManager.canWrite(): " + credentialsManager.canWrite()); //$NON-NLS-1$
        log.debug("persistCredentials(): " + persistCredentials()); //$NON-NLS-1$

        credentials =
            validateCredentials(serverURI, credentials, credentialsManager.canWrite() && persistCredentials());

        /*
         * The CLCConnectionAdvisor looks for the "proxy" option to configure
         * the TF download proxy.
         */
        final TFSTeamProjectCollection connection = getConnection(serverURI, credentials);

        return connection;
    }

    private TFSTeamProjectCollection getConnection(URI serverURI, final Credentials credentials) {
        List<TFSTeamProjectCollection> connectionList;

        serverURI = URIUtils.ensurePathHasTrailingSlash(serverURI);
        serverURI = URIUtils.toLowerCase(serverURI);

        if (connections.containsKey(serverURI)) {
            connectionList = connections.get(serverURI);
        } else {
            connectionList = new ArrayList<TFSTeamProjectCollection>();
            connections.put(serverURI, connectionList);
        }

        for (final TFSTeamProjectCollection connection : connectionList) {
            if (credentials.equals(connection.getCredentials())) {
                return connection;
            }
        }

        collection = new TFSTeamProjectCollection(serverURI, credentials, new CLCConnectionAdvisor(this));

        connectionList.add(collection);

        return collection;
    }

    /**
     * Tests for EULA acceptance and product id installation.
     *
     * @throws LicenseException
     *         if there is a licensing error.
     */
    private void testLicense() throws LicenseException {
        if (!LicenseManager.getInstance().isEULAAccepted()) {
            throw new LicenseException(LicenseExceptionType.EULA, Messages.getString("Command.YouMustAcceptEULA")); //$NON-NLS-1$
        }
    }

    /**
     * Validates the credentials given, prompting for any missing bits (if
     * allowed), or filling in with default credentials (if available and
     * allowed). Throws on error, returns the updated credentials on success.
     *
     * @param credentials
     *        the credentials to validate (or <code>null</code> if no
     *        credentials are known)
     * @param persistCredentials
     *        true to save credentials, false otherwise
     * @throws CLCException
     *         if the credentials were not valid and couldn't be fixed.
     */
    private Credentials validateCredentials(
        final URI serverURI,
        Credentials credentials,
        final boolean persistCredentials) throws CLCException {

        /*
         * Disable UI prompts for encrypted password storage (eg, unlock
         * keychains) when in noprompt mode.
         */
        final boolean prompt = (findOptionType(OptionNoPrompt.class) == null);

        // We'll prompt (or throw) for any of these still invalid at end of
        // method
        String username = null;
        String password = null;

        // No credentials specified
        if (credentials == null) {
            /*
             * Defensively create a DefaultNTCredentials so we do not throw NPE
             */
            credentials = new DefaultNTCredentials();

            if (Prompt.interactiveLoginAllowed() && ServerURIUtils.isHosted(serverURI) && prompt) {
                log.debug(
                    "Request against VisualStudio Team Services and credential is not available, try OAuth2 flow."); //$NON-NLS-1$
                /*
                 * Interactive auth for team services when there is no cred
                 * presented
                 */
                final UsernamePasswordCredentials cred =
                    Prompt.getCredentialsInteractively(serverURI, display, persistCredentials);
                if (cred != null) {
                    log.debug("Retrieved a credential interactively from OAuth2 flow."); //$NON-NLS-1$
                    credentials = cred;
                } else {
                    log.debug(
                        "Failed to retrieve any credential, did user close the browser or JavaFx failed to load?"); //$NON-NLS-1$
                }
            }

        }

        if (credentials instanceof JwtCredentials) {
            return credentials;
        } else if (credentials instanceof UsernamePasswordCredentials) {
            username = ((UsernamePasswordCredentials) credentials).getUsername();
            password = ((UsernamePasswordCredentials) credentials).getPassword();
        } else if (credentials instanceof DefaultNTCredentials) {
            // Validate default credentials are available
            if (!NegotiateEngine.getInstance().isAvailable() && !NTLMEngine.getInstance().isAvailable()) {
                log.debug("Default credentials are unavailable (library load failure or other initialization problem)"); //$NON-NLS-1$
                getDisplay().printLine(Messages.getString("Command.DefaultCredsUnavailableLibraryLoadFailure")); //$NON-NLS-1$
            } else if (!NegotiateEngine.getInstance().supportsCredentialsDefault()
                && !NTLMEngine.getInstance().supportsCredentialsDefault()) {
                log.debug("Default credentials are unavailable (no ticket or token)"); //$NON-NLS-1$
                getDisplay().printLine(Messages.getString("Command.DefaultCredsUnavailableNoTicket")); //$NON-NLS-1$
            }

            // Tests again that the libraries loaded and the user has a ticket
            if (CredentialsUtils.supportsDefaultCredentials()) {
                log.debug("Using default credentials (supported and available)"); //$NON-NLS-1$
                return credentials;
            }

            /*
             * Can't load Kerberos libs or no ticket available.
             */
        } else {
            /* Unknown credential type. */
            throwForInsufficientCredentials();
        }

        // Incomplete
        if (!(username != null && username.length() > 0 && password != null)) {
            if (!prompt) {
                throwForInsufficientCredentials();
            }

            credentials = Prompt.getCredentials(getDisplay(), getInput(), username, password);

            // Null if IO error or not enough info to create credentials
            if (credentials == null) {
                throwForInsufficientCredentials();
            }
        }

        if (persistCredentials) {
            CredentialsManagerFactory.getCredentialsManager(
                CLC_PERSISTENCE_PROVIDER,
                usePersistanceCredentialsManager()).setCredentials(new CachedCredentials(serverURI, credentials));
        }
        return credentials;
    }

    /**
     * @return <code>true</code> if credentials may be saved (possibly
     *         insecurely), <code>false</code> if they may not be saved
     */
    public boolean persistCredentials() {
        return (PlatformMiscUtils.getInstance().getEnvironmentVariable(
            EnvironmentVariables.AUTO_SAVE_CREDENTIALS) != null);
    }

    /**
     * @return <code>true</code> if PersistanceCredentialsManager may be used
     *
     */
    public boolean usePersistanceCredentialsManager() {
        return !EnvironmentVariables.getBoolean(EnvironmentVariables.USE_KEYCHAIN, true);
    }

    private String throwForInsufficientCredentials() throws CLCException {
        throw new CLCException(Messages.getString("Command.AutoCredentialsNotAvailable")); //$NON-NLS-1$
    }

    /**
     * Just like {@link WorkspaceInfo#getWorkspace(TFSTeamProjectCollection)}
     * except it throws if no workspace could be found, which is useful for CLC
     * command classes.
     *
     * @param cachedWorkspace
     *        the cached workspace to realize (not null).
     * @param client
     *        a {@link VersionControlClient} initialized for this connection
     *        (not null).
     * @return the {@link Workspace} object that matches this cached workspace.
     * @throws CannotFindWorkspaceException
     *         if an {@link Workspace} could not be found that matches the
     *         cached workspace (it may have been deleted, renamed, permissions
     *         changed, etc.).
     */
    public final Workspace realizeCachedWorkspace(
        final WorkspaceInfo cachedWorkspace,
        final VersionControlClient client) throws CannotFindWorkspaceException {
        Check.notNull(cachedWorkspace, "cachedWorkspace"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        final Workspace ret = cachedWorkspace.getWorkspace(client.getConnection());

        if (ret == null) {
            final String workspaceSpec =
                new WorkspaceSpec(cachedWorkspace.getName(), cachedWorkspace.getOwnerDisplayName()).toString();
            final String messageFormat = Messages.getString("Command.WorkspaceNoLongerExistsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, workspaceSpec);
            throw new CannotFindWorkspaceException(message);
        }

        return ret;
    }

    /**
     * Hooks up event listeners and configures a local workspace path watcher
     * factory for the given client.
     *
     * @param client
     *        the client to initialize (must not be <code>null</code>)
     */
    protected void initializeClient(final VersionControlClient client) {
        Check.notNull(client, "client"); //$NON-NLS-1$

        final VersionControlEventEngine ee = client.getEventEngine();

        /*
         * Hook up all the events we care about.
         */
        ee.addNewPendingChangeListener(this);
        ee.addBeforeCheckinListener(this);
        ee.addBeforeShelveListener(this);
        ee.addCheckinListener(this);
        ee.addNonFatalErrorListener(this);
        ee.addUndonePendingChangeListener(this);
        ee.addDestroyListener(this);
        ee.addConflictListener(this);
        ee.addMergingListener(this);

        client.setPathWatcherFactory(new CLCPathWatcherFactory());

        // Store this for close()
        this.client = client;
        workstation = Workstation.getCurrent(client.getConnection().getPersistenceStoreProvider());

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            try {
                notificationManager = new MessageWindowNotificationManager();
                workstation.setNotificationManager(notificationManager);
            } catch (final Exception e) {
                // Might fail for non-interactive process, ignore
            }
        }
    }

    @Override
    public void close() {
        // Flush any remaining notifications and remove the manager
        if (notificationManager != null) {
            notificationManager.close();
            workstation.setNotificationManager(null);

            notificationManager = null;
            workstation = null;
        }

        // Update client event engine
        if (client != null) {
            client.setPathWatcherFactory(new NullPathWatcherFactory());

            final VersionControlEventEngine ee = client.getEventEngine();
            ee.clear();

            client = null;
        }
    }

    @Override
    public void onCheckin(final CheckinEvent e) {
        /*
         * Diplay any changes that were undone by the server (not committed in
         * the changeset).
         */
        if (e.getUndoneChanges().length > 0) {
            getDisplay().printLine(""); //$NON-NLS-1$
            getDisplay().printLine(Messages.getString("Command.ChangesNotCheckedInBecauseNotModified")); //$NON-NLS-1$

            for (final PendingChange pc : e.getUndoneChanges()) {
                String localOrServerPath = pc.getLocalItem();

                if (StringUtil.isNullOrEmpty(localOrServerPath)) {
                    localOrServerPath = pc.getServerItem();
                }

                getDisplay().printLine(MessageFormat.format(
                    Messages.getString("Command.UndoingUndoneFormat"), //$NON-NLS-1$
                    pc.getChangeType().toUIString(false, pc),
                    localOrServerPath));
            }
        }

        if (e.getChangesetID() == 0) {
            getDisplay().printErrorLine(""); //$NON-NLS-1$
            getDisplay().printErrorLine(Messages.getString("Command.NoChangesLeftToCheckin")); //$NON-NLS-1$
        } else if (e.getChangesetID() > 0) {
            getDisplay().printLine(""); //$NON-NLS-1$
            getDisplay().printLine(MessageFormat.format(
                Messages.getString("Command.ChangesetNumberCheckedInFormat"), //$NON-NLS-1$
                Integer.toString(e.getChangesetID())));
        }
    }

    @Override
    public void onNonFatalError(final NonFatalErrorEvent e) {
        boolean showError = true;
        String s = null;

        synchronized (nonFatalErrorLock) {
            if (e.getThrowable() != null) {
                numNonFatalErrors++;

                if (e.getThrowable() == null || !(e.getThrowable() instanceof DownloadProxyException)) {
                    setExitCode(ExitCode.PARTIAL_SUCCESS);
                }

                if (e.getThrowable().getLocalizedMessage() != null) {
                    s = e.getThrowable().getLocalizedMessage();
                } else {
                    final StringWriter sw = new StringWriter();
                    e.getThrowable().printStackTrace(new PrintWriter(sw));
                    s = sw.toString();
                }
            }

            if (e.getFailure() != null) {
                if (e.getFailure().getSeverity() == SeverityType.ERROR) {
                    numNonFatalErrors++;
                    setExitCode(ExitCode.PARTIAL_SUCCESS);
                } else if (e.getFailure().getSeverity() == SeverityType.WARNING) {
                    showError = false;
                    numNonFatalWarnings++;
                } else {
                    log.error("Unexpected severity: " + e.getFailure().getSeverity()); //$NON-NLS-1$
                }

                if (e.getFailure().getWarnings() != null && e.getFailure().getWarnings().length > 0) {
                    /*
                     * For warnings, separating the output with a blank line
                     * makes them easier to read since they consist of multiple
                     * lines.
                     */
                    s = NewlineUtils.PLATFORM_NEWLINE + e.getFailure().getFormattedMessage();
                } else {
                    s = e.getFailure().getFormattedMessage();
                }
            }

            // Record it in the list of errors and warnings.
            nonFatalMessageList.add(new NonFatalErrorMessage(showError, s));
        }

        // Display the error. (TODO if we get a warning stream, print
        // non-showError messages there)

        getDisplay().printErrorLine(s);
    }

    @Override
    public void onConflict(final ConflictEvent e) {
        getDisplay().printLine(e.getMessage());
    }

    /*
     * Note: this is for conflicts that are automatically resolved (in dev 11).
     * Commands must hook this manually, it is not hooked in initializeClient().
     * This is to avoid printing the automatic conflict resolution message when
     * running CommandResolve.
     */
    @Override
    public void onConflictResolved(final ConflictResolvedEvent e) {
        getDisplay().printLine(MessageFormat.format(
            Messages.getString("Command.AutoResolvedConflictFormat"), //$NON-NLS-1$
            e.getConflict().getDetailedMessage(false),
            ConflictResolutionHelper.getResolutionString(e.getConflict().getResolution())));
    }

    @Override
    public void onMerging(final MergingEvent e) {
        // Keep a list of unsuccessful operations for displaying in the summary.
        if (e.getStatus() != OperationStatus.GETTING
            && e.getStatus() != OperationStatus.DELETING
            && e.getStatus() != OperationStatus.REPLACING) {
            mergeWarningList.add(e);
        }

        displayMergingEvent(e);
    }

    /**
     * Displays a merge event.
     *
     * @param e
     *        the event to display
     */
    private void displayMergingEvent(final MergingEvent e) {
        final AtomicReference<String> errorHolder = new AtomicReference<String>();
        final String message = e.getMessage(errorHolder);

        // Display the merge info.
        if (e.getStatus() == OperationStatus.CONFLICT) {
            getDisplay().printErrorLine(message);
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        } else {
            getDisplay().printLine(message);
        }

        if (e.getStatus() == OperationStatus.GETTING
            || e.getStatus() == OperationStatus.REPLACING
            || e.getStatus() == OperationStatus.DELETING
            || e.getStatus() == OperationStatus.CONFLICT) {
            // Nothing to do
        } else if (e.getStatus() == OperationStatus.SOURCE_DIRECTORY_NOT_EMPTY
            || e.getStatus() == OperationStatus.SOURCE_WRITABLE
            || e.getStatus() == OperationStatus.TARGET_IS_DIRECTORY
            || e.getStatus() == OperationStatus.TARGET_LOCAL_PENDING
            || e.getStatus() == OperationStatus.TARGET_WRITABLE) {
            getDisplay().printErrorLine(errorHolder.get());
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        }
    }

    @Override
    public void onGet(final GetEvent e) {
        // Get a short version of the target path if it's not null.
        String shortTargetName = null;
        if (e.getTargetLocalItem() != null) {
            // First check for an item that doesn't have a parent, such as c:\.
            // Then extract the parent directory name from the path.
            if (LocalPath.getParent(e.getTargetLocalItem()) == null
                || e.getTargetLocalItem().equals(LocalPath.getParent(e.getTargetLocalItem()))) {
                shortTargetName = e.getTargetLocalItem();
            } else {
                shortTargetName = LocalPath.getFileName(e.getTargetLocalItem());
                final String folder = LocalPath.getParent(e.getTargetLocalItem());

                // Write a header if necessary.
                if (lastGetDirectory == null || !LocalPath.equals(lastGetDirectory, folder)) {
                    if (lastGetDirectory != null) {
                        getDisplay().printLine(""); //$NON-NLS-1$
                    }

                    getDisplay().printLine(MessageFormat.format(Messages.getString("Command.AColonFormat"), folder)); //$NON-NLS-1$

                    // Update the folder of the last item displayed.
                    lastGetDirectory = folder;
                }
            }
        }

        if (e.getStatus() != OperationStatus.GETTING
            && e.getStatus() != OperationStatus.DELETING
            && e.getStatus() != OperationStatus.REPLACING) {
            getWarningList.add(e);
        }

        displayGetEvent(e, shortTargetName);
    }

    /**
     * Displays a {@link GetEvent}.
     *
     * @param e
     *        the event to display (must not be <code>null</code>)
     * @param targetName
     *        the target name to use (may be <code>null</code> to use an item
     *        name from the event)
     */
    private void displayGetEvent(final GetEvent e, final String targetName) {
        final AtomicReference<String> errorHolder = new AtomicReference<String>();
        final String message = e.getMessage(targetName, errorHolder);

        if (e.getStatus() == OperationStatus.CONFLICT
            || e.getStatus() == OperationStatus.SOURCE_WRITABLE
            || e.getStatus() == OperationStatus.TARGET_LOCAL_PENDING
            || e.getStatus() == OperationStatus.TARGET_WRITABLE
            || e.getStatus() == OperationStatus.SOURCE_DIRECTORY_NOT_EMPTY
            || e.getStatus() == OperationStatus.TARGET_IS_DIRECTORY
            || e.getStatus() == OperationStatus.UNABLE_TO_REFRESH) {
            getDisplay().printErrorLine(errorHolder.get());
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        } else if (e.getStatus() == OperationStatus.GETTING
            || e.getStatus() == OperationStatus.REPLACING
            || e.getStatus() == OperationStatus.DELETING) {
            getDisplay().printLine(message);
        }
    }

    /**
     * Displays the summary information for a merge.
     *
     * @param status
     *        get status info
     * @param showConflicts
     *        If false, only warnings and errors will be shown.
     */
    protected void displayMergeSummary(final GetStatus status, final boolean showConflicts) {
        if (!shouldDisplaySummary(status)) {
            return;
        }

        displayGetStatus(status);

        if (status.getNumConflicts() > 0 && !showConflicts) {
            // Conflict display is suppressed: inform the user.
            getDisplay().printErrorLine(
                MessageFormat.format(
                    Messages.getString("Command.MergeConflictsSuppressedFormat"), //$NON-NLS-1$
                    Integer.toString(status.getNumConflicts()),
                    OptionsMap.getPreferredOptionPrefix()));
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        }

        for (final MergingEvent e : mergeWarningList) {
            if ((e.getStatus() != OperationStatus.CONFLICT) || showConflicts) {
                displayMergingEvent(e);
            }
        }

        displayErrors();
    }

    /**
     * Displays the summary information for a get.
     *
     * @param get
     *        status info
     */
    protected void displayGetSummary(final GetStatus status) {
        if (!shouldDisplaySummary(status)) {
            return;
        }

        displayGetStatus(status);

        for (final GetEvent e : getWarningList) {
            displayGetEvent(e, e.getTargetLocalItem());
        }

        displayErrors();
    }

    /**
     * Returns true if the get/merge summary should be displayed.
     */
    private boolean shouldDisplaySummary(final GetStatus status) {
        final int numNonFatalErrorsAndWarnings = numNonFatalErrors + numNonFatalWarnings;

        final String noSummaryValue =
            PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.NO_SUMMARY);

        /*
         * Since the conflicts and warnings are accounted for by NumOperations,
         * add NumFailures to determine whether the threshold has been crossed.
         */
        return numNonFatalErrorsAndWarnings + status.getNumConflicts() + status.getNumWarnings() != 0
            && status.getNumOperations() >= SUMMARY_THRESHOLD
            && (noSummaryValue == null || noSummaryValue.length() == 0);
    }

    /**
     * Display the totals from GetStatus.
     *
     * @param status
     *        the status information to display
     */
    protected void displayGetStatus(final GetStatus status) {
        getDisplay().printLine(""); //$NON-NLS-1$
        getDisplay().printLine(MessageFormat.format(
            Messages.getString("Command.GetStatusSummaryFormat"), //$NON-NLS-1$
            Integer.toString(status.getNumConflicts()),
            Integer.toString(status.getNumWarnings() + numNonFatalWarnings),
            Integer.toString(numNonFatalErrors)));
    }

    protected boolean displayCheckinNoteFailures(final CheckinNoteFailure[] noteFailures) {
        if (noteFailures != null && noteFailures.length > 0) {
            getDisplay().printLine(""); //$NON-NLS-1$

            if (noteFailures.length > 1) {
                getDisplay().printErrorLine(Messages.getString("Command.CheckInNotesDoNotPassRequirement")); //$NON-NLS-1$
            } else {
                getDisplay().printErrorLine(Messages.getString("Command.CheckinNoteDoesNotPassRequirement")); //$NON-NLS-1$
            }

            for (final CheckinNoteFailure failure : noteFailures) {
                final String messageFormat = Messages.getString("Command.CheckinNoteFailureOutputFormat"); //$NON-NLS-1$
                final String message =
                    MessageFormat.format(messageFormat, failure.getDefinition().getName(), failure.getMessage());

                getDisplay().printErrorLine(message);
            }

            return true;
        }

        return false;
    }

    /**
     * Display the non-fatal errors captured during execution of the command.
     */
    private void displayErrors() {
        synchronized (nonFatalErrorLock) {
            for (final NonFatalErrorMessage nfem : nonFatalMessageList) {
                // TODO if we ever get stdwarn in Unix, print non-isError()
                // items there :)

                getDisplay().printErrorLine(nfem.getMessage());
            }
        }
    }

    @Override
    public void onBeforeCheckin(final PendingChangeEvent e) {
        Check.notNull(e, "e"); //$NON-NLS-1$
        Check.notNull(e.getPendingChange(), "e.getPendingChange()"); //$NON-NLS-1$

        displayPendingChange(e.getPendingChange(), Messages.getString("Command.OnCheckingInFormat")); //$NON-NLS-1$
    }

    @Override
    public void onBeforeShelve(final PendingChangeEvent e) {
        Check.notNull(e, "e"); //$NON-NLS-1$
        Check.notNull(e.getPendingChange(), "e.getPendingChange()"); //$NON-NLS-1$

        displayPendingChange(e.getPendingChange(), Messages.getString("Command.OnShelvingFormat")); //$NON-NLS-1$
    }

    @Override
    public void onUndonePendingChange(final PendingChangeEvent e) {
        Check.notNull(e, "e"); //$NON-NLS-1$
        Check.notNull(e.getPendingChange(), "e.getPendingChange()"); //$NON-NLS-1$

        displayPendingChange(e.getPendingChange(), Messages.getString("Command.OnUndoingFormat")); //$NON-NLS-1$
    }

    @Override
    public void onNewPendingChange(final PendingChangeEvent e) {
        Check.notNull(e, "e"); //$NON-NLS-1$
        Check.notNull(e.getPendingChange(), "e.getPendingChange()"); //$NON-NLS-1$

        displayPendingChange(e.getPendingChange(), Messages.getString("Command.OnNewPendingChangeFormatSKIPVALIDATE")); //$NON-NLS-1$
    }

    @Override
    public void onDestroy(final DestroyEvent event) {
        final Item destroyedItem = event.getDestroyedItem();
        String itemString = destroyedItem.getServerItem();
        if (destroyedItem.getDeletionID() != 0) {
            itemString += ";X" + destroyedItem.getDeletionID(); //$NON-NLS-1$
        }
        getDisplay().printLine(MessageFormat.format(Messages.getString("Command.DestroyedFormat"), itemString)); //$NON-NLS-1$
    }

    /**
     * Displays the pending change information to the user. Unifies code that
     * should run during onNewPendingChange, onUndonePendingChange, etc.
     *
     * @param change
     *        the change to display (if null, ignored).
     * @param message
     *        a format string for MessageFormat.format(), correct for the null,
     *        the change's item name is printed. The message will be built with
     *        the following parameters: {0} = operation name, {1} = item name.
     */
    protected void displayPendingChange(final PendingChange change, final String messageFormat) {
        if (change != null) {
            if (xmlWriter != null) {
                try {
                    displayPendingChangeAsXML(change);
                } catch (final SAXException e) {
                    log.error("XML exception", e); //$NON-NLS-1$
                    e.printStackTrace();
                }

                return;
            }

            String folder;
            String item;

            String localItem = change.getLocalItem();
            String serverItem = change.getServerItem();

            /*
             * If the change is an undone rename (and the change type is not
             * undelete), we need to swap the source and destination for them to
             * make sense to the user.
             */
            if (change.isUndone()
                && change.getChangeType().contains(ChangeType.RENAME)
                && !change.getChangeType().contains(ChangeType.UNDELETE)) {
                localItem = change.getSourceLocalItem();
                serverItem = change.getSourceServerItem();
            }

            if (localItem != null) {
                folder = LocalPath.getDirectory(localItem);
                item = LocalPath.getFileName(localItem);

                /*
                 * Microsoft's client prints things relative to the current
                 * directory.
                 */
                String shortFolder = LocalPath.makeRelative(folder, LocalPath.getCurrentWorkingDirectory());
                if (shortFolder.length() == 0) {
                    shortFolder = LocalPath.getCurrentWorkingDirectory();
                }

                /*
                 * If this is the first change we're printing or we've changed
                 * directories, we need to print the new directory. If the
                 * directory is the same as the current directory, don't print
                 * it at all.
                 */
                if ((lastPendingChangeDirectory == null
                    && !LocalPath.equals(folder, LocalPath.getCurrentWorkingDirectory()))
                    || (lastPendingChangeDirectory != null
                        && (ServerPath.isServerPath(lastPendingChangeDirectory)
                            || !LocalPath.equals(lastPendingChangeDirectory, folder)))) {
                    if (lastPendingChangeDirectory != null) {
                        getDisplay().printLine(""); //$NON-NLS-1$
                    }

                    getDisplay().printLine(shortFolder + ":"); //$NON-NLS-1$
                }
            } else if (serverItem != null) {
                folder = ServerPath.getParent(serverItem);
                item = ServerPath.getFileName(serverItem);

                /*
                 * If this is the first change we're printing or we've changed
                 * server directories, we need to print the new directory.
                 */
                if (lastPendingChangeDirectory == null
                    || !ServerPath.isServerPath(folder)
                    || !ServerPath.equals(lastPendingChangeDirectory, folder)) {
                    if (lastPendingChangeDirectory != null) {
                        getDisplay().printLine(""); //$NON-NLS-1$
                    }

                    getDisplay().printLine(folder + ":"); //$NON-NLS-1$
                }
            } else {
                /*
                 * Microsoft's implementation mentions this case happening for
                 * odd rename change undo operations.
                 */
                if (change.getLocalItem() != null) {
                    item = LocalPath.makeRelative(change.getLocalItem(), LocalPath.getCurrentWorkingDirectory());
                    if (item.length() == 0) {
                        item = LocalPath.getCurrentWorkingDirectory();
                    }
                } else if (change.getSourceServerItem() != null) {
                    item = change.getSourceServerItem();
                } else {
                    /*
                     * This is all we have.
                     */
                    item = change.getServerItem();
                }

                folder = null;
                // TODO How does this affect the item string?
                // item = Resources.Format(Resources.UndoMovedTo, item);
            }

            lastPendingChangeDirectory = folder;

            if (messageFormat != null) {
                getDisplay().printLine(MessageFormat.format(messageFormat, new Object[] {
                    change.getChangeType().toUIString(false, change),
                    item
                }));
            } else {
                // Print something, at least the item name.
                getDisplay().printLine(item);
            }
        }
    }

    private void displayPendingChangeAsXML(final PendingChange change) throws SAXException {
        Check.notNull(change, "change"); //$NON-NLS-1$
        Check.notNull(xmlWriter, "this.xmlWriter"); //$NON-NLS-1$

        final AttributesImpl changeAttributes = new AttributesImpl();

        final String localItem;
        final String serverItem;

        /*
         * If the change is an undone rename (and the change type is not
         * undelete), we need to swap the source and destination for them to
         * make sense to the user.
         */
        if (change.isUndone()
            && change.getChangeType().contains(ChangeType.RENAME)
            && !change.getChangeType().contains(ChangeType.UNDELETE)) {
            localItem = change.getSourceLocalItem();
            serverItem = change.getSourceServerItem();
        } else {
            localItem = change.getLocalItem();
            serverItem = change.getServerItem();
        }

        changeAttributes.addAttribute("", "", CommonXMLNames.SERVER_ITEM, "CDATA", serverItem); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        if (localItem != null) {
            changeAttributes.addAttribute("", "", CommonXMLNames.LOCAL_ITEM, "CDATA", localItem); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        changeAttributes.addAttribute("", "", CommonXMLNames.VERSION, "CDATA", Integer.toString(change.getVersion())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        changeAttributes.addAttribute(
            "", //$NON-NLS-1$
            "", //$NON-NLS-1$
            CommonXMLNames.DATE,
            "CDATA", //$NON-NLS-1$
            SimpleXMLWriter.ISO_DATE_FORMAT.format(change.getCreationDate().getTime()));
        changeAttributes.addAttribute("", "", CommonXMLNames.LOCK, "CDATA", change.getLockLevelName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        changeAttributes.addAttribute(
            "", //$NON-NLS-1$
            "", //$NON-NLS-1$
            CommonXMLNames.CHANGE_TYPE,
            "CDATA", //$NON-NLS-1$
            change.getChangeType().toUIString(false, change));

        /*
         * If the source server item and server item paths differ, we have a
         * rename, move, etc., and want to print the source item.
         */
        if (change.getSourceServerItem() != null
            && !ServerPath.equals(change.getSourceServerItem(), change.getServerItem())) {
            changeAttributes.addAttribute(
                "", //$NON-NLS-1$
                "", //$NON-NLS-1$
                CommonXMLNames.SOURCE_SERVER_ITEM,
                "CDATA", //$NON-NLS-1$
                change.getSourceServerItem());
        }

        if (change.getItemType() == ItemType.FILE
            && change.getEncoding() != VersionControlConstants.ENCODING_UNCHANGED) {
            changeAttributes.addAttribute(
                "", //$NON-NLS-1$
                "", //$NON-NLS-1$
                CommonXMLNames.FILE_TYPE,
                "CDATA", //$NON-NLS-1$
                new FileEncoding(change.getEncoding()).getName());
        }

        if (change.getDeletionID() != 0) {
            changeAttributes.addAttribute(
                "", //$NON-NLS-1$
                "", //$NON-NLS-1$
                CommonXMLNames.DELETION_ID,
                "CDATA", //$NON-NLS-1$
                Integer.toString(change.getDeletionID()));
        }

        xmlWriter.startElement("", "", CommonXMLNames.PENDING_CHANGE, changeAttributes); //$NON-NLS-1$ //$NON-NLS-2$
        xmlWriter.endElement("", "", CommonXMLNames.PENDING_CHANGE); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Parses qualified items (item specs with optional versions, version
     * ranges, or deletion specifiers) from the free arguments.
     *
     * @param defaultVersion
     *        the default version spec to create the new QualifiedItems with if
     *        the free argument does not specify a version (if null, null is set
     *        on the qualified items).
     * @param allowVersionRange
     *        if true, allow the free arguments to specify a version range
     *        instead of a single version.
     * @param startIndex
     *        the free argument array index (0 is the first element) at which to
     *        start parsing as qualified items. Useful to skip some arguments
     *        that may have already been parsed as non-qualified items (e.g.
     *        label name).
     *
     * @return an array of qualified items parsed from the free arguments.
     */
    protected QualifiedItem[] parseQualifiedItems(
        final VersionSpec defaultVersion,
        final boolean allowVersionRange,
        final int startIndex) {
        return parseQualifiedItems(getFreeArguments(), defaultVersion, allowVersionRange, startIndex);
    }

    /**
     * Parses qualified items (item specs with optional versions, version
     * ranges, or deletion specifiers) from the given strings.
     *
     * @param arguments
     *        the arguments to parse as {@link QualifiedItem}s (not null).
     * @param defaultVersion
     *        the default version spec to create the new {@link QualifiedItem}s
     *        with if the argument does not specify a version (if null, null is
     *        set on the qualified items).
     * @param allowVersionRange
     *        if true, allow the free arguments to specify a version range
     *        instead of a single version.
     * @param startIndex
     *        the argument array index (0 is the first element) at which to
     *        start parsing as qualified items. Useful to skip some arguments
     *        that may have already been parsed as non-qualified items (e.g.
     *        label name).
     *
     * @return an array of qualified items parsed from the given argument
     *         strings.
     */
    protected QualifiedItem[] parseQualifiedItems(
        final String[] arguments,
        final VersionSpec defaultVersion,
        final boolean allowVersionRange,
        final int startIndex) {
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$

        final List<QualifiedItem> items = new ArrayList<QualifiedItem>(arguments.length);

        for (int i = startIndex; i < arguments.length; i++) {
            final String arg = arguments[i];
            if (!StringUtil.isNullOrEmpty(arg)) {
                try {
                    /*
                     * The constructor for QualifiedItem will canonialize the
                     * local path we supply (the arg).
                     */
                    final QualifiedItem qi = new QualifiedItem(
                        arg,
                        VersionControlConstants.AUTHENTICATED_USER,
                        defaultVersion,
                        allowVersionRange);

                    items.add(qi);
                } catch (final VersionSpecParseException e) {
                    reportWrongArgument(arg, e);
                } catch (final LabelSpecParseException e) {
                    reportWrongArgument(arg, e);
                }
            }
        }

        return items.toArray(new QualifiedItem[items.size()]);
    }

    private void reportWrongArgument(final String arg, final Exception e) {
        final String messageFormat = Messages.getString("Command.ArgumentSkippedFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, arg, e.getLocalizedMessage());
        getDisplay().printErrorLine(message);
        setExitCode(ExitCode.PARTIAL_SUCCESS);
    }

    /**
     * Gets the pending changes that match the free arguments given for this
     * command. Warnings are printed for each free argument which fails to match
     * a pending change. If any non-wildcard argument fails to match a change,
     * an empty array is returned (in addition to the warnings printed).
     *
     * @param workspace
     *        the workspace to use when matching pending changes.
     * @param recursive
     *        whether the free arguments should match changes recursively.
     * @return an array of matched changes, an empty array if any argument
     *         failed to match a path.
     * @throws ServerPathFormatException
     *         when the format of one of the free arguments is detected to be a
     *         server path, but is in an invalid format.
     */
    protected PendingChange[] getPendingChangesMatchingFreeArguments(final Workspace workspace, final boolean recursive)
        throws ServerPathFormatException {
        return getPendingChangesMatchingLocalPaths(workspace, recursive, getFreeArguments());
    }

    /**
     * Gets the pending changes that match the given local paths. Warnings are
     * printed for each free argument which fails to match a pending change. If
     * any non-wildcard argument fails to match a change, an empty array is
     * returned (in addition to the warnings printed).
     *
     * @param workspace
     *        the workspace to use when matching pending changes.
     * @param recursive
     *        whether the free arguments should match changes recursively.
     * @param localPaths
     *        the local paths to find changes for.
     * @return an array of matched changes, an empty array if any argument
     *         failed to match a path.
     * @throws TFSException
     * @throws ServerPathFormatException
     *         when the format of one of the free arguments is detected to be a
     *         server path, but is in an invalid format.
     */
    protected PendingChange[] getPendingChangesMatchingLocalPaths(
        final Workspace workspace,
        final boolean recursive,
        final String[] localPaths) throws ServerPathFormatException {
        final PendingSet pendingChangeSet = workspace.getPendingChanges();

        if (pendingChangeSet == null) {
            return null;
        }

        final PendingChange[] pendingChanges = pendingChangeSet.getPendingChanges();

        if (pendingChanges == null || pendingChanges.length == 0) {
            return null;
        }

        // The matched arguments (results of matching below).
        final MatchedFileArgument[] matchedArguments = new MatchedFileArgument[localPaths.length];
        final ArrayList<PendingChange> matchedChanges = new ArrayList<PendingChange>();

        for (int i = 0; i < pendingChanges.length; i++) {
            final PendingChange change = pendingChanges[i];
            if (change == null) {
                continue;
            }

            for (int j = 0; j < localPaths.length; j++) {
                /*
                 * Step 1: Figure out which local path from the change we should
                 * use.
                 */
                final String changePath;
                if (ServerPath.isServerPath(localPaths[j])) {
                    changePath = change.getServerItem();
                } else if (change.getLocalItem() != null) {
                    changePath = change.getLocalItem();
                } else if (change.getSourceLocalItem() != null) {
                    changePath = change.getSourceLocalItem();
                } else {
                    /*
                     * This change doesn't have any paths associated with it, so
                     * we can't match it. Not an error.
                     */
                    continue;
                }

                /*
                 * Step 2: Allocate a matched argument class (if needed).
                 *
                 * Since we may evaluate a free argument multiple times (against
                 * multiple changes), we only allocate an argument match class
                 * for this item if one does not already exist.
                 */
                if (matchedArguments[j] == null) {
                    matchedArguments[j] = new MatchedFileArgument();
                    matchedArguments[j].exactString = localPaths[j];

                    /*
                     * Transform a server path into a MatchedFileArgument.
                     */
                    if (ServerPath.isServerPath(matchedArguments[j].exactString)) {
                        matchedArguments[j].isServerItem = true;

                        /*
                         * Always break a server path into file and folder
                         * parts. We'll search for the item both as a file and a
                         * folder below, because we can't know what type the
                         * user is referring to (without hitting the server).
                         */
                        matchedArguments[j].folderPart = ServerPath.getParent(matchedArguments[j].exactString);
                        matchedArguments[j].filePart = ServerPath.getFileName(matchedArguments[j].exactString);

                        matchedArguments[j].fullPath = ServerPath.canonicalize(matchedArguments[j].exactString);
                    }

                    /*
                     * Transform a local path into a MatchedFileArgument.
                     */
                    if (!ServerPath.isServerPath(matchedArguments[j].exactString)) {
                        if (LocalPath.equals(changePath, LocalPath.canonicalize(matchedArguments[j].exactString))) {
                            /*
                             * In the local path case, we may have a pending
                             * change where a change describes a directory that
                             * doesn't yet exist (the target of a branch, for
                             * example), but a checkin with that (not yet
                             * created) target as a free argument should match,
                             * so we handle that here.
                             */

                            /*
                             * We can ask the change directly if it's a file or
                             * folder (since the disk item doesn't exist, we
                             * couldn't test it that way if we wanted to).
                             */
                            if (change.getItemType() == ItemType.FOLDER) {
                                matchedArguments[j].folderPart = change.getLocalItem();
                            } else {
                                // It's a file.
                                matchedArguments[j].filePart =
                                    LocalPath.getLastComponent(LocalPath.canonicalize(localPaths[j]));
                                matchedArguments[j].folderPart =
                                    LocalPath.getDirectory(LocalPath.canonicalize(localPaths[j]));
                            }
                        } else if (new File(matchedArguments[j].exactString).exists()
                            && new File(matchedArguments[j].exactString).isDirectory()) {
                            // The argument is a local directory.
                            matchedArguments[j].filePart = null;
                            matchedArguments[j].folderPart = LocalPath.canonicalize(matchedArguments[j].exactString);
                        } else {
                            // The argument is a local file.
                            matchedArguments[j].filePart =
                                LocalPath.getLastComponent(LocalPath.canonicalize(localPaths[j]));
                            matchedArguments[j].folderPart =
                                LocalPath.getDirectory(LocalPath.canonicalize(localPaths[j]));
                        }

                        matchedArguments[j].fullPath = LocalPath.canonicalize(matchedArguments[j].exactString);
                    }
                }

                /*
                 * Step 3: Test for a match between the change and the argument.
                 *
                 * If this argument is a server path, we have to test it in both
                 * direct path match (possibly with recursion) and path/wildcard
                 * pattern combination (also possibly with recursion) so we can
                 * be sure to match every change. For local paths, we always do
                 * the match respecting wildcards.
                 *
                 * This is the behavior of Microsoft's client, and we should
                 * match it.
                 */
                boolean matchesAsServerItem = false;
                boolean matchesAsServerWildcard = false;
                boolean matchesAsLocalWildcard = false;
                if (matchedArguments[j].isServerItem) {
                    matchesAsServerItem =
                        ServerPath.matchesWildcard(changePath, matchedArguments[j].fullPath, null, recursive);
                    matchesAsServerWildcard = ServerPath.matchesWildcard(
                        changePath,
                        matchedArguments[j].folderPart,
                        matchedArguments[j].filePart,
                        recursive);
                } else {
                    matchesAsLocalWildcard = LocalPath.matchesWildcard(
                        changePath,
                        matchedArguments[j].folderPart,
                        matchedArguments[j].filePart,
                        recursive);
                }

                if ((matchedArguments[j].isServerItem && (matchesAsServerItem || matchesAsServerWildcard))
                    || (!matchedArguments[j].isServerItem && matchesAsLocalWildcard)) {
                    // Only add if we didn't just add it (input list was
                    // sorted).
                    if (matchedChanges.size() == 0 || matchedChanges.get(matchedChanges.size() - 1) != change) {
                        matchedChanges.add(change);
                    }

                    // Mark it matched.
                    matchedArguments[j].isMatched = true;
                }

                /*
                 * Don't break so we can continue to catch other matches.
                 */
            }
        }

        // Make sure each argument matched something.
        boolean failedToMatchAll = false;
        for (int i = 0; i < matchedArguments.length; i++) {
            if (!matchedArguments[i].isMatched) {
                if (!ItemPath.isWildcard(matchedArguments[i].filePart)) {
                    final String messageFormat = Messages.getString("Command.ArgumentFailMatchNoWildcardFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, matchedArguments[i].exactString);
                    getDisplay().printErrorLine(message);

                    failedToMatchAll = true;
                } else {
                    // Just warn about the wildcards.
                    final String messageFormat = Messages.getString("Command.ArgumentFailMatchWildcardFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, localPaths[i]);
                    getDisplay().printErrorLine(message);
                }
            }
        }

        if (failedToMatchAll) {
            return new PendingChange[0];
        }

        return matchedChanges.toArray(new PendingChange[0]);
    }

    /**
     * Gets a version spec appropriate for the given path string. If the path is
     * a local path, a workspace spec is constructed using the given workspace
     * name and owner. If the path is a server path, an ALatestVersionSpec is
     * returned.
     *
     * @param itemPath
     *        the item path to check (not null or empty).
     * @param workspaceName
     *        the workspace name to use if the path is local (not null or
     *        empty).
     * @param workspaceOwner
     *        the workspace owner to use if the path is local (not null or
     *        empty).
     * @param workspaceOwnerDisplayName
     *        the display name for a workspace owner to use if the path is local
     *        (not null or empty)
     * @return an AWorkspaceVersionSpec if the given item path is local, an
     *         ALatestVersionSpec if the path was a server path.
     */
    public VersionSpec getVersionSpecForPath(
        final String itemPath,
        final String workspaceName,
        final String workspaceOwner,
        final String workspaceOwnerDisplayName) {
        Check.notNullOrEmpty(itemPath, "itemPath"); //$NON-NLS-1$
        Check.notNullOrEmpty(workspaceName, "workspaceName"); //$NON-NLS-1$
        Check.notNullOrEmpty(workspaceOwner, "workspaceOwner"); //$NON-NLS-1$
        Check.notNullOrEmpty(workspaceOwnerDisplayName, "workspaceOwnerDisplayName"); //$NON-NLS-1$

        if (!ServerPath.isServerPath(itemPath)) {
            return new WorkspaceVersionSpec(workspaceName, workspaceOwner, workspaceOwnerDisplayName);
        } else {
            return LatestVersionSpec.INSTANCE;
        }
    }

    /**
     * Sets the {@link SimpleXMLWriter} to use when writing XML to the display.
     * Set a null writer to print normal text (the default).
     *
     * @param xmlWriter
     *        the {@link SimpleXMLWriter} to use when writing XML to the
     *        display, or null to print normal text.
     */
    public void setXMLWriter(final SimpleXMLWriter xmlWriter) {
        this.xmlWriter = xmlWriter;
    }

    protected void reportBadOptionCombinationIfPresent(
        final Class<? extends Option> firstClass,
        final Class<? extends Option> secondClass) throws InvalidOptionException {
        final Option firstOption = findOptionType(firstClass);
        final Option secondOption = findOptionType(secondClass);

        if (firstOption != null && secondOption != null) {
            throw new InvalidOptionException(
                MessageFormat.format(
                    Messages.getString("Command.InvalidOptionCombinationFormat"), //$NON-NLS-1$
                    OptionsMap.getPreferredOptionPrefix(),
                    firstOption.getMatchedAlias(),
                    OptionsMap.getPreferredOptionPrefix(),
                    secondOption.getMatchedAlias()));
        }
    }
}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.prompt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.alm.auth.Authenticator;
import com.microsoft.alm.auth.PromptBehavior;
import com.microsoft.alm.auth.oauth.DeviceFlowResponse;
import com.microsoft.alm.auth.oauth.OAuth2Authenticator;
import com.microsoft.alm.auth.pat.VstsPatAuthenticator;
import com.microsoft.alm.helpers.Action;
import com.microsoft.alm.provider.Options;
import com.microsoft.alm.provider.UserPasswordCredentialProvider;
import com.microsoft.alm.secret.Credential;
import com.microsoft.alm.secret.Token;
import com.microsoft.alm.secret.TokenPair;
import com.microsoft.alm.storage.InsecureInMemoryStore;
import com.microsoft.alm.storage.SecretStore;
import com.microsoft.tfs.client.clc.EnvironmentVariables;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.console.input.Input;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.jni.ConsoleUtils;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;

/**
 * Static methods to ask the user for information (questions, etc.).
 */
public final class Prompt {

    private static final Log log = LogFactory.getLog(Prompt.class);

    /**
     * Constants for OAuth2 Interactive Browser logon flow
     */
    private static final String CLIENT_ID = "97877f11-0fc6-4aee-b1ff-febb0519dd00"; //$NON-NLS-1$
    private static final String REDIRECT_URL = "https://java.visualstudio.com"; //$NON-NLS-1$

    /**
     * Maps a {@link QuestionType} to a localized string for use in the prompt
     * suffix (the "(Yes/No/All)" part).
     */
    private final static Map<QuestionType, String> questionSuffixes = new HashMap<QuestionType, String>();

    /**
     * Maps a {@link QuestionResponse} to a localized string for use in parsing
     * the user's response.
     */
    private final static Map<QuestionResponse, String> questionResponses = new HashMap<QuestionResponse, String>();

    static {
        /*
         * Load the resource strings into the maps. Visual Studio puts the
         * resource keys in the maps, but loading the resources when this class
         * is loaded (static initialization) makes the code more
         * straight-forward.
         */

        questionSuffixes.put(QuestionType.YES_NO, Messages.getString("Prompt.QuestionYesNoSuffix")); //$NON-NLS-1$
        questionSuffixes.put(QuestionType.YES_NO_ALL, Messages.getString("Prompt.QuestionYesNoAllSuffix")); //$NON-NLS-1$
        questionSuffixes.put(QuestionType.YES_NO_CANCEL, Messages.getString("Prompt.QuestionYesNoCancelSuffix")); //$NON-NLS-1$
        questionSuffixes.put(QuestionType.YES_NO_ALL_CANCEL, Messages.getString("Prompt.QuestionYesNoAllCancelSuffix")); //$NON-NLS-1$

        questionResponses.put(QuestionResponse.YES, Messages.getString("Prompt.AnswerYes")); //$NON-NLS-1$
        questionResponses.put(QuestionResponse.NO, Messages.getString("Prompt.AnswerNo")); //$NON-NLS-1$
        questionResponses.put(QuestionResponse.ALL, Messages.getString("Prompt.AnswerAll")); //$NON-NLS-1$
        questionResponses.put(QuestionResponse.CANCEL, Messages.getString("Prompt.AnswerCancel")); //$NON-NLS-1$
    }

    /**
     * Asks the user a question and returns the response.
     *
     * @param display
     *        the {@link Display} to write the prompts to (must not be
     *        <code>null</code>)
     * @param input
     *        the {@link Input} to read responses from (must not be
     *        <code>null</code>)
     * @param questionType
     *        the type of question to ask (must not be <code>null</code>)
     * @param question
     *        the question string, which may include multiple lines, but should
     *        end so the question suffix can be appended after a single space
     *        (must not be <code>null</code>)
     * @return the {@link QuestionResponse} the user provided, <code>null</code>
     *         if there was an error reading the user's response
     */
    public static QuestionResponse askQuestion(
        final Display display,
        final Input input,
        final QuestionType questionType,
        final String question) {
        Check.notNull(display, "display"); //$NON-NLS-1$
        Check.notNull(input, "input"); //$NON-NLS-1$
        Check.notNull(questionType, "questionType"); //$NON-NLS-1$
        Check.notNull(question, "question"); //$NON-NLS-1$

        while (true) {
            display.print(question);
            display.print(" "); //$NON-NLS-1$
            display.print(questionSuffixes.get(questionType));
            display.flush();

            // Do not close this stream, we need to keep the InputStream open.
            final BufferedReader reader = new BufferedReader(new InputStreamReader(input.getInputStream()));

            try {
                final String response = reader.readLine();

                if (response == null) {
                    return null;
                }

                if (response.toLowerCase().startsWith(questionResponses.get(QuestionResponse.YES).toLowerCase())) {
                    return QuestionResponse.YES;
                } else if (response.toLowerCase().startsWith(
                    questionResponses.get(QuestionResponse.NO).toLowerCase())) {
                    return QuestionResponse.NO;
                } else if ((questionType == QuestionType.YES_NO_ALL || questionType == QuestionType.YES_NO_ALL_CANCEL)
                    && response.toLowerCase().startsWith(questionResponses.get(QuestionResponse.ALL).toLowerCase())) {
                    return QuestionResponse.ALL;
                } else if ((questionType == QuestionType.YES_NO_CANCEL
                    || questionType == QuestionType.YES_NO_ALL_CANCEL)
                    && response.toLowerCase().startsWith(
                        questionResponses.get(QuestionResponse.CANCEL).toLowerCase())) {
                    return QuestionResponse.CANCEL;
                } else {
                    display.printLine(Messages.getString("Prompt.InvalidResponse")); //$NON-NLS-1$
                }
            } catch (final IOException e) {
                return null;
            }
        }
    }

    /**
     * Makes an {@link UsernamePasswordCredentials} from the given fields,
     * prompting for any which are <code>null</code>.
     *
     * @param display
     *        the display to use (must not be <code>null</code>)
     * @param input
     *        the input to use (must not be <code>null</code>)
     * @param username
     *        the username to use in the credentials (<code>null</code> or empty
     *        to prompt the user)
     * @param password
     *        the username to use in the credentials (<code>null</code> to
     *        prompt the user)
     * @return an {@link UsernamePasswordCredentials} object or
     *         <code>null</code> if there was an error writing to the display or
     *         reading from the input or if the data read from the user was
     *         insufficient to create {@link UsernamePasswordCredentials} with
     *         (for example, empty username)
     */
    public static UsernamePasswordCredentials getCredentials(
        final Display display,
        final Input input,
        String username,
        String password) {
        Check.notNull(display, "display"); //$NON-NLS-1$
        Check.notNull(input, "input"); //$NON-NLS-1$

        // Username must be non-empty
        if (username == null || username.length() == 0) {
            username = Prompt.promptForUsername(display, input);

            if (username == null || username.length() == 0) {
                return null;
            }
        } else {
            display.printLine(MessageFormat.format(Messages.getString("Prompt.DisplayUsernameFormat"), username)); //$NON-NLS-1$
        }

        if (password == null) {
            password = Prompt.promptForPassword(display, input);

            if (password == null) {
                return null;
            }
        }

        return new UsernamePasswordCredentials(username, password);
    }

    /**
     * Makes an {@link UsernamePasswordCredentials} from OAuth2 flow
     * 
     * Prompting the user with a browser
     * 
     * @param display
     *        the display to use (must not be <code>null</code>)
     * 
     * @param persistCredentials
     *        caller intend to persist the retrieved credential
     * 
     * @return an {@link UsernamePasswordCredentials} object or
     *         <code>null</code> if there was an error retrieving the credential
     *         from OAuth2 flow. (for example, user closed the browser, or is
     *         running on a Java runtime that does not support JavaFx)
     */
    public static UsernamePasswordCredentials getCredentialsInteractively(
        final URI serverURI,
        final Display display,
        final boolean persistCredentials) {
        Check.notNull(display, "display"); //$NON-NLS-1$
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        log.debug("Interactively retrieving credential based on oauth2 flow for " + serverURI.toString()); //$NON-NLS-1$
        final SecretStore<TokenPair> accessTokenStore = new InsecureInMemoryStore<TokenPair>();
        final SecretStore<Token> tokenStore = new InsecureInMemoryStore<Token>();

        final Action<DeviceFlowResponse> deviceFlowCallback = new Action<DeviceFlowResponse>() {
            @Override
            public void call(final DeviceFlowResponse response) {
                display.printLine("------------------------------------"); //$NON-NLS-1$
                display.printLine(Messages.getString("Command.DeviceFlowCallbackTitle")); //$NON-NLS-1$
                display.printLine("------------------------------------"); //$NON-NLS-1$
                display.printLine(Messages.getString("Command.DeviceFlowCallbackInstructionUrl")); //$NON-NLS-1$
                display.printLine(response.getVerificationUri().toString());
                display.printLine(
                    MessageFormat.format(
                        Messages.getString("Command.DeviceFlowCallbackInstructionCodeFormat"), //$NON-NLS-1$
                        response.getExpiresIn() / 60));
                display.printLine(response.getUserCode());
                display.printLine(Messages.getString("Command.DeviceFlowCallbackInstructionContinue")); //$NON-NLS-1$

                display.printLine(
                    MessageFormat.format(
                        Messages.getString("Command.DeviceFlowCallbackInstructionBypassFormat"), //$NON-NLS-1$
                        EnvironmentVariables.BYPASS_INTERACTIVE_BROWSER_LOGIN));
            }
        };

        final OAuth2Authenticator oauth2Authenticator =
            OAuth2Authenticator.getAuthenticator(CLIENT_ID, REDIRECT_URL, accessTokenStore, deviceFlowCallback);

        final Authenticator authenticator;
        final Options options = Options.getDefaultOptions();

        if (persistCredentials) {
            log.debug("Trying to persist credential, generating a PAT"); //$NON-NLS-1$
            /*
             * If this credential is to be persisted, then let's create a PAT
             */
            authenticator = new VstsPatAuthenticator(oauth2Authenticator, tokenStore);
            options.patGenerationOptions.displayName = getPATDisplayName();

        } else {
            log.debug("Do not try to persist, generating oauth2 token."); //$NON-NLS-1$
            /*
             * Not persisting this credential, simply create an oauth2 token
             */
            authenticator = oauth2Authenticator;
        }

        final UserPasswordCredentialProvider provider = new UserPasswordCredentialProvider(authenticator);

        final Credential tokenCreds = provider.getCredentialFor(serverURI, PromptBehavior.AUTO, options);

        if (tokenCreds != null && tokenCreds.Username != null && tokenCreds.Password != null) {
            return new UsernamePasswordCredentials(tokenCreds.Username, tokenCreds.Password);
        } else {
            log.warn(Messages.getString("Command.InteractiveAuthenticationFailedDetailedLog1")); //$NON-NLS-1$
            log.warn(Messages.getString("Command.InteractiveAuthenticationFailedDetailedLog2")); //$NON-NLS-1$
            log.warn(Messages.getString("Command.InteractiveAuthenticationFailedDetailedLog3")); //$NON-NLS-1$

            display.printLine(MessageFormat.format(
                Messages.getString("Command.InteractiveAuthenticationFailedFormat"), //$NON-NLS-1$
                EnvironmentVariables.BYPASS_INTERACTIVE_BROWSER_LOGIN));
        }

        // Failed to get credential, return null
        return null;
    }

    /**
     * @return <code>true</code> by default <code>false</code> if user
     *         explicitly disables browser interactive login
     */
    public static boolean interactiveLoginAllowed() {
        return !EnvironmentVariables.getBoolean(EnvironmentVariables.BYPASS_INTERACTIVE_BROWSER_LOGIN, false);
    }

    private static String getPATDisplayName() {
        // following IntelliJ Plugin's format
        final String formatter = "TF from: %s on: %s"; //$NON-NLS-1$
        final String machineName = LocalHost.getShortName();

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //$NON-NLS-1$
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
        final String time = dateFormat.format(new Date());

        return String.format(formatter, machineName, time);
    }

    private static String promptForUsername(final Display display, final Input input) {
        return readLine(display, input, Messages.getString("Prompt.LoginPromptUsername"), true); //$NON-NLS-1$
    }

    private static String promptForPassword(final Display display, final Input input) {
        return readLine(display, input, Messages.getString("Prompt.LoginPromptPassword"), false); //$NON-NLS-1$
    }

    /**
     * Reads a line from this command's {@link Input}.
     *
     * @param prompt
     *        the line to print (without automatic newline) as the prompt the
     *        input (not null). Provide the full prompt string including any
     *        spaces or separators, for example:
     *
     *        <pre>
     *        &quot;Username: &quot;
     *        </pre>
     *
     * @param echo
     *        if true, what the user types is echoed. If false, it is not
     *        echoed.
     * @return the text the user typed, not including line endings, or null if
     *         an error occured writing the prompt or reading the input.
     */
    public static String readLine(final Display display, final Input input, final String prompt, final boolean echo) {
        Check.notNull(prompt, "prompt"); //$NON-NLS-1$

        final PrintStream out = display.getPrintStream();
        if (out == null) {
            return null;
        }

        final InputStream in = input.getInputStream();
        if (in == null) {
            return null;
        }

        // Do not close this stream, we need to keep the InputStream open.
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String ret;
        try {
            if (echo == false) {
                if (ConsoleUtils.getInstance().disableEcho() == false) {
                    display.printErrorLine(Messages.getString("Prompt.LocalEchoCouldNotBeDisabled")); //$NON-NLS-1$
                }
            }

            out.print(prompt);

            try {
                ret = reader.readLine();
            } catch (final IOException e) {
                /*
                 * Don't log because it might contain sensitive information.
                 */
                return null;
            }
        } finally {
            if (echo == false) {
                ConsoleUtils.getInstance().enableEcho();

                /*
                 * Since echo was off, we didn't echo the user's newline, so
                 * provide one here to even out the lines.
                 */
                out.println();
            }

            /*
             * Note we don't close the BufferedReader because we need to keep
             * the input stream open for further processing
             */
        }

        return ret;
    }

}

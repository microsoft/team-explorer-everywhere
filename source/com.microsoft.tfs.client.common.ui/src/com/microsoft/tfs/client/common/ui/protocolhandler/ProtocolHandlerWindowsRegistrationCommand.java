// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.protocolhandler;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.util.SpecialFolders;
import com.microsoft.tfs.jni.RegistryKey;
import com.microsoft.tfs.jni.RootKey;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

public class ProtocolHandlerWindowsRegistrationCommand extends TFSCommand {
    private static final Log log = LogFactory.getLog(ProtocolHandlerWindowsRegistrationCommand.class);

    private final static String PROTOCOL_HANDLER_LAUNCHER_PROPERTY = "eclipse.launcher"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_REGISTRY_KEY =
        ProtocolHandler.PROTOCOL_HANDLER_SCHEME + "\\Shell\\Open\\Command"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_REGISTRY_PATH = "HKCR\\" + PROTOCOL_HANDLER_REGISTRY_KEY; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_REG_VALUE_TYPE = "REG_EXPAND_SZ"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_SCRIPT_PATH = "%USERPROFILE%\\.vsts\\latestIDE.cmd"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_REG_VALUE =
        MessageFormat.format("\"{0}\" \"%1\"", PROTOCOL_HANDLER_SCRIPT_PATH); //$NON-NLS-1$

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return Messages.getString("ProtocolHandlerRegistrationJob.CommandName"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorDescription() {
        return Messages.getString("ProtocolHandlerRegistrationJob.CommandErrorMessage"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLoggingDescription() {
        return Messages.getString("ProtocolHandlerRegistrationJob.CommandName", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus doRun(IProgressMonitor progressMonitor) throws Exception {
        Check.isTrue(Platform.isCurrentPlatform(Platform.WINDOWS));

        try {
            final String launcher = System.getProperty(PROTOCOL_HANDLER_LAUNCHER_PROPERTY, null);
            if (StringUtil.isNullOrEmpty(launcher)) {
                log.error(
                    "Unknown Eclipse application launcher. Cannot register the protocol handler with this application"); //$NON-NLS-1$
                return Status.CANCEL_STATUS;
            }

            updateRegistryIfNeeded(launcher);
            updateLauncherScriptIfNeeded(launcher);

        } catch (final InterruptedException e) {
            log.warn("Protocol handler registration has been cancelled."); //$NON-NLS-1$
        } catch (final Exception e) {
            log.error("Error accessing Windows registry:", e); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }

    /*
     * ----------------- Registry update functions ----------------
     */
    private void updateRegistryIfNeeded(final String launcher) throws IOException, InterruptedException {
        if (isRegistryUpdateNeeded()) {

            log.info(MessageFormat.format(
                "Registering {0} as the \"{1}\" protocol handler", //$NON-NLS-1$
                PROTOCOL_HANDLER_REG_VALUE,
                ProtocolHandler.PROTOCOL_HANDLER_SCHEME));

            final File script = createRegeditFile(launcher);

            final ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", //$NON-NLS-1$
                "/C", //$NON-NLS-1$
                "regedit.exe /s \"" + script.getPath() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
            final Process cmd = pb.start();
            int rc = cmd.waitFor();
            log.info("   rc = " + rc); //$NON-NLS-1$

            script.delete();
        }
    }

    private boolean isRegistryUpdateNeeded() {
        final RegistryKey handlerKey = new RegistryKey(RootKey.HKEY_CLASSES_ROOT, PROTOCOL_HANDLER_REGISTRY_KEY);

        if (!handlerKey.exists()) {
            return true;
        }

        final String value = getCurrentProtocolHandlerRegistryValue();
        return StringUtil.isNullOrEmpty(value) || !PROTOCOL_HANDLER_REG_VALUE.equalsIgnoreCase(value);
    }

    private File createRegeditFile(final String launcher) throws IOException {
        final File script = File.createTempFile("CreateKeys", ".reg"); //$NON-NLS-1$ //$NON-NLS-2$
        final PrintWriter writer = new PrintWriter(script);
        try {
            writer.println("Windows Registry Editor Version 5.00"); //$NON-NLS-1$
            writer.println(MessageFormat.format("[-HKEY_CLASSES_ROOT\\{0}]", ProtocolHandler.PROTOCOL_HANDLER_SCHEME)); //$NON-NLS-1$
            writer.println(MessageFormat.format("[HKEY_CLASSES_ROOT\\{0}]", PROTOCOL_HANDLER_REGISTRY_KEY)); //$NON-NLS-1$
            writer.print("@=hex(2):"); //$NON-NLS-1$
            writeHexValue(writer, PROTOCOL_HANDLER_REG_VALUE);
            writer.println();
            writer.flush();
        } finally {
            writer.close();
        }
        return script;
    }

    private void writeHexValue(PrintWriter writer, final String s) throws IOException {
        final byte[] b = s.getBytes("UTF_16LE"); //$NON-NLS-1$

        for (final byte x : b) {
            writer.write(String.format("%02x", x)); //$NON-NLS-1$
            writer.write(',');
        }

        // The value in the RegEdit script should be zero-terminated
        writer.write("00,00"); //$NON-NLS-1$
    }

    private String getCurrentProtocolHandlerRegistryValue() {
        BufferedReader stdout = null;
        String line;

        try {
            final ProcessBuilder pb = new ProcessBuilder(
                "reg", //$NON-NLS-1$
                "query", //$NON-NLS-1$
                PROTOCOL_HANDLER_REGISTRY_PATH,
                "/ve"); //$NON-NLS-1$
            pb.redirectErrorStream(true);

            final Process cmd = pb.start();
            int rc = cmd.waitFor();

            if (rc == 0) {
                stdout = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                while ((line = stdout.readLine()) != null) {
                    int idx = 0;
                    /* @formatter:off
                     * the output contains at most one line like 
                     * 
                     *    (Default)    REG_EXPAND_SZ    "%USERPROFILE%\.vsts\latestIDE.cmd" "%1"
                     * 
                     * @formatter:on
                     */
                    if ((idx = line.indexOf(PROTOCOL_HANDLER_REG_VALUE_TYPE)) < 0) {
                        continue;
                    }

                    return line.substring(idx + PROTOCOL_HANDLER_REG_VALUE_TYPE.length()).trim();
                }
            }
        } catch (final InterruptedException e) {
            log.warn("Protocol handler registration has been cancelled."); //$NON-NLS-1$
        } catch (final Exception e) {
            log.error("Error accessing Windows registry:", e); //$NON-NLS-1$
        } finally {
            tryClose(stdout);
        }

        return null;
    }
    /*
     * ------------------------------------------------------------
     */

    /*
     * ------------- Launcher script update functions -------------
     */
    private void updateLauncherScriptIfNeeded(final String launcher) {
        final String launcherScriptPath =
            PROTOCOL_HANDLER_SCRIPT_PATH.replace("%USERPROFILE%", SpecialFolders.getUserProfilePath()); //$NON-NLS-1$
        final File launcherScriptFile = new File(launcherScriptPath);
        final List<String> launcherScriptCommands = getLauncherScriptCommands(launcher);

        if (isLauncherScriptUpdateNeeded(launcherScriptFile, launcherScriptCommands)) {
            createLauncherCmdFile(launcherScriptFile, launcherScriptCommands);
        }
    }

    private boolean isLauncherScriptUpdateNeeded(final File launcherScriptFile, final List<String> newLines) {
        final List<String> oldLines = readLauncherCmdFile(launcherScriptFile);

        if (oldLines.size() != newLines.size()) {
            return true;
        }

        for (int i = 0; i < newLines.size(); i++) {
            if (!oldLines.get(i).equals(newLines.get(i))) {
                return true;
            }
        }

        return false;
    }

    private List<String> getLauncherScriptCommands(final String launcher) {
        return Arrays.asList(new String[] {
            "@rem version=1.0", //$NON-NLS-1$
            MessageFormat.format("@start \"\" \"{0}\" {1} %*", launcher, ProtocolHandler.PROTOCOL_HANDLER_ARG), //$NON-NLS-1$
        });
    }

    private List<String> readLauncherCmdFile(final File launcherScriptFile) {
        final List<String> lines = new ArrayList<String>();

        if (launcherScriptFile.exists()) {
            BufferedReader reader = null;
            String line;

            try {
                reader = new BufferedReader(new FileReader(launcherScriptFile));
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (final Exception e) {
                log.error(e);
            } finally {
                tryClose(reader);
            }
        }

        return lines;
    }

    private void createLauncherCmdFile(final File launcherScriptFile, final List<String> commands) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(launcherScriptFile);
            for (final String command : commands) {
                writer.println(command);
            }
        } catch (final FileNotFoundException e) {
            log.error(e);
        } finally {
            tryClose(writer);
        }
    }
    /*
     * ------------------------------------------------------------
     */

    private void tryClose(final Closeable file) {
        if (file != null) {
            try {
                file.close();
            } catch (final IOException e) {
                log.error(e);
            }
        }
    }
}

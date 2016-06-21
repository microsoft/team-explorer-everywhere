// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.utils;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.exceptions.BuildTypeFileParseException;
import com.microsoft.tfs.core.clients.build.internal.BuildTypeInfo;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.temp.TempStorageService;

public class BuildTypeUtil {
    private static final Log log = LogFactory.getLog(BuildTypeUtil.class);

    /**
     * Download the TFSBuild.proj file specified in the passed {@link Item},
     * then attempt to parse it for the values required for the returned
     * {@link BuildTypeInfo}.
     *
     * @param server
     *        the build server (must not be <code>null</code>)
     * @param item
     *        An {@link Item} representing the TFSBuild.proj file for the build
     *        type.
     * @return parsed values for the passed AItem.
     * @throws IOException
     *         if Exception occurred reading or parsing item.
     */
    public static BuildTypeInfo parseBuildTypeFile(final IBuildServer server, final Item item) throws IOException {
        // Download file to temp location
        Check.notNull(item, "item"); //$NON-NLS-1$

        final String buildTypeName = ServerPath.getFileName(ServerPath.getParent(item.getServerItem()));
        final String fileName = MessageFormat.format("{0}-{1}", buildTypeName, BuildConstants.PROJECT_FILE_NAME); //$NON-NLS-1$

        final File localBuildFile =
            item.downloadFileToTempLocation(server.getConnection().getVersionControlClient(), fileName);

        BuildTypeInfo info;

        try {
            info = parseBuildTypeInfo(buildTypeName, localBuildFile, item.getEncoding());
        } finally {
            try {
                localBuildFile.delete();
            } catch (final Exception e) {
                // We did our best, log and ignore.
                log.error(Messages.getString("BuildTypeUtil.ErrorDeletingTemporaryBuildProjectFile"), e); //$NON-NLS-1$
            }

            TempStorageService.getInstance().cleanUpItem(localBuildFile.getParentFile());
        }

        return info;
    }

    /**
     * Attempt to parse the specified file for values required to populated the
     * returned {@link BuildTypeInfo}
     *
     * @param buildTypeName
     *        name of the build type downloading.
     * @param localBuildFile
     *        local file to parse.
     * @param encoding
     *        suggested file encoding to use, passing <code>null</code> will
     *        mean a suggesting encoding of <code>UTF-8</code> will be used.
     * @return parsed values for the passed file.
     */
    public static BuildTypeInfo parseBuildTypeInfo(
        final String buildTypeName,
        final File localBuildFile,
        final FileEncoding encoding) throws IOException {
        final BasicBuildTypeParseHandler handler = new BasicBuildTypeParseHandler(buildTypeName);

        final SAXParserFactory factory = SAXParserFactory.newInstance();

        try {
            final SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(localBuildFile, handler);
        } catch (final SAXException e) {
            // We did our best - log and rethrow any exceptions...

            final String message =
                MessageFormat.format(
                    Messages.getString("BuildTypeUtil.SAXExceptionParsingFileFormat"), //$NON-NLS-1$
                    localBuildFile.getPath(),
                    e.getLocalizedMessage());

            log.error(message, e);
            throw new BuildTypeFileParseException(message, e);
        } catch (final ParserConfigurationException e) {
            // We did our best - log and rethrow any exceptions...

            final String message =
                MessageFormat.format(
                    Messages.getString("BuildTypeUtil.ParserConfigurationExceptionParsingFileFormat"), //$NON-NLS-1$
                    localBuildFile.getPath(),
                    e.getLocalizedMessage());

            log.error(message, e);
            throw new BuildTypeFileParseException(message, e);
        }

        return handler.getBuildTypeInfo();
    }
}
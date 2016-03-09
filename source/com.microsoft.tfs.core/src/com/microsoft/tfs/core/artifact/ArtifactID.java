// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.artifact;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * <p>
 * An artifact ID represents the unique, multi-part identifier of an object
 * stored in one of the data repositories of a component of Team Foundation
 * Server. Work items and version control changesets are examples of artifacts.
 * </p>
 * <p>
 * An artifact ID is composed of 3 parts:
 * </p>
 * <p>
 * <ol>
 * <li>a tool</li>
 * <li>an artifact type</li>
 * <li>a tool-specific identifier</li>
 * </ol>
 * </p>
 * <p>
 * An artifact ID can be represented by a URI. This URI is also known as a TFS
 * URI or an artifact URI. The URI contains all 3 parts of an artifact ID in the
 * following form:
 * </p>
 * <p>
 * <tt>vstfs:///tool/artifact-type/tool-specific-identifier</tt>
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class ArtifactID {
    /**
     * The prefix that all well-formed TFS URIs begin with.
     */
    public static final String VSTFS_PREFIX = "vstfs:///"; //$NON-NLS-1$

    /**
     * The separator used to separate the various parts of the artifact id
     * within a TFS URI.
     */
    public static final String URI_SEPARATOR = "/"; //$NON-NLS-1$

    /**
     * IMPLEMENTATION DETAIL (keep private) The encoding to use when URL
     * encoding/decoding
     */
    private static final String URL_ENCODING = "UTF-8"; //$NON-NLS-1$

    private final String tool;
    private final String artifactType;
    private final String toolSpecificId;

    /**
     * Create a new artifact id by specifying each of the parts individually.
     * The resulting artifact id may or may not be well-formed. You can check
     * for well-formedness by calling isWellFormed().
     *
     * @param tool
     *        the tool part of the artifact id
     * @param artifactType
     *        the artifact type part of the artifact id
     * @param toolSpecificId
     *        the tool specific id part of the artifact id
     */
    public ArtifactID(final String tool, final String artifactType, final String toolSpecificId) {
        this.tool = tool;
        this.artifactType = artifactType;
        this.toolSpecificId = toolSpecificId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ArtifactID) {
            final ArtifactID other = (ArtifactID) obj;

            return (tool == null ? other.tool == null : tool.equals(other.tool))
                && (artifactType == null ? other.artifactType == null : artifactType.equals(other.artifactType))
                && (toolSpecificId == null ? other.toolSpecificId == null
                    : toolSpecificId.equals(other.toolSpecificId));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (tool == null ? 0 : tool.hashCode())
            + (artifactType == null ? 0 : artifactType.hashCode())
            + (toolSpecificId == null ? 0 : toolSpecificId.hashCode());
    }

    /**
     * Create a new artifact id from a TFS URI. The uri must be well-formed.
     *
     * @throws MalformedURIException
     *         if the uri is not well-formed
     * @param uri
     *        the uri to decode and create an artifact id from
     */
    public ArtifactID(final String uri) {
        final String[] parts = decodeURI(uri);

        /*
         * the decode routine returns null if the uri is not well-formed
         */
        if (parts == null) {
            throw new MalformedURIException(uri);
        }

        tool = parts[0];
        artifactType = parts[1];
        toolSpecificId = parts[2];
    }

    public static void checkURIIsWellFormed(final String uri) {
        final String[] parts = decodeURI(uri);
        if (parts == null) {
            throw new MalformedURIException(uri);
        }
    }

    /**
     * @return the tool part of this artifact id
     */
    public String getTool() {
        return tool;
    }

    /**
     * @return the artifact type part of this artifact id
     */
    public String getArtifactType() {
        return artifactType;
    }

    /**
     * @return the tool specific id part of this artifact id
     */
    public String getToolSpecificID() {
        return toolSpecificId;
    }

    /**
     * Encodes this artifact id as a TFS URI.
     *
     * @throws MalformedArtifactIDException
     *         if this artifact id is not well-formed
     * @return a well-formed TFS URI
     */
    public String encodeURI() {
        if (!isWellFormed()) {
            throw new MalformedArtifactIDException(this);
        }

        final StringBuffer uri = new StringBuffer();

        try {
            uri.append(VSTFS_PREFIX);
            uri.append(URLEncoder.encode(tool, URL_ENCODING));
            uri.append(URI_SEPARATOR);
            uri.append(URLEncoder.encode(artifactType, URL_ENCODING));
            uri.append(URI_SEPARATOR);
            uri.append(URLEncoder.encode(toolSpecificId, URL_ENCODING));
        } catch (final UnsupportedEncodingException ex) {
            /*
             * we should never get here (UTF-8 URL encoding is the recommended
             * encoding and should be supported on all platforms), so convert
             * into a runtime exception and throw
             */
            throw new RuntimeException(ex);
        }

        return uri.toString();
    }

    /**
     * Checks whether this artifact id is well formed.
     *
     * @return true if this artifact id is well formed
     */
    public boolean isWellFormed() {
        return isToolWellFormed(tool)
            && isArtifactTypeWellFormed(artifactType)
            && isToolSpecificIDWellFormed(toolSpecificId);
    }

    private static boolean isToolSpecificIDWellFormed(final String toolSpecificId) {
        if (isNullOrEmpty(toolSpecificId)) {
            return false;
        }

        return true;
    }

    private static boolean isArtifactTypeWellFormed(final String artifactType) {
        if (isNullOrEmpty(artifactType)) {
            return false;
        }

        /*
         * artifact type cannot contain forward slashes
         */
        if (artifactType.indexOf('/') != -1) {
            return false;
        }

        return true;
    }

    private static boolean isToolWellFormed(final String tool) {
        if (isNullOrEmpty(tool)) {
            return false;
        }

        /*
         * tool cannot contain forward slashes, back slashes, or periods, and
         * must be non-zero length
         */
        if ((tool.indexOf('\\') != -1) || (tool.indexOf('/') != -1) || (tool.indexOf('.') != -1)) {
            return false;
        }

        return true;
    }

    private static boolean isNullOrEmpty(final String input) {
        return input == null || input.trim().length() == 0;
    }

    /**
     * IMPLEMENTATION DETAIL (keep private)
     *
     * Decodes a TFS URI, returning an array with the decoded parts. If the URI
     * is well-formed, the returned array will have 3 elements. The elements (in
     * order) will be tool, artifact type, and tool-specific id. If the URI is
     * not well-formed, null will be returned.
     *
     * @param uri
     *        a TFS URI to decode
     * @return the return described above, or null if the input is not
     *         well-formed
     */
    private static String[] decodeURI(final String uri) {
        /*
         * the input must not be null
         */
        if (uri == null) {
            return null;
        }

        final String trimmedInput = uri.trim();

        /*
         * the input must begin with the vstfs prefiew
         */
        if (!trimmedInput.startsWith(VSTFS_PREFIX)) {
            return null;
        }

        final String inputWithoutPrefix = trimmedInput.substring(VSTFS_PREFIX.length());
        final String[] parts = inputWithoutPrefix.split(URI_SEPARATOR);

        /*
         * the URI separator must split the non-prefixed URI into exactly 3
         * parts
         */
        if (parts.length != 3) {
            return null;
        }

        String tool = null;
        String artifactType = null;
        String toolSpecificId = null;

        try {
            tool = URLDecoder.decode(parts[0], URL_ENCODING);
            artifactType = URLDecoder.decode(parts[1], URL_ENCODING);
            toolSpecificId = URLDecoder.decode(parts[2], URL_ENCODING);
        } catch (final UnsupportedEncodingException ex) {
            /*
             * we should never get here (UTF-8 URL encoding is the recommended
             * encoding and should be supported on all platforms), so convert
             * into a runtime exception and throw
             */
            throw new RuntimeException(ex);
        }

        /*
         * check for tool well-formedness
         */
        if (!isToolWellFormed(tool)
            || !isArtifactTypeWellFormed(artifactType)
            || !isToolSpecificIDWellFormed(toolSpecificId)) {
            return null;
        }

        return new String[] {
            tool,
            artifactType,
            toolSpecificId
        };
    }
}

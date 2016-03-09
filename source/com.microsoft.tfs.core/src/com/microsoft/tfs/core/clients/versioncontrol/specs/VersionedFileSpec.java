// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import java.util.ArrayList;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.DeletionVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpecParseException;
import com.microsoft.tfs.util.Check;

/**
 * Describes a version control item (by path) at one or more versions (possibly
 * a range of versions). This class is generally used to parse user-entered text
 * from the command line.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class VersionedFileSpec {
    /**
     * Separates the item from the version information when a VersionSpec is
     * formatted for a given repository or local path.
     */
    private final static char ITEM_AND_VERSION_SEPARATOR = ';';

    /**
     * Separates a range of version specs.
     */
    private final static char VERSION_RANGE_SEPARATOR = '~';

    /**
     * Contains the string we parsed to create this object exactly as the user
     * typed it (if this object did indeed come from a user).
     */
    private String exactUserString;

    private final String item;
    private final DeletionVersionSpec deletionSpec;
    private final VersionSpec[] versions;

    /**
     * Creates a {@link VersionedFileSpec}.
     *
     * @param item
     *        the item (must not be <code>null</code> or empty)
     * @param deletionSpec
     *        the deletion spec (may be null)
     * @param versions
     *        the versions (must not be <code>null</code>)
     */
    public VersionedFileSpec(final String item, final DeletionVersionSpec deletionSpec, final VersionSpec[] versions) {
        Check.notNullOrEmpty(item, "item"); //$NON-NLS-1$
        Check.notNull(versions, "versions"); //$NON-NLS-1$

        this.item = item;
        this.deletionSpec = deletionSpec;
        this.versions = versions;
    }

    /**
     * Parses the given information into this AVersionedFileSpec object. If a
     * deletion specifier is encountered, zero or one other version specifier is
     * allowed (an exception will be thrown if more than one is found).
     *
     * @param spec
     *        the versioned file spec string (like "filename.ext;C1" or
     *        "filename.ext;X4;C1") (must not be <code>null</code> or empty)
     * @param user
     *        the current user (must not be <code>null</code> or empty).
     * @param allowVersionRange
     *        whether to allow ranges in the version specifications.
     * @throws VersionSpecParseException
     *         if an error occured parsing the version spec string.
     * @throws LabelSpecParseException
     *         if an error occured parsing the label spec string.
     */
    public static VersionedFileSpec parse(final String spec, final String user, final boolean allowVersionRange)
        throws VersionSpecParseException,
            LabelSpecParseException {
        if (spec == null || spec.length() == 0) {
            throw new VersionSpecParseException(
                Messages.getString("VersionedFileSpec.VersionSpecMustNotBeNullOrEmpty")); //$NON-NLS-1$
        }

        if (user == null || user.length() == 0) {
            throw new VersionSpecParseException(Messages.getString("VersionedFileSpec.UserStringMustNotBeNullOrEmpty")); //$NON-NLS-1$
        }

        final int sepIndex = spec.indexOf(ITEM_AND_VERSION_SEPARATOR);

        String item;
        DeletionVersionSpec deletionSpec = null;
        VersionSpec[] versions = new VersionSpec[0];

        /*
         * To be compatible with Visual Studio's parsing allowances, we support
         * the following types of input:
         *
         * filename.txt : Just a file name, no version or deletion ID.
         *
         * filename.txt;C7 : A changeset version specifier.
         *
         * filename.txt;X7 : A deletion ID specifier.
         *
         * filename.txt;X7;C9~C107 : A deletion ID and changeset range (or
         * single).
         *
         * filename.txt;C9~C107;X7 : Above, reversed.
         */

        if (sepIndex == -1) {
            /*
             * No separator found, the whole string is just an item name. No
             * versions were specified.
             */

            item = ItemPath.smartNativeToTFS(ItemPath.canonicalize(spec));
        } else {
            /*
             * The item is the first part, the versions need to be parsed.
             */
            item = ItemPath.smartNativeToTFS(ItemPath.canonicalize(spec.substring(0, sepIndex)));

            final VersionSpec[] parsedVersions =
                VersionSpec.parseMultipleVersionsFromSpec(spec.substring(sepIndex + 1), user, allowVersionRange);

            /*
             * We must perform some sanity checks for deletion identifiers. If a
             * deletion identifier (ADeletionVersionSpec) is found, there can be
             * only one other version spec. Multiple deletion specs are also not
             * allowed.
             */

            final ArrayList tmpVersions = new ArrayList();
            boolean alreadyFoundDeletion = false;
            for (int i = 0; i < parsedVersions.length; i++) {
                if (parsedVersions[i] instanceof DeletionVersionSpec) {
                    if (alreadyFoundDeletion) {
                        throw new VersionSpecParseException(
                            Messages.getString("VersionedFileSpec.OnlyOneDeletionVersionIsAllowedPerItem")); //$NON-NLS-1$
                    }

                    deletionSpec = (DeletionVersionSpec) parsedVersions[i];
                    alreadyFoundDeletion = true;
                } else {
                    tmpVersions.add(parsedVersions[i]);
                }
            }

            versions = (VersionSpec[]) tmpVersions.toArray(new VersionSpec[tmpVersions.size()]);

            if (deletionSpec != null && versions.length > 2) {
                throw new VersionSpecParseException(
                    Messages.getString("VersionedFileSpec.DeletionVersionMayBeCombinedWithAtMostOneOtherVersionSpec")); //$NON-NLS-1$
            }
        }

        /*
         * Make sure we parsed an item.
         */
        if (item == null || item.length() == 0) {
            throw new VersionSpecParseException(
                Messages.getString("VersionedFileSpec.NoItemPathCouldBeFoundInTheVersionSpec")); //$NON-NLS-1$
        }

        return new VersionedFileSpec(item, deletionSpec, versions);
    }

    public String getItem() {
        return ItemPath.smartTFSToNative(item);
    }

    /**
     * Gets the versions specified by this versioned file spec.
     *
     * @return the versions specified by this versioned file spec.
     */
    public VersionSpec[] getVersions() {
        return versions;
    }

    /**
     * Gets the deletion ID specified by this versioned file spec. The default
     * deletion ID is 0 (which will be ignored by the server).
     *
     * @return the deletion ID specified by this versioned file spec.
     */
    public DeletionVersionSpec getDeletionVersionSpec() {
        return deletionSpec;
    }

    /**
     * Returns a string representation of a VersionSpec that will contain the
     * given repository or local path plus version information that this
     * AVersionSpec contains.
     *
     * @param repositoryOrLocalPath
     *        a repository path or local path to include in the returned string.
     * @param spec
     *        an AVersionSpec.
     * @return a string for the given path plus version information from the
     *         given spec instance.
     */
    public final static String formatForPath(final String repositoryOrLocalPath, final VersionSpec spec) {
        return (repositoryOrLocalPath + ITEM_AND_VERSION_SEPARATOR + spec.toString());
    }

    /**
     * Returns a string representation of a VersionSpec range that will contain
     * the given repository or local path plus version information that these
     * AVersionSpecs contain.
     *
     * @param repositoryOrLocalPath
     *        a repository path or local path to include in the returned string.
     * @param specFrom
     *        the first AVersionSpec.
     * @param specTo
     *        the second AVersionSpec.
     * @return a string for the given path plus version information from the
     *         version spec instances.
     */
    public final static String formatForPath(
        final String repositoryOrLocalPath,
        final VersionSpec specFrom,
        final VersionSpec specTo) {
        return (repositoryOrLocalPath
            + ITEM_AND_VERSION_SEPARATOR
            + specFrom.toString()
            + VERSION_RANGE_SEPARATOR
            + specTo.toString());
    }

    /**
     * Gets the string the user typed to create this spec. null if this spec was
     * not created by user input.
     *
     * @return the string the user typed to create this spec, null if this spec
     *         was not created by user input.
     */
    public String getExactUserString() {
        return exactUserString;
    }

    /**
     * Takes a path and deletion ID, and if the deletion ID is 0, returns the
     * path with the deletion appended (after a separator).
     *
     * @param path
     *        the path to format (must not be <code>null</code>)
     * @param deletionID
     *        the deletion ID to append to the path after a separator, if the
     *        deletion ID is not 0
     * @return the original path with the deletion ID appended if the ID was not
     *         0
     */
    public static String formatPathWithDeletionIfNecessary(final String path, final int deletionID) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        if (deletionID == 0) {
            return path;
        }

        return path + ITEM_AND_VERSION_SEPARATOR + new DeletionVersionSpec(deletionID).toString();
    }
}

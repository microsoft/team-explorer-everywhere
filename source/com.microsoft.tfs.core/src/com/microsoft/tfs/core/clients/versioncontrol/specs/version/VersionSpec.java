// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs.version;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpecParseException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._ChangesetVersionSpec;
import ms.tfs.versioncontrol.clientservices._03._DateVersionSpec;
import ms.tfs.versioncontrol.clientservices._03._LabelVersionSpec;
import ms.tfs.versioncontrol.clientservices._03._LatestVersionSpec;
import ms.tfs.versioncontrol.clientservices._03._VersionSpec;
import ms.tfs.versioncontrol.clientservices._03._WorkspaceVersionSpec;

/**
 * A {@link VersionSpec} represents a specific version of some version control,
 * although the item identity is not part of this object (see
 * {@link VersionedFileSpec} for this representation).
 *
 * This class is the base class of all concrete version spec classes. All
 * version spec classes are immutable.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public abstract class VersionSpec extends WebServiceObjectWrapper {
    /**
     * Separates the (possibly) multiple version ranges in a spec string.
     */
    public final static char RANGE_DELIMITER = '~';

    protected VersionSpec(final Object webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Parses multiple AVersionSpec instances out of a string containing a
     * version range separator. If the string contains only one version
     * specificaion, only one AVersionSpec is returned. Deletion identifiers are
     * also parsed and returned.
     *
     * @param versionSpecs
     *        the string containing (possibly) multiple version specs, separated
     *        by a range separator character.
     * @param user
     *        the username to use for the types of version specs that require
     *        one.
     * @param allowVersionRange
     *        whether to allow version ranges (instead of just one version). If
     *        set to false, and a version range is detected, an
     *        AVersionSpecParseException is thrown
     * @return an array of AVersionSpec objects, one for each version spec
     *         parsed from the string.
     * @throws VersionSpecParseException
     *         if an error occured parsing a version spec from the string.
     * @throws LabelSpecParseException
     *         if an error occured parsing a label spec from the string.
     */
    public static VersionSpec[] parseMultipleVersionsFromSpec(
        final String versionSpecs,
        final String user,
        final boolean allowVersionRange) throws VersionSpecParseException, LabelSpecParseException {
        final ArrayList versions = new ArrayList();

        Check.notNullOrEmpty(versionSpecs, "versionSpecs"); //$NON-NLS-1$

        final String[] specs = versionSpecs.split(Character.toString(RANGE_DELIMITER));

        if (allowVersionRange == false && specs.length > 1) {
            throw new VersionSpecParseException(
                Messages.getString("VersionSpec.AVersionRangeIsNotAllowedInThisVersionSpec")); //$NON-NLS-1$
        }

        for (int i = 0; i < specs.length; i++) {
            final VersionSpec vs = VersionSpec.parseSingleVersionFromSpec(specs[i], user);

            Check.notNull(vs, "vs"); //$NON-NLS-1$
            versions.add(vs);
        }

        return (VersionSpec[]) versions.toArray(new VersionSpec[0]);
    }

    /**
     * Parses a single version spec string (no ranges allowed) and returns an
     * AVersionSpec object that represents that spec. Times in the spec string
     * are converted from local time to UTC before being set in the objects.
     * Deletion identifiers are also parsed and returned.
     *
     * @param spec
     *        the single spec string to parse (no range delimiter allowed) (must
     *        not be <code>null</code>)
     * @param user
     *        the username to use to qualify a workspace spec.
     * @return a new AVersionSpec, appropriate for the spec string.
     * @throws VersionSpecParseException
     *         if an error occurred parsing the spec string.
     * @throws LabelSpecParseException
     *         if a label spec string was encountered but an error occurred
     *         parsing it.
     */
    public static VersionSpec parseSingleVersionFromSpec(final String spec, final String user)
        throws VersionSpecParseException,
            LabelSpecParseException {
        VersionSpec ret = null;

        if (spec == null || spec.length() == 0) {
            throw new VersionSpecParseException(
                Messages.getString("VersionSpec.TheVersionSpecStringMustNotBeNullOrEmpty")); //$NON-NLS-1$
        }

        /*
         * We don't allow range specifiers here. Those should be parsed before
         * this method is called.
         */
        if (spec.indexOf(RANGE_DELIMITER) != -1) {
            throw new VersionSpecParseException(
                MessageFormat.format(
                    Messages.getString("VersionSpec.TheVersionSpecStringMustNotContainRangeDelimiterCharacterFormat"), //$NON-NLS-1$
                    RANGE_DELIMITER));
        }

        /*
         * Determine which kind of spec this is by its identifying first
         * character.
         */
        final char specTypeChar = Character.toUpperCase(spec.charAt(0));
        if (specTypeChar == LatestVersionSpec.IDENTIFIER) {
            if (spec.length() > 1) {
                throw new VersionSpecParseException(
                    Messages.getString("VersionSpec.TheLatestVersionSpecMustContainOneCharacter")); //$NON-NLS-1$
            }

            ret = LatestVersionSpec.INSTANCE;
        } else if (specTypeChar == DeletionVersionSpec.IDENTIFIER) {
            if (spec.length() == 1) {
                throw new VersionSpecParseException(
                    Messages.getString("VersionSpec.TheDeletionIDSpecMustIncludeANumber")); //$NON-NLS-1$
            }

            int deletionID = 0;

            try {
                deletionID = Integer.parseInt(spec.substring(1));
            } catch (final NumberFormatException e) {
                throw new VersionSpecParseException(
                    Messages.getString("VersionSpec.TheDeletionIDSpecWasNotRecognizedAsANumber")); //$NON-NLS-1$
            }

            ret = new DeletionVersionSpec(deletionID);
        } else if (specTypeChar == WorkspaceVersionSpec.IDENTIFIER) {
            if (spec.length() == 1) {
                throw new VersionSpecParseException(
                    Messages.getString("VersionSpec.TheWorkspaceSpecMustIncludeTheWorkspaceName")); //$NON-NLS-1$
            }

            WorkspaceSpec ws;
            try {
                ws = WorkspaceSpec.parse(spec.substring(1), user);
            } catch (final WorkspaceSpecParseException e) {
                /*
                 * AWorkspaceSpecParseException messages are specific enough to
                 * pass up to the user.
                 */
                throw new VersionSpecParseException(e.getMessage());
            }

            // TODO Verify username includes domain name. Maybe
            // WorkspaceVersionSpec should do it.

            ret = new WorkspaceVersionSpec(ws);
        } else if (specTypeChar == LabelVersionSpec.IDENTIFIER) {
            if (spec.length() == 1) {
                throw new VersionSpecParseException(
                    Messages.getString("VersionSpec.TheLabelVersionSpecMustContainALabelNameAndScope")); //$NON-NLS-1$
            }

            ret = new LabelVersionSpec(LabelSpec.parse(spec.substring(1), null, false));
        } else if (specTypeChar == DateVersionSpec.IDENTIFIER) {
            if (spec.length() == 1) {
                throw new VersionSpecParseException(
                    Messages.getString("VersionSpec.TheDateVersionSpecMustContainADate")); //$NON-NLS-1$
            }

            try {
                final DateVersionSpec dvs = new DateVersionSpec(spec.substring(1));
                ret = dvs;
            } catch (final ParseException e) {
                throw new VersionSpecParseException(
                    MessageFormat.format(
                        Messages.getString("VersionSpec.TheValueCouldNotBeConvertedToADateFormat"), //$NON-NLS-1$
                        e.getMessage()));
            }

        } else {
            /*
             * If we're here, we assume the user wants to do version by
             * changeset. We support an explicit changeset version spec
             * (starting with a 'C'), but also support a bare number.
             */

            int changeSetNumber = 0;
            int index = 0;

            if (specTypeChar == ChangesetVersionSpec.IDENTIFIER) {
                if (spec.length() == 1) {
                    throw new VersionSpecParseException(
                        Messages.getString("VersionSpec.TheVersionSpecMustIncludeAChangesetNumber")); //$NON-NLS-1$
                }

                index = 1;
            }

            try {
                changeSetNumber = Integer.parseInt(spec.substring(index));
            } catch (final NumberFormatException e) {
                throw new VersionSpecParseException(
                    Messages.getString("VersionSpec.TheChangesetVersionSpecWasNotRecognizedAsANumber")); //$NON-NLS-1$
            }

            if (changeSetNumber < 1) {
                throw new VersionSpecParseException(
                    Messages.getString("VersionSpec.AChangesetVersionNumberMustBePositiveInteger")); //$NON-NLS-1$
            }

            ret = new ChangesetVersionSpec(changeSetNumber);
        }

        Check.notNull(ret, "ret"); //$NON-NLS-1$
        return ret;
    }

    /**
     * Constructs a new {@link VersionSpec}-derived class, the type of which is
     * appropriate for the given {@link _VersionSpec} instance.
     *
     * @param spec
     *        the version spec instance to use as source.
     * @return a new {@link VersionSpec}-derived class initialized with the
     *         given spec.
     */
    public final static VersionSpec fromWebServiceObject(final _VersionSpec spec) {
        if (spec instanceof _ChangesetVersionSpec) {
            return new ChangesetVersionSpec((_ChangesetVersionSpec) spec);
        }

        if (spec instanceof _LatestVersionSpec) {
            return LatestVersionSpec.INSTANCE;
        }

        if (spec instanceof _WorkspaceVersionSpec) {
            return new WorkspaceVersionSpec((_WorkspaceVersionSpec) spec);
        }

        if (spec instanceof _DateVersionSpec) {
            return new DateVersionSpec((_DateVersionSpec) spec);
        }

        if (spec instanceof _LabelVersionSpec) {
            return new LabelVersionSpec((_LabelVersionSpec) spec);
        }

        // TODO Update this method with any more version specs.

        throw new RuntimeException(MessageFormat.format(
            "Cannot convert {0} to any VersionSpec-derived class.", //$NON-NLS-1$
            spec.getClass().getName().toString()));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _VersionSpec getWebServiceObject() {
        return (_VersionSpec) webServiceObject;
    }

    /**
     * Returns the version component of the spec string for the type of
     * VersionSpec implemented by this class. This is used to format version
     * specs for display to the user.
     *
     * @return the version component of the spec string.
     */
    @Override
    public abstract String toString();
}

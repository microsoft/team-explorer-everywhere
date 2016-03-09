// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.ws.runtime.types.FlagSet;
import com.microsoft.tfs.util.BitField;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._ChangeType;
import ms.tfs.versioncontrol.clientservices._03._ChangeType._ChangeType_Flag;

/**
 * Describes the type of change that applied to an item.
 *
 * @since TEE-SDK-10.1
 */
public final class ChangeType extends BitField {
    /*
     * Standard fields defined by WSDL. Notice 0 is used for NONE so an the
     * integer 0 maps to a value which is a set with no changes. The other
     * fields' values are assigned to match the "ClientValue" attribute defined
     * in the server's ChangeType enumeration. These values are shifted one bit
     * to the left when reading from the "wire", one bit to the right when
     * writing to the "wire".
     */

    public static final ChangeType NONE = new ChangeType(0, _ChangeType_Flag.None.toString(), false);
    public static final ChangeType ADD = new ChangeType(2, _ChangeType_Flag.Add.toString(), false);
    public static final ChangeType EDIT = new ChangeType(4, _ChangeType_Flag.Edit.toString(), false);
    public static final ChangeType ENCODING = new ChangeType(8, _ChangeType_Flag.Encoding.toString(), false);
    public static final ChangeType RENAME = new ChangeType(16, _ChangeType_Flag.Rename.toString(), false);
    public static final ChangeType DELETE = new ChangeType(32, _ChangeType_Flag.Delete.toString(), false);
    public static final ChangeType UNDELETE = new ChangeType(64, _ChangeType_Flag.Undelete.toString(), false);
    public static final ChangeType BRANCH = new ChangeType(128, _ChangeType_Flag.Branch.toString(), false);
    public static final ChangeType MERGE = new ChangeType(256, _ChangeType_Flag.Merge.toString(), false);
    public static final ChangeType LOCK = new ChangeType(512, _ChangeType_Flag.Lock.toString(), false);
    public static final ChangeType ROLLBACK = new ChangeType(1024, _ChangeType_Flag.Rollback.toString(), true);
    public static final ChangeType SOURCE_RENAME = new ChangeType(2048, _ChangeType_Flag.SourceRename.toString(), true);

    /*
     * Introduced in TFS 2010. This flag's name is present in the the WSDL, but
     * these the name never appears in flag sets sent to/from TFS. Instead it is
     * sent to/from TFS as an additional integer bitfield, whose values
     * complement the standard fields.
     */
    public static final ChangeType TARGET_RENAME = new ChangeType(4096, "TargetRename", true); //$NON-NLS-1$

    public static final ChangeType PROPERTY = new ChangeType(8192, _ChangeType_Flag.Property.toString(), true);

    /*
     * Flag which includes all possible change types.
     */
    public static final ChangeType ALL = new ChangeType(0xffff, "ALL", false); //$NON-NLS-1$

    /*
     * Allocate some useful combinations
     */
    public static final ChangeType ADD_ENCODING = ChangeType.ADD.combine(ChangeType.ENCODING);
    public static final ChangeType ADD_EDIT_ENCODING =
        ChangeType.ADD.combine(ChangeType.EDIT).combine(ChangeType.ENCODING);
    public static final ChangeType RENAME_OR_DELETE = ChangeType.RENAME.combine(ChangeType.DELETE);
    public static final ChangeType ADD_BRANCH_OR_RENAME =
        ChangeType.ADD.combine(ChangeType.BRANCH).combine(ChangeType.RENAME);

    /**
     * Standard private constructor, which registers the given name for the
     * given flags, and permits a single-bit flag value to be force-registered
     * as "special".
     */
    private ChangeType(final int flags, final String name, final boolean forceSpecial) {
        super(flags);

        registerStringValue(getClass(), flags, name, forceSpecial);
    }

    /**
     * Special private constructor which uses the given flags but does not
     * register any name for it. Overridden to provide the derived type for
     * {@link #combine(ChangeType)}, etc.
     */
    private ChangeType(final int flags) {
        super(flags);
    }

    /**
     * Constructs a {@link ChangeType} from the given web service object
     * {@link FlagSet}.
     *
     * @param flagSet
     *        the {@link FlagSet} (must not be <code>null</code>)
     * @param flagsExtended
     *        the extended flag information (pass 0 to enable no extended flags)
     */
    public ChangeType(final _ChangeType flagSet, final int flagsExtended) {
        this(webServiceObjectToFlags(flagSet) | (flagsExtended << 1));
    }

    /**
     * Determines the correct flags value that represents the individual flag
     * types contained by the given web service object (a flag set).
     *
     * @param webServiceObject
     *        the web service object flag set (must not be <code>null</code>)
     * @return the flags value appropriate for the given flag set
     */
    private static int webServiceObjectToFlags(final _ChangeType webServiceObject) {
        Check.notNull(webServiceObject, "changeType"); //$NON-NLS-1$

        /*
         * The web service type is a flag set. Get all the strings from the
         * flags contained in it, and convert those values to one integer.
         */

        final List<String> strings = new ArrayList<String>();
        final _ChangeType_Flag[] flags = webServiceObject.getFlags();

        for (int i = 0; i < flags.length; i++) {
            strings.add(flags[i].toString());
        }

        return fromStringValues(strings.toArray(new String[strings.size()]), ChangeType.class);
    }

    /**
     * Gets the web service object. The returned object should not be modified.
     *
     * @return the web service object
     */
    public _ChangeType getWebServiceObject() {
        return new _ChangeType(toStringValues());
    }

    /**
     * Gets the extended flag information for this {@link ChangeType}, which is
     * sent to TFS with the web service object. The extended flags contain bits
     * for the non-extended flags, too.
     *
     * @return the extended type information flags
     */
    public int getWebServiceObjectExtendedFlags() {
        // Include flags for all change types (extended and traditional)
        return toIntFlags() >> 1;
    }

    /*
     * BitField Overrides.
     */

    public static ChangeType combine(final ChangeType[] changeTypes) {
        return new ChangeType(BitField.combine(changeTypes));
    }

    public boolean containsAll(final ChangeType other) {
        return containsAllInternal(other);
    }

    public boolean contains(final ChangeType other) {
        return containsInternal(other);
    }

    public boolean containsAny(final ChangeType other) {
        return containsAnyInternal(other);
    }

    public ChangeType remove(final ChangeType other) {
        return new ChangeType(removeInternal(other));
    }

    public ChangeType retain(final ChangeType other) {
        return new ChangeType(retainInternal(other));
    }

    public ChangeType combine(final ChangeType other) {
        return new ChangeType(combineInternal(other));
    }

    /*
     * Methods for change types.
     */

    public String toUIString(final boolean showLock) {
        return toUIString(showLock, (PropertyValue[]) null);
    }

    public String toUIString(final boolean showLock, final Item item) {
        return toUIString(showLock, item != null ? item.getPropertyValues() : null);
    }

    public String toUIString(final boolean showLock, final ExtendedItem extendedItem) {
        return toUIString(showLock, extendedItem != null ? extendedItem.getPropertyValues() : null);
    }

    public String toUIString(final boolean showLock, final PendingChange pendingChange) {
        return toUIString(showLock, pendingChange != null ? pendingChange.getPropertyValues() : null);
    }

    public String toUIString(final boolean showLock, final Change change) {
        final Item item = (change != null ? change.getItem() : null);
        return toUIString(showLock, item != null ? item.getPropertyValues() : null);
    }

    /**
     * Gets a localized string describing the changes in this {@link ChangeType}
     * including property change information (if supplied).
     *
     * @param showLock
     *        if <code>false</code>, the "lock" change type string will not be
     *        included if this {@link ChangeType} has lock unless lock is the
     *        only change type
     * @param properties
     *        the {@link PropertyValue}s to include in the summary if the change
     *        type includes a {@link ChangeType#PROPERTY} change (may be
     *        <code>null</code> to omit the property value information)
     * @return a localized {@link String} that represents this
     *         {@link ChangeType}
     */
    public String toUIString(final boolean showLock, final PropertyValue[] properties) {
        /*
         * Duplicate the change type so we can modify it.
         */
        final ChangeType tmpChangeType = new ChangeType(toIntFlags());

        final StringBuilder sb = new StringBuilder();

        /*
         * If Lock is not the only change, do not show it.
         */
        if (!showLock && !tmpChangeType.equals(ChangeType.LOCK)) {
            tmpChangeType.remove(ChangeType.LOCK);
        }

        appendIf(tmpChangeType.contains(ChangeType.MERGE), sb, "ChangeType.MergeDYNAMIC"); //$NON-NLS-1$
        appendIf(tmpChangeType.contains(ChangeType.ADD), sb, "ChangeType.AddDYNAMIC"); //$NON-NLS-1$
        appendIf(tmpChangeType.contains(ChangeType.BRANCH), sb, "ChangeType.BranchDYNAMIC"); //$NON-NLS-1$
        appendIf(tmpChangeType.contains(ChangeType.DELETE), sb, "ChangeType.DeleteDYNAMIC"); //$NON-NLS-1$

        /*
         * Only show Encoding if the change is not an Add or a Branch.
         */
        appendIf(
            tmpChangeType.contains(ChangeType.ENCODING)
                && tmpChangeType.contains(ChangeType.BRANCH) == false
                && tmpChangeType.contains(ChangeType.ADD) == false,
            sb,
            "ChangeType.EncodingDYNAMIC"); //$NON-NLS-1$

        appendIf(tmpChangeType.contains(ChangeType.LOCK), sb, "ChangeType.LockDYNAMIC"); //$NON-NLS-1$
        appendIf(tmpChangeType.contains(ChangeType.RENAME), sb, "ChangeType.RenameDYNAMIC"); //$NON-NLS-1$
        appendIf(tmpChangeType.contains(ChangeType.UNDELETE), sb, "ChangeType.UndeleteDYNAMIC"); //$NON-NLS-1$

        /*
         * Show Edit, unless it's an Add.
         */
        appendIf(
            tmpChangeType.contains(ChangeType.EDIT) && tmpChangeType.contains(ChangeType.ADD) == false,
            sb,
            "ChangeType.EditDYNAMIC"); //$NON-NLS-1$

        /*
         * New for TFS 2010.
         */
        appendIf(tmpChangeType.contains(ChangeType.ROLLBACK), sb, "ChangeType.RollbackDYNAMIC"); //$NON-NLS-1$
        appendIf(tmpChangeType.contains(ChangeType.SOURCE_RENAME), sb, "ChangeType.SourceRenameDYNAMIC"); //$NON-NLS-1$

        /*
         * New for TFS 2012.
         */
        appendIf(tmpChangeType.contains(ChangeType.PROPERTY), sb, "ChangeType.PropertyDYNAMIC"); //$NON-NLS-1$

        if (tmpChangeType.contains(ChangeType.PROPERTY)) {
            final StringBuilder propertiesSummary = new StringBuilder();

            final PropertyValue symlink = PropertyUtils.selectMatching(properties, PropertyConstants.SYMBOLIC_KEY);
            final PropertyValue executable = PropertyUtils.selectMatching(properties, PropertyConstants.EXECUTABLE_KEY);
            // Symbolic link property
            if (symlink != null) {
                if (PropertyConstants.IS_SYMLINK.equals(symlink)) {
                    propertiesSummary.append(Messages.getString("ChangeType.ChangeToSymlinkSummary")); //$NON-NLS-1$
                } else if (PropertyConstants.NOT_SYMLINK.equals(symlink)) {
                    propertiesSummary.append(Messages.getString("ChangeType.LoseSymlinkSummary")); //$NON-NLS-1$
                }
            }
            // Executable property
            if (executable != null) {
                /*
                 * TEE always pends PropertyConstants.EXECUTABLE_DISABLED_VALUE
                 * to turn off the property so it can be detected here and
                 * displayed. Another method of disabling execute bit is
                 * deleting the property entirely, and that won't get a "-x"
                 * summary display.
                 */
                if (PropertyConstants.EXECUTABLE_ENABLED_VALUE.equals(executable)) {
                    propertiesSummary.append(Messages.getString("ChangeType.EnableExecutableSummary")); //$NON-NLS-1$
                } else if (PropertyConstants.EXECUTABLE_DISABLED_VALUE.equals(executable)) {
                    propertiesSummary.append(Messages.getString("ChangeType.DisableExecutableSummary")); //$NON-NLS-1$
                }
            }

            if (propertiesSummary.length() > 0) {
                sb.append(MessageFormat.format(
                    Messages.getString("ChangeType.PropertiesSummaryListFormat"), //$NON-NLS-1$
                    propertiesSummary.toString()));
            }
        }

        return sb.toString();
    }

    private static void appendIf(final boolean condition, final StringBuilder sb, final String messageKey) {
        if (condition) {
            if (sb.length() > 0) {
                sb.append(", "); //$NON-NLS-1$
            }

            sb.append(Messages.getString(messageKey));
        }
    }

    /**
     * Creates a {@link ChangeType} for the given integer flags (which may come
     * from {@link #toIntFlags()}.
     *
     * @param flags
     *        The integer flags for this {@link ChangeType}
     * @return The appropriate {@link ChangeType}
     */
    public static ChangeType fromIntFlags(final int flags) {
        return new ChangeType(flags);
    }

    /**
     * Creates a {@link ChangeType} for the given integer flags and extended
     * (2010 style) flags.
     *
     * @param flags
     *        The integer flags for this {@link ChangeType}
     * @param extendedFlags
     *        The integer extended flags for this {@link ChangeType}
     * @return The appropriate {@link ChangeType}
     */
    public static ChangeType fromIntFlags(final int flags, final int extendedFlags) {
        return new ChangeType(flags | (extendedFlags << 1));
    }
}

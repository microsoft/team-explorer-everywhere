// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.utils;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.Wildcard;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.util.StringUtil;

public class BuildValidation {
    // Note that this class is roughly analogous to
    // Microsoft.TeamFoundation.Build.Common.Validation

    public static void checkValidDefinitionName(final String name) {
        final ValidationResult result = validateDefinitionName(name);
        if (!result.isValid()) {
            throw new TECoreException(result.getErrorMessage());
        }
    }

    public static boolean isValidDefinitionName(final String name) {
        return validateDefinitionName(name).isValid();
    }

    private static ValidationResult validateDefinitionName(final String name) {
        if (name == null || name.length() == 0) {
            return new ValidationResult(Messages.getString("BuildValidation.MustSpecifyNameForBuildDefinition")); //$NON-NLS-1$
        }
        if (name.length() > BuildConstants.MAX_PATH_NAME_LENGTH) {
            return new ValidationResult(
                MessageFormat.format(
                    Messages.getString("BuildValidation.BuildDefinitionNameTooLongMaxCharactersFormat"), //$NON-NLS-1$
                    name,
                    BuildConstants.MAX_PATH_NAME_LENGTH));
        }
        if (isNTFSReservedName(name)) {
            return new ValidationResult(MessageFormat.format(
                Messages.getString("BuildValidation.BuildDefinitionNameNotValidCannotBeNTFSReservedFormat"), //$NON-NLS-1$
                name));
        }
        if (name.endsWith(" ") || name.endsWith(".")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return new ValidationResult(
                MessageFormat.format(
                    Messages.getString("BuildValidation.BuildDefinitionNameNotValidCannotEndSpaceOrPeriodFormat"), //$NON-NLS-1$
                    name));
        }

        // Check the characters for legality.
        final char[] chars = name.toCharArray();
        boolean charsValid = true;
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            for (int j = 0; j < ILLEGAL_NTFS_CHARS.length; j++) {
                if (ILLEGAL_NTFS_CHARS[j] == c) {
                    charsValid = false;
                    break;
                }
            }
            for (int j = 0; j < DEFINITION_SPECIAL_CHARS.length; j++) {
                if (DEFINITION_SPECIAL_CHARS[j] == c) {
                    charsValid = false;
                    break;
                }
            }
            if (!charsValid) {
                return new ValidationResult(
                    MessageFormat.format(
                        Messages.getString("BuildValidation.BuildDefinitionNameContainsInvalidCharactersFormat"), //$NON-NLS-1$
                        name));
            }
        }
        return new ValidationResult(true);
    }

    public static void checkAgentName(final String name) {
        final ValidationResult result = validateAgentName(name);
        if (!result.isValid()) {
            throw new TECoreException(result.getErrorMessage());
        }
    }

    public static boolean isValidAgentName(final String name) {
        final ValidationResult result = validateAgentName(name);
        return result.isValid();
    }

    public static void checkValidControllerName(final String name, final boolean allowWildcards) {
        final ValidationResult result = validateControllerName(name, allowWildcards);
        if (!result.isValid()) {
            throw new TECoreException(result.getErrorMessage());
        }
    }

    public static boolean isValidControllerName(final String name, final boolean allowWildcards) {
        final ValidationResult result = validateControllerName(name, allowWildcards);
        return result.isValid();
    }

    private static ValidationResult validateControllerName(final String name, final boolean allowWildcards) {
        if (StringUtil.isNullOrEmpty(name)) {
            return new ValidationResult(false, Messages.getString("BuildValidation.NameRequired")); //$NON-NLS-1$
        }

        if (name.length() > BuildConstants.MAX_PATH_NAME_LENGTH) {
            final String format = Messages.getString("BuildValidation.NameTooLongFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, name, BuildConstants.MAX_PATH_LENGTH);
            return new ValidationResult(false, message);
        }

        // Note: Controller names are not currently used to create file system
        // folders, so the next two checks are
        // not technically necessary. The server-side validation logic validates
        // FullPath, however, not Name, so
        // to avoid having to modify the ValidatePath method agent names are
        // treated just like definition names.
        if (isNTFSReservedName(name)) {
            final String format = Messages.getString("BuildValidation.NameReservedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, name);
            return new ValidationResult(false, message);
        }

        if (name.endsWith(" ") || name.endsWith(".")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            final String format = Messages.getString("BuildValidation.NameTerminationErrorFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, name);
            return new ValidationResult(false, message);
        }

        final char[] chars = name.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            for (int j = 0; j < ILLEGAL_NTFS_CHARS.length; j++) {
                if (ILLEGAL_NTFS_CHARS[j] == c) {
                    final String format = Messages.getString("BuildValidation.NameInvalidCharsFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(format, name);
                    return new ValidationResult(false, message);
                }
            }
        }

        if (!allowWildcards && Wildcard.isWildcard(name)) {
            final String format = Messages.getString("BuildValidation.NameInvalidCharsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, name);
            return new ValidationResult(false, message);
        }

        return new ValidationResult(true);
    }

    private static ValidationResult validateAgentName(final String name) {
        if (name == null || name.length() == 0) {
            return new ValidationResult(Messages.getString("BuildValidation.MustSpecifyNameForBuildAgent")); //$NON-NLS-1$
        }
        if (name.length() > BuildConstants.MAX_PATH_NAME_LENGTH) {
            return new ValidationResult(
                MessageFormat.format(
                    Messages.getString("BuildValidation.BuildAgentNameTooLongMaxCharactersFormat"), //$NON-NLS-1$
                    name,
                    BuildConstants.MAX_PATH_NAME_LENGTH));
        }
        if (isNTFSReservedName(name)) {
            return new ValidationResult(MessageFormat.format(
                Messages.getString("BuildValidation.BuildAGentNameNotValidCannotBeNTFSReservedFormat"), //$NON-NLS-1$
                name));
        }
        if (name.endsWith(" ") || name.endsWith(".")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return new ValidationResult(
                MessageFormat.format(
                    Messages.getString("BuildValidation.BuildAgentNameNotValidCannotEndSpaceOrPeriodFormat"), //$NON-NLS-1$
                    name));
        }

        // Check the characters for legality.
        final char[] chars = name.toCharArray();
        boolean charsValid = true;
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            for (int j = 0; j < ILLEGAL_NTFS_CHARS.length; j++) {
                if (ILLEGAL_NTFS_CHARS[j] == c) {
                    charsValid = false;
                    break;
                }
            }
            for (int j = 0; j < AGENT_SPECIAL_CHARS.length; j++) {
                if (AGENT_SPECIAL_CHARS[j] == c) {
                    charsValid = false;
                    break;
                }
            }
            if (!charsValid) {
                return new ValidationResult(
                    MessageFormat.format(
                        Messages.getString("BuildValidation.BuildAgentNameContainsInvalidCharactersFormat"), //$NON-NLS-1$
                        name));
            }
        }

        return new ValidationResult(true);
    }

    private static boolean isNTFSReservedName(final String name) {
        for (int i = 0; i < NTFS_RESERVED_FILENAMES.length; i++) {
            if (NTFS_RESERVED_FILENAMES[i].equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(final boolean valid) {
            this(valid, null);
        }

        public ValidationResult(final String errorMessage) {
            this(false, errorMessage);
        }

        public ValidationResult(final boolean valid, final String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        /**
         * @return the valid
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * @return the errorMessage
         */
        public String getErrorMessage() {
            return errorMessage;
        }

    }

    private static final char[] AGENT_SPECIAL_CHARS = new char[] {
        '*',
        '?'
    };
    private static final char[] DEFINITION_SPECIAL_CHARS = new char[] {
        '*',
        '?',
        '@'
    };

    private static final char[] ILLEGAL_NTFS_CHARS = new char[] {
        '\0',
        '\u0001',
        '\u0002',
        '\u0003',
        '\u0004',
        '\u0005',
        '\u0006',
        '\u0007',
        '\u0008',
        '\u0009',
        '\n',
        '\u000B',
        '\u000C',
        '\r',
        '\u000e',
        '\u000f',
        '\u0010',
        '\u0011',
        '\u0012',
        '\u0013',
        '\u0014',
        '\u0015',
        '\u0016',
        '\u0017',
        '\u0018',
        '\u0019',
        '\u001a',
        '\u001b',
        '\u001c',
        '\u001d',
        '\u001e',
        '\u001f',
        '"',
        '/',
        ':',
        '<',
        '>',
        '\\',
        '|'
    };

    private static final String[] NTFS_RESERVED_FILENAMES = new String[] {
        "CON", //$NON-NLS-1$
        "PRN", //$NON-NLS-1$
        "AUX", //$NON-NLS-1$
        "NUL", //$NON-NLS-1$
        "COM1", //$NON-NLS-1$
        "COM2", //$NON-NLS-1$
        "COM3", //$NON-NLS-1$
        "COM4", //$NON-NLS-1$
        "COM5", //$NON-NLS-1$
        "COM6", //$NON-NLS-1$
        "COM7", //$NON-NLS-1$
        "COM8", //$NON-NLS-1$
        "COM9", //$NON-NLS-1$
        "LPT1", //$NON-NLS-1$
        "LPT2", //$NON-NLS-1$
        "LPT3", //$NON-NLS-1$
        "LPT4", //$NON-NLS-1$
        "LPT5", //$NON-NLS-1$
        "LPT6", //$NON-NLS-1$
        "LPT7", //$NON-NLS-1$
        "LPT8", //$NON-NLS-1$
        "LPT9" //$NON-NLS-1$
    };

    /**
     * Class is static - prevent construction.
     */
    private BuildValidation() {

    }

}

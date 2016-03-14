// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.exceptions.BuildException;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.DefinitionQueueStatus;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.core.clients.build.soapextensions.Agent2008Status;
import com.microsoft.tfs.core.clients.build.soapextensions.AgentStatus;
import com.microsoft.tfs.core.clients.build.soapextensions.ControllerStatus;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.clients.build.soapextensions.QueuePriority;
import com.microsoft.tfs.util.StringUtil;

public class BuildEnumerationHelper {
    private static final Log log = LogFactory.getLog(BuildEnumerationHelper.class);
    private static final String ENUM_SEPERATOR = ", "; //$NON-NLS-1$

    /**
     * Gets the localized display text for known enumeration values (and the
     * ToString value for others).
     *
     *
     * @param value
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final Object value) {
        if (value == null) {
            return StringUtil.EMPTY;
        } else if (value instanceof AgentStatus) {
            return getDisplayText((AgentStatus) value);
        } else if (value instanceof Agent2008Status) {
            return getDisplayText((Agent2008Status) value);
        } else if (value instanceof BuildPhaseStatus) {
            return getDisplayText((BuildPhaseStatus) value);
        } else if (value instanceof BuildReason) {
            return getDisplayText((BuildReason) value);
        } else if (value instanceof BuildStatus) {
            return getDisplayText((BuildStatus) value);
        } else if (value instanceof ControllerStatus) {
            return getDisplayText((ControllerStatus) value);
        } else if (value instanceof DefinitionQueueStatus) {
            return getDisplayText((DefinitionQueueStatus) value);
        } else if (value instanceof DeleteOptions) {
            return getDisplayText((DeleteOptions) value);
        } else if (value instanceof QueuePriority) {
            return getDisplayText((QueuePriority) value);
        } else if (value instanceof QueueStatus) {
            return getDisplayText((QueueStatus) value);
        } else if (value instanceof DefinitionTriggerType) {
            return getDisplayText((DefinitionTriggerType) value);
        } else {
            return value.toString();
        }
    }

    /**
     * Gets the localized display text for AgentStatus values.
     *
     *
     * @param status
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final AgentStatus status) {
        if (status.equals(AgentStatus.UNAVAILABLE)) {
            return Messages.getString("BuildClient.AgentStatusUnavailable"); //$NON-NLS-1$
        }
        if (status.equals(AgentStatus.AVAILABLE)) {
            return Messages.getString("BuildClient.AgentStatusAvailable"); //$NON-NLS-1$
        }
        if (status.equals(AgentStatus.OFFLINE)) {
            return Messages.getString("BuildClient.AgentStatusOffline"); //$NON-NLS-1$
        }
        return StringUtil.EMPTY;
    }

    /**
     * Gets the localized display text for Agent2008Status values. This method
     * is for back compat and so is left internal for now.
     *
     *
     * @param status
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final Agent2008Status status) {
        if (status == Agent2008Status.ENABLED) {
            return Messages.getString("BuildClient.2008StatusEnabled"); //$NON-NLS-1$
        }
        if (status == Agent2008Status.DISABLED) {
            return Messages.getString("BuildClient.2008StatusDisabled"); //$NON-NLS-1$
        }
        if (status == Agent2008Status.UNREACHABLE) {
            return Messages.getString("BuildClient.2008StatusUnreachable"); //$NON-NLS-1$
        }
        if (status == Agent2008Status.INITIALIZING) {
            return Messages.getString("BuildClient.2008StatusInitializing"); //$NON-NLS-1$
        }
        return StringUtil.EMPTY;
    }

    /**
     * Gets the localized display text for BuildPhaseStatus values.
     *
     *
     * @param status
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final BuildPhaseStatus status) {
        if (status.equals(BuildPhaseStatus.FAILED)) {
            return Messages.getString("BuildClient.PhaseStatusFailed"); //$NON-NLS-1$
        }
        if (status.equals(BuildPhaseStatus.SUCCEEDED)) {
            return Messages.getString("BuildClient.PhaseStatusSucceeded"); //$NON-NLS-1$
        }
        if (status.equals(BuildPhaseStatus.UNKOWN)) {
            return Messages.getString("BuildClient.PhaseStatusUnknown"); //$NON-NLS-1$
        }
        return StringUtil.EMPTY;
    }

    /**
     * Gets the localized display text for BuildReason values.
     *
     *
     * @param reason
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final BuildReason reason) {
        final List<String> reasonStrings = new ArrayList<String>();

        if (reason.contains(BuildReason.ALL)) {
            reasonStrings.add(Messages.getString("BuildClient.BuildReasonAll")); //$NON-NLS-1$
        } else if (reason.contains(BuildReason.TRIGGERED)) {
            reasonStrings.add(Messages.getString("BuildClient.BuildReasonTriggeredAndManual")); //$NON-NLS-1$
        } else if (reason.equals(BuildReason.NONE)) {
            reasonStrings.add(Messages.getString("BuildClient.BuildReasonNone")); //$NON-NLS-1$
        } else {
            if (reason.contains(BuildReason.BATCHED_CI)) {
                reasonStrings.add(Messages.getString("BuildClient.BuildReasonBatchedCI")); //$NON-NLS-1$
            }
            if (reason.contains(BuildReason.INDIVIDUAL_CI)) {
                reasonStrings.add(Messages.getString("BuildClient.BuildReasonIndividualCI")); //$NON-NLS-1$
            }
            if (reason.contains(BuildReason.MANUAL)) {
                reasonStrings.add(Messages.getString("BuildClient.BuildReasonManual")); //$NON-NLS-1$
            }
            if (reason.contains(BuildReason.SCHEDULE)) {
                reasonStrings.add(Messages.getString("BuildClient.BuildReasonScheduled")); //$NON-NLS-1$
            }
            if (reason.contains(BuildReason.SCHEDULE_FORCED)) {
                reasonStrings.add(Messages.getString("BuildClient.BuildReasonScheduledForced")); //$NON-NLS-1$
            }
            if (reason.contains(BuildReason.USER_CREATED)) {
                reasonStrings.add(Messages.getString("BuildClient.BuildReasonUserCreated")); //$NON-NLS-1$
            }
            if (reason.contains(BuildReason.VALIDATE_SHELVESET)) {
                reasonStrings.add(Messages.getString("BuildClient.BuildReasonPrivate")); //$NON-NLS-1$
            }
            if (reason.contains(BuildReason.CHECK_IN_SHELVESET)) {
                reasonStrings.add(Messages.getString("BuildClient.BuildReasonCheckInShelveset")); //$NON-NLS-1$
            }
        }
        return StringUtil.join(reasonStrings.toArray(new String[reasonStrings.size()]), ENUM_SEPERATOR);
    }

    /**
     * Gets the localized display text for BuildStatus values.
     *
     *
     * @param status
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final BuildStatus status) {
        final List<String> statusStrings = new ArrayList<String>();

        if (status.contains(BuildStatus.ALL)) {
            statusStrings.add(Messages.getString("BuildClient.BuildStatusAll")); //$NON-NLS-1$
        } else if (status.equals(BuildStatus.NONE)) {
            statusStrings.add(Messages.getString("BuildClient.BuildStatusNone")); //$NON-NLS-1$
        } else {
            if (status.contains(BuildStatus.IN_PROGRESS)) {
                statusStrings.add(Messages.getString("BuildClient.BuildStatusInProgress")); //$NON-NLS-1$
            }
            if (status.contains(BuildStatus.SUCCEEDED)) {
                statusStrings.add(Messages.getString("BuildClient.BuildStatusSucceeded")); //$NON-NLS-1$
            }
            if (status.contains(BuildStatus.PARTIALLY_SUCCEEDED)) {
                statusStrings.add(Messages.getString("BuildClient.BuildStatusPartiallySucceeded")); //$NON-NLS-1$
            }
            if (status.contains(BuildStatus.FAILED)) {
                statusStrings.add(Messages.getString("BuildClient.BuildStatusFailed")); //$NON-NLS-1$
            }
            if (status.contains(BuildStatus.STOPPED)) {
                statusStrings.add(Messages.getString("BuildClient.BuildStatusStopped")); //$NON-NLS-1$
            }
            if (status.contains(BuildStatus.NOT_STARTED)) {
                statusStrings.add(Messages.getString("BuildClient.BuildStatusNotStarted")); //$NON-NLS-1$
            }
        }

        return StringUtil.join(statusStrings.toArray(new String[statusStrings.size()]), ENUM_SEPERATOR);
    }

    /**
     * Gets the localized display text for ControllerStatus values.
     *
     *
     * @param status
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final ControllerStatus status) {
        if (status.equals(ControllerStatus.UNAVAILABLE)) {
            return Messages.getString("BuildClient.ControllerStatusUnavailable"); //$NON-NLS-1$
        }
        if (status.equals(ControllerStatus.AVAILABLE)) {
            return Messages.getString("BuildClient.ControllerStatusAvailable"); //$NON-NLS-1$
        }
        if (status.equals(ControllerStatus.OFFLINE)) {
            return Messages.getString("BuildClient.ControllerStatusOffline"); //$NON-NLS-1$
        }
        return StringUtil.EMPTY;
    }

    /**
     * Gets the localized display text for DefinitionQueueStatus values.
     *
     *
     * @param status
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final DefinitionQueueStatus status) {
        if (status.equals(DefinitionQueueStatus.DISABLED)) {
            return Messages.getString("BuildEnumerationHelper.DefinitionQueueStatusDisabled"); //$NON-NLS-1$
        }
        if (status.equals(DefinitionQueueStatus.ENABLED)) {
            return Messages.getString("BuildEnumerationHelper.DefinitionQueueStatusEnabled"); //$NON-NLS-1$
        }
        if (status.equals(DefinitionQueueStatus.PAUSED)) {
            return Messages.getString("BuildEnumerationHelper.DefinitionQueueStatusPaused"); //$NON-NLS-1$
        }
        return StringUtil.EMPTY;
    }

    /**
     * Gets the localized display text for DeleteOptions values.
     *
     *
     * @param options
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final DeleteOptions options) {
        final List<String> optionsStrings = new ArrayList<String>();

        if (options.contains(DeleteOptions.ALL)) {
            optionsStrings.add(Messages.getString("BuildEnumerationHelper.DeleteOptionsAll")); //$NON-NLS-1$
        } else if (options.equals(DeleteOptions.NONE)) {
            optionsStrings.add(Messages.getString("BuildEnumerationHelper.DeleteOptionsNone")); //$NON-NLS-1$
        } else {
            if (options.contains(DeleteOptions.DETAILS)) {
                optionsStrings.add(Messages.getString("BuildEnumerationHelper.DeleteOptionsDetails")); //$NON-NLS-1$
            }
            if (options.contains(DeleteOptions.DROP_LOCATION)) {
                optionsStrings.add(Messages.getString("BuildEnumerationHelper.DeleteOptionsDrop")); //$NON-NLS-1$
            }
            if (options.contains(DeleteOptions.LABEL)) {
                optionsStrings.add(Messages.getString("BuildEnumerationHelper.DeleteOptionsLabel")); //$NON-NLS-1$
            }
            if (options.contains(DeleteOptions.TEST_RESULTS)) {
                optionsStrings.add(Messages.getString("BuildEnumerationHelper.DeleteOptionsTestResults")); //$NON-NLS-1$
            }
            if (options.contains(DeleteOptions.SYMBOLS)) {
                optionsStrings.add(Messages.getString("BuildEnumerationHelper.DeleteOptionsSymbols")); //$NON-NLS-1$
            }
        }

        return StringUtil.join(optionsStrings.toArray(new String[optionsStrings.size()]), ENUM_SEPERATOR);
    }

    /**
     * Gets the localized display text for QueuePriority values.
     *
     *
     * @param priority
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final QueuePriority priority) {
        if (priority.equals(QueuePriority.ABOVE_NORMAL)) {
            return Messages.getString("BuildEnumerationHelper.QueuePriorityAboveNormal"); //$NON-NLS-1$
        }
        if (priority.equals(QueuePriority.BELOW_NORMAL)) {
            return Messages.getString("BuildEnumerationHelper.QueuePriorityBelowNormal"); //$NON-NLS-1$
        }
        if (priority.equals(QueuePriority.HIGH)) {
            return Messages.getString("BuildEnumerationHelper.QueuePriorityHigh"); //$NON-NLS-1$
        }
        if (priority.equals(QueuePriority.LOW)) {
            return Messages.getString("BuildEnumerationHelper.QueuePriorityLow"); //$NON-NLS-1$
        }
        if (priority.equals(QueuePriority.NORMAL)) {
            return Messages.getString("BuildEnumerationHelper.QueuePriorityNormal"); //$NON-NLS-1$
        }
        return StringUtil.EMPTY;
    }

    /**
     * Gets the localized display text for QueueStatus values.
     *
     *
     * @param status
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final QueueStatus status) {
        final List<String> statusStrings = new ArrayList<String>();

        if (status.contains(QueueStatus.ALL)) {
            statusStrings.add(Messages.getString("BuildEnumerationHelper.StatusAll")); //$NON-NLS-1$
        } else if (status.equals(QueueStatus.NONE)) {
            statusStrings.add(Messages.getString("BuildEnumerationHelper.StatusNone")); //$NON-NLS-1$
        } else {
            if (status.contains(QueueStatus.CANCELED)) {
                statusStrings.add(Messages.getString("BuildEnumerationHelper.StatusCanceled")); //$NON-NLS-1$
            }
            if (status.contains(QueueStatus.COMPLETED)) {
                statusStrings.add(Messages.getString("BuildEnumerationHelper.StatusCompleted")); //$NON-NLS-1$
            }
            if (status.contains(QueueStatus.IN_PROGRESS)) {
                statusStrings.add(Messages.getString("BuildEnumerationHelper.StatusInProgress")); //$NON-NLS-1$
            }
            if (status.contains(QueueStatus.POSTPONED)) {
                statusStrings.add(Messages.getString("BuildEnumerationHelper.StatusPostponed")); //$NON-NLS-1$
            }
            if (status.contains(QueueStatus.QUEUED)) {
                statusStrings.add(Messages.getString("BuildEnumerationHelper.StatusQueued")); //$NON-NLS-1$
            }
            if (status.contains(QueueStatus.RETRY)) {
                statusStrings.add(Messages.getString("BuildEnumerationHelper.StatusRetry")); //$NON-NLS-1$
            }
        }

        return StringUtil.join(statusStrings.toArray(new String[statusStrings.size()]), ENUM_SEPERATOR);
    }

    /**
     * Gets the localized display text for DefinitionTriggerType values.
     *
     *
     * @param value
     *        The value for which display text is returned.
     * @return The localized display text.
     */
    public static String getDisplayText(final DefinitionTriggerType value) {
        final List<String> triggerStrings = new ArrayList<String>();

        if (value.contains(DefinitionTriggerType.ALL)) {
            triggerStrings.add(Messages.getString("BuildEnumerationHelper.DefinitionTriggerAll")); //$NON-NLS-1$
        } else if (value.equals(DefinitionTriggerType.NONE)) {
            triggerStrings.add(Messages.getString("BuildEnumerationHelper.DefinitionTriggerManual")); //$NON-NLS-1$
        } else {
            if (value.contains(DefinitionTriggerType.BATCHED_CONTINUOUS_INTEGRATION)) {
                triggerStrings.add(Messages.getString("BuildEnumerationHelper.DefinitionTriggerBatchedCI")); //$NON-NLS-1$
            }
            if (value.contains(DefinitionTriggerType.CONTINUOUS_INTEGRATION)) {
                triggerStrings.add(Messages.getString("BuildEnumerationHelper.DefinitionTriggerCI")); //$NON-NLS-1$
            }
            if (value.contains(DefinitionTriggerType.SCHEDULE)) {
                triggerStrings.add(Messages.getString("BuildEnumerationHelper.DefinitionTriggerSchedule")); //$NON-NLS-1$
            }
            if (value.contains(DefinitionTriggerType.SCHEDULE_FORCED)) {
                triggerStrings.add(Messages.getString("BuildEnumerationHelper.DefinitionTriggerScheduleForce")); //$NON-NLS-1$
            }
            if (value.contains(DefinitionTriggerType.GATED_CHECKIN)) {
                triggerStrings.add(Messages.getString("BuildEnumerationHelper.DefinitionTriggerGated")); //$NON-NLS-1$
            }
            if (value.contains(DefinitionTriggerType.BATCHED_GATED_CHECKIN)) {
                triggerStrings.add(Messages.getString("BuildEnumerationHelper.DefinitionTriggerBatchedGated")); //$NON-NLS-1$
            }
        }

        return StringUtil.join(triggerStrings.toArray(new String[triggerStrings.size()]), ENUM_SEPERATOR);
    }

    @SuppressWarnings("rawtypes")
    public static String[] getDisplayTextValues(final Class enumType) {
        final List<String> displayValues = new ArrayList<String>();

        // Look for public static final fields in class that are same type as
        // the passed class.
        final Field[] fields = enumType.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getType().equals(enumType)
                && Modifier.isPublic(fields[i].getModifiers())
                && Modifier.isFinal(fields[i].getModifiers())
                && Modifier.isStatic(fields[i].getModifiers())) {
                try {
                    displayValues.add(getDisplayText(fields[i].get(null)));
                } catch (final IllegalAccessException e) {
                    throw new BuildException(
                        MessageFormat.format(
                            "IllegalAccessException calculating display values for {0}", //$NON-NLS-1$
                            enumType.getName()),
                        e);
                }
            }
        }
        return displayValues.toArray(new String[displayValues.size()]);
    }
}

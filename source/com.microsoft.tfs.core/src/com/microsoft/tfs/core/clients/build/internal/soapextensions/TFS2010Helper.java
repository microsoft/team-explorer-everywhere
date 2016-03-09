// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.build.IBuildAgentSpec;
import com.microsoft.tfs.core.clients.build.IBuildControllerSpec;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.IBuildDetailSpec;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IBuildServiceHost;
import com.microsoft.tfs.core.clients.build.IFailure;
import com.microsoft.tfs.core.clients.build.IFailure2010;
import com.microsoft.tfs.core.clients.build.IRetentionPolicy;
import com.microsoft.tfs.core.clients.build.ISchedule;
import com.microsoft.tfs.core.clients.build.flags.AgentStatus2010;
import com.microsoft.tfs.core.clients.build.flags.BuildAgentUpdate;
import com.microsoft.tfs.core.clients.build.flags.BuildAgentUpdate2010;
import com.microsoft.tfs.core.clients.build.flags.BuildControllerUpdate;
import com.microsoft.tfs.core.clients.build.flags.BuildControllerUpdate2010;
import com.microsoft.tfs.core.clients.build.flags.BuildPhaseStatus2010;
import com.microsoft.tfs.core.clients.build.flags.BuildQueryOrder;
import com.microsoft.tfs.core.clients.build.flags.BuildQueryOrder2010;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildReason2010;
import com.microsoft.tfs.core.clients.build.flags.BuildServerVersion;
import com.microsoft.tfs.core.clients.build.flags.BuildServiceHostUpdate;
import com.microsoft.tfs.core.clients.build.flags.BuildServiceHostUpdate2010;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus2010;
import com.microsoft.tfs.core.clients.build.flags.BuildUpdate;
import com.microsoft.tfs.core.clients.build.flags.BuildUpdate2010;
import com.microsoft.tfs.core.clients.build.flags.ControllerStatus2010;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions2010;
import com.microsoft.tfs.core.clients.build.flags.GetOption2010;
import com.microsoft.tfs.core.clients.build.flags.InformationEditOptions;
import com.microsoft.tfs.core.clients.build.flags.InformationEditOptions2010;
import com.microsoft.tfs.core.clients.build.flags.ProcessTemplateType2010;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption2010;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions2010;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions2010;
import com.microsoft.tfs.core.clients.build.flags.QueuePriority2010;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus2010;
import com.microsoft.tfs.core.clients.build.flags.QueuedBuildUpdate;
import com.microsoft.tfs.core.clients.build.flags.QueuedBuildUpdate2010;
import com.microsoft.tfs.core.clients.build.flags.ScheduleDays;
import com.microsoft.tfs.core.clients.build.flags.ScheduleDays2010;
import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingType2010;
import com.microsoft.tfs.core.clients.build.soapextensions.AgentStatus;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.clients.build.soapextensions.ControllerStatus;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.clients.build.soapextensions.GetOption;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;
import com.microsoft.tfs.core.clients.build.soapextensions.QueuePriority;
import com.microsoft.tfs.core.clients.build.soapextensions.WorkspaceMappingType;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._04._BuildAgent;
import ms.tfs.build.buildservice._04._BuildController;
import ms.tfs.build.buildservice._04._BuildDefinition;
import ms.tfs.build.buildservice._04._BuildServiceHost;
import ms.tfs.build.buildservice._04._QueuedBuild;

public class TFS2010Helper {
    public static QueueStatus convert(final QueueStatus2010 status) {
        if (status == null) {
            return QueueStatus.NONE;
        }

        if (status.contains(QueueStatus2010.ALL)) {
            return QueueStatus.ALL;
        }

        QueueStatus newStatus = QueueStatus.NONE;

        if (status.contains(QueueStatus2010.CANCELED)) {
            newStatus = newStatus.combine(QueueStatus.CANCELED);
        }
        if (status.contains(QueueStatus2010.COMPLETED)) {
            newStatus = newStatus.combine(QueueStatus.COMPLETED);
        }
        if (status.contains(QueueStatus2010.IN_PROGRESS)) {
            newStatus = newStatus.combine(QueueStatus.IN_PROGRESS);
        }
        if (status.contains(QueueStatus2010.POSTPONED)) {
            newStatus = newStatus.combine(QueueStatus.POSTPONED);
        }
        if (status.contains(QueueStatus2010.QUEUED)) {
            newStatus = newStatus.combine(QueueStatus.QUEUED);
        }

        return newStatus;
    }

    public static BuildReason convert(final BuildReason2010 reason) {
        if (reason == null) {
            return BuildReason.NONE;
        }

        if (reason.contains(BuildReason2010.ALL)) {
            return BuildReason.ALL;
        }

        BuildReason newReason = BuildReason.NONE;

        if (reason.contains(BuildReason2010.BATCHED_CI)) {
            newReason = newReason.combine(BuildReason.BATCHED_CI);
        }
        if (reason.contains(BuildReason2010.CHECK_IN_SHELVESET)) {
            newReason = newReason.combine(BuildReason.CHECK_IN_SHELVESET);
        }
        if (reason.contains(BuildReason2010.INDIVIDUAL_CI)) {
            newReason = newReason.combine(BuildReason.INDIVIDUAL_CI);
        }
        if (reason.contains(BuildReason2010.MANUAL)) {
            newReason = newReason.combine(BuildReason.MANUAL);
        }
        if (reason.contains(BuildReason2010.SCHEDULE)) {
            newReason = newReason.combine(BuildReason.SCHEDULE);
        }
        if (reason.contains(BuildReason2010.SCHEDULE_FORCED)) {
            newReason = newReason.combine(BuildReason.SCHEDULE_FORCED);
        }
        if (reason.contains(BuildReason2010.TRIGGERED)) {
            newReason = newReason.combine(BuildReason.TRIGGERED);
        }
        if (reason.contains(BuildReason2010.USER_CREATED)) {
            newReason = newReason.combine(BuildReason.USER_CREATED);
        }
        if (reason.contains(BuildReason2010.VALIDATE_SHELVESET)) {
            newReason = newReason.combine(BuildReason.VALIDATE_SHELVESET);
        }

        return newReason;
    }

    public static QueuePriority convert(final QueuePriority2010 priority) {
        if (priority == null) {
            return QueuePriority.NORMAL;
        }

        if (priority.equals(QueuePriority2010.ABOVE_NORMAL)) {
            return QueuePriority.ABOVE_NORMAL;
        } else if (priority.equals(QueuePriority2010.BELOW_NORMAL)) {
            return QueuePriority.BELOW_NORMAL;
        } else if (priority.equals(QueuePriority2010.HIGH)) {
            return QueuePriority.HIGH;
        } else if (priority.equals(QueuePriority2010.LOW)) {
            return QueuePriority.LOW;
        } else {
            return QueuePriority.NORMAL;
        }
    }

    public static GetOption convert(final GetOption2010 options) {
        if (options.equals(GetOption2010.LATEST_ON_BUILD)) {
            return GetOption.LATEST_ON_BUILD;
        } else if (options.equals(GetOption2010.LATEST_ON_QUEUE)) {
            return GetOption.LATEST_ON_QUEUE;
        } else {
            return GetOption.CUSTOM;
        }
    }

    public static ProcessTemplateType2010 convert(final ProcessTemplateType type) {
        if (type.equals(ProcessTemplateType.DEFAULT)) {
            return ProcessTemplateType2010.DEFAULT;
        } else if (type.equals(ProcessTemplateType.UPGRADE)) {
            return ProcessTemplateType2010.UPGRADE;
        } else {
            return ProcessTemplateType2010.CUSTOM;
        }
    }

    public static ScheduleDays convert(final ScheduleDays2010 days) {
        if (days.containsAll(ScheduleDays2010.ALL)) {
            return new ScheduleDays(ScheduleDays.ALL);
        }

        final ScheduleDays newDays = new ScheduleDays();

        if (days.contains(ScheduleDays2010.FRIDAY)) {
            newDays.add(ScheduleDays.FRIDAY);
        }
        if (days.contains(ScheduleDays2010.MONDAY)) {
            newDays.add(ScheduleDays.MONDAY);
        }
        if (days.contains(ScheduleDays2010.SATURDAY)) {
            newDays.add(ScheduleDays.SATURDAY);
        }
        if (days.contains(ScheduleDays2010.SUNDAY)) {
            newDays.add(ScheduleDays.SUNDAY);
        }
        if (days.contains(ScheduleDays2010.THURSDAY)) {
            newDays.add(ScheduleDays.THURSDAY);
        }
        if (days.contains(ScheduleDays2010.TUESDAY)) {
            newDays.add(ScheduleDays.TUESDAY);
        }
        if (days.contains(ScheduleDays2010.WEDNESDAY)) {
            newDays.add(ScheduleDays.WEDNESDAY);
        }

        return newDays;
    }

    public static QueuedBuildUpdate2010 convert(final QueuedBuildUpdate update) {
        if (update == null) {
            return QueuedBuildUpdate2010.NONE;
        }

        QueuedBuildUpdate2010 newUpdate = QueuedBuildUpdate2010.NONE;

        if (update.contains(QueuedBuildUpdate.POSTPONED)) {
            newUpdate = newUpdate.combine(QueuedBuildUpdate2010.POSTPONED);
        }
        if (update.contains(QueuedBuildUpdate.PRIORITY)) {
            newUpdate = newUpdate.combine(QueuedBuildUpdate2010.PRIORITY);
        }

        return newUpdate;
    }

    public static QueuePriority2010 convert(final QueuePriority priority) {
        if (priority.equals(QueuePriority.ABOVE_NORMAL)) {
            return QueuePriority2010.ABOVE_NORMAL;
        } else if (priority.equals(QueuePriority.BELOW_NORMAL)) {
            return QueuePriority2010.BELOW_NORMAL;
        } else if (priority.equals(QueuePriority.HIGH)) {
            return QueuePriority2010.HIGH;
        } else if (priority.equals(QueuePriority.LOW)) {
            return QueuePriority2010.LOW;
        } else {
            return QueuePriority2010.NORMAL;
        }
    }

    public static DeleteOptions convert(final DeleteOptions2010 options) {
        if (options == null) {
            return DeleteOptions.NONE;
        }

        if (options.contains(DeleteOptions2010.ALL)) {
            return DeleteOptions.ALL;
        }

        DeleteOptions newOptions = DeleteOptions.NONE;

        if (options.contains(DeleteOptions2010.DETAILS)) {
            newOptions = newOptions.combine(DeleteOptions.DETAILS);
        }
        if (options.contains(DeleteOptions2010.DROP_LOCATION)) {
            newOptions = newOptions.combine(DeleteOptions.DROP_LOCATION);
        }
        if (options.contains(DeleteOptions2010.LABEL)) {
            newOptions = newOptions.combine(DeleteOptions.LABEL);
        }
        if (options.contains(DeleteOptions2010.SYMBOLS)) {
            newOptions = newOptions.combine(DeleteOptions.SYMBOLS);
        }
        if (options.contains(DeleteOptions2010.TEST_RESULTS)) {
            newOptions = newOptions.combine(DeleteOptions.TEST_RESULTS);
        }

        return newOptions;
    }

    public static BuildStatus convert(final BuildStatus2010 status) {
        if (status == null) {
            return BuildStatus.NONE;
        }

        if (status.contains(BuildStatus2010.ALL)) {
            return BuildStatus.ALL;
        }

        BuildStatus newStatus = BuildStatus.NONE;

        if (status.contains(BuildStatus2010.FAILED)) {
            newStatus = newStatus.combine(BuildStatus.FAILED);
        }
        if (status.contains(BuildStatus2010.IN_PROGRESS)) {
            newStatus = newStatus.combine(BuildStatus.IN_PROGRESS);
        }
        if (status.contains(BuildStatus2010.NOT_STARTED)) {
            newStatus = newStatus.combine(BuildStatus.NOT_STARTED);
        }
        if (status.contains(BuildStatus2010.PARTIALLY_SUCCEEDED)) {
            newStatus = newStatus.combine(BuildStatus.PARTIALLY_SUCCEEDED);
        }
        if (status.contains(BuildStatus2010.STOPPED)) {
            newStatus = newStatus.combine(BuildStatus.STOPPED);
        }
        if (status.contains(BuildStatus2010.SUCCEEDED)) {
            newStatus = newStatus.combine(BuildStatus.SUCCEEDED);
        }

        return newStatus;
    }

    public static ProcessTemplateType convert(final ProcessTemplateType2010 type) {
        if (type.equals(ProcessTemplateType2010.DEFAULT)) {
            return ProcessTemplateType.DEFAULT;
        } else if (type.equals(ProcessTemplateType2010.UPGRADE)) {
            return ProcessTemplateType.UPGRADE;
        } else {
            return ProcessTemplateType.CUSTOM;
        }
    }

    public static ProcessTemplateType[] convert(final ProcessTemplateType2010[] types) {
        final ProcessTemplateType[] newTypes = new ProcessTemplateType[types.length];

        for (int i = 0; i < types.length; i++) {
            newTypes[i] = convert(types[i]);
        }

        return newTypes;
    }

    public static ProcessTemplateType2010[] convert(final ProcessTemplateType[] types) {
        final ProcessTemplateType2010[] newTypes = new ProcessTemplateType2010[types.length];

        for (int i = 0; i < types.length; i++) {
            newTypes[i] = convert(types[i]);
        }

        return newTypes;
    }

    public static DeleteOptions2010 convert(final DeleteOptions options) {
        if (options == null) {
            return DeleteOptions2010.NONE;
        }

        if (options.contains(DeleteOptions.ALL)) {
            return DeleteOptions2010.ALL;
        }

        DeleteOptions2010 newOptions = DeleteOptions2010.NONE;

        if (options.contains(DeleteOptions.DETAILS)) {
            newOptions = newOptions.combine(DeleteOptions2010.DETAILS);
        }
        if (options.contains(DeleteOptions.DROP_LOCATION)) {
            newOptions = newOptions.combine(DeleteOptions2010.DROP_LOCATION);
        }
        if (options.contains(DeleteOptions.LABEL)) {
            newOptions = newOptions.combine(DeleteOptions2010.LABEL);
        }
        if (options.contains(DeleteOptions.SYMBOLS)) {
            newOptions = newOptions.combine(DeleteOptions2010.SYMBOLS);
        }
        if (options.contains(DeleteOptions.TEST_RESULTS)) {
            newOptions = newOptions.combine(DeleteOptions2010.TEST_RESULTS);
        }

        return newOptions;
    }

    public static InformationEditOptions2010 convert(final InformationEditOptions options) {
        if (options.equals(InformationEditOptions.REPLACE_FIELDS)) {
            return InformationEditOptions2010.REPLACE_FIELDS;
        } else {
            return InformationEditOptions2010.MERGE_FIELDS;
        }
    }

    public static WorkspaceMappingType2010 convert(final WorkspaceMappingType type) {
        if (type.equals(WorkspaceMappingType.MAP)) {
            return WorkspaceMappingType2010.MAP;
        } else {
            return WorkspaceMappingType2010.CLOAK;
        }
    }

    public static WorkspaceMappingType convert(final WorkspaceMappingType2010 type) {
        if (type.equals(WorkspaceMappingType2010.MAP)) {
            return WorkspaceMappingType.MAP;
        } else {
            return WorkspaceMappingType.CLOAK;
        }
    }

    public static BuildReason2010 convert(final BuildReason reason) {
        if (reason == null) {
            return BuildReason2010.NONE;
        }

        if (reason.contains(BuildReason.ALL)) {
            return BuildReason2010.ALL;
        }

        BuildReason2010 newReason = BuildReason2010.NONE;

        if (reason.contains(BuildReason.BATCHED_CI)) {
            newReason = newReason.combine(BuildReason2010.BATCHED_CI);
        }
        if (reason.contains(BuildReason.CHECK_IN_SHELVESET)) {
            newReason = newReason.combine(BuildReason2010.CHECK_IN_SHELVESET);
        }
        if (reason.contains(BuildReason.INDIVIDUAL_CI)) {
            newReason = newReason.combine(BuildReason2010.INDIVIDUAL_CI);
        }
        if (reason.contains(BuildReason.MANUAL)) {
            newReason = newReason.combine(BuildReason2010.MANUAL);
        }
        if (reason.contains(BuildReason.SCHEDULE)) {
            newReason = newReason.combine(BuildReason2010.SCHEDULE);
        }
        if (reason.contains(BuildReason.SCHEDULE_FORCED)) {
            newReason = newReason.combine(BuildReason2010.SCHEDULE_FORCED);
        }
        if (reason.contains(BuildReason.TRIGGERED)) {
            newReason = newReason.combine(BuildReason2010.TRIGGERED);
        }
        if (reason.contains(BuildReason.USER_CREATED)) {
            newReason = newReason.combine(BuildReason2010.USER_CREATED);
        }
        if (reason.contains(BuildReason.VALIDATE_SHELVESET)) {
            newReason = newReason.combine(BuildReason2010.VALIDATE_SHELVESET);
        }

        return newReason;
    }

    public static BuildStatus2010 convert(final BuildStatus status) {
        if (status == null) {
            return BuildStatus2010.NONE;
        }

        if (status.equals(BuildStatus.ALL)) {
            return BuildStatus2010.ALL;
        }

        BuildStatus2010 newStatus = BuildStatus2010.NONE;

        if (status.contains(BuildStatus.FAILED)) {
            newStatus = newStatus.combine(BuildStatus2010.FAILED);
        }
        if (status.contains(BuildStatus.IN_PROGRESS)) {
            newStatus = newStatus.combine(BuildStatus2010.IN_PROGRESS);
        }
        if (status.contains(BuildStatus.NOT_STARTED)) {
            newStatus = newStatus.combine(BuildStatus2010.NOT_STARTED);
        }
        if (status.contains(BuildStatus.PARTIALLY_SUCCEEDED)) {
            newStatus = newStatus.combine(BuildStatus2010.PARTIALLY_SUCCEEDED);
        }
        if (status.contains(BuildStatus.STOPPED)) {
            newStatus = newStatus.combine(BuildStatus2010.STOPPED);
        }
        if (status.contains(BuildStatus.SUCCEEDED)) {
            newStatus = newStatus.combine(BuildStatus2010.SUCCEEDED);
        }

        return newStatus;
    }

    public static ScheduleDays2010 convert(final ScheduleDays days) {
        if (days.containsAll(ScheduleDays.ALL)) {
            return new ScheduleDays2010(ScheduleDays2010.ALL);
        }

        final ScheduleDays2010 newDays = new ScheduleDays2010();

        if ((days.contains(ScheduleDays.FRIDAY))) {
            newDays.add(ScheduleDays2010.FRIDAY);
        }
        if (days.contains(ScheduleDays.MONDAY)) {
            newDays.add(ScheduleDays2010.MONDAY);
        }
        if (days.contains(ScheduleDays.SATURDAY)) {
            newDays.add(ScheduleDays2010.SATURDAY);
        }
        if (days.contains(ScheduleDays.SUNDAY)) {
            newDays.add(ScheduleDays2010.SUNDAY);
        }
        if (days.contains(ScheduleDays.THURSDAY)) {
            newDays.add(ScheduleDays2010.THURSDAY);
        }
        if (days.contains(ScheduleDays.TUESDAY)) {
            newDays.add(ScheduleDays2010.TUESDAY);
        }
        if (days.contains(ScheduleDays.WEDNESDAY)) {
            newDays.add(ScheduleDays2010.WEDNESDAY);
        }

        return newDays;
    }

    public static BuildPhaseStatus2010 convert(final BuildPhaseStatus status) {
        if (status.equals(BuildPhaseStatus.FAILED)) {
            return BuildPhaseStatus2010.FAILED;
        } else if (status.equals(BuildPhaseStatus.SUCCEEDED)) {
            return BuildPhaseStatus2010.SUCCEEDED;
        } else {
            return BuildPhaseStatus2010.UNKOWN;
        }
    }

    public static BuildUpdate2010 convert(final BuildUpdate update) {
        final BuildUpdate2010 newUpdate = new BuildUpdate2010();

        if (update.contains(BuildUpdate.BUILD_NUMBER)) {
            newUpdate.add(BuildUpdate2010.BUILD_NUMBER);
        }
        if (update.contains(BuildUpdate.COMPILATION_STATUS)) {
            newUpdate.add(BuildUpdate2010.COMPILATION_STATUS);
        }
        if (update.contains(BuildUpdate.DROP_LOCATION)) {
            newUpdate.add(BuildUpdate2010.DROP_LOCATION);
        }
        if (update.contains(BuildUpdate.KEEP_FOREVER)) {
            newUpdate.add(BuildUpdate2010.KEEP_FOREVER);
        }
        if (update.contains(BuildUpdate.LABEL_NAME)) {
            newUpdate.add(BuildUpdate2010.LABEL_NAME);
        }
        if (update.contains(BuildUpdate.LOG_LOCATION)) {
            newUpdate.add(BuildUpdate2010.LOG_LOCATION);
        }
        if (update.contains(BuildUpdate.QUALITY)) {
            newUpdate.add(BuildUpdate2010.QUALITY);
        }
        if (update.contains(BuildUpdate.SOURCE_GET_VERSION)) {
            newUpdate.add(BuildUpdate2010.SOURCE_GET_VERSION);
        }
        if (update.contains(BuildUpdate.STATUS)) {
            newUpdate.add(BuildUpdate2010.STATUS);
        }
        if (update.contains(BuildUpdate.TEST_STATUS)) {
            newUpdate.add(BuildUpdate2010.TEST_STATUS);
        }

        return newUpdate;
    }

    public static BuildServiceHostUpdate2010 convert(final BuildServiceHostUpdate update) {
        if (update == null) {
            return BuildServiceHostUpdate2010.NONE;
        }

        BuildServiceHostUpdate2010 newUpdate = BuildServiceHostUpdate2010.NONE;

        if (update.contains(BuildServiceHostUpdate.BASE_URI)) {
            newUpdate = newUpdate.combine(BuildServiceHostUpdate2010.BASE_URI);
        }
        if (update.contains(BuildServiceHostUpdate.NAME)) {
            newUpdate = newUpdate.combine(BuildServiceHostUpdate2010.NAME);
        }
        if (update.contains(BuildServiceHostUpdate.REQUIRE_CLIENT_CERTIFICATE)) {
            newUpdate = newUpdate.combine(BuildServiceHostUpdate2010.REQUIRE_CLIENT_CERTIFICATE);
        }

        return newUpdate;
    }

    public static GetOption2010 convert(final GetOption options) {
        if (options.equals(GetOption.LATEST_ON_BUILD)) {
            return GetOption2010.LATEST_ON_BUILD;
        } else if (options.equals(GetOption.LATEST_ON_QUEUE)) {
            return GetOption2010.LATEST_ON_QUEUE;
        } else {
            return GetOption2010.CUSTOM;
        }
    }

    public static QueueStatus2010 convert(final QueueStatus status) {
        if (status == null) {
            return QueueStatus2010.NONE;
        }

        if (status.containsAll(QueueStatus.ALL)) {
            return QueueStatus2010.ALL;
        }

        QueueStatus2010 newStatus = QueueStatus2010.NONE;

        if (status.contains(QueueStatus.CANCELED)) {
            newStatus = newStatus.combine(QueueStatus2010.CANCELED);
        }
        if (status.contains(QueueStatus.COMPLETED)) {
            newStatus = newStatus.combine(QueueStatus2010.COMPLETED);
        }
        if (status.contains(QueueStatus.IN_PROGRESS)) {
            newStatus = newStatus.combine(QueueStatus2010.IN_PROGRESS);
        }
        if (status.contains(QueueStatus.POSTPONED)) {
            newStatus = newStatus.combine(QueueStatus2010.POSTPONED);
        }
        if (status.contains(QueueStatus.QUEUED)) {
            newStatus = newStatus.combine(QueueStatus2010.QUEUED);
        }
        if (status.contains(QueueStatus.RETRY)) {
            newStatus = newStatus.combine(QueueStatus2010.IN_PROGRESS);
        }
        return newStatus;
    }

    public static BuildQueryOrder2010 convert(final BuildQueryOrder order) {
        if (order.equals(BuildQueryOrder.FINISH_TIME_ASCENDING)) {
            return BuildQueryOrder2010.FINISH_TIME_ASCENDING;
        } else if (order.equals(BuildQueryOrder.FINISH_TIME_DESCENDING)) {
            return BuildQueryOrder2010.FINISH_TIME_DESCENDING;
        } else if (order.equals(BuildQueryOrder.START_TIME_ASCENDING)) {
            return BuildQueryOrder2010.START_TIME_ASCENDING;
        } else if (order.equals(BuildQueryOrder.START_TIME_DESCENDING)) {
            return BuildQueryOrder2010.START_TIME_DESCENDING;
        } else {
            return BuildQueryOrder2010.START_TIME_ASCENDING;
        }
    }

    public static BuildControllerUpdate2010 convert(final BuildControllerUpdate update) {
        if (update == null) {
            return BuildControllerUpdate2010.NONE;
        }

        BuildControllerUpdate2010 newUpdate = BuildControllerUpdate2010.NONE;

        if (update.contains(BuildControllerUpdate.CUSTOM_ASSEMBLY_PATH)) {
            newUpdate = newUpdate.combine(BuildControllerUpdate2010.CUSTOM_ASSEMBLY_PATH);
        }
        if (update.contains(BuildControllerUpdate.DESCRIPTION)) {
            newUpdate = newUpdate.combine(BuildControllerUpdate2010.DESCRIPTION);
        }
        if (update.contains(BuildControllerUpdate.ENABLED)) {
            newUpdate = newUpdate.combine(BuildControllerUpdate2010.ENABLED);
        }
        if (update.contains(BuildControllerUpdate.MAX_CONCURRENT_BUILDS)) {
            newUpdate = newUpdate.combine(BuildControllerUpdate2010.MAX_CONCURRENT_BUILDS);
        }
        if (update.contains(BuildControllerUpdate.NAME)) {
            newUpdate = newUpdate.combine(BuildControllerUpdate2010.NAME);
        }
        if (update.contains(BuildControllerUpdate.STATUS)) {
            newUpdate = newUpdate.combine(BuildControllerUpdate2010.STATUS);
        }
        if (update.contains(BuildControllerUpdate.STATUS_MESSAGE)) {
            newUpdate = newUpdate.combine(BuildControllerUpdate2010.STATUS_MESSAGE);
        }

        return newUpdate;
    }

    public static ControllerStatus convert(final ControllerStatus2010 status) {
        if (status.equals(ControllerStatus2010.AVAILABLE)) {
            return ControllerStatus.AVAILABLE;
        } else if (status.equals(ControllerStatus2010.OFFLINE)) {
            return ControllerStatus.OFFLINE;
        } else if (status.equals(ControllerStatus2010.UNAVAILABLE)) {
            return ControllerStatus.UNAVAILABLE;
        } else {
            return ControllerStatus.UNAVAILABLE;
        }
    }

    public static ControllerStatus2010 convert(final ControllerStatus status) {
        if (status.equals(ControllerStatus.AVAILABLE)) {
            return ControllerStatus2010.AVAILABLE;
        } else if (status.equals(ControllerStatus.OFFLINE)) {
            return ControllerStatus2010.OFFLINE;
        } else if (status.equals(ControllerStatus.UNAVAILABLE)) {
            return ControllerStatus2010.UNAVAILABLE;
        } else {
            return ControllerStatus2010.UNAVAILABLE;
        }
    }

    public static AgentStatus2010 convert(final AgentStatus status) {
        if (status.equals(AgentStatus.AVAILABLE)) {
            return AgentStatus2010.AVAILABLE;
        } else if (status.equals(AgentStatus.OFFLINE)) {
            return AgentStatus2010.OFFLINE;
        } else if (status.equals(AgentStatus.UNAVAILABLE)) {
            return AgentStatus2010.UNAVAILABLE;
        } else {
            return AgentStatus2010.UNAVAILABLE;
        }
    }

    public static BuildAgentUpdate2010 convert(final BuildAgentUpdate update) {
        if (update == null) {
            return BuildAgentUpdate2010.NONE;
        }

        BuildAgentUpdate2010 newUpdate = BuildAgentUpdate2010.NONE;

        if (update.contains(BuildAgentUpdate.BUILD_DIRECTORY)) {
            newUpdate = newUpdate.combine(BuildAgentUpdate2010.BUILD_DIRECTORY);
        }
        if (update.contains(BuildAgentUpdate.CONTROLLER_URI)) {
            newUpdate = newUpdate.combine(BuildAgentUpdate2010.CONTROLLER_URI);
        }
        if (update.contains(BuildAgentUpdate.DESCRIPTION)) {
            newUpdate = newUpdate.combine(BuildAgentUpdate2010.DESCRIPTION);
        }
        if (update.contains(BuildAgentUpdate.ENABLED)) {
            newUpdate = newUpdate.combine(BuildAgentUpdate2010.ENABLED);
        }
        if (update.contains(BuildAgentUpdate.NAME)) {
            newUpdate = newUpdate.combine(BuildAgentUpdate2010.NAME);
        }
        if (update.contains(BuildAgentUpdate.STATUS)) {
            newUpdate = newUpdate.combine(BuildAgentUpdate2010.STATUS);
        }
        if (update.contains(BuildAgentUpdate.STATUS_MESSAGE)) {
            newUpdate = newUpdate.combine(BuildAgentUpdate2010.STATUS_MESSAGE);
        }
        if (update.contains(BuildAgentUpdate.TAGS)) {
            newUpdate = newUpdate.combine(BuildAgentUpdate2010.TAGS);
        }

        return newUpdate;
    }

    public static BuildPhaseStatus convert(final BuildPhaseStatus2010 status) {
        if (status.equals(BuildPhaseStatus2010.FAILED)) {
            return BuildPhaseStatus.FAILED;
        } else if (status.equals(BuildPhaseStatus2010.SUCCEEDED)) {
            return BuildPhaseStatus.SUCCEEDED;
        } else {
            return BuildPhaseStatus.UNKOWN;
        }
    }

    public static AgentStatus convert(final AgentStatus2010 status) {
        if (status.equals(AgentStatus2010.AVAILABLE)) {
            return AgentStatus.AVAILABLE;
        } else if (status.equals(AgentStatus2010.OFFLINE)) {
            return AgentStatus.OFFLINE;
        } else if (status.equals(AgentStatus2010.UNAVAILABLE)) {
            return AgentStatus.UNAVAILABLE;
        } else {
            return AgentStatus.UNAVAILABLE;
        }
    }

    public static QueryDeletedOption2010 convert(final QueryDeletedOption option) {
        if (option.equals(QueryDeletedOption.EXCLUDE_DELETED)) {
            return QueryDeletedOption2010.EXCLUDE_DELETED;
        } else if (option.equals(QueryDeletedOption.INCLUDE_DELETED)) {
            return QueryDeletedOption2010.INCLUDE_DELETED;
        } else if (option.equals(QueryDeletedOption.ONLY_DELETED)) {
            return QueryDeletedOption2010.ONLY_DELETED;
        } else {
            return QueryDeletedOption2010.EXCLUDE_DELETED;
        }
    }

    public static QueryOptions2010 convert(final QueryOptions options) {
        if (options == null) {
            return QueryOptions2010.NONE;
        }

        if (options.contains(QueryOptions.ALL)) {
            return QueryOptions2010.ALL;
        }

        QueryOptions2010 newOptions = QueryOptions2010.NONE;

        if (options.contains(QueryOptions.AGENTS)) {
            newOptions = newOptions.combine(QueryOptions2010.AGENTS);
        }
        if (options.contains(QueryOptions.CONTROLLERS)) {
            newOptions = newOptions.combine(QueryOptions2010.CONTROLLERS);
        }
        if (options.contains(QueryOptions.DEFINITIONS)) {
            newOptions = newOptions.combine(QueryOptions2010.DEFINITIONS);
        }
        if (options.contains(QueryOptions.PROCESS)) {
            newOptions = newOptions.combine(QueryOptions2010.PROCESS);
        }
        if (options.contains(QueryOptions.WORKSPACES)) {
            newOptions = newOptions.combine(QueryOptions2010.WORKSPACES);
        }

        return newOptions;
    }

    public static QueueOptions2010 convert(final QueueOptions options) {
        if (options == null) {
            return QueueOptions2010.NONE;
        }

        if (options.equals(QueueOptions.PREVIEW)) {
            return QueueOptions2010.PREVIEW;
        } else {
            return QueueOptions2010.NONE;
        }
    }

    public static ContinuousIntegrationType convert(final DefinitionTriggerType trigger) {
        if (trigger == null) {
            return ContinuousIntegrationType.NONE;
        }

        if (trigger.contains(DefinitionTriggerType.ALL)) {
            return ContinuousIntegrationType.ALL;
        }

        ContinuousIntegrationType newTrigger = ContinuousIntegrationType.NONE;

        if (trigger.contains(DefinitionTriggerType.BATCHED_CONTINUOUS_INTEGRATION)) {
            newTrigger = newTrigger.combine(ContinuousIntegrationType.BATCH);
        }
        if (trigger.contains(DefinitionTriggerType.BATCHED_GATED_CHECKIN)) {
            newTrigger = newTrigger.combine(ContinuousIntegrationType.GATED);
        }
        if (trigger.contains(DefinitionTriggerType.CONTINUOUS_INTEGRATION)) {
            newTrigger = newTrigger.combine(ContinuousIntegrationType.INDIVIDUAL);
        }
        if (trigger.contains(DefinitionTriggerType.GATED_CHECKIN)) {
            newTrigger = newTrigger.combine(ContinuousIntegrationType.GATED);
        }
        if (trigger.contains(DefinitionTriggerType.SCHEDULE)) {
            newTrigger = newTrigger.combine(ContinuousIntegrationType.SCHEDULE);
        }
        if (trigger.contains(DefinitionTriggerType.SCHEDULE_FORCED)) {
            newTrigger = newTrigger.combine(ContinuousIntegrationType.SCHEDULE_FORCED);
        }

        return newTrigger;
    }

    public static DefinitionTriggerType convert(final ContinuousIntegrationType trigger) {
        if (trigger == null) {
            return DefinitionTriggerType.NONE;
        }

        if (trigger.contains(ContinuousIntegrationType.ALL)) {
            return DefinitionTriggerType.ALL;
        }

        DefinitionTriggerType newTrigger = DefinitionTriggerType.NONE;

        if (trigger.contains(ContinuousIntegrationType.BATCH)) {
            newTrigger = newTrigger.combine(DefinitionTriggerType.BATCHED_CONTINUOUS_INTEGRATION);
        }
        if (trigger.contains(ContinuousIntegrationType.GATED)) {
            newTrigger = newTrigger.combine(DefinitionTriggerType.GATED_CHECKIN);
        }
        if (trigger.contains(ContinuousIntegrationType.INDIVIDUAL)) {
            newTrigger = newTrigger.combine(DefinitionTriggerType.CONTINUOUS_INTEGRATION);
        }
        if (trigger.contains(ContinuousIntegrationType.SCHEDULE)) {
            newTrigger = newTrigger.combine(DefinitionTriggerType.SCHEDULE);
        }
        if (trigger.contains(ContinuousIntegrationType.SCHEDULE_FORCED)) {
            newTrigger = newTrigger.combine(DefinitionTriggerType.SCHEDULE_FORCED);
        }

        return newTrigger;
    }

    public static BuildDetail convert(final IBuildServer buildServer, final BuildDetail2010 build) {
        if (build == null) {
            return null;
        }
        return new BuildDetail(buildServer, build);
    }

    public static BuildDefinitionSpec2010 convert(final IBuildDefinitionSpec spec) {
        if (spec == null) {
            return null;
        }
        return new BuildDefinitionSpec2010(spec);
    }

    public static BuildDefinitionSpec2010[] convert(final IBuildDefinitionSpec[] specs) {
        final BuildDefinitionSpec2010[] newSpecs = new BuildDefinitionSpec2010[specs.length];
        for (int i = 0; i < specs.length; i++) {
            newSpecs[i] = convert(specs[i]);
        }
        return newSpecs;
    }

    public static BuildQueueSpec2010[] convert(final BuildQueueSpec[] specs) {
        final BuildQueueSpec2010[] newSpecs = new BuildQueueSpec2010[specs.length];
        for (int i = 0; i < specs.length; i++) {
            newSpecs[i] = new BuildQueueSpec2010(specs[i]);
        }
        return newSpecs;
    }

    public static BuildDetail[] convert(final IBuildServer buildServer, final BuildDetail2010[] builds) {
        final BuildDetail[] newBuilds = new BuildDetail[builds.length];
        for (int i = 0; i < builds.length; i++) {
            newBuilds[i] = convert(buildServer, builds[i]);
        }
        return newBuilds;
    }

    public static BuildDetailSpec2010 convert(final IBuildServer buildServer, final IBuildDetailSpec spec) {
        return new BuildDetailSpec2010(buildServer, spec);
    }

    public static BuildDetailSpec2010[] convert(final IBuildServer buildServer, final IBuildDetailSpec[] specs) {
        final BuildDetailSpec2010[] newSpecs = new BuildDetailSpec2010[specs.length];
        for (int i = 0; i < specs.length; i++) {
            newSpecs[i] = convert(buildServer, specs[i]);
        }
        return newSpecs;
    }

    public static BuildDeletionResult[] convert(final BuildDeletionResult2010[] results) {
        final BuildDeletionResult[] newResults = new BuildDeletionResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = new BuildDeletionResult(results[i]);
        }
        return newResults;
    }

    public static BuildControllerSpec2010[] convert(final IBuildControllerSpec[] specs) {
        final BuildControllerSpec2010[] newSpecs = new BuildControllerSpec2010[specs.length];
        for (int i = 0; i < specs.length; i++) {
            newSpecs[i] = new BuildControllerSpec2010((BuildControllerSpec) specs[i]);
        }
        return newSpecs;
    }

    public static BuildControllerUpdateOptions2010[] convert(final BuildControllerUpdateOptions[] updates) {
        final BuildControllerUpdateOptions2010[] newUpdates = new BuildControllerUpdateOptions2010[updates.length];
        for (int i = 0; i < updates.length; i++) {
            newUpdates[i] = new BuildControllerUpdateOptions2010(updates[i]);
        }
        return newUpdates;
    }

    public BuildQueryResult convert(final IBuildServer buildServer, final BuildQueryResult2010 result) {
        return new BuildQueryResult(
            buildServer,
            convert(result.getAgents()),
            convert(buildServer, result.getControllers()),
            convert(buildServer, result.getDefinitions()),
            convert(buildServer, result.getBuilds()),
            convert(buildServer, result.getServiceHosts()));
    }

    public BuildQueryResult[] convert(final IBuildServer buildServer, final BuildQueryResult2010[] results) {
        final BuildQueryResult[] newResults = new BuildQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convert(buildServer, results[i]);
        }
        return newResults;
    }

    public BuildDefinitionQueryResult convert(
        final IBuildServer buildServer,
        final BuildDefinitionQueryResult2010 result) {
        return new BuildDefinitionQueryResult(
            buildServer,
            convert(result.getAgents()),
            convert(buildServer, result.getControllers()),
            convert(buildServer, result.getDefinitions()),
            convert(buildServer, result.getServiceHosts()));
    }

    public BuildDefinitionQueryResult[] convert(
        final IBuildServer buildServer,
        final BuildDefinitionQueryResult2010[] results) {
        final BuildDefinitionQueryResult[] newResults = new BuildDefinitionQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convert(buildServer, results[i]);
        }
        return newResults;
    }

    public BuildAgentQueryResult convert(final IBuildServer buildServer, final BuildAgentQueryResult2010 result) {
        return new BuildAgentQueryResult(
            buildServer,
            convert(result.getAgents()),
            convert(buildServer, result.getControllers()),
            convert(buildServer, result.getServiceHosts()));
    }

    public BuildAgentQueryResult[] convert(final IBuildServer buildServer, final BuildAgentQueryResult2010[] results) {
        final BuildAgentQueryResult[] newResults = new BuildAgentQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convert(buildServer, results[i]);
        }
        return newResults;
    }

    public static BuildControllerQueryResult convert(
        final IBuildServer buildServer,
        final BuildControllerQueryResult2010 result) {
        return new BuildControllerQueryResult(
            buildServer,
            convert(result.getAgents()),
            convert(buildServer, result.getControllers()),
            convert(buildServer, result.getServiceHosts()));
    }

    public BuildControllerQueryResult[] convert(
        final IBuildServer buildServer,
        final BuildControllerQueryResult2010[] results) {
        final BuildControllerQueryResult[] newResults = new BuildControllerQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convert(buildServer, results[i]);
        }
        return newResults;
    }

    public static BuildInformationNode convert(final BuildInformationNode2010 node2010) {
        return (node2010 == null) ? null : new BuildInformationNode(node2010);
    }

    public static WorkspaceMapping[] convert(final WorkspaceMapping2010[] mappings) {
        final WorkspaceMapping[] result = new WorkspaceMapping[mappings.length];

        for (int i = 0; i < mappings.length; i++) {
            result[i] = new WorkspaceMapping(mappings[i]);
        }

        return result;
    }

    public static BuildRequest2010[] convert(final IBuildRequest[] requests) {
        final BuildRequest2010[] newRequests = new BuildRequest2010[requests.length];
        for (int i = 0; i < requests.length; i++) {
            newRequests[i] = convert((BuildRequest) requests[i]);
        }
        return newRequests;
    }

    public static BuildInformationNode[] convert(final BuildInformationNode2010[] nodes) {
        final BuildInformationNode[] result = new BuildInformationNode[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            result[i] = convert(nodes[i]);
        }

        return result;
    }

    public BuildServiceHostQueryResult convert(
        final IBuildServer buildServer,
        final BuildServiceHostQueryResult2010 result) {
        return new BuildServiceHostQueryResult(
            buildServer,
            convert(result.getAgents()),
            convert(buildServer, result.getControllers()),
            convert(buildServer, result.getServiceHosts()));
    }

    public BuildServiceHostQueryResult[] convert(
        final IBuildServer buildServer,
        final BuildServiceHostQueryResult2010[] results) {
        final BuildServiceHostQueryResult[] newResults = new BuildServiceHostQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convert(buildServer, results[i]);
        }
        return newResults;
    }

    public static BuildAgentSpec2010[] convert(final IBuildAgentSpec[] specs) {
        final BuildAgentSpec2010[] newSpecs = new BuildAgentSpec2010[specs.length];
        for (int i = 0; i < specs.length; i++) {
            newSpecs[i] = new BuildAgentSpec2010((BuildAgentSpec) specs[i]);
        }
        return newSpecs;
    }

    public static BuildAgentUpdateOptions2010[] convert(final BuildAgentUpdateOptions[] updates) {
        final BuildAgentUpdateOptions2010[] newUpdates = new BuildAgentUpdateOptions2010[updates.length];
        for (int i = 0; i < updates.length; i++) {
            newUpdates[i] = new BuildAgentUpdateOptions2010(updates[i]);
        }
        return newUpdates;
    }

    public static BuildServiceHostUpdateOptions2010 convert(final BuildServiceHostUpdateOptions update) {
        if (update == null) {
            return null;
        }
        return new BuildServiceHostUpdateOptions2010(update);
    }

    public static BuildTeamProjectPermission2010[] convert(final BuildTeamProjectPermission[] permissions) {
        final BuildTeamProjectPermission2010[] newPermissions = new BuildTeamProjectPermission2010[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            newPermissions[i] = new BuildTeamProjectPermission2010(permissions[i]);
        }
        return newPermissions;
    }

    public static BuildAgent2010[] convert(final BuildAgent[] agents) {
        final BuildAgent2010[] newAgents = new BuildAgent2010[agents.length];
        for (int i = 0; i < agents.length; i++) {
            newAgents[i] = new BuildAgent2010(agents[i]);
        }
        return newAgents;
    }

    public static BuildUpdateOptions2010 convert(final BuildUpdateOptions update) {
        return new BuildUpdateOptions2010(update);
    }

    public static InformationField[] convert(final InformationField2010[] fields) {
        final InformationField[] newFields = new InformationField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            newFields[i] = new InformationField(fields[i].getName(), fields[i].getValue());
        }
        return newFields;
    }

    public static InformationField2010[] convert(final InformationField[] fields) {
        final InformationField2010[] newFields = new InformationField2010[fields.length];
        for (int i = 0; i < fields.length; i++) {
            newFields[i] = new InformationField2010(fields[i].getName(), fields[i].getValue());
        }
        return newFields;
    }

    public static InformationChangeRequest2010[] convert(final InformationChangeRequest[] requests) {
        final List<InformationChangeRequest2010> list = new ArrayList<InformationChangeRequest2010>();

        for (final InformationChangeRequest request : requests) {
            if (request instanceof InformationAddRequest) {
                final InformationAddRequest addRequest = (InformationAddRequest) request;
                final InformationAddRequest2010 newRequest = new InformationAddRequest2010();

                newRequest.setBuildURI(addRequest.getBuildURI());
                newRequest.setFields(convert(addRequest.getFields()));
                newRequest.setNodeID(addRequest.getNodeID());
                newRequest.setNodeType(addRequest.getNodeType());
                newRequest.setParentID(addRequest.getParentID());

                list.add(newRequest);
            } else if (request instanceof InformationEditRequest) {
                final InformationEditRequest editRequest = (InformationEditRequest) request;
                final InformationEditRequest2010 newRequest = new InformationEditRequest2010();

                newRequest.setBuildURI(editRequest.getBuildURI());
                newRequest.setFields(convert(editRequest.getFields()));
                newRequest.setNodeID(editRequest.getNodeID());
                newRequest.setOptions(convert(editRequest.getOptions()));

                list.add(newRequest);
            } else if (request instanceof InformationDeleteRequest) {
                final InformationDeleteRequest deleteRequest = (InformationDeleteRequest) request;
                final InformationDeleteRequest2010 newRequest = new InformationDeleteRequest2010();

                newRequest.setBuildURI(deleteRequest.getBuildURI());
                newRequest.setNodeID(deleteRequest.getNodeID());

                list.add(newRequest);
            }
        }

        return list.toArray(new InformationChangeRequest2010[list.size()]);
    }

    public static BuildUpdateOptions2010[] convert(final BuildUpdateOptions[] updates) {
        final BuildUpdateOptions2010[] newUpdates = new BuildUpdateOptions2010[updates.length];
        for (int i = 0; i < updates.length; i++) {
            newUpdates[i] = convert(updates[i]);
        }
        return newUpdates;
    }

    public static ProcessTemplate[] convert(final IBuildServer buildServer, final ProcessTemplate2010[] templates) {
        final ProcessTemplate[] newTemplates = new ProcessTemplate[templates.length];
        for (int i = 0; i < templates.length; i++) {
            newTemplates[i] = new ProcessTemplate(buildServer, templates[i]);
        }
        return newTemplates;
    }

    public static ProcessTemplate2010 convert(final ProcessTemplate template) {
        if (template == null) {
            return null;
        }
        return new ProcessTemplate2010(template);
    }

    public static ProcessTemplate convert(final IBuildServer buildServer, final ProcessTemplate2010 template) {
        if (template == null) {
            return null;
        }
        return new ProcessTemplate(buildServer, template);
    }

    public static ProcessTemplate2010[] convert(final ProcessTemplate[] templates) {
        final ProcessTemplate2010[] newTemplates = new ProcessTemplate2010[templates.length];
        for (int i = 0; i < templates.length; i++) {
            newTemplates[i] = convert(templates[i]);
        }
        return newTemplates;
    }

    public static QueuedBuild convert(final IBuildServer buildServer, final QueuedBuild2010 build) {
        if (build == null) {
            return null;
        }
        return new QueuedBuild(buildServer, build);
    }

    public static QueuedBuild[] convert(final IBuildServer buildServer, final QueuedBuild2010[] builds) {
        final QueuedBuild[] newBuilds = new QueuedBuild[builds.length];
        for (int i = 0; i < builds.length; i++) {
            newBuilds[i] = convert(buildServer, builds[i]);
        }
        return newBuilds;
    }

    public static WorkspaceMapping2010[] convert(final WorkspaceMapping[] mappings) {
        final WorkspaceMapping2010[] newMappings = new WorkspaceMapping2010[mappings.length];
        for (int i = 0; i < mappings.length; i++) {
            newMappings[i] = new WorkspaceMapping2010(mappings[i]);
        }
        return newMappings;
    }

    public static WorkspaceTemplate2010 convert(final WorkspaceTemplate template) {
        if (template == null) {
            return null;
        }
        return new WorkspaceTemplate2010(template);
    }

    public static WorkspaceTemplate convert(final WorkspaceTemplate2010 template) {
        if (template == null) {
            return null;
        }
        return new WorkspaceTemplate(template);
    }

    public static IFailure2010 convert(final IFailure failure) {
        final Failure2010 newFailure = new Failure2010();
        newFailure.setCode(failure.getCode());
        newFailure.setMessage(failure.getMessage());
        return newFailure;
    }

    public static BuildQueueQueryResult convert(final BuildServer buildServer, final BuildQueueQueryResult2010 result) {
        final List<BuildDetail2010> buildDetails = new ArrayList<BuildDetail2010>();
        final QueuedBuild2010[] queuedBuilds = result.getBuilds();
        for (final QueuedBuild2010 queuedBuild : queuedBuilds) {
            if (queuedBuild.getBuild() != null) {
                buildDetails.add(queuedBuild.getBuild());
            }
        }

        return new BuildQueueQueryResult(
            buildServer,
            convert(result.getAgents()),
            convert(buildServer, result.getControllers()),
            convert(buildServer, result.getDefinitions()),
            convert(buildServer, result.getBuilds()),
            convert(buildServer, result.getServiceHosts()),
            convert(buildServer, buildDetails.toArray(new BuildDetail2010[buildDetails.size()])));
    }

    public BuildQueueQueryResult[] convert(final BuildServer buildServer, final BuildQueueQueryResult2010[] results) {
        final BuildQueueQueryResult[] newResults = new BuildQueueQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convert(buildServer, results[i]);
        }
        return newResults;
    }

    public static BuildRequest2010 convert(final BuildRequest request) {
        if (request == null) {
            return null;
        }

        request.beforeSerialize();
        return new BuildRequest2010(request);
    }

    public static QueuedBuildUpdateOptions2010[] convert(final QueuedBuildUpdateOptions[] updates) {
        final QueuedBuildUpdateOptions2010[] newUpdates = new QueuedBuildUpdateOptions2010[updates.length];
        for (int i = 0; i < updates.length; i++) {
            newUpdates[i] = new QueuedBuildUpdateOptions2010(updates[i]);
        }
        return newUpdates;
    }

    public static BuildServiceHost2010 convert(final BuildServiceHost serviceHost) {
        if (serviceHost == null) {
            return null;
        }
        return new BuildServiceHost2010(serviceHost);
    }

    public static BuildAgent convert(final IBuildServiceHost serviceHost, final BuildAgent2010 agent) {
        return agent == null ? null : new BuildAgent(serviceHost, agent);
    }

    public static BuildController convert(final IBuildServer server, final BuildController2010 controller) {
        return controller == null ? null : new BuildController(server, controller);
    }

    public static BuildDefinition convert(final IBuildServer buildServer, final BuildDefinition2010 definition) {
        return definition == null ? null : new BuildDefinition(buildServer, definition);
    }

    public static BuildServiceHost convert(final IBuildServer server, final BuildServiceHost2010 serviceHost) {
        return serviceHost == null ? null : new BuildServiceHost(server, serviceHost);
    }

    public static _BuildAgent[] unwrap(final BuildAgent[] values) {
        return (_BuildAgent[]) WrapperUtils.unwrap(_BuildAgent.class, values);
    }

    public static _BuildController[] unwrap(final BuildController[] values) {
        return (_BuildController[]) WrapperUtils.unwrap(_BuildController.class, values);
    }

    public static _BuildDefinition[] unwrap(final BuildDefinition[] values) {
        return (_BuildDefinition[]) WrapperUtils.unwrap(_BuildDefinition.class, values);
    }

    public static _QueuedBuild[] unwrap(final QueuedBuild[] values) {
        return (_QueuedBuild[]) WrapperUtils.unwrap(_QueuedBuild.class, values);
    }

    public static _BuildServiceHost[] unwrap(final BuildServiceHost[] values) {
        return (_BuildServiceHost[]) WrapperUtils.unwrap(_BuildServiceHost.class, values);
    }

    public static BuildAgent[] convert(final BuildAgent2010[] values) {
        final List<BuildAgent> list = new ArrayList<BuildAgent>();
        for (final BuildAgent2010 value : values) {
            list.add(convert(null, value));
        }
        return list.toArray(new BuildAgent[list.size()]);
    }

    public static BuildController[] convert(final IBuildServer server, final BuildController2010[] values) {
        final List<BuildController> list = new ArrayList<BuildController>();
        for (final BuildController2010 value : values) {
            list.add(convert(server, value));
        }
        return list.toArray(new BuildController[list.size()]);
    }

    public static BuildController2010[] convert(final BuildController[] controllers) {
        final BuildController2010[] newControllers = new BuildController2010[controllers.length];
        for (int i = 0; i < controllers.length; i++) {
            newControllers[i] = new BuildController2010(controllers[i]);
        }
        return newControllers;
    }

    public static BuildDefinition[] convert(final IBuildServer buildServer, final BuildDefinition2010[] values) {
        final List<BuildDefinition> list = new ArrayList<BuildDefinition>();
        for (final BuildDefinition2010 value : values) {
            list.add(convert(buildServer, value));
        }
        return list.toArray(new BuildDefinition[list.size()]);
    }

    public static BuildDefinition2010[] convert(final BuildDefinition[] definitions) {
        final BuildDefinition2010[] newDefinitions = new BuildDefinition2010[definitions.length];
        for (int i = 0; i < definitions.length; i++) {
            definitions[i].prepareToSave();
            newDefinitions[i] = new BuildDefinition2010(BuildServerVersion.V3, definitions[i]);
        }
        return newDefinitions;
    }

    public static BuildServiceHost[] convert(final IBuildServer server, final BuildServiceHost2010[] values) {
        final List<BuildServiceHost> list = new ArrayList<BuildServiceHost>();
        for (final BuildServiceHost2010 value : values) {
            list.add(convert(server, value));
        }
        return list.toArray(new BuildServiceHost[list.size()]);
    }

    public static Schedule2010[] convert(final ISchedule[] schedules) {
        final Schedule2010[] newSchedules = new Schedule2010[schedules.length];
        for (int i = 0; i < schedules.length; i++) {
            newSchedules[i] = new Schedule2010((Schedule) schedules[i]);
        }
        return newSchedules;
    }

    public static Schedule[] convert(final BuildDefinition definition, final Schedule2010[] schedules) {
        final Schedule[] newSchedules = new Schedule[schedules.length];
        for (int i = 0; i < schedules.length; i++) {
            newSchedules[i] = new Schedule(definition, schedules[i]);
        }
        return newSchedules;
    }

    public static RetentionPolicy2010[] convert(final IRetentionPolicy[] policies) {
        final RetentionPolicy2010[] newPolicies = new RetentionPolicy2010[policies.length];
        for (int i = 0; i < policies.length; i++) {
            newPolicies[i] = new RetentionPolicy2010((RetentionPolicy) policies[i]);
        }
        return newPolicies;
    }

    public static RetentionPolicy[] convert(final BuildDefinition definition, final RetentionPolicy2010[] policies) {
        final RetentionPolicy[] newPolicies = new RetentionPolicy[policies.length];
        for (int i = 0; i < policies.length; i++) {
            newPolicies[i] = new RetentionPolicy(
                definition,
                convert(policies[i].getBuildReason()),
                convert(policies[i].getBuildStatus()),
                policies[i].getNumberToKeep(),
                convert(policies[i].getDeleteOptions()));
        }
        return newPolicies;
    }

}

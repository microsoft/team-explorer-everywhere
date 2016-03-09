// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionAll;
import com.microsoft.tfs.client.clc.vc.options.OptionAssociate;
import com.microsoft.tfs.client.clc.vc.options.OptionAuthor;
import com.microsoft.tfs.client.clc.vc.options.OptionBypass;
import com.microsoft.tfs.client.clc.vc.options.OptionComment;
import com.microsoft.tfs.client.clc.vc.options.OptionForce;
import com.microsoft.tfs.client.clc.vc.options.OptionNoAutoResolve;
import com.microsoft.tfs.client.clc.vc.options.OptionNotes;
import com.microsoft.tfs.client.clc.vc.options.OptionOverride;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionResolve;
import com.microsoft.tfs.client.clc.vc.options.OptionSaved;
import com.microsoft.tfs.client.clc.vc.options.OptionValidate;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyContextKeys;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyLoadErrorEvent;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyLoadErrorListener;
import com.microsoft.tfs.core.checkinpolicies.loaders.ClasspathPolicyLoader;
import com.microsoft.tfs.core.clients.build.buildstatus.BuildStatusCache;
import com.microsoft.tfs.core.clients.versioncontrol.CheckinFlags;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ActionDeniedBySubscriberException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.CheckinException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.GatedCheckinException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.SavedCheckin;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkItemCheckedInfo;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.exceptions.DeniedOrNotExistException;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameter;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameterCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.pendingcheckin.CheckinConflict;
import com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationOptions;
import com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationResult;
import com.microsoft.tfs.core.pendingcheckin.CheckinNoteFailure;
import com.microsoft.tfs.util.Check;

/**
 * Checks in pending changes.
 * <p>
 * Unlike for the VS CLC, the "new" option is not available here because all
 * that option does is open the checkin dialog with no initial saved checkin
 * data, and this program never opens dialogs.
 */
public final class CommandCheckin extends Command {
    /**
     * Keeps track of whether a single run of this command has had an error, so
     * output can be formatted better (spaces between errors).
     */
    private boolean hadError;

    public CommandCheckin() {
        super();
    }

    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        // Reset error.
        hadError = false;

        // Connect and determine workspace

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        client.getEventEngine().addConflictResolvedListener(this);

        final String[] args = getFreeArguments();

        reportBadOptionCombinationIfPresent(OptionValidate.class, OptionAll.class);

        /*
         * The VS command line client sets QUEUE_BUILD_FOR_GATED_CHECK_IN
         * whenever "noprompt" is active, which is always the behavior for this
         * client.
         */
        CheckinFlags checkinFlags = CheckinFlags.QUEUE_BUILD_FOR_GATED_CHECK_IN;

        if (findOptionType(OptionBypass.class) != null) {
            checkinFlags = checkinFlags.combine(CheckinFlags.OVERRIDE_GATED_CHECK_IN);
        }

        if (findOptionType(OptionForce.class) != null) {
            checkinFlags = checkinFlags.combine(CheckinFlags.ALLOW_UNCHANGED_CONTENT);
        }

        if (findOptionType(OptionNoAutoResolve.class) != null) {
            checkinFlags = checkinFlags.combine(CheckinFlags.NO_AUTO_RESOLVE);
        }

        Option o = null;

        String author = null;
        if ((o = findOptionType(OptionAuthor.class)) != null) {
            author = ((OptionAuthor) o).getValue();
        }

        final boolean recursive = findOptionType(OptionRecursive.class) != null;
        final boolean validateOnly = findOptionType(OptionValidate.class) != null;
        final boolean checkinAll = findOptionType(OptionAll.class) != null;
        final boolean useSavedCheckinInfo = findOptionType(OptionSaved.class) != null;

        final Workspace checkinWorkspace = realizeCachedWorkspace(determineCachedWorkspace(), client);

        SavedCheckin checkinInfo = null;
        PendingChange[] checkinChanges = null;

        /*
         * For the combination of options and arguments present, read the saved
         * comment, work items, notes, and policy override comment into the
         * checkinInfo, and the changes into checkinChanges (or leave null to
         * have the server check in all).
         */

        if (!checkinAll && args.length == 0) {
            // Check in all changes we know of in the workspace

            checkinInfo = getCheckinInformation(checkinWorkspace, useSavedCheckinInfo);

            // Get all the pending changes for the workspace.
            final PendingSet set = checkinWorkspace.getPendingChanges();
            final PendingChange[] allChanges = (set != null) ? set.getPendingChanges() : null;

            if (set == null || allChanges == null || allChanges.length == 0) {
                getDisplay().printLine(Messages.getString("CommandCheckin.NoChangesToCheckinInThisWorkspace")); //$NON-NLS-1$
                setExitCode(ExitCode.PARTIAL_SUCCESS);
                return;
            }

            checkinChanges = getIncludedPendingChanges(checkinInfo, allChanges);
        } else if (!checkinAll) {
            // Choose changes from command arguments

            checkinInfo = getCheckinInformation(checkinWorkspace, useSavedCheckinInfo);

            /*
             * Check in only the items mentioned in the arguments.
             * determineWorkspace will find the correct one, since it can read
             * the free arguments.
             */

            try {
                checkinChanges = getPendingChangesMatchingFreeArguments(checkinWorkspace, recursive);
            } catch (final ServerPathFormatException e) {
                throw new InvalidFreeArgumentException(e.getMessage());
            }

            if (checkinChanges == null || checkinChanges.length == 0) {
                getDisplay().printLine(Messages.getString("CommandCheckin.NoChangesMatchedByArguments")); //$NON-NLS-1$
                setExitCode(ExitCode.FAILURE);
                return;
            }
        } else {
            /*
             * The "all" option was specified. Send "null" pending changes the
             * server to make it check in all items it knows of in the
             * workspace. The server will handle all error notification in this
             * case.
             */

            checkinInfo = getCheckinInformation(checkinWorkspace, useSavedCheckinInfo);

            // Print warnings if user specified /all and some itemspecs
            final QualifiedItem[] items = parseQualifiedItems(LatestVersionSpec.INSTANCE, false, 0);

            for (final QualifiedItem item : items) {
                getDisplay().printErrorLine(
                    MessageFormat.format(Messages.getString("CommandCheckin.IgnoringItemFormat"), item.getPath())); //$NON-NLS-1$
            }

            checkinChanges = null;
        }

        Check.notNull(checkinInfo, "checkinInfo"); //$NON-NLS-1$
        // checkinChanges may be null here

        /*
         * Make sure the associated/resolved work items exist and the state
         * transition is valid.
         */
        final WorkItemCheckinInfo[] workItemCheckinInfos = checkWorkItems(checkinInfo, connection);

        /*
         * Evaluate checkin notes, policies, etc.
         *
         * Can't do validation with "checkinAll" because only the server knows
         * the full set of changes.
         */
        PolicyOverrideInfo overrideInfo = null;
        if (!checkinAll) {
            /*
             * Default to checking notes and policies, because the actual
             * checkin will return conflicts for us.
             */
            CheckinEvaluationOptions evaluationOptions =
                CheckinEvaluationOptions.NOTES.combine(CheckinEvaluationOptions.POLICIES);

            /*
             * In the validate-only case, we also check for conflicts (because
             * we won't get them from an actual checkin failure).
             */
            if (validateOnly) {
                evaluationOptions = evaluationOptions.combine(CheckinEvaluationOptions.CONFLICTS);
            }

            final CheckinEvaluationResult result = evaluateCheckin(
                evaluationOptions,
                checkinWorkspace,
                checkinChanges,
                checkinInfo.getCheckinNotes(),
                workItemCheckinInfos,
                checkinInfo.getComment(),
                checkinInfo.getPolicyOverrideComment());

            if (result.getPolicyFailures() != null) {
                overrideInfo = doPolicyOverride(result.getPolicyFailures(), checkinInfo.getPolicyOverrideComment());

                if (overrideInfo != null) {
                    // User supplied an override comment, no failure.
                    getDisplay().printLine(""); //$NON-NLS-1$
                    getDisplay().printLine(Messages.getString("CommandCheckin.PoliciesHaveBeenOverridden")); //$NON-NLS-1$
                    getDisplay().printLine(""); //$NON-NLS-1$
                }
            }

            /*
             * If we're only validating, or we have had errors, return now.
             */
            if (hadError) {
                getDisplay().printLine(""); //$NON-NLS-1$
                if (validateOnly) {
                    getDisplay().printLine(Messages.getString("CommandCheckin.ValidationFailed")); //$NON-NLS-1$
                } else {
                    getDisplay().printLine(Messages.getString("CommandCheckin.NoFilesCheckedIn")); //$NON-NLS-1$
                }

                setExitCode(ExitCode.FAILURE);
                return;
            } else if (validateOnly) {
                getDisplay().printLine(Messages.getString("CommandCheckin.ValidationWasSuccessful")); //$NON-NLS-1$
                return;
            }
        }

        /*
         * Perform the checkin.
         */

        try {
            // When checkinAll, checkinChanges will be null here
            final int csid = checkinWorkspace.checkIn(
                checkinChanges,
                author,
                author,
                checkinInfo.getComment(),
                checkinInfo.getCheckinNotes(),
                workItemCheckinInfos,
                overrideInfo,
                checkinFlags);

            if (csid == 0) {
                setExitCode(ExitCode.PARTIAL_SUCCESS);
            } else {
                setExitCode(ExitCode.SUCCESS);
            }

            /*
             * An event handler prints the changeset number to the standard
             * output. We still need to print work item resolution status.
             */

            if (checkinInfo.getAssociateOrResolveWorkItemsCheckedInfo().length > 0) {
                if (csid == 0) {
                    getDisplay().printLine(Messages.getString("CommandCheckin.NoWorkItemsResolvedOrAssociated")); //$NON-NLS-1$
                    getDisplay().printLine(""); //$NON-NLS-1$
                } else {
                    for (int i = 0; i < workItemCheckinInfos.length; i++) {
                        final WorkItemCheckinInfo info = workItemCheckinInfos[i];

                        if (info.getAction() == CheckinWorkItemAction.ASSOCIATE) {
                            getDisplay().printLine(
                                MessageFormat.format(
                                    Messages.getString("CommandCheckin.AssociatedWorkitemFormat"), //$NON-NLS-1$
                                    Integer.toString(info.getWorkItem().getFields().getID())));
                        } else if (info.getAction() == CheckinWorkItemAction.RESOLVE) {
                            getDisplay().printLine(
                                MessageFormat.format(
                                    Messages.getString("CommandCheckin.ResolvedWorkItemFormat"), //$NON-NLS-1$
                                    Integer.toString(info.getWorkItem().getFields().getID())));
                        }
                    }
                }
            }
        } catch (final GatedCheckinException gatedException) {
            /*
             * We always set CheckinFlags.QUEUE_BUILD_FOR_GATED_CHECK_IN so the
             * server queues builds for us. Print the message and examine the
             * exception's properties to get the build we need to watch.
             */
            getDisplay().printLine(""); //$NON-NLS-1$
            getDisplay().printLine(gatedException.getMessage());

            /*
             * The VS client checks for these conditions before saving the build
             * ID for future notification/reconcile.
             */
            if (gatedException.getSubCode() == 3
                && gatedException.getAffectedBuildDefinitions().size() > 0
                && gatedException.getShelvesetName() != null
                && gatedException.getShelvesetName().length() > 0
                && gatedException.getQueueID() != 0) {
                final BuildStatusCache cache = BuildStatusCache.load(connection);
                cache.addBuild(gatedException.getQueueID());
                cache.save(connection);
            }

            setExitCode(ExitCode.PARTIAL_SUCCESS);
        } catch (final CheckinException e) {
            if (e.isAnyResolvable()) {
                final String messageFormat = Messages.getString("CommandCheckin.ResolvableConflictFlaggedFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());

                reportExceptionError(message);
            } else {
                reportExceptionError(e.getLocalizedMessage());
            }
        } catch (final ActionDeniedBySubscriberException e) {
            reportExceptionError(e.getLocalizedMessage());
        }
    }

    private void reportExceptionError(final String exceptionMessage) {
        getDisplay().printErrorLine(exceptionMessage);

        getDisplay().printLine(""); //$NON-NLS-1$
        getDisplay().printLine(Messages.getString("CommandCheckin.NoFilesCheckedIn")); //$NON-NLS-1$

        setExitCode(ExitCode.FAILURE);
    }

    private WorkItemCheckinInfo[] checkWorkItems(final SavedCheckin checkin, final TFSTeamProjectCollection connection)
        throws InvalidOptionValueException {
        Check.notNull(checkin, "checkin"); //$NON-NLS-1$
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        // Skip the "NONE" action infos
        final WorkItemCheckedInfo[] checkedInfos = checkin.getAssociateOrResolveWorkItemsCheckedInfo();

        if (checkedInfos.length == 0) {
            return new WorkItemCheckinInfo[0];
        }

        /*
         * Make sure an ID is not marked for both associate and resolve.
         */
        for (final WorkItemCheckedInfo info : checkedInfos) {
            for (final WorkItemCheckedInfo info2 : checkedInfos) {
                if (info == info2) {
                    continue;
                }

                if (info.getID() == info2.getID() && info.getCheckinAction() != info2.getCheckinAction()) {
                    throw new InvalidOptionValueException(
                        MessageFormat.format(
                            Messages.getString("CommandCheckin.WorkItemCannotBeBothResolvedAndAssociatedFormat"), //$NON-NLS-1$
                            Integer.toString(info.getID())));
                }
            }
        }

        /*
         * Perform a batch read of all the work items to verify they exist.
         */
        final BatchReadParameterCollection batchReadParams = new BatchReadParameterCollection();

        for (final WorkItemCheckedInfo info : checkedInfos) {
            batchReadParams.add(new BatchReadParameter(info.getID()));
        }

        final WorkItemClient workItemClient = connection.getWorkItemClient();
        final WorkItemCollection items = workItemClient.query(
            "select [System.Id], [System.State] from workitems", //$NON-NLS-1$
            batchReadParams);

        final Map<Integer, WorkItem> idToWorkItemMap = new HashMap<Integer, WorkItem>();

        /*
         * We have to access them through the collection to force the paging
         * mechanism to retrieve them. Invalid items cause an exception.
         */
        for (int i = 0; i < items.size(); i++) {
            WorkItem wi;
            try {
                // Must open the work item to ensure it exists and is readable
                wi = items.getWorkItem(i);
                wi.open();
            } catch (final DeniedOrNotExistException e) {
                throw new InvalidOptionValueException(e.getLocalizedMessage());
            }

            idToWorkItemMap.put(new Integer(wi.getFields().getID()), wi);
        }

        // Convert from "checked" (persisted in saved checkins) to "checkin"
        // (sent during checkin)

        final WorkItemCheckinInfo[] checkinInfos = new WorkItemCheckinInfo[checkedInfos.length];

        for (int i = 0; i < checkedInfos.length; i++) {
            final WorkItemCheckedInfo checkedInfo = checkedInfos[i];
            final WorkItem wi = idToWorkItemMap.get(new Integer(checkedInfo.getID()));
            final WorkItemCheckinInfo checkinInfo = new WorkItemCheckinInfo(wi);

            checkinInfo.setAction(checkedInfo.getCheckinAction());

            if (checkinInfo.getAction() == CheckinWorkItemAction.RESOLVE && !checkinInfo.isResolveSupported()) {
                throw new InvalidOptionValueException(
                    MessageFormat.format(
                        Messages.getString("CommandCheckin.WorkItemDoesNotSupportResolveFormat"), //$NON-NLS-1$
                        Integer.toString(wi.getFields().getID()),
                        wi.getType().getName(),
                        wi.getFields().getField(CoreFieldReferenceNames.STATE).getValue()));
            }

            checkinInfos[i] = checkinInfo;
        }

        return checkinInfos;
    }

    /**
     * Prints the policy override message, if the user has supplied one.
     * Otherwise, flips on the {@link #hadError} field and returns null.
     *
     * @param failures
     *        the policy failures (not null).
     * @param policyFailureOverrideReason
     *        the reason the user supplied (may be null).
     * @return the override info the user supplied, or null if the user did not
     *         supply any (checkin should fail).
     */
    private PolicyOverrideInfo doPolicyOverride(
        final PolicyFailure[] failures,
        final String policyFailureOverrideReason) {
        Check.notNull(failures, "failures"); //$NON-NLS-1$

        if (failures.length > 0) {
            getDisplay().printLine(""); //$NON-NLS-1$
            getDisplay().printErrorLine(Messages.getString("CommandCheckin.TheseCheckInPoliciesHaveNotBeenSatisfied")); //$NON-NLS-1$

            for (int i = 0; i < failures.length; i++) {
                getDisplay().printErrorLine(" " + failures[i].getMessage()); //$NON-NLS-1$
            }

            /*
             * If user offered a comment, don't fail.
             */
            if (policyFailureOverrideReason != null && policyFailureOverrideReason.length() > 0) {
                return new PolicyOverrideInfo(policyFailureOverrideReason, failures);
            }

            // No override, so failure.
            hadError = true;
        }

        return null;
    }

    /**
     * Checks the checkin data for problems before checkin.
     */
    private CheckinEvaluationResult evaluateCheckin(
        final CheckinEvaluationOptions evaluationOptions,
        final Workspace workspace,
        final PendingChange[] checkinChanges,
        CheckinNote notes,
        WorkItemCheckinInfo[] workItemCheckinInfo,
        String comment,
        final String policyFailureOverrideReason) throws CLCException {
        Check.notNull(evaluationOptions, "evaluationOptions"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        if (checkinChanges == null || checkinChanges.length == 0) {
            return new CheckinEvaluationResult(
                new CheckinConflict[0],
                new CheckinNoteFailure[0],
                new PolicyFailure[0],
                null,
                null);
        }

        if (notes == null) {
            notes = new CheckinNote(new CheckinNoteFieldValue[0]);
        }

        if (workItemCheckinInfo == null) {
            workItemCheckinInfo = new WorkItemCheckinInfo[0];
        }

        if (comment == null) {
            comment = ""; //$NON-NLS-1$
        }

        final PendingSet allChangesSet = workspace.getPendingChanges();

        if (allChangesSet == null) {
            throw new CLCException(Messages.getString("CommandCheckin.PendingChangesDisappearedBeforeCheckin")); //$NON-NLS-1$
        }

        final PendingChange[] allChanges = allChangesSet.getPendingChanges();

        if (allChanges == null || allChanges.length == 0) {
            throw new CLCException(Messages.getString("CommandCheckin.PendingChangesDisappearedBeforeCheckin")); //$NON-NLS-1$
        }

        final PolicyEvaluator evaluator = new PolicyEvaluator(workspace.getClient(), new ClasspathPolicyLoader());

        evaluator.addPolicyLoadErrorListener(new PolicyLoadErrorListener() {
            @Override
            public void onPolicyLoadError(final PolicyLoadErrorEvent event) {
                getDisplay().printErrorLine(PolicyEvaluator.makeTextErrorForLoadException(event.getError()));
            }
        });

        final PolicyContext context = new PolicyContext();
        context.addProperty(PolicyContextKeys.RUNNING_PRODUCT_CLC, new Object());
        context.addProperty(PolicyContextKeys.TFS_TEAM_PROJECT_COLLECTION, workspace.getClient().getConnection());

        final CheckinEvaluationResult result = workspace.evaluateCheckIn(
            evaluationOptions,
            allChanges,
            checkinChanges,
            comment,
            notes,
            workItemCheckinInfo,
            evaluator,
            context);

        /*
         * If the policy evaluator failed to load some policies, stop.
         */
        if (evaluator.getPolicyEvaluatorState() == PolicyEvaluatorState.POLICIES_LOAD_ERROR) {
            if (policyFailureOverrideReason == null || policyFailureOverrideReason.length() == 0) {
                throw new CLCException(
                    Messages.getString("CommandCheckin.CheckinNotCompletedBecausePoliciesCouldNotBeLoaded")); //$NON-NLS-1$
            }

            getDisplay().printErrorLine(
                Messages.getString("CommandCheckin.SomePoliciesCouldNotBeLoadedButOverrideEnabled")); //$NON-NLS-1$

            getDisplay().printErrorLine(""); //$NON-NLS-1$
        }

        /*
         * Policy failures are not printed here. They're printed by
         * doPolicyOverride.
         */

        /*
         * Print all the conflicts.
         */
        if (result.getConflicts().length > 0) {
            for (int i = 0; i < result.getConflicts().length; i++) {
                final CheckinConflict c = result.getConflicts()[i];

                if (c.isResolvable()) {
                    final String messageFormat = Messages.getString("CommandCheckin.ConflictFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, c.getMessage());

                    getDisplay().printErrorLine(message);
                } else {
                    getDisplay().printErrorLine(c.getMessage());
                }
            }
            hadError = true;
        }

        /*
         * Print checkin note failures.
         */
        if (result.getNoteFailures().length > 0) {
            if (displayCheckinNoteFailures(result.getNoteFailures())) {
                hadError = true;
            }
        }

        /*
         * Print checkin policy exception.
         */
        if (result.getPolicyEvaluationException() != null) {
            getDisplay().printLine(""); //$NON-NLS-1$
            getDisplay().printErrorLine(result.getPolicyEvaluationException().getMessage());
            hadError = true;
        }

        return result;
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        /*
         * OptionNew isn't listed here because we don't open dialogs and that's
         * it's primary purpose.
         */

        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionAll.class,
            OptionAuthor.class,
            OptionComment.class,
            OptionNotes.class,
            OptionOverride.class,
            OptionRecursive.class,
            OptionValidate.class,
            OptionBypass.class,
            OptionForce.class,
            OptionNoAutoResolve.class,
            OptionAssociate.class,
            OptionResolve.class,
            OptionSaved.class,
        }, "[<itemSpec>...]"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandCheckin.HelpText1") //$NON-NLS-1$
        };
    }

    /**
     * Builds a {@link SavedCheckin} from (optionally) the last saved info plus
     * the info specified in command line options.
     *
     * @param workspace
     *        the workspace to load the saved checkin from (must not be
     *        <code>null</code>)
     * @param useSaved
     *        if <code>true</code> the last saved information is loaded and
     *        command line options override those values, if <code>false</code>
     *        only command line info is used
     * @return the {@link SavedCheckin} with option data merged into it
     * @throws InvalidOptionValueException
     *         if one of the option values was invalid
     */
    private SavedCheckin getCheckinInformation(final Workspace workspace, final boolean useSaved)
        throws InvalidOptionValueException {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        SavedCheckin checkin = useSaved ? workspace.getLastSavedCheckin() : null;

        // The saved one could have been null
        if (checkin == null) {
            checkin = new SavedCheckin();
        }

        Option o = null;

        if ((o = findOptionType(OptionComment.class)) != null) {
            checkin.setComment(((OptionComment) o).getValue());
        }

        if ((o = findOptionType(OptionNotes.class)) != null) {
            checkin.setCheckinNotes(((OptionNotes) o).getNotes());
        }

        if ((o = findOptionType(OptionOverride.class)) != null) {
            final String comment = ((OptionOverride) o).getValue();
            if (comment != null && comment.length() == 0) {
                throw new InvalidOptionValueException(Messages.getString("CommandCheckin.OverrideRequiresComment")); //$NON-NLS-1$
            }

            checkin.setPolicyOverrideComment(comment);
        }

        final List<WorkItemCheckedInfo> checkedInfos = new ArrayList<WorkItemCheckedInfo>();

        if ((o = findOptionType(OptionResolve.class)) != null) {
            for (final int i : ((OptionResolve) o).getIntegerValues()) {
                checkedInfos.add(new WorkItemCheckedInfo(i, true, CheckinWorkItemAction.RESOLVE));
            }
        }

        if ((o = findOptionType(OptionAssociate.class)) != null) {
            for (final int i : ((OptionAssociate) o).getIntegerValues()) {
                checkedInfos.add(new WorkItemCheckedInfo(i, true, CheckinWorkItemAction.ASSOCIATE));
            }
        }

        if (checkedInfos.size() > 0) {
            checkin.setPersistentWorkItemsCheckedInfo(
                checkedInfos.toArray(new WorkItemCheckedInfo[checkedInfos.size()]));
        }

        return checkin;
    }

    private static PendingChange[] getIncludedPendingChanges(
        final SavedCheckin savedCheckin,
        final PendingChange[] allChanges) {
        Check.notNull(savedCheckin, "savedCheckin"); //$NON-NLS-1$
        Check.notNull(allChanges, "allChanges"); //$NON-NLS-1$

        final List<PendingChange> checkinChangesList = new ArrayList<PendingChange>();

        for (final PendingChange pendingChange : allChanges) {
            if (!savedCheckin.isExcluded(pendingChange.getServerItem())) {
                checkinChangesList.add(pendingChange);
            }
        }

        return checkinChangesList.toArray(new PendingChange[checkinChangesList.size()]);
    }
}

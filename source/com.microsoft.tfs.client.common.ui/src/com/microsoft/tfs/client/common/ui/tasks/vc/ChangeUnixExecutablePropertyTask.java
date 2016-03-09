// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.ChangePropertiesCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryItemsExtendedCommand;
import com.microsoft.tfs.client.common.commands.vc.UndoCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.ItemProperties;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

/**
 * Pends a property change to enable or disable the Unix execute bit for a file
 * item (directories not supported). Enabling the bit always pends a property
 * change. Disabling the bit may pend a property change (with a "null" value for
 * the executable property), or it may undo all pending changes on the item if
 * the only pending change is currently an execute-enabling property change.
 * <p>
 * This task does an online call to get {@link ExtendedItem}s so it won't work
 * offline.
 */
public class ChangeUnixExecutablePropertyTask extends BaseTask {
    private final TFSRepository repository;
    private final String[] paths;
    private final boolean executable;
    private final LockLevel lockLevel;
    private final PendChangesOptions options;

    public ChangeUnixExecutablePropertyTask(
        final Shell shell,
        final TFSRepository repository,
        final String[] paths,
        final boolean executable,
        final LockLevel lockLevel,
        final PendChangesOptions options) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(paths, "paths"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        this.repository = repository;
        this.paths = paths;
        this.executable = executable;
        this.lockLevel = lockLevel;
        this.options = options;
    }

    @Override
    public IStatus run() {
        /*
         * Query the items, including pending change information, including ALL
         * properties. We must query for all properties, not just the execute
         * bit property, so we can correctly detect whether a property change
         * for the executable property is the only change.
         */

        final QueryItemsExtendedCommand queryCommand = new QueryItemsExtendedCommand(
            repository,
            ItemSpec.fromStrings(paths, RecursionType.NONE),
            DeletedState.NON_DELETED,
            ItemType.FILE,
            GetItemsOptions.NONE,
            PropertyConstants.QUERY_ALL_PROPERTIES_FILTERS);

        final IStatus queryStatus = getCommandExecutor().execute(queryCommand);

        if (!queryStatus.isOK()) {
            ErrorDialog.openError(
                getShell(),
                Messages.getString("ChangeUnixExecutablePropertyTask.ErrorQueryingItemInformationTitle"), //$NON-NLS-1$
                null,
                queryStatus);

            return queryStatus;
        }

        final ExtendedItem[][] sets = queryCommand.getItems();

        if (sets == null || sets.length == 0 || sets[0] == null || sets[0].length == 0) {
            final IStatus emptySetStatus = new Status(
                Status.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(
                    Messages.getString("ChangeUnixExecutablePropertyTask.NoMatchesFoundFormat"), //$NON-NLS-1$
                    paths.length),
                null);

            ErrorDialog.openError(
                getShell(),
                Messages.getString("ChangeUnixExecutablePropertyTask.ErrorQueryingItemInformationTitle"), //$NON-NLS-1$
                null,
                emptySetStatus);

            return emptySetStatus;
        }

        final List<ItemProperties> itemProperties = new ArrayList<ItemProperties>();
        final List<String> undoPaths = new ArrayList<String>();

        // Split the items into two lists
        prepareRequests(sets[0], itemProperties, undoPaths);

        // Pend properties
        if (itemProperties.size() > 0) {
            // Use ResourceChangingCommand because the execute bit may change on
            // disk
            final IStatus propertiesStatus = getCommandExecutor().execute(
                new ResourceChangingCommand(
                    new ChangePropertiesCommand(
                        repository,
                        itemProperties.toArray(new ItemProperties[itemProperties.size()]),
                        RecursionType.NONE,
                        lockLevel,
                        options,
                        null)));
            if (!propertiesStatus.isOK()) {
                ErrorDialog.openError(
                    getShell(),
                    Messages.getString("ChangeUnixExecutablePropertyTask.ErrorSettingPropertiesTitle"), //$NON-NLS-1$
                    null,
                    propertiesStatus);
                return propertiesStatus;
            }
        }

        // Undo others
        if (undoPaths.size() > 0) {
            // Use ResourceChangingCommand because the execute bit may change on
            // disk
            final IStatus undoStatus = getCommandExecutor().execute(
                new ResourceChangingCommand(
                    new UndoCommand(
                        repository,
                        ItemSpec.fromStrings(undoPaths.toArray(new String[undoPaths.size()]), RecursionType.NONE))));
            if (!undoStatus.isOK()) {
                ErrorDialog.openError(
                    getShell(),
                    Messages.getString("ChangeUnixExecutablePropertyTask.ErrorSettingPropertiesUndoTitle"), //$NON-NLS-1$
                    null,
                    undoStatus);
                return undoStatus;
            }
        }

        return Status.OK_STATUS;
    }

    private void prepareRequests(
        final ExtendedItem[] items,
        final List<ItemProperties> newProperties,
        final List<String> undoPaths) {
        for (final ExtendedItem item : items) {
            if (item.getTargetServerItem() == null) {
                continue;
            }

            // We queried on all properties above
            final PropertyValue[] allProperties = item.getPropertyValues();
            final PropertyValue executableProperty =
                PropertyUtils.selectMatching(allProperties, PropertyConstants.EXECUTABLE_KEY);

            /*
             * If there's a pending change on the item, and the only pending
             * change type is property, and there is only one property, and that
             * property matches the new executable property we're setting, we
             * can undo the entire pending change.
             *
             * Because properties in pending change objects are a combination of
             * the baseline item and the pending changes, we will miss the
             * chance to undo the change if some baseline properties which are
             * not executable properties appear in the item. No big deal (we'll
             * still remove the property below).
             */
            if (item.getPendingChange() != null
                && item.getPendingChange().contains(ChangeType.PROPERTY)
                && item.getPendingChange().remove(ChangeType.PROPERTY).equals(ChangeType.NONE)
                && executableProperty != null
                && allProperties.length == 1
                && executableProperty.equals(allProperties[0])) {
                undoPaths.add(item.getTargetServerItem());
                continue;
            }

            final boolean currentExecutable = PropertyConstants.EXECUTABLE_ENABLED_VALUE.equals(executableProperty);

            if (executable != currentExecutable) {
                final PropertyValue value = executable ? PropertyConstants.EXECUTABLE_ENABLED_VALUE
                    : PropertyConstants.EXECUTABLE_DISABLED_VALUE;

                newProperties.add(new ItemProperties(item.getTargetServerItem(), new PropertyValue[] {
                    value
                }));
            }
        }
    }
}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.jni.FileSystemUtils;

/**
 * <p>
 * The base class for Teamviewer actions. The base class, {@link ExtendedAction}
 * , provides naming, exception handling, selection tracking and adaptation, and
 * implements {@link IObjectActionDelegate}.
 * </p>
 * <p>
 * {@link IActionDelegate2} is implemented so
 * {@link #runWithEvent(IAction, Event)} is called, instead of
 * {@link #run(IAction)}, so we can examine the original event (is the shift key
 * held down during a click, etc.).
 * </p>
 * <h3>Understanding Enablement</h3>
 * <p>
 * These actions are all "declarative": they are hooked into the teamviewer via
 * plugin.xml as contributions to org.eclipse.ui.popupMenus. When
 * {@link #selectionChanged(IAction, org.eclipse.jface.viewers.ISelection)} is
 * called on a {@link TeamViewerAction}, the enablement for the action has
 * <b>already</b> been computed, and set via {@link Action#setEnabled(boolean)},
 * according to the enablement rules in the plugin.xml. These rules may specify
 * two kinds of conditions (they can specify more, but these are the important
 * ones):
 * <ul>
 * <li>The Java class of selected objects for which the action will be enabled
 * </li>
 * <li>The count of selected objects for which the action will be enabled</li>
 * </ul>
 * An action which specifies both of these conditions will only need to handle
 * <em>special</em> enablement cases in
 * {@link #selectionChanged(IAction, org.eclipse.jface.viewers.ISelection)}.
 * </p>
 * <p>
 * For example, a "Compare" action is enabled for exactly 1 selected object
 * which is a {@link TFSItem}, but the item must also have a local copy in a
 * working folder. If the "exactly 1 selected object which is a{@link TFSItem}"
 * part is already satified (it was computed by the framework and the action is
 * enabled)
 * {@link #selectionChanged(IAction, org.eclipse.jface.viewers.ISelection)} need
 * only <b>disable</b> the control if the item is not local.
 * </p>
 * <p>
 * <h3>Enablement Policy</h3>
 * </p>
 * <p>
 * Enablement should be precise. Actions should be enabled only when
 * appropriate. Microsoft's UI takes some shortcuts in this respect:
 * "Check In Pending Changes..." is always enabled, even if there are no pending
 * changes in the selection. An error dialog is raised if the user selects the
 * action when no changes are available, but our UI can be better (we don't want
 * the user to see error dialogs if good UI could prevent them).
 * </p>
 * <p>
 * Some actions, like {@link CheckinAction}, may require carefully designed
 * enablement tests to ensure the tests scale well with large selections and
 * pending changes. Other actions have simpler enablement calculations.
 * </p>
 * <p>
 * <h3>Label Plurality</h3>
 * </p>
 * <p>
 * Resist the temptation to change action labels (text) to be precise about
 * plurality ("Check In Pending Change..." vs. "Check In Pending Changes..." for
 * 1 vs. 2 selected items). This isn't so much for performance reasons
 * (enablement calculation must be careful about scaling anyway, and we probably
 * count them correctly), but for consistency with Microsoft's interface (read
 * on).
 * </p>
 * <p>
 * The most tempting labels to "correct" for plurality are for check in, undo,
 * and shelve. But if you consider precisely what these actions do (raise a
 * dialog with check boxes for final selection), any type of initial selection,
 * singular or plural, may result in <em>multiple</em> changes actually being
 * checked in, undone, or shelved after the dialog is used. So it's better to
 * leave the menu items plural all the time for this case.
 * </p>
 *
 * @threadsafety unknown
 */
public abstract class TeamViewerAction extends ExtendedAction
    implements IEditorActionDelegate, IWorkbenchWindowActionDelegate, IActionDelegate2 {
    private IEditorPart activeEditor;
    private Event event;

    /*
     * IWorkbenchWindowActionDelegate methods
     */

    /**
     * {@inheritDoc}
     *
     * Made final so extending classes must ignore the {@link IWorkbenchWindow}
     * lifecycle.
     */
    @Override
    public final void init(final IWorkbenchWindow window) {
        /*
         * Ignore this window, since our ancestor class ObjectActionDelegate
         * handles tracking the appropriate workbench windows.
         */
    }

    /*
     * IEditorActionDelegate methods
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
        activeEditor = targetEditor;
    }

    /*
     * IActionDelegate2 methods
     */

    /**
     * {@inheritDoc}
     *
     * Made final so extending classes must ignore the {@link IAction}
     * lifecycle.
     */
    @Override
    public final void init(final IAction action) {
        /*
         * This method is a lifecycle method for IActionDelegate2, but this
         * class doesn't care about the action lifecycle.
         */
    }

    /**
     * {@inheritDoc}
     *
     * Made final so extending classes need only implement {@link #run(IAction)}
     * . Protected methods on this class can be used to query the event
     * information ({@link #wasShiftKeyPressedWhenActionInvoked()}).
     */
    @Override
    public final void runWithEvent(final IAction action, final Event event) {
        this.event = event;
        run(action);
    }

    /*
     * IWorkbenchWindowActionDelegate && IActionDelegate2 methods
     */

    /**
     * {@inheritDoc}
     *
     * Made final so extending classes must ignore the lifecycle events.
     */
    @Override
    public final void dispose() {
    }

    /*
     * This class's methods.
     */

    /**
     * @return the {@link IEditorPart} which was last set via
     *         {@link #setActiveEditor(IAction, IEditorPart)}, possibly
     *         <code>null</code>
     */
    public IEditorPart getActiveEditor() {
        return activeEditor;
    }

    /**
     * @return the {@link Event} that was associated with the last call to
     *         {@link #runWithEvent(IAction, Event)}, possibly <code>null</code>
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Test whether the shift key was pressed when this action was invoked.
     *
     * @return true if the shift key was pressed when the action was invoked,
     *         false if it was not pressed
     */
    public boolean wasShiftKeyPressedWhenActionInvoked() {
        if (event != null) {
            return (event.stateMask & SWT.SHIFT) > 0;
        } else {
            return false;
        }
    }

    /**
     * Gets the only connection teamviewer has.
     *
     * @return the current {@link TFSRepository}, possibly <code>null</code>
     */
    protected TFSRepository getCurrentRepository() {
        return TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
    }

    /**
     * Gets the only workspace teamviewer currently has active.
     *
     * @return the current active {@link Workspace}, possibly <code>null</code>
     */
    protected Workspace getCurrentWorkspace() {
        final TFSRepository repository = getCurrentRepository();

        if (repository == null) {
            return null;
        }

        return repository.getWorkspace();
    }

    public boolean symbolicLinkSelected(final String path) {
        if (path == null) {
            return false;
        }
        return FileSystemUtils.getInstance().getAttributes(path).isSymbolicLink();
    }

    public boolean pendingAddSelected(final String path) {
        if (path == null) {
            return false;
        }
        return PendingChangesHelpers.isPendingAdd(getCurrentRepository(), path);
    }
}

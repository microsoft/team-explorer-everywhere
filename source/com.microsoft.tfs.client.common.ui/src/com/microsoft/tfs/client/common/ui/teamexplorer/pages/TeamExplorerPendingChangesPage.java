// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.pages;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.ShelvesetMRUCombo;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl.ValidationResult;
import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.PolicyFailureData;
import com.microsoft.tfs.client.common.ui.dialogs.vc.checkinpolicies.OverridePolicyFailuresDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.tasks.ViewBuildReportTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.AbstractShelveTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.CheckinTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.ShelveTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.ViewChangesetDetailsTask;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerResizeListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.CheckinValidationHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PageHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesViewModel;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangesChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent.WorkspaceEventSource;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.webservices.IdentityHelper;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class TeamExplorerPendingChangesPage extends TeamExplorerBasePage {
    public static final String ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage"; //$NON-NLS-1$

    public static final String SHELVESET_NAME_COMBO_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage#shelvesetNameText"; //$NON-NLS-1$

    public static final String CHECKIN_BUTTON_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage#checkinButton"; //$NON-NLS-1$

    public static final String SHELVE_BUTTON_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage#shelveButton"; //$NON-NLS-1$

    public static final String PRESERVE_CHANGES_CHECKBOX_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage#preserveChangesCheckBox"; //$NON-NLS-1$

    public static final CodeMarker PENDINGCHANGES_PAGE_LOADED = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage#PendingChangesPageLoaded"); //$NON-NLS-1$

    public static final CodeMarker AFTER_CHECKIN = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage#afterCheckin"); //$NON-NLS-1$

    private Button checkinButton;
    private ImageHyperlink shelveLink;

    private Composite statusComposite;
    private StackLayout statusStackLayout;

    private Composite changesetComposite;
    private Hyperlink changesetLink;
    private int changesetID;

    private Composite shelvesetComposite;
    private ShelvesetMRUCombo shelvesetNameCombo;
    private Hyperlink savedShelvesetLink;
    private String savedShelvesetName;

    private Composite gatedCheckinComposite;
    private Hyperlink gatedCheckinLink;
    private IBuildDetail gatedBuild;

    private Composite errorStatusComposite;
    private Label errorStatusLabel;

    private Composite shelveComposite;
    private Button preserveCheckbox;
    private Button evaluateCheckbox;

    private PendingChangesViewModel model;
    private Shell shell;

    private TeamExplorerPendingChangesPageState state;

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        model = TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel();
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context, final Object state) {
        initialize(monitor, context);
        if (state instanceof TeamExplorerPendingChangesPageState) {
            this.state = (TeamExplorerPendingChangesPageState) state;
        }
    }

    private void setCheckinAndShelveButtonEnablement(final PendingChangesViewModel model) {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                if (checkinButton.isDisposed()) {
                    return;
                }

                model.updateFilteredIncludedChanges();

                if (!haveIncludedChanges() && shelveComposite.isVisible()) {
                    closeShelvesetComposite();
                }
                if (checkinButton != null && !checkinButton.isDisposed()) {
                    checkinButton.setEnabled(haveIncludedChanges() && !shelveComposite.isVisible());
                }
                if (shelveLink != null && !shelveLink.isDisposed()) {
                    shelveLink.setEnabled(haveIncludedChanges());
                }
            }
        });
    }

    @Override
    public void refresh(final IProgressMonitor monitor, final TeamExplorerContext context) {
        final TFSRepository repository = context.getDefaultRepository();

        if (repository != null) {
            // Updates the PCVM's data store and refreshes pending
            // changes controls
            repository.getPendingChangeCache().refresh();

            // Refreshes candidates controls (not tracked by cache)
            if (repository.getWorkspace().getLocation() == WorkspaceLocation.LOCAL) {
                repository.getVersionControlClient().getEventEngine().firePendingChangeCandidatesChangedEvent(
                    new WorkspaceEvent(
                        EventSource.newFromHere(),
                        repository.getWorkspace(),
                        WorkspaceEventSource.INTERNAL));
            }
        }
    }

    @Override
    public Composite getPageContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        this.shell = parent.getShell();

        // Create the status composite. The status composite is initially hidden
        // and only becomes visible when a check-in completes. Once closed, it
        // remains hidden until another check-in completes.
        statusComposite = createStatusComposite(toolkit, parent, context);
        GridDataBuilder.newInstance().applyTo(statusComposite);

        TeamExplorerHelpers.toggleCompositeVisibility(statusComposite);

        // Create the page composite consisting of the check-in button, shelve
        // UI, and the actions drop down. This composite is always visible at
        // the top of the page unless the check-in status composite is visible.

        final Composite composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 3, false, 0, 5);

        checkinButton =
            toolkit.createButton(
                composite,
                Messages.getString("TeamExplorerPendingChangesPage.CheckInButton"), //$NON-NLS-1$
                SWT.FLAT);
        AutomationIDHelper.setWidgetID(checkinButton, CHECKIN_BUTTON_ID);
        checkinButton.setEnabled(haveIncludedChanges());
        GridDataBuilder.newInstance().applyTo(checkinButton);
        checkinButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                checkinClicked();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                checkinClicked();
            }
        });

        final String shelveText = Messages.getString("TeamExplorerPendingChangesPage.ShelveDropDownText"); //$NON-NLS-1$
        shelveLink = PageHelpers.createDropHyperlink(toolkit, composite, shelveText);
        shelveLink.setEnabled(haveIncludedChanges());
        GridDataBuilder.newInstance().applyTo(shelveLink);
        shelveLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                if (shelveComposite.isVisible()) {
                    closeShelvesetComposite();
                } else {
                    openShelvesetComposite();
                }
            }
        });

        // Create a menu manager for the action drop down.
        final MenuManager actionMenuManager = new MenuManager("#popup"); //$NON-NLS-1$
        actionMenuManager.setRemoveAllWhenShown(true);
        actionMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                if (!context.isConnectedToCollection()) {
                    final IAction disconnected = new Action() {

                        @Override
                        public String getText() {
                            return Messages.getString("TeamExplorerPendingChangesPage.DisconnectedActionMenuItem"); //$NON-NLS-1$
                        }

                        @Override
                        public boolean isEnabled() {
                            return false;
                        }
                    };

                    manager.add(disconnected);
                } else {
                    fillActionMenu(manager, context);
                }
            }
        });

        final String actionText = Messages.getString("TeamExplorerPendingChangesPage.ActionsLinkText"); //$NON-NLS-1$
        final Menu actionMenu = actionMenuManager.createContextMenu(composite.getShell());
        final ImageHyperlink actionLink = PageHelpers.createDropHyperlink(toolkit, composite, actionText, actionMenu);
        GridDataBuilder.newInstance().applyTo(actionLink);

        // Create the shelve composite (initially invisible).
        shelveComposite = createShelveComposite(toolkit, composite, context);
        GridDataBuilder.newInstance().hSpan(3).applyTo(shelveComposite);

        // Exclude the invisible composite from the layout.
        final GridData gridData = (GridData) shelveComposite.getLayoutData();

        if (state == null) {
            shelveComposite.setVisible(false);
            gridData.exclude = true;
        } else {
            shelveComposite.setVisible(state.isShelveCompositeVisible());
            gridData.exclude = !state.isShelveCompositeVisible();
        }

        // Add a listener for included pending changes is modified.
        final PendingChangesChangedListener listener = new IncludedChangesChangedListener();
        model.addIncludedPendingChangesChangedListener(listener);

        // Add resize listeners for the status and shelve composites.
        final TeamExplorerResizeListener shelveResizeListener = new TeamExplorerResizeListener(shelveComposite);
        final TeamExplorerResizeListener statusResizeListener = new TeamExplorerResizeListener(statusComposite);
        context.getEvents().addListener(TeamExplorerEvents.FORM_RESIZED, shelveResizeListener);
        context.getEvents().addListener(TeamExplorerEvents.FORM_RESIZED, statusResizeListener);

        // Remove the listeners when disposed.
        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                model.updateLastSavedCheckin();
                model.removeIncludedPendingChangesChangedListener(listener);
                context.getEvents().removeListener(TeamExplorerEvents.FORM_RESIZED, shelveResizeListener);
                context.getEvents().removeListener(TeamExplorerEvents.FORM_RESIZED, statusResizeListener);
            }
        });

        CodeMarkerDispatch.dispatch(PENDINGCHANGES_PAGE_LOADED);
        return composite;
    }

    private Composite createStatusComposite(
        final FormToolkit toolkit,
        final Composite parent,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);
        composite.setBackground(TeamExplorerHelpers.getDropCompositeBackground(parent));
        SWTUtil.gridLayout(composite, 1, false, 3, 3);

        final Composite innerComposite = toolkit.createComposite(composite);
        innerComposite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        innerComposite.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        SWTUtil.gridLayout(innerComposite, 2, false, 5, 5);
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(innerComposite);

        final Composite stackComposite = toolkit.createComposite(innerComposite);
        statusStackLayout = new StackLayout();
        stackComposite.setLayout(statusStackLayout);
        stackComposite.setBackground(innerComposite.getBackground());
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(stackComposite);

        changesetComposite = createChangesetStatusComposite(toolkit, stackComposite, context);
        gatedCheckinComposite = createGatedCheckinComposite(toolkit, stackComposite, context);
        shelvesetComposite = createShelvesetStatusComposite(toolkit, stackComposite, context);
        errorStatusComposite = createErrorStatusComposite(toolkit, stackComposite, context);

        final ImageHyperlink closeButton = toolkit.createImageHyperlink(innerComposite, SWT.PUSH);

        final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
        final Image image = imageHelper.getImage("/images/common/close_button.png"); //$NON-NLS-1$

        closeButton.setImage(image);
        closeButton.setText(""); //$NON-NLS-1$
        closeButton.setBackground(innerComposite.getBackground());
        GridDataBuilder.newInstance().hAlignRight().applyTo(closeButton);

        closeButton.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                TeamExplorerHelpers.toggleCompositeVisibility(statusComposite);
                TeamExplorerHelpers.relayoutContainingScrolledComposite(parent);
            }
        });

        composite.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
                image.dispose();
            }
        });

        return composite;
    }

    private Composite createChangesetStatusComposite(
        final FormToolkit toolkit,
        final Composite parent,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);
        composite.setBackground(parent.getBackground());

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 2, false, 0, 0);

        changesetLink = toolkit.createHyperlink(composite, "link", SWT.NONE); //$NON-NLS-1$
        changesetLink.setUnderlined(true);
        changesetLink.setBackground(parent.getBackground());
        GridDataBuilder.newInstance().applyTo(changesetLink);

        changesetLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                new ViewChangesetDetailsTask(shell, context.getDefaultRepository(), changesetID).run();
            }
        });

        final String labelText = Messages.getString("TeamExplorerPendingChangesPage.SuccessfullyCheckedIn"); //$NON-NLS-1$
        final Label label = toolkit.createLabel(composite, labelText, SWT.NONE);
        label.setBackground(parent.getBackground());
        label.setForeground(parent.getForeground());
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(label);

        return composite;
    }

    private Composite createShelvesetStatusComposite(
        final FormToolkit toolkit,
        final Composite parent,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);
        composite.setBackground(parent.getBackground());

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 2, false, 0, 0);

        savedShelvesetLink = toolkit.createHyperlink(composite, "link", SWT.NONE); //$NON-NLS-1$
        savedShelvesetLink.setUnderlined(true);
        savedShelvesetLink.setBackground(parent.getBackground());
        GridDataBuilder.newInstance().applyTo(savedShelvesetLink);

        savedShelvesetLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                PendingChangesHelpers.showShelvesetDetails(shell, context.getDefaultRepository(), savedShelvesetName);
            }
        });

        final String labelText = Messages.getString("TeamExplorerPendingChangesPage.SuccessfullyCreated"); //$NON-NLS-1$
        final Label label = toolkit.createLabel(composite, labelText, SWT.NONE);
        label.setBackground(parent.getBackground());
        label.setForeground(parent.getForeground());
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(label);

        return composite;
    }

    private Composite createGatedCheckinComposite(
        final FormToolkit toolkit,
        final Composite parent,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);
        composite.setBackground(parent.getBackground());

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 2, false, 0, 0);

        final String labelText = Messages.getString("TeamExplorerPendingChangesPage.GatedBuildQueuedText"); //$NON-NLS-1$
        final Label label = toolkit.createLabel(composite, labelText, SWT.NONE);
        label.setBackground(parent.getBackground());
        label.setForeground(parent.getForeground());
        GridDataBuilder.newInstance().applyTo(label);

        gatedCheckinLink =
            toolkit.createHyperlink(
                composite,
                Messages.getString("TeamExplorerPendingChangesPage.ViewStatus"), //$NON-NLS-1$
                SWT.NONE);
        gatedCheckinLink.setUnderlined(true);
        gatedCheckinLink.setBackground(parent.getBackground());
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(gatedCheckinLink);

        gatedCheckinLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                Check.notNull(gatedBuild, "gatedBuild"); //$NON-NLS-1$
                new ViewBuildReportTask(
                    shell,
                    gatedBuild.getBuildServer(),
                    gatedBuild.getURI(),
                    gatedBuild.getBuildNumber()).run();
            }
        });

        return composite;
    }

    private Composite createErrorStatusComposite(
        final FormToolkit toolkit,
        final Composite parent,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);
        composite.setBackground(parent.getBackground());

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 1, false, 0, 0);

        errorStatusLabel = toolkit.createLabel(composite, "", SWT.WRAP); //$NON-NLS-1$
        errorStatusLabel.setBackground(parent.getBackground());
        errorStatusLabel.setForeground(parent.getForeground());
        errorStatusLabel.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));

        GridDataBuilder.newInstance().applyTo(errorStatusLabel);
        return composite;
    }

    private Composite createShelveComposite(
        final FormToolkit toolkit,
        final Composite parent,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        // Text controls present in this composite, enable form-style borders,
        // must have at least 1 pixel margins
        toolkit.paintBordersFor(composite);
        SWTUtil.gridLayout(composite, 2, false, 5, 5);

        final Color subCompositeBackgroundColor = TeamExplorerHelpers.getDropCompositeBackground(parent);
        final Color subCompositeForegroundColor = TeamExplorerHelpers.getDropCompositeForeground(parent);
        final Color requiredBackgroundColor = parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
        final Color normalBackgroundColor = toolkit.getColors().getBackground();

        composite.setBackground(subCompositeBackgroundColor);
        composite.setForeground(subCompositeForegroundColor);

        shelvesetNameCombo = new ShelvesetMRUCombo(composite, SWT.BORDER);
        shelvesetNameCombo.setCaseSensitive(true);
        shelvesetNameCombo.setTextLimit(64);
        AutomationIDHelper.setWidgetID(shelvesetNameCombo, SHELVESET_NAME_COMBO_ID);
        shelvesetNameCombo.populateMRU();

        // TODO add watermark text
        String message = Messages.getString("TeamExplorerPendingChangesPage.ShelvesetNameWatermarkText"); //$NON-NLS-1$
        shelvesetNameCombo.setBackground(requiredBackgroundColor);
        GridDataBuilder.newInstance().hAlignFill().hGrab().hSpan(2).applyTo(shelvesetNameCombo);

        message = Messages.getString("ShelveDialog.PreserveButtonText"); //$NON-NLS-1$
        preserveCheckbox = toolkit.createButton(composite, message, SWT.CHECK);
        preserveCheckbox.setBackground(subCompositeBackgroundColor);
        preserveCheckbox.setForeground(subCompositeForegroundColor);
        AutomationIDHelper.setWidgetID(preserveCheckbox, PRESERVE_CHANGES_CHECKBOX_ID);
        GridDataBuilder.newInstance().hAlignFill().hGrab().hSpan(2).applyTo(preserveCheckbox);

        message = Messages.getString("ShelveDialog.EvaluatePoliciesButtonText"); //$NON-NLS-1$
        evaluateCheckbox = toolkit.createButton(composite, message, SWT.CHECK);
        if (state == null) {
            preserveCheckbox.setSelection(true);
            evaluateCheckbox.setSelection(false);
        } else {
            shelvesetNameCombo.setText(state.getShelvesetNameComboText());
            preserveCheckbox.setSelection(state.isPreserveCheckboxChecked());
            evaluateCheckbox.setSelection(state.isEvaluateCheckboxChecked());
        }
        evaluateCheckbox.setBackground(subCompositeBackgroundColor);
        evaluateCheckbox.setForeground(subCompositeForegroundColor);
        GridDataBuilder.newInstance().hAlignFill().hGrab().hSpan(2).applyTo(evaluateCheckbox);

        final Button shelveButton =
            toolkit.createButton(
                composite,
                Messages.getString("TeamExplorerPendingChangesPage.ShelveButtonText"), //$NON-NLS-1$
                SWT.PUSH);
        AutomationIDHelper.setWidgetID(shelveButton, SHELVE_BUTTON_ID);

        shelveButton.setBackground(subCompositeBackgroundColor);
        shelveButton.setEnabled(false);

        shelveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                shelveIt(context, preserveCheckbox, evaluateCheckbox);
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                shelveIt(context, preserveCheckbox, evaluateCheckbox);
            }

        });

        GridDataBuilder.newInstance().applyTo(shelveButton);
        shelvesetNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                shelveIt(context, preserveCheckbox, evaluateCheckbox);
            }

        });

        final Button closeButton = toolkit.createButton(
            composite,
            Messages.getString("TeamExplorerPendingChangesWorkItemsSection.CloseButtonText"), //$NON-NLS-1$
            SWT.PUSH);

        closeButton.setBackground(subCompositeBackgroundColor);
        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                closeShelvesetComposite();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                closeShelvesetComposite();
            }
        });

        GridDataBuilder.newInstance().applyTo(closeButton);

        shelvesetNameCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                if (shelvesetNameCombo.getText().length() == 0) {
                    shelvesetNameCombo.setBackground(requiredBackgroundColor);
                    shelveButton.setEnabled(false);
                } else {
                    shelvesetNameCombo.setBackground(normalBackgroundColor);
                    shelveButton.setEnabled(true);
                }
            }
        });

        return composite;
    }

    private void openShelvesetComposite() {
        if (!shelveComposite.isVisible()) {
            TeamExplorerHelpers.toggleCompositeVisibility(shelveComposite);
            TeamExplorerHelpers.relayoutContainingScrolledComposite(shelveComposite.getParent());
            setCheckinAndShelveButtonEnablement(model);
            shelvesetNameCombo.setFocus();
        }
    }

    private void closeShelvesetComposite() {
        if (shelveComposite.isVisible()) {
            shelvesetNameCombo.setText(""); //$NON-NLS-1$
            TeamExplorerHelpers.toggleCompositeVisibility(shelveComposite);
            TeamExplorerHelpers.relayoutContainingScrolledComposite(shelveComposite.getParent());
            setCheckinAndShelveButtonEnablement(model);
        }
    }

    private void shelveIt(
        final TeamExplorerContext context,
        final Button preserveCheckbox,
        final Button evaluateCheckbox) {
        final TFSRepository repository = context.getDefaultRepository();
        final String shelvesetName = shelvesetNameCombo.getText();
        final boolean preserveFlag = preserveCheckbox.getSelection();
        final boolean evaluatePolicies = evaluateCheckbox.getSelection();

        final PendingCheckin pendingCheckin = PendingChangesHelpers.getPendingCheckin(shell, model, evaluatePolicies);

        if (evaluatePolicies) {
            // Evaluate check-in notes and conflicts.
            final ValidationResult result =
                CheckinValidationHelper.validateForCheckin(shell, repository, pendingCheckin);
            if (result.getSucceeded() == false) {
                return;
            }

            // Evaluate check-in policies.
            final AtomicReference<PolicyFailure[]> outPolicyFailures = new AtomicReference<PolicyFailure[]>();
            final PolicyEvaluatorState state = model.evaluateCheckinPolicies(pendingCheckin, outPolicyFailures);

            if (state == PolicyEvaluatorState.CANCELLED) {
                return;
            } else if (outPolicyFailures.get().length > 0) {
                // We don't do anything with the policy override for shelve, but
                // do allow them to continue with an override comment.
                if (getPolicyFailureOverrideMessage(shell, state, true, model.getPolicyWarnings()) == null) {
                    return;
                }
            }
        }

        if (!PendingChangesHelpers.confirmShelvesetCanBeWritten(shell, repository, shelvesetName)) {
            return;
        }

        final AbstractShelveTask task = new ShelveTask(shell, repository, pendingCheckin, shelvesetName, preserveFlag);
        final IStatus status = task.run();

        if (status.isOK()) {
            shelvesetNameCombo.updateMRU(shelvesetName);
            shelvesetNameCombo.populateMRU();
            showShelvesetStatus(shelvesetName);
            closeShelvesetComposite();

            if (!preserveFlag) {
                model.clearComment();
                model.dissociateAllWorkItems();
                model.clearCheckinNotes();
            }
        } else if (status.getSeverity() == Status.CANCEL) {
            showErrorStatus(Messages.getString("TeamExplorerPendingChangesPage.ShelveCancelled")); //$NON-NLS-1$
        } else if (status.getException() != null) {
            showErrorStatus(status.getException().getLocalizedMessage());
        } else if (status.getMessage() != null) {
            showErrorStatus(status.getMessage());
        }
    }

    private void checkinClicked() {
        final PendingCheckin pendingCheckin = PendingChangesHelpers.getPendingCheckin(shell, model, false);
        if (pendingCheckin == null) {
            return;
        }

        final TFSRepository repository = model.getRepository();
        final ValidationResult result = CheckinValidationHelper.validateForCheckin(shell, repository, pendingCheckin);

        if (result.getSucceeded() == false) {
            return;
        }

        final AtomicReference<PolicyFailure[]> outPolicyFailures = new AtomicReference<PolicyFailure[]>();
        final PolicyEvaluatorState state = model.evaluateCheckinPolicies(pendingCheckin, outPolicyFailures);

        PolicyOverrideInfo policyOverrideInfo = null;
        final PolicyFailure[] policyFailures = outPolicyFailures.get();

        if (state == PolicyEvaluatorState.CANCELLED) {
            return;
        } else if (policyFailures.length > 0) {
            final String reason = getPolicyFailureOverrideMessage(shell, state, model.getPolicyWarnings());
            if (reason == null) {
                return;
            }

            policyOverrideInfo = new PolicyOverrideInfo(reason, policyFailures);
        }

        final int changeCount = pendingCheckin.getPendingChanges().getCheckedPendingChanges().length;
        if (!PendingChangesHelpers.confirmCheckin(shell, changeCount)) {
            return;
        }

        final CheckinTask checkinTask = new CheckinTask(shell, repository, pendingCheckin, policyOverrideInfo);
        final IStatus status = checkinTask.run();

        if (status.isOK()) {
            if (checkinTask.getChangesetID() > 0) {
                showCheckinStatus(checkinTask.getChangesetID());
            } else if (checkinTask.getGatedBuildDetail() != null) {
                showQueuedBuildStatus(checkinTask.getGatedBuildDetail());
            }

            if (checkinTask.getPendingChangesCleared()) {
                // Check-in was successful and pending changes where cleared.
                model.clearComment();
                model.dissociateAllWorkItems();
                model.clearCheckinNotes();
            }
        } else if (status.getSeverity() == Status.CANCEL) {
            showErrorStatus(Messages.getString("TeamExplorerPendingChangesPage.CheckinCancelled")); //$NON-NLS-1$
        } else if (status.getException() != null) {
            showErrorStatus(getExceptionMessage(status.getException()));
        } else if (status.getMessage() != null) {
            showErrorStatus(status.getMessage());
        }

        CodeMarkerDispatch.dispatch(AFTER_CHECKIN);

    }

    private String getExceptionMessage(final Throwable t) {
        final String mainErrorMessage = t.getLocalizedMessage();

        final Throwable cause = t.getCause();
        final String causeErrorMessage = cause != null ? cause.getLocalizedMessage() : null;

        final StringBuilder sb = new StringBuilder();

        if (!StringUtil.isNullOrEmpty(mainErrorMessage)) {
            sb.append(mainErrorMessage);
        }

        if (!StringUtil.isNullOrEmpty(causeErrorMessage)) {
            if (sb.length() > 0) {
                sb.append(": "); //$NON-NLS-1$
            }
            sb.append(causeErrorMessage);
        }

        if (sb.length() == 0) {
            sb.append(Messages.getString("TeamExplorerPendingChangesPage.UnexpectedCheckinError")); //$NON-NLS-1$
        }

        return sb.toString();
    }

    private void showCheckinStatus(final int changesetID) {
        this.changesetID = changesetID;

        final String format = Messages.getString("TeamExplorerPendingChangesPage.ChangesetLinkFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(format, Integer.toString(changesetID));
        changesetLink.setText(message);

        statusStackLayout.topControl = changesetComposite;
        showStatusComposite();
    }

    private void showShelvesetStatus(final String shelvesetName) {
        this.savedShelvesetName = shelvesetName;

        final String format = Messages.getString("TeamExplorerPendingChangesPage.ShelvesetLinkFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(format, shelvesetName);
        savedShelvesetLink.setText(message);

        statusStackLayout.topControl = shelvesetComposite;
        showStatusComposite();
    }

    private void showQueuedBuildStatus(final IBuildDetail gatedBuild) {
        this.gatedBuild = gatedBuild;

        statusStackLayout.topControl = gatedCheckinComposite;
        showStatusComposite();
    }

    private void showErrorStatus(final String errorMessage) {
        errorStatusLabel.setText(errorMessage);

        statusStackLayout.topControl = errorStatusComposite;
        showStatusComposite();
    }

    private void showStatusComposite() {
        if (statusComposite.getVisible() == false) {
            TeamExplorerHelpers.toggleCompositeVisibility(statusComposite);
            TeamExplorerHelpers.relayoutContainingScrolledComposite(statusComposite);
        }
    }

    private boolean haveIncludedChanges() {
        return model.getIncludedFilteredChangeCount() > 0;
    }

    private void fillActionMenu(final IMenuManager menuManager, final TeamExplorerContext context) {
        final PendingChangesViewModel model = TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel();

        final IAction unshelveAction = new Action() {
            @Override
            public void run() {
                PendingChangesHelpers.unshelve(shell, context.getDefaultRepository());
            }
        };

        unshelveAction.setText(Messages.getString("TeamExplorerPendingChangesPage.UnshelveCommandText")); //$NON-NLS-1$
        menuManager.add(unshelveAction);

        menuManager.add(new Separator());

        final IAction resolveConflictsAction = new Action() {
            @Override
            public void run() {
                PendingChangesHelpers.resolveConflicts(shell, context.getDefaultRepository());
            }
        };

        resolveConflictsAction.setText(
            Messages.getString("TeamExplorerPendingChangesPage.ResolveConflictsCommandText")); //$NON-NLS-1$
        menuManager.add(resolveConflictsAction);

        final IAction undoAllAction = new Action() {
            @Override
            public void run() {
                PendingChangesHelpers.undoAll(shell, context.getDefaultRepository());
            }
        };

        undoAllAction.setText(Messages.getString("TeamExplorerPendingChangesPage.UndoAllCommandText")); //$NON-NLS-1$
        menuManager.add(undoAllAction);

        menuManager.add(new Separator());

        final IAction evaluateCheckinPoliciesAction = new Action() {
            @Override
            public void run() {
                PendingChangesHelpers.evaluatePolicies(shell, model);
            }
        };

        evaluateCheckinPoliciesAction.setText(
            Messages.getString("TeamExplorerPendingChangesPage.EvaluatePoliciesCommandText")); //$NON-NLS-1$
        menuManager.add(evaluateCheckinPoliciesAction);

        if (model != null && model.isLocalWorkspace()) {
            final IAction detectLocalChangesAction = new Action() {
                @Override
                public void run() {
                    PendingChangesHelpers.detectLocalChanges(shell, context.getDefaultRepository());
                }
            };

            detectLocalChangesAction.setText(
                Messages.getString("TeamExplorerPendingChangesPage.DetectLocalChangesMenuText")); //$NON-NLS-1$
            menuManager.add(detectLocalChangesAction);
        }

        menuManager.add(new Separator());

        final IAction undoUnchangedAction = new Action() {
            @Override
            public void run() {
                PendingChangesHelpers.undoUnchangedPendingChanges(shell, context.getDefaultRepository());
            }
        };

        undoUnchangedAction.setText(Messages.getString("TeamExplorerPendingChangesPage.UndoUnchangedCommandText")); //$NON-NLS-1$
        menuManager.add(undoUnchangedAction);

        menuManager.add(new Separator());

        final String subMenuText = Messages.getString("TeamExplorerPendingChangesPage.SwitchWorkspaceMenuText"); //$NON-NLS-1$
        final IMenuManager subMenu = new MenuManager(subMenuText);
        subMenu.setRemoveAllWhenShown(true);
        subMenu.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                final Workspace currentWorkspace = model.getWorkspace();
                final Workspace[] workspaces = model.getWorkspaces();

                if (workspaces == null || workspaces.length == 0) {
                    final IAction emptyAction = new Action() {
                    };

                    final String text =
                        workspaces == null ? Messages.getString("TeamExplorerPendingChangesPage.CannotGetWorkspaces") : //$NON-NLS-1$
                            Messages.getString("TeamExplorerPendingChangesPage.NoWorkpacesMenuText"); //$NON-NLS-1$

                    emptyAction.setText(text);
                    emptyAction.setEnabled(false);
                    subMenu.add(emptyAction);
                } else {
                    final TeamFoundationIdentity authorizedIdentity =
                        currentWorkspace.getClient().getConnection().getAuthorizedIdentity();

                    for (final Workspace workspace : workspaces) {
                        final IAction switchWorkspaceAction = new Action() {
                            @Override
                            public void run() {
                                PendingChangesHelpers.switchWorkspace(shell, workspace);
                            }
                        };

                        String menuItemName = workspace.getName();
                        if (!IdentityHelper.identityHasName(authorizedIdentity, workspace.getOwnerName())) {
                            menuItemName += ";" + workspace.getOwnerDisplayName(); //$NON-NLS-1$
                        }

                        switchWorkspaceAction.setText(menuItemName);
                        switchWorkspaceAction.setChecked(workspace.equals(currentWorkspace));
                        subMenu.add(switchWorkspaceAction);
                    }
                }
            }
        });

        menuManager.add(subMenu);

        final IAction manageWorkspacesAction = new Action() {
            @Override
            public void run() {
                PendingChangesHelpers.manageWorkspaces(shell);
            }
        };

        manageWorkspacesAction.setText(
            Messages.getString("TeamExplorerPendingChangesPage.ManageWorkspacesCommandText")); //$NON-NLS-1$
        menuManager.add(manageWorkspacesAction);
    }

    public static String getPolicyFailureOverrideMessage(
        final Shell shell,
        final PolicyEvaluatorState evaluatorState,
        final PolicyFailureData[] failures) {
        return getPolicyFailureOverrideMessage(shell, evaluatorState, false, failures);
    }

    public static String getPolicyFailureOverrideMessage(
        final Shell shell,
        final PolicyEvaluatorState evaluatorState,
        final boolean shelving,
        final PolicyFailureData[] failures) {
        if (evaluatorState == PolicyEvaluatorState.POLICIES_LOAD_ERROR || failures.length > 0) {
            String customMessage = null;
            if (evaluatorState == PolicyEvaluatorState.POLICIES_LOAD_ERROR) {
                customMessage = Messages.getString("CheckinControl.PoliciesLoadErrorMultilineText"); //$NON-NLS-1$
            } else if (shelving) {
                customMessage = Messages.getString("OverridePolicyFailuresDialog.ShelveErrorStatusLabelText"); //$NON-NLS-1$
            }

            // Passing a null message uses the default message.
            final OverridePolicyFailuresDialog dialog = new OverridePolicyFailuresDialog(shell, customMessage);

            if (IDialogConstants.OK_ID == dialog.open()) {
                final String override = dialog.getOverrideComment();
                if (override != null && override.length() > 0) {
                    return override;
                }
            }
        }

        return null;
    }

    private class IncludedChangesChangedListener implements PendingChangesChangedListener {
        @Override
        public void onPendingChangesChanged(final WorkspaceEvent e) {
            setCheckinAndShelveButtonEnablement(model);
        }
    };

    @Override
    public Object saveState() {
        if (shelveComposite == null || shelvesetNameCombo == null) {
            state = null;
        } else {
            state = new TeamExplorerPendingChangesPageState(
                shelveComposite.isVisible(),
                shelvesetNameCombo.getText(),
                preserveCheckbox.getSelection(),
                evaluateCheckbox.getSelection());
        }
        return state;
    }
}

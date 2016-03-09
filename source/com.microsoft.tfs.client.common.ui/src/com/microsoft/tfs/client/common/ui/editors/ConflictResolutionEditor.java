// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.editors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tfs.client.common.commands.ResolveConflictsCommand;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryConflictsCommand;
import com.microsoft.tfs.client.common.commands.vc.RefreshPendingChangesCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.compare.TFSItemContentComparator;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.conflicts.ConflictComparisonFactory;
import com.microsoft.tfs.client.common.ui.conflicts.ConflictComparisonOption;
import com.microsoft.tfs.client.common.ui.conflicts.resolutions.EclipseMergeConflictResolution;
import com.microsoft.tfs.client.common.ui.controls.vc.ConflictResolutionControl;
import com.microsoft.tfs.client.common.ui.controls.vc.ConflictResolutionControl.ConflictResolutionCancelledListener;
import com.microsoft.tfs.client.common.ui.controls.vc.ConflictResolutionControl.ConflictResolutionSelectionListener;
import com.microsoft.tfs.client.common.ui.controls.vc.ConflictTable;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ConflictResolutionEncodingDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ConflictResolutionNameAndEncodingDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ConflictResolutionNameSelectionDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ConflictResolutionRenameDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.ConflictHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.FilenameConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionStatus;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionStatusListener;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.CoreConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ExternalConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ExternalConflictResolution.ExternalConflictResolver;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationStartedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.UndonePendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class ConflictResolutionEditor extends EditorPart implements ConflictResolutionStatusListener {
    public static final String ID = "com.microsoft.tfs.client.common.ui.editors.ConflictResolutionEditor"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(ConflictResolutionEditor.class);

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private final ConflictResolutionEditorRepositoryManagerListener repositoryManagerListener =
        new ConflictResolutionEditorRepositoryManagerListener();
    private final ConflictResolutionEditorCoreListener coreEventListener = new ConflictResolutionEditorCoreListener();

    private TFSRepository repository;

    private Label summaryLabel;

    private ToolBar toolbar;
    private ToolItem autoResolveItem;
    private ToolItem getAllConflictsItem;
    private ToolItem refreshItem;
    private ToolItem compareItem;

    private ConflictTable conflictTable;

    private ConflictResolutionControl conflictResolutionControl;

    private ConflictDescription[] initialConflicts = null;
    private ItemSpec[] filters = null;

    private boolean needsPaint = false;
    private boolean hasPainted = false;
    private boolean disposed = false;

    public ConflictResolutionEditor() {
        final RepositoryManager repositoryManager =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();

        repositoryManager.addListener(repositoryManagerListener);

        setRepository(repositoryManager.getDefaultRepository());
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void doSave(final IProgressMonitor monitor) {
        /* Should never be called as isSaveAllowed should return false. */
        throw new RuntimeException("Saving conflict results is not implemented."); //$NON-NLS-1$
    }

    @Override
    public void doSaveAs() {
        /* Should never be called as isSaveAllowed should return false. */
        throw new RuntimeException("Saving conflict results is not implemented."); //$NON-NLS-1$
    }

    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    @Override
    public void setInput(final IEditorInput input) {
        Check.isTrue(input instanceof ConflictResolutionEditorInput, "input instanceof ConflictResolutionEditorInput"); //$NON-NLS-1$

        repository = ((ConflictResolutionEditorInput) input).getRepository();
        initialConflicts = ((ConflictResolutionEditorInput) input).getConflictDescriptions();
        filters = computeFilters(initialConflicts);

        if (hasPainted) {
            if (initialConflicts == null) {
                queryConflicts(null);
            } else {
                setConflictDescriptions(initialConflicts);
            }
        } else {
            needsPaint = true;
        }

        super.setInput(input);
    }

    private ItemSpec[] computeFilters(final ConflictDescription[] conflictDescriptions) {
        if (conflictDescriptions == null) {
            return null;
        }

        final ItemSpec[] filters = new ItemSpec[conflictDescriptions.length];

        for (int i = 0; i < conflictDescriptions.length; i++) {
            filters[i] = new ItemSpec(conflictDescriptions[i].getServerPath(), RecursionType.NONE);
        }

        return filters;
    }

    @Override
    public void createPartControl(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        /* Compute metrics in pixels */
        final GC gc = new GC(composite);
        final FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();

        Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_SPACING);
        Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_SPACING);
        final int marginWidth = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_MARGIN);
        final int marginHeight = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_MARGIN);

        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

        final Composite summaryComposite = new Composite(composite, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(summaryComposite);

        final GridLayout summaryLayout = new GridLayout(1, false);
        summaryLayout.horizontalSpacing = 0;
        summaryLayout.verticalSpacing = 0;
        summaryLayout.marginWidth = marginWidth;
        summaryLayout.marginHeight = marginHeight;
        summaryComposite.setLayout(summaryLayout);
        summaryComposite.setBackground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

        summaryLabel = new Label(summaryComposite, SWT.NONE);
        summaryLabel.setBackground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        summaryLabel.setForeground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(summaryLabel);

        final Label separatorLabel = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(separatorLabel);

        final Composite toolbarComposite = new Composite(composite, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(toolbarComposite);

        final GridLayout toolbarCompositeLayout = new GridLayout(1, false);
        toolbarCompositeLayout.horizontalSpacing = 0;
        toolbarCompositeLayout.verticalSpacing = 0;
        toolbarCompositeLayout.marginWidth = marginWidth;
        toolbarCompositeLayout.marginHeight = 0;
        toolbarComposite.setLayout(toolbarCompositeLayout);

        toolbar = new ToolBar(toolbarComposite, SWT.HORIZONTAL | SWT.FLAT | SWT.RIGHT);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(toolbar);

        setupToolbar(toolbar);

        conflictTable = new ConflictTable(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
        conflictTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                updateSelection();
            }
        });

        final MenuManager menuManager = new MenuManager("#popup"); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        conflictTable.setMenu(menuManager.createContextMenu(conflictTable));

        GridDataBuilder.newInstance().grab().fill().applyTo(conflictTable);

        getSite().setSelectionProvider(conflictTable);

        /*
         * Set up the resolution options control
         */

        conflictResolutionControl = new ConflictResolutionControl(composite, SWT.NONE);
        conflictResolutionControl.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent e) {
                conflictTable.setSelection(e.getSelection());
            }
        });
        conflictResolutionControl.addConflictResolutionSelectionListener(new ConflictResolutionSelectionListener() {
            @Override
            public void conflictResolutionSelected(
                final ConflictDescription[] conflictDescriptions,
                final ConflictResolution resolution) {
                if (conflictDescriptions.length == 1) {
                    resolveConflict(conflictDescriptions[0], resolution);
                } else {
                    resolveConflicts(conflictDescriptions, resolution);
                }
            }
        });
        conflictResolutionControl.addConflictResolutionCancelledListener(new ConflictResolutionCancelledListener() {
            @Override
            public void conflictResolutionCancelled(
                final ConflictDescription conflictDescription,
                final ConflictResolution resolution) {
                resolution.removeStatusListener(ConflictResolutionEditor.this);
                resolution.cancel();
            }
        });
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(conflictResolutionControl);

        setPartName(Messages.getString("ConflictResolutionEditor.PartName")); //$NON-NLS-1$

        if (needsPaint == true) {
            if (initialConflicts == null) {
                queryConflicts(null);
            } else {
                setConflictDescriptions(initialConflicts);
            }
        } else {
            updateSummary();
            updateSelection();
        }

        hasPainted = true;
    }

    private void setupToolbar(final ToolBar toolbar) {
        autoResolveItem = new ToolItem(toolbar, SWT.PUSH);
        autoResolveItem.setText(Messages.getString("ConflictResolutionEditor.ActionAutoResolve")); //$NON-NLS-1$
        autoResolveItem.setEnabled(false);
        autoResolveItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                autoResolveAll();
            }
        });

        new ToolItem(toolbar, SWT.SEPARATOR);

        getAllConflictsItem = new ToolItem(toolbar, SWT.PUSH);
        getAllConflictsItem.setText(Messages.getString("ConflictResolutionEditor.ActionGetAllConflicts")); //$NON-NLS-1$
        getAllConflictsItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                queryConflicts(null);
            }
        });

        refreshItem = new ToolItem(toolbar, SWT.PUSH);
        refreshItem.setText(Messages.getString("ConflictResolutionEditor.ActionRefresh")); //$NON-NLS-1$
        refreshItem.setImage(imageHelper.getImage("images/common/refresh.gif")); //$NON-NLS-1$
        refreshItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                queryConflicts(filters);
            }
        });

        new ToolItem(toolbar, SWT.SEPARATOR);

        compareItem = new ToolItem(toolbar, SWT.DROP_DOWN);
        compareItem.setText(Messages.getString("ConflictResolutionEditor.ActionCompare")); //$NON-NLS-1$
        compareItem.setImage(imageHelper.getImage("images/vc/compare.gif")); //$NON-NLS-1$
        compareItem.setEnabled(false);
        compareItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final ConflictDescription[] conflict = conflictTable.getSelectedElements();

                Check.isTrue(conflict.length == 1, "conflict.length == 1"); //$NON-NLS-1$

                if ((e.detail & SWT.ARROW) == SWT.ARROW) {
                    showCompareMenu(((ToolItem) e.widget).getParent().toDisplay(new Point(e.x, e.y)), conflict[0]);
                } else {
                    openDefaultComparison(conflict[0]);
                }
            }
        });
    }

    private void setEnabled(final boolean enabled) {
        if (enabled) {
            summaryLabel.setEnabled(true);
            toolbar.setEnabled(true);
            autoResolveItem.setEnabled(
                conflictTable.getConflictDescriptions() != null && conflictTable.getConflictDescriptions().length > 0);
            getAllConflictsItem.setEnabled(true);
            refreshItem.setEnabled(true);
            compareItem.setEnabled(false);
            conflictTable.setEnabled(true);
        } else {
            toolbar.setEnabled(false);
            autoResolveItem.setEnabled(false);
            getAllConflictsItem.setEnabled(false);
            refreshItem.setEnabled(false);
            compareItem.setEnabled(false);
            conflictTable.setEnabled(false);
        }
    }

    private void updateSelection() {
        final ConflictDescription[] selection = conflictTable.getSelectedElements();
        ConflictComparisonOption[] comparisons = null;

        if (selection.length == 1) {
            try {
                comparisons = ConflictComparisonFactory.getConflictComparison(selection[0]).getOptions();
            } catch (final Exception e) {
                log.warn("Could not determine conflict comparison options", e); //$NON-NLS-1$
            }
        }

        compareItem.setEnabled(selection.length == 1 && comparisons != null && comparisons.length > 0);

        conflictResolutionControl.setConflictDescriptions(conflictTable.getSelectedElements());
    }

    private void updateSummary() {
        final ConflictDescription[] conflicts = conflictTable.getConflictDescriptions();

        if ((filters == null || filters.length == 0) && conflicts.length == 0) {
            summaryLabel.setText(Messages.getString("ConflictResolutionEditor.SummaryNoConflicts")); //$NON-NLS-1$
            return;
        } else if (conflicts.length == 0) {
            summaryLabel.setText(Messages.getString("ConflictResolutionEditor.SummaryPathFilterNoConflicts")); //$NON-NLS-1$
            return;
        }

        final StringBuffer typeSummary = new StringBuffer();

        final Map<String, Integer> countByType = new TreeMap<String, Integer>();

        for (final ConflictDescription conflict : conflicts) {
            if (countByType.containsKey(conflict.getName())) {
                countByType.put(conflict.getName(), countByType.get(conflict.getName()) + 1);
            } else {
                countByType.put(conflict.getName(), 1);
            }
        }

        int types = 0;
        for (final Entry<String, Integer> entry : countByType.entrySet()) {
            if (types > 0) {
                typeSummary.append(Messages.getString("ConflictResolutionEditor.SummaryTypeSeparator")); //$NON-NLS-1$
            }

            types++;

            typeSummary.append(
                MessageFormat.format(
                    Messages.getString("ConflictResolutionEditor.SummaryNumberOfTypeFormat"), //$NON-NLS-1$
                    entry.getValue(),
                    entry.getKey()));
        }

        if ((filters == null || filters.length == 0) && conflicts.length == 1) {
            summaryLabel.setText(
                MessageFormat.format(
                    Messages.getString("ConflictResolutionEditor.SummaryOneConflictFormat"), //$NON-NLS-1$
                    typeSummary.toString()));
        } else if (filters == null || filters.length == 0) {
            summaryLabel.setText(MessageFormat.format(
                Messages.getString("ConflictResolutionEditor.SummaryFormat"), //$NON-NLS-1$
                conflicts.length,
                typeSummary.toString()));
        } else if (conflicts.length == 1) {
            summaryLabel.setText(
                MessageFormat.format(
                    Messages.getString("ConflictResolutionEditor.SummaryPathFilterOneConflictFormat"), //$NON-NLS-1$
                    typeSummary.toString()));
        } else {
            summaryLabel.setText(
                MessageFormat.format(
                    Messages.getString("ConflictResolutionEditor.SummaryPathFilterFormat"), //$NON-NLS-1$
                    conflicts.length,
                    typeSummary.toString()));
        }
    }

    private void showCompareMenu(final Point location, final ConflictDescription conflict) {
        final ConflictComparisonOption[] comparisons =
            ConflictComparisonFactory.getConflictComparison(conflict).getOptions();

        final Menu compareMenu = new Menu(getSite().getShell(), SWT.POP_UP);

        for (final ConflictComparisonOption comparison : comparisons) {
            final Object originalNode = comparison.getOriginalNode();
            final Object modifiedNode = comparison.getModifiedNode();

            final MenuItem compareItem = new MenuItem(compareMenu, SWT.NONE);

            final String messageFormat = Messages.getString("ConflictResolutionEditor.CompareActionTextFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, comparison.getModifiedTitle(), comparison.getOriginalTitle());

            compareItem.setText(message);
            compareItem.setEnabled(modifiedNode != null && originalNode != null);

            compareItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    openComparison(conflict, originalNode, modifiedNode);
                }
            });
        }

        getSite().getShell().setMenu(compareMenu);

        compareMenu.setLocation(location);
        compareMenu.setVisible(true);
    }

    private void fillContextMenu(final IMenuManager menu) {
        if (conflictTable.getSelectedElements().length != 1) {
            return;
        }

        final MenuManager compareSubMenu =
            new MenuManager(Messages.getString("ConflictResolutionEditor.CompareMenuText")); //$NON-NLS-1$

        final ConflictDescription conflict = conflictTable.getSelectedElements()[0];
        final ConflictComparisonOption[] comparisons =
            ConflictComparisonFactory.getConflictComparison(conflict).getOptions();

        for (final ConflictComparisonOption comparison : comparisons) {
            final Object originalNode = comparison.getOriginalNode();
            final Object modifiedNode = comparison.getModifiedNode();

            final String messageFormat = Messages.getString("ConflictResolutionEditor.CompareActionTextFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, comparison.getModifiedTitle(), comparison.getOriginalTitle());

            final Action compareAction = new Action(message) {
                @Override
                public void run() {
                    openComparison(conflict, originalNode, modifiedNode);
                }
            };
            compareAction.setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));

            compareSubMenu.add(compareAction);
        }

        menu.add(compareSubMenu);
    }

    private void autoResolveAll() {
        if (this.repository == null) {
            return;
        }

        final ConflictDescription[] conflictDescriptions = conflictTable.getConflictDescriptions();
        final List<ConflictResolution> resolutionList = new ArrayList<ConflictResolution>();

        /* Weed out conflicts that cannot be automerged. */
        for (final ConflictDescription conflictDescription : conflictDescriptions) {
            /* Ignore currently running resolutions. */
            if (conflictResolutionControl.isResolving(conflictDescription)) {
                continue;
            }

            /*
             * Conflicts that are rename changes, or encoding changes, must be
             * resolved manually.
             */
            if (conflictDescription.getConflict().isEncodingMismatched()
                || conflictDescription.getConflict().isNameChanged()) {
                continue;
            }

            final CoreConflictResolution automergeResolution = new CoreConflictResolution(
                conflictDescription,
                Messages.getString("ConflictResolutionEditor.AutomergeResolutionDescription"), //$NON-NLS-1$
                Messages.getString("ConflictResolutionEditor.AutomergeResolutionHelpText"), //$NON-NLS-1$
                ConflictResolutionOptions.NONE,
                Resolution.ACCEPT_MERGE);

            /*
             * Don't bother trying to resolve conflicts that we've already
             * analyzed.
             */
            if (!conflictDescription.isResolutionEnabled(automergeResolution)) {
                continue;
            }

            resolutionList.add(automergeResolution);
        }

        if (resolutionList.size() == 0) {
            MessageDialog.openInformation(
                getSite().getShell(),
                Messages.getString("ConflictResolutionEditor.AutomergeNoCandidatesTitle"), //$NON-NLS-1$
                Messages.getString("ConflictResolutionEditor.AutomergeNoCandidates")); //$NON-NLS-1$

            return;
        }

        final ConflictResolution[] resolutions = resolutionList.toArray(new ConflictResolution[resolutionList.size()]);

        final ResolveConflictsCommand resolver = new ResolveConflictsCommand(repository, resolutions);

        UICommandExecutorFactory.newUICommandExecutor(getSite().getShell()).execute(
            new ResourceChangingCommand(resolver));

        resolutionFinished(resolutions, resolver.getStatuses());
    }

    private void openDefaultComparison(final ConflictDescription conflict) {
        final ConflictComparisonOption[] comparisons =
            ConflictComparisonFactory.getConflictComparison(conflict).getOptions();

        openComparison(conflict, comparisons[0].getOriginalNode(), comparisons[0].getModifiedNode());
    }

    private void openComparison(
        final ConflictDescription conflict,
        final Object originalNode,
        final Object modifiedNode) {
        final Compare compare = new Compare();

        compare.setOriginal(originalNode);
        compare.setModified(modifiedNode);

        compare.addComparator(TFSItemContentComparator.INSTANCE);

        compare.setUIType(CompareUIType.DIALOG);

        compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(getSite().getShell()));
        compare.open();
    }

    private void queryConflicts(final ItemSpec[] filters) {
        setEnabled(false);

        if (summaryLabel != null && !summaryLabel.isDisposed()) {
            summaryLabel.setText(Messages.getString("ConflictResolutionEditor.SummaryQueryingConflicts")); //$NON-NLS-1$
        }

        if (conflictTable != null && !conflictTable.isDisposed()) {
            conflictTable.setConflictDescriptions(new ConflictDescription[0]);
        }

        this.filters = filters;

        final ConflictResolutionEditorQueryCommand queryCommand = new ConflictResolutionEditorQueryCommand(filters);
        final ICommandExecutor commandExecutor = UICommandExecutorFactory.newUIJobCommandExecutor(getSite().getShell());

        commandExecutor.execute(queryCommand);
    }

    private void setConflictDescriptions(final ConflictDescription[] conflicts) {
        conflictTable.setConflictDescriptions(conflicts);
        updateSummary();
        setEnabled(true);
    }

    @Override
    public void dispose() {
        imageHelper.dispose();

        this.disposed = true;

        /* setRepository will unhook core events */
        setRepository(null);
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().removeListener(
            repositoryManagerListener);
    }

    @Override
    public void setFocus() {
        conflictTable.setFocus();
    }

    /**
     * Raise the dialog prompting the user for conflict resolution, and then
     * attempt to resolve the conflict with that given resolution.
     *
     * @param description
     *        ConflictDescription to resolve
     */
    private void resolveConflict(final ConflictDescription conflictDescription, final ConflictResolution resolution) {
        if (this.repository == null) {
            return;
        }

        if (!promptForMissingResolutionData(conflictDescription, resolution)) {
            return;
        }

        // add this as a status listener so that we can handle async resolutions
        // (eg, ExternalConflictResolution)
        resolution.addStatusListener(this);

        /*
         * Add the resolution options control as a status listener so that it
         * can update resolution options when an external resolution is running.
         */
        resolution.addStatusListener(conflictResolutionControl);

        /* Run us in a workspace command. */
        if (resolution instanceof ExternalConflictResolution) {
            ((ExternalConflictResolution) resolution).setConflictResolver(
                new ResourceChangingConflictResolver(getSite().getShell()));
        }

        // resolve the conflict
        final ResolveConflictsCommand resolver = new ResolveConflictsCommand(repository, resolution);

        final ICommandExecutor commandExecutor;

        /*
         * TODO: this is quick and dirty for SP1, replace with a more elegant
         * solution.
         *
         * When running a command executor with a delay, the internal merge tool
         * will typically pop up parented on the conflict resolution dialog,
         * then the command executor dialog can pop up the "Resolving..."
         * progress dialog parented on the same shell after the delay. This can
         * lead to UI shell parenting deadlocks. Thus, using a command executor
         * with no delay will guarantee that the merge dialog parents itself off
         * the best parent (the progress dialog.)
         */
        if (resolution instanceof EclipseMergeConflictResolution) {
            commandExecutor = UICommandExecutorFactory.newUICommandExecutor(getSite().getShell(), 0);
        } else {
            commandExecutor = UICommandExecutorFactory.newUICommandExecutor(getSite().getShell());
        }

        commandExecutor.execute(new ResourceChangingCommand(resolver));

        // the status listener will handle notification, etc
    }

    private boolean promptForMissingResolutionData(
        final ConflictDescription conflictDescription,
        final ConflictResolution resolution) {
        /* Filename conflicts (eg, add of an existing filename) */
        if (resolution.needsNewPath() && conflictDescription instanceof FilenameConflictDescription) {
            final ConflictResolutionRenameDialog renameDialog =
                new ConflictResolutionRenameDialog(getSite().getShell());
            renameDialog.setFilename(conflictDescription.getServerPath());

            if (renameDialog.open() != IDialogConstants.OK_ID) {
                return false;
            }

            resolution.setNewPath(renameDialog.getFilename());
        }

        /*
         * User must resolve both a name conflict and an encoding conflict.
         */
        else if (resolution.needsNewPath() && resolution.needsEncodingSelection()) {
            final ConflictResolutionNameAndEncodingDialog nameAndEncodingDialog =
                new ConflictResolutionNameAndEncodingDialog(getSite().getShell());
            nameAndEncodingDialog.setConflictDescription(conflictDescription);

            if (nameAndEncodingDialog.open() != IDialogConstants.OK_ID) {
                return false;
            }

            resolution.setNewPath(nameAndEncodingDialog.getFilename());
            resolution.setEncoding(nameAndEncodingDialog.getFileEncoding());
        }

        /*
         * Some version / merge conflicts require name selection (eg, target
         * rename)
         */
        else if (resolution.needsNewPath()) {
            final ConflictResolutionNameSelectionDialog nameDialog =
                new ConflictResolutionNameSelectionDialog(getSite().getShell());
            nameDialog.setConflictDescription(conflictDescription);

            if (nameDialog.open() != IDialogConstants.OK_ID) {
                return false;
            }

            resolution.setNewPath(nameDialog.getFilename());
        }

        else if (resolution.needsEncodingSelection()) {
            final ConflictResolutionEncodingDialog encodingDialog =
                new ConflictResolutionEncodingDialog(getSite().getShell());

            encodingDialog.setConflictDescription(conflictDescription);

            if (encodingDialog.open() != IDialogConstants.OK_ID) {
                return false;
            }

            resolution.setEncoding(encodingDialog.getFileEncoding());
        }

        return true;
    }

    /**
     * Raise the dialog prompting the user for conflict resolutions, and then
     * attempt to resolve the conflicts with that given resolution(s).
     *
     * @param descriptions
     *        ConflictDescriptions to resolve
     */
    private void resolveConflicts(final ConflictDescription[] descriptions, final ConflictResolution resolution) {
        if (this.repository == null) {
            return;
        }

        /*
         * We were given a "dummy" conflict resolution (one for the category,
         * not tied to a conflict). Create a proper resolution for each
         * conflict.
         */
        final ConflictResolution[] resolutions = new ConflictResolution[descriptions.length];

        for (int i = 0; i < descriptions.length; i++) {
            resolutions[i] = resolution.newForConflictDescription(descriptions[i]);
        }

        final ResolveConflictsCommand resolver = new ResolveConflictsCommand(repository, resolutions);

        UICommandExecutorFactory.newUICommandExecutor(getSite().getShell()).execute(
            new ResourceChangingCommand(resolver));

        resolutionFinished(resolutions, resolver.getStatuses());
    }

    /**
     * Complete any resolution for a single conflict. (Raise errors, remove from
     * the list(s), etc.)
     *
     * TODO: this needs to fire to the conflict manager for the plugin.
     *
     * @param resolution
     *        Conflict Resolution that finished
     * @param status
     *        The ConflictResolutionStatus that resolution completed with
     */
    private void resolutionFinished(final ConflictResolution resolution, final ConflictResolutionStatus status) {
        if (this.repository == null) {
            log.warn("Could not resolve conflict.  Connection went offline before resolution finished."); //$NON-NLS-1$
            return;
        }

        ConflictHelpers.showConflictError(getSite().getShell(), resolution, status);

        if (ConflictResolutionStatus.SUCCESS.equals(status)
            || ConflictResolutionStatus.SUCCEEDED_WITH_CONFLICTS.equals(status)) {
            final RefreshPendingChangesCommand refreshCommand = new RefreshPendingChangesCommand(repository);
            UICommandExecutorFactory.newUIJobCommandExecutor(getSite().getShell()).execute(refreshCommand);

            queryConflicts(filters);
        } else if (ConflictResolutionStatus.CANCELLED.equals(status)) {
            resolution.getConflictDescription().clearAnalysis();
        }
    }

    @Override
    public void statusChanged(final ConflictResolution resolution, final ConflictResolutionStatus newStatus) {
        UIHelpers.runOnUIThread(false, new Runnable() {
            @Override
            public void run() {
                resolutionFinished(resolution, newStatus);
            }
        });
    }

    /**
     * Complete any resolution for multiple conflicts. (Raise errors, remove
     * from the list(s), etc.)
     *
     * TODO: this needs to fire to the conflict manager for the plugin.
     *
     * @param resolutions
     *        Conflict Resolutions that finished
     * @param statuses
     *        The ConflictResolutionStatus for each resolution
     */
    private void resolutionFinished(final ConflictResolution[] resolutions, final ConflictResolutionStatus[] statuses) {
        ConflictHelpers.showConflictErrors(getSite().getShell(), resolutions, statuses);

        final RefreshPendingChangesCommand refreshCommand = new RefreshPendingChangesCommand(repository);
        UICommandExecutorFactory.newUIJobCommandExecutor(getSite().getShell()).execute(refreshCommand);

        queryConflicts(filters);
    }

    private void setRepository(final TFSRepository repository) {
        if (this.repository != null) {
            this.repository.getVersionControlClient().getEventEngine().removeOperationStartedListener(
                coreEventListener);
            this.repository.getVersionControlClient().getEventEngine().removeOperationCompletedListener(
                coreEventListener);
            this.repository.getVersionControlClient().getEventEngine().removeUndonePendingChangeListener(
                coreEventListener);
        }

        this.repository = repository;
        initialConflicts = null;
        filters = null;

        if (hasPainted && !disposed && repository == null) {
            summaryLabel.setText(Messages.getString("ConflictResolutionEditor.SummaryOffline")); //$NON-NLS-1$
            toolbar.setEnabled(false);
            conflictTable.setConflictDescriptions(new ConflictDescription[0]);
            conflictTable.setEnabled(false);
            conflictResolutionControl.setConflictDescriptions(null);
            conflictResolutionControl.setEnabled(false);
        } else if (hasPainted && !disposed) {
            toolbar.setEnabled(true);
            conflictTable.setEnabled(true);
            conflictResolutionControl.setEnabled(true);

            queryConflicts(null);
        }

        if (this.repository != null) {
            this.repository.getVersionControlClient().getEventEngine().addOperationStartedListener(coreEventListener);
            this.repository.getVersionControlClient().getEventEngine().addOperationCompletedListener(coreEventListener);
            this.repository.getVersionControlClient().getEventEngine().addUndonePendingChangeListener(
                coreEventListener);
        }
    }

    private static class ResourceChangingConflictResolver extends ExternalConflictResolver {
        private final Shell parentShell;

        public ResourceChangingConflictResolver(final Shell parentShell) {
            Check.notNull(parentShell, "parentShell"); //$NON-NLS-1$

            this.parentShell = parentShell;
        }

        @Override
        public boolean resolveConflict(final Workspace workspace, final Conflict conflict) {
            final ConflictResolutionCommand resolveCommand = new ConflictResolutionCommand(workspace, conflict);
            final IStatus resolveStatus = UICommandExecutorFactory.newUICommandExecutor(parentShell).execute(
                new ResourceChangingCommand(resolveCommand));

            return (resolveStatus.isOK());
        }
    }

    private static class ConflictResolutionCommand extends TFSCommand {
        private final Workspace workspace;
        private final Conflict conflict;

        public ConflictResolutionCommand(final Workspace workspace, final Conflict conflict) {
            this.workspace = workspace;
            this.conflict = conflict;
        }

        @Override
        public String getName() {
            return Messages.getString("ConflictDialog.ResolveCommandText"); //$NON-NLS-1$
        }

        @Override
        public String getErrorDescription() {
            return Messages.getString("ConflictDialog.ResolveCommandErrorText"); //$NON-NLS-1$
        }

        @Override
        public String getLoggingDescription() {
            return Messages.getString("ConflictDialog.ResolveCommandText", LocaleUtil.ROOT); //$NON-NLS-1$
        }

        @Override
        protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
            workspace.resolveConflict(conflict);

            return Status.OK_STATUS;
        }
    }

    private final class ConflictResolutionEditorQueryCommand extends QueryConflictsCommand {
        public ConflictResolutionEditorQueryCommand(final ItemSpec[] filters) {
            super(repository, filters);
        }

        @Override
        protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
            final IStatus queryStatus = super.doRun(progressMonitor);

            UIHelpers.runOnUIThread(false, new Runnable() {
                @Override
                public void run() {
                    if (repository == null
                        || conflictTable == null
                        || summaryLabel == null
                        || conflictTable.isDisposed()
                        || summaryLabel.isDisposed()) {
                        return;
                    }

                    if (queryStatus.isOK()) {
                        conflictTable.setConflictDescriptions(getConflictDescriptions());
                        setEnabled(true);
                        updateSummary();
                        updateSelection();
                    } else {
                        conflictTable.setConflictDescriptions(null);
                        summaryLabel.setText(queryStatus.getMessage());
                    }
                }
            });

            return queryStatus;
        }
    }

    private class ConflictResolutionEditorRepositoryManagerListener extends RepositoryManagerAdapter {
        @Override
        public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    setRepository(event.getRepository());
                }
            });
        }
    }

    private class ConflictResolutionEditorCoreListener
        implements OperationStartedListener, OperationCompletedListener, UndonePendingChangeListener {
        private final Object lock = new Object();
        private boolean hasUndonePendingChanges = false;

        @Override
        public void onOperationStarted(final OperationStartedEvent e) {
            synchronized (lock) {
            }
        }

        @Override
        public void onOperationCompleted(final OperationCompletedEvent e) {
            boolean refresh;

            synchronized (lock) {
                refresh = hasUndonePendingChanges;
                hasUndonePendingChanges = false;
            }

            if (!refresh) {
                return;
            }

            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    queryConflicts(filters);
                }
            });
        }

        @Override
        public void onUndonePendingChange(final PendingChangeEvent e) {
            synchronized (lock) {
                hasUndonePendingChanges = true;
            }
        }
    }
}

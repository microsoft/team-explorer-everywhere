// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.compare.TFSItemContentComparator;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.conflicts.ConflictComparisonDescription;
import com.microsoft.tfs.client.common.ui.conflicts.ConflictComparisonFactory;
import com.microsoft.tfs.client.common.ui.conflicts.resolutions.contributors.EclipseMergeConflictResolutionContributor;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.framework.helper.FontHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.prefs.ExternalToolPreferenceKey;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictCategory;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescriptionFactory;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionStatus;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionStatusListener;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ExternalConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.CompositeConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ExternalConflictResolutionContributor;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.util.MementoRepository;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class ConflictResolutionControl extends BaseControl
    implements ISelectionProvider, ConflictResolutionStatusListener {
    public static final CodeMarker CODEMARKER_CONFLICTANALYSIS_COMPLETE = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.controls.vc.ConflictResolutionControl#conflictAnalysisComplete"); //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(ConflictResolutionControl.class);

    private final SingleListenerFacade selectionChangedListeners =
        new SingleListenerFacade(ISelectionChangedListener.class);

    private final SingleListenerFacade conflictResolutionSelectionListener =
        new SingleListenerFacade(ConflictResolutionSelectionListener.class);

    private final SingleListenerFacade conflictResolutionCancelledListener =
        new SingleListenerFacade(ConflictResolutionCancelledListener.class);

    private ConflictDescription[] conflictDescriptions;

    private final Composite buttonComposite;
    private Button[] resolutionButtons;
    private ConflictResolution[] conflictResolutions;

    private final Map<ConflictDescription, ConflictResolution> resolutionsInProgress =
        new HashMap<ConflictDescription, ConflictResolution>();

    private final Composite descriptionComposite;
    private final Label descriptionLabel;
    private final Font descriptionLabelFont;

    private final Label contentChangesPromptLabel;
    private final Label contentChangesLabel;

    private final int compareDescriptionCount = 2;
    private final Label[] compareDescriptionPromptLabel;
    private final Link[] compareDescriptionLabel;
    private final SelectionListener[] compareSelectionListener;

    private final CompositeConflictResolutionContributor resolutionContributor;

    private static final int versionPromptMaxLength;

    static {
        int promptLen =
            Messages.getString("ConflictResolutionControl.MultipleConflictResolutionOptionsPrompt").length(); //$NON-NLS-1$

        for (final String prompt : ConflictComparisonFactory.getAllPrompts()) {
            promptLen = Math.max(promptLen, prompt.length());
        }

        versionPromptMaxLength = promptLen + 1;
    }

    public ConflictResolutionControl(final Composite parent, final int style) {
        super(parent, style);

        resolutionContributor = new CompositeConflictResolutionContributor();
        resolutionContributor.addContributor(new EclipseMergeConflictResolutionContributor());
        resolutionContributor.addContributor(
            new ExternalConflictResolutionContributor(
                ExternalToolset.loadFromMemento(
                    new MementoRepository(
                        DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore()).load(
                            ExternalToolPreferenceKey.MERGE_KEY))));

        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing() * 2;
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        setLayout(layout);

        /* Add buttons and layout to buttonComposite on the fly. */
        buttonComposite = new Composite(this, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(buttonComposite);

        descriptionComposite = new Composite(this, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(descriptionComposite);

        final GridLayout descriptionLayout = new GridLayout(2, false);
        descriptionLayout.horizontalSpacing = getHorizontalSpacing();
        descriptionLayout.verticalSpacing = 0;
        descriptionLayout.marginWidth = 0;
        descriptionLayout.marginHeight = 0;
        descriptionComposite.setLayout(descriptionLayout);

        descriptionLabel = new Label(descriptionComposite, SWT.NONE);
        descriptionLabelFont =
            new Font(getShell().getDisplay(), FontHelper.bold(descriptionLabel.getFont().getFontData()));
        descriptionLabel.setFont(descriptionLabelFont);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).vAlignTop().applyTo(descriptionLabel);

        contentChangesPromptLabel = new Label(descriptionComposite, SWT.NONE);
        contentChangesLabel = new Label(descriptionComposite, SWT.WRAP);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(contentChangesLabel);

        compareDescriptionPromptLabel = new Label[compareDescriptionCount];
        compareDescriptionLabel = new Link[compareDescriptionCount];
        compareSelectionListener = new SelectionListener[compareDescriptionCount];

        for (int i = 0; i < compareDescriptionCount; i++) {
            compareDescriptionPromptLabel[i] = new Label(descriptionComposite, SWT.NONE);
            GridDataBuilder.newInstance().hFill().wCHint(
                compareDescriptionPromptLabel[i],
                versionPromptMaxLength).vAlignTop().applyTo(compareDescriptionPromptLabel[i]);

            compareDescriptionLabel[i] = new Link(descriptionComposite, SWT.NONE);
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(compareDescriptionLabel[i]);
        }

        setConflictDescriptions(null);

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                if (descriptionLabelFont != null) {
                    descriptionLabelFont.dispose();
                }
            }
        });
    }

    @Override
    public void setSelection(final ISelection selection) {
        Check.notNull(selection, "selection"); //$NON-NLS-1$
        Check.isTrue(selection instanceof IStructuredSelection, "selection instanceof IStructuredSelection"); //$NON-NLS-1$

        setConflictDescriptions((ConflictDescription[]) ((IStructuredSelection) selection).toArray());
    }

    @Override
    public ISelection getSelection() {
        return new StructuredSelection(conflictDescriptions);
    }

    public void setConflictDescriptions(final ConflictDescription[] conflictDescriptions) {
        this.conflictDescriptions = conflictDescriptions;

        /* Dispose existing buttons */
        for (final Control button : buttonComposite.getChildren()) {
            button.dispose();
        }
        resolutionButtons = null;

        if (conflictDescriptions == null || conflictDescriptions.length == 0) {
            configureEmptyConflictDescriptions();
        } else if (conflictDescriptions.length == 1 && resolutionsInProgress.containsKey(conflictDescriptions[0])) {
            configureRunningConflictDescription(conflictDescriptions[0]);
        } else if (conflictDescriptions.length == 1) {
            configureConflictDescription(conflictDescriptions[0]);
        } else {
            /* Determine if all the selected conflicts are of the same type. */
            final Map<ConflictCategory, List<ConflictDescription>> conflictsByCategory =
                new TreeMap<ConflictCategory, List<ConflictDescription>>();

            for (final ConflictDescription conflictDescription : conflictDescriptions) {
                final ConflictCategory category = conflictDescription.getConflictCategory();
                List<ConflictDescription> conflictsForCategory = conflictsByCategory.get(category);

                if (conflictsForCategory == null) {
                    conflictsForCategory = new ArrayList<ConflictDescription>();
                    conflictsByCategory.put(category, conflictsForCategory);
                }

                conflictsForCategory.add(conflictDescription);
            }

            if (conflictsByCategory.keySet().size() == 1) {
                configureMultipleConflicts(conflictDescriptions);
            } else {
                configureMultipleConflictCategories(conflictsByCategory);
            }
        }

        final GridLayout buttonLayout = new GridLayout(resolutionButtons.length, false);
        buttonLayout.horizontalSpacing = getHorizontalSpacing();
        buttonLayout.verticalSpacing = getVerticalSpacing();
        buttonLayout.marginWidth = 0;
        buttonLayout.marginHeight = 0;
        buttonComposite.setLayout(buttonLayout);

        buttonComposite.pack(true);
        pack(true);

        buttonComposite.layout(true);
        layout(true);
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        selectionChangedListeners.addListener(listener);
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        selectionChangedListeners.removeListener(listener);
    }

    public void addConflictResolutionSelectionListener(final ConflictResolutionSelectionListener listener) {
        conflictResolutionSelectionListener.addListener(listener);
    }

    public void removeConflictResolutionSelectionListener(final ConflictResolutionSelectionListener listener) {
        conflictResolutionSelectionListener.removeListener(listener);
    }

    public void addConflictResolutionCancelledListener(final ConflictResolutionCancelledListener listener) {
        conflictResolutionCancelledListener.addListener(listener);
    }

    public void removeConflictResolutionCancelledListener(final ConflictResolutionCancelledListener listener) {
        conflictResolutionCancelledListener.removeListener(listener);
    }

    private void configureEmptyConflictDescriptions() {
        /* Use default / dummy conflict descriptions that are disabled. */
        descriptionLabel.setText(Messages.getString("ConflictResolutionControl.DescriptionNoConflictsSelected")); //$NON-NLS-1$

        contentChangesPromptLabel.setText(""); //$NON-NLS-1$
        contentChangesLabel.setText(""); //$NON-NLS-1$

        for (int i = 0; i < compareDescriptionCount; i++) {
            compareDescriptionPromptLabel[i].setText(""); //$NON-NLS-1$
            compareDescriptionLabel[i].setText(""); //$NON-NLS-1$

            if (compareSelectionListener[i] != null) {
                compareDescriptionLabel[i].removeSelectionListener(compareSelectionListener[i]);
                compareSelectionListener[i] = null;
            }
        }

        conflictResolutions =
            ConflictDescriptionFactory.getConflictDescription(ConflictCategory.VERSION).getResolutions(
                resolutionContributor);

        final List<Button> resolutionButtonsList = new ArrayList<Button>();
        for (final ConflictResolution resolution : conflictResolutions) {
            final Button resolutionButton = new Button(buttonComposite, SWT.PUSH);
            resolutionButton.setText(resolution.getDescription());
            resolutionButton.setEnabled(false);

            resolutionButtonsList.add(resolutionButton);
        }
        resolutionButtons = resolutionButtonsList.toArray(new Button[resolutionButtonsList.size()]);
    }

    private void configureRunningConflictDescription(final ConflictDescription conflictDescription) {
        /* Use default / dummy conflict descriptions that are disabled. */
        descriptionLabel.setText(Messages.getString("ConflictResolutionControl.ConflictBeingResolved")); //$NON-NLS-1$

        contentChangesPromptLabel.setText(""); //$NON-NLS-1$
        contentChangesLabel.setText(""); //$NON-NLS-1$

        for (int i = 0; i < compareDescriptionCount; i++) {
            compareDescriptionPromptLabel[i].setText(""); //$NON-NLS-1$
            compareDescriptionLabel[i].setText(""); //$NON-NLS-1$

            if (compareSelectionListener[i] != null) {
                compareDescriptionLabel[i].removeSelectionListener(compareSelectionListener[i]);
                compareSelectionListener[i] = null;
            }
        }

        final ConflictResolution resolution = resolutionsInProgress.get(conflictDescription);

        conflictResolutions = new ConflictResolution[] {
            resolution
        };

        final Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setText(Messages.getString("ConflictResolutionControl.CancelResolutionText")); //$NON-NLS-1$
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (resolution != null) {
                    ((ConflictResolutionCancelledListener) conflictResolutionCancelledListener.getListener()).conflictResolutionCancelled(
                        conflictDescription,
                        resolution);
                }
            }
        });

        resolutionButtons = new Button[] {
            cancelButton
        };
    }

    private void configureConflictDescription(final ConflictDescription conflictDescription) {
        descriptionLabel.setText(conflictDescription.getDescription());

        contentChangesPromptLabel.setText(Messages.getString("ConflictResolutionControl.ContentChangesPrompt")); //$NON-NLS-1$

        if (conflictDescription.showChangeDescription() && conflictDescription.hasAnalyzed()) {
            contentChangesLabel.setText(conflictDescription.getChangeDescription());
        } else if (conflictDescription.showChangeDescription()) {
            contentChangesLabel.setText(Messages.getString("ConflictResolutionControl.ContentChangesAnalyzing")); //$NON-NLS-1$
        } else {
            contentChangesLabel.setText(Messages.getString("ConflictResolutionControl.NotMergeable")); //$NON-NLS-1$
        }

        ConflictComparisonDescription[] compareDescriptions;

        try {
            compareDescriptions =
                ConflictComparisonFactory.getConflictComparison(conflictDescription).getDescriptions();
        } catch (final Exception e) {
            log.warn("Could not determine conflict comparison options", e); //$NON-NLS-1$

            compareDescriptions = new ConflictComparisonDescription[0];
        }

        for (int i = 0; i < compareDescriptionCount
            && i < compareDescriptions.length
            && i < compareDescriptionCount; i++) {
            final ConflictComparisonDescription compareDescription = compareDescriptions[i];

            compareDescriptionPromptLabel[i].setText(
                MessageFormat.format(
                    Messages.getString("ConflictResolutionControl.ComparePromptFormat"), //$NON-NLS-1$
                    compareDescription.getPathDescription()));

            compareDescriptionLabel[i].setText(compareDescription.getPathAndVersionLink());

            if (compareSelectionListener[i] != null) {
                compareDescriptionLabel[i].removeSelectionListener(compareSelectionListener[i]);
            }

            compareSelectionListener[i] = new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    openComparison(
                        conflictDescription,
                        compareDescription.getOriginalNode(),
                        compareDescription.getModifiedNode());
                }
            };
            compareDescriptionLabel[i].addSelectionListener(compareSelectionListener[i]);

            compareDescriptionLabel[i].setToolTipText(compareDescription.getCompareActionDescription());
        }

        for (int i = compareDescriptions.length; i < compareDescriptionCount; i++) {
            compareDescriptionPromptLabel[i].setText(""); //$NON-NLS-1$
            compareDescriptionLabel[i].setText(""); //$NON-NLS-1$

            if (compareSelectionListener[i] != null) {
                compareDescriptionLabel[i].removeSelectionListener(compareSelectionListener[i]);
                compareSelectionListener[i] = null;
            }
        }

        conflictResolutions = conflictDescription.getResolutions(resolutionContributor);

        final List<Button> resolutionButtonsList = new ArrayList<Button>();
        for (final ConflictResolution resolution : conflictResolutions) {
            final Button resolutionButton = new Button(buttonComposite, SWT.PUSH);
            resolutionButton.setText(resolution.getDescription());
            resolutionButton.setEnabled(conflictDescription.isResolutionEnabled(resolution));
            resolutionButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    ((ConflictResolutionSelectionListener) conflictResolutionSelectionListener.getListener()).conflictResolutionSelected(
                        new ConflictDescription[] {
                            conflictDescription
                    },
                        resolution);
                }
            });
            resolutionButton.setToolTipText(resolution.getHelpText());

            resolutionButtonsList.add(resolutionButton);
        }
        resolutionButtons = resolutionButtonsList.toArray(new Button[resolutionButtonsList.size()]);

        if (conflictDescription.showChangeDescription() && !conflictDescription.hasAnalyzed()) {
            analyzeConflict(conflictDescription);
        }
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

        compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(getShell()));
        compare.open();
    }

    private void configureMultipleConflicts(final ConflictDescription[] conflictDescriptions) {
        descriptionLabel.setText(
            MessageFormat.format(
                Messages.getString("ConflictResolutionControl.DescriptionMultipleConflictsSelectedFormat"), //$NON-NLS-1$
                conflictDescriptions.length,
                conflictDescriptions[0].getName()));

        contentChangesPromptLabel.setText(
            Messages.getString("ConflictResolutionControl.MultipleConflictResolutionOptionsPrompt")); //$NON-NLS-1$
        contentChangesLabel.setText(Messages.getString("ConflictResolutionControl.MultipleConflictResolutionOptions")); //$NON-NLS-1$

        for (int i = 0; i < compareDescriptionCount; i++) {
            compareDescriptionPromptLabel[i].setText(""); //$NON-NLS-1$
            compareDescriptionLabel[i].setText(""); //$NON-NLS-1$

            if (compareSelectionListener[i] != null) {
                compareDescriptionLabel[i].removeSelectionListener(compareSelectionListener[i]);
                compareSelectionListener[i] = null;
            }
        }

        conflictResolutions = new ConflictResolution[0];

        final ConflictResolution[] resolutions = ConflictDescriptionFactory.getConflictDescription(
            conflictDescriptions[0].getConflictCategory()).getResolutions(null);

        final List<Button> resolutionButtonsList = new ArrayList<Button>();
        for (final ConflictResolution resolution : resolutions) {
            final Button resolutionButton = new Button(buttonComposite, SWT.PUSH);
            resolutionButton.setText(resolution.getDescription());
            resolutionButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    ((ConflictResolutionSelectionListener) conflictResolutionSelectionListener.getListener()).conflictResolutionSelected(
                        conflictDescriptions,
                        resolution);
                }
            });

            resolutionButtonsList.add(resolutionButton);
        }

        resolutionButtons = resolutionButtonsList.toArray(new Button[resolutionButtonsList.size()]);
    }

    private void configureMultipleConflictCategories(
        final Map<ConflictCategory, List<ConflictDescription>> conflictsByCategory) {
        final List<Button> resolutionButtonsList = new ArrayList<Button>();

        for (final Entry<ConflictCategory, List<ConflictDescription>> categoryEntry : conflictsByCategory.entrySet()) {
            final String categoryName =
                ConflictDescriptionFactory.getConflictDescription(categoryEntry.getKey()).getName();
            final ConflictDescription[] conflictsForCategory =
                categoryEntry.getValue().toArray(new ConflictDescription[categoryEntry.getValue().size()]);

            final Button categoryButton = new Button(buttonComposite, SWT.PUSH);
            categoryButton.setText(categoryName);
            categoryButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    final StructuredSelection selection = new StructuredSelection(conflictsForCategory);

                    final SelectionChangedEvent conflictSelectionEvent =
                        new SelectionChangedEvent(ConflictResolutionControl.this, selection);
                    ((ISelectionChangedListener) selectionChangedListeners.getListener()).selectionChanged(
                        conflictSelectionEvent);
                }
            });

            resolutionButtonsList.add(categoryButton);
        }
        resolutionButtons = resolutionButtonsList.toArray(new Button[resolutionButtonsList.size()]);

        descriptionLabel.setText(Messages.getString("ConflictResolutionControl.MultipleConflictCategoriesDescription")); //$NON-NLS-1$

        contentChangesPromptLabel.setText(
            Messages.getString("ConflictResolutionControl.MultipleConflictCategoriesResolutionOptionsPrompt")); //$NON-NLS-1$
        contentChangesLabel.setText(
            Messages.getString("ConflictResolutionControl.MultipleConflictCategoriesResolutionOptions")); //$NON-NLS-1$

        for (int i = 0; i < compareDescriptionCount; i++) {
            compareDescriptionPromptLabel[i].setText(""); //$NON-NLS-1$
            compareDescriptionLabel[i].setText(""); //$NON-NLS-1$

            if (compareSelectionListener[i] != null) {
                compareDescriptionLabel[i].removeSelectionListener(compareSelectionListener[i]);
                compareSelectionListener[i] = null;
            }
        }
    }

    private void analyzeConflict(final ConflictDescription conflictDescription) {
        final Shell shell = getShell();

        final Thread analyzerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // if analyzeConflict() returns false, there's nothing to do
                if (!conflictDescription.analyzeConflict()) {
                    CodeMarkerDispatch.dispatch(CODEMARKER_CONFLICTANALYSIS_COMPLETE);
                    return;
                }

                if (shell == null || shell.isDisposed()) {
                    return;
                }

                // run on the ui thread to avoid contention
                UIHelpers.runOnUIThread(shell, false, new Runnable() {
                    @Override
                    public void run() {
                        if (shell.isDisposed() || contentChangesLabel.isDisposed()) {
                            return;
                        }

                        // make sure we've still got the original conflict
                        // description selected
                        if (conflictDescriptions == null
                            || conflictDescriptions.length != 1
                            || conflictDescriptions[0] != conflictDescription) {
                            return;
                        }

                        contentChangesLabel.setText(conflictDescription.getChangeDescription());

                        // disable the automerge button if there were content
                        // conflicts
                        for (int i = 0; i < conflictResolutions.length; i++) {
                            if (conflictDescription.isResolutionEnabled(conflictResolutions[i]) == false) {
                                resolutionButtons[i].setEnabled(false);
                            }
                        }

                        // check resolution options again
                        CodeMarkerDispatch.dispatch(CODEMARKER_CONFLICTANALYSIS_COMPLETE);
                    }
                });
            }
        });

        analyzerThread.start();
    }

    @Override
    public void statusChanged(final ConflictResolution resolution, final ConflictResolutionStatus newStatus) {
        UIHelpers.asyncExec(new Runnable() {
            @Override
            public void run() {
                if (ConflictResolutionStatus.RUNNING.equals(newStatus)) {
                    resolutionsInProgress.put(resolution.getConflictDescription(), resolution);
                } else {
                    resolution.removeStatusListener(ConflictResolutionControl.this);
                    resolutionsInProgress.remove(resolution.getConflictDescription());
                }

                if (resolution instanceof ExternalConflictResolution
                    && conflictDescriptions.length == 1
                    && conflictDescriptions[0].equals(resolution.getConflictDescription())) {
                    /*
                     * set the same conflict descriptions back in just to cause
                     * a ui refresh
                     */
                    setConflictDescriptions(conflictDescriptions);
                }
            }
        });
    }

    public boolean isResolving(final ConflictDescription conflictDescription) {
        return resolutionsInProgress.containsKey(conflictDescription);
    }

    public interface ConflictResolutionSelectionListener {
        public void conflictResolutionSelected(
            ConflictDescription[] conflictDescriptions,
            ConflictResolution resolution);
    }

    public interface ConflictResolutionCancelledListener {
        public void conflictResolutionCancelled(ConflictDescription conflictDescription, ConflictResolution resolution);
    }
}

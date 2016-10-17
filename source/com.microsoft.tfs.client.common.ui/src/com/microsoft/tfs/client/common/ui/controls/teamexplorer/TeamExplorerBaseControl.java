// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.teamexplorer;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerResizeEventArg;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerPageConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerSectionConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.pages.ITeamExplorerPage;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.ITeamExplorerSection;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerSectionRegenerateListener;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.teamsettings.TeamConfiguration;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;
import com.microsoft.tfs.util.tasks.CanceledException;

/**
 * Base class for @TeamExplorerControl and
 *
 * @TeamExplorerDockableControl for code reuse.
 */
public abstract class TeamExplorerBaseControl extends BaseControl {

    private static final Log log = LogFactory.getLog(TeamExplorerBaseControl.class);
    protected final TeamExplorerConfig configuration;
    protected final TeamExplorerContext context;
    protected final FormToolkit toolkit;
    protected final Form form;
    protected final ScrolledForm subForm;
    protected Composite pageComposite;

    // Property name for the Job property bag. Value is a
    // TeamExplorerNavigationItemConfig
    protected static final QualifiedName NAVITEM_CONFIG_DATA_NAME = new QualifiedName(null, "NavigationItemConfig"); //$NON-NLS-1$

    // Property name for the Job property bag. Value is an ITeamExplorerSection.
    protected static final QualifiedName SECTION_INSTANCE_PROPERY_NAME = new QualifiedName(null, "SectionInstance"); //$NON-NLS-1$

    // Property name for the Job property bag. Value is a FormToolkit section.
    protected static final QualifiedName SECTION_CONTROL_PROPERTY_NAME = new QualifiedName(null, "SectionControl"); //$NON-NLS-1$

    protected static final String SECTION_CONFIG_DATA_NAME =
        "com.microsoft.tfs.client.common.ui.controls.teamexplorer.sectionConfig"; //$NON-NLS-1$

    protected static final String SECTION_INSTANCE_DATA_NAME =
        "com.microsoft.tfs.client.common.ui.controls.teamexplorer.sectionInstance"; //$NON-NLS-1$

    protected final Map<String, Section> sectionMap = new HashMap<String, Section>();

    protected final SectionRegenerateListener sectionRegenerateListener = new SectionRegenerateListener();
    protected ITeamExplorerPage currentPage = null;

    // The map maintains all the TeamExplorer Sections, keeps the connection
    // between pages and sections
    protected final Map<String, ITeamExplorerSection> teamExplorerSectionMap =
        new HashMap<String, ITeamExplorerSection>();

    // The map maintains all the states of pages/sections, the key is the
    // page/section ID, the value is the real state object
    protected final Map<String, Object> stateMap = new HashMap<String, Object>();

    // The map maintains the expanded state of all the Team Explorer sections
    protected final Map<String, Boolean> sectionExpandStateMap = new HashMap<String, Boolean>();

    public TeamExplorerBaseControl(
        final TeamExplorerConfig configuration,
        final TeamExplorerContext context,
        final Composite parent,
        final int style) {
        super(parent, style);
        this.configuration = configuration;
        this.context = context;

        setLayout(new FillLayout());

        toolkit = new FormToolkit(this.getDisplay());
        form = toolkit.createForm(this);
        toolkit.decorateFormHeading(form);

        form.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                handleHeadResize();
            }
        });

        final GridLayout gridLayout = new GridLayout();
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        form.getBody().setLayout(gridLayout);

        subForm = toolkit.createScrolledForm(form.getBody());
        subForm.getBody().setLayout(gridLayout);
        GridDataBuilder.newInstance().grab().fill().applyTo(subForm);
        subForm.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                handleBodyResize();
            }
        });
    }

    @Override
    public boolean setFocus() {
        form.setFocus();
        return true;
    }

    protected void handleHeadResize() {
        form.layout(true, true);
    }

    protected void handleBodyResize() {
        int availableWidth = subForm.getSize().x;
        final ScrollBar verticalScrollBar = subForm.getVerticalBar();
        if (verticalScrollBar != null && verticalScrollBar.isVisible()) {
            availableWidth -= verticalScrollBar.getSize().x;
        }

        final TeamExplorerResizeEventArg arg = new TeamExplorerResizeEventArg(availableWidth);
        context.getEvents().notifyListener(TeamExplorerEvents.FORM_RESIZED, arg);
        subForm.layout(true, true);
        subForm.reflow(true);
    }

    protected void createPageContent(
        final Composite parent,
        final TeamExplorerPageConfig page,
        final TeamExplorerSectionConfig[] sections) {
        final Composite composite = createPageHeaderContent(parent, page);

        if (composite != null && !composite.isDisposed()) {
            GridDataBuilder.newInstance().hFill().hGrab().applyTo(composite);
        }

        if (!context.isConnectedToCollection()) {
            return;
        }

        if (sections != null) {
            for (int i = 0; i < sections.length; i++) {
                final TeamExplorerSectionConfig section = sections[i];

                Composite sectionComposite;

                try {
                    final ITeamExplorerSection sectionInstance = section.createInstance();
                    sectionInstance.addSectionRegenerateListener(sectionRegenerateListener);

                    final Section ec = createSectionContent(parent, section, sectionInstance);
                    ec.setText(sectionInstance.getTitle());

                    sectionMap.put(sectionInstance.getID(), ec);
                    teamExplorerSectionMap.put(sectionInstance.getID(), sectionInstance);
                    sectionComposite = ec;
                } catch (final Throwable t) {
                    sectionComposite = createExceptionComposite(parent, section.getTitle(), t);
                }

                final GridData gridData = new GridData();
                gridData.horizontalAlignment = SWT.FILL;
                gridData.grabExcessHorizontalSpace = true;

                if (i > 0) {
                    gridData.verticalIndent = 0;
                }

                if (i == sections.length - 1) {
                    gridData.verticalAlignment = SWT.FILL;
                    gridData.grabExcessVerticalSpace = true;
                }

                if (sectionComposite.getVisible() == false) {
                    gridData.exclude = true;
                }

                sectionComposite.setLayoutData(gridData);
            }
        }
    }

    protected Composite createPageHeaderContent(final Composite parent, final TeamExplorerPageConfig page) {
        Composite composite;
        try {
            final ITeamExplorerPage pageInstance = page.createInstance();
            pageInstance.initialize(null, context, stateMap.get(page.getID()));
            currentPage = pageInstance;
            composite = pageInstance.getPageContent(toolkit, parent, SWT.NONE, context);
        } catch (final Exception e) {
            composite = createExceptionComposite(parent, page.getTitle(), e);
        }

        return composite;
    }

    protected Section createSectionContent(
        final Composite parent,
        final TeamExplorerSectionConfig sectionConfig,
        final ITeamExplorerSection sectionInstance) {
        final Section ec =
            toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);

        Composite sectionComposite;
        boolean shouldExpand = true;
        try {
            if (sectionExpandStateMap.get(sectionInstance.getID()) != null) {
                shouldExpand = sectionExpandStateMap.get(sectionInstance.getID());
            } else {
                shouldExpand = true;
            }

            if (sectionInstance.initializeInBackground(context)) {
                createBackgroundSectionLoadJob(ec, sectionConfig, sectionInstance);
                sectionComposite = createLoadingComposite(ec);
            } else {
                sectionInstance.initialize(null, context, stateMap.get(sectionInstance.getID()));
                sectionComposite = sectionInstance.getSectionContent(toolkit, ec, SWT.NONE, context);
            }
        } catch (final CanceledException e) {
            sectionComposite = createCancelledComposite(ec);
        } catch (final TransportRequestHandlerCanceledException e) {
            sectionComposite = createCancelledComposite(ec);
        } catch (final Exception e) {
            sectionComposite = createExceptionComposite(ec, sectionInstance.getTitle(), e);
        }

        if (sectionComposite != null) {
            ec.setClient(sectionComposite);
            ec.setExpanded(shouldExpand);
        }

        ec.addExpansionListener(new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(final ExpansionEvent e) {
                form.layout();
            }
        });

        if (!sectionInstance.isVisible(context)) {
            ec.setVisible(false);
        }

        ec.setData(SECTION_CONFIG_DATA_NAME, sectionConfig);
        ec.setData(SECTION_INSTANCE_DATA_NAME, sectionInstance);
        return ec;
    }

    private void createBackgroundSectionLoadJob(
        final Section section,
        final TeamExplorerSectionConfig config,
        final ITeamExplorerSection instance) {
        // Use the general title from the config, not the content-specific one
        // from the instance
        final SectionLoadJob job = new SectionLoadJob(config.getTitle(), instance);
        job.setProperty(SECTION_CONTROL_PROPERTY_NAME, section);
        job.setProperty(SECTION_INSTANCE_PROPERY_NAME, instance);
        job.addJobChangeListener(new SectionLoadJobChangeListener());
        job.schedule();
    }

    protected Composite createLoadingComposite(final Composite parent) {
        final Composite composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite);

        final Label textLabel =
            toolkit.createLabel(composite, Messages.getString("TeamExplorerBaseControl.LoadingLabelText"), SWT.WRAP); //$NON-NLS-1$
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(textLabel);

        return composite;
    }

    protected Composite createCancelledComposite(final Composite parent) {
        final Composite composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite);

        final String message = Messages.getString("TeamExplorerBaseSection.Cancelled"); //$NON-NLS-1$
        final Label label = toolkit.createLabel(composite, message);
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(label);

        return composite;
    }

    protected Composite createExceptionComposite(final Composite parent, final String itemName, final Throwable e) {
        if (context == null || !context.isConnected()) {
            return createOfflineComposite(parent);
        }

        log.warn("Caught exception creating TE UI component", e); //$NON-NLS-1$

        // Make sure to use a non-disposed parent composite
        if (parent == null || parent.isDisposed()) {
            return parent;
        }

        final Composite composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite);

        final Composite border = toolkit.createComposite(composite, SWT.BORDER);
        SWTUtil.gridLayout(border, 2);
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(border);

        final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
        final Image image = imageHelper.getImage("/images/common/team_explorer_exception.gif"); //$NON-NLS-1$
        final Label imageLabel = toolkit.createLabel(border, "", SWT.NONE); //$NON-NLS-1$
        imageLabel.setImage(image);
        GridDataBuilder.newInstance().applyTo(imageLabel);

        final String format = Messages.getString("TeamExplorerControl.ExceptionCreatingContributionFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(
            format,
            itemName,
            e.getLocalizedMessage() != null ? e.getLocalizedMessage()
                : Messages.getString("TeamExplorerControl.NoMessageAvailable")); //$NON-NLS-1$

        final Label textLabel = toolkit.createLabel(border, message, SWT.WRAP);
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(textLabel);

        return composite;
    }

    private Composite createOfflineComposite(final Composite parent) {
        // Create the container composite.
        final Composite composite = toolkit.createComposite(parent);
        SWTUtil.gridLayout(composite, 1, true, 0, 5);
        final Label label =
            toolkit.createLabel(composite, Messages.getString("TeamExplorerReportsSection.DisconnectedLabel"));//$NON-NLS-1$
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(label);
        return composite;
    }

    protected void regenerateSection(final String sectionID) {
        final Section section = sectionMap.get(sectionID);
        boolean expandedState = true;
        if (section != null) {
            expandedState = section.isExpanded();
        }

        sectionMap.remove(sectionID);

        final Composite parentComposite = getParentComposite();
        final Control[] children = parentComposite.getChildren();

        for (int i = 0; i < children.length; i++) {
            if (children[i] == section) {
                final TeamExplorerSectionConfig sectionConfig =
                    (TeamExplorerSectionConfig) section.getData(SECTION_CONFIG_DATA_NAME);

                final ITeamExplorerSection sectionInstance =
                    (ITeamExplorerSection) section.getData(SECTION_INSTANCE_DATA_NAME);

                section.dispose();

                final Section newSection = createSectionContent(parentComposite, sectionConfig, sectionInstance);

                final GridData gridData = new GridData();
                gridData.horizontalAlignment = SWT.FILL;
                gridData.grabExcessHorizontalSpace = true;

                if (i > 0) {
                    gridData.verticalIndent = 0;
                }

                if (i == children.length - 1) {
                    gridData.verticalAlignment = SWT.FILL;
                    gridData.grabExcessVerticalSpace = true;
                }

                newSection.setLayoutData(gridData);
                newSection.setExpanded(expandedState);
                sectionMap.put(sectionID, newSection);

                newSection.setText(sectionInstance.getTitle());
                newSection.moveBelow(children[Math.max(0, i - 1)]);
                return;
            }
        }
    }

    /**
     * Get the parent composite to put control on
     *
     * @return
     */
    protected Composite getParentComposite() {
        return pageComposite;
    }

    /**
     * @return the text that describes the current project and team, shown in
     *         the {@link Form}'s message area
     */
    protected String getProjectAndTeamText() {
        if (context.isConnectedToCollection()) {
            final ProjectInfo project = context.getCurrentProjectInfo();
            if (project != null) {
                final TeamConfiguration team = context.getCurrentTeam();
                if (team != null && !team.isDefaultTeam()) {
                    return MessageFormat.format(
                        Messages.getString("TeamExplorerControl.TeamExplorerProjectAndTeamFormat"), //$NON-NLS-1$
                        project.getName(),
                        team.getTeamName());
                }

                return project.getName();
            } else {
                return null;
            }
        } else {
            return Messages.getString("TeamExplorerView.Disconnected"); //$NON-NLS-1$ ;
        }
    }

    public void refreshView() {
    }

    private class SectionLoadJob extends Job {
        private final ITeamExplorerSection sectionInstance;

        public SectionLoadJob(final String title, final ITeamExplorerSection sectionInstance) {
            super(MessageFormat.format(Messages.getString("TeamExplorerControl.SectionLoadJobFormat"), title)); //$NON-NLS-1$
            this.sectionInstance = sectionInstance;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            try {
                sectionInstance.initialize(monitor, context, stateMap.get(sectionInstance.getID()));
            } catch (final CanceledException e) {
                return Status.CANCEL_STATUS;
            } catch (final TransportRequestHandlerCanceledException e) {
                return Status.CANCEL_STATUS;
            } catch (final Exception e) {
                log.error("Failed to initialize Team Explorer section", e); //$NON-NLS-1$

                // Return a status of WARNING so that the progress monitor won't
                // display a modal dialog for an ERROR status. We'll handle the
                // exception in the job adapter's 'done' method.
                return new Status(Status.WARNING, TFSCommonClientPlugin.PLUGIN_ID, null, e);
            }

            return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
        }
    }

    private class SectionRegenerateListener implements TeamExplorerSectionRegenerateListener {
        @Override
        public void onSectionRegenerate(final String sectionID) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    regenerateSection(sectionID);
                }
            });
        }
    }

    private class SectionLoadJobChangeListener extends JobChangeAdapter {
        @Override
        public void done(final IJobChangeEvent event) {
            final ITeamExplorerSection sectionInstance;
            final Section ec;

            sectionInstance = (ITeamExplorerSection) event.getJob().getProperty(SECTION_INSTANCE_PROPERY_NAME);
            ec = (Section) event.getJob().getProperty(SECTION_CONTROL_PROPERTY_NAME);

            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    if (ec != null && !ec.isDisposed()) {
                        // Dispose of the place holder UI.
                        ec.getClient().dispose();

                        // Create the final UI.
                        final Composite content;
                        if (event.getResult() == Status.OK_STATUS) {
                            content = sectionInstance.getSectionContent(toolkit, ec, SWT.NONE, context);
                        } else if (event.getResult().getException() != null) {
                            content = createExceptionComposite(
                                ec,
                                sectionInstance.getTitle(),
                                event.getResult().getException());
                        } else {
                            content = createCancelledComposite(ec);
                        }

                        // Display the final UI.
                        ec.setText(sectionInstance.getTitle());
                        ec.setClient(content);

                        if (sectionExpandStateMap.get(sectionInstance.getID()) != null) {
                            ec.setExpanded(sectionExpandStateMap.get(sectionInstance.getID()));
                        } else {
                            ec.setExpanded(true);
                        }

                        // Simulate a resize to get the GridData wHints set
                        // properly after initial layout.
                        //
                        // HACK: We only need to do this if there are listeners
                        // for this section. We always have a listener for the
                        // search control and only within sections in the
                        // pending changes page. Thus, all pages other than
                        // pending changes page will have only one listener so
                        // we don't have to simulate a resize for this section.
                        // This could be improved but is being included as a
                        // quick and dirty optimization for Dev12 RTM.
                        if (context.getEvents().getListenerCount(TeamExplorerEvents.FORM_RESIZED) > 1) {
                            handleBodyResize();
                        }
                    }
                }
            });
        }
    }
}

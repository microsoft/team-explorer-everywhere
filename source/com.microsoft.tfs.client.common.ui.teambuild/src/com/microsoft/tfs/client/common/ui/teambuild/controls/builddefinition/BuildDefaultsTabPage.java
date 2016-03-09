// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildHelper;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.ServerCapabilities;
import com.microsoft.tfs.core.clients.build.BuildSourceProviders;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.StringUtil;

public class BuildDefaultsTabPage extends BuildDefinitionTabPage {

    private BuildDefaultsControl control;

    public BuildDefaultsTabPage(final IBuildDefinition buildDefinition) {
        super(buildDefinition);
    }

    @Override
    public Control createControl(final Composite parent) {
        control = new BuildDefaultsControl(
            parent,
            SWT.NONE,
            getBuildDefinition().getBuildServer(),
            getBuildDefinition().getTeamProject(),
            BuildSourceProviders.isTfVersionControl(getBuildDefinition().getDefaultSourceProvider()),
            getBuildDefinition().getBuildServer().getConnection().getServerCapabilities().contains(
                ServerCapabilities.HOSTED));

        populate();
        return control;
    }

    private void populate() {
        if (getBuildDefinition().getBuildController() != null) {
            control.setSelectedBuildController(getBuildDefinition().getBuildController());
        } else if (control.getControllers().length > 0) {
            control.setSelectedBuildController(control.getControllers()[0]);
        }

        control.setDropLocation(getBuildDefinition().getDefaultDropLocation());
    }

    @Override
    public String getName() {
        return Messages.getString("BuildDefaultsTabPage.PigeTitle"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        return control.isValid();
    }

    public class BuildDefaultsControl extends BaseControl {
        private final String teamProject;
        private final IBuildServer buildServer;
        private final boolean isTFVC;
        private final boolean isHosted;

        private Combo controllerCombo;
        private IBuildController[] controllers;
        private Text descriptionText;
        private Text dropToDiskText;
        private Text dropToTfVcText;
        private Button noDropButton;
        private Button dropToDiskButton;
        private Button dropToTfVcButton;
        private Button dropToFileContainerButton;

        public BuildDefaultsControl(
            final Composite parent,
            final int style,
            final IBuildServer buildServer,
            final String teamProject,
            final boolean isTFVC,
            final boolean isHosted) {
            super(parent, style);
            this.buildServer = buildServer;
            this.teamProject = teamProject;
            this.isTFVC = isTFVC;
            this.isHosted = isHosted;

            createControls(this);
        }

        private void createControls(final Composite composite) {
            final GridLayout layout = SWTUtil.gridLayout(composite, 2);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.horizontalSpacing = getHorizontalSpacing();
            layout.verticalSpacing = getVerticalSpacing();

            final String message = buildServer.getBuildServerVersion().isV3OrGreater()
                ? Messages.getString("BuildDefaultsTabPage.SpecifyBuildControllerSummaryText") //$NON-NLS-1$
                : Messages.getString("BuildDefaultsTabPage.SpecifyBuildAgentSummaryText"); //$NON-NLS-1$

            final Label summary = SWTUtil.createLabel(composite, SWT.WRAP, message);
            GridDataBuilder.newInstance().hSpan(layout).fill().hGrab().applyTo(summary);
            ControlSize.setCharWidthHint(summary, 42);

            final Label agentLabel = SWTUtil.createLabel(
                composite,
                buildServer.getBuildServerVersion().isV3OrGreater()
                    ? Messages.getString("BuildDefaultsTabPage.BuildControllerLabelText") //$NON-NLS-1$
                    : Messages.getString("BuildDefaultsTabPage.BuildAgentLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hSpan(layout).fill().applyTo(agentLabel);

            controllerCombo = new Combo(composite, SWT.READ_ONLY);
            GridDataBuilder.newInstance().hSpan(layout).fill().hGrab().applyTo(controllerCombo);
            controllerCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    updateControllerDescription();
                }
            });

            final Label descLabel =
                SWTUtil.createLabel(composite, Messages.getString("BuildDefaultsTabPage.DescriptionLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hSpan(layout).fill().applyTo(descLabel);

            descriptionText = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
            GridDataBuilder.newInstance().hSpan(layout).fill().applyTo(descriptionText);
            ControlSize.setCharHeightHint(descriptionText, 3);
            descriptionText.setEnabled(false);

            final Label stagingLabel =
                SWTUtil.createLabel(composite, Messages.getString("BuildDefaultsTabPage.StagingLocationLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hSpan(layout).fill().applyTo(stagingLabel);

            noDropButton =
                SWTUtil.createButton(composite, SWT.RADIO, Messages.getString("BuildDefaultsTabPage.NoDropButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hSpan(layout).fill().applyTo(noDropButton);
            noDropButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    dropToDiskText.setEnabled(false);
                    if (dropToTfVcText != null) {
                        dropToTfVcText.setEnabled(false);
                    }
                }
            });

            dropToDiskButton = SWTUtil.createButton(
                composite,
                SWT.RADIO,
                Messages.getString("BuildDefaultsTabPage.DropToDiskButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hSpan(layout).fill().applyTo(dropToDiskButton);
            dropToDiskButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    dropToDiskText.setEnabled(true);
                    if (dropToTfVcText != null) {
                        dropToTfVcText.setEnabled(false);
                    }
                }
            });
            dropToDiskText = new Text(composite, SWT.BORDER);
            GridDataBuilder.newInstance().hGrab().hIndent(15).fill().applyTo(dropToDiskText);

            if (isTFVC && isHosted) {
                dropToTfVcButton = SWTUtil.createButton(
                    composite,
                    SWT.RADIO,
                    Messages.getString("BuildDefaultsTabPage.DropToTfVcButtonText")); //$NON-NLS-1$
                GridDataBuilder.newInstance().hSpan(layout).fill().applyTo(dropToTfVcButton);
                dropToTfVcButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        dropToTfVcText.setEnabled(true);
                        dropToDiskText.setEnabled(false);
                    }
                });
                dropToTfVcText = new Text(composite, SWT.BORDER);
                GridDataBuilder.newInstance().hGrab().hIndent(15).fill().applyTo(dropToTfVcText);
            }

            dropToFileContainerButton = SWTUtil.createButton(
                composite,
                SWT.RADIO,
                Messages.getString("BuildDefaultsTabPage.DropToFileContainerButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hSpan(layout).fill().applyTo(dropToFileContainerButton);
            dropToFileContainerButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    dropToDiskText.setEnabled(false);
                    if (dropToTfVcText != null) {
                        dropToTfVcText.setEnabled(false);
                    }
                }
            });

            updateBuildControllers();
        }

        protected void updateControllerDescription() {
            final int index = controllerCombo.getSelectionIndex();
            if (index >= 0 && index < controllers.length) {
                final String description =
                    controllers[index].getDescription() != null ? controllers[index].getDescription() : ""; //$NON-NLS-1$
                descriptionText.setText(description);
            }
        }

        private void updateBuildControllers() {
            controllers = TeamBuildCache.getInstance(buildServer, teamProject).getBuildControllers(false);
            final String selectedController = controllerCombo.getText();
            controllerCombo.removeAll();
            for (int i = 0; i < controllers.length; i++) {
                controllerCombo.add(TeamBuildHelper.getControllerDisplayString(buildServer, controllers[i]));
            }
            controllerCombo.setText(selectedController);
        }

        public Combo getControllerCombo() {
            return controllerCombo;
        }

        public IBuildController[] getControllers() {
            return controllers;
        }

        public Text getDescriptionText() {
            return descriptionText;
        }

        public void setSelectedBuildController(final IBuildController controller) {
            for (int i = 0; i < controllers.length; i++) {
                if (controllers[i].equals(controller)) {
                    controllerCombo.select(i);

                    final String description =
                        controllers[i].getDescription() != null ? controllers[i].getDescription() : ""; //$NON-NLS-1$
                    descriptionText.setText(description);

                    break;
                }
            }
        }

        public IBuildController getSelectedBuildController() {
            final int selection = controllerCombo.getSelectionIndex();

            if (selection >= 0 && selection < controllers.length) {
                return controllers[selection];
            }

            return null;
        }

        public void setDropLocation(final String dropLocation) {
            if (dropLocation == null) {
                if (getBuildDefinition().getBuildServer().getBuildServerVersion().isV4OrGreater()) {
                    dropToFileContainerButton.setSelection(true);
                } else {
                    noDropButton.setSelection(true);
                    dropToFileContainerButton.setEnabled(false);
                }

                dropToDiskText.setEnabled(false);
                if (dropToTfVcText != null) {
                    dropToTfVcText.setEnabled(false);
                }

                return;
            }

            dropToFileContainerButton.setEnabled(
                getBuildDefinition().getBuildServer().getBuildServerVersion().isV4OrGreater());

            if (dropLocation.equals(BuildHelpers.DROP_TO_FILE_CONTAINER_LOCATION)
                && dropToFileContainerButton.isEnabled()) {
                dropToFileContainerButton.setSelection(true);

                dropToDiskText.setEnabled(false);
                if (dropToTfVcText != null) {
                    dropToTfVcText.setEnabled(false);
                }
            } else if (dropLocation.startsWith(ServerPath.ROOT) && dropToTfVcButton != null) {
                dropToTfVcButton.setSelection(true);
                dropToTfVcText.setEnabled(true);
                dropToTfVcText.setText(dropLocation);

                dropToDiskText.setEnabled(false);
            } else if (dropLocation.startsWith(BuildHelpers.UNC_LOCATION_PREFIX)) {
                dropToDiskButton.setSelection(true);
                dropToDiskText.setEnabled(true);
                dropToDiskText.setText(dropLocation);

                if (dropToTfVcText != null) {
                    dropToTfVcText.setEnabled(false);
                }
            } else {
                noDropButton.setSelection(true);

                dropToDiskText.setEnabled(false);
                if (dropToTfVcText != null) {
                    dropToTfVcText.setEnabled(false);
                }
            }
        }

        public String getDropLocation() {
            if (dropToFileContainerButton.getSelection()) {
                return BuildHelpers.DROP_TO_FILE_CONTAINER_LOCATION;
            } else if (dropToDiskButton.getSelection()) {
                if (dropToDiskText.getText() == null) {
                    return StringUtil.EMPTY;
                } else {
                    return dropToDiskText.getText().trim();
                }
            } else if (dropToTfVcButton != null && dropToTfVcButton.getSelection()) {
                if (dropToTfVcText.getText() == null) {
                    return StringUtil.EMPTY;
                } else {
                    return dropToTfVcText.getText().trim();
                }
            } else {
                return StringUtil.EMPTY;
            }
        }

        @Override
        public void addFocusListener(final FocusListener listener) {
            dropToDiskText.addFocusListener(listener);
            if (dropToTfVcText != null) {
                dropToTfVcText.addFocusListener(listener);
            }
            controllerCombo.addFocusListener(listener);
        }

        public void addSelectionListener(final SelectionListener listener) {
            noDropButton.addSelectionListener(listener);
            dropToDiskButton.addSelectionListener(listener);
            if (dropToTfVcButton != null) {
                dropToTfVcButton.addSelectionListener(listener);
            }
            dropToFileContainerButton.addSelectionListener(listener);
        }

        public boolean isValid() {
            if (getSelectedBuildController() == null) {
                return false;
            }

            if (dropToFileContainerButton.getSelection() || noDropButton.getSelection()) {
                return true;
            }

            if (dropToDiskButton.getSelection()) {
                return getDropLocation().startsWith(BuildHelpers.UNC_LOCATION_PREFIX);
            } else if (dropToTfVcButton != null && dropToTfVcButton.getSelection()) {
                return getDropLocation().startsWith(ServerPath.ROOT);
            }

            return false;
        }
    }

    public BuildDefaultsControl getControl() {
        return control;
    }
}

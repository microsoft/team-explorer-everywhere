// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IProcessTemplate;
import com.microsoft.tfs.core.clients.build.IRetentionPolicy;
import com.microsoft.tfs.core.clients.build.ISchedule;
import com.microsoft.tfs.core.clients.build.IWorkspaceTemplate;
import com.microsoft.tfs.core.clients.build.exceptions.ConfigurationFolderPathNotFoundException;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.DefinitionQueueStatus;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildValidation;
import com.microsoft.tfs.core.clients.build.internal.utils.XamlHelper;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.core.clients.linking.LinkingClient;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

import ms.tfs.build.buildservice._04._BuildDefinition;
import ms.tfs.build.buildservice._04._BuildDefinitionSourceProvider;
import ms.tfs.build.buildservice._04._ProcessTemplate;
import ms.tfs.build.buildservice._04._PropertyValue;
import ms.tfs.build.buildservice._04._RetentionPolicy;
import ms.tfs.build.buildservice._04._Schedule;
import ms.tfs.services.linking._03._Artifact;
import ms.tfs.services.linking._03._ExtendedAttribute;

public class BuildDefinition extends WebServiceObjectWrapper implements IBuildDefinition {
    private IBuildController buildController;
    private IBuildServer buildServer;
    private String id;
    private String name;
    private String teamProject;
    private AttachedPropertyDictionary attachedProperties;
    private List<IRetentionPolicy> retentionPolicyList;
    private List<ISchedule> scheduleList;
    private List<BuildDefinitionSourceProvider> sourceProviders;
    private String configurationFolderPath;

    // Present for compatibility only -- do not use
    private String configurationFolderURI;
    private static final String CONFIGURATION_FOLDER_PATH = "ConfigurationFolderPath"; //$NON-NLS-1$

    public BuildDefinition(final IBuildServer buildServer, final _BuildDefinition webServiceObject) {
        super(webServiceObject);

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        this.buildServer = buildServer;

        afterDeserialize();
    }

    /**
     * Creates a new build definition owned by the given build server and team
     * project.
     *
     *
     * @param buildServer
     *        The server that owns this build definition.
     * @param teamProject
     *        The team project that owns this build definition.
     */
    public BuildDefinition(final IBuildServer buildServer, final String teamProject) {
        super(new _BuildDefinition());

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNull(teamProject, "teamProject"); //$NON-NLS-1$

        this.buildServer = buildServer;
        this.teamProject = teamProject;

        // Default queue status is enabled when creating on the client.
        getWebServiceObject().setQueueStatus(DefinitionQueueStatus.ENABLED.getWebServiceObject());

        // Save properties for comparing later
        this.attachedProperties = new AttachedPropertyDictionary();
    }

    public BuildDefinition(final IBuildServer buildServer, final BuildDefinition2010 definition) {
        super(new _BuildDefinition());

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNull(definition, "definition"); //$NON-NLS-1$
        this.buildServer = buildServer;

        final _BuildDefinition _o = getWebServiceObject();

        setBatchSize(1);
        if (definition.getBuildControllerURI() == null) {
            setBuildControllerURI(definition.getDefaultBuildAgentURI());
        } else {
            setBuildControllerURI(definition.getBuildControllerURI());
        }
        setContinuousIntegrationQuietPeriod(definition.getContinuousIntegrationQuietPeriod());
        setTriggerType(TFS2010Helper.convert(definition.getContinuousIntegrationType()));
        setDefaultDropLocation(definition.getDefaultDropLocation());
        setDescription(definition.getDescription());
        if (definition.isEnabled()) {
            setQueueStatus(DefinitionQueueStatus.ENABLED);
        } else {
            setQueueStatus(DefinitionQueueStatus.DISABLED);
        }
        setFullPath(definition.getWebServiceObject().getFullPath());
        _o.setLastBuildUri(definition.getLastBuildURI());
        _o.setLastGoodBuildLabel(definition.getLastGoodBuildLabel());
        _o.setLastGoodBuildUri(definition.getLastGoodBuildURI());
        setProcess(TFS2010Helper.convert(buildServer, definition.getProcess()));
        setProcessParameters(definition.getProcessParameters());
        setRetentionPolicies(TFS2010Helper.convert(this, definition.getRetentionPolicies()));
        setSchedules(TFS2010Helper.convert(this, definition.getSchedules()));
        teamProject = definition.getTeamProject();
        setURI(definition.getURI());
        setWorkspace(TFS2010Helper.convert(definition.getWorkspaceTemplate()));

        if (buildServer.getBuildServerVersion().isV2()) {
            configurationFolderURI = definition.getConfigurationFolderURI();
        }

        afterDeserialize();
    }

    private void afterDeserialize() {
        // Set full path to split into project and name.
        setFullPath(getWebServiceObject().getFullPath());

        // Put all retention policies into the list.
        final IRetentionPolicy[] policies =
            (IRetentionPolicy[]) WrapperUtils.wrap(RetentionPolicy.class, getWebServiceObject().getRetentionPolicies());
        retentionPolicyList = new ArrayList<IRetentionPolicy>();
        if (policies != null) {
            for (final IRetentionPolicy policy : policies) {
                retentionPolicyList.add(policy);
            }
        }

        // Get the properties
        final PropertyValue[] properties =
            (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getProperties());
        attachedProperties =
            properties == null ? new AttachedPropertyDictionary() : new AttachedPropertyDictionary(properties);
    }

    public _BuildDefinition getWebServiceObject() {
        return (_BuildDefinition) this.webServiceObject;
    }

    /**
     * Gets or sets the maximum batch size when using the
     * <see cref="DefinitionTriggerType.GatedCheckIn" /> trigger. {@inheritDoc}
     */
    @Override
    public int getBatchSize() {
        return getWebServiceObject().getBatchSize();
    }

    @Override
    public void setBatchSize(final int value) {
        getWebServiceObject().setBatchSize(value);
    }

    /**
     * Gets or sets the URI of the default build controller. {@inheritDoc}
     */
    @Override
    public String getBuildControllerURI() {
        return getWebServiceObject().getBuildControllerUri();
    }

    public void setBuildControllerURI(final String value) {
        getWebServiceObject().setBuildControllerUri(value);
    }

    /**
     * Gets or sets the minimum quiet period between builds when using the
     * <see cref="DefinitionTriggerType.BatchedContinuousIntegration" />
     * trigger. {@inheritDoc}
     */
    @Override
    public int getContinuousIntegrationQuietPeriod() {
        return getWebServiceObject().getContinuousIntegrationQuietPeriod();
    }

    @Override
    public void setContinuousIntegrationQuietPeriod(final int value) {
        getWebServiceObject().setContinuousIntegrationQuietPeriod(value);
    }

    /**
     * Gets the date this build definition was created. This field is read-only.
     * {@inheritDoc}
     */
    @Override
    public Calendar getDateCreated() {
        return getWebServiceObject().getDateCreated();
    }

    /**
     * Gets or sets the UNC path of the default drop location. {@inheritDoc}
     */
    @Override
    public String getDefaultDropLocation() {
        return getWebServiceObject().getDefaultDropLocation();
    }

    @Override
    public void setDefaultDropLocation(final String value) {
        getWebServiceObject().setDefaultDropLocation(value);
    }

    /**
     * Gets or sets the description. {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    @Override
    public void setDescription(final String value) {
        getWebServiceObject().setDescription(value);
    }

    /**
     * Gets the attached property values.
     *
     *
     * @return
     */
    public PropertyValue[] getInternalProperties() {
        return (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getProperties());
    }

    public void setInternalProperties(final PropertyValue[] value) {
        getWebServiceObject().setProperties((_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, value));
    }

    /**
     * Gets the URI of the last build. This field is read-only. {@inheritDoc}
     */
    @Override
    public String getLastBuildURI() {
        return getWebServiceObject().getLastBuildUri();
    }

    /**
     * Gets the label created for the last good build. This field is read-only.
     * {@inheritDoc}
     */
    @Override
    public String getLastGoodBuildLabel() {
        return getWebServiceObject().getLastGoodBuildLabel();
    }

    /**
     * Gets the URI of the last good build. This field is read-only.
     * {@inheritDoc}
     */
    @Override
    public String getLastGoodBuildURI() {
        return getWebServiceObject().getLastGoodBuildUri();
    }

    /**
     * Gets or sets the queue status of the definition. {@inheritDoc}
     */
    @Override
    public DefinitionQueueStatus getQueueStatus() {
        return DefinitionQueueStatus.fromWebServiceObject(getWebServiceObject().getQueueStatus());
    }

    @Override
    public void setQueueStatus(final DefinitionQueueStatus value) {
        getWebServiceObject().setQueueStatus(value.getWebServiceObject());
    }

    /**
     * Gets or sets the trigger used for system builds. Multiple values are not
     * allowed. {@inheritDoc}
     */
    @Override
    public DefinitionTriggerType getTriggerType() {
        return new DefinitionTriggerType(getWebServiceObject().getTriggerType());
    }

    @Override
    public void setTriggerType(final DefinitionTriggerType value) {
        getWebServiceObject().setTriggerType(value.getWebServiceObject());
    }

    /**
     * Gets or sets the URI of the item. {@inheritDoc}
     */
    @Override
    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public void setURI(final String value) {
        getWebServiceObject().setUri(value);
    }

    @Override
    public String getTeamProject() {
        return teamProject;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String value) {
        name = value;
        setFullPath(getFullPath());
    }

    /**
     * The full path of this build group item. {@inheritDoc}
     */
    @Override
    public String getFullPath() {
        final StringBuilder sb = new StringBuilder();
        sb.append(BuildPath.PATH_SEPERATOR);
        sb.append(getTeamProject());
        sb.append(BuildPath.PATH_SEPERATOR);
        sb.append(getName());
        return sb.toString();
    }

    public void setFullPath(final String value) {
        getWebServiceObject().setFullPath(value);
        if (StringUtil.isNullOrEmpty(value)) {
            teamProject = StringUtil.EMPTY;
            name = StringUtil.EMPTY;
        } else {
            teamProject = BuildPath.getTeamProject(value);
            name = BuildPath.getItemName(value);
        }
    }

    /**
     * The BuildServer that owns this build definition. {@inheritDoc}
     */
    @Override
    public IBuildServer getBuildServer() {
        return buildServer;
    }

    public void setBuildServer(final IBuildServer value) {
        buildServer = value;
    }

    /**
     * The build controller that builds this build definition. {@inheritDoc}
     */
    @Override
    public IBuildController getBuildController() {
        return buildController;
    }

    @Override
    public void setBuildController(final IBuildController value) {
        buildController = value;

        if (buildController == null) {
            getWebServiceObject().setBuildControllerUri(null);
        } else {
            getWebServiceObject().setBuildControllerUri(buildController.getURI());
        }
    }

    /**
     * The Id portion of the build definition's URI. {@inheritDoc}
     */
    @Override
    public String getID() {
        if (id == null && getURI() != null) {
            final ArtifactID artifactID = new ArtifactID(getURI());
            id = artifactID.getToolSpecificID();
        }

        return id;
    }

    @Override
    public IRetentionPolicy[] getRetentionPolicies() {
        if (retentionPolicyList == null) {
            retentionPolicyList = new ArrayList<IRetentionPolicy>();

            if (buildServer.getBuildServerVersion().isV3OrGreater()) {
                // Excludes Test Results from the default list of items being
                // deleted when the build is gone
                final DeleteOptions options = DeleteOptions.ALL.remove(DeleteOptions.TEST_RESULTS);

                retentionPolicyList.add(
                    new RetentionPolicy(this, BuildReason.TRIGGERED, BuildStatus.FAILED, 10, options));
                retentionPolicyList.add(
                    new RetentionPolicy(this, BuildReason.TRIGGERED, BuildStatus.PARTIALLY_SUCCEEDED, 10, options));
                retentionPolicyList.add(
                    new RetentionPolicy(this, BuildReason.TRIGGERED, BuildStatus.STOPPED, 1, options));
                retentionPolicyList.add(
                    new RetentionPolicy(this, BuildReason.TRIGGERED, BuildStatus.SUCCEEDED, 10, options));

                retentionPolicyList.add(
                    new RetentionPolicy(this, BuildReason.VALIDATE_SHELVESET, BuildStatus.FAILED, 10, options));
                retentionPolicyList.add(
                    new RetentionPolicy(
                        this,
                        BuildReason.VALIDATE_SHELVESET,
                        BuildStatus.PARTIALLY_SUCCEEDED,
                        10,
                        options));
                retentionPolicyList.add(
                    new RetentionPolicy(this, BuildReason.VALIDATE_SHELVESET, BuildStatus.STOPPED, 1, options));
                retentionPolicyList.add(
                    new RetentionPolicy(this, BuildReason.VALIDATE_SHELVESET, BuildStatus.SUCCEEDED, 10, options));
            } else if (getBuildServer().getBuildServerVersion().isV2()) {
                // Orcas servers only support retention policies by status.
                retentionPolicyList.add(
                    new RetentionPolicy(
                        this,
                        BuildReason.ALL,
                        BuildStatus.FAILED,
                        Integer.MAX_VALUE,
                        DeleteOptions.ALL));
                retentionPolicyList.add(
                    new RetentionPolicy(
                        this,
                        BuildReason.ALL,
                        BuildStatus.PARTIALLY_SUCCEEDED,
                        Integer.MAX_VALUE,
                        DeleteOptions.ALL));
                retentionPolicyList.add(
                    new RetentionPolicy(
                        this,
                        BuildReason.ALL,
                        BuildStatus.STOPPED,
                        Integer.MAX_VALUE,
                        DeleteOptions.ALL));
                retentionPolicyList.add(
                    new RetentionPolicy(
                        this,
                        BuildReason.ALL,
                        BuildStatus.SUCCEEDED,
                        Integer.MAX_VALUE,
                        DeleteOptions.ALL));
            }
        }

        return retentionPolicyList.toArray(new IRetentionPolicy[retentionPolicyList.size()]);
    }

    public void setRetentionPolicies(final RetentionPolicy[] value) {
        final _RetentionPolicy[] _policies = (_RetentionPolicy[]) WrapperUtils.unwrap(_RetentionPolicy.class, value);
        getWebServiceObject().setRetentionPolicies(_policies);
    }

    @Override
    public ISchedule[] getSchedules() {
        if (scheduleList == null) {
            scheduleList = new ArrayList<ISchedule>();

            final ISchedule[] schedules =
                (ISchedule[]) WrapperUtils.wrap(Schedule.class, getWebServiceObject().getSchedules());
            if (schedules != null) {
                for (final ISchedule schedule : schedules) {
                    scheduleList.add(schedule);
                }
            }
        }
        return scheduleList.toArray(new ISchedule[scheduleList.size()]);
    }

    public void setSchedules(final Schedule[] value) {
        final _Schedule[] _schedules = (_Schedule[]) WrapperUtils.unwrap(_Schedule.class, value);
        getWebServiceObject().setSchedules(_schedules);
    }

    @Override
    public IWorkspaceTemplate getWorkspace() {
        if (getWebServiceObject().getWorkspaceTemplate() == null) {
            final WorkspaceTemplate template = new WorkspaceTemplate();
            getWebServiceObject().setWorkspaceTemplate(template.getWebServiceObject());
        }
        return new WorkspaceTemplate(getWebServiceObject().getWorkspaceTemplate());
    }

    public void setWorkspace(final IWorkspaceTemplate value) {
        if (value == null) {
            getWebServiceObject().setWorkspaceTemplate(null);
        } else {
            final WorkspaceTemplate template = (WorkspaceTemplate) value;
            getWebServiceObject().setWorkspaceTemplate(template.getWebServiceObject());
        }
    }

    @Override
    public BuildDefinitionSourceProvider getDefaultSourceProvider() {
        final BuildDefinitionSourceProvider[] providers = getSourceProviders();
        if (providers != null && providers.length > 0) {
            return providers[0];
        } else {
            return new BuildDefinitionSourceProvider();
        }
    }

    @Override
    public void setDefaultSourceProvider(final BuildDefinitionSourceProvider provider) {
        final BuildDefinitionSourceProvider[] providers = new BuildDefinitionSourceProvider[1];
        providers[0] = provider;
        setSourceProviders(providers);
    }

    @Override
    public BuildDefinitionSourceProvider[] getSourceProviders() {
        if (sourceProviders == null) {
            sourceProviders = new ArrayList<BuildDefinitionSourceProvider>();

            final BuildDefinitionSourceProvider[] providers = (BuildDefinitionSourceProvider[]) WrapperUtils.wrap(
                BuildDefinitionSourceProvider.class,
                getWebServiceObject().getSourceProviders());
            if (providers != null) {
                for (final BuildDefinitionSourceProvider provider : providers) {
                    sourceProviders.add(provider);
                }
            }
        }
        return sourceProviders.toArray(new BuildDefinitionSourceProvider[sourceProviders.size()]);
    }

    public void setSourceProviders(final BuildDefinitionSourceProvider[] value) {
        final _BuildDefinitionSourceProvider[] _providers =
            (_BuildDefinitionSourceProvider[]) WrapperUtils.unwrap(_BuildDefinitionSourceProvider.class, value);
        getWebServiceObject().setSourceProviders(_providers);
    }

    @Override
    public ProcessTemplate getProcess() {
        final _ProcessTemplate process = getWebServiceObject().getProcess();
        return process == null ? null : new ProcessTemplate(buildServer, process);
    }

    @Override
    public void setProcess(final IProcessTemplate value) {
        if (value == null) {
            getWebServiceObject().setProcess(null);
        } else {
            final ProcessTemplate template = (ProcessTemplate) value;
            getWebServiceObject().setProcess(template.getWebServiceObject());
        }
    }

    @Override
    public String getProcessParameters() {
        String processParameters = getWebServiceObject().getProcessParameters();
        if (processParameters == null && configurationFolderURI != null) {
            final String configurationFolderPath = getConfigurationFolderPath();
            if (configurationFolderPath != null) {
                final Properties parameters = new Properties();
                parameters.put("ConfigurationFolderPath", configurationFolderPath); //$NON-NLS-1$
                processParameters = XamlHelper.save(parameters);
            } else {
                // Initialize to a non-null value to ensure we do not continue
                // to query
                processParameters = StringUtil.EMPTY;
            }
        }

        return processParameters;
    }

    @Override
    public void setProcessParameters(final String value) {
        getWebServiceObject().setProcessParameters(value);
    }

    @Override
    public ContinuousIntegrationType getContinuousIntegrationType() {
        return TFS2010Helper.convert(new DefinitionTriggerType(getWebServiceObject().getTriggerType()));
    }

    @Override
    public void setContinuousIntegrationType(final ContinuousIntegrationType value) {
        getWebServiceObject().setTriggerType(TFS2010Helper.convert(value).getWebServiceObject());
    }

    @Override
    public boolean isEnabled() {
        return getQueueStatus().equals(DefinitionQueueStatus.ENABLED)
            || getQueueStatus().equals(DefinitionQueueStatus.PAUSED);
    }

    @Override
    public void setEnabled(final boolean value) {
        setQueueStatus(value ? DefinitionQueueStatus.ENABLED : DefinitionQueueStatus.DISABLED);
    }

    /**
     * Adds a schedule to the build definition. {@inheritDoc}
     */
    @Override
    public ISchedule addSchedule() {
        if (getSchedules().length != 0) {
            final String format = Messages.getString("BuildDefinition2012.ScheduleNotSupportedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, this.getName());
            throw new NotSupportedException(message);
        }

        final Schedule schedule = new Schedule(this);
        scheduleList.add(schedule);

        return schedule;
    }

    /**
     * Adds a retention policy to the list of retention policies for the build
     * definition. {@inheritDoc}
     */
    @Override
    public IRetentionPolicy addRetentionPolicy(
        final BuildReason reason,
        final BuildStatus status,
        final int numberToKeep,
        final DeleteOptions deleteOptions) {
        throw new NotSupportedException();
    }

    /**
     * Creates an IBuildRequest from this build definition with values for
     * BuildAgent and DropLocation set to DefaultBuildAgent and
     * DefaultDropLocation. {@inheritDoc}
     */
    @Override
    public IBuildRequest createBuildRequest() {
        return new BuildRequest(this);
    }

    /**
     * Delete this build definition. Note: this will throw if there are builds
     * pending or existing for this definition {@inheritDoc}
     */
    @Override
    public void delete() {
        if (getURI() != null) {
            buildServer.deleteBuildDefinitions(new IBuildDefinition[] {
                this
            });
        }
    }

    /**
     * Save any changes to this build definition to the TFS Build server.
     * {@inheritDoc}
     */
    @Override
    public void save() {
        buildServer.saveBuildDefinitions(new IBuildDefinition[] {
            this
        });
    }

    public void prepareToSave() {
        // Validate Name here and convert TeamProject and Name to FullPath. This
        // method will result in better error messages than letting
        // BuildPath.Root throw.
        BuildValidation.checkValidDefinitionName(name);

        getWebServiceObject().setFullPath(BuildPath.root(teamProject, name));

        // Convert retention policy dictionary to array.
        final IRetentionPolicy[] policies = getRetentionPolicies();
        getWebServiceObject().setRetentionPolicies(
            (_RetentionPolicy[]) WrapperUtils.unwrap(_RetentionPolicy.class, policies));

        // Convert schedule list to array
        final ISchedule[] schedules = getSchedules();
        getWebServiceObject().setSchedules((_Schedule[]) WrapperUtils.unwrap(_Schedule.class, schedules));

        // Add a default mapping $/TeamProject -> $(SourceDir) for Adds, not for
        // Updates.
        if (getURI() == null && getWorkspace().getMappings().length == 0) {
            getWorkspace().map(ServerPath.ROOT + getTeamProject(), BuildConstants.SOURCE_DIR_ENVIRONMENT_VARIABLE);
        }

        if (!getTriggerType().contains(DefinitionTriggerType.BATCHED_CONTINUOUS_INTEGRATION)) {
            // Default the quiet period if we are not in batched CI
            setContinuousIntegrationQuietPeriod(0);
        }

        if (!getTriggerType().contains(DefinitionTriggerType.BATCHED_GATED_CHECKIN)) {
            // Default the batch size if we are not gated
            getWebServiceObject().setBatchSize(1);
        }

        if (buildServer.getBuildServerVersion().isV3OrGreater()) {
            if (configurationFolderPath != null && configurationFolderPath.trim().length() > 0) {
                // Set the configuration folder path in the process parameters
                // (See ProcessParamters get in .Net BuildDefinition )
                final Properties properties = new Properties();
                properties.setProperty(CONFIGURATION_FOLDER_PATH, configurationFolderPath);
                if (getProcessParameters() == null) {
                    // We haven't had process parameters before - create new
                    // ones.
                    getWebServiceObject().setProcessParameters(XamlHelper.save(properties));
                } else {
                    // Process parameters exists on build definition, update
                    // with our values.
                    getWebServiceObject().setProcessParameters(
                        XamlHelper.updateProperties(getWebServiceObject().getProcessParameters(), properties));
                }
            }
        }

        final _PropertyValue[] props =
            (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, attachedProperties.getChangedProperties());
        getWebServiceObject().setProperties(props);
    }

    /**
     * Creates a new build definition specification for this definition.
     * {@inheritDoc}
     */
    @Override
    public IBuildDefinitionSpec createSpec() {
        return buildServer.createBuildDefinitionSpec(this);
    }

    /**
     * Gets all of the builds for this build definition. {@inheritDoc}
     */
    @Override
    public IBuildDetail[] queryBuilds() {
        return buildServer.queryBuilds(this);
    }

    /**
     * Creates a BuildDetail record in the TFS Build database. Build Information
     * and other changes can be made to the IBuildDetail object {@inheritDoc}
     */
    @Override
    public IBuildDetail createManualBuild(final String buildNumber) {
        return createManualBuild(buildNumber, null, BuildStatus.IN_PROGRESS, null, null);
    }

    /**
     * Creates a BuildDetail record in the TFS Build database. Build Information
     * and other changes can be made to the IBuildDetail object {@inheritDoc}
     */
    @Override
    public IBuildDetail createManualBuild(final String buildNumber, final String dropLocation) {
        return createManualBuild(buildNumber, dropLocation, BuildStatus.IN_PROGRESS, null, null);
    }

    /**
     * Creates a BuildDetail record in the TFS Build database. Build Information
     * and other changes can be made to the IBuildDetail object {@inheritDoc}
     */
    @Override
    public IBuildDetail createManualBuild(
        final String buildNumber,
        final String dropLocation,
        final BuildStatus buildStatus,
        final IBuildController controller,
        final String requestedFor) {
        throw new NotSupportedException();
    }

    public VersionControlClient getVersionControl() {
        return buildServer.getConnection().getVersionControlClient();
    }

    public String getConfigurationFolderUri(final String configurationFolderPath) {
        if (StringUtil.isNullOrEmpty(configurationFolderPath)) {
            return null;
        }

        final ItemSet itemSet = getVersionControl().getItems(
            new ItemSpec(configurationFolderPath, RecursionType.NONE, 0),
            LatestVersionSpec.INSTANCE,
            DeletedState.NON_DELETED,
            ItemType.FOLDER,
            GetItemsOptions.NONE);

        if (itemSet.getItems().length == 0) {
            throw new ConfigurationFolderPathNotFoundException(configurationFolderPath);
        } else {
            final ArtifactID artifact = new ArtifactID(
                ToolNames.VERSION_CONTROL,
                VersionControlConstants.LATEST_ITEM_ARTIFACT_TYPE,
                String.valueOf(itemSet.getItems()[0].getItemID()));

            return artifact.encodeURI();
        }
    }

    @Override
    public AttachedPropertyDictionary getAttachedProperties() {
        return attachedProperties;
    }

    public void setAttachedProperties(final AttachedPropertyDictionary value) {
        attachedProperties = value;
    }

    @Override
    public String getConfigurationFolderPath() {
        if (configurationFolderPath == null
            && buildServer.getBuildServerVersion().isV3OrGreater()
            && getProcessParameters() != null) {
            // Attempt to load from process parameters.
            final Properties props = XamlHelper.loadPartial(getProcessParameters());
            configurationFolderPath = props.getProperty(CONFIGURATION_FOLDER_PATH);
        }

        if (configurationFolderPath == null && configurationFolderURI != null) {
            final LinkingClient linkingClient =
                (LinkingClient) buildServer.getConnection().getClient(LinkingClient.class);

            final _Artifact[] artifacts = linkingClient.getArtifacts(new String[] {
                configurationFolderURI
            });
            final _Artifact artifact = artifacts[0];
            final _ExtendedAttribute[] attributes = artifact.getExtendedAttributes();
            for (int i = 0; i < attributes.length; i++) {
                if (attributes[i].getName().equals("ServerPath")) //$NON-NLS-1$
                {
                    configurationFolderPath = attributes[i].getValue();
                    return configurationFolderPath;
                }
            }

            throw new ConfigurationFolderPathNotFoundException(
                MessageFormat.format(
                    Messages.getString("BuildDefinition.ItemWasNotFoundInSourceControlFormat"), //$NON-NLS-1$
                    configurationFolderURI));
        }
        return configurationFolderPath;
    }

    @Override
    public void setConfigurationFolderPath(final String value) {
        configurationFolderPath = value;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (getURI() == null) {
            return 0;
        }
        return getURI().hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BuildDefinition)) {
            return false;
        }

        final BuildDefinition other = (BuildDefinition) obj;

        if (getURI() == null) {
            if (other.getURI() != null) {
                return false;
            }
            if (!stringEquals(getTeamProject(), other.getTeamProject())) {
                return false;
            }
            return stringEquals(getName(), other.getName());
        }

        return getURI().equals(other.getURI());
    }

    private boolean stringEquals(final String s1, final String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null) {
            return s2 == null;
        }
        return s1.equals(s2);
    }

    @Override
    public void refresh() {
        throw new NotSupportedException();
    }
}

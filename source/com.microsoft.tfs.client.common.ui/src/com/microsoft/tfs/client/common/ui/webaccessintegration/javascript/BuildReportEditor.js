//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.
//
// Shim for a Build Report.
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//
// Register for WebAccess hooks for actions initiated on the build report.
//
TFS.using(["TFS.Host", "TFS.OM", "TFS.WorkItemTracking.Controls"], function()
{
    //
    // Hook to handle "Open TFS artifact link".  This action is triggered when the user click a TFS artifact
    // link (such as a changeset) on the build report.
    //
    // Arguments:
    // A TFS artifact. 
    
	if (typeof (Host_OpenArtifactLink) == 'function')
	{
		TFS.Host.ActionManager.registerActionWorker(TFS.OM.Artifact.ACTION_ARTIFACT_EXECUTE, function (actionArgs, next) {
			var handled = Host_OpenArtifactLink(actionArgs.artifact.getUri());
			if (handled == false)
			{
				next(actionArgs);
			}
		}, 90);
	}

    //
    // Hook to handle "Open Work Item" action.  This action is triggered when the user click an associated work item link.
    //
    // Arguments:
    // The ID of the work item to be opened.
    
	if (typeof (Host_OpenWorkItemLink) == 'function')
	{
		TFS.Host.ActionManager.registerActionWorker(TFS.WorkItemTracking.Controls.WorkItemActions.ACTION_WORKITEM_OPEN, function (actionArgs, next) {
			Host_OpenWorkItemLink(actionArgs.id);
	    }, 90);
	}

    //
    // Hook to handle "Open Window" action.  This action is triggered when the user opens drop folder in the
    // build report.
    //
    // Arguments:
    // A URL.
    
	if (typeof (Host_OpenURL) == 'function')
	{
		TFS.Host.ActionManager.registerActionWorker(TFS.Host.CommonActions.ACTION_WINDOW_OPEN, function (actionArgs, next) {
			var handled = Host_OpenURL(actionArgs.url);
			if (handled == false) {
				next(actionArgs);
			}
	    }, 90);
	}
	
	//
	// Register event listeners.
	//
	var host = TFS.Host;
	var hostDocSvc = host.hostServiceManager.getService(host.DocumentService);
	
	//
	// Register for build deleted events.
	//
	if (typeof(hostDocSvc.addDeleteListener) == 'function')
	{
		hostDocSvc.addDeleteListener(function (source, args)
		{
	    	if (typeof (Host_DeleteBuild) == 'function')
	    	{
	        	Host_DeleteBuild(args.moniker);
	    	}
		});
	}
	
	//
	// Register for build deleted events.
	//
	if (typeof(hostDocSvc.addBuildStoppedListener) == 'function')
	{ 
		hostDocSvc.addBuildStoppedListener(function (source, args)
		{
	    	if (typeof (Host_BuildStopped) == 'function')
	    	{
	        	Host_BuildStopped(args.moniker);
	    	}
		});
	}

	//
	// Register for build property changed events.
	//
	if (typeof(hostDocSvc.addBuildPropertyChangedListener) == 'function')
	{
		hostDocSvc.addBuildPropertyChangedListener(function (source, args)
		{
			if (typeof (Host_BuildPropertyChanged) == 'function')
			{
				Host_BuildPropertyChanged(args.moniker, args.property, args.value);
			}
		});
	}
});
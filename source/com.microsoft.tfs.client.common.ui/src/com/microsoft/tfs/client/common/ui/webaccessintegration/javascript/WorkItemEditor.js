//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.
//
// Shim for Work Item Editor.
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//
// Register for WebAccess hooks for actions initiated on the work item.
//
TFS.using(["TFS.Host", "TFS.WorkItemTracking.Controls"], function()
{
    //
    // Hook to handle "Open TFS artifact link".  This action is triggered when the user opens a TFS artifact
    // link (such as a changeset, versioned item, or storyboard) on a links control.
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
    // Hook to handle "Open Window" action.  This action is triggered when the user opens a hyperlink in the
    // links control.  It is also triggered when the user chooses "Open" or "Save" for a work item attachment.
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
    // Hook to handle "Open Work Item" action.  This action is triggered when the user chooses "Open existing link" in a links
    // control when the selected link is a link to another work item.
    //
    // Arguments:
    // The ID of the work item to be opened.
    	
	if (typeof (Host_OpenWorkItemLink) == 'function')
	{
		TFS.Host.ActionManager.registerActionWorker(TFS.WorkItemTracking.Controls.WorkItemActions.ACTION_WORKITEM_OPEN, function (actionArgs, next) {
			Host_OpenWorkItemLink(actionArgs.id);
	    }, 90);
	}
	
	// Hook to handle "Discard New Work Item" action.  This action is triggered when the user clicks the discard toolbar button
	// on a new work item.  The host may want to close the editor without saving.
	
	TFS.Host.ActionManager.registerActionWorker(TFS.WorkItemTracking.Controls.WorkItemActions.ACTION_WORKITEM_DISCARD_IF_NEW, function (actionArgs, next) {
	    if (typeof (Host_DiscardNewWorkItem) == 'function')
	    {
	        Host_DiscardNewWorkItem();
	    }
	}, 90);
	
	// Hook to handle "Window Unload" action.  This action is triggered when the user is closing a document.  We hook this action
	// and return undefined so that the browser does not display a "Leave this page" when closing a dirty work item.  The host
	// prompts for save so we don't want the browser to prompt.
	
	TFS.Host.ActionManager.registerActionWorker(TFS.Host.CommonActions.ACTION_WINDOW_UNLOAD, function (actionArgs, next) {
		return undefined;
	}, 90);

    //
	// Register for a notification when this work item's dirty state changes.
	//
	// Arguments:
	// The work items moniker.
	
	var host = TFS.Host;
	var hostDocSvc = host.hostServiceManager.getService(host.DocumentService);
	
	hostDocSvc.addModifiedChangedListener(function (source, args)
	{
		if (typeof (Host_WorkItemDirtyChanged) == 'function')
		{
			Host_WorkItemDirtyChanged(args.moniker);
		}
	});
});

//
// The host has requested the work item be saved. Notify the WebAccess document service that the document
// should be saved.  Handles the success and error callbacks from the document service.
//
function Host_DoSave()
{
	TFS.using(["TFS.Host"], function()
	{
		var host = TFS.Host;
		var hostDocSvc = host.hostServiceManager.getService(host.DocumentService);
		var doc = hostDocSvc.getActiveDocument();
		
		doc.save(
			function(args) {
		        if (typeof (Host_WorkItemSaved) == 'function')
		        {
			        Host_WorkItemSaved(doc.getMoniker());
		        }
			},
			function(error) {
		        if (typeof (Host_WorkItemSaveFailed) == 'function')
		        {
			        Host_WorkItemSaveFailed(error.message);
		        }
			}
		);
	});
};

//
// The host is asking if the work item has been modified. Retrieve the dirty state from the WebAccess document
// service.
//
// Returns:
// True if the document is dirty, false otherwise.
//
function Host_IsModified()
{
    var hostReturn;
    
	TFS.using(["TFS.Host"], function()
	{
		var host = TFS.Host;
		var hostDocSvc = host.hostServiceManager.getService(host.DocumentService);
		
		hostReturn = hostDocSvc.isModified();
	});
	
	return hostReturn;
}

//
// The host is asking for the ID of the work item. Retrieve the ID from the current document.
//
// Returns:
// The current documents id.
//
function Host_GetWorkItemID()
{
    var hostReturn;
    
	TFS.using(["TFS.Host"], function()
	{
		var host = TFS.Host;
		var hostDocSvc = host.hostServiceManager.getService(host.DocumentService);
		var doc = hostDocSvc.getActiveDocument();

		hostReturn = doc._workItem.id;
	});
	
	return hostReturn;
}
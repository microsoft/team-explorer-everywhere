// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands.annotate;

import java.io.File;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.UIPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension4;

import com.microsoft.tfs.client.common.commands.vc.GetVersionedItemToTempLocationCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryHistoryCommand;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.vc.HistoryManager;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class AnnotateCommand extends Command {
    private static final Log log = LogFactory.getLog(AnnotateCommand.class);

    private static final String REVISION_CLASSNAME = "org.eclipse.jface.text.revisions.Revision"; //$NON-NLS-1$
    private static final String REVISION_INFORMATION_CLASSNAME = "org.eclipse.jface.text.revisions.RevisionInformation"; //$NON-NLS-1$
    private static final VersionSpec BEGINNING_OF_TIME = new ChangesetVersionSpec(1);

    private final Shell shell;
    private final TFSRepository repository;
    private final IResource resource;

    /* Username -> color map */
    private int colorIdx = 0;
    private final Map colorMap = new HashMap();
    private static final RGB[] colors = new RGB[] {
        new RGB(171, 227, 255), /* light blue */
        new RGB(184, 245, 194), /* light green */
        new RGB(204, 246, 246), /* light cyan */
        new RGB(249, 190, 226), /* light magenta */
        new RGB(255, 224, 171), /* light orange */
        new RGB(238, 184, 245), /* light maroon */
        new RGB(255, 171, 171), /* light red */
        new RGB(228, 217, 250), /* light purple */
        new RGB(254, 255, 171), /* yellow */
        new RGB(185, 185, 185), /* light grey */
        new RGB(60, 190, 255), /* blue */
        new RGB(91, 233, 114), /* green */
        new RGB(137, 234, 234), /* cyan */
        new RGB(241, 103, 187), /* magenta */
        new RGB(255, 184, 60), /* orange */
        new RGB(220, 113, 236), /* maroon */
        new RGB(255, 60, 60), /* red */
        new RGB(175, 142, 240), /* purple */
        new RGB(36, 36, 36), /* grey */
    };

    public AnnotateCommand(final TFSRepository repository, final IResource resource, final Shell shell) {
        this.repository = repository;
        this.resource = resource;
        this.shell = shell;
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("AnnotateCommand.CommandNameFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, resource.getName());
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat = Messages.getString("AnnotateCommand.CommandErrorTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, resource.getName());
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("AnnotateCommand.CommandNameFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, resource.getName());
    }

    /**
     * Queries history in item mode for the class's configured resource and
     * returns the changesets found.
     *
     * Consumes one unit of work from the given {@link IProgressMonitor}.
     *
     * @param progressMonitor
     *        a progress monitor used for running {@link QueryHistoryCommand}; 1
     *        unit of work will be used by this method (must not be
     *        <code>null</code> )
     * @param multiStatus
     *        a {@link MultiStatus} into which the status of the history query
     *        will be merged (must not be <code>null</code>)
     * @return the changesets returned by the server, <code>null</code> if no
     *         changesets were returned by the server or the user cancelled
     */
    private Changeset[] queryHistory(final IProgressMonitor progressMonitor, final MultiStatus multiStatus)
        throws Exception {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$
        Check.notNull(multiStatus, "multiStatus"); //$NON-NLS-1$

        try {
            final String messageFormat = Messages.getString("AnnotateCommand.ProgressQueryingHistoryFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, resource.getLocation().toOSString());
            progressMonitor.setTaskName(message);

            final QueryHistoryCommand queryHistoryCommand = new QueryHistoryCommand(
                repository,
                resource.getLocation().toOSString(),
                new WorkspaceVersionSpec(repository.getWorkspace()),
                RecursionType.NONE,
                BEGINNING_OF_TIME,
                new WorkspaceVersionSpec(repository.getWorkspace()));

            final IStatus status = queryHistoryCommand.run(new NullProgressMonitor());
            multiStatus.merge(status);

            return queryHistoryCommand.getChangesets();
        } finally {
            progressMonitor.worked(1);
        }
    }

    /**
     * Expands the given list of {@link Changeset}s into a potentially larger
     * list by following any branch/merge children that need followed (applies
     * to TFS 2010 and later changesets, but harmless to follow changesets from
     * previous servers).
     *
     * Consumes one unit of work from the given {@link IProgressMonitor}.
     *
     * @param progressMonitor
     *        a progress monitor used for cancellation and status repotring; 1
     *        unit of work will be used by this method (must not be
     *        <code>null</code>)
     * @param changesets
     *        the changesets to expand children of (must not be
     *        <code>null</code>)
     * @return the expanded changesets, never <code>null</code>
     * @throws CoreException
     *         if cancelled
     */
    private Changeset[] expandChildChangesets(final IProgressMonitor progressMonitor, final Changeset[] changesets)
        throws CoreException {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$
        Check.notNull(changesets, "changesets"); //$NON-NLS-1$

        /*
         * Use a sub monitor to give the user more feedback.
         */
        final SubProgressMonitor expandChildrenMonitor = new SubProgressMonitor(progressMonitor, 1);

        try {
            expandChildrenMonitor.beginTask(
                Messages.getString("AnnotateCommand.ProgressFollowBranch"), //$NON-NLS-1$
                changesets.length);

            final List expandedChangesetList = new ArrayList();

            for (int i = 0; i < changesets.length; i++) {
                checkForCancellation(expandChildrenMonitor);

                expandChildrenMonitor.subTask(
                    Messages.getString("AnnotateCommand.ProgressGetMergeHistory") + changesets[i].getChangesetID()); //$NON-NLS-1$

                // Always add the original.
                expandedChangesetList.add(changesets[i]);

                // check if we have children
                if (HistoryManager.mayHaveChildren(repository, changesets[i])) {
                    final Changeset[] children = HistoryManager.findChangesetChildren(repository, changesets[i]);
                    if (children != null && children.length > 0) {
                        expandedChangesetList.addAll(Arrays.asList(children));
                    }
                }

                expandChildrenMonitor.worked(1);
            }

            return (Changeset[]) expandedChangesetList.toArray(new Changeset[expandedChangesetList.size()]);
        } finally {
            expandChildrenMonitor.worked(1);
        }
    }

    /**
     * Downloads this command's current resource in each of the given changesets
     * to temporary files on disk.
     *
     * Consumes one unit of work from the given {@link IProgressMonitor}.
     *
     * @param progressMonitor
     *        a progress monitor used for cancellation and status repotring and
     *        passed to {@link GetVersionedItemToTempLocationCommand}; 1 unit of
     *        work will be used by this method (must not be <code>null</code>)
     * @param multiStatus
     *        a {@link MultiStatus} into which the status of the download
     *        commands will be merged (must not be <code>null</code>)
     * @param changesets
     *        the changesets to download items for (must not be
     *        <code>null</code>)
     * @return the {@link Map} of {@link Changeset} to {@link File} or
     *         <code>null</code> if the user cancelled
     * @throws Exception
     *         if {@link GetVersionedItemToTempLocationCommand} throw an
     *         exception
     */
    private Map downloadTempFiles(
        final IProgressMonitor progressMonitor,
        final MultiStatus multiStatus,
        final Changeset[] changesets) throws Exception {
        final Map changesetToFilePathMap = new LinkedHashMap();

        /*
         * Use a sub monitor to give the user more feedback.
         */
        final SubProgressMonitor downloadMonitor = new SubProgressMonitor(progressMonitor, 1);

        try {
            downloadMonitor.beginTask(
                Messages.getString("AnnotateCommand.ProgressRetrievingHistory"), //$NON-NLS-1$
                changesets.length);

            for (int i = 0; i < changesets.length; i++) {
                checkForCancellation(downloadMonitor);

                final Changeset changeset = changesets[i];

                final String messageFormat = Messages.getString("AnnotateCommand.ProgressRetrievingHistoryFormat"); //$NON-NLS-1$
                final String message =
                    MessageFormat.format(messageFormat, Integer.toString(changeset.getChangesetID()));
                downloadMonitor.subTask(message);

                final Item item = changeset.getChanges()[0].getItem();
                final GetVersionedItemToTempLocationCommand cmd = new GetVersionedItemToTempLocationCommand(
                    repository,
                    item.getServerItem(),
                    new ChangesetVersionSpec(changeset.getChangesetID()));

                final IStatus status = cmd.run(new NullProgressMonitor());

                multiStatus.merge(status);
                if (status.isOK()) {
                    final String tempFilePath = cmd.getTempLocation();
                    if (tempFilePath != null) {
                        changesetToFilePathMap.put(changeset, tempFilePath);
                    }

                    findChangeBlocks(changesetToFilePathMap);
                }

                downloadMonitor.worked(1);
            }
        } finally {
            downloadMonitor.done();
        }

        return changesetToFilePathMap;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final MultiStatus multiStatus = new MultiStatus(
            TFSCommonUIClientPlugin.PLUGIN_ID,
            IStatus.INFO,
            Messages.getString("AnnotateCommand.AnnotateCommandStatus"), //$NON-NLS-1$
            null);

        /*
         * This command has three subitems of work: history query, branch/merge
         * expansion, download files.
         */
        progressMonitor.beginTask(
            Messages.getString("AnnotateCommand.ProgressAnnotate") + resource.getLocation().toOSString(), //$NON-NLS-1$
            3);

        try {
            // Works 1 unit.
            final Changeset[] originalChangesets = queryHistory(progressMonitor, multiStatus);
            if (originalChangesets != null) {
                // Works 1 unit
                final Changeset[] expandedChangesets = expandChildChangesets(progressMonitor, originalChangesets);
                if (expandedChangesets != null) {
                    // Works 1 unit
                    downloadTempFiles(progressMonitor, multiStatus, expandedChangesets);
                }
            }
        } finally {
            progressMonitor.done();
        }

        return multiStatus;
    }

    private void findChangeBlocks(final Map tempCopyMap) {
        shell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                if (tempCopyMap != null) {
                    final Block[] blocks = VersionComparator.extractChangeBlocks(tempCopyMap, shell);
                    final IEditorPart editor = getEditor(resource);
                    showChangeBlocks(resource, editor, blocks);
                }
            }
        });
    }

    private void showChangeBlocks(final IResource r, final IEditorPart editor, final Block[] blocks) {
        if (editor instanceof ITextEditorExtension4) {
            final ITextEditorExtension4 diffExt = (ITextEditorExtension4) editor;
            Object info;
            try {
                info = buildRevisionInfo(blocks);
                if (info != null) {
                    final Method showRevInfo = diffExt.getClass().getMethod("showRevisionInformation", new Class[] //$NON-NLS-1$
                    {
                        info.getClass(),
                        String.class
                    });
                    // diffExt.showRevisionInformation(info,
                    // TfsItemReferenceprovider.ID);
                    showRevInfo.invoke(diffExt, new Object[] {
                        info,
                        TFSItemReferenceProvider.ID
                    });
                }
            } catch (final Exception e) {
                e.printStackTrace();
                MessageDialog.openError(
                    shell,
                    Messages.getString("AnnotateCommand.UpdateErrorDialogTitle"), //$NON-NLS-1$
                    e.getMessage());
            }
        }
    }

    private Object buildRevisionInfo(final Block[] blocks) throws Exception {
        final Class revInfoClass = Class.forName(REVISION_INFORMATION_CLASSNAME);
        if (revInfoClass != null) {
            final Object revInfo = revInfoClass.newInstance();
            final Class revClass = Class.forName(REVISION_CLASSNAME);
            if (revClass != null) {
                class BlockRevision extends Revision {

                    private final Block b;

                    public BlockRevision(final Block b) {
                        this.b = b;
                        addRange(new LineRange(b.start, b.end - b.start));
                    }

                    /*
                     * Despite being called "getColor()" this is used to get the
                     * color for the particular author. It is used when you
                     * select "color by author" in annotate details. Thus this
                     * should be unique for a user.
                     */
                    @Override
                    public RGB getColor() {
                        final String author = getAuthor();

                        if (colorMap.containsKey(author)) {
                            return (RGB) colorMap.get(author);
                        }

                        final RGB color = colors[colorIdx];
                        colorIdx = (colorIdx + 1) % colors.length;

                        colorMap.put(author, color);

                        return color;
                    }

                    @Override
                    public Date getDate() {
                        return b.v.cs.getDate().getTime();
                    }

                    @Override
                    public Object getHoverInfo() {
                        // Path, Changeset, Owner, date, lines, comment
                        final int id = b.v.cs.getChangesetID();
                        final String owner = b.v.cs.getOwnerDisplayName();
                        final String time = new SimpleDateFormat().format(b.v.cs.getDate().getTime());
                        final String comment = b.v.cs.getComment();
                        String h = "<p>"; //$NON-NLS-1$
                        h += "<h3>Changeset " + id + "</h3>"; //$NON-NLS-1$ //$NON-NLS-2$
                        if (owner != null) {
                            h += " by <b>" + owner + "</b>"; //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        if (time != null) {
                            h += " on <b>" + time + "</b>"; //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        // TODO lines
                        if (comment != null && comment.length() > 0) {
                            h += "<i> (" + comment + ")</i>"; //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        h += "</p>"; //$NON-NLS-1$
                        return h;
                    }

                    @Override
                    public String getId() {
                        return b.v.cs.getChangesetID() + ""; //$NON-NLS-1$
                    }

                    @Override
                    public String getAuthor() {
                        return b.v.cs.getCommitter();
                    }

                }
                final Map map = new HashMap();
                for (int i = 0; i < blocks.length; i++) {
                    final Block block = blocks[i];
                    // No revision if block has been deleted
                    if (block.end > block.start) {
                        final String key = block.v.cs.getChangesetID() + ""; //$NON-NLS-1$
                        if (map.containsKey(key)) {
                            final BlockRevision br = (BlockRevision) map.get(key);
                            br.addRange(new LineRange(block.start, block.end - block.start));
                        } else {
                            final BlockRevision br = new BlockRevision(block);
                            map.put(key, br);
                            final Method m = revInfoClass.getMethod("addRevision", new Class[] //$NON-NLS-1$
                            {
                                revClass
                            });
                            if (m != null) {
                                m.invoke(revInfo, new Object[] {
                                    br
                                });
                            }
                        }

                    }
                }
            }
            return revInfo;
        }
        return null;
    }

    public void onSelectionChanged(final IAction action, final ISelection selection) {
        // TODO need isSelectionManaged()?
        action.setEnabled(annotationsSupported() && resource.getType() == IResource.FILE);
    }

    public boolean annotationsSupported() {
        try {
            final Class revInfoClass = Class.forName(REVISION_INFORMATION_CLASSNAME);
            return revInfoClass != null;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    private AbstractDecoratedTextEditor getEditor(final IResource resource) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.isTrue(resource instanceof IFile, "resource instanceof IFile"); //$NON-NLS-1$

        /* Get the workbench window's active page */
        final IWorkbenchPage activePage =
            UIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
        final IEditorReference[] references = activePage.getEditorReferences();

        /* Query the open workbench page for its open editors */
        for (int i = 0; i < references.length; i++) {
            try {
                IEditorPart editor;

                /*
                 * See if this editor has the given resource open and can
                 * display decorations
                 */
                if (resource.equals(references[i].getEditorInput().getAdapter(IFile.class))
                    && (editor = references[i].getEditor(false)) instanceof AbstractDecoratedTextEditor) {
                    return (AbstractDecoratedTextEditor) editor;
                }
            } catch (final PartInitException e) {
                final String messageFormat = "Could not open current editor for annotation of {0}"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, resource.getLocation().toOSString());
                log.warn(message, e);
                continue;
            }
        }

        /*
         * We need to open an editor page for this resource: try to determine
         * the proper editor to use
         */
        final IContentType contentType =
            Platform.getContentTypeManager().findContentTypeFor(resource.getLocation().toOSString());
        final IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();

        final IEditorDescriptor editor =
            editorRegistry.getDefaultEditor(resource.getLocation().toOSString(), contentType);

        /* We can only annotate internal editors */
        if (editor != null && editor.isInternal() == true) {
            IEditorPart part = null;

            try {
                part = IDE.openEditor(activePage, (IFile) resource);
            } catch (final PartInitException e) {
                final String messageFormat = "Could not open editor input for annotation of {0}"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, resource.getLocation().toOSString());
                log.warn(message, e);
            }

            /* We created a decoratable editor */
            if (part != null && part instanceof AbstractDecoratedTextEditor) {
                return (AbstractDecoratedTextEditor) part;
            }
            /*
             * We created an editor that can't be decorated (eg, plugin editor,
             * web browser.) Not what we want.
             */
            else if (part != null) {
                activePage.closeEditor(part, false);
            }
        }

        try {
            final IEditorPart part = IDE.openEditor(activePage, (IFile) resource, EditorsUI.DEFAULT_TEXT_EDITOR_ID);

            if (part != null && part instanceof AbstractDecoratedTextEditor) {
                return (AbstractDecoratedTextEditor) part;
            }
        } catch (final PartInitException e) {
            final String messageFormat = "Could not open editor input for annotation of {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, resource.getLocation().toOSString());
            log.warn(message, e);
        }

        MessageDialog.openError(
            shell,
            Messages.getString("AnnotateCommand.OpenErrorDialogTitle"), //$NON-NLS-1$
            Messages.getString("AnnotateCommand.OpenErrorDialogText")); //$NON-NLS-1$

        return null;
    }
}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.Workbench;

import com.microsoft.tfs.util.Check;

public final class ViewHelper {
    private static final Log log = LogFactory.getLog(ViewHelper.class);

    private ViewHelper() {
    }

    /**
     * Locates a view by ID in the workbench and returns the first found (or
     * null if none were located.)
     *
     * @param id
     *        The view ID to locate (not <code>null</code>)
     * @return the first found corresponding {@link IViewPart} or
     *         <code>null</code> if none were located in the active workbench
     */
    public static IViewPart getActiveView(final String id) {
        return getActiveView(id, true);
    }

    /**
     * Locates a view by ID in the workbench and returns the first found (or
     * null if none were located.)
     *
     * @param id
     *        The view ID to locate (not <code>null</code>)
     * @param activate
     *        True to activate an inactive (but existing) view
     * @return the first found corresponding {@link IViewPart} or
     *         <code>null</code> if none were located in the active workbench
     */
    public static IViewPart getActiveView(final String id, final boolean activate) {
        Check.notNull(id, "id"); //$NON-NLS-1$

        try {
            final IWorkbenchWindow workbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();

            if (workbenchWindow != null) {
                final IWorkbenchPage[] pages = workbenchWindow.getPages();

                for (int i = 0; i < pages.length; i++) {
                    final IViewReference[] viewReferences = pages[i].getViewReferences();

                    for (int j = 0; j < viewReferences.length; j++) {
                        if (id.equals(viewReferences[j].getId())) {
                            final IViewPart view = viewReferences[j].getView(activate);

                            if (view != null) {
                                return view;
                            }
                        }
                    }
                }
            } else {
                log.warn("Could not locate active workbench window"); //$NON-NLS-1$
            }
        } catch (final Exception e) {
            final String messageFormat = "Could not locate view part {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, id);
            log.warn(message, e);
        }

        return null;
    }

    /**
     * Locates the instance of the given view ID and returns it, or tries to
     * create it if none were active.
     *
     * @param id
     *        The view ID to locate or create (not <code>null</code>)
     * @return the {@link IViewPart} found or created, or null if none could be
     *         created
     */
    public static IViewPart getOrCreateView(final String id) {
        IViewPart viewPart = getActiveView(id);

        if (viewPart != null) {
            return viewPart;
        }

        try {
            final IWorkbenchWindow workbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();

            if (workbenchWindow != null) {
                final IWorkbenchPage activePage = workbenchWindow.getActivePage();

                if (activePage != null) {
                    viewPart = activePage.showView(id);

                    if (viewPart != null) {
                        return viewPart;
                    }
                }
            }
        } catch (final Exception e) {
            final String messageFormat = "Could not create view part {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, id);
            log.warn(message, e);
        }

        return null;
    }

    /**
     * Shows the specified view in the workbench.
     *
     * @param viewPart
     *        The view to show.
     */
    public static void showView(final IViewPart viewPart) {
        Check.notNull(viewPart, "viewPart"); //$NON-NLS-1$

        try {
            final IViewSite viewSite = viewPart.getViewSite();
            viewSite.getPage().showView(viewSite.getId());
        } catch (final PartInitException e) {
            final String messageFormat = "Could not create view part {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, viewPart.getViewSite().getId());
            log.warn(message, e);
        }
    }

    /**
     * Locates all views by ID in the workbench and returns them (or null if
     * none were located.)
     *
     * @param id
     *        The view ID to locate (not <code>null</code>)
     * @return an array of {@link IViewPart}s (never <code>null</code>)
     */
    public static IViewPart[] getActiveViews(final String id) {
        return getActiveViews(id, true);
    }

    /**
     * Locates all views by ID in the workbench and returns them (or null if
     * none were located.)
     *
     * @param id
     *        The view ID to locate (not <code>null</code>)
     * @param activate
     *        True to activate an inactive (but existing) view
     * @return an array of {@link IViewPart}s (never <code>null</code>)
     */
    public static IViewPart[] getActiveViews(final String id, final boolean activate) {
        Check.notNull(id, "id"); //$NON-NLS-1$

        final List viewList = new ArrayList();

        try {
            final IWorkbenchWindow workbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();

            if (workbenchWindow != null) {
                final IWorkbenchPage[] pages = workbenchWindow.getPages();

                for (int i = 0; i < pages.length; i++) {
                    final IViewReference[] viewReferences = pages[i].getViewReferences();

                    for (int j = 0; j < viewReferences.length; j++) {
                        if (id.equals(viewReferences[j].getId())) {
                            final IViewPart view = viewReferences[j].getView(activate);

                            if (view != null) {
                                viewList.add(view);
                            }
                        }
                    }
                }
            } else {
                log.warn("Could not locate active workbench window"); //$NON-NLS-1$
            }
        } catch (final Exception e) {
            final String messageFormat = "Could not locate any view part for {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, id);
            log.warn(message, e);
            viewList.clear();
        }

        return (IViewPart[]) viewList.toArray(new IViewPart[viewList.size()]);
    }
}

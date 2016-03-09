// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.CellEditorAccessibilityHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.layout.GridLayoutMarginHelper;
import com.microsoft.tfs.client.common.ui.framework.sizing.MeasureItemHeightListener;
import com.microsoft.tfs.client.common.ui.framework.viewer.SafeLabelProvider;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.clients.workitem.link.Topology;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEnd;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEndCollection;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQuery;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryGrouping;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRow;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRowCollection;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.LinkQueryMode;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;

public class QueryEditorControl extends Composite {
    /*
     * The keys of the columns. These are internal identifiers and are never
     * shown to the end user.
     */
    public static final String HIDDEN_LEFTMOST_COLUMN = "hidden leftmost column"; //$NON-NLS-1$
    public static final String ADD_ROW_COLUMN = "add row column"; //$NON-NLS-1$
    public static final String LOGICAL_OPERATOR_COLUMN = "logical operator"; //$NON-NLS-1$
    public static final String FIELD_NAME_COLUMN = "field name"; //$NON-NLS-1$
    public static final String OPERATOR_COLUMN = "operator"; //$NON-NLS-1$
    public static final String VALUE_COLUMN = "value"; //$NON-NLS-1$

    public static final String ALLTOPLEVEL_RADIO_ID = "QueryEditorControl.radioAllTopLevel"; //$NON-NLS-1$
    public static final String TOPLEVELSELECTED_RADIO_ID = "QueryEditorControl.radioTopLevelSelected"; //$NON-NLS-1$
    public static final String TOPLEVELNOTSELECTED_RADIO_ID = "QueryEditorControl.radioTopLevelNotSelected"; //$NON-NLS-1$
    public static final String LINKTYPEANY_RADIO_ID = "QueryEditorControl.radioLinkTypeAny"; //$NON-NLS-1$
    public static final String LINKTYPESELECTED_RADIO_ID = "QueryEditorControl.radioLinkTypeSelected"; //$NON-NLS-1$
    public static final String TYPEOFTREE_COMBO_ID = "QueryEditorControl.comboTypeOfTree"; //$NON-NLS-1$
    public static final String LINKTYPES_TABLE_ID = "QueryEditorControl.tableLinkTypes"; //$NON-NLS-1$
    public static final String SOURCE_CLAUSE_TABLE_ID = "QueryEditorControl.tableSourceClause"; //$NON-NLS-1$
    public static final String TARGET_CLAUSE_TABLE_ID = "QueryEditorControl.tableTargetClause"; //$NON-NLS-1$

    /*
     * The column data array. The array has one entry for each column in the
     * query builder UI. Each ColumnData instance tracks initialization data for
     * a column.
     */
    private static final ColumnData[] COLUMN_DATA = new ColumnData[] {
        new ColumnData(HIDDEN_LEFTMOST_COLUMN, null, 0, false, SWT.NONE),
        new ColumnData(ADD_ROW_COLUMN, null, 25, false, SWT.CENTER),
        new ColumnData(
            LOGICAL_OPERATOR_COLUMN,
            Messages.getString("QueryEditorControl.ColumNameAndOr"), //$NON-NLS-1$
            100,
            true,
            SWT.NONE),
        new ColumnData(
            FIELD_NAME_COLUMN,
            Messages.getString("QueryEditorControl.ColumnNameField"), //$NON-NLS-1$
            100,
            true,
            SWT.NONE),
        new ColumnData(
            OPERATOR_COLUMN,
            Messages.getString("QueryEditorControl.ColumnNameOperator"), //$NON-NLS-1$
            100,
            true,
            SWT.NONE),
        new ColumnData(VALUE_COLUMN, Messages.getString("QueryEditorControl.ColumnNameValue"), 0, true, SWT.NONE), //$NON-NLS-1$
    };

    public static int columnIndexOf(final String columnName) {
        for (int i = 0; i < COLUMN_DATA.length; i++) {
            if (COLUMN_DATA[i].name.equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private static String[] getColumnProperties() {
        final String[] columnProperties = new String[COLUMN_DATA.length];
        for (int i = 0; i < columnProperties.length; i++) {
            columnProperties[i] = COLUMN_DATA[i].name;
        }
        return columnProperties;
    }

    private final QEQuery query;
    private final Project project;
    private QueryType currentQueryType = QueryType.INVALID;

    private final TableViewer tableViewerSource;
    private TableViewer tableViewerTarget;

    private final SashForm sash;
    private final Composite bottomSashComposite;
    private Composite optionsComposite;
    private Composite linkOptionsComposite;
    private Composite treeOptionsComposite;

    private Button radioTopLevelAll;
    private Button radioTopLevelSelected;
    private Button radioTopLevelNotSelected;
    private Button radioLinkTypeAny;
    private Button radioLinkTypeSelected;
    private Table tableLinkTypes;

    private MenuManager menuManager;
    private final ImageHelper imageHelper;
    private HashMap mapTreeDisplayNameToReferenceName;
    private HashMap mapLinkDisplayNameToLinkTypeEnd;

    private IAction insertAction;
    private IAction deleteAction;
    private IAction groupAction;
    private IAction ungroupAction;

    public QueryEditorControl(
        final Composite parent,
        final int style,
        final QEQuery inputQuery,
        final Project project) {
        super(parent, style);
        query = inputQuery;
        this.project = project;
        imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

        // Create a sash that fills the entire control. The sash will be two
        // panels. The top panel always contains the a query table viewer for
        // the primary conditions in the where clause. A LIST query has only
        // primary conditions and hides the bottom sash. Other query types will
        // have a table viewer with the secondary conditions (link conditions)
        // and will be visible when that type of query is open.
        SWTUtil.gridLayout(this, 1, false, 0, 0);
        sash = new SashForm(this, SWT.VERTICAL);
        SWTUtil.gridLayout(sash, 1, false, 0, 0);
        sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        // SetSashWidth is only available on Eclipse 3.4+.
        setSashWidth(sash, 6);

        // Create a table viewer to fill the top portion of the sash.
        tableViewerSource = new TableViewer(sash, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).span(1, 1).minHeight(75).applyTo(
            tableViewerSource.getTable());
        setupTableViewer(tableViewerSource, project, query.getSourceRowCollection());
        tableViewerSource.setInput(query.getSourceRowCollection());
        AutomationIDHelper.setWidgetID(tableViewerSource.getTable(), SOURCE_CLAUSE_TABLE_ID);

        // Add a composite to fill the bottom portion of the sash.
        bottomSashComposite = new Composite(sash, SWT.NONE);
        bottomSashComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        final GridLayout gridLayout = SWTUtil.gridLayout(bottomSashComposite, 1, false, 0, 0);
        gridLayout.verticalSpacing = 2;
        GridLayoutMarginHelper.setMarginLeft(gridLayout, 24);

        // Set a 40/60 split between the top and bottom panes.
        final int[] weights = new int[2];
        weights[0] = 40;
        weights[1] = 60;
        sash.setWeights(weights);

        // We've allocated the common parts of the UI. Now allocate the
        // remaining parts for a specific view. Note, the components for other
        // views are lazily created so they won't even be created in the 99%
        // case of opening an editor in its native view and not changing to
        // a different view type.
        setupView(query.getQueryType());

        createActions();
    }

    public void setQueryType(final QueryType queryType) {
        if (queryType != currentQueryType) {
            currentQueryType = queryType;
            query.setQueryType(queryType);
            setupView(queryType);
        }
    }

    /**
     * Ensure the view is populated for the specified query type. This is called
     * by the constructor for the initial layout but is also called if there is
     * a view switch during the edit session. Note that view components for the
     * link query type are only allocated if a link query view is requested. The
     * components are lazily created as needed. The entire bottom panel of the
     * sash is hidden for the normal flat query type.
     *
     *
     * @param queryType
     *        The query type
     */
    private void setupView(final QueryType queryType) {
        // Show the bottom pane in the sash. We maximize the top pane first to
        // ensure that the proper layout is triggered for the bottom pane (which
        // was not happening in a view switch from LINK->TREE or TREE->LINK.
        // This is a bit of a hack to work around a layout issue.
        sash.setMaximizedControl(tableViewerSource.getTable());

        // Hide the bottom pane of the sash if this is a simple LIST query.
        if (queryType == QueryType.LIST) {
            return;
        }

        // This is some type of link query which requires a separate table to
        // specify the target conditions for a link query. The bottom sash will
        // also contain a composite to host options for the specific query type.
        if (optionsComposite == null) {
            final Label label = new Label(bottomSashComposite, SWT.NONE);
            label.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
            label.setText(Messages.getString("QueryEditorControl.LinkItemsLabelText")); //$NON-NLS-1$

            tableViewerTarget = new TableViewer(bottomSashComposite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
            GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).span(1, 1).minHeight(75).applyTo(
                tableViewerTarget.getTable());
            setupTableViewer(tableViewerTarget, project, query.getTargetRowCollection());
            tableViewerTarget.setInput(query.getTargetRowCollection());
            AutomationIDHelper.setWidgetID(tableViewerTarget.getTable(), TARGET_CLAUSE_TABLE_ID);

            optionsComposite = new Composite(bottomSashComposite, SWT.NONE);
            optionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
            optionsComposite.setLayout(new FillLayout());
        }

        // Layout the options area for a link query.
        if (queryType == QueryType.ONE_HOP) {
            if (treeOptionsComposite != null) {
                treeOptionsComposite.dispose();
                treeOptionsComposite = null;
            }

            if (linkOptionsComposite == null) {
                linkOptionsComposite = createLinkOptionsComposite();
            }
        }

        // Layout the options area for tree query.
        if (queryType == QueryType.TREE) {
            if (linkOptionsComposite != null) {
                linkOptionsComposite.dispose();
                linkOptionsComposite = null;
            }

            if (treeOptionsComposite == null) {
                treeOptionsComposite = createTreeOptionsComposite();
            }
        }

        // Show the bottom pane in the sash.
        sash.setMaximizedControl(null);
    }

    private Composite createTreeOptionsComposite() {
        final Composite composite = new Composite(optionsComposite, SWT.NONE);
        SWTUtil.gridLayout(composite, 2, false, 0, 8);

        final Label label2 = new Label(composite, SWT.DROP_DOWN);
        label2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        label2.setText(Messages.getString("QueryEditorControl.TreeTypeLabelText")); //$NON-NLS-1$

        final Combo combo = new Combo(composite, SWT.NONE);
        combo.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false, 1, 1));
        AutomationIDHelper.setWidgetID(combo, TYPEOFTREE_COMBO_ID);

        populateTreeOptionsCombo(combo, query.getTreeQueryLinkType());

        return composite;
    }

    private Composite createLinkOptionsComposite() {
        final Composite composite = new Composite(optionsComposite, SWT.NONE);
        SWTUtil.gridLayout(composite, 3, false, 0, 8);

        final Composite group1Composite = new Composite(composite, SWT.NONE);
        group1Composite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        SWTUtil.gridLayout(group1Composite, 1, false, 0, 0);

        Label label = new Label(group1Composite, SWT.NONE);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        label.setText(Messages.getString("QueryEditorControl.TopLevelItemsLabelText")); //$NON-NLS-1$

        radioTopLevelAll = new Button(group1Composite, SWT.RADIO);
        radioTopLevelAll.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        radioTopLevelAll.setText(Messages.getString("QueryEditorControl.AllTopLevelButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(radioTopLevelAll, ALLTOPLEVEL_RADIO_ID);

        radioTopLevelSelected = new Button(group1Composite, SWT.RADIO);
        radioTopLevelSelected.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        radioTopLevelSelected.setText(Messages.getString("QueryEditorControl.OnlySpecifiedButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(radioTopLevelSelected, TOPLEVELSELECTED_RADIO_ID);

        radioTopLevelNotSelected = new Button(group1Composite, SWT.RADIO);
        radioTopLevelNotSelected.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        radioTopLevelNotSelected.setText(Messages.getString("QueryEditorControl.OnlyNonSpecifiedButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(radioTopLevelNotSelected, TOPLEVELNOTSELECTED_RADIO_ID);

        final Composite group2Composite = new Composite(composite, SWT.NONE);
        final GridLayout gridLayout = SWTUtil.gridLayout(group2Composite, 1, false, 0, 0);
        GridLayoutMarginHelper.setMarginLeft(gridLayout, 20);
        group2Composite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        label = new Label(group2Composite, SWT.NONE);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        label.setText(Messages.getString("QueryEditorControl.LinkTypesLabelText")); //$NON-NLS-1$

        radioLinkTypeAny = new Button(group2Composite, SWT.RADIO);
        radioLinkTypeAny.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        radioLinkTypeAny.setText(Messages.getString("QueryEditorControl.AnyTypeButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(radioLinkTypeAny, LINKTYPEANY_RADIO_ID);

        radioLinkTypeSelected = new Button(group2Composite, SWT.RADIO);
        radioLinkTypeSelected.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        radioLinkTypeSelected.setText(Messages.getString("QueryEditorControl.SelectedTypesButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(radioLinkTypeSelected, LINKTYPESELECTED_RADIO_ID);

        mapLinkDisplayNameToLinkTypeEnd = new HashMap();
        final ArrayList listDisplayNames = new ArrayList();
        final WorkItemLinkTypeEndCollection linkEnds = query.getWorkItemClient().getLinkTypes().getLinkTypeEnds();

        final HashSet selectedReferenceNames = new HashSet();
        final HashSet selectedDisplayNames = new HashSet();
        final String[] linkTypes = query.getLinkQueryLinkTypes();
        for (int i = 0; i < linkTypes.length; i++) {
            selectedReferenceNames.add(linkTypes[i]);
        }

        for (final Iterator it = linkEnds.iterator(); it.hasNext();) {
            final WorkItemLinkTypeEnd linkEnd = (WorkItemLinkTypeEnd) it.next();
            final String displayName = linkEnd.getName();
            final String referenceName = linkEnd.getImmutableName();

            listDisplayNames.add(displayName);
            mapLinkDisplayNameToLinkTypeEnd.put(displayName, linkEnd);
            if (selectedReferenceNames.contains(referenceName)) {
                selectedDisplayNames.add(displayName);
            }
        }

        final String[] displayNames = (String[]) listDisplayNames.toArray(new String[listDisplayNames.size()]);
        Arrays.sort(displayNames);

        if (displayNames.length > 0) {
            tableLinkTypes = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL);
            final GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
            final TableItem firstItem = new TableItem(tableLinkTypes, SWT.NONE);
            gridData.heightHint = firstItem.getBounds(0).height * 3;
            tableLinkTypes.setLayoutData(gridData);
            tableLinkTypes.remove(0);
            AutomationIDHelper.setWidgetID(tableLinkTypes, QueryEditorControl.LINKTYPES_TABLE_ID);

            for (int i = 0; i < displayNames.length; i++) {
                final TableItem item = new TableItem(tableLinkTypes, SWT.NONE);
                item.setText(displayNames[i]);
                if (selectedDisplayNames.contains(displayNames[i])) {
                    item.setChecked(true);
                }
            }

            tableLinkTypes.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(final Event event) {
                    if (event.detail == SWT.CHECK) {
                        final TableItem item = (TableItem) event.item;
                        final WorkItemLinkTypeEnd linkEnd =
                            (WorkItemLinkTypeEnd) mapLinkDisplayNameToLinkTypeEnd.get(item.getText());
                        final String referenceName = linkEnd.getImmutableName();

                        if (item.getChecked()) {
                            query.addLinkQueryLinkType(referenceName);
                        } else {
                            query.removeLinkQueryLinkType(referenceName);
                        }
                    }
                }
            });
        }

        final LinkQueryMode mode = query.getLinkQueryMode();
        if (mode == LinkQueryMode.LINKS_MUST_CONTAIN) {
            radioTopLevelSelected.setSelection(true);
        } else if (mode == LinkQueryMode.LINKS_DOES_NOT_CONTAIN) {
            radioTopLevelNotSelected.setSelection(true);
        } else {
            radioTopLevelAll.setSelection(true);
        }

        final RadioTopLevelClickHandler radioTopLevelClickHandler = new RadioTopLevelClickHandler();
        radioTopLevelAll.addSelectionListener(radioTopLevelClickHandler);
        radioTopLevelSelected.addSelectionListener(radioTopLevelClickHandler);
        radioTopLevelNotSelected.addSelectionListener(radioTopLevelClickHandler);

        if (query.getUseSelectedLinkTypes()) {
            radioLinkTypeSelected.setSelection(true);
            tableLinkTypes.setEnabled(true);
        } else {
            radioLinkTypeAny.setSelection(true);
            tableLinkTypes.setEnabled(false);
        }

        final RadioLinkTypeClickHandler radioLinkTypeClickHandler = new RadioLinkTypeClickHandler();
        radioLinkTypeAny.addSelectionListener(radioLinkTypeClickHandler);
        radioLinkTypeSelected.addSelectionListener(radioLinkTypeClickHandler);

        return composite;
    }

    private void setupTableViewer(
        final TableViewer viewer,
        final Project project,
        final QEQueryRowCollection rowCollection) {
        final Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        createColumns(table);

        /* Pad table height by four pixels to increase readability */
        final int extraTableHeight = 4;

        table.addListener(/* SWT.MeasureItem */41, new MeasureItemHeightListener(extraTableHeight));

        viewer.setLabelProvider(
            SafeLabelProvider.wrap(
                new LabelProvider(rowCollection, table.getItemHeight() + extraTableHeight, getDisplay())));
        viewer.setContentProvider(new ContentProvider());

        viewer.setColumnProperties(getColumnProperties());

        final QueryEditorCellModifier cellModifier = new QueryEditorCellModifier(rowCollection, project, viewer);
        viewer.setCellModifier(cellModifier);

        viewer.setCellEditors(new CellEditor[] {
            null,
            null,
            new ComboBoxCellEditor(table, new String[] {}),
            new ComboBoxCellEditor(table, new String[] {}),
            new ComboBoxCellEditor(table, new String[] {}),
            new ComboBoxCellEditor(table, new String[] {})
        });

        cellModifier.hookCellEditors();

        CellEditorAccessibilityHelper.setupAccessibleCellEditors(viewer);

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectConsecutiveRows(table);
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(final MouseEvent e) {
                final Table table = (Table) e.widget;
                final TableItem lastTableItem = table.getItem(table.getItemCount() - 1);
                if (lastTableItem.getBounds(1).contains(e.x, e.y)) {
                    addRowPressed(viewer, rowCollection);
                }
            }
        });

        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                if (e.keyCode != SWT.CR && e.keyCode != ' ') {
                    return;
                }

                final Point index = CellEditorAccessibilityHelper.getFocusCellIndex(viewer);

                if (index.x == columnIndexOf(ADD_ROW_COLUMN) && index.y == table.getItemCount() - 1) {
                    addRowPressed(viewer, rowCollection);
                }
            }
        });

        menuManager = new MenuManager("#popup"); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                fillContextMenu(manager);
            }
        });

        table.setMenu(menuManager.createContextMenu(table));
    }

    private void addRowPressed(final TableViewer viewer, final QEQueryRowCollection rowCollection) {
        final QEQueryRow lastRow = rowCollection.getRow(rowCollection.getRowCount() - 1);
        if (lastRow.getFieldName() != null && lastRow.getFieldName().length() > 0) {
            addNewRow(rowCollection.getRowCount(), viewer);
        } else {
            viewer.editElement(lastRow, columnIndexOf(FIELD_NAME_COLUMN));
        }
    }

    public IAction getAction(final String actionId) {
        if (!ActionFactory.DELETE.getId().equals(actionId)) {
            return null;
        }

        final TableViewer tableViewer = getFocusedTableViewer();
        if (tableViewer == null) {
            return null;
        }

        if (tableViewer.getTable().getSelectionCount() == 0) {
            return null;
        }

        return deleteAction;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public TableViewer getTableViewer() {
        return tableViewerSource;
    }

    public void refresh(final TableViewer tableViewer) {
        tableViewer.refresh();
    }

    @Override
    public void dispose() {
        imageHelper.dispose();
        super.dispose();
    }

    private void createActions() {
        insertAction = new Action() {
            @Override
            public void run() {
                final TableViewer tableViewer = getFocusedTableViewer();
                final int[] boundaries = getSelectedRowBoundaries(tableViewer.getTable());
                final QEQueryRowCollection rows = getRowCollection(tableViewer);

                int addAtIndex;
                if (boundaries == null) {
                    addAtIndex = rows.getRowCount();
                } else {
                    addAtIndex = boundaries[boundaries.length - 1];
                }

                addNewRow(addAtIndex, tableViewer);
            }
        };
        insertAction.setText(Messages.getString("QueryEditorControl.InsertActionText")); //$NON-NLS-1$
        insertAction.setImageDescriptor(imageHelper.getImageDescriptor("images/wit/insert_clause.gif")); //$NON-NLS-1$

        deleteAction = new Action() {
            @Override
            public void run() {
                /*
                 * See:
                 * Microsoft.TeamFoundation.WorkItemTracking.Controls.FilterGrid
                 * .DeleteSelectedClause()
                 */

                final TableViewer tableViewer = getFocusedTableViewer();
                final Object[] selection = ((IStructuredSelection) tableViewer.getSelection()).toArray();
                final QEQueryRowCollection rows = getRowCollection(tableViewer);

                for (int i = 0; i < selection.length; i++) {
                    rows.deleteRow((QEQueryRow) selection[i]);
                }

                if (rows.getRowCount() == 0) {
                    /*
                     * We deleted all the rows. Add a new one at the beginning.
                     */
                    addNewRow(0, tableViewer);
                } else {
                    if (rows.getRowCount() > 0) {
                        /*
                         * ensure that the first row never has a logical
                         * operator set
                         */
                        rows.getRow(0).setLogicalOperator(""); //$NON-NLS-1$
                    }

                    refresh(tableViewer);
                }
            }
        };
        deleteAction.setText(Messages.getString("QueryEditorControl.DeleteClauseActionText")); //$NON-NLS-1$
        deleteAction.setImageDescriptor(imageHelper.getImageDescriptor("images/wit/delete_clause.gif")); //$NON-NLS-1$

        groupAction = new Action() {
            @Override
            public void run() {
                final TableViewer tableViewer = getFocusedTableViewer();
                final int[] boundaries = getSelectedRowBoundaries(tableViewer.getTable());
                getRowCollection(tableViewer).getGrouping().addGrouping(boundaries[0], boundaries[1]);
                refresh(tableViewer);
            }
        };
        groupAction.setText(Messages.getString("QueryEditorControl.GroupClauseActionText")); //$NON-NLS-1$
        groupAction.setImageDescriptor(imageHelper.getImageDescriptor("images/wit/group_clause.gif")); //$NON-NLS-1$

        ungroupAction = new Action() {
            @Override
            public void run() {
                final TableViewer tableViewer = getFocusedTableViewer();
                final int[] boundaries = getSelectedRowBoundaries(tableViewer.getTable());
                getRowCollection(tableViewer).getGrouping().removeGrouping(boundaries[0], boundaries[1]);
                refresh(tableViewer);
            }
        };
        ungroupAction.setText(Messages.getString("QueryEditorControl.UnGroupClauseActionText")); //$NON-NLS-1$
        ungroupAction.setImageDescriptor(imageHelper.getImageDescriptor("images/wit/ungroup_clause.gif")); //$NON-NLS-1$
    }

    private void fillContextMenu(final IMenuManager manager) {
        final TableViewer tableViewer = getFocusedTableViewer();
        if (tableViewer != null) {
            final boolean itemsSelected = ((IStructuredSelection) tableViewer.getSelection()).size() > 0;
            final int[] boundaries = getSelectedRowBoundaries(tableViewer.getTable());
            final QEQueryRowCollection rows = getRowCollection(tableViewer);

            insertAction.setEnabled(true);
            deleteAction.setEnabled(itemsSelected);
            groupAction.setEnabled(itemsSelected && rows.getGrouping().canGroup(boundaries[0], boundaries[1]));
            ungroupAction.setEnabled(itemsSelected && rows.getGrouping().canUngroup(boundaries[0], boundaries[1]));

            manager.add(insertAction);
            manager.add(deleteAction);
            manager.add(groupAction);
            manager.add(ungroupAction);
            manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        }
    }

    private void addNewRow(final int index, final TableViewer tableViewer) {
        final QEQueryRowCollection rows = getRowCollection(tableViewer);
        rows.addNewRow(index);

        refresh(tableViewer);
        tableViewer.editElement(rows.getRow(index), columnIndexOf(FIELD_NAME_COLUMN));
    }

    private void selectConsecutiveRows(final Table table) {
        if (table.getSelectionCount() > 0) {
            final int[] boundaries = getSelectedRowBoundaries(table);
            table.select(boundaries[0], boundaries[1]);
        }
    }

    public QEQuery getQuery() {
        return query;
    }

    public QEQueryRow[] getSelectedRows() {
        return (QEQueryRow[]) ((IStructuredSelection) tableViewerSource.getSelection()).toList().toArray(
            new QEQueryRow[] {});
    }

    private TableViewer getFocusedTableViewer() {
        if (tableViewerTarget != null) {
            if (getDisplay().getFocusControl() == tableViewerTarget.getTable()) {
                return tableViewerTarget;
            }
        }

        return tableViewerSource;
    }

    private QEQueryRowCollection getRowCollection(final TableViewer tableViewer) {
        if (tableViewer == tableViewerSource) {
            return query.getSourceRowCollection();
        }

        if (tableViewer == tableViewerTarget) {
            return query.getTargetRowCollection();
        }

        throw new IllegalArgumentException();
    }

    private void createColumns(final Table table) {
        table.setLayout(new TableLayout());

        final ControlListener layoutWhenResizedListener = new ControlAdapter() {
            @Override
            public void controlResized(final ControlEvent e) {
                ((TableColumn) e.widget).getParent().layout();
            }
        };

        for (int i = 0; i < COLUMN_DATA.length; i++) {
            final ColumnData columnData = COLUMN_DATA[i];

            final TableColumn column = new TableColumn(table, columnData.style);
            column.setWidth(columnData.defaultWidth);
            column.setResizable(columnData.resizable);
            if (columnData.label != null) {
                column.setText(columnData.label);
            }
            column.addControlListener(layoutWhenResizedListener);
        }
    }

    private static class ContentProvider extends ContentProviderAdapter {
        @Override
        public Object[] getElements(final Object inputElement) {
            return ((QEQueryRowCollection) inputElement).getRows();
        }
    }

    private static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements ITableLabelProvider {
        private final QEQueryRowCollection rowCollection;
        private final int height;
        private final Display display;

        public LabelProvider(final QEQueryRowCollection rowCollection, final int height, final Display display) {
            this.rowCollection = rowCollection;
            this.height = (height > 6 ? height : 20);
            this.display = display;
        }

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            final String columnName = COLUMN_DATA[columnIndex].name;

            if (LOGICAL_OPERATOR_COLUMN.equals(columnName)) {
                final QEQueryGrouping grouping = rowCollection.getGrouping();
                final QEQueryRow row = (QEQueryRow) element;
                final int rowIx = rowCollection.indexOf(row);

                if (grouping.hasGroupings()) {
                    return FilterGrid.createGroupImage(display, grouping, height, rowIx);
                }
            }

            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            final String columnName = COLUMN_DATA[columnIndex].name;
            final QEQueryRow row = (QEQueryRow) element;

            if (ADD_ROW_COLUMN.equals(columnName)) {
                if (rowCollection.indexOf(row) == rowCollection.getRowCount() - 1) {
                    return "*"; //$NON-NLS-1$
                }
                return ""; //$NON-NLS-1$
            }
            if (LOGICAL_OPERATOR_COLUMN.equals(columnName)) {
                return row.getLogicalOperator();
            }
            if (FIELD_NAME_COLUMN.equals(columnName)) {
                return row.getFieldName();
            }
            if (OPERATOR_COLUMN.equals(columnName)) {
                return row.getOperator();
            }
            if (VALUE_COLUMN.equals(columnName)) {
                return row.getValue();
            }
            return ""; //$NON-NLS-1$
        }
    }

    private static class TableLayout extends Layout {
        @Override
        protected Point computeSize(
            final Composite composite,
            final int wHint,
            final int hHint,
            final boolean flushCache) {
            /*
             * Use the native computeSize algorithm. We must temporarily replace
             * ourselves as the layout to avoid a recursive call.
             */
            composite.setLayout(null);
            final Point size = composite.computeSize(wHint, hHint, flushCache);
            composite.setLayout(this);

            return size;
        }

        @Override
        protected void layout(final Composite composite, final boolean flushCache) {
            final Rectangle clientArea = composite.getClientArea();

            final Table table = (Table) composite;
            final TableColumn[] columns = table.getColumns();

            int totalColumnWidth = 0;
            for (int i = 0; i < columns.length; i++) {
                totalColumnWidth += columns[i].getWidth();
            }

            /*
             * Temporarily replace ourselves as the layout to avoid a recursive
             * call.
             */
            table.setLayout(null);

            if (totalColumnWidth < clientArea.width) {
                final TableColumn lastColumn = columns[columns.length - 1];
                int lastColumnWidth = lastColumn.getWidth();
                lastColumnWidth += (clientArea.width - totalColumnWidth);
                lastColumn.setWidth(lastColumnWidth);
            } else if (totalColumnWidth > clientArea.width) {
                final TableColumn lastColumn = columns[columns.length - 1];
                final int lastColumnWidth = lastColumn.getWidth();

                final int otherColumnWidths = totalColumnWidth - lastColumnWidth;
                int newWidth = clientArea.width - otherColumnWidths;
                if (newWidth < 15) {
                    newWidth = 15;
                }
                lastColumn.setWidth(newWidth);
            }

            table.setLayout(this);
        }
    }

    public int[] getSelectedRowBoundaries(final Table table) {
        final int[] indices = table.getSelectionIndices();
        if (indices.length == 0) {
            return null;
        }

        int high = indices[0];
        int low = indices[0];

        for (int i = 1; i < indices.length; i++) {
            if (indices[i] > high) {
                high = indices[i];
            }
            if (indices[i] < low) {
                low = indices[i];
            }
        }

        return new int[] {
            low,
            high
        };
    }

    private void populateTreeOptionsCombo(final Combo combo, final String initialValue) {
        int selectedItemIndex = 0;
        final ArrayList list = new ArrayList();
        mapTreeDisplayNameToReferenceName = new HashMap();

        final WorkItemLinkTypeCollection linkTypes = query.getWorkItemClient().getLinkTypes();
        final WorkItemLinkTypeEndCollection endTypes = linkTypes.getLinkTypeEnds();

        for (final Iterator it = endTypes.iterator(); it.hasNext();) {
            final WorkItemLinkTypeEnd end = (WorkItemLinkTypeEnd) it.next();
            if (end.getLinkType().getLinkTopology() == Topology.TREE && end.isForwardLink()) {
                final String referenceName = end.getImmutableName();
                final String display =
                    MessageFormat.format(Messages.getString("QueryEditorControl.HierarchyLinkTypeFormat"), new Object[] //$NON-NLS-1$
                {
                    end.getOppositeEnd().getName(),
                    end.getName()
                });

                if (referenceName.equalsIgnoreCase(initialValue)) {
                    selectedItemIndex = list.size();
                }

                list.add(display);
                mapTreeDisplayNameToReferenceName.put(display, referenceName);
            }
        }

        combo.setItems((String[]) list.toArray(new String[list.size()]));
        if (combo.getItemCount() > 0) {
            combo.select(selectedItemIndex);
        }

        combo.addSelectionListener(new ComboTreeOptionsSelectionHandler());
    }

    /**
     * SetSashWidth is only available on Eclipse 3.4+. Use reflection to see if
     * the method is available. There is no need to implement a fallback if the
     * method does not exist. Allowing the default sash width is sufficient.
     *
     *
     * @param sash
     *        The sash control.
     *
     * @param width
     *        The desired width.
     */
    private static void setSashWidth(final SashForm sash, final int width) {
        try {
            final Class[] parameters = new Class[1];
            parameters[0] = Integer.TYPE;

            final Object[] arguments = new Object[1];
            arguments[0] = new Integer(width);

            final Method m = sash.getClass().getMethod("setSashWidth", parameters); //$NON-NLS-1$
            m.invoke(sash, arguments);
        } catch (final Exception e) {
            // ignore
        }
    }

    private class ComboTreeOptionsSelectionHandler extends SelectionAdapter {
        @Override
        public void widgetSelected(final SelectionEvent e) {
            final Combo combo = (Combo) e.widget;
            final String displayName = combo.getItem(combo.getSelectionIndex());
            final String referenceName = (String) mapTreeDisplayNameToReferenceName.get(displayName);
            query.setTreeQueryLinkType(referenceName);
        }
    }

    private class RadioTopLevelClickHandler extends SelectionAdapter {
        @Override
        public void widgetSelected(final SelectionEvent e) {
            final Button button = (Button) e.widget;

            if (button.getSelection()) {
                if (button.equals(radioTopLevelAll)) {
                    query.setLinkQueryMode(LinkQueryMode.LINKS_MAY_CONTAIN);
                } else if (button.equals(radioTopLevelSelected)) {
                    query.setLinkQueryMode(LinkQueryMode.LINKS_MUST_CONTAIN);
                } else if (button.equals(radioTopLevelNotSelected)) {
                    query.setLinkQueryMode(LinkQueryMode.LINKS_DOES_NOT_CONTAIN);
                }
            }
        }
    }

    private class RadioLinkTypeClickHandler extends SelectionAdapter {
        @Override
        public void widgetSelected(final SelectionEvent e) {
            final Button button = (Button) e.widget;

            if (button.getSelection()) {
                if (button.equals(radioLinkTypeAny)) {
                    tableLinkTypes.setEnabled(false);
                    query.setUseSelectedLinkTypes(false);
                } else if (button.equals(radioLinkTypeSelected)) {
                    tableLinkTypes.setEnabled(true);
                    query.setUseSelectedLinkTypes(true);
                }
            }
        }
    }

    private static class ColumnData {
        private final String name;
        public String label;
        public int defaultWidth;
        public boolean resizable;
        public int style;

        public ColumnData(
            final String name,
            final String label,
            final int defaultWidth,
            final boolean resizable,
            final int style) {
            this.name = name;
            this.label = label;
            this.defaultWidth = defaultWidth;
            this.resizable = resizable;
            this.style = style;
        }
    }
}

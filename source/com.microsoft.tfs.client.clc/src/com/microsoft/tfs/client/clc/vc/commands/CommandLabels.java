// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.options.shared.OptionFormat;
import com.microsoft.tfs.client.clc.vc.options.OptionOwner;
import com.microsoft.tfs.client.clc.vc.printers.BasicPrinter;
import com.microsoft.tfs.client.clc.xml.CommonXMLNames;
import com.microsoft.tfs.client.clc.xml.SimpleXMLWriter;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpec;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.util.Check;

public final class CommandLabels extends Command {
    private final static String LABELS_ELEMENT_NAME = "labels"; //$NON-NLS-1$
    private final static String LABEL_NAME_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
    private final static String LABEL_SCOPE_ATTRIBUTE_NAME = "scope"; //$NON-NLS-1$

    /**
     * The default long format for the current locale.
     */
    private final DateFormat DEFAULT_DATE_FORMAT = SimpleDateFormat.getDateTimeInstance();

    public CommandLabels() {
        super();
    }

    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (getFreeArguments().length > 1) {
            final String messageFormat = Messages.getString("CommandLabels.CommandRequiresZeroOrOneLabelNamesFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        String ownerArg = null;
        String format = OptionFormat.BRIEF;
        String labelOwner = null;

        Option o = null;

        if ((o = findOptionType(OptionFormat.class)) != null) {
            format = ((OptionFormat) o).getValue();
        }

        if ((o = findOptionType(OptionOwner.class)) != null) {
            ownerArg = ((OptionOwner) o).getValue();
        }

        final TFSTeamProjectCollection connection = createConnection(true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        String labelName = null;
        String labelScope = null;

        if (getFreeArguments().length == 1) {
            final LabelSpec spec = LabelSpec.parse(getFreeArguments()[0], ServerPath.ROOT, true);

            labelName = spec.getLabel();
            labelScope = spec.getScope();
        }

        if (ownerArg != null) {
            labelOwner = ownerArg;
        }

        if (labelOwner == null) {
            labelOwner = VersionControlConstants.AUTHENTICATED_USER;
        } else if (labelOwner.equalsIgnoreCase("*")) //$NON-NLS-1$
        {
            labelOwner = null;
        }

        final VersionControlLabel[] labels = client.queryLabels(
            labelName,
            labelScope,
            labelOwner,
            (OptionFormat.BRIEF.equalsIgnoreCase(format) ? false : true),
            null,
            null);

        if (labels.length == 0) {
            getDisplay().printLine(Messages.getString("CommandLabels.NoLabelsFound")); //$NON-NLS-1$
            setExitCode(ExitCode.FAILURE);
            return;
        }

        if (OptionFormat.DETAILED.equalsIgnoreCase(format)) {
            printDetailed(labels);
        } else if (OptionFormat.BRIEF.equalsIgnoreCase(format)) {
            printBrief(labels);
        } else if (OptionFormat.XML.equalsIgnoreCase(format)) {
            try {
                printXML(labels);
            } catch (final TransformerConfigurationException e) {
                throw new CLCException(e);
            } catch (final SAXException e) {
                throw new CLCException(e);
            }
        } else {
            final String messageFormat = Messages.getString("CommandLabels.UnsupportedOutputFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, format);

            throw new RuntimeException(message);
        }
    }

    private void printXML(final VersionControlLabel[] labels) throws TransformerConfigurationException, SAXException {
        Check.notNull(labels, "labels"); //$NON-NLS-1$

        final SimpleXMLWriter xmlWriter = new SimpleXMLWriter(getDisplay());

        xmlWriter.startDocument();
        xmlWriter.startElement("", "", LABELS_ELEMENT_NAME, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$

        for (int i = 0; i < labels.length; i++) {
            final VersionControlLabel label = labels[i];

            final AttributesImpl labelAttributes = new AttributesImpl();

            labelAttributes.addAttribute("", "", LABEL_NAME_ATTRIBUTE_NAME, "CDATA", label.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            labelAttributes.addAttribute("", "", LABEL_SCOPE_ATTRIBUTE_NAME, "CDATA", label.getScope()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            labelAttributes.addAttribute("", "", CommonXMLNames.USER, "CDATA", label.getOwner()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            labelAttributes.addAttribute(
                "", //$NON-NLS-1$
                "", //$NON-NLS-1$
                CommonXMLNames.DATE,
                "CDATA", //$NON-NLS-1$
                SimpleXMLWriter.ISO_DATE_FORMAT.format(label.getDate().getTime()));

            xmlWriter.startElement("", "", CommonXMLNames.LABEL, labelAttributes); //$NON-NLS-1$ //$NON-NLS-2$

            if (label.getComment() != null && label.getComment().length() > 0) {
                xmlWriter.startElement("", "", CommonXMLNames.COMMENT, new AttributesImpl()); //$NON-NLS-1$ //$NON-NLS-2$
                xmlWriter.characters(label.getComment().toCharArray(), 0, label.getComment().length());
                xmlWriter.endElement("", "", CommonXMLNames.COMMENT); //$NON-NLS-1$ //$NON-NLS-2$
            }

            /*
             * Print the changeset details.
             */
            final Item[] items = label.getItems();
            if (items != null && items.length > 0) {
                for (int j = 0; j < items.length; j++) {
                    final Item item = items[j];
                    final AttributesImpl itemAttributes = new AttributesImpl();

                    itemAttributes.addAttribute(
                        "", //$NON-NLS-1$
                        "", //$NON-NLS-1$
                        CommonXMLNames.CHANGESET,
                        "CDATA", //$NON-NLS-1$
                        Integer.toString(item.getChangeSetID()));
                    itemAttributes.addAttribute("", "", CommonXMLNames.SERVER_ITEM, "CDATA", item.getServerItem()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                    xmlWriter.startElement("", "", CommonXMLNames.ITEM, itemAttributes); //$NON-NLS-1$ //$NON-NLS-2$
                    xmlWriter.endElement("", "", CommonXMLNames.ITEM); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

            xmlWriter.endElement("", "", CommonXMLNames.LABEL); //$NON-NLS-1$ //$NON-NLS-2$
        }

        xmlWriter.endElement("", "", LABELS_ELEMENT_NAME); //$NON-NLS-1$ //$NON-NLS-2$
        xmlWriter.endDocument();
    }

    private void printDetailed(final VersionControlLabel[] labels) {
        Check.notNull(labels, "labels"); //$NON-NLS-1$

        /*
         * This table can be re-used for each item with the same column
         * settings.
         */
        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
        table.setHeadingsVisible(false);
        table.setColumns(new Column[] {
            new Column("", Sizing.TIGHT), //$NON-NLS-1$
            new Column("", Sizing.EXPAND) //$NON-NLS-1$
        });

        for (int i = 0; i < labels.length; i++) {
            final VersionControlLabel label = labels[i];
            Check.notNull(label, "label"); //$NON-NLS-1$

            table.clearRows();

            /*
             * Write a separator row.
             */
            if (i > 0) {
                getDisplay().printLine(""); //$NON-NLS-1$
                BasicPrinter.printSeparator(getDisplay(), '=');
            }

            table.addRow(new String[] {
                Messages.getString("CommandLabels.LabelColon"), //$NON-NLS-1$
                label.getName()
            });
            table.addRow(new String[] {
                Messages.getString("CommandLabels.ScopeColon"), //$NON-NLS-1$
                label.getScope()
            });
            table.addRow(new String[] {
                Messages.getString("CommandLabels.OwnerColon"), //$NON-NLS-1$
                label.getOwnerDisplayName()
            });
            table.addRow(new String[] {
                Messages.getString("CommandLabels.DateColon"), //$NON-NLS-1$
                DEFAULT_DATE_FORMAT.format(label.getDate().getTime())
            });
            table.addRow(new String[] {
                Messages.getString("CommandLabels.CommentColon"), //$NON-NLS-1$
                label.getComment()
            });

            getDisplay().printLine(""); //$NON-NLS-1$
            table.print(getDisplay().getPrintStream());
            getDisplay().printLine(""); //$NON-NLS-1$

            /*
             * Print the changeset details.
             */
            final Item[] items = label.getItems();
            if (items != null && items.length > 0) {
                final TextOutputTable subtable = new TextOutputTable(getDisplay().getWidth());
                subtable.setColumns(new Column[] {
                    new Column(Messages.getString("CommandLabels.Changeset"), Sizing.TIGHT), //$NON-NLS-1$
                    new Column(Messages.getString("CommandLabels.Item"), Sizing.EXPAND) //$NON-NLS-1$
                });
                subtable.setHeadingsVisible(true);

                for (int j = 0; j < items.length; j++) {
                    final Item item = items[j];
                    Check.notNull(item, "item"); //$NON-NLS-1$

                    subtable.addRow(new String[] {
                        Integer.toString(item.getChangeSetID()),
                        item.getServerItem()
                    });
                }

                subtable.print(getDisplay().getPrintStream());
            }
        }
    }

    private void printBrief(final VersionControlLabel[] labels) {
        Check.notNull(labels, "labels"); //$NON-NLS-1$

        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
        table.setHeadingsVisible(true);
        table.setTruncateLongLines(true);
        table.setColumns(new Column[] {
            new Column(Messages.getString("CommandLabels.Label"), Sizing.EXPAND), //$NON-NLS-1$
            new Column(Messages.getString("CommandLabels.Owner"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("CommandLabels.Date"), Sizing.TIGHT) //$NON-NLS-1$
        });

        // Display the labels.
        for (int i = 0; i < labels.length; i++) {
            final VersionControlLabel label = labels[i];
            Check.notNull(label, "label"); //$NON-NLS-1$

            table.addRow(new String[] {
                label.getName(),
                label.getOwnerDisplayName(),
                DEFAULT_DATE_FORMAT.format(label.getDate().getTime())
            });
        }

        table.print(getDisplay().getPrintStream());
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionOwner.class,
            OptionFormat.class
        }, "[<labelNameFilter>]"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandLabels.HelpText1") //$NON-NLS-1$
        };
    }
}

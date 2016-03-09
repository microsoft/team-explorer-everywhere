// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionDeleteAll;
import com.microsoft.tfs.client.clc.vc.options.OptionDeleteValues;
import com.microsoft.tfs.client.clc.vc.options.OptionLock;
import com.microsoft.tfs.client.clc.vc.options.OptionOutput;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionSetValues;
import com.microsoft.tfs.client.clc.vc.options.OptionSilent;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.client.clc.vc.printers.BasicPrinter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;

/**
 * Sets, lists, and deletes TFS 2012 versioned properties. TFS 2010 "properties"
 * (renamed "attributes" in TFS 2012) never had a command-line interface.
 */
public final class CommandProperty extends Command {
    private static final String OUTPUT_FILE_AS_TEXT_ENCODING = "UTF-8"; //$NON-NLS-1$

    public CommandProperty() {
        super();
    }

    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (getFreeArguments().length == 0) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandProperty.NoFilesSpecified")); //$NON-NLS-1$
        }

        reportBadOptionCombinationIfPresent(OptionSetValues.class, OptionDeleteValues.class);
        reportBadOptionCombinationIfPresent(OptionSetValues.class, OptionOutput.class);
        reportBadOptionCombinationIfPresent(OptionDeleteValues.class, OptionOutput.class);
        reportBadOptionCombinationIfPresent(OptionDeleteValues.class, OptionDeleteAll.class);
        reportBadOptionCombinationIfPresent(OptionSetValues.class, OptionDeleteAll.class);
        reportBadOptionCombinationIfPresent(OptionOutput.class, OptionDeleteAll.class);
        reportBadOptionCombinationIfPresent(OptionSetValues.class, OptionVersion.class);
        reportBadOptionCombinationIfPresent(OptionDeleteValues.class, OptionVersion.class);
        reportBadOptionCombinationIfPresent(OptionDeleteAll.class, OptionVersion.class);

        RecursionType recursion = RecursionType.NONE;
        LockLevel lockLevel = LockLevel.UNCHANGED;

        Option o = null;
        if ((o = findOptionType(OptionRecursive.class)) != null) {
            recursion = RecursionType.FULL;
        }

        if ((o = findOptionType(OptionLock.class)) != null) {
            lockLevel = ((OptionLock) o).getValueAsLockLevel();
        }

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);

        final String itemSpec = LocalPath.canonicalize(getFreeArguments()[0]);

        if (findOptionType(OptionSetValues.class) != null) {
            runSet(workspace, itemSpec, recursion, lockLevel);
        } else if (findOptionType(OptionDeleteValues.class) != null || findOptionType(OptionDeleteAll.class) != null) {
            runDelete(workspace, itemSpec, recursion, lockLevel);
        } else {
            runList(workspace, itemSpec, recursion, lockLevel);
        }
    }

    private void runList(
        final Workspace workspace,
        final String itemSpec,
        final RecursionType recursion,
        final LockLevel lockLevel)
            throws CLCException,
                InvalidFreeArgumentException,
                InvalidOptionException,
                InvalidOptionValueException {
        final boolean hasOutputSpecified = findOptionType(OptionOutput.class) != null;

        String propertyName = PropertyConstants.QUERY_ALL_PROPERTIES_WILDCARD;
        if (getFreeArguments().length > 1) {
            propertyName = getFreeArguments()[1];
        }

        if (hasOutputSpecified
            && (propertyName == PropertyConstants.QUERY_ALL_PROPERTIES_WILDCARD || recursion != RecursionType.NONE)) {
            throw new InvalidOptionException(
                Messages.getString("CommandProperty.OutputRequiresPropertyNameNoRecursion")); //$NON-NLS-1$
        }

        Option o;
        VersionSpec defaultVersion = new WorkspaceVersionSpec(workspace);

        if ((o = findOptionType(OptionVersion.class)) != null) {
            final VersionSpec[] versions = ((OptionVersion) o).getParsedVersionSpecs();

            if (versions == null || versions.length != 1) {
                throw new InvalidOptionValueException(Messages.getString("CommandProperty.VersionRangesNotSupported")); //$NON-NLS-1$
            }

            defaultVersion = versions[0];
        }

        final QualifiedItem[] qualifiedItems = parseQualifiedItems(new String[] {
            itemSpec
        }, defaultVersion, false, 0);

        if (qualifiedItems.length != 1) {
            throw new InvalidFreeArgumentException(
                MessageFormat.format(Messages.getString("CommandProperty.CouldNotParseQualifiedItemFormat"), itemSpec)); //$NON-NLS-1$
        }

        final QualifiedItem qualifiedItem = qualifiedItems[0];

        // Call QueryItems to get the properties
        final ItemSet[] items = workspace.getClient().getItems(new ItemSpec[] {
            qualifiedItem.toItemSpec(recursion)
        }, qualifiedItem.getVersions()[0], DeletedState.ANY, ItemType.ANY, GetItemsOptions.NONE, new String[] {
            propertyName
        });

        // Warn and return if we found nothing.
        if (items.length == 0 || items[0] == null || items[0].getItems().length == 0) {
            getDisplay().printErrorLine(MessageFormat.format(
                Messages.getString("CommandProperty.NoFileMatchesFormat"), //$NON-NLS-1$
                qualifiedItem.getPath()));
            return;
        }

        if (hasOutputSpecified) {
            final String outputFile = ((OptionOutput) findOptionType(OptionOutput.class)).getValue();

            // Get the first property off of the first extended item
            final PropertyValue[] propertyValues = items[0].getItems()[0].getPropertyValues();
            final PropertyValue property =
                propertyValues != null && propertyValues.length > 0 ? propertyValues[0] : null;

            if (property == null) {
                getDisplay().printErrorLine(
                    MessageFormat.format(
                        Messages.getString("CommandProperty.PropertyDoesNotExistFormat"), //$NON-NLS-1$
                        propertyName,
                        itemSpec));
                return;
            }

            try {
                if ((new byte[0]).getClass().equals(property.getPropertyType())) {
                    final FileOutputStream fos = new FileOutputStream(outputFile);
                    try {
                        fos.write((byte[]) property.getPropertyValue());
                    } finally {
                        IOUtils.closeSafely(fos);
                    }
                } else {
                    // VS uses File.WriteAllText, which defaults to UTF-8
                    final OutputStreamWriter writer =
                        new OutputStreamWriter(new FileOutputStream(outputFile), OUTPUT_FILE_AS_TEXT_ENCODING);
                    try {
                        writer.write(property.getPropertyValue().toString());
                    } finally {
                        IOUtils.closeSafely(writer);
                    }
                }
            } catch (final IOException e) {
                throw new CLCException(Messages.getString("CommandProperty.ErrorWritingPropertyToFile"), e); //$NON-NLS-1$
            }
        } else {
            displayProperties(items);
        }

    }

    private void runDelete(
        final Workspace workspace,
        final String itemSpec,
        final RecursionType recursion,
        final LockLevel lockLevel) {
        final List<PropertyValue> properties = new ArrayList<PropertyValue>();
        if (findOptionType(OptionDeleteAll.class) != null) {
            // This will delete all properties
            properties.add(new PropertyValue(null, (Object) null));
        } else {
            final OptionDeleteValues optionDeleteValues = (OptionDeleteValues) findOptionType(OptionDeleteValues.class);
            Check.notNull(optionDeleteValues, "optionDelete"); //$NON-NLS-1$

            final String[] names = optionDeleteValues.getValues();
            for (final String name : names) {
                properties.add(new PropertyValue(name, (Object) null));
            }
        }

        // Actually pend the change
        final PendChangesOptions options =
            findOptionType(OptionSilent.class) != null ? PendChangesOptions.SILENT : PendChangesOptions.NONE;

        workspace.pendPropertyChange(new String[] {
            itemSpec
        }, properties.toArray(new PropertyValue[properties.size()]), recursion, lockLevel, options, null);

    }

    private void runSet(
        final Workspace workspace,
        final String itemSpec,
        final RecursionType recursion,
        final LockLevel lockLevel) {
        // Parse the values being set
        final List<PropertyValue> propertyValues =
            ((OptionSetValues) findOptionType(OptionSetValues.class)).getValues();

        // Actually pend the change
        final PendChangesOptions options =
            findOptionType(OptionSilent.class) != null ? PendChangesOptions.SILENT : PendChangesOptions.NONE;

        workspace.pendPropertyChange(new String[] {
            itemSpec
        }, propertyValues.toArray(new PropertyValue[propertyValues.size()]), recursion, lockLevel, options, null);
    }

    private void displayProperties(final ItemSet[] items) {
        for (final ItemSet itemSet : items) {
            for (final Item item : itemSet.getItems()) {
                boolean propertyWritten = false;
                BasicPrinter.printSeparator(getDisplay(), '-');

                // item
                getDisplay().printLine(MessageFormat.format(
                    Messages.getString("CommandProperty.AColonBFormat"), //$NON-NLS-1$
                    Messages.getString("CommandProperty.ServerItem"), //$NON-NLS-1$
                    item.getServerItem()));

                // now write each of the properties
                if (item.getPropertyValues() != null) {
                    for (final PropertyValue property : item.getPropertyValues()) {
                        propertyWritten = true;
                        getDisplay().printLine(MessageFormat.format(
                            Messages.getString("CommandProperty.AColonBFormat"), //$NON-NLS-1$
                            Messages.getString("CommandProperty.Name"), //$NON-NLS-1$
                            property.getPropertyName()));

                        final String valueString;
                        if (property.getPropertyValue() == null) {
                            valueString = Messages.getString("CommandProperty.NullValue"); //$NON-NLS-1$
                        } else if ((new byte[0]).getClass().equals(property.getPropertyType())) {
                            valueString = Messages.getString("CommandProperty.BinaryValue"); //$NON-NLS-1$
                        } else {
                            valueString = property.getPropertyValue().toString();
                        }

                        getDisplay().printLine(MessageFormat.format(
                            Messages.getString("CommandProperty.AColonBFormat"), //$NON-NLS-1$
                            Messages.getString("CommandProperty.Value"), //$NON-NLS-1$
                            valueString));
                        getDisplay().printLine(""); //$NON-NLS-1$
                    }
                }

                if (!propertyWritten) {
                    getDisplay().printLine(Messages.getString("CommandProperty.NoPropertiesOnItem")); //$NON-NLS-1$
                    getDisplay().printLine(""); //$NON-NLS-1$
                }
            }
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[4];

        // List all properties or one named property
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionOutput.class,
            OptionRecursive.class,
            OptionVersion.class
        }, "<itemSpec> [<propertyname>]"); //$NON-NLS-1$

        // Set properties
        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class
        }, "<itemSpec>", new Class[] { //$NON-NLS-1$
            OptionSetValues.class
        });

        // Delete one or more properties by name
        optionSets[2] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class
        }, "<itemSpec>", new Class[] { //$NON-NLS-1$
            OptionDeleteValues.class
        });

        // Delete all properties on an object
        optionSets[3] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class,
        }, "<itemSpec>", new Class[] { //$NON-NLS-1$
            OptionDeleteAll.class
        });

        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandProperty.HelpText1"), //$NON-NLS-1$
            Messages.getString("CommandProperty.HelpText2"), //$NON-NLS-1$
            MessageFormat.format(
                Messages.getString("CommandProperty.HelpText3Format"), //$NON-NLS-1$
                PropertyConstants.EXECUTABLE_KEY,
                PropertyConstants.EXECUTABLE_ENABLED_VALUE.getPropertyValue().toString(),
                PropertyConstants.EXECUTABLE_DISABLED_VALUE.getPropertyValue().toString())
        };
    }
}

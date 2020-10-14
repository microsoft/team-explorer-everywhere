// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.witcontrols;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Auto-complete combo custom control which gets dropdown values from a file
 */
public class FileSourceDropdown extends ExternalSourceDropdown {
    private String[] dropdownItems;

    /**
     * Read the dropdown items from file - each line is a dropdown item
     * {@inheritDoc}
     */
    @Override
    protected String[] getDropdownItems() {
        if (dropdownItems == null) {
            final List<String> items = new ArrayList<String>();

            final String filePath = getControlDescription().getAttribute("FilePath"); //$NON-NLS-1$
            if (filePath == null || filePath.length() == 0) {
                items.add("Must specify the 'FilePath' attribute in this work item type definition Control"); //$NON-NLS-1$
            } else {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(filePath));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        items.add(line);
                    }
                } catch (final Exception e) {
                    items.clear();
                    items.add("Failed to read file: " + filePath + ". Error: " + e.getLocalizedMessage()); //$NON-NLS-1$ //$NON-NLS-2$
                } finally {
                    if (reader != null) {
                        try { reader.close();
                        } catch (IOException ignore) {
                        }
                    }
                }
            }

            dropdownItems = items.toArray(new String[items.size()]);
        }

        return dropdownItems;
    }
}

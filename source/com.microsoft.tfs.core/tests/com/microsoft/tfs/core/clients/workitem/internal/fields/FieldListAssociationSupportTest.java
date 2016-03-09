// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

public class FieldListAssociationSupportTest extends TestCase {
    private static final List LIST1 = Arrays.asList(new Object[] {
        "A", //$NON-NLS-1$
        "B", //$NON-NLS-1$
        "C", //$NON-NLS-1$
        "D", //$NON-NLS-1$
        "E" //$NON-NLS-1$
    });
    private static final List LIST2 = Arrays.asList(new Object[] {
        "1", //$NON-NLS-1$
        "2", //$NON-NLS-1$
        "3", //$NON-NLS-1$
        "4", //$NON-NLS-1$
        "5" //$NON-NLS-1$
    });

    private FieldListAssociationSupport associations;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        associations = new FieldListAssociationSupport();
    }

    // test:
    // 1 multiple associations for an association type
    // 2 pick list
    // 2a pick list sorting
    // 2b pick list null value handling
    // 3 data validation
    // 3b data validation null value handling

    public void testMultipleAllowed() {
        associations.addAllowedValues(LIST1);
        assertTrue(elementsAreInCollection(associations.getAllowed(), new Object[] {
            "A", //$NON-NLS-1$
            "E" //$NON-NLS-1$
        }));

        associations.addAllowedValues(Arrays.asList(new Object[] {
            "C", //$NON-NLS-1$
            "D", //$NON-NLS-1$
            "E" //$NON-NLS-1$
        }));
        assertTrue(elementsAreInCollection(associations.getAllowed(), new Object[] {
            "C", //$NON-NLS-1$
            "D", //$NON-NLS-1$
            "E" //$NON-NLS-1$
        }));
        assertTrue(elementsAreNotInCollection(associations.getAllowed(), new Object[] {
            "A", //$NON-NLS-1$
            "B" //$NON-NLS-1$
        }));

        associations.addAllowedValues(LIST2);
        assertTrue(associations.getAllowed().size() == 0);

        associations.addAllowedValues(Arrays.asList(new Object[] {
            "X" //$NON-NLS-1$
        }));
        assertTrue(associations.getAllowed().size() == 0);
    }

    public void testMultipleProhibited() {
        associations.addProhibitedValues(LIST1);
        assertTrue(elementsAreInCollection(associations.getProhibited(), new Object[] {
            "A", //$NON-NLS-1$
            "E" //$NON-NLS-1$
        }));

        associations.addProhibitedValues(LIST2);
        assertTrue(elementsAreInCollection(associations.getProhibited(), new Object[] {
            "A", //$NON-NLS-1$
            "E", //$NON-NLS-1$
            "1", //$NON-NLS-1$
            "5" //$NON-NLS-1$
        }));

        assertTrue(associations.getProhibited().size() == (LIST1.size() + LIST2.size()));
    }

    public void testMultipleSuggested() {
        associations.addSuggestedValues(LIST1);
        assertTrue(elementsAreInCollection(associations.getSuggested(), new Object[] {
            "A", //$NON-NLS-1$
            "E" //$NON-NLS-1$
        }));

        associations.addSuggestedValues(LIST2);
        assertTrue(elementsAreInCollection(associations.getSuggested(), new Object[] {
            "A", //$NON-NLS-1$
            "E", //$NON-NLS-1$
            "1", //$NON-NLS-1$
            "5" //$NON-NLS-1$
        }));

        assertTrue(associations.getSuggested().size() == (LIST1.size() + LIST2.size()));
    }

    public void testPickListS() {
        associations.addSuggestedValues(LIST1);
        assertTrue(elementsAreInCollection(associations.getPickList(), LIST1));
    }

    public void testPickListSA() {
        associations.addSuggestedValues(LIST1);
        associations.addAllowedValues(Arrays.asList(new Object[] {
            "A", //$NON-NLS-1$
            "B" //$NON-NLS-1$
        }));
        assertTrue(elementsAreInCollection(associations.getPickList(), new Object[] {
            "A", //$NON-NLS-1$
            "B" //$NON-NLS-1$
        }));
        assertTrue(elementsAreNotInCollection(associations.getPickList(), new Object[] {
            "C", //$NON-NLS-1$
            "D", //$NON-NLS-1$
            "E" //$NON-NLS-1$
        }));
    }

    public void testPickListSP() {
        associations.addSuggestedValues(LIST1);
        associations.addProhibitedValues(Arrays.asList(new Object[] {
            "A", //$NON-NLS-1$
            "B" //$NON-NLS-1$
        }));
        assertTrue(elementsAreNotInCollection(associations.getPickList(), new Object[] {
            "A", //$NON-NLS-1$
            "B" //$NON-NLS-1$
        }));
        assertTrue(elementsAreInCollection(associations.getPickList(), new Object[] {
            "C", //$NON-NLS-1$
            "D", //$NON-NLS-1$
            "E" //$NON-NLS-1$
        }));
    }

    public void testPickListSAP() {
        associations.addSuggestedValues(LIST1);
        associations.addAllowedValues(Arrays.asList(new Object[] {
            "A", //$NON-NLS-1$
            "B", //$NON-NLS-1$
            "C" //$NON-NLS-1$
        }));
        associations.addProhibitedValues(Arrays.asList(new Object[] {
            "D" //$NON-NLS-1$
        }));
        assertTrue(elementsAreNotInCollection(associations.getPickList(), new Object[] {
            "D", //$NON-NLS-1$
            "E" //$NON-NLS-1$
        }));
        assertTrue(elementsAreInCollection(associations.getPickList(), new Object[] {
            "A", //$NON-NLS-1$
            "B", //$NON-NLS-1$
            "C" //$NON-NLS-1$
        }));
    }

    public void testPickListEmpty() {
        assertTrue(associations.getPickList() == null);
    }

    public void testPickListA() {
        associations.addAllowedValues(LIST1);
        assertTrue(elementsAreInCollection(associations.getPickList(), LIST1));
    }

    public void testPickListP() {
        associations.addProhibitedValues(LIST1);
        assertTrue(associations.getPickList() == null);
    }

    public void testPickListAP() {
        associations.addAllowedValues(LIST1);
        associations.addProhibitedValues(Arrays.asList(new Object[] {
            "A", //$NON-NLS-1$
            "B" //$NON-NLS-1$
        }));
        assertTrue(elementsAreNotInCollection(associations.getPickList(), new Object[] {
            "A", //$NON-NLS-1$
            "B" //$NON-NLS-1$
        }));
        assertTrue(elementsAreInCollection(associations.getPickList(), new Object[] {
            "C", //$NON-NLS-1$
            "D", //$NON-NLS-1$
            "E" //$NON-NLS-1$
        }));
    }

    public void testPickListSorting() {
        associations.addAllowedValues(Arrays.asList(new Object[] {
            "A", //$NON-NLS-1$
            "Z", //$NON-NLS-1$
            "B", //$NON-NLS-1$
            "Y" //$NON-NLS-1$
        }));

        assertTrue(associations.getPickList().get(0).equals("A")); //$NON-NLS-1$
        assertTrue(associations.getPickList().get(1).equals("B")); //$NON-NLS-1$
        assertTrue(associations.getPickList().get(2).equals("Y")); //$NON-NLS-1$
        assertTrue(associations.getPickList().get(3).equals("Z")); //$NON-NLS-1$
    }

    public void testPickListNullValues() {
        associations.addAllowedValues(Arrays.asList(new String[] {
            "A", //$NON-NLS-1$
            "B", //$NON-NLS-1$
            "C", //$NON-NLS-1$
            null
        }));

        assertTrue(associations.isValueLegal("A")); //$NON-NLS-1$
        assertTrue(associations.isValueLegal(null));

        final List pickList = associations.getPickList();

        assertTrue(pickList.size() == 3);
        assertFalse(pickList.contains(null));

        associations.reset();

        associations.addAllowedValues(Arrays.asList(new String[] {
            null
        }));
        assertNull(associations.getPickList());
    }

    public void testValidationEmpty() {
        assertTrue(associations.isValueLegal("A")); //$NON-NLS-1$
    }

    public void testValidationA() {
        associations.addAllowedValues(LIST1);
        assertTrue(associations.isValueLegal("A")); //$NON-NLS-1$
        assertTrue(associations.isValueLegal("B")); //$NON-NLS-1$
        assertFalse(associations.isValueLegal("1")); //$NON-NLS-1$
        assertFalse(associations.isValueLegal("2")); //$NON-NLS-1$
    }

    public void testValidationP() {
        associations.addProhibitedValues(LIST1);
        assertFalse(associations.isValueLegal("A")); //$NON-NLS-1$
        assertFalse(associations.isValueLegal("B")); //$NON-NLS-1$
        assertTrue(associations.isValueLegal("1")); //$NON-NLS-1$
        assertTrue(associations.isValueLegal("2")); //$NON-NLS-1$
    }

    public void testValidationAP() {
        associations.addAllowedValues(LIST1);
        associations.addProhibitedValues(Arrays.asList(new Object[] {
            "D", //$NON-NLS-1$
            "E" //$NON-NLS-1$
        }));
        assertTrue(associations.isValueLegal("A")); //$NON-NLS-1$
        assertTrue(associations.isValueLegal("B")); //$NON-NLS-1$
        assertTrue(associations.isValueLegal("C")); //$NON-NLS-1$
        assertFalse(associations.isValueLegal("D")); //$NON-NLS-1$
        assertFalse(associations.isValueLegal("E")); //$NON-NLS-1$
    }

    public void testValidationNullValues() {
        associations.addAllowedValues(LIST1);
        assertFalse(associations.isValueLegal(null));

        associations.reset();
        associations.addAllowedValues(Arrays.asList(new String[] {
            "A", //$NON-NLS-1$
            null
        }));

        assertTrue(associations.isValueLegal("A")); //$NON-NLS-1$
        assertTrue(associations.isValueLegal(null));
    }

    private boolean elementsAreNotInCollection(final Collection collection, final Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            if (collection.contains(elements[i])) {
                return false;
            }
        }

        return true;
    }

    private boolean elementsAreInCollection(final Collection collection, final Collection elements) {
        return elementsAreInCollection(collection, elements.toArray());
    }

    private boolean elementsAreInCollection(final Collection collection, final Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            if (!collection.contains(elements[i])) {
                return false;
            }
        }

        return true;
    }
}
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.fields.ValuesCollection;

public class ValuesCollectionImpl implements ValuesCollection {
    private final List<String> list = new ArrayList<String>();

    public ValuesCollectionImpl(final String[] values, final int psType) {
        if (values != null && values.length > 0) {
            prepareList(values, psType);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return list.iterator();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public String get(final int index) {
        return list.get(index);
    }

    @Override
    public int indexOf(final String value) {
        for (int i = 0; i < list.size(); i++) {
            /*
             * I18N: need to use a java.text.Collator
             */
            if (list.get(i).equalsIgnoreCase(value)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean contains(final String value) {
        return indexOf(value) != -1;
    }

    @Override
    public String[] getValues() {
        return list.toArray(new String[list.size()]);
    }

    private void prepareList(final String[] values, final int psType) {
        /*
         * Yes, this looks strange. It's written this way for exact parity with
         * Visual Studio's implementation. This way I can easily compare between
         * the two. See: Microsoft.TeamFoundation.WorkItemTracking.Client.
         * AllowedValuesCollection .PrepareList
         */

        if (psType <= PSFieldDefinitionTypeEnum.SINGLE_VALUED_BOOLEAN) {
            if (psType <= PSFieldDefinitionTypeEnum.SINGLE_VALUED_DATE_TIME) {
                if (psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD) {
                    if (psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_INTEGER) {
                        if (psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_DATE_TIME) {
                            return;
                        }
                    } else {
                        prepareIntList(values);
                        return;
                    }
                }
                prepareStringList(values);
                return;
            }

            switch (psType) {
                case PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_PLAINTEXT:
                case PSFieldDefinitionTypeEnum.SINGLE_VALUED_GUID:
                case PSFieldDefinitionTypeEnum.TREE_NODE:
                    prepareStringList(values);
                    return;
            }
            if (psType == PSFieldDefinitionTypeEnum.SINGLE_VALUED_BOOLEAN) {
                return;
            }
            return;
        }

        if (psType <= PSFieldDefinitionTypeEnum.SINGLE_VALUED_INTEGER_TREEID) {
            if (psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_DOUBLE) {
                if (psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREEPATH) {
                    if (psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_INTEGER_TREEID) {
                        return;
                    }
                    prepareIntList(values);
                    return;
                }
                prepareStringList(values);
                return;
            }
            prepareDoubleList(values);
            return;
        }

        if (psType <= PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREE_NODE_NAME) {
            if (psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_HISTORY
                && psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREE_NODE_NAME) {
                return;
            }
        } else if (psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_HTML
            && psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREE_NODE_TYPE) {
            return;
        }

        prepareStringList(values);
        return;
    }

    private void prepareIntList(final String[] values) {
        final List<Integer> intList = new ArrayList<Integer>();
        final List<String> stringList = new ArrayList<String>();

        for (int i = 0; i < values.length; i++) {
            boolean parseSuccess = true;

            try {
                /*
                 * this is intended to be a non-Locale specific conversion: a
                 * java.text.NumberFormat should not be used
                 */
                intList.add(Integer.valueOf(values[i]));
            } catch (final NumberFormatException ex) {
                parseSuccess = false;
            }

            if (!parseSuccess) {
                stringList.add(values[i]);
            }
        }

        Collections.sort(intList);

        /*
         * I18N: need to use a java.text.Collator
         */
        Collections.sort(stringList, String.CASE_INSENSITIVE_ORDER);

        for (int i = 0; i < intList.size(); i++) {
            final Integer integer = intList.get(i);

            /*
             * I18N: need to use a java.text.NumberFormat
             */
            list.add(integer.toString());
        }

        for (int i = 0; i < stringList.size(); i++) {
            list.add(stringList.get(i));
        }
    }

    private void prepareStringList(final String[] values) {
        for (int i = 0; i < values.length; i++) {
            list.add(values[i]);
        }

        /*
         * I18N: need to use a java.text.Collator
         */
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
    }

    private void prepareDoubleList(final String[] values) {
        final List<Double> doubleList = new ArrayList<Double>();
        final List<String> stringList = new ArrayList<String>();

        for (int i = 0; i < values.length; i++) {
            boolean parseSuccess = true;

            try {
                /*
                 * this is intended to be a non-Locale specific conversion: a
                 * java.text.NumberFormat should not be used
                 */
                doubleList.add(Double.valueOf(values[i]));
            } catch (final NumberFormatException ex) {
                parseSuccess = false;
            }

            if (!parseSuccess) {
                stringList.add(values[i]);
            }
        }

        Collections.sort(doubleList);

        /*
         * I18N: need to use a java.text.Collator
         */
        Collections.sort(stringList, String.CASE_INSENSITIVE_ORDER);

        for (int i = 0; i < doubleList.size(); i++) {
            final Double doubleValue = doubleList.get(i);

            /*
             * I18N: need to use a java.text.NumberFormat
             */
            list.add(doubleValue.toString());
        }

        for (int i = 0; i < stringList.size(); i++) {
            list.add(stringList.get(i));
        }
    }
}

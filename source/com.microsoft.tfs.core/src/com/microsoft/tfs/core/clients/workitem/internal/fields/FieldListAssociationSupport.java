// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FieldListAssociationSupport {
    private Set allowed;
    private Set prohibited;
    private Set suggested;
    private List pickList;
    private boolean computedPickList = false;

    Set getAllowed() {
        return allowed;
    }

    Set getProhibited() {
        return prohibited;
    }

    Set getSuggested() {
        return suggested;
    }

    public void reset() {
        allowed = null;
        prohibited = null;
        suggested = null;
        computedPickList = false;
    }

    public void addProhibitedValues(final Collection values) {
        if (prohibited == null) {
            prohibited = new HashSet();
        }

        prohibited.addAll(values);
    }

    public void addAllowedValues(final Collection values) {
        if (allowed == null) {
            allowed = new HashSet();
            allowed.addAll(values);
        } else {
            allowed.retainAll(values);
        }
    }

    public void addSuggestedValues(final Collection values) {
        if (suggested == null) {
            suggested = new HashSet();
        }

        suggested.addAll(values);
    }

    public List getPickList() {
        if (!computedPickList) {
            computedPickList = true;

            if (suggested == null && allowed == null) {
                pickList = null;
            } else {
                pickList = new ArrayList();

                if (suggested != null) {
                    pickList.addAll(suggested);
                    if (allowed != null) {
                        pickList.retainAll(allowed);
                    }
                } else {
                    pickList.addAll(allowed);
                }

                if (prohibited != null) {
                    pickList.removeAll(prohibited);
                }

                /*
                 * null values are needed for data validation (implicit empty),
                 * but don't show them in the picklist
                 */
                pickList.remove(null);

                if (pickList.size() == 0) {
                    /*
                     * Size could be 0 if removing null values from the list
                     * removed all values. In this case there is no pick list.
                     */
                    pickList = null;
                } else {
                    Collections.sort(pickList);
                }
            }
        }

        return pickList;
    }

    public boolean isValidating() {
        return allowed != null || prohibited != null;
    }

    public boolean isValueLegal(final String value) {
        if (allowed == null && prohibited == null) {
            return true;
        } else if (allowed != null && prohibited == null) {
            return allowed.contains(value);
        } else if (allowed == null && prohibited != null) {
            return !prohibited.contains(value);
        } else {
            return allowed.contains(value) && !prohibited.contains(value);
        }
    }
}
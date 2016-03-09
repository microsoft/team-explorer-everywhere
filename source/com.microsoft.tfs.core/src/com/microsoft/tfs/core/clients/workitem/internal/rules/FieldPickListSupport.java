// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.fields.AllowedValuesCollection;
import com.microsoft.tfs.core.clients.workitem.fields.ValuesCollection;
import com.microsoft.tfs.core.clients.workitem.internal.fields.AllowedValuesCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.fields.ValuesCollectionImpl;

public class FieldPickListSupport implements IFieldPickListSupport {
    private static final Log log = LogFactory.getLog(FieldPickListSupport.class);

    private final String debuggingIdentifier;

    private Set<String> allowed;
    private Set<String> prohibited;
    private Set<String> suggested;
    private AllowedValuesCollection allowedValues;
    private ValuesCollection prohibitedValues;
    private final int psType;

    public FieldPickListSupport(final int psType, final String debuggingIdentifier) {
        this.psType = psType;
        this.debuggingIdentifier = debuggingIdentifier;
    }

    @Override
    public void reset() {
        allowed = null;
        prohibited = null;
        suggested = null;
        allowedValues = null;
        prohibitedValues = null;

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("{0}: reset picklist", debuggingIdentifier)); //$NON-NLS-1$
        }
    }

    @Override
    public void addProhibitedValues(final Collection<String> values) {
        if (prohibited == null) {
            prohibited = new HashSet<String>();
        }

        prohibited.addAll(values);

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format(
                "{0}: added {1} prohibited value(s) to picklist, prohibited size now = {2}", //$NON-NLS-1$
                debuggingIdentifier,
                values.size(),
                prohibited.size()));
        }
    }

    @Override
    public void addAllowedValues(final Collection<String> values) {
        if (allowed == null) {
            allowed = new HashSet<String>();
            allowed.addAll(values);
        } else {
            allowed.retainAll(values);
        }

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format(
                "{0}: added {1} allowed value(s) to picklist, allowed size now = {2}", //$NON-NLS-1$
                debuggingIdentifier,
                values.size(),
                allowed.size()));
        }
    }

    @Override
    public void addSuggestedValues(final Collection<String> values) {
        if (suggested == null) {
            suggested = new HashSet<String>();
        }

        suggested.addAll(values);

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format(
                "{0}: added {1} suggested value(s) to picklist, suggested size now = {2}", //$NON-NLS-1$
                debuggingIdentifier,
                values.size(),
                suggested.size()));
        }
    }

    public ValuesCollection getProhibitedValues() {
        if (prohibitedValues == null) {
            final List<String> prohibitedList = new ArrayList<String>();

            if (prohibited != null) {
                prohibitedList.addAll(prohibited);
            }

            final String[] values = prohibitedList.toArray(new String[prohibitedList.size()]);
            prohibitedValues = new ValuesCollectionImpl(values, psType);
        }

        return prohibitedValues;
    }

    public AllowedValuesCollection getAllowedValues() {
        if (allowedValues == null) {
            final List<String> pickList = new ArrayList<String>();

            if (suggested == null && allowed == null) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        MessageFormat.format(
                            "{0}: computed non-existing picklist, since suggested and allowed are null", //$NON-NLS-1$
                            debuggingIdentifier));
                }
            } else {
                if (suggested != null) {
                    pickList.addAll(suggested);

                    if (log.isDebugEnabled()) {
                        log.debug(
                            MessageFormat.format(
                                "{0}: computing picklist, added {1} suggested values, pick list size = {2}", //$NON-NLS-1$
                                debuggingIdentifier,
                                suggested.size(),
                                pickList.size()));
                    }

                    if (allowed != null) {
                        pickList.retainAll(allowed);

                        if (log.isDebugEnabled()) {
                            log.debug(
                                MessageFormat.format(
                                    "{0}: computing picklist, retained {1} allowed values, pick list size = {2}", //$NON-NLS-1$
                                    debuggingIdentifier,
                                    allowed.size(),
                                    pickList.size()));
                        }
                    }
                } else {
                    pickList.addAll(allowed);

                    if (log.isDebugEnabled()) {
                        log.debug(
                            MessageFormat.format(
                                "{0}: computing picklist, added {1} allowed values, pick list size = {2}", //$NON-NLS-1$
                                debuggingIdentifier,
                                allowed.size(),
                                pickList.size()));
                    }
                }

                if (prohibited != null) {
                    pickList.removeAll(prohibited);

                    if (log.isDebugEnabled()) {
                        log.debug(
                            MessageFormat.format(
                                "{0}: computing picklist, removed {1} prohibited values, pick list size = {2}", //$NON-NLS-1$
                                debuggingIdentifier,
                                prohibited.size(),
                                pickList.size()));
                    }
                }

                /*
                 * null values are needed for data validation (implicit empty),
                 * but don't show them in the picklist
                 *
                 * TODO: I think this is old code and can be removed. Pretty
                 * sure the RuleEngine never adds null to a FieldPickListSupport
                 * any more, but need to check this before removing this line.
                 */
                pickList.remove(null);

                /*
                 * NOTE: we used to check here if the pick list size was empty,
                 * and if so set to null. The behavior was that if a pick list
                 * is empty, no pick list exists.
                 *
                 * It seems that the MS OM does not do this - if a pick list is
                 * empty a field still has a pick list. Our code was changed to
                 * act the same way. The main implication is that on the work
                 * item form we show an empty combobox in this case instead of a
                 * text box.
                 */
            }

            /*
             * Create AllowedValueCollection from list.
             */
            final String[] values = pickList.toArray(new String[pickList.size()]);
            allowedValues = new AllowedValuesCollectionImpl(values, psType);
        }

        return allowedValues;
    }
}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NodeFieldName extends NodeItem {
    private final Log log = LogFactory.getLog(NodeFieldName.class);

    private Object tag;
    private DataType dataType;
    private Direction direction;
    private String prefix;

    public NodeFieldName(final NodeName n) {
        this(n.getValue());
        setStartOffset(n.getStartOffset());
        setEndOffset(n.getEndOffset());
    }

    public NodeFieldName(final String s) {
        super(NodeType.FIELD_NAME, s);
    }

    public NodeFieldName(final NodeName prefix, final NodeName n) {
        this(n.getValue());
        this.prefix = prefix.getValue();
        setStartOffset(n.getStartOffset());
        setEndOffset(n.getEndOffset());
        if ((prefix.getStartOffset() >= 0) && (prefix.getStartOffset() < getStartOffset())) {
            setStartOffset(prefix.getStartOffset());
        }
    }

    public NodeFieldName(final String prefix, final String s) {
        this(s);
        this.prefix = prefix;
    }

    @Override
    public void appendTo(final StringBuffer b) {
        if (prefix != null && !prefix.equals("")) //$NON-NLS-1$
        {
            Tools.AppendName(b, prefix);
            b.append("."); //$NON-NLS-1$
        }
        Tools.AppendName(b, getValue());
        if (direction == Direction.ASCENDING) {
            b.append(" asc"); //$NON-NLS-1$
        }
        if (direction == Direction.DESCENDING) {
            b.append(" desc"); //$NON-NLS-1$
        }
    }

    @Override
    public void bind(final IExternal e, final NodeTableName tableContext, final NodeFieldName fieldContext) {
        if (e != null) {
            final String name = getValue();
            final Object tableTag = (tableContext != null) ? tableContext.getTag() : null;
            tag = e.findField(name, prefix, tableTag);

            if (((tag == null) && (prefix == null)) && (tableTag != null)) {
                final int index = name.indexOf('.');
                if ((index > 0) && ((index + 1) < name.length())) {
                    final String prefix = name.substring(0, index);
                    final String str3 = name.substring(index + 1);
                    tag = e.findField(str3, prefix, tableTag);
                    if (tag != null) {
                        this.prefix = prefix;
                        setValue(str3);
                    }
                }
            }
            Tools.ensureSyntax(tag != null, SyntaxError.FIELD_DOES_NOT_EXIST_IN_THE_TABLE, this);
            dataType = e.getFieldDataType(tag);
            Tools.ensureSyntax(dataType != DataType.UNKNOWN, SyntaxError.UNKNOWN_FIELD_TYPE, this);
        }
        super.bind(e, tableContext, fieldContext);
    }

    @Override
    public String checkPrefix(String prefix) {
        String currentPrefix;
        if (this.prefix == null || this.prefix.length() == 0) {
            currentPrefix = ""; //$NON-NLS-1$
        } else {
            currentPrefix = this.prefix;
        }

        if (prefix == null) {
            // Prefix not set yet, all good.
            prefix = currentPrefix;
        } else {
            // If it is set - then should match our current prefix.
            if (!prefix.equalsIgnoreCase(currentPrefix)) {
                log.debug("Not equal prefixes"); //$NON-NLS-1$
            }

            Tools.ensureSyntax(prefix.equalsIgnoreCase(currentPrefix), SyntaxError.MIXED_PREFIXES, this);
        }
        return prefix;
    }

    @Override
    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(final Direction direction) {
        this.direction = direction;
    }

    @Override
    public boolean isConst() {
        return false;
    }

    public Object getTag() {
        return tag;
    }
}

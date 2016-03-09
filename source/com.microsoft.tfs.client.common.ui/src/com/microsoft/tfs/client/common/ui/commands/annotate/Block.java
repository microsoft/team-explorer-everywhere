// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands.annotate;

import java.text.MessageFormat;

import com.microsoft.tfs.client.common.ui.Messages;

public class Block {

    public Version v;

    public int start, end, prevStart, prevEnd;

    public Block(final Version v, final int start, final int end, final int prevStart, final int prevEnd) {
        this.v = v;
        this.start = start;
        this.end = end;
        this.prevStart = prevStart;
        this.prevEnd = prevEnd;
    }

    public Block[] intersect(final Block b) {
        final Block[] blocks = new Block[3];
        final int s = prevStart - b.start;
        final int e = b.end - prevEnd;
        if (s > 0) {
            final int newEnd = (b.end > prevStart) ? prevStart : b.end;
            blocks[0] = new Block(b.v, b.start, newEnd, b.prevStart, b.prevEnd);
        }
        if (e > 0) {
            final int newStart = b.start > prevEnd ? b.start : prevEnd;
            blocks[2] = new Block(b.v, newStart, b.end, b.prevStart, b.prevEnd);
        }
        return blocks;
    }

    @Override
    public String toString() {
        final String messageFormat = Messages.getString("Block.BlockToStringFormat"); //$NON-NLS-1$
        return MessageFormat.format(
            messageFormat,
            v,
            Integer.toString(start),
            Integer.toString(end),
            Integer.toString(prevStart),
            Integer.toString(prevEnd));
    }

}

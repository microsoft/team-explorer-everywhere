// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands.annotate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;

public class Version {

    public Changeset cs, prev;

    ArrayList blocks = new ArrayList();

    public Version(final Changeset cs, final Changeset prev, final String csFile, final String prevFile)
        throws FileNotFoundException,
            IOException {
        this.cs = cs;
        this.prev = prev;
        init(getComparator(cs, csFile), getComparator(prev, prevFile));
    }

    public Version(final Changeset cs, final String tmpFile) throws FileNotFoundException, IOException {
        this.cs = cs;
        prev = null;
        blocks.add(new Block(this, 0, getComparator(cs, tmpFile).getRangeCount(), 0, 0));
    }

    public void fold(final Version v) {
        final ArrayList adopted = new ArrayList();
        for (int i = 0; i < v.blocks.size(); i++) {
            Block b = (Block) v.blocks.get(i);
            for (int j = 0; j < blocks.size(); j++) {
                final Block block = (Block) blocks.get(j);
                final Block[] blocks = block.intersect(b);
                if (blocks[0] != null) {
                    adopted.add(blocks[0]);
                }
                b = blocks[2];
                if (b == null) {
                    break;
                }
            }
            if (b != null) {
                adopted.add(b);
            }
        }

        for (int i = 0; i < adopted.size(); i++) {
            adopt((Block) adopted.get(i));
        }
    }

    public void adopt(final Block b) {
        final int offset = offset(b);
        b.start += offset;
        b.end += offset;
        blocks.add(b);
    }

    public int offset(final Block b) {
        int offset = 0;
        for (int i = 0; i < blocks.size(); i++) {
            final Block block = (Block) blocks.get(i);
            if (block.v.equals(this) && b.start >= block.prevEnd) {
                offset += (block.end - block.start) - (block.prevEnd - block.prevStart);
            }
        }
        return offset;
    }

    private void init(final IRangeComparator left, final IRangeComparator right)
        throws FileNotFoundException,
            IOException {
        final RangeDifference[] diff = RangeDifferencer.findDifferences(left, right);
        for (int i = 0; i < diff.length; i++) {
            final RangeDifference r = diff[i];
            blocks.add(new Block(this, r.leftStart(), r.leftEnd(), r.rightStart(), r.rightEnd()));
        }

    }

    private LineComparator getComparator(final Changeset cs, final String tmpFile)
        throws FileNotFoundException,
            IOException {
        final Item item = cs.getChanges()[0].getItem();
        return new LineComparator(new FileInputStream(tmpFile), item.getEncoding().getName());
    }

    @Override
    public String toString() {
        final String messageFormat = Messages.getString("Version.VersionToStringFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, Integer.toString(cs.getChangesetID()));
    }

}

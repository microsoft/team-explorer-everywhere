// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import junit.framework.TestCase;
import ms.tfs.versioncontrol.clientservices._03._ChangeType;
import ms.tfs.versioncontrol.clientservices._03._ChangeType._ChangeType_Flag;

public class ChangeTypeTest extends TestCase {
    static class UnsafeFlag extends _ChangeType_Flag {
        public UnsafeFlag(final String name) {
            super(name);
        }
    }

    public void testConstructFromWebServiceObjectEmpty() {
        /*
         * Construction from an empty flag set should result in NONE.
         */
        final ChangeType ct = new ChangeType(new _ChangeType(new _ChangeType_Flag[0]), 0);
        assertEquals(ChangeType.NONE, ct);
        assertEquals(0, ct.getWebServiceObjectExtendedFlags());
    }

    public void testConstructFromWebServiceObjectUnknownFlags() {
        /*
         * Construction from ONLY unknown string flag names (perhaps from a
         * newer TFS server?) should result in NONE.
         */
        final ChangeType ct = new ChangeType(new _ChangeType(new _ChangeType_Flag[] {
            new UnsafeFlag("blah"), //$NON-NLS-1$
            new UnsafeFlag("zap") //$NON-NLS-1$
        }), 0);
        assertEquals(ChangeType.NONE, ct);
        assertEquals(0, ct.getWebServiceObjectExtendedFlags());

    }

    public void testConstructFromWebServiceObjectSomeKnownFlags() {
        /*
         * Construction from some unknown string flag names should only pick up
         * the known ones.
         */
        final ChangeType ct = new ChangeType(new _ChangeType(new _ChangeType_Flag[] {
            new UnsafeFlag("blah"), //$NON-NLS-1$
            _ChangeType_Flag.Rename
        }), 0);
        assertEquals(ChangeType.RENAME, ct);
        // Shift one to the right for the wire format
        assertEquals(ChangeType.RENAME.toIntFlags() >> 1, ct.getWebServiceObjectExtendedFlags());

    }

    public void testConstructFromWebServiceObjectExtended() {
        /*
         * Extended flags should be accessible.
         */

        // Value from the server
        final int rollbackExtendedValue = 512;

        ChangeType ct = new ChangeType(new _ChangeType(new _ChangeType_Flag[] {
            _ChangeType_Flag.Rename
        }), rollbackExtendedValue);

        assertEquals(ChangeType.RENAME.combine(ChangeType.ROLLBACK), ct);
        assertEquals(rollbackExtendedValue, ct.remove(ChangeType.RENAME).getWebServiceObjectExtendedFlags());

        /*
         * Multiple extended flags.
         */

        // Value from the server
        final int sourceRenameExtendedValue = 1024;

        ct = new ChangeType(new _ChangeType(new _ChangeType_Flag[] {
            _ChangeType_Flag.Rename
        }), rollbackExtendedValue | sourceRenameExtendedValue);

        assertEquals(ChangeType.RENAME.combine(ChangeType.ROLLBACK).combine(ChangeType.SOURCE_RENAME), ct);
        // Shift normal types' flags to the right for the wire format
        assertEquals(
            (ChangeType.RENAME.toIntFlags() >> 1) | rollbackExtendedValue | sourceRenameExtendedValue,
            ct.getWebServiceObjectExtendedFlags());
    }

    public void testConstructFromWebServiceObjectNone() {
        /*
         * None is special and should result in a NONE value.
         */
        final ChangeType ct = new ChangeType(new _ChangeType(new _ChangeType_Flag[] {
            _ChangeType_Flag.None
        }), 0);
        assertEquals(ChangeType.NONE, ct);
    }

    public void testConstructFromWebServiceObjectIgnoreAdditionalNone() {
        /*
         * The second flag, None, should be ignored in the presence of a
         * non-None flag.
         */
        final ChangeType ct = new ChangeType(new _ChangeType(new _ChangeType_Flag[] {
            _ChangeType_Flag.Lock,
            _ChangeType_Flag.None
        }), 0);
        assertEquals(ChangeType.LOCK, ct);
    }

    public void testConstructFromWebServiceSingle() {
        /*
         * Handle one flag.
         */
        final ChangeType ct = new ChangeType(new _ChangeType(new _ChangeType_Flag[] {
            _ChangeType_Flag.Merge,
        }), 0);
        assertEquals(ChangeType.MERGE, ct);
    }

    public void testConstructFromWebServiceMultiple() {
        /*
         * Handle multiple.
         */
        final ChangeType ct = new ChangeType(new _ChangeType(new _ChangeType_Flag[] {
            _ChangeType_Flag.Add,
            _ChangeType_Flag.Edit
        }), 0);
        assertEquals(ChangeType.ADD.combine(ChangeType.EDIT), ct);
    }

    public void testGetWebServiceObjectEmpty() {
        /*
         * The web service object should contain NONE.
         */
        final ChangeType ct = ChangeType.EDIT.remove(ChangeType.EDIT);

        assertEquals(new _ChangeType(new _ChangeType_Flag[] {
            _ChangeType_Flag.None
        }), ct.getWebServiceObject());
    }

    public void testGetWebServiceObjectSingle() {
        /*
         * The web service object should describe the same flag.
         */
        final ChangeType ct = ChangeType.EDIT;
        assertEquals(new _ChangeType(new _ChangeType_Flag[] {
            _ChangeType_Flag.Edit
        }), ct.getWebServiceObject());
    }

    public void testGetWebServiceObjectMultiple() {
        /*
         * The web service object should describe the same flags.
         */
        final ChangeType ct = ChangeType.EDIT.combine(ChangeType.ENCODING);
        assertEquals(new _ChangeType(new _ChangeType_Flag[] {
            _ChangeType_Flag.Edit,
            _ChangeType_Flag.Encoding
        }), ct.getWebServiceObject());
    }
}

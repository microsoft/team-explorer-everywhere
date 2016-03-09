// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.link;

/**
 * Enumeration of the allowed topology types.
 *
 * @since TEE-SDK-10.1
 */
public class Topology {
    private static final int TOPOLOGY_MASK = 28;

    private static final int UNKNOWN_VALUE = -1;
    private static final int NETWORK_VALUE = 0;
    private static final int DIRECTED_NETWORK_VALUE = 4;
    private static final int DEPENDENCY_VALUE = 12;
    private static final int TREE_VALUE = 28;

    public static final Topology UNKNOWN = new Topology("Unknown", UNKNOWN_VALUE); //$NON-NLS-1$
    public static final Topology NETWORK = new Topology("Network", NETWORK_VALUE); //$NON-NLS-1$
    public static final Topology DIRECTED_NETWORK = new Topology("DirectedNetwork", DIRECTED_NETWORK_VALUE); //$NON-NLS-1$
    public static final Topology DEPENDENCY = new Topology("Dependency", DEPENDENCY_VALUE); //$NON-NLS-1$
    public static final Topology TREE = new Topology("Tree", TREE_VALUE); //$NON-NLS-1$

    private final String name;
    private final int value;

    private Topology(final String name, final int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Topology other = (Topology) obj;
        if (value != other.value) {
            return false;
        }
        return true;
    }

    public static Topology getTopology(final int topology) {
        switch (topology & TOPOLOGY_MASK) {
            case Topology.NETWORK_VALUE:
                return Topology.NETWORK;
            case Topology.DIRECTED_NETWORK_VALUE:
                return Topology.DIRECTED_NETWORK;
            case Topology.DEPENDENCY_VALUE:
                return Topology.DEPENDENCY;
            case Topology.TREE_VALUE:
                return Topology.TREE;
            default:
                return Topology.UNKNOWN;
        }
    }

}

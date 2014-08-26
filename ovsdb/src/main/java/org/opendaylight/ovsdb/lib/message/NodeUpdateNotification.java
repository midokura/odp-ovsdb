/*
 * Copyright (c) 2014 Midokura SARL, All Rights Reserved.
 */
package org.opendaylight.ovsdb.lib.message;

import org.opendaylight.controller.sal.core.Node;

public class NodeUpdateNotification {
    public final Node node;
    public final TableUpdates tableUpdates;

    public NodeUpdateNotification(Node node, TableUpdates tableUpdates) {
        this.node = node;
        this.tableUpdates = tableUpdates;
    }
}

/*
 * Copyright (C) 2013 Ebay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.table.internal;

import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Capability;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.Controller;
import org.opendaylight.ovsdb.lib.table.Manager;
import org.opendaylight.ovsdb.lib.table.Mirror;
import org.opendaylight.ovsdb.lib.table.NetFlow;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Qos;
import org.opendaylight.ovsdb.lib.table.Queue;
import org.opendaylight.ovsdb.lib.table.SFlow;
import org.opendaylight.ovsdb.lib.table.SSL;
import org.opendaylight.ovsdb.lib.table.Flow_Sample_Collector_Set;
import org.opendaylight.ovsdb.lib.table.Flow_Table;
import org.opendaylight.ovsdb.lib.table.IPFIX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tables {
    private static Map<String, List<Table>> dbTables = new HashMap<>();

    static {
        List<Table> ovsTables = new ArrayList<>();
        dbTables.put(Open_vSwitch.NAME.getName(), ovsTables);
        ovsTables.add(new Bridge());
        ovsTables.add(new Port());
        ovsTables.add(new Capability());
        ovsTables.add(new Interface());
        ovsTables.add(new Controller());
        ovsTables.add(new Manager());
        ovsTables.add(new Mirror());
        ovsTables.add(new NetFlow());
        ovsTables.add(new Open_vSwitch());
        ovsTables.add(new Qos());
        ovsTables.add(new Queue());
        ovsTables.add(new SFlow());
        ovsTables.add(new SSL());
        ovsTables.add(new Flow_Sample_Collector_Set());
        ovsTables.add(new Flow_Table());
        ovsTables.add(new IPFIX());
    }

    public static List<Table> getTables(String dbName) {
        List<Table> tables = dbTables.get(dbName);
        return (tables == null) ? new ArrayList<Table>() : tables;
    }
}

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.opendaylight.ovsdb.lib.table.vtep.Global;
import org.opendaylight.ovsdb.lib.table.vtep.Logical_Switch;
import org.opendaylight.ovsdb.lib.table.vtep.Mcast_Macs_Local;
import org.opendaylight.ovsdb.lib.table.vtep.Mcast_Macs_Remote;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Locator;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Locator_Set;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Port;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Switch;
import org.opendaylight.ovsdb.lib.table.vtep.Ucast_Macs_Local;
import org.opendaylight.ovsdb.lib.table.vtep.Ucast_Macs_Remote;

public class Tables {
    private static Map<String, List<Table>> dbTables = new HashMap<>();

    static {
        List<Table> ovsTables = new ArrayList<>();
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

        List<Table> vtepTables = new ArrayList<>();
        vtepTables.add(new Global());
        vtepTables.add(new Manager());
        vtepTables.add(new Physical_Switch());
        vtepTables.add(new Physical_Locator());
        vtepTables.add(new Physical_Locator_Set());
        vtepTables.add(new Physical_Port());
        vtepTables.add(new Logical_Switch());
        vtepTables.add(new Mcast_Macs_Local());
        vtepTables.add(new Mcast_Macs_Remote());
        vtepTables.add(new Ucast_Macs_Local());
        vtepTables.add(new Ucast_Macs_Remote());

        dbTables.put(Open_vSwitch.NAME.getName(), ovsTables);
        dbTables.put("hardware_vtep", vtepTables);
    }

    public static List<Table> getTables(String dbName) {
        List<Table> tables = dbTables.get(dbName);
        return (tables == null) ? new ArrayList<Table>() : tables;
    }
}

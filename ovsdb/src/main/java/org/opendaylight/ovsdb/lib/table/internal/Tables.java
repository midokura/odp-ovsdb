package org.opendaylight.ovsdb.lib.table.internal;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.ovsdb.lib.table.*;
import org.opendaylight.ovsdb.lib.table.vtep.Global;
import org.opendaylight.ovsdb.lib.table.vtep.Logical_Switch;
import org.opendaylight.ovsdb.lib.table.vtep.Mcast_Macs_Local;
import org.opendaylight.ovsdb.lib.table.vtep.Mcast_Macs_Remote;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Locator;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Port;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Switch;
import org.opendaylight.ovsdb.lib.table.vtep.Ucast_Macs_Local;
import org.opendaylight.ovsdb.lib.table.vtep.Ucast_Macs_Remote;

public class Tables {
    public static List<Table> tables = new ArrayList<Table>();
    static {
        tables.add(new Bridge());
        tables.add(new Port());
        tables.add(new Capability());
        tables.add(new Interface());
        tables.add(new Controller());
        tables.add(new Manager());
        tables.add(new Mirror());
        tables.add(new NetFlow());
        tables.add(new Open_vSwitch());
        tables.add(new Qos());
        tables.add(new Queue());
        tables.add(new SFlow());
        tables.add(new SSL());
        // VTEP
        tables.add(new Global());
        tables.add(new Manager());
        tables.add(new Physical_Switch());
        tables.add(new Physical_Locator());
        tables.add(new Physical_Port());
        tables.add(new Logical_Switch());
        tables.add(new Mcast_Macs_Local());
        tables.add(new Mcast_Macs_Remote());
        tables.add(new Ucast_Macs_Local());
        tables.add(new Ucast_Macs_Remote());
    }
    public static List<Table> getTables() {
        return tables;
    }
}

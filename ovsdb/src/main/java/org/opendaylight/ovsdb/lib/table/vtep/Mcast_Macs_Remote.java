package org.opendaylight.ovsdb.lib.table.vtep;

import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Mcast_Macs_Remote extends Table<Mcast_Macs_Remote> {

    public static final Name<Mcast_Macs_Remote> NAME =
        new Name<Mcast_Macs_Remote>("Mcast_Macs_Remote"){};

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Mcast_Macs_Remote> {
        mac,
        logical_switch,
        locator_set,
        ipaddr
    }

    private String mac;
    private UUID logical_switch;
    private UUID locator_set;
    private String ipaddr;

    @Override
    public Name<Mcast_Macs_Remote> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Mcast_Macs_Remote [mac=");
        sb.append(mac).append(", logical_switch=");
        sb.append(logical_switch).append(", locator_set=");
        sb.append(locator_set).append(", ipaddr=");
        sb.append(ipaddr).append("]");
        return sb.toString();
    }

}

package org.opendaylight.ovsdb.lib.table.vtep;

import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Mcast_Macs_Local extends Table<Mcast_Macs_Local> {

    public static final Name<Mcast_Macs_Local> NAME =
        new Name<Mcast_Macs_Local>("Mcast_Macs_Local"){};

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Mcast_Macs_Local> {
        mac,
        logical_switch,
        locator_set
    }

    private String mac;
    private UUID logical_switch;
    private UUID locator_set;

    @Override
    @JsonIgnore
    public Name<Mcast_Macs_Local> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Mcast_Macs_Local [mac=");
        sb.append(mac).append(", logical_switch=");
        sb.append(logical_switch).append(", locator_set=");
        sb.append(locator_set).append("]");
        return sb.toString();
    }

}

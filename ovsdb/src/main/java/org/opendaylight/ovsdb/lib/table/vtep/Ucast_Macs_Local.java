package org.opendaylight.ovsdb.lib.table.vtep;

import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Ucast_Macs_Local extends Table<Ucast_Macs_Local> {

    public static final Name<Ucast_Macs_Local> NAME =
        new Name<Ucast_Macs_Local>("Ucast_Macs_Local"){};

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Ucast_Macs_Local> {
        mac,
        logical_switch,
        locator,
        ipaddr
    }

    private String mac;
    private UUID logical_switch;
    private UUID locator;
    private String ipaddr;

    @Override
    public Name<Ucast_Macs_Local> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Ucast_Macs_Local [mac=");
        sb.append(mac).append(", logical_switch=");
        sb.append(logical_switch).append(", locator=");
        sb.append(locator).append(", ipaddr=");
        sb.append(ipaddr).append("]");
        return sb.toString();
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public UUID getLogical_switch() {
        return logical_switch;
    }

    public void setLogical_switch(UUID logical_switch) {
        this.logical_switch = logical_switch;
    }

    public UUID getLocator() {
        return locator;
    }

    public void setLocator(UUID locator) {
        this.locator = locator;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }
}

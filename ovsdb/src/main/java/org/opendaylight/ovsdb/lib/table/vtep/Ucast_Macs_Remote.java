package org.opendaylight.ovsdb.lib.table.vtep;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Ucast_Macs_Remote extends Table<Ucast_Macs_Remote> {

    public static final Name<Ucast_Macs_Remote> NAME =
        new Name<Ucast_Macs_Remote>("Ucast_Macs_Remote"){};

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Ucast_Macs_Remote> {
        mac,
        logical_switch,
        locator,
        ipaddr
    }

    @JsonProperty(value="MAC")
    private String mac;
    private OvsDBSet<UUID> logical_switch;
    private OvsDBSet<UUID> locator;
    private String ipaddr;

    @Override
    @JsonIgnore
    public Name<Ucast_Macs_Remote> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Ucast_Macs_Remote [mac=");
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

    public OvsDBSet<UUID> getLogical_switch() {
        return logical_switch;
    }

    public void setLogical_switch(OvsDBSet<UUID> logical_switch) {
        this.logical_switch = logical_switch;
    }

    public OvsDBSet<UUID> getLocator() {
        return locator;
    }

    public void setLocator(OvsDBSet<UUID> locator) {
        this.locator = locator;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }
}

package org.opendaylight.ovsdb.lib.table.vtep;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Mcast_Macs_Local extends Table<Mcast_Macs_Local> {

    public static final Name<Mcast_Macs_Local> NAME =
        new Name<Mcast_Macs_Local>("Mcast_Macs_Local"){};

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Mcast_Macs_Local> {
        mac,
        logical_switch,
        ipaddr,
        locator_set
    }

    @JsonProperty(value="MAC")
    private String mac;
    private OvsDBSet<UUID> logical_switch;
    private OvsDBSet<UUID> locator_set;
    private String ipaddr;

    @Override
    @JsonIgnore
    public Name<Mcast_Macs_Local> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Mcast_Macs_Local [mac=");
        sb.append(mac).append(", logical_switch=");
        sb.append(logical_switch).append(", locator_set=");
        sb.append(ipaddr).append(", ipaddr=");
        sb.append(locator_set).append("]");
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

    public OvsDBSet<UUID> getLocator_set() {
        return locator_set;
    }

    public void setLocator_set(OvsDBSet<UUID> locator_set) {
        this.locator_set = locator_set;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

}

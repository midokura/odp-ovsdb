package org.opendaylight.ovsdb.lib.table.vtep;

import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Physical_Switch extends Table<Physical_Switch> {

    public static final Name<Physical_Switch> NAME =
        new Name<Physical_Switch>("Physical_Switch"){};

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Physical_Switch> {
        description,
        name,
        management_ips,
        ports,
        switchFaultStatus,
        tunnel_ips
    }

    private String description;
    private String name;
    private OvsDBSet<String> management_ips;
    private OvsDBSet<UUID> ports;
    private OvsDBSet<String> switch_fault_status;
    private OvsDBSet<String> tunnel_ips;

    @Override
    @JsonIgnore
    public Name<Physical_Switch> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Physical_Switch [name=");
        sb.append(name).append(", description=");
        sb.append(description).append(", management_ips=");
        sb.append(management_ips).append(", ports=");
        sb.append(ports).append(", switch_fault_status=");
        sb.append(switch_fault_status).append(", tunnel_ips=");
        sb.append(tunnel_ips).append("]");
        return sb.toString();
    }

    public String getDescription() { return description; }

    public void setDescription(String desc) { this.description = desc; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public OvsDBSet<String> getManagement_ips() { return management_ips; }

    public void setManagement_ips(OvsDBSet<String> ips) {
        this.management_ips = ips;
    }

    public OvsDBSet<UUID> getPorts() { return ports; }

    public void setPorts(OvsDBSet<UUID> ports) { this.ports = ports; }

    public OvsDBSet<String> getSwitch_fault_status() {
        return switch_fault_status;
    }

    public void setSwitch_fault_status(OvsDBSet<String> status) {
        this.switch_fault_status = status;
    }

    public OvsDBSet<String> getTunnel_ips() { return tunnel_ips; }

    public void setTunnel_ips(OvsDBSet<String> ips) { this.tunnel_ips = ips; }
}

package org.opendaylight.ovsdb.lib.table.vtep;

import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Physical_Port extends Table<Physical_Port> {

    public static final Name<Physical_Port> NAME =
        new Name<Physical_Port>("Physical_Port"){};

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Physical_Port> {
        description,
        name,
        port_fault_status,
        vlan_bindings,
        vlan_stats
    }

    private String description;
    private String name;
    private OvsDBSet<String> port_fault_status;
    private OvsDBMap<Integer, UUID> vlan_bindings;
    private OvsDBMap<Integer, UUID> vlan_stats;

    @Override
    public Name<Physical_Port> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Physical_Port [name=");
        sb.append(name).append(", description=");
        sb.append(description).append(", port_fault_status=");
        sb.append(port_fault_status).append(", vlan_bindings=");
        sb.append(vlan_bindings).append(", vlan_stats=");
        sb.append(vlan_stats).append("]");
        return sb.toString();
    }

    public String getDescription() { return description; }

    public void setDescription(String desc) { this.description = desc; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public OvsDBSet<String> getPort_fault_status() { return port_fault_status; }

    public void setPort_fault_status(OvsDBSet<String> status) {
        this.port_fault_status = port_fault_status;
    }

    public OvsDBMap<Integer, UUID> getVlan_bindings() { return vlan_bindings; }

    public void setVlan_bindings(OvsDBMap<Integer, UUID> bindings) {
        this.vlan_bindings = bindings;
    }

    public OvsDBMap<Integer, UUID> getVlan_stats() { return vlan_stats; }

    public void setVlan_stats(OvsDBMap<Integer, UUID> vlan_stats) {
        this.vlan_stats = vlan_stats;
    }

}

package org.opendaylight.ovsdb.lib.table.vtep;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Logical_Switch extends Table<Logical_Switch> {

    public static final Table.Name<Logical_Switch> NAME =
        new Name<Logical_Switch>("Logical_Switch"){};

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Logical_Switch> {
        description,
        name,
        tunnel_key
    }

    private String description;
    private String name;
    // It's a plain int in the documentation but the emulator has an array
    private OvsDBSet<Integer> tunnel_key;

    @Override
    @JsonIgnore
    public Name<Logical_Switch> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Logical_Switch [name=");
        sb.append(name).append(", description=");
        sb.append(description).append(", tunnel_key=");
        sb.append(tunnel_key).append("]");
        return sb.toString();
    }


    public String getDescription() { return description; }

    public void setDescription(String desc) { this.description = desc; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public OvsDBSet<Integer> getTunnel_key() { return tunnel_key; }

    public void setTunnel_key(OvsDBSet<Integer> key) { this.tunnel_key = key; }

}

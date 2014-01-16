package org.opendaylight.ovsdb.lib.table.vtep;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Global extends Table<Global> {

    public static final Name<Global> NAME = new Table.Name<Global>("Global"){};

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Global> {
        managers,
        switches
    }

    private OvsDBSet<UUID> managers = new OvsDBSet<>();
    private OvsDBSet<UUID> switches = new OvsDBSet<>();

    @Override
    @JsonIgnore
    public Name<Global> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Global [managers=");
        sb.append(managers).append(", switches=");
        sb.append(switches).append("]");
        return sb.toString();
    }

    public OvsDBSet<UUID> getManagers() {
        return managers;
    }

    public void setManagers(OvsDBSet<UUID> managers) {
        this.managers = managers;
    }

    public OvsDBSet<UUID> getSwitches() {
        return switches;
    }

    public void setSwitches(OvsDBSet<UUID> switches) {
        this.switches = switches;
    }

}

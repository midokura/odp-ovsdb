package org.opendaylight.ovsdb.lib.table.vtep;

import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Physical_Locator_Set extends Table<Physical_Locator_Set> {

    public static final Name<Physical_Locator_Set> NAME =
        new Name<Physical_Locator_Set>("Physical_Locator_Set"){};

    // TODO: support bfd
    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Physical_Locator_Set> {
        locators
    }

    private OvsDBSet<UUID> locators;

    @Override
    @JsonIgnore
    public Name<Physical_Locator_Set> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Physical_Locator_Set [locators=");
        sb.append(locators).append("]");
        return sb.toString();
    }

    public OvsDBSet<UUID> getLocators() {
        return locators;
    }

    public void setLocators(OvsDBSet<UUID> locators) {
        this.locators = locators;
    }

}

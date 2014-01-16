package org.opendaylight.ovsdb.lib.table.vtep;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Physical_Locator extends Table<Physical_Locator> {

    public static final Name<Physical_Locator> NAME =
        new Name<Physical_Locator>("Physical_Locator"){};

    // TODO: support bfd
    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Physical_Locator> {
        dst_ip, encapsulation_type
    }

    private String encapsulation_type;
    private String dst_ip;

    @Override
    @JsonIgnore
    public Name<Physical_Locator> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Physical_Locator [dst_ip=");
        sb.append(dst_ip).append(", encapsulation_type=");
        sb.append(encapsulation_type).append("]");
        return sb.toString();
    }

    public String getEncapsulation_type() {
        return encapsulation_type;
    }

    public void setEncapsulation_type(String type) {
        this.encapsulation_type = type;
    }

    public String getDst_ip() {
        return dst_ip;
    }

    public void setDst_ip(String dst_ip) {
        this.dst_ip = dst_ip;
    }

}

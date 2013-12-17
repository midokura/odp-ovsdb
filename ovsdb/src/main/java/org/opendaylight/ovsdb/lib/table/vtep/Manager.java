package org.opendaylight.ovsdb.lib.table.vtep;

import org.opendaylight.ovsdb.lib.table.internal.Table;

public class Manager extends Table<Manager> {

    public static final Name<Manager> NAME =
        new Name<Manager>("Manager"){};

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Manager> {
        target,
        max_backoff,
        inactivity_probe,
        is_connected,
        last_error,
        state,
        see_since_connect,
        see_since_disconnect,
        locks_held,
        locks_waiting,
        locks_lost,
        n_connections,
        dscp
    }

    private String target;
    private Integer max_backoff;
    private Integer inactivity_probe;
    private Boolean is_connected;
    private String last_error;
    private String state;
    private String see_since_connect;
    private String see_since_disconnect;
    private String locks_held;
    private String locks_waiting;
    private String locks_lost;
    private String n_connections;
    private String dscp;

    @Override
    public Name<Manager> getTableName() { return NAME; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Manager [target=");
        sb.append(target).append(", max_backoff=");
        sb.append(max_backoff).append(", inactivity_probe=");
        sb.append(inactivity_probe).append(", is_connected=");
        sb.append(is_connected).append(", last_error=");
        sb.append(last_error).append(", state=");
        sb.append(state).append(", see_since_connect=");
        sb.append(see_since_connect).append(", see_since_disconnect=");
        sb.append(see_since_disconnect).append(", locks_held=");
        sb.append(locks_held).append(", locks_waiting=");
        sb.append(locks_waiting).append(", locks_lost=");
        sb.append(locks_lost).append(", n_connections=");
        sb.append(n_connections).append(", dscp=");
        sb.append(dscp).append("]");
        return sb.toString();
    }


    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Integer getMax_backoff() {
        return max_backoff;
    }

    public void setMax_backoff(Integer max_backoff) {
        this.max_backoff = max_backoff;
    }

    public Integer getInactivity_probe() {
        return inactivity_probe;
    }

    public void setInactivity_probe(Integer inactivity_probe) {
        this.inactivity_probe = inactivity_probe;
    }

    public Boolean getIs_connected() {
        return is_connected;
    }

    public void setIs_connected(Boolean is_connected) {
        this.is_connected = is_connected;
    }

    public String getLast_error() {
        return last_error;
    }

    public void setLast_error(String last_error) {
        this.last_error = last_error;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSee_since_connect() {
        return see_since_connect;
    }

    public void setSee_since_connect(String see_since_connect) {
        this.see_since_connect = see_since_connect;
    }

    public String getSee_since_disconnect() {
        return see_since_disconnect;
    }

    public void setSee_since_disconnect(String see_since_disconnect) {
        this.see_since_disconnect = see_since_disconnect;
    }

    public String getLocks_held() {
        return locks_held;
    }

    public void setLocks_held(String locks_held) {
        this.locks_held = locks_held;
    }

    public String getLocks_waiting() {
        return locks_waiting;
    }

    public void setLocks_waiting(String locks_waiting) {
        this.locks_waiting = locks_waiting;
    }

    public String getLocks_lost() {
        return locks_lost;
    }

    public void setLocks_lost(String locks_lost) {
        this.locks_lost = locks_lost;
    }

    public String getN_connections() {
        return n_connections;
    }

    public void setN_connections(String n_connections) {
        this.n_connections = n_connections;
    }

    public String getDscp() {
        return dscp;
    }

    public void setDscp(String dscp) {
        this.dscp = dscp;
    }

}

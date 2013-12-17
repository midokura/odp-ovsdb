package org.opendaylight.ovsdb.lib.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.table.*;
import org.opendaylight.ovsdb.lib.table.Manager;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.lib.table.vtep.*;


public  class TableUpdates extends Response {

    Map<Table.Name, TableUpdate> map = Maps.newHashMap();

    public Set<Table.Name> availableUpdates() {
        return map.keySet();
    }

    @SuppressWarnings("unchecked")
    public <T extends Table> TableUpdate<T> getUpdate(Table.Name<T> name) {
        return map.get(name);
    }


    private <T extends Table> void put(Table.Name<T> name, TableUpdate<T> update) {
        map.put(name, update);
    }


    @JsonProperty("Interface")
    public TableUpdate<Interface> getInterfaceUpdate() {
        return getUpdate(Interface.NAME);
    }

    public void setInterfaceUpdate(TableUpdate<Interface> interfaceUpdate) {
        put(Interface.NAME, interfaceUpdate);
    }

    @JsonProperty("Bridge")
    TableUpdate<Bridge> getBridgeUpdate() {
        return getUpdate(Bridge.NAME);
    }

    public void setBridgeUpdate(TableUpdate<Bridge> bridgeUpdate) {
        put(Bridge.NAME, bridgeUpdate);
    }

    @JsonProperty("Port")
    TableUpdate<Port> getPortUpdate() {
        return getUpdate(Port.NAME);
    }

    void setPortUpdate(TableUpdate<Port> portUpdate) {
        put(Port.NAME, portUpdate);
    }

    @JsonProperty("Capability")
    public TableUpdate<Capability> getCapabilityUpdate() {
        return getUpdate(Capability.NAME);
    }

    public void setCapabilityUpdate(TableUpdate<Capability> capabilityUpdate) {
        put(Capability.NAME, capabilityUpdate);
    }

    @JsonProperty("Controller")
    public TableUpdate<Controller> getControllerUpdate() {
        return getUpdate(Controller.NAME);
    }

    public void setControllerUpdate(TableUpdate<Controller> controllerUpdate) {
        put(Controller.NAME, controllerUpdate);
    }

    @JsonProperty("Manager")
    public TableUpdate<Manager> getManagerUpdate() {
        return getUpdate(Manager.NAME);
    }

    public void setManagerUpdate(TableUpdate<Manager> managerUpdate) {
        put(Manager.NAME, managerUpdate);
    }

    @JsonProperty("Mirror")
    public TableUpdate<Mirror> getMirrorUpdate() {
        return getUpdate(Mirror.NAME);
    }

    public void setMirrorUpdate(TableUpdate<Mirror> mirrorUpdate) {
        put(Mirror.NAME, mirrorUpdate);
    }

    @JsonProperty("NetFlow")
    public TableUpdate<NetFlow> getNetFlowUpdate() {
        return getUpdate(NetFlow.NAME);
    }

    public void setNetFlowUpdate(TableUpdate<NetFlow> netFlowUpdate) {
        put(NetFlow.NAME, netFlowUpdate);
    }

    @JsonProperty("Open_vSwitch")
    public TableUpdate<Open_vSwitch> getOpen_vSwitchUpdate() {
        return getUpdate(Open_vSwitch.NAME);
    }

    public void setOpen_vSwitchUpdate(TableUpdate<Open_vSwitch> openVSwitchUpdate) {
        put(Open_vSwitch.NAME, openVSwitchUpdate);
    }

    @JsonProperty("QoS")
    public TableUpdate<Qos> getQosUpdate() {
        return getUpdate(Qos.NAME);
    }

    public void setQosUpdate(TableUpdate<Qos> qosUpdate) {
        put(Qos.NAME, qosUpdate);
    }

    @JsonProperty("Queue")
    public TableUpdate<Queue> getQueueUpdate() {
        return getUpdate(Queue.NAME);
    }

    public void setQueueUpdate(TableUpdate<Queue> queueUpdate) {
        put(Queue.NAME, queueUpdate);
    }

    @JsonProperty("sFlow")
    public TableUpdate<SFlow> getSFlowUpdate() {
        return getUpdate(SFlow.NAME);
    }

    public void setSFlowUpdate(TableUpdate<SFlow> sFlowUpdate) {
        put(SFlow.NAME, sFlowUpdate);
    }

    @JsonProperty("SSL")
    public TableUpdate<SSL> getSSLUpdate() {
        return getUpdate(SSL.NAME);
    }

    public void setSSLUpdate(TableUpdate<SSL> sslUpdate) {
        put(SSL.NAME, sslUpdate);
    }

    // VTEP

    @JsonProperty("Global")
    public TableUpdate<Global> getGlobalUpdate() {
        return getUpdate(Global.NAME);
    }

    public void setGlobalUpdate(TableUpdate<Global> update) {
        put(Global.NAME, update);
    }

    @JsonProperty("Physical_Locator")
    public TableUpdate<Physical_Locator> getPhysicalLocatorUpdate() {
        return getUpdate(Physical_Locator.NAME);
    }

    public void setPhysicalLocatorUpdate(TableUpdate<Physical_Locator> update) {
        put(Physical_Locator.NAME, update);
    }

    @JsonProperty("Physical_Switch")
    public TableUpdate<Physical_Switch> getPhysicalSwitchUpdate() {
        return getUpdate(Physical_Switch.NAME);
    }

    public void setPhysicalSwitchUpdate(TableUpdate<Physical_Switch> update) {
        put(Physical_Switch.NAME, update);
    }

    @JsonProperty("Physical_Port")
    public TableUpdate<Physical_Port> getPhysicalPortUpdate() {
        return getUpdate(Physical_Port.NAME);
    }

    public void setPhysicalPortUpdate(TableUpdate<Physical_Port> update) {
        put(Physical_Port.NAME, update);
    }

    @JsonProperty("Logical_Switch")
    public TableUpdate<Logical_Switch> getLogicalSwitchUpdate() {
        return getUpdate(Logical_Switch.NAME);
    }

    public void setLogicalSwitchUpdate(TableUpdate<Logical_Switch> update) {
        put(Logical_Switch.NAME, update);
    }

    @JsonProperty("Physical_Locator_Set")
    public TableUpdate<Physical_Locator_Set> getPhysicalLocatorSetUpdate() {
        return getUpdate(Physical_Locator_Set.NAME);
    }

    public void setPhysicalLocatorSetUpdate(TableUpdate<Physical_Locator_Set> update) {
        put(Physical_Locator_Set.NAME, update);
    }

    @JsonProperty("Mcast_Macs_Remote")
    public TableUpdate<Mcast_Macs_Remote> getMcast_Macs_RemoteUpdate() {
        return getUpdate(Mcast_Macs_Remote.NAME);
    }

    public void setMcast_Macs_RemoteUpdate(TableUpdate<Mcast_Macs_Remote> update) {
        put(Mcast_Macs_Remote.NAME, update);
    }

    @JsonProperty("Mcast_Macs_Local")
    public TableUpdate<Mcast_Macs_Local> getMcast_Macs_LocalUpdate() {
        return getUpdate(Mcast_Macs_Local.NAME);
    }

    public void setMcast_Macs_LocalUpdate(TableUpdate<Mcast_Macs_Local> update) {
        put(Mcast_Macs_Local.NAME, update);
    }

    @JsonProperty("Ucast_Macs_Local")
    public TableUpdate<Ucast_Macs_Local> getUcast_Macs_LocalUpdate() {
        return getUpdate(Ucast_Macs_Local.NAME);
    }

    public void setUcast_Macs_LocalUpdate(TableUpdate<Ucast_Macs_Local> update) {
        put(Ucast_Macs_Local.NAME, update);
    }

    @JsonProperty("Ucast_Macs_Remote")
    public TableUpdate<Ucast_Macs_Remote> getUcast_Macs_RemoteUpdate() {
        return getUpdate(Ucast_Macs_Remote.NAME);
    }

    public void setUcast_Macs_RemoteUpdate(TableUpdate<Ucast_Macs_Remote> update) {
        put(Ucast_Macs_Remote.NAME, update);
    }
}

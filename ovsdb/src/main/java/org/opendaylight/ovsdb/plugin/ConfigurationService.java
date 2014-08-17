/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Keith Burns
 */
package org.opendaylight.ovsdb.plugin;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IPluginInBridgeDomainConfigService;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.database.OVSInstance;
import org.opendaylight.ovsdb.lib.database.OvsdbType;
import org.opendaylight.ovsdb.lib.message.TransactBuilder;
import org.opendaylight.ovsdb.lib.message.operations.DeleteOperation;
import org.opendaylight.ovsdb.lib.message.operations.InsertOperation;
import org.opendaylight.ovsdb.lib.message.operations.MutateOperation;
import org.opendaylight.ovsdb.lib.message.operations.Operation;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;
import org.opendaylight.ovsdb.lib.message.operations.SelectOperation;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Controller;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Manager;
import org.opendaylight.ovsdb.lib.table.Mirror;
import org.opendaylight.ovsdb.lib.table.NetFlow;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.Qos;
import org.opendaylight.ovsdb.lib.table.Queue;
import org.opendaylight.ovsdb.lib.table.SFlow;
import org.opendaylight.ovsdb.lib.table.SSL;
import org.opendaylight.ovsdb.lib.table.internal.Column;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.lib.table.vtep.Global;
import org.opendaylight.ovsdb.lib.table.vtep.Logical_Switch;
import org.opendaylight.ovsdb.lib.table.vtep.Mcast_Macs_Local;
import org.opendaylight.ovsdb.lib.table.vtep.Mcast_Macs_Remote;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Locator;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Locator_Set;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Port;
import org.opendaylight.ovsdb.lib.table.vtep.Physical_Switch;
import org.opendaylight.ovsdb.lib.table.vtep.Ucast_Macs_Local;
import org.opendaylight.ovsdb.lib.table.vtep.Ucast_Macs_Remote;

/**
 * Offers OVS configuration operations.
 */
public class ConfigurationService extends ConfigurationServiceBase
    implements IPluginInBridgeDomainConfigService {

    private static final String dbName = "hardware_vtep"; // Open_vSwitch.NAME.getName();

    @Override
    String getDatabaseName() { return dbName; }

    /**
     * Add a new bridge
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Connector
     * @param configs configuration params
     * @return Bridge Connector configurations
     */
    @Override
    public Status createBridgeDomain(Node node, String bridgeIdentifier,
                                     Map<ConfigConstants, Object> configs) {
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }

            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node,
                                                                                    dbName);
            String newBridge = "new_bridge";
            String newInterface = "new_interface";
            String newPort = "new_port";
            String newSwitch = "new_switch";

            Operation addSwitchRequest = null;

            if(ovsTable != null){
                String ovsTableUUID = (String) ovsTable.keySet().toArray()[0];
                UUID bridgeUuidPair = new UUID(newBridge);
                Mutation mutation = new Mutation("bridges", Mutator.INSERT, bridgeUuidPair);
                UUID uuid = new UUID(ovsTableUUID);
                Condition where = new Condition("_uuid", Function.EQUALS, uuid);
                addSwitchRequest = new MutateOperation(dbName, where, mutation);
            } else {
                Open_vSwitch ovsTableRow = new Open_vSwitch();
                OvsDBSet<UUID> bridges = new OvsDBSet<>();
                UUID bridgeUuidPair = new UUID(newBridge);
                bridges.add(bridgeUuidPair);
                ovsTableRow.setBridges(bridges);
                addSwitchRequest = new InsertOperation(dbName, newSwitch, ovsTableRow);
            }

            Bridge bridgeRow = new Bridge();
            bridgeRow.setName(bridgeIdentifier);
            OvsDBSet<UUID> ports = new OvsDBSet<>();
            UUID port = new UUID(newPort);
            ports.add(port);
            bridgeRow.setPorts(ports);
            InsertOperation addBridgeRequest = new InsertOperation(Bridge.NAME.getName(), newBridge, bridgeRow);

            Port portRow = new Port();
            portRow.setName(bridgeIdentifier);
            OvsDBSet<UUID> interfaces = new OvsDBSet<>();
            UUID interfaceid = new UUID(newInterface);
            interfaces.add(interfaceid);
            portRow.setInterfaces(interfaces);
            InsertOperation addPortRequest = new InsertOperation(Port.NAME.getName(), newPort, portRow);

            Interface interfaceRow = new Interface();
            interfaceRow.setName(bridgeIdentifier);
            interfaceRow.setType("internal");
            InsertOperation addIntfRequest = new InsertOperation(Interface.NAME.getName(), newInterface, interfaceRow);

            /* Update config version */
            // TODO risks NPE if getTableCache above returned null?
            String ovsTableUUID = (String) ovsTable.keySet().toArray()[0];
            Mutation mutation = new Mutation("next_cfg", Mutator.SUM, 1);
            UUID uuid = new UUID(ovsTableUUID);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            MutateOperation updateCfgVerRequest = new MutateOperation(dbName, where, mutation);

            TransactBuilder transaction = makeTransaction(Arrays.asList(
                addSwitchRequest, addIntfRequest, addPortRequest,
                addBridgeRequest, updateCfgVerRequest));

            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();
            Status status = new Status(StatusCode.SUCCESS);
            for (int i = 0; i < tr.size() ; i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i) != null && tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size() - 1);
                logger.error("Error creating Bridge : {}\n Error : {}\n Details : {}", bridgeIdentifier,
                             result.getError(),
                             result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            if (status.isSuccess()) {
                setBridgeOFController(node, bridgeIdentifier);
            }
            return status;
        } catch(Exception e){
            logger.error("Error in createBridgeDomain(): ",e);
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    /**
     * Create a Port Attached to a Bridge
     * Ex. ovs-vsctl add-port br0 vif0
     * @param node Node serving this configuration service
     * @param portIdentifier String representation of a user defined Port Name
     * @param configs configuration params
     */
    @Override
    public Status addPort(Node node, String bridgeIdentifier, String portIdentifier,
                          Map<ConfigConstants, Object> configs) {
        try {
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }

            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            String newInterface = "new_interface";
            String newPort = "new_port";

            if(brTable != null){
                String brUuid = null;
                for (String uuid : brTable.keySet()) {
                    Bridge bridge = (Bridge) brTable.get(uuid);
                    if (bridge.getName().contains(bridgeIdentifier)) {
                        brUuid = uuid; // TODO could we break here?
                    }
                }

                UUID brUuidPair = new UUID(newPort);
                Mutation mutation = new Mutation("ports", Mutator.INSERT, brUuidPair);
                UUID uuid = new UUID(brUuid);
                Condition where = new Condition("_uuid", Function.EQUALS, uuid);
                Operation addBrMutRequest = new MutateOperation(Bridge.NAME.getName(), where, mutation);

                OvsDBMap<String, String> options = null;
                String type = null;
                OvsDBSet<BigInteger> tags = null;
                if (configs != null) {
                    type = (String) configs.get(ConfigConstants.TYPE);
                    Map<String, String> customConfigs = (Map<String, String>) configs.get(ConfigConstants.CUSTOM);
                    if (customConfigs != null) {
                        options = new OvsDBMap<>();
                        for (String customConfig : customConfigs.keySet()) {
                            options.put(customConfig, customConfigs.get(customConfig));
                        }
                    }
                }

                Interface interfaceRow = new Interface();
                interfaceRow.setName(portIdentifier);

                if (type != null) {
                    if (type.equalsIgnoreCase(OvsdbType.PortType.TUNNEL.name())) {
                        interfaceRow.setType((String)configs.get(ConfigConstants.TUNNEL_TYPE));
                        if (options == null) options = new OvsDBMap<>();
                        options.put("remote_ip", (String)configs.get(ConfigConstants.DEST_IP));
                    } else if (type.equalsIgnoreCase(OvsdbType.PortType.VLAN.name())) {
                        tags = new OvsDBSet<>();
                        tags.add(BigInteger.valueOf(Integer.parseInt((String)configs.get(ConfigConstants.VLAN))));
                    } else if (type.equalsIgnoreCase(OvsdbType.PortType.PATCH.name())) {
                        interfaceRow.setType(type.toLowerCase());
                    }
                }
                if (options != null) {
                    interfaceRow.setOptions(options);
                }

                InsertOperation addIntfRequest = new InsertOperation(Interface.NAME.getName(),
                                                                     newInterface, interfaceRow);

                Port portRow = new Port();
                portRow.setName(portIdentifier);
                if (tags != null) portRow.setTag(tags);
                OvsDBSet<UUID> interfaces = new OvsDBSet<>();
                UUID interfaceid = new UUID(newInterface);
                interfaces.add(interfaceid);
                portRow.setInterfaces(interfaces);
                InsertOperation addPortRequest = new InsertOperation(Port.NAME.getName(), newPort, portRow);

                TransactBuilder transaction = makeTransaction(Arrays.asList(
                    addBrMutRequest, addPortRequest, addIntfRequest));

                ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
                List<OperationResult> tr = transResponse.get();
                List<Operation> requests = transaction.getRequests();
                Status status = new Status(StatusCode.SUCCESS);
                for (int i = 0; i < tr.size() ; i++) {
                    if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                    if (tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                        OperationResult result = tr.get(i);
                        status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                    }
                }

                if (tr.size() > requests.size()) {
                    OperationResult result = tr.get(tr.size()-1);
                    logger.error("Error creating Bridge : {}\n Error : {}\n Details : {}", bridgeIdentifier,
                                 result.getError(),
                                 result.getDetails());
                    status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
                return status;
            }
        } catch(Exception e){
            logger.error("Error in addPort()",e);
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    /**
     * Implements the OVS Connection for Managers
     *
     * @param node Node serving this configuration service
     * @param managerIp Address of the manager
     */
    @SuppressWarnings("unchecked")
    public boolean setManager(Node node, String managerIp) {

        try {
            Connection connection = this.getConnection(node);
            if (connection == null) {
                return false;
            }

            String newmanager = "new_manager";

            OVSInstance instance = OVSInstance.monitorOVS(connection);

            Map ovsoutter = new LinkedHashMap();
            Map ovsinner = new LinkedHashMap();
            ArrayList ovsalist1 = new ArrayList();
            ArrayList ovsalist2 = new ArrayList();
            ArrayList ovsalist3 = new ArrayList();
            ArrayList ovsalist4 = new ArrayList();

            //OVS Table Update
            ovsoutter.put("where", ovsalist1);
            ovsalist1.add(ovsalist2);
            ovsalist2.add("_uuid");
            ovsalist2.add("==");
            ovsalist2.add(ovsalist3);
            ovsalist3.add("uuid");
            ovsalist3.add(instance.getUuid());
            ovsoutter.put("op", "update");
            ovsoutter.put("table", "Open_vSwitch");
            ovsoutter.put("row", ovsinner);
            ovsinner.put("manager_options", ovsalist4);
            ovsalist4.add("named-uuid");
            ovsalist4.add(newmanager);

            Map mgroutside = new LinkedHashMap();
            Map mgrinside = new LinkedHashMap();

            //Manager Table Insert
            mgroutside.put("uuid-name", newmanager);
            mgroutside.put("op", "insert");
            mgroutside.put("table","Manager");
            mgroutside.put("row", mgrinside);
            mgrinside.put("target", managerIp);

            Object[] params = {"Open_vSwitch", ovsoutter, mgroutside};
            OvsdbMessage msg = new OvsdbMessage("transact", params);

            //connection.sendMessage(msg);
        } catch(Exception e) {
            logger.error("Error in setManager(): ",e);
        }
        return true;
    }

    @Override
    public Status addBridgeDomainConfig(Node node, String bridgeIdentfier,
                                        Map<ConfigConstants, Object> configs) {
        String mgmt = (String)configs.get(ConfigConstants.MGMT);
        if (mgmt != null) {
            if (setManager(node, mgmt)) return new Status(StatusCode.SUCCESS);
        }
        return new Status(StatusCode.BADREQUEST);
    }

    @Override
    public Status addPortConfig(Node node, String bridgeIdentifier, String portIdentifier,
                                Map<ConfigConstants, Object> configs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status deletePort(Node node, String bridgeIdentifier, String portIdentifier) {

        try {

            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            Map<String, Table<?>> portTable = inventoryServiceInternal.getTableCache(node, Port.NAME.getName());
            String brUuid = null;
            String portUuid = null;
            if(brTable != null){
                for (String uuid : brTable.keySet()) {
                    Bridge bridge = (Bridge) brTable.get(uuid);
                    if (bridge.getName().contains(bridgeIdentifier)) {
                        brUuid = uuid;
                    }
                }
            }

            if(portTable != null){
                for (String uuid : portTable.keySet()) {
                    Port port = (Port) portTable.get(uuid);
                    if (port.getName().contains(portIdentifier)) {
                        portUuid = uuid;
                    }
                }
            }

            UUID portUuidPair = new UUID(portUuid);
            Mutation mutation = new Mutation("ports", Mutator.DELETE, portUuidPair);
            UUID uuid = new UUID(brUuid);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            Operation delPortRequest = new MutateOperation(Bridge.NAME.getName(), where, mutation);

            TransactBuilder transaction = makeTransaction(delPortRequest);

            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();
            Status status = new Status(StatusCode.SUCCESS);
            for (int i = 0; i < tr.size() ; i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size()-1);
                logger.error("Error creating Bridge : {}\n Error : {}\n Details : {}", bridgeIdentifier,
                             result.getError(),
                             result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            return status;
        } catch(Exception e){
            logger.error("Error in deletePort()", e);
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    @Override
    public Node getBridgeDomainNode(Node node, String bridgeIdentifier) {
        logger.info("getBridgeDomainNode is not implemented yet");
        return null;
    }

    @Override
    public Map<ConfigConstants, Object> getPortConfigs(Node node, String bridgeIdentifier,
                                                       String portIdentifier) {
        logger.info("getPortConfigs is not implemented yet");
        return null;
    }

    @Override
    public Status removeBridgeDomainConfig(Node node, String bridgeIdentifier,
                                           Map<ConfigConstants, Object> configs) {
        logger.info("RemoveBridgeDomainConfig is not implemented yet");
        return null;
    }

    @Override
    public Status removePortConfig(Node node, String bridgeIdentifier, String portIdentifier,
                                   Map<ConfigConstants, Object> configs) {
        logger.info("RemovePortConfig is not implemented yet");
        return null;
    }

    @Override
    public Status deleteBridgeDomain(Node node, String bridgeIdentifier) {

        try {
            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }
            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node,
                                                                                    dbName);
            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            String ovsUuid = null;
            String brUuid = null;

            if (brTable != null) {
                for (String uuid : brTable.keySet()) {
                    Bridge bridge = (Bridge) brTable.get(uuid);
                    if (bridge.getName().contains(bridgeIdentifier)) {
                        brUuid = uuid;
                    }
                }
            }
            if (ovsTable != null) {
                ovsUuid = (String) ovsTable.keySet().toArray()[0];
            }

            UUID bridgeUuidPair = new UUID(brUuid);
            Mutation mutation = new Mutation("bridges", Mutator.DELETE, bridgeUuidPair);
            UUID uuid = new UUID(ovsUuid);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            Operation delBrRequest = new MutateOperation(dbName, where, mutation);

            TransactBuilder transaction = makeTransaction(delBrRequest);

            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();
            Status status = new Status(StatusCode.SUCCESS);
            for (int i = 0; i < tr.size(); i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i) != null && tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size() - 1);
                logger.error("Error deleting Bridge : {}\n Error : {}\n Details : {}",
                             bridgeIdentifier, result.getError(), result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            return status;
        } catch (Exception e) {
            logger.error("Error in deleteBridgeDomain(): ",e);
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    @Override
    public Map<ConfigConstants, Object> getBridgeDomainConfigs(Node node, String bridgeIdentifier) {
        logger.info("getBridgeDomainConfigs is not implemented yet");
        return null;
    }

    @Override
    public List<String> getBridgeDomains(Node node) {
        List<String> brlist = new ArrayList<>();
        Map<String, Table<?>> brTableCache = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
        if(brTableCache != null){
            for (String uuid : brTableCache.keySet()) {
                Bridge bridge = (Bridge) brTableCache.get(uuid);
                brlist.add(bridge.getName());
            }
        }
        return brlist;
    }

    @Override
    public NodeConnector getNodeConnector(Node arg0, String arg1, String arg2) {
        return null;
    }

    Boolean setBridgeOFController(Node node, String bridgeIdentifier) {
        if (connectionService == null) {
            logger.error("Couldn't refer to the ConnectionService");
            return false;
        }

        try{
            Map<String, Table<?>> brTableCache = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            for (String uuid : brTableCache.keySet()) {
                Bridge bridge = (Bridge)brTableCache.get(uuid);
                if (bridge.getName().contains(bridgeIdentifier)) {
                    return connectionService.setOFController(node, uuid);
                }
            }
        } catch(Exception e) {
            logger.error("Error in setBridgeOFController()",e);
        }
        return false;
    }

    @Override
    public StatusWithUuid insertRow(Node node, String tableName, String parent_uuid, Table<?> row) {
        logger.debug("tableName : {}, parent_uuid : {} Row : {}", tableName, parent_uuid, row.toString());
        StatusWithUuid statusWithUUID = null;

        //this.dbName = Open_vSwitch.NAME.getName();

        // Schema based Table handling will help fix this static Table handling.

        if (row.getTableName().getName().equalsIgnoreCase("Bridge")) {
            statusWithUUID = insertBridgeRow(node, parent_uuid, (Bridge)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Capability")) {
            return new StatusWithUuid(StatusCode.NOTIMPLEMENTED, "Insert operation for this Table is not implemented yet.");
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Controller")) {
            statusWithUUID = insertControllerRow(node, parent_uuid, (Controller)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Interface")) {
            statusWithUUID = insertInterfaceRow(node, parent_uuid, (Interface)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Manager")) {
            statusWithUUID = insertManagerRow(node, parent_uuid, (Manager)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Mirror")) {
            statusWithUUID = insertMirrorRow(node, parent_uuid, (Mirror)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("NetFlow")) {
            statusWithUUID = insertNetFlowRow(node, parent_uuid, (NetFlow)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Open_vSwitch")) {
            statusWithUUID = insertOpen_vSwitchRow(node, (Open_vSwitch)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Port")) {
            statusWithUUID = insertPortRow(node, parent_uuid, (Port)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("QoS")) {
            statusWithUUID = insertQosRow(node, parent_uuid, (Qos)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Queue")) {
            statusWithUUID = insertQueueRow(node, parent_uuid, (Queue)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("sFlow")) {
            statusWithUUID = insertSflowRow(node, parent_uuid, (SFlow)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("SSL")) {
            statusWithUUID = insertSSLRow(node, parent_uuid, (SSL)row);
        }
        return statusWithUUID;
    }

    @Override
    public Status deleteRow(Node node, String tableName, String uuid) {
        if (tableName.equalsIgnoreCase("Bridge")) {
            return deleteBridgeRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Capbility")) {
            return new Status(StatusCode.NOTIMPLEMENTED, "Delete operation for this Table is not implemented yet.");
        }
        else if (tableName.equalsIgnoreCase("Controller")) {
            return deleteControllerRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Interface")) {
            return deleteInterfaceRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Manager")) {
            return deleteManagerRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Mirror")) {
            return deleteMirrorRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("NetFlow")) {
            return deleteNetFlowRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Open_vSwitch")) {
            return deleteOpen_vSwitchRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Port")) {
            return deletePortRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("QoS")) {
            return deleteQosRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Queue")) {
            return deleteQueueRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("sFlow")) {
            return deleteSflowRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("SSL")) {
            return deleteSSLRow(node, uuid);
        }
        return new Status(StatusCode.NOTFOUND, "Table "+tableName+" not supported");
    }

    private StatusWithUuid insertBridgeRow(Node node, String open_VSwitch_uuid, Bridge bridgeRow) {

        String insertErrorMsg = "bridge";
        String rowName=bridgeRow.getName();

        try{
            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node,
                                                                                    dbName);

            if (ovsTable == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "There are no Open_vSwitch instance in the Open_vSwitch table");
            }

            String newBridge = "new_bridge";

            String ovsTableUUID = open_VSwitch_uuid;
            if (ovsTableUUID == null) ovsTableUUID = (String) ovsTable.keySet().toArray()[0];
            UUID bridgeUuid = new UUID(newBridge);
            Mutation mutation = new Mutation("bridges", Mutator.INSERT, bridgeUuid);
            UUID uuid = new UUID(ovsTableUUID);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            Operation addSwitchRequest = new MutateOperation(dbName, where, mutation);

            InsertOperation addBridgeRequest = new InsertOperation(Bridge.NAME.getName(), newBridge, bridgeRow);

            TransactBuilder transaction = makeTransaction(Arrays.asList(
                addSwitchRequest, addBridgeRequest));

            int bridgeInsertIndex = transaction.getRequests().indexOf(addBridgeRequest);

            return _insertTableRow(node,transaction,bridgeInsertIndex,insertErrorMsg,rowName);

        } catch(Exception e){
            logger.error("Error in insertBridgeRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private TransactBuilder makeTransaction(Operation req) {
        TransactBuilder tr = new TransactBuilder(dbName);
        tr.addOperation(req);
        return tr;
    }

    private TransactBuilder makeTransaction(List<Operation> reqs) {
        TransactBuilder tr = new TransactBuilder(dbName);
        tr.addOperations(reqs);
        return tr;
    }

    private StatusWithUuid insertPortRow(Node node, String bridge_uuid, Port portRow) {

        String insertErrorMsg = "port";
        String rowName=portRow.getName();

        try{
            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            if (brTable == null ||  brTable.get(bridge_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "Bridge with UUID "+bridge_uuid+" Not found");
            }
            String newPort = "new_port";
            UUID portUUID = new UUID(newPort);
            Mutation mutation = new Mutation("ports", Mutator.INSERT, portUUID);
            UUID uuid = new UUID(bridge_uuid);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            Operation addBrMutRequest = new MutateOperation(Bridge.NAME.getName(), where, mutation);

            // Default OVS schema is to have 1 or more interface part of Bridge. Hence it is mandatory to
            // Insert an Interface in a Port add case

            String newInterface = "new_interface";
            Interface interfaceRow = new Interface();
            interfaceRow.setName(portRow.getName());
            InsertOperation addIntfRequest = new InsertOperation(Interface.NAME.getName(),
                                                                 newInterface, interfaceRow);

            OvsDBSet<UUID> interfaces = new OvsDBSet<>();
            UUID interfaceid = new UUID(newInterface);
            interfaces.add(interfaceid);
            portRow.setInterfaces(interfaces);

            InsertOperation addPortRequest = new InsertOperation(Port.NAME.getName(), newPort, portRow);

            TransactBuilder transaction = makeTransaction(Arrays.asList(
                addBrMutRequest, addPortRequest, addIntfRequest));
            int portInsertIndex = transaction.getRequests().indexOf(addPortRequest);

            return _insertTableRow(node,transaction,portInsertIndex,insertErrorMsg,rowName);

        } catch (Exception e) {
            logger.error("Error in insertPortRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertInterfaceRow(Node node, String port_uuid, Interface interfaceRow) {

        String insertErrorMsg = "interface";
        String rowName=interfaceRow.getName();

        try {

            // Interface table must have entry in Port table, checking port table for port
            Map<String, Table<?>> portTable = inventoryServiceInternal.getTableCache(node, Port.NAME.getName());
            if (portTable == null ||  portTable.get(port_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "Port with UUID "+port_uuid+" Not found");
            }
            // MUTATOR, need to insert the interface UUID to LIST of interfaces in PORT TABLE for port_uuid
            String newInterface = "new_interface";
            UUID interfaceUUID = new UUID(newInterface);
            Mutation mutation = new Mutation("interfaces", Mutator.INSERT, interfaceUUID); // field name to append is "interfaces"
            // Create the Operation which will be used in Transact to perform the PORT TABLE mutation
            UUID uuid = new UUID(port_uuid);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            Operation addPortMutationRequest = new MutateOperation(Port.NAME.getName(), where, mutation);

            // Create the interface row request
            InsertOperation addIntfRequest = new InsertOperation(Interface.NAME.getName(),newInterface, interfaceRow);

            // Transaction to insert/modify tables - validate using "sudo ovsdb-client dump" on host running OVSDB process
            TransactBuilder transaction = makeTransaction(Arrays.asList(
                addIntfRequest,addPortMutationRequest));

            // Check the results. Iterates over the results of the Array of transaction Operations, and reports STATUS
            int interfaceInsertIndex = transaction.getRequests().indexOf(addIntfRequest);

            return _insertTableRow(node,transaction,interfaceInsertIndex,insertErrorMsg,rowName);

        } catch (Exception e) {
            logger.error("Error in insertInterfaceRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertOpen_vSwitchRow(Node node, Open_vSwitch row) {
        return new StatusWithUuid(StatusCode.NOTIMPLEMENTED, "Insert operation for this Table is not implemented yet.");
    }

    private StatusWithUuid insertControllerRow(Node node, String bridge_uuid, Controller row) {

        String insertErrorMsg = "controller";
        String rowName=row.getTableName().toString();

        try{

            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            if (brTable == null ||  brTable.get(bridge_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "Bridge with UUID "+bridge_uuid+" Not found");
            }

            Map<String, Table<?>> controllerCache = inventoryServiceInternal.getTableCache(node, Controller.NAME.getName());

            String uuid_name = "new_controller";
            boolean controllerExists = false;
            if (controllerCache != null) {
                for (String uuid : controllerCache.keySet()) {
                    Controller controller = (Controller)controllerCache.get(uuid);
                    if (controller.getTarget().equals(row.getTarget())) {
                        uuid_name = uuid;
                        controllerExists = true;
                        break;
                    }
                }
            }

            UUID controllerUUID = new UUID(uuid_name);
            Mutation mutation = new Mutation("controller", Mutator.INSERT, controllerUUID);
            UUID uuid = new UUID(bridge_uuid);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            Operation addBrMutRequest = new MutateOperation(Bridge.NAME.getName(), where, mutation);
            InsertOperation addControllerRequest = null;

            TransactBuilder transaction = makeTransaction(addBrMutRequest);
            int portInsertIndex = -1;
            if (!controllerExists) {
                addControllerRequest = new InsertOperation(Controller.NAME.getName(), uuid_name, row);
                transaction.addOperation(addControllerRequest);
                portInsertIndex = transaction.getRequests().indexOf(addControllerRequest);
            }

            StatusWithUuid status = _insertTableRow(node,transaction,portInsertIndex,insertErrorMsg,rowName);
            if (status.isSuccess() && controllerExists) {
                // We won't get the uuid from the transact, so we set it here
                status = new StatusWithUuid(status.getCode(), controllerUUID);
            }
            return status;

        } catch (Exception e) {
            logger.error("Error in insertControllerRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertSSLRow(Node node, String parent_uuid, SSL row) {
        String insertErrorMsg = "SSL";
        String rowName=SSL.NAME.getName();

        try{
            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node,
                                                                                    dbName);

            if (ovsTable == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "There are no Open_vSwitch instance in the Open_vSwitch table");
            }

            String newSSL = "new_SSL";

            String ovsTableUUID = parent_uuid;
            if (ovsTableUUID == null) ovsTableUUID = (String) ovsTable.keySet().toArray()[0];
            UUID sslUuid = new UUID(newSSL);
            Mutation mutation = new Mutation("ssl", Mutator.INSERT, sslUuid);
            UUID uuid = new UUID(ovsTableUUID);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            Operation addOpen_vSwitchRequest = new MutateOperation(dbName, where, mutation);

            InsertOperation addSSLRequest = new InsertOperation(SSL.NAME.getName(), newSSL, row);

            TransactBuilder transaction = new TransactBuilder(dbName);
            transaction.addOperations(Arrays.asList(addSSLRequest,
                                                    addOpen_vSwitchRequest));

            int sslInsertIndex = transaction.getRequests().indexOf(addSSLRequest);

            return _insertTableRow(node,transaction,sslInsertIndex,insertErrorMsg,rowName);

        } catch(Exception e){
            logger.error("Error in insertSSLRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertSflowRow(Node node, String parent_uuid, SFlow row) {

        String insertErrorMsg = "sFlow";
        String rowName=row.NAME.getName();

        try{
            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            if (brTable == null ||  brTable.get(parent_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "Bridge with UUID "+parent_uuid+" Not found");
            }

            if (parent_uuid == null) {
                return new StatusWithUuid(StatusCode.BADREQUEST, "Require parent Bridge UUID.");
            }

            UUID uuid = new UUID(parent_uuid);

            String newSflow = "new_sflow";

            UUID sflowUuid = new UUID(newSflow);
            Mutation mutation = new Mutation("sflow", Mutator.INSERT, sflowUuid);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            Operation addBridgeRequest = new MutateOperation(Bridge.NAME.getName(), where, mutation);

            InsertOperation addSflowRequest = new InsertOperation(SFlow.NAME.getName(), newSflow, row);

            TransactBuilder transaction = new TransactBuilder(dbName);
            transaction.addOperations(Arrays.asList(addSflowRequest,
                                                    addBridgeRequest));

            int sflowInsertIndex = transaction.getRequests().indexOf(addSflowRequest);


            return _insertTableRow(node,transaction,sflowInsertIndex,insertErrorMsg,rowName);

        } catch (Exception e) {
            logger.error("Error in insertInterfaceRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertQueueRow(Node node, String parent_uuid, Queue row) {
        String insertErrorMsg = "Queue";
        String rowName = Queue.NAME.getName();

        try{
            Map<String, Table<?>> qosTable = inventoryServiceInternal.getTableCache(node, rowName);
            if (qosTable == null ||  qosTable.get(parent_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "QoS with UUID "+parent_uuid+" Not found");
            }

            if (parent_uuid == null) {
                return new StatusWithUuid(StatusCode.BADREQUEST, "Require parent QoS UUID.");
            }

            // NOTE: Queue Table is "isroot" meaning it can have a hanging reference. This is different from
            // standing insertRow due to the parent column type being a map, where one of the items may not be known
            // at time of insert. Therefore this is a simple insert, rather than mutate/insert.
            String newQueue = "new_queue";
            InsertOperation addQueueRequest = new InsertOperation(Queue.NAME.getName(), newQueue, row);

            TransactBuilder transaction = new TransactBuilder(dbName);
            transaction.addOperations(new ArrayList<Operation>(Arrays.asList(addQueueRequest)));

            int queueInsertIndex = transaction.getRequests().indexOf(addQueueRequest);

            return _insertTableRow(node,transaction,queueInsertIndex,insertErrorMsg,rowName);

        } catch (Exception e) {
            logger.error("Error in insertQueueRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);    }

    private StatusWithUuid insertQosRow(Node node, String parent_uuid, Qos row) {
        String insertErrorMsg = "Qos";
        String rowName = Qos.NAME.getName();

        try {

            String newQos = "new_qos";

            // QoS Table "isroot" meaning it can have hanging references. If parent_uuid is not supplied in API call this becomes a simple
            // insert operation, rather than the typical mutate/insert parent/child insert.
            if (parent_uuid != null) {
                // Port (parent) table check for UUID existance.
                Map<String, Table<?>> portTable = inventoryServiceInternal.getTableCache(node, Port.NAME.getName());
                if (portTable == null ||  portTable.get(parent_uuid) == null) {
                    return new StatusWithUuid(StatusCode.NOTFOUND, "Port with UUID "+parent_uuid+" Not found");
                }

                UUID qosUuid = new UUID(newQos);
                Mutation mutation = new Mutation("qos", Mutator.INSERT, qosUuid);
                UUID uuid = new UUID(parent_uuid);
                Condition where = new Condition("_uuid", Function.EQUALS, uuid);
                Operation addPortRequest = new MutateOperation(Port.NAME.getName(), where, mutation);

                InsertOperation addQosRequest = new InsertOperation(Qos.NAME.getName(), newQos, row);

                TransactBuilder transaction = makeTransaction(Arrays.asList(addQosRequest,addPortRequest));

                int qosInsertIndex = transaction.getRequests().indexOf(addQosRequest);

                return _insertTableRow(node,transaction,qosInsertIndex,insertErrorMsg,rowName);

            } else {
                Operation addQosRequest = new InsertOperation(Qos.NAME.getName(), newQos, row);

                TransactBuilder transaction = makeTransaction(addQosRequest);

                int qosInsertIndex = transaction.getRequests().indexOf(addQosRequest);

                return _insertTableRow(node,transaction,qosInsertIndex,insertErrorMsg,rowName);
            }

        } catch (Exception e) {
            logger.error("Error in insertQosRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }


    private StatusWithUuid insertNetFlowRow(Node node, String parent_uuid, NetFlow row) {
        String insertErrorMsg = "netFlow";
        String rowName = NetFlow.NAME.getName();

        try{
            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            if (brTable == null ||  brTable.get(parent_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "Bridge with UUID "+parent_uuid+" Not found");
            }

            if (parent_uuid == null) {
                return new StatusWithUuid(StatusCode.BADREQUEST, "Require parent Bridge UUID.");
            }

            UUID uuid = new UUID(parent_uuid);
            String newNetflow = "new_netflow";

            UUID netFlowUuid = new UUID(newNetflow);
            Mutation mutation = new Mutation("netflow", Mutator.INSERT, netFlowUuid);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            Operation addBridgeRequest = new MutateOperation(Bridge.NAME.getName(), where, mutation);

            Operation addNetflowRequest = new InsertOperation(NetFlow.NAME.getName(), newNetflow, row);

            TransactBuilder transaction = makeTransaction(Arrays.asList(addNetflowRequest, addBridgeRequest));

            int netflowInsertIndex = transaction.getRequests().indexOf(addNetflowRequest);


            return _insertTableRow(node,transaction,netflowInsertIndex,insertErrorMsg,rowName);

        } catch (Exception e) {
            logger.error("Error in insertNetFlowRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertMirrorRow(Node node, String parent_uuid, Mirror row) {
        String insertErrorMsg = "mirror";
        String rowName = Mirror.NAME.getName();

        try{
            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            if (brTable == null ||  brTable.get(parent_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "Bridge with UUID "+parent_uuid+" Not found");
            }

            if (parent_uuid == null) {
                return new StatusWithUuid(StatusCode.BADREQUEST, "Require parent Bridge UUID.");
            }

            UUID uuid = new UUID(parent_uuid);
            String newMirror = "new_mirror";

            Operation addBridgeRequest = null;

            UUID mirrorUuid = new UUID(newMirror);
            Mutation mutation = new Mutation("mirrors", Mutator.INSERT, mirrorUuid);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            addBridgeRequest = new MutateOperation(Bridge.NAME.getName(), where, mutation);

            InsertOperation addMirrorRequest = new InsertOperation(Mirror.NAME.getName(), newMirror, row);

            TransactBuilder transaction = makeTransaction(Arrays.asList(addBridgeRequest, addMirrorRequest));

            int mirrorInsertIndex = transaction.getRequests().indexOf(addMirrorRequest);

            return _insertTableRow(node,transaction,mirrorInsertIndex,insertErrorMsg,rowName);

        } catch (Exception e) {
            logger.error("Error in insertMirrorRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertManagerRow(Node node, String parent_uuid, Manager row) {
        String insertErrorMsg = "manager";
        String rowName = Manager.NAME.getName();

        try{
            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node,
                                                                                    dbName);

            if (ovsTable == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "There are no Open_vSwitch instance in the Open_vSwitch table");
            }

            String newManager = "new_manager";

            String ovsTableUUID = parent_uuid;
            if (ovsTableUUID == null) ovsTableUUID = (String) ovsTable.keySet().toArray()[0];
            UUID managerUuid = new UUID(newManager);
            Mutation mutation = new Mutation("manager_options", Mutator.INSERT, managerUuid);
            UUID uuid = new UUID(ovsTableUUID);
            Condition where = new Condition("_uuid", Function.EQUALS, uuid);
            Operation  addSwitchRequest = new MutateOperation(dbName, where, mutation);

            InsertOperation addManagerRequest = new InsertOperation(Manager.NAME.getName(), newManager, row);

            TransactBuilder transaction = makeTransaction(Arrays.asList(
                addSwitchRequest, addManagerRequest));

            int managerInsertIndex = transaction.getRequests().indexOf(addManagerRequest);

            return _insertTableRow(node,transaction,managerInsertIndex,insertErrorMsg,rowName);

        } catch(Exception e){
            logger.error("Error in insertManagerRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private Status deleteBridgeRow(Node node, String uuid) {
        String parentTableName=Open_vSwitch.NAME.getName();
        String childTableName=Bridge.NAME.getName();
        String parentColumn = "bridges";
        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deletePortRow(Node node, String uuid) {
        String parentTableName=Bridge.NAME.getName();
        String childTableName=Port.NAME.getName();
        String parentColumn = "ports";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteInterfaceRow(Node node, String uuid) {
        // Since Port<-Interface tables have a 1:n relationship, need to test if this is the last interface
        // assigned to a port before attempting delete.
        Map<String, Table<?>> portTable = inventoryServiceInternal.getTableCache(node, Port.NAME.getName());
        Map<String, Table<?>> interfaceTable = inventoryServiceInternal.getTableCache(node, Interface.NAME.getName());
        // Check that the UUID exists
        if (portTable == null || interfaceTable == null || uuid == null || interfaceTable.get(uuid) == null) {
            return new Status(StatusCode.NOTFOUND, "");
        }

        // Since the above past, it's safe to use the generic _deleteTableRow method
        String parentTableName=Port.NAME.getName();
        String childTableName=Interface.NAME.getName();
        String parentColumn = "interfaces";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteControllerRow(Node node, String uuid) {
        String parentTableName=Bridge.NAME.getName();
        String childTableName=Controller.NAME.getName();
        String parentColumn = "controller";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteOpen_vSwitchRow(Node node, String uuid) {
        return new Status(StatusCode.NOTIMPLEMENTED, "delete operation for this Table is not implemented yet.");
    }

    private Status deleteSSLRow(Node node, String uuid) {
        String parentTableName=Open_vSwitch.NAME.getName();
        String childTableName=SSL.NAME.getName();
        String parentColumn = "ssl";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteSflowRow(Node node, String uuid) {
        String parentTableName=Bridge.NAME.getName();
        String childTableName=SFlow.NAME.getName();
        String parentColumn = "sflow";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteQueueRow(Node node, String uuid) {
        // This doesn't do a mutate on parent, but simply deletes row
        String childTableName=Queue.NAME.getName();

        return _deleteRootTableRow(node,uuid,childTableName);
    }

    private Status deleteQosRow(Node node, String uuid) {
        String parentTableName=Port.NAME.getName();
        String childTableName=Qos.NAME.getName();
        String parentColumn = "qos";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteNetFlowRow(Node node, String uuid) {
        String parentTableName=Bridge.NAME.getName();
        String childTableName=NetFlow.NAME.getName();
        String parentColumn = "netflow";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteMirrorRow(Node node, String uuid) {
        String parentTableName=Bridge.NAME.getName();
        String childTableName=Mirror.NAME.getName();
        String parentColumn = "mirrors";
        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteManagerRow(Node node, String uuid) {
        String parentTableName=Open_vSwitch.NAME.getName();
        String childTableName=Manager.NAME.getName();
        String parentColumn = "manager_options";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    @SuppressWarnings("unused")
    public void _ovsconnect (CommandInterpreter ci) {
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter node name");
            return;
        }
        super._connect(nodeName, ci);
    }

    @SuppressWarnings("unused")
    public void addBridge (CommandInterpreter ci) {
        //this.dbName = "Open_vSwitch";
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }
        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }
        Status status;

        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.createBridgeDomain(node, bridgeName, null);
        ci.println("Bridge creation status : "+status.toString());
    }

    @SuppressWarnings("unused")
    public void getBridgeDomains (CommandInterpreter ci) {
        //this.dbName = "Open_vSwitch";
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }

        Node node = Node.fromString(nodeName);
        List<String> brlist = this.getBridgeDomains(node);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        ci.println("Existing Bridges: " + brlist);
    }

    @SuppressWarnings("unused")
    public void deleteBridgeDomain (CommandInterpreter ci) {
        //this.dbName = "Open_vSwitch";
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }
        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }
        Status status;
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.deleteBridgeDomain(node, bridgeName);
        ci.println("Bridge deletion status : "+status.toString());
    }

    @SuppressWarnings("unused")
    public void addPort (CommandInterpreter ci) {
        //this.dbName = "Open_vSwitch";
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }

        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }

        String portName = ci.nextArgument();
        if (portName == null) {
            ci.println("Please enter Port Name");
            return;
        }

        String type = ci.nextArgument();

        Map<String, String> configs = new HashMap<>();
        while(true) {
            String configKey = ci.nextArgument();
            if (configKey == null) break;
            String configValue = ci.nextArgument();
            if (configValue == null) break;
            configs.put(configKey, configValue);
        }

        Map<ConfigConstants, Object> customConfigs = null;
        if (type != null) {
            customConfigs = new HashMap<>();
            customConfigs.put(ConfigConstants.TYPE, type);
        }

        if (configs.size() > 0) {
            if (customConfigs == null) customConfigs = new HashMap<>();
            customConfigs.put(ConfigConstants.CUSTOM, configs);
            ci.println(customConfigs.toString());
        }
        Status status;
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.addPort(node, bridgeName, portName, customConfigs);
        ci.println("Port creation status : "+status.toString());
    }

    @SuppressWarnings("unused")
    public void deletePort (CommandInterpreter ci) {
        //this.dbName = "Open_vSwitch";
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }

        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }

        String portName = ci.nextArgument();
        if (portName == null) {
            ci.println("Please enter Port Name");
            return;
        }

        Status status;
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.deletePort(node, bridgeName, portName);
        ci.println("Port deletion status : "+status.toString());
    }

    @SuppressWarnings("unused")
    public void addPortVlan (CommandInterpreter ci) {
        //this.dbName = "Open_vSwitch";
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }

        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }

        String portName = ci.nextArgument();
        if (portName == null) {
            ci.println("Please enter Port Name");
            return;
        }

        String vlan = ci.nextArgument();
        if (vlan == null) {
            ci.println("Please enter Valid Vlan");
            return;
        } else {
            try {
                Integer.parseInt(vlan);
            } catch (NumberFormatException e) {
                ci.println("Please enter Valid Vlan");
                return;
            }
        }

        Map<ConfigConstants, Object> configs = new HashMap<>();
        configs.put(ConfigConstants.TYPE, "VLAN");
        configs.put(ConfigConstants.VLAN, vlan);

        Status status;
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.addPort(node, bridgeName, portName, configs);
        ci.println("Port creation status : "+status.toString());
    }

    @SuppressWarnings("unused")
    public void addTunnel (CommandInterpreter ci) {
        //this.dbName = "Open_vSwitch";
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }

        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }

        String portName = ci.nextArgument();
        if (portName == null) {
            ci.println("Please enter Port Name");
            return;
        }

        String tunnelType = ci.nextArgument();
        if (tunnelType == null) {
            ci.println("Please enter Tunnel Type");
            return;
        }

        String remoteIp = ci.nextArgument();
        if (remoteIp == null) {
            ci.println("Please enter valid Remote IP Address");
            return;
        }

        try {
            InetAddress.getByName(remoteIp);
        }  catch (Exception e) {
            logger.error("Unable to resolve " + remoteIp, e);
            ci.println("Please enter valid Remote IP Address");
            return;
        }

        Map<ConfigConstants, Object> configs = new HashMap<>();
        configs.put(ConfigConstants.TYPE, "TUNNEL");
        configs.put(ConfigConstants.TUNNEL_TYPE, tunnelType);
        configs.put(ConfigConstants.DEST_IP, remoteIp);

        Status status;
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.addPort(node, bridgeName, portName, configs);
        ci.println("Port creation status : "+status.toString());
    }

    @Override
    public String getHelp() {
        return "---OVSDB CLI---\n"
            + "\t ovstrconnect <ConnectionName> <ip-address>                        - Connect to OVSDB\n"
            + "\t addBridge <Node> <BridgeName>                                   - Add Bridge\n"
            + "\t getBridgeDomains <Node>                                         - Get Bridges\n"
            + "\t deleteBridgeDomain <Node> <BridgeName>                          - Delete a Bridge\n"
            + "\t addPort <Node> <BridgeName> <PortName> <type> <options pairs>   - Add Port\n"
            + "\t deletePort <Node> <BridgeName> <PortName>                       - Delete Port\n"
            + "\t addPortVlan <Node> <BridgeName> <PortName> <vlan>               - Add Port, Vlan\n"
            + "\t addTunnel <Node> <Bridge> <Port> <tunnel-type> <remote-ip>      - Add Tunnel\n"
            + "\t printCache <Node>                                               - Prints Table Cache";
    }

    /**
     * VTEP methods.
     */

    /**
     * Connects to the VTEP.
     * @param ci The command interpreter.
     */
    public void vtepConnect(CommandInterpreter ci) {
        ci.print("Connecting to the Hardware VTEP.");
        this._connect("vtep", ci);
    }

    // vtepAddPsPort <phys_br_name> <port_name>
    // this is a bit awkward: the emulator monitors the ovs bridge for
    // consistency and makes sure that all its ports are also in the emulated
    // vtep one. So, the maybe this needs to be refactored and changed so it
    // actually does an ovs-vsctl add-port <phys_br_name> <port_name> and lets
    // the emulator put them in sync.
    public void vtepAddPysicalSwitchPort(Node node, CommandInterpreter ci) {
        ci.println("Adding a physical port");

        String psName = ci.nextArgument();
        String portName = ci.nextArgument();

        vtepAddPhysicalSwitchPort(node, psName, portName);

        ci.println("Adding port completed");
    }

    public Status vtepAddPhysicalSwitchPort(
        Node node, String psName, String portName) {
        //this.dbName = "hardware_vtep";

        UUID psUuid = findPhysicalSwitch(node, psName);
        if (psUuid == null) {
            logger.error("Physical switch " + psName + " not found");
            return new StatusWithUuid(StatusCode.NOTFOUND);
        }

        UUID newPortUuid = new UUID("new_ps_port");
        Mutation m = new Mutation("ports", Mutator.INSERT, newPortUuid);
        Condition where = new Condition("_uuid", Function.EQUALS, psUuid);
        Operation mr = new MutateOperation(Physical_Switch.NAME.getName(),
                                           where, m);

        Physical_Port row = new Physical_Port();
        row.setName(portName);
        row.setVlan_bindings(new OvsDBMap<BigInteger, UUID>());
        InsertOperation ins = new InsertOperation(Physical_Port.NAME.getName(),
                                                  "new_ps_port", row);

        TransactBuilder tr = makeTransaction(Arrays.asList(mr, ins));
        try {
            List<OperationResult> opsRes = getConnection(node)
                .getRpc().transact(tr).get();
            for (OperationResult opRes : opsRes) {
                if (opRes.getError() != null) {
                    logger.error("ERROR! " + opRes);
                } else {
                    logger.info("OK! " + opRes);
                }
            }
        } catch (Exception e) {
            logger.error("Something went wrong adding port", e);
        }

        return new Status(StatusCode.SUCCESS);

    }

    public void vtepAddLogicalSwitch(Node node, CommandInterpreter ci) {
        ci.println("Let's add a logical switch");
        String vni = ci.nextArgument();
        String name = ci.nextArgument();
        StatusWithUuid status = vtepAddLogicalSwitch(node, name,
                                                     Integer.parseInt(vni));
        ci.println("Done (" + status.getDescription() +
                       "), uuid: " + status.getUuid());
    }

    public StatusWithUuid vtepAddLogicalSwitch(
        Node node, String name, int vni) {
        //this.dbName = "hardware_vtep";
        Logical_Switch row = new Logical_Switch();
        row.setName(name);
        row.setTunnel_key(set(BigInteger.valueOf(vni)));
        return this.insLogicalSwitch(node, row);
    }

    public void vtepBindVlan(Node node, CommandInterpreter ci) {
        ci.println("Let's bind a vlan");
        String vlan = ci.nextArgument();
        String lsName = ci.nextArgument();
        String portName = ci.nextArgument();
        vtepBindVlan(node, lsName, portName, Short.parseShort(vlan), null,
                     new ArrayList<String>());
    }

    /**
     * Adds bindings to an existing logical switch, and sets flooding IPs.
     */
    public Status vtepAddBindings(
        Node node, UUID lsUuid, Collection<Pair<String, Short>> portVlanPairs) {
        //this.dbName = "hardware_vtep";

        List<Operation> ops = makeBindingOperations(node, lsUuid,
                                                    portVlanPairs);

        logger.debug("Issuing transaction with ops {}", ops);
        TransactBuilder tr = makeTransaction(ops);
        Status result = new Status(StatusCode.SUCCESS);

        try {
            List<OperationResult> opsRes =
                getConnection(node).getRpc().transact(tr).get();

            for (OperationResult opRes : opsRes) {
                String sErr = opRes.getError();
                if (sErr != null && !sErr.trim().equals("")) {
                    String msg = "Error adding bindings " + opRes;
                    logger.error(msg);
                    return new Status(StatusCode.INTERNALERROR, msg);
                }
            }
        } catch (Exception e) {
            logger.error("Failure adding bindings", e);
            result = new Status(StatusCode.INTERNALERROR);
        }

        logger.info("Add bindings result: " + result.getCode());
        return result;

    }

    /**
     * Makes all the OVSDB operations required to add the given bindings to the
     * logical switch.
     */
    private List<Operation> makeBindingOperations(
        Node node, UUID lsUuid, Collection<Pair<String, Short>> portVlanPairs) {

        List<Operation> ops = new ArrayList<>();
        for (Pair<String, Short> p : portVlanPairs) {
            String portName = p.getKey();
            Short vlan = p.getValue();
            UUID portUUID = findPhysPort(node, portName);
            if (portUUID == null) {
                logger.warn("Physical port " + portName + " not found");
            }

            OvsDBMap<Short, UUID> vlanToLs = new OvsDBMap<>();
            vlanToLs.put(vlan, lsUuid);
            Mutation m = new Mutation("vlan_bindings", Mutator.INSERT, vlanToLs);
            Condition where = new Condition("_uuid", Function.EQUALS, portUUID);

            ops.add(
                new MutateOperation(Physical_Port.NAME.getName(), where, m)
            );
        }

        return ops;
    }

    /**
     * Make the operations to set flood IPs on the given logical switch.
     */
    private List<Operation> makeFloodIpOperations(
        Node node, UUID lsUuid, List<String> floodIps) {
        List<Operation> ops = new ArrayList<>(floodIps.size());
        for (String ip : floodIps) {

            logger.info("Map unknown-dst to {} in mcast and ucast remote", ip);

            UUID plUuid = findPhysLocator(node, ip);
            if (plUuid == null) {
                logger.info("New physical locator needed for " + ip);
                String newPlName = "new_pl";
                plUuid = new UUID(newPlName);
                Physical_Locator pl = new Physical_Locator();
                pl.setDst_ip(ip);
                pl.setEncapsulation_type("vxlan_over_ipv4");
                ops.add(new InsertOperation(Physical_Locator.NAME.getName(),
                                            newPlName, pl));
            }

            String newPlsName = "new_pls_name";
            UUID plsUuid = new UUID(newPlsName);
            Physical_Locator_Set pls = new Physical_Locator_Set();
            pls.setLocators(set(plUuid));
            ops.add(new InsertOperation(Physical_Locator_Set.NAME.getName(),
                                        newPlsName, pls));

            Mcast_Macs_Remote rowMcast = new Mcast_Macs_Remote();
            rowMcast.setMac("unknown-dst");
            rowMcast.setLocator_set(set(plsUuid));
            rowMcast.setLogical_switch(set(lsUuid));
            ops.add(new InsertOperation(Mcast_Macs_Remote.NAME.getName(),
                                        "new_mcast_mac", rowMcast));

            Ucast_Macs_Remote rowUcast = new Ucast_Macs_Remote();
            rowUcast.setMac("unknown-dst");
            rowUcast.setLocator(set(plUuid));
            rowUcast.setIpaddr(ip);
            rowUcast.setLogical_switch(set(lsUuid));
            ops.add(new InsertOperation(Ucast_Macs_Remote.NAME.getName(),
                                        "new_ucast_mac", rowUcast));
        }
        return ops;
    }


    /**
     * Will create a new binding using the given logical switch. If this does
     * not exist and a VNi is provided, it will create a new LS. This will
     * also include the floodIps in the Mcast and Ucast remote tables associated
     * to unknown-mac, thus including the remote peers in all floods.
     *
     * @param floodIps include these IPs in Mcast and Ucast remote mac tables
     *                 for the unknown-dst
     */
    public Status vtepBindVlan(
        Node node, String lsName, String portName, short vlan, Integer vni,
        List<String> floodIps) {
        //this.dbName = "hardware_vtep";

        boolean creatingLs = false;
        UUID lsUuid = findLogicalSwitch(node, lsName);
        List<Operation> ops = new ArrayList<>();
        if (lsUuid == null) {
            if (vni == null) {
                logger.error("Logical switch " + lsName +
                                 " not found, no vni provided");
                return new Status(StatusCode.NOTFOUND,
                                  "Logical Switch " + lsName + " not found");
            }
            logger.info("Logical switch will be added, vni {}", vni);
            Logical_Switch row = new Logical_Switch();
            row.setName(lsName);
            row.setTunnel_key(set(BigInteger.valueOf(vni)));
            lsUuid = new UUID(lsName);
            ops.add(new InsertOperation(Logical_Switch.NAME.getName(),
                                        lsName, row));
            creatingLs = true;
        } else {
            logger.info("Logical switch exists, {}", lsUuid);
        }

        ops.addAll(makeBindingOperations(node, lsUuid, Arrays.asList(Pair.of(portName, vlan))));

        ops.addAll(makeFloodIpOperations(node, lsUuid, floodIps));

        logger.debug("Issuing transaction with ops {}", ops);
        TransactBuilder tr = makeTransaction(ops);
        try {
            List<OperationResult> opRes = getConnection(node)
                .getRpc().transact(tr).get();

            Iterator<OperationResult> itRes = opRes.iterator();
            if (creatingLs) {
                OperationResult addLsRes = itRes.next();
                if (addLsRes.getError() != null) {
                    String msg = "Error creating logical switch: " +
                        addLsRes.getError() + ", " +
                        addLsRes.getDetails();
                    logger.error(msg);
                    return new Status(StatusCode.INTERNALERROR, msg);
                }
            }

            OperationResult bindRes = itRes.next();
            if (bindRes.getError() != null) {
                String msg = "Error binding vlan " + bindRes.getError() +
                             ", " + bindRes.getDetails();
                logger.error(msg);
                return new Status(StatusCode.INTERNALERROR, msg);

            }
        } catch (Exception e) {
            String msg = "Cant' add vlan binding to port";
            logger.error(msg, e);
            return new Status(StatusCode.INTERNALERROR, msg);
        }

        Status st = new Status(StatusCode.SUCCESS);
        logger.info("Bind vlan result: " + st.getCode());
        return st;
    }

    /**
     * Clears all the port-vlan bindings for a given logical switch.
     */
    public Status vtepClearBindings(Node node, UUID lsUuid) {

        logger.debug("Clearing bindings for logical switch {}", lsUuid);

        //this.dbName = "hardware_vtep";

        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(node)
                .get(Physical_Port.NAME.getName());
        if (tableCache == null || tableCache.isEmpty()) {
            logger.warn("Trying to delete bindings for logical switch {}, but" +
                        "port cache is empty");
            return new Status(StatusCode.NOTFOUND);
        }

        List<Operation> ops = new ArrayList<>();

        // Examine all the physical ports looking for bindings to the given
        // logical switch
        for (Map.Entry<String, Table<?>> e : tableCache.entrySet()) {
            Physical_Port physPort = (Physical_Port)e.getValue();
            OvsDBMap<BigInteger, UUID> bindings = physPort.getVlan_bindings();

            // Scan all its bindings looking for our target logical switch
            List<Mutation> mutations = new ArrayList<>();
            for(Map.Entry<BigInteger, UUID> _e : bindings.entrySet()) {
                BigInteger vlan = _e.getKey();
                if (lsUuid.equals(_e.getValue())) {
                    mutations.add(
                        new Mutation("vlan_bindings", Mutator.DELETE, vlan)
                    );
                }
            }

            // Mutate operation for this port row
            UUID port = new UUID(e.getKey());
            ops.add(new MutateOperation(
                Physical_Port.NAME.getName(),
                Arrays.asList(new Condition("_uuid", Function.EQUALS, port)),
                mutations)
            );
        }

        // At this point we have a list of Operations each containing all the
        // mutations required to remove all bindings for each vtep
        TransactBuilder tr = makeTransaction(ops);
        Connection cnxn = this.getConnection(node);
        if (cnxn == null) {
            logger.warn("Connection is not available!");
            return new Status(StatusCode.NOSERVICE);
        }

        // There we go.
        try {
            List<OperationResult> opsRes = cnxn.getRpc().transact(tr).get();
            if (opsRes.size() != ops.size()) {
                return new Status(StatusCode.BADREQUEST,
                          "Unexpected results clearing bindings: " + opsRes);
            }
            for (OperationResult opRes : opsRes) {
                String sError = opRes.getError();
                if (sError != null && !"".equals(sError.trim())) {
                    logger.error("Failed clearing bindings" + sError);
                    return new Status(StatusCode.BADREQUEST, sError);
                }
            }
            logger.info("Bindings for logical switch {} cleared successfully",
                        lsUuid);
            return new Status(StatusCode.SUCCESS);
        } catch (InterruptedException | ExecutionException e) {
            String msg = "Error deleting bindings";
            logger.error(msg, e);
            return new Status(StatusCode.BADREQUEST, msg);
        }
    }

    /**
     * Deletes a binding between port and VLAN on a given physical switch.
     *
     * @param psId The identifier of the physical switch.
     * @param portName The physical port name.
     * @param vlan The VLAN.
     * @return The operation status.
     */
    public Status vtepDelBinding(Node node, UUID psId, String portName,
                                 short vlan) {

        logger.info("Deleting binding on switch {}, port {}, VLAN {}",
                    psId, portName, vlan);

        //this.dbName = "hardware_vtep";

        UUID portId = findPhysPort(node, portName);
        if (portId == null) {
            logger.error("Physical port {} not found", portName);
            return new Status(StatusCode.NOTFOUND, "Physical port not found");
        }

        Condition where = new Condition("_uuid", Function.EQUALS, portId);
        Mutation m = new Mutation("vlan_bindings", Mutator.DELETE, vlan);
        TransactBuilder tr = makeTransaction(
            new MutateOperation(Physical_Port.NAME.getName(), where, m)
        );
        Connection cnxn = this.getConnection(node);
        if (cnxn == null) {
            return new Status(StatusCode.NOSERVICE,
                              "Connection to ovsdb-server not available");
        }

        try {
            List<OperationResult> trRes = cnxn.getRpc().transact(tr).get();
            if (trRes.size() > 1) {
                return new Status(StatusCode.BADREQUEST,
                              "Unexpected results on vtepDelBinding: " + trRes);
            }

            String sError = trRes.get(0).getError();
            if (StringUtils.isNotBlank(sError)) {
                logger.error("Deleting binding failed" + sError);
                return new Status(StatusCode.BADREQUEST, sError);
            }
            logger.info("Binding deleted successfully");
            return new Status(StatusCode.SUCCESS);
        } catch (InterruptedException | ExecutionException e) {
            String msg = "Error in vtepDelBinding";
            logger.error(msg, e);
            return new Status(StatusCode.BADREQUEST, msg);
        }
    }

    /**
     * Will remove a logical switch from the VTEP database, along with any
     * bindings associated to it.
     *
     * @param lsName the logical switch name
     * @return
     */
    public Status vtepDelLogicalSwitch(Node node, String lsName) {

        logger.debug("Deleting logical switch {}", lsName);

        //this.dbName = "hardware_vtep";

        UUID lsId = findLogicalSwitch(node, lsName);
        if (lsId == null) {
            logger.debug("Logical switch {} not found", lsName);
            return new Status(StatusCode.NOTFOUND, "Logical switch not found");
        }

        List<Operation> ops = new ArrayList<>();

        // We need to clear all the mac tables, both local and remote mac tables
        List<String> macTables = Arrays.asList(
            Ucast_Macs_Local.NAME.getName(), Mcast_Macs_Local.NAME.getName(),
            Ucast_Macs_Remote.NAME.getName(), Mcast_Macs_Remote.NAME.getName()
        );
        for(String table : macTables) {
            ops.add(new DeleteOperation(
                table, new Condition("logical_switch", Function.EQUALS, lsId))
            );
        }

        // Brace yourself. Now we need to find out all the bindings where this
        // logical switch participates.
        Collection<Pair<UUID, Short>> bindings =
            vtepPortVlanBindings(node, lsId);

        logger.debug("Will remove (portId, vlan) bindings: " + bindings);

        // For each known binding, remove it
        for(Pair<UUID, Short> binding : bindings) {
            UUID portId = binding.getLeft();
            Short vlan = binding.getRight();
            Condition where = new Condition("_uuid", Function.EQUALS, portId);
            Mutation m = new Mutation("vlan_bindings", Mutator.DELETE, vlan);
            ops.add(new MutateOperation(Physical_Port.NAME.getName(), where, m));
        }

        // And finally, delete the switch
        ops.add(new DeleteOperation(
            Logical_Switch.NAME.getName(),
            new Condition("_uuid", Function.EQUALS, lsId)
        ));

        Connection cnxn = this.getConnection(node);
        if (cnxn == null) {
            return new Status(StatusCode.NOSERVICE,
                              "Connection to ovsdb-server not available");
        }

        TransactBuilder tr = new TransactBuilder(getDatabaseName());
        tr.addOperations(ops);

        try {
            List<OperationResult> trRes = cnxn.getRpc().transact(tr).get();
            if (trRes.size() > ops.size()) {
                return new Status(StatusCode.BADREQUEST,
                                  "Unexpected results on vtepDelLogicalSwitch: "
                                  + trRes);
            }

            int i = 0;
            for (OperationResult opRes : trRes) {
                String sError = opRes == null ? null : opRes.getError();
                if (sError != null && !"".equals(sError.trim())) {
                    Operation failedOp = ops.get(i);
                    logger.error("Failed vtepDelLogicalSwitch, op {} {}: {}",
                                 i, failedOp.getOp(), sError);
                    return new Status(StatusCode.BADREQUEST, sError);
                }
                i++;
            }

            // Done, at last
            logger.debug("Logical switch {} successfully deleted", lsName);
            return new Status(StatusCode.SUCCESS);

        } catch (InterruptedException | ExecutionException e) {
            String msg = "Error in vtepDelBinding";
            logger.error(msg, e);
            return new Status(StatusCode.BADREQUEST, msg);
        }

    }

    /**
     * Offers a list of all the Port-Vlan bindings for the given logical switch.
     */
    public List<Pair<UUID, Short>> vtepPortVlanBindings(Node node,
                                                          UUID lsUUID) {
        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(node)
                                    .get(Physical_Port.NAME.getName());
        if (tableCache == null) {
            return new ArrayList<>();
        }

        List<Pair<UUID, Short>> foundBindings = new ArrayList<>();
        for (Map.Entry<String, Table<?>> e : tableCache.entrySet()) {
            UUID portId = new UUID(e.getKey());
            Physical_Port physPort = (Physical_Port)e.getValue();
            OvsDBMap<BigInteger, UUID> bindings = physPort.getVlan_bindings();
            for(Map.Entry<BigInteger, UUID> _e : bindings.entrySet()) {
                if (_e.getValue().equals(lsUUID)) {
                    foundBindings.add(
                        Pair.of(portId, _e.getKey().shortValue()));
                }
            }
        }
        return foundBindings;
    }

    public void vtepAddUcastMacRemote(Node node, CommandInterpreter ci) {

        ci.println("Adding remote ucast, args: ls mac vtep_ip mac_ip");

        String ls = ci.nextArgument();
        String mac = ci.nextArgument();
        String vtepIp = ci.nextArgument();
        String macIp = ci.nextArgument();

        vtepAddUcastMacRemote(node, ls, mac, vtepIp, macIp);
    }

    /**
     * Adds an entry to the unicast remote MAC table.
     * @param ls The logical switch name.
     * @param mac The MAC address.
     * @param vtepIp The VTEP IP address.
     * @param macIp The MAC IP address.
     * @return The operation status with the entry identifier.
     */
    public StatusWithUuid vtepAddUcastMacRemote(
        Node node, String ls, String mac, String vtepIp, String macIp) {

        //this.dbName = "hardware_vtep";

        UUID lsUuid = findLogicalSwitch(node, ls);
        if (lsUuid == null) {
            logger.error("No logical switch named " + ls);
            return new StatusWithUuid(StatusCode.NOTFOUND);
        }

        TransactBuilder transaction = new TransactBuilder(getDatabaseName());
        UUID plUuid= findPhysLocator(node, vtepIp);
        if (plUuid == null) {
            logger.debug("New physical locator needed for " + vtepIp);
            String newPlName = "new_pl";
            plUuid = new UUID(newPlName);
            Physical_Locator pl = new Physical_Locator();
            pl.setDst_ip(vtepIp);
            pl.setEncapsulation_type("vxlan_over_ipv4");
            transaction.addOperation(new InsertOperation(
                Physical_Locator.NAME.getName(), newPlName, pl));
        }

        // Check if the entry already exists
        List<UUID> entryIdList = findAllUcastMacRemoteWithLocator(
            node, mac, lsUuid, macIp, plUuid);
        if (!entryIdList.isEmpty()) {
            return new StatusWithUuid(StatusCode.SUCCESS,
                                      entryIdList.iterator().next());
        }

        // FIXME: Before adding an entry, we should check that there are
        // no stale entries with similar information (e.g. same mac going
        // to different physical locators).
        // This is tricky because a single entry can contain several
        // logical switch names and several end point locators; so by now
        // we assume that everything is all right - cleanup shoul be added
        // in a new patch.

        Ucast_Macs_Remote row = new Ucast_Macs_Remote();
        row.setMac(mac);
        row.setLocator(set(plUuid));
        row.setIpaddr(macIp);
        row.setLogical_switch(set(lsUuid));
        logger.debug("Add ucast mac remote row: {}", row);
        Operation op = new InsertOperation(Ucast_Macs_Remote.NAME.getName(),
                                           "new_ucast_mac", row);
        int insertIdx = transaction.getRequests().indexOf(op);
        transaction.addOperation(op);
        StatusWithUuid st = _insertTableRow(
            node, transaction, insertIdx, Ucast_Macs_Remote.NAME.getName(),
            "new_ucast_mac");
        logger.debug("Add ucast MAC remote result: " + st.getCode());
        return st;
    }

    /**
     * Deletes all remote unicast MAC entries for the specified logical switch.
     * The method returns a NotFound status when there is no logical switch,
     * there is no unicast MAC table, or there are no entries to delete.
     *
     * @param ls The logical switch name.
     * @param mac The MAC address.
     * @return The operation status code.
     */
    public Status vtepDelUcastMacRemote(Node node, String ls, String mac) {
        //this.dbName = "hardware_vtep";
        UUID lsId = findLogicalSwitch(node, ls);
        if (lsId == null) {
            logger.debug("No logical switch named " + ls);
            return new StatusWithUuid(StatusCode.NOTFOUND);
        }

        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(node).get(
                Ucast_Macs_Remote.NAME.getName());
        if (tableCache == null) {
            logger.debug("No cache table found for ucast MAC entries.");
            return new Status(StatusCode.NOTFOUND);
        }

        List<UUID> macIds = findAllUcastMacRemote(node, mac, lsId);
        if (macIds.isEmpty()) {
            logger.debug("Trying to delete non-existing MAC {} for logical "
                        + "switch {}", mac, ls);
            return new Status(StatusCode.NOTFOUND);
        }

        TransactBuilder transaction = new TransactBuilder(getDatabaseName());
        for (UUID id : macIds) {
            transaction.addOperation(
                new DeleteOperation(
                    Ucast_Macs_Remote.NAME.getName(),
                    new Condition("_uuid", Function.EQUALS, id)));
        }

        Status status = _doTableTransaction(
            node, transaction, Ucast_Macs_Remote.NAME.getName());

        logger.debug("Delete all ucast MAC remote result: " + status.getCode());
        return status;
    }

    /**
     * Deletes all remote unicast MAC entries for the specified logical switch.
     * The entries must match both the MAC and the IP addresses. The method
     * returns a NotFound status when there is no logical switch, there is no
     * unicast MAC table, or there are no entries to delete.
     *
     * @param ls The logical switch name.
     * @param mac The MAC address.
     * @param macIp The MAC IP address.
     * @return The operation status code.
     */
    public Status vtepDelUcastMacRemote(
        Node node, String ls, String mac, String macIp) {
        //this.dbName = "hardware_vtep";

        UUID lsId = findLogicalSwitch(node, ls);
        if (lsId == null) {
            logger.debug("No logical switch named " + ls);
            return new StatusWithUuid(StatusCode.NOTFOUND);
        }

        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(node).get(
                Ucast_Macs_Remote.NAME.getName());
        if (tableCache == null) {
            logger.debug("No cache table found for ucast MAC entries.");
            return new Status(StatusCode.NOTFOUND);
        }

        List<UUID> macIds = findAllUcastMacRemote(node, mac, lsId, macIp);
        if (macIds.isEmpty()) {
            logger.debug("Trying to delete non-existing MAC {} for logical "
                         + "switch {} with IP", mac, ls, macIp);
            return new Status(StatusCode.NOTFOUND);
        }

        TransactBuilder transaction = new TransactBuilder(getDatabaseName());
        for (UUID id : macIds) {
            transaction.addOperation(
                new DeleteOperation(
                    Ucast_Macs_Remote.NAME.getName(),
                    new Condition("_uuid", Function.EQUALS, id)));
        }

        Status status = _doTableTransaction(
            node, transaction, Ucast_Macs_Remote.NAME.getName());

        logger.debug("Delete all ucast MAC remote result: " + status.getCode());
        return status;
    }

    /**
     * Adds a remote multicast MAC address.
     * @param ci The command interpreter.
     */
    public void vtepAddMcastMacRemote(Node node, CommandInterpreter ci) {
        ci.println("Adding remote ucast, args: ls mac vtep_ip");
        String ls = ci.nextArgument();
        String mac = ci.nextArgument();
        String ip = ci.nextArgument();
        vtepAddMcastMacRemote(node, ls, mac, ip);
    }

    /**
     * Adds a remote multicast MAC address.
     * @param ls The logical switch name.
     * @param mac The MAC address.
     * @param ip The IP address.
     * @return The operation status.
     */
    public StatusWithUuid vtepAddMcastMacRemote(
        Node node, String ls, String mac, String ip) {

        //this.dbName = "hardware_vtep";

        UUID lsUuid = findLogicalSwitch(node, ls);
        if (lsUuid == null) {
            logger.error("No logical switch named " + ls);
            return new StatusWithUuid(StatusCode.NOTFOUND);
        }

        TransactBuilder transaction = new TransactBuilder(getDatabaseName());
        UUID plUuid= findPhysLocator(node, ip);
        if (plUuid == null) {
            logger.debug("New physical locator needed for " + ip);
            String newPlName = "new_pl";
            plUuid = new UUID(newPlName);
            Physical_Locator pl = new Physical_Locator();
            pl.setDst_ip(ip);
            pl.setEncapsulation_type("vxlan_over_ipv4");
            transaction.addOperation(new InsertOperation(
                Physical_Locator.NAME.getName(), newPlName, pl));
        }

        String newPlsName = "new_pls_name";
        UUID plsUuid = new UUID(newPlsName);
        Physical_Locator_Set pls = new Physical_Locator_Set();
        pls.setLocators(set(plUuid));
        transaction.addOperation(new InsertOperation(
            Physical_Locator_Set.NAME.getName(), newPlsName, pls));

        Mcast_Macs_Remote rowMcast = new Mcast_Macs_Remote();
        rowMcast.setMac(mac);
        rowMcast.setLocator_set(set(plsUuid));
        rowMcast.setLogical_switch(set(lsUuid));
        Operation opMcast = new InsertOperation(Mcast_Macs_Remote.NAME.getName(),
                                                "new_mcast_mac", rowMcast);

        transaction.addOperation(opMcast);

        int insertIdx = transaction.getRequests().indexOf(opMcast);
        StatusWithUuid st = _insertTableRow(node, transaction, insertIdx,
                                            Mcast_Macs_Remote.NAME.getName(),
                                            "new_mcast_mac");
        logger.debug("Add mcast remote result: " + st.getCode());
        return st;
    }

    /**
     * Deletes all remote multicast MAC entries for the specified logical switch.
     * The method returns a NotFound status when there is no logical switch,
     * there is no unicast MAC table, or there are no entries to delete.
     *
     * @param node The node serving this configuration service.
     * @param ls The logical switch name.
     * @param mac The MAC address.
     * @return The operation status code.
     */
    public Status vtepDelMcastMacRemote(Node node, String ls, String mac) {
        //this.dbName = "hardware_vtep";

        UUID lsId = findLogicalSwitch(node, ls);
        if (lsId == null) {
            logger.error("No logical switch named " + ls);
            return new StatusWithUuid(StatusCode.NOTFOUND);
        }

        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(node).get(
                Mcast_Macs_Remote.NAME.getName());
        if (tableCache == null) {
            logger.warn("No cache table found for mcast MAC entries.");
            return new Status(StatusCode.NOTFOUND);
        }

        List<UUID> macIds = findAllMcastMacRemote(node, mac, lsId);
        if (macIds.isEmpty()) {
            logger.debug("Trying to delete non-existing MAC {} for logical "
                         + "switch {}", mac, ls);
            return new Status(StatusCode.NOTFOUND);
        }

        TransactBuilder transaction = new TransactBuilder(getDatabaseName());
        for (UUID id : macIds) {
            transaction.addOperation(
                new DeleteOperation(
                    Mcast_Macs_Remote.NAME.getName(),
                    new Condition("_uuid", Function.EQUALS, id)));
        }

        Status status = _doTableTransaction(
            node, transaction, Mcast_Macs_Remote.NAME.getName());

        logger.debug("Delete all mcast MAC remote result: " + status.getCode());
        return status;
    }

    public void vtepListPs(Node node, CommandInterpreter ci) {
        listToCI(node, ci, Physical_Switch.NAME.getName());
    }

    public void vtepListLs(Node node, CommandInterpreter ci) {
        listToCI(node, ci, Logical_Switch.NAME.getName());
    }

    public void vtepListPsPorts(Node node, CommandInterpreter ci) {
        listToCI(node, ci, Physical_Port.NAME.getName());
    }

    public void vtepListMcastMacs(Node node, CommandInterpreter ci) {
        listToCI(node, ci, Mcast_Macs_Local.NAME.getName());
        ci.println("------");
        listToCI(node, ci, Mcast_Macs_Remote.NAME.getName());
    }

    public void vtepListUcastMacs(Node node, CommandInterpreter ci) {
        listToCI(node, ci, Ucast_Macs_Local.NAME.getName());
        ci.println("------");
        listToCI(node, ci, Ucast_Macs_Remote.NAME.getName());
    }

    private <T> OvsDBSet<T> set(T item) {
        OvsDBSet<T> s = new OvsDBSet<>();
        s.add(item);
        return s;
    }

    private UUID findPhysicalSwitch(Node n, String psName) {
        Map<String, Table<?>> psCache =
            inventoryServiceInternal.getCache(n).get(
                Physical_Switch.NAME.getName());
        if (psCache == null) {
            return null;
        }
        for (Map.Entry<String, Table<?>> e : psCache.entrySet()) {
            Physical_Switch ps = (Physical_Switch)e.getValue();
            if (ps.getName().equals(psName)) {
                return new UUID(e.getKey());
            }
        }
        return null;
    }

    private UUID findLogicalSwitch(Node n, String lsName) {
        Map<String, Table<?>> lsCache =
            inventoryServiceInternal.getCache(n).get(
                Logical_Switch.NAME.getName());
        if (lsCache == null) {
            return null;
        }
        for (Map.Entry<String, Table<?>> e : lsCache.entrySet()) {
            Logical_Switch ls = (Logical_Switch)e.getValue();
            if (ls.getName().equals(lsName)) {
                return new UUID(e.getKey());
            }
        }
        return null;
    }

    /**
     * Finds all unicast remote MAC addresses for the specified logical switch,
     * that have the given MAC address.
     *
     * @param n The DB node.
     * @param mac The MAC address.
     * @param lsId The logical switch DB entry identifier.
     * @return The list of MAC entry identifiers.
     */
    private List<UUID> findAllUcastMacRemote(Node n, String mac, UUID lsId) {
        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(n).get(
                Ucast_Macs_Remote.NAME.getName());
        List<UUID> idList = new ArrayList<>();
        if (tableCache == null) {
            return idList;
        }
        for (Map.Entry<String, Table<?>> e : tableCache.entrySet()) {
            Ucast_Macs_Remote umr = (Ucast_Macs_Remote)e.getValue();
            if (umr.getMac().equalsIgnoreCase(mac) &&
                umr.getLogical_switch().contains(lsId)) {
                idList.add(new UUID(e.getKey()));
            }
        }
        return idList;
    }

    /**
     * Finds all unicast remote MAC addresses for the specified logical switch,
     * that have the given MAC IP address. If the MAC  IP address is <b>null</b>
     * then the MAC IP in the row must also be null.
     *
     * @param n The DB node.
     * @param mac The MAC address.
     * @param lsId The logical switch DB entry identifier.
     * @param macIp The MAC IP address.
     * @return The list of MAC entry identifiers.
     */
    private List<UUID> findAllUcastMacRemote(
        Node n, String mac, UUID lsId, String macIp) {
        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(n).get(
                Ucast_Macs_Remote.NAME.getName());
        List<UUID> idList = new ArrayList<>();
        if (tableCache == null) {
            return idList;
        }
        for (Map.Entry<String, Table<?>> e : tableCache.entrySet()) {
            Ucast_Macs_Remote umr = (Ucast_Macs_Remote)e.getValue();
            if (umr.getMac().equalsIgnoreCase(mac) &&
                ((macIp == null && StringUtils.isEmpty(umr.getIpaddr())) ||
                 (macIp != null && macIp.equals(umr.getIpaddr()))) &&
                umr.getLogical_switch().contains(lsId)) {
                idList.add(new UUID(e.getKey()));
            }
        }
        return idList;
    }

    /**
     * Finds all unicast remote MAC entries for the specified logical switch,
     * that have the given MAC IP address, and physical locator. If the MAC
     * IP address is <b>null</b> then the MAC IP in the row must also be null.
     *
     * @param n The DB node.
     * @param mac The MAC address.
     * @param lsId The logical switch DB entry identifier.
     * @param macIp The MAC IP address.
     * @param physLoc The physical locator.
     * @return The list of MAC entry identifiers.
     */
    private List<UUID> findAllUcastMacRemoteWithLocator(
        Node n, String mac, UUID lsId, String macIp, UUID physLoc) {
        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(n).get(
                Ucast_Macs_Remote.NAME.getName());
        List<UUID> idList = new ArrayList<>();
        if (tableCache == null) {
            return idList;
        }
        for (Map.Entry<String, Table<?>> e : tableCache.entrySet()) {
            Ucast_Macs_Remote umr = (Ucast_Macs_Remote) e.getValue();
            if (umr.getMac().equalsIgnoreCase(mac) &&
                ((macIp == null && StringUtils.isEmpty(umr.getIpaddr())) ||
                 (macIp != null && macIp.equals(umr.getIpaddr()))) &&
                umr.getLocator().contains(physLoc) &&
                umr.getLogical_switch().contains(lsId)) {
                idList.add(new UUID(e.getKey()));
            }
        }
        return idList;
    }

    /**
     * Finds all multicast remote MAC addresses for the specified logical
     * switch, that have the given MAC address.
     *
     * @param n The DB node.
     * @param mac The MAC address.
     * @param lsId The logical switch DB entry identifier.
     * @return The list of MAC entry identifiers.
     */
    private List<UUID> findAllMcastMacRemote(Node n, String mac, UUID lsId) {
        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(n).get(
                Mcast_Macs_Remote.NAME.getName());
        List<UUID> idList = new ArrayList<>();
        if (tableCache == null) {
            return idList;
        }
        for (Map.Entry<String, Table<?>> e : tableCache.entrySet()) {
            Mcast_Macs_Remote mmr = (Mcast_Macs_Remote)e.getValue();
            if (mmr.getMac().equalsIgnoreCase(mac) &&
                mmr.getLogical_switch().contains(lsId)) {
                idList.add(new UUID(e.getKey()));
            }
        }
        return idList;
    }

    private UUID findPhysLocator(Node n, String ip) {

        Map<String, Table<?>> plCache = inventoryServiceInternal.getCache(n)
                            .get(Physical_Locator.NAME.getName());
        if (plCache == null) {
            return null;
        }

        for (Map.Entry<String, Table<?>> e : plCache.entrySet()) {
            Physical_Locator pl = (Physical_Locator)e.getValue();
            if (pl.getDst_ip().equals(ip)) {
                return new UUID(e.getKey());
            }
        }
        return null;
    }

    private UUID findPhysPort(Node n, String portName) {
        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(n)
                                    .get(Physical_Port.NAME.getName());
        if (tableCache == null) {
            return null;
        }
        for (Map.Entry<String, Table<?>> e : tableCache.entrySet()) {
            Physical_Port physPort = (Physical_Port)e.getValue();
            if (physPort.getName().equals(portName)) {
                return new UUID(e.getKey());
            }
        }
        return null;
    }

    public Logical_Switch vtepGetLogicalSwitch(Node n, String lsName) {
        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(n)
                                    .get(Logical_Switch.NAME.getName());
        if (tableCache == null) {
            return null;
        }
        for (Map.Entry<String, Table<?>> e : tableCache.entrySet()) {
            Logical_Switch logicalSwitch = (Logical_Switch)e.getValue();
            logicalSwitch.setId(new UUID(e.getKey()));
            if (logicalSwitch.getName().equals(lsName)) {
                return logicalSwitch;
            }
        }
        return null;
    }

    public Physical_Switch vtepGetPhysicalSwitch(Node n, String psName) {
        Map<String, Table<?>> tableCache =
            inventoryServiceInternal.getCache(n)
                                    .get(Physical_Switch.NAME.getName());
        if (tableCache == null) {
            return null;
        }
        for (Map.Entry<String, Table<?>> e : tableCache.entrySet()) {
            Physical_Switch physSwitch = (Physical_Switch)e.getValue();
            if (physSwitch.getName().equals(psName)) {
                return physSwitch;
            }
        }
        return null;
    }

    public Physical_Port vtepGetPhysicalPort(Node n, String portName) {
        Map<String, Table<?>> portCache =
            inventoryServiceInternal.getCache(n)
                                    .get(Physical_Port.NAME.getName());
        if (portCache == null) {
            return null;
        }
        for (Map.Entry<String, Table<?>> e : portCache.entrySet()) {
            Physical_Port physPort = (Physical_Port)e.getValue();
            if (physPort.getName().equals(portName)) {
                return physPort;
            }
        }
        return null;
    }

    /**
     * From cache.
     * @param tableName
     * @return
     */
    private Set<Map.Entry<String, Table<?>>> list(Node node, String tableName) {
        //this.dbName = "hardware_vtep";
        Map<String, Table<?>> tableCache =
            this.inventoryServiceInternal.getCache(node).get(tableName);
        if (tableCache == null || tableCache.isEmpty()) {
            return new HashSet<>();
        }
        return tableCache.entrySet();
    }

    /**
     * From cache
     */
    private void listToCI(Node node, CommandInterpreter ci, String tableName) {
        //this.dbName = "hardware_vtep";
        ci.println("");
        ci.println("Listing " + tableName);
        ci.println("");
        Set<Map.Entry<String, Table<?>>> entries = list(node, tableName);
        if (entries.isEmpty()) {
            ci.println("No entries returned");
        }
        for (Map.Entry<String, Table<?>> e : entries) {
            ci.println("> " + e.getValue());
            ci.println("  uuid: " + e.getKey());
        }
        ci.println("");
    }

    private List<String> toColumns(Column[] cols) {
        List<String> columns = new ArrayList<>();
        columns.add("_uuid");
        for (Column c : cols) { columns.add(c.toString()); }
        return columns;
    }

    private void queryTable(CommandInterpreter ci, Node node, String tableName,
                            Condition where, List<String> columns) {

        //this.dbName = "hardware_vtep";
        Map<String, Table<?>> t = inventoryServiceInternal.getTableCache(
            node, tableName);
        if (t == null) {
            ci.println("Table cache not found");
            return;
        }

        Operation opGet = new SelectOperation(tableName, where, columns);
        TransactBuilder tr = makeTransaction(opGet);

        try {
            List<OperationResult> opRes = getConnection(node).getRpc()
                .transact(tr).get();
            logger.debug("RESULT: {}", opRes);
            for (OperationResult r : opRes) {
                for (Object row : r.getRows()) {
                    ci.println("--> " + row);
                }
            }
        } catch (Exception e) {
            ci.println("ERROR! " + e.getMessage());
        }

        //this.dbName = Open_vSwitch.NAME.getName();

    }

    private StatusWithUuid insManager(Node node, String globalUuid,
                                      Manager row) {
        Map<String, Table<?>> globalTable = inventoryServiceInternal
            .getTableCache(node, Global.NAME.getName());

        if (globalTable == null) {
            return new StatusWithUuid(StatusCode.NOTFOUND,
                                      "No instance in the Global table");
        }

        if (globalUuid == null) {
            globalUuid = globalTable.keySet().iterator().next();
        }

        String newManager = "new_manager";
        Operation opMutateGlobal = new MutateOperation(
            Global.NAME.getName(),
            new Condition("_uuid", Function.EQUALS, new UUID(globalUuid)),
            new Mutation("managers", Mutator.INSERT, new UUID(newManager))
        );

        String tableName = Manager.NAME.getName();
        Operation opInsertManager = new
            InsertOperation(tableName, newManager, row);

        TransactBuilder transaction = new TransactBuilder(getDatabaseName());
        transaction.addOperations(Arrays.asList(opMutateGlobal,
                                                opInsertManager));

        int insertIndex = transaction.getRequests().indexOf(opMutateGlobal);
        return _insertTableRow(node, transaction, insertIndex,
                               tableName, tableName);
    }

    private StatusWithUuid insPhysicalPort(Node node, String switchUuid,
                                           Physical_Port row) {
        String pSwitchTableName = Physical_Switch.NAME.getName();

        Map<String, Table<?>> swTable =
            inventoryServiceInternal.getTableCache(node, pSwitchTableName);

        if (swTable == null || swTable.get(switchUuid) == null) {
            return new StatusWithUuid(
                StatusCode.NOTFOUND,
                "No instance in the " + pSwitchTableName + " table");
        }

        String newPort = "new_phys_port";
        UUID portUuid = new UUID(newPort);
        UUID physSwitchUuid = new UUID(switchUuid);

        // add to the phys switch table's "ports" column
        Operation opMutateSwitch = new MutateOperation(
            Physical_Switch.NAME.getName(),
            new Condition("_uuid", Function.EQUALS, physSwitchUuid),
            new Mutation("ports", Mutator.INSERT, portUuid)
        );

        // the port row
        String rowName = Physical_Port.NAME.getName();
        Operation opInsPort = new InsertOperation(rowName, newPort, row);

        try {
            TransactBuilder transaction = makeTransaction(Arrays.asList(
                opMutateSwitch, opInsPort));
            int insertIdx = transaction.getRequests().indexOf(opInsPort);
            return _insertTableRow(node, transaction, insertIdx,
                                   "error inserting port", "ports");
        } catch (Exception e) {
            logger.error("Exception inserting physical port", e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insLogicalSwitch(Node node,
                                            Logical_Switch row) {
        return doInsertTransact(node, Logical_Switch.NAME.getName(),
                                "new_log_switch", row);
    }
}


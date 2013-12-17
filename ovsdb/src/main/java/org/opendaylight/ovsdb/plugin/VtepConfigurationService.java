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

import java.util.Arrays;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.message.TransactBuilder;
import org.opendaylight.ovsdb.lib.message.operations.InsertOperation;
import org.opendaylight.ovsdb.lib.message.operations.MutateOperation;
import org.opendaylight.ovsdb.lib.message.operations.Operation;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Manager;
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
 * Offers VTEP configuration operations.
 */
public class VtepConfigurationService extends ConfigurationServiceBase {

    @Override
    String getDatabaseName() { return "hardware_vtep"; }

    StatusWithUuid insertRow(Node node, String parentUuid, Table<?> row) {
        StatusWithUuid statusWithUUID = null;
        String tableName = row.getTableName().getName();

        try {
            if (tableName.equalsIgnoreCase("Physical_Port")) {
                statusWithUUID = insPhysicalPort(node, parentUuid,
                                                 (Physical_Port)row);
            } else if (tableName.equalsIgnoreCase("Physical_Locator_Set")) {
                statusWithUUID = insPhysicalLocatorSet(node,
                                                       (Physical_Locator_Set)row);
            } else if (tableName.equalsIgnoreCase("Manager")) {
                statusWithUUID = insManager(node, parentUuid, (Manager)row);
            } else if (tableName.equalsIgnoreCase("Global")) {
                statusWithUUID = insGlobal(node, (Global)row);
            } else if (tableName.equalsIgnoreCase("Physical_Switch")) {
                statusWithUUID = insPhysicalSwitch(node, parentUuid,
                                                   (Physical_Switch) row);
            } else if (tableName.equalsIgnoreCase("Logical_Switch")) {
                statusWithUUID = insLogicalSwitch(node, (Logical_Switch) row);
            } else if (tableName.equalsIgnoreCase("Ucast_Macs_Local")) {
                statusWithUUID = insUcastMacsLocal(node,
                                                   (Ucast_Macs_Local) row);
            } else if (tableName.equalsIgnoreCase("Ucast_Macs_Remote")) {
                statusWithUUID = insUcastMacsRemote(node,
                                                    (Ucast_Macs_Remote) row);
            } else if (tableName.equalsIgnoreCase("Mcast_Macs_Local")) {
                statusWithUUID = insMcastMacsLocal(node,
                                                   (Mcast_Macs_Local) row);
            } else if (tableName.equalsIgnoreCase("Mcast_Macs_Remote")) {
                statusWithUUID = insMcastMacsRemote(node,
                                                    (Mcast_Macs_Remote) row);
            } else if (tableName.equalsIgnoreCase("Logical_Binding_Stats")) {
                statusWithUUID = insLogicalBindingStats(node, parentUuid,
                                                        row);
            } else if (tableName.equalsIgnoreCase("Physical_Locator")) {
                statusWithUUID = insPhysicalLocator(node,
                                                    (Physical_Locator) row);
            } else if (tableName.equalsIgnoreCase("Logical_Router")) {
                statusWithUUID = insLogicalRouter(node, parentUuid, row);
            }
        } catch (Exception e) {
            logger.error("Error in VTEP insertRow", e);
            statusWithUUID = new StatusWithUuid(StatusCode.INTERNALERROR);
        }

        return statusWithUUID;
    }

    // INSERTS

    /**
     * @param node the node.
     * @param globalUuid the uuid of the global table (needed? this has to
     *                   be unique according to spec so I dont't really see
     *                   the point, leaving to keep analogous to the ovs
     *                   ones.
     * @param row the row to be inserted.
     * @return the resulting status of the operation.
     */
    private StatusWithUuid insPhysicalSwitch(Node node,
                                             String globalUuid,
                                             Physical_Switch row) {
        Map<String, Table<?>> globalTable = inventoryServiceInternal
            .getTableCache(node, Global.NAME.getName());

        if (globalTable == null) {
            return new StatusWithUuid(StatusCode.NOTFOUND,
                                      "No instance in the Global table");
        }

        if (globalUuid == null) {
            globalUuid = globalTable.keySet().iterator().next();
        }

        String newSwitch = "new_phys_switch"; // TODO shouldn't be random?
        Operation opMutateGlobal = new MutateOperation(
            Global.NAME.getName(),
            new Condition("_uuid", Function.EQUALS, new UUID(globalUuid)),
            new Mutation("switches", Mutator.INSERT, new UUID(newSwitch))
        );

        String tableName = Physical_Switch.NAME.getName();
        Operation opInsertSwitch = new
            InsertOperation(tableName, newSwitch, row);

        TransactBuilder transaction = new TransactBuilder(getDatabaseName());
        transaction.addOperations(Arrays.asList(opMutateGlobal,
                                                opInsertSwitch));

        int insertIndex = transaction.getRequests().indexOf(opMutateGlobal);
        return _insertTableRow(node, transaction, insertIndex,
                               tableName, tableName);
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
        Map<String, Table<?>> switchTable =
            inventoryServiceInternal.getTableCache(node, pSwitchTableName);

        if (switchTable == null || switchTable.get(switchUuid) == null) {
            return new StatusWithUuid(StatusCode.NOTFOUND,
                                      "No instance in the "+pSwitchTableName+ " table");
        }

        String tableName = Physical_Port.NAME.getName();
        String newPort = "new_phys_port";
        UUID portUuid = new UUID(newPort);
        Operation opMutateSwitch = new MutateOperation(tableName,
                           new Condition("_uuid", Function.EQUALS, switchUuid),
                           new Mutation(tableName, Mutator.INSERT, portUuid)
        );

        Operation opInsPort = new InsertOperation(tableName, newPort, row);

        TransactBuilder transaction = new TransactBuilder(getDatabaseName());
        transaction.addOperations(Arrays.asList(opMutateSwitch,
                                                opInsPort));

        int insertIdx = transaction.getRequests().indexOf(opInsPort);
        return _insertTableRow(node, transaction, insertIdx,
                               tableName, tableName);
    }

    private StatusWithUuid insLogicalSwitch(Node node,
                                            Logical_Switch row) {
        return doInsertTransact(node, "new_logical_switch",
                                Logical_Switch.NAME.getName(), row);
    }

    private StatusWithUuid insPhysicalLocator(Node node,
                                              Physical_Locator row) {
        return doInsertTransact(node, Physical_Locator.NAME.getName(),
                                "new_phys_locator", row);
    }

    private StatusWithUuid insUcastMacsRemote(Node node,
                                              Ucast_Macs_Remote row) {
        return doInsertTransact(node, Ucast_Macs_Remote.NAME.getName(),
                                "new_ucast_macs_remote", row);
    }

    private StatusWithUuid insUcastMacsLocal(Node node,
                                             Ucast_Macs_Local row) {
        return doInsertTransact(node, Ucast_Macs_Local.NAME.getName(),
                                "new_ucast_macs_local", row);
    }

    private StatusWithUuid insMcastMacsLocal(Node node,
                                             Mcast_Macs_Local row) {
        return doInsertTransact(node, Mcast_Macs_Local.NAME.getName(),
                                "new_mcast_macs_local", row);
    }

    private StatusWithUuid insMcastMacsRemote(Node node,
                                              Mcast_Macs_Remote row) {
        return doInsertTransact(node, Mcast_Macs_Remote.NAME.getName(),
                                "new_mcast_macs_remote", row);
    }

    private StatusWithUuid insPhysicalLocatorSet(Node node,
                                                 Physical_Locator_Set row) {
        return doInsertTransact(node, Physical_Locator_Set.NAME.getName(),
                                "new_phys_locator_set", row);
    }

    private StatusWithUuid insGlobal(Node node, Global row) {
        return doInsertTransact(node, Global.NAME.getName(), "new_global",
                                row);
    }

    private StatusWithUuid insLogicalRouter(Node node,
                                            String parentUuid, Table<?> row) {
        return null;
    }

    private StatusWithUuid insLogicalBindingStats(Node node, String parentUuid,
                                                  Table<?> row) {
        return null;
    }

    @Override
    public String getHelp() {
        StringBuilder help = new StringBuilder();
        help.append("---OVSDB VTEP CLI---\n");
        help.append("TODO");
        return help.toString();
    }

}


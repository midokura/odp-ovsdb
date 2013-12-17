package org.opendaylight.ovsdb.plugin;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.message.TransactBuilder;
import org.opendaylight.ovsdb.lib.message.operations.DeleteOperation;
import org.opendaylight.ovsdb.lib.message.operations.InsertOperation;
import org.opendaylight.ovsdb.lib.message.operations.MutateOperation;
import org.opendaylight.ovsdb.lib.message.operations.Operation;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;
import org.opendaylight.ovsdb.lib.message.operations.UpdateOperation;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfigurationServiceBase implements OVSDBConfigService,
                                                          CommandProvider {

    static final Logger logger = LoggerFactory.getLogger(
        ConfigurationService.class);

    public static final String DEFAULT_OVSDB_PORT = "6634";

    IConnectionServiceInternal connectionService;
    InventoryServiceInternal inventoryServiceInternal;
    boolean forceConnect = false;

    /**
     * Implement returning the appropriate database name managed by the
     * child ConfigurationService.
     *
     * @return the database name.
     */
    abstract String getDatabaseName();

    void init() {
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        registerWithOSGIConsole();
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                                                   .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                                      null);
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

    public void setConnectionServiceInternal(IConnectionServiceInternal connectionService) {
        this.connectionService = connectionService;
    }

    public void unsetConnectionServiceInternal(IConnectionServiceInternal connectionService) {
        if (this.connectionService == connectionService) {
            this.connectionService = null;
        }
    }

    public void setInventoryServiceInternal(InventoryServiceInternal inventoryServiceInternal) {
        this.inventoryServiceInternal = inventoryServiceInternal;
    }

    public void unsetInventoryServiceInternal(InventoryServiceInternal inventoryServiceInternal) {
        if (this.inventoryServiceInternal == inventoryServiceInternal) {
            this.inventoryServiceInternal = null;
        }
    }

    @Override
    public Status updateRow (Node node, String tableName, String parentUUID, String rowUUID, Table<?> row) {
        try {
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }

            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            String dbName = getDatabaseName();
            Map<String, Table<?>> table = inventoryServiceInternal.getTableCache(node, dbName);
            if (table == null) {
                return new Status(StatusCode.NOTFOUND, "No table cache for db:" + dbName);
            }

            UUID uuid = new UUID(rowUUID);
            Operation updateRequest = new UpdateOperation(
                tableName, new Condition("_uuid", Function.EQUALS, uuid), row);

            TransactBuilder transaction = new TransactBuilder(getDatabaseName());
            transaction.addOperations(Arrays.asList(updateRequest));

            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();
            Status status = new StatusWithUuid(StatusCode.SUCCESS);
            for (int i = 0; i < tr.size() ; i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i) != null && tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new StatusWithUuid(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size()-1);
                logger.error("Error Updating Row : {}/{}\n Error : {}\n Details : {}", tableName, row,
                             result.getError(),
                             result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            if (status.isSuccess()) {
                status = new Status(StatusCode.SUCCESS);
            }
            return status;
        } catch (Exception e){
            logger.error("Error in updateRow(): ",e);
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    @Override
    public Map<String, Table<?>> getRows(Node node, String tableName) throws Exception{
        try {
            if (inventoryServiceInternal == null) {
                throw new Exception("Inventory Service is Unavailable.");
            }
            return inventoryServiceInternal.getTableCache(node, tableName);
        } catch (Exception e){
            throw new Exception("Unable to read table due to "+e.getMessage());
        }
    }

    @Override
    public Table<?> getRow(Node node, String tableName, String uuid) throws Exception {
        try {
            if (inventoryServiceInternal == null) {
                throw new Exception("Inventory Service is Unavailable.");
            }
            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node, tableName);
            return (ovsTable == null) ? null :  ovsTable.get(uuid);
        } catch (Exception e){
            throw new Exception("Unable to read table due to "+e.getMessage());
        }
    }

    @Override
    public String getSerializedRows(Node node, String tableName) throws Exception{
        try {
            Map<String, Table<?>> ovsTable = this.getRows(node, tableName);
            if (ovsTable == null) return null;
            return new ObjectMapper().writeValueAsString(ovsTable);
        } catch (Exception e){
            throw new Exception("Unable to read table due to "+e.getMessage());
        }
    }

    @Override
    public String getSerializedRow(Node node, String tableName, String uuid) throws Exception {
        try {
            Table<?> row = this.getRow(node, tableName, uuid);
            if (row == null) return null;
            return new ObjectMapper().writeValueAsString(row);
        } catch (Exception e){
            throw new Exception("Unable to read table due to "+e.getMessage());
        }
    }

    @Override
    public List<String> getTables(Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Retrieve the connection to the given node, if there is one, and it is
     * active.
     *
     * @param node Node serving the configuration service
     * @return the connection to the node.
     */
    Connection getConnection (Node node) {
        // Check for connection before calling RPC to perform transaction
        if (connectionService == null) {
            logger.error("Couldn't refer to the ConnectionService");
            return null;
        }
        Connection connection = connectionService.getConnection(node);
        if (connection == null || !connection.getChannel().isActive()) {
            logger.info("Connection channel inactive");
            return null;
        }
        return connection;
    }

    StatusWithUuid _insertTableRow(Node node, TransactBuilder transaction, Integer insertIndex, String insertErrorMsg, String rowName){

        try {
            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new StatusWithUuid(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(
                transaction);
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();
            StatusWithUuid status = new StatusWithUuid(StatusCode.SUCCESS);
            for (int i = 0; i < tr.size() ; i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i) != null && tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new StatusWithUuid(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size()-1);
                logger.error("Error creating {} : {}\n Error : {}\n Details : {}",
                             insertErrorMsg,
                             rowName,
                             result.getError(),
                             result.getDetails());
                status = new StatusWithUuid(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            if (status.isSuccess()) {
                if (insertIndex >= 0 && insertIndex < tr.size() && tr.get(insertIndex) != null) {
                    UUID uuid = tr.get(insertIndex).getUuid();
                    status = new StatusWithUuid(StatusCode.SUCCESS, uuid);
                } else {
                    // We can't get the uuid from the transact as the
                    // insertIndex is invalid or -1 return null uuid.
                    status = new StatusWithUuid(StatusCode.SUCCESS);
                }
            }
            return status;
        } catch(Exception e){
            logger.error("Error in _insertTableRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    Status _deleteTableRow(Node node,String uuid,String parentTableName, String childTableName, String parentColumn) {
        try {
            // Establish the connection
            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            // Remove from Parent and Child
            Map<String, Table<?>> parentTable = inventoryServiceInternal.getTableCache(node, parentTableName);
            Map<String, Table<?>> childTable = inventoryServiceInternal.getTableCache(node, childTableName);

            // Check that the UUID exists
            if (parentTable == null || childTable == null || uuid == null || childTable.get(uuid) == null) {
                return new Status(StatusCode.NOTFOUND, "UUID: " + uuid);
            }

            UUID rowUuid = new UUID(uuid);
            // Capture all rows in the parent table (ie duplicates) that have the child UUID
            Condition where = new Condition(parentColumn, Function.INCLUDES, rowUuid);
            // Prepare the mutator to remove the child UUID from the parentColumn list in the parent TABLE
            Mutation mutator = new Mutation(parentColumn, Mutator.DELETE, rowUuid);
            Operation delRequest = new MutateOperation(parentTableName, where, mutator);

            TransactBuilder transaction = new TransactBuilder(getDatabaseName());
            transaction.addOperations(Arrays.asList(delRequest));
            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);

            Status status = new Status(StatusCode.SUCCESS);

            // Pull the responses
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();
            for (int i = 0; i < tr.size(); i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i) != null && tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size() - 1);
                logger.error("Error deleting: {}\n Error : {}\n Details : {}",
                             uuid, result.getError(), result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            return status;
        } catch (Exception e) {
            logger.error("Error in _deleteTableRow",e);
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    Status _deleteRootTableRow(Node node,String uuid,String tableName) {
        try {

            // Establish the connection
            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            Map<String, Table<?>> table = inventoryServiceInternal.getTableCache(node, tableName);

            // Check that the UUID exists
            if (table == null || table.get(uuid) == null) {
                return new Status(StatusCode.NOTFOUND, "");
            }

            // Initialise the actual request var
            UUID rowUuid = new UUID(uuid);
            Condition where = new Condition("_uuid", Function.EQUALS, rowUuid);
            Operation delRequest = new DeleteOperation(tableName, where);

            TransactBuilder transaction = new TransactBuilder(getDatabaseName());
            transaction.addOperations(Arrays.asList(delRequest));

            // This executes the transaction.
            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);

            // Pull the responses
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
                logger.error("Error deleting: {}\n Error : {}\n Details : {}",
                             uuid, result.getError(), result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            return status;
        } catch (Exception e) {
            logger.error("Error in _deleteRootTableRow",e);
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    /*
     * Convenience method, does a single Op transaction, useful in a bunch
     * of operations below.
     */
    StatusWithUuid doInsertTransact(Node node, String tableName,
                                    String uuidName, Table<?> row) {
        Operation op = new InsertOperation(tableName, uuidName, row);
        TransactBuilder transaction = new TransactBuilder(getDatabaseName());
        transaction.addOperation(op);
        int insertIdx = transaction.getRequests().indexOf(op);
        return _insertTableRow(node, transaction, insertIdx, tableName,
                               tableName);
    }

    // Service operations

    public void _connect(CommandInterpreter ci) {

        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }

        String server = ci.nextArgument();
        if (server == null) {
            ci.println("Please enter valid IP-Address");
            return;
        }

        try {
            InetAddress ia = InetAddress.getByName(server);
            server = ia.getHostAddress();
        }  catch (UnknownHostException e) {
            logger.error("Unable to resolve " + server, e);
            ci.println("Please enter valid IP-Address");
            return;
        }

        String port = ci.nextArgument();
        if (port == null) {
            port = ConfigurationServiceBase.DEFAULT_OVSDB_PORT;
        }

        ci.println("Connecting to ovsdb server: "+server+":"+port+" ... ");
        Map<ConnectionConstants, String> params = new HashMap<>();
        params.put(ConnectionConstants.ADDRESS, server);
        params.put(ConnectionConstants.PORT, port);
        Node node = connectionService.connect(nodeName, params);
        if (node != null) ci.println("Node Name: " + node);
        else ci.println("Could not connect to Node " + nodeName);
    }

    public void _printCache (CommandInterpreter ci) {
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        inventoryServiceInternal.printCache(node);
    }

    public void _forceConnect (CommandInterpreter ci) {
        String force = ci.nextArgument();
        if (force.equalsIgnoreCase("YES")) {
            forceConnect = true;
        }
        else if (force.equalsIgnoreCase("NO")) {
            forceConnect = false;
        }
        else {
            ci.println("Please enter YES or NO.");
        }
        ci.println("Current ForceConnect State : "+forceConnect);
    }

}

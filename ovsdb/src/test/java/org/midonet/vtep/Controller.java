/*
 * Copyright (c) 2014 Midokura Europe SARL, All Rights Reserved.
 */
package org.midonet.vtep;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.ConfigurationService;
import org.opendaylight.ovsdb.plugin.ConnectionService;
import org.opendaylight.ovsdb.plugin.InventoryService;
import org.osgi.framework.Bundle;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A dumb utility class to ease running arbitrary code during tests.
 */
class Controller {

    private static final org.slf4j.Logger log = getLogger(Controller.class);

    private static String nodeName = "OVS|vtep";

    private CommandInterpreter ci = new CommandInterpreter() {

        @Override
        public String nextArgument() {
            return null;
        }

        @Override
        public Object execute(String s) {
            return null;
        }

        @Override
        public void print(Object o) {

        }

        @Override
        public void println() {
        }

        @Override
        public void println(Object o) {
            log.info("> {}", o.toString());
        }

        @Override
        public void printStackTrace(Throwable throwable) {

        }

        @Override
        public void printDictionary(Dictionary dictionary, String s) {

        }

        @Override
        public void printBundleResource(Bundle bundle, String s) {

        }
    };

    private ConnectionService conSrv = null;
    public Node node = null;
    private ConfigurationService cfgSrv = null;

    private String ip;
    private String port;

    Controller(String ip, String port) {
        this.ip = ip;
        this.port = port;
    }

    private void init() {
        conSrv = new ConnectionService();
        Map<ConnectionConstants, String> params = new HashMap<>();

        log.info("Connecting to {}:{}", ip, port);
        Properties props = new Properties();
        params.put(ConnectionConstants.ADDRESS,
                   props.getProperty("ovsdbserver.ipaddress", ip));
        params.put(ConnectionConstants.PORT,
                   props.getProperty("ovsdbserver.port", port));

        InventoryService is= new InventoryService();

        conSrv.setInventoryServiceInternal(is);
        node = conSrv.connect(nodeName, params);
        log.info("NODE NAME: {}", node.getID());
        cfgSrv = new ConfigurationService();
        cfgSrv.setInventoryServiceInternal(is);
        cfgSrv.setConnectionServiceInternal(conSrv);
    }

    public static void main(String[] args) {
        Controller c = new Controller(args[0], args[1]);
        c.init();
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            log.error("OH", e);
        }
        Map<String, Table<?>> tableCache =
            c.conSrv.getInventoryServiceInternal().getCache(c.node).get("Logical_Switch");
        for (Map.Entry<String, Table<?>> e : tableCache.entrySet()) {
            log.info("> " + e.getValue());
            log.info("  uuid: " + e.getKey());
        }

        //c.cfgSrv.vtepAddLogicalSwitch("testLs", 2323);
        c.cfgSrv.vtepBindVlan(c.node, "testLs1", "in1", (short)2323, 3222, null);

        // INVOKE
        // c.cfgSrv.vtepDelLogicalSwitch("midonet-1d22f1be-93ba-42ae-8b3c-ed8b604cc643");
        // c.cfgSrv.vtepDelBinding("br0", "eth0", 78);
    }
}

package com.github.rinde.rinsim.geom;

import com.github.christofluyten.IO.IO;
import com.github.christofluyten.routingtable.RoutingTable;

/**
 * Created by christof on 14.04.17.
 */
public class RoutingTableSupplier {
    private static RoutingTable routingTable = null;

    public static RoutingTable getRoutingTable(String path) {
        synchronized (RoutingTableSupplier.class) {
            if (routingTable == null) {
                try {
                    routingTable = (RoutingTable) IO.readFile(path);
                } catch (Exception e) {
                	e.printStackTrace();
                    System.out.println("failed to load the routingtable " + path);
                    routingTable = new RoutingTable();
                }
            }
        }
        return routingTable;
    }
}
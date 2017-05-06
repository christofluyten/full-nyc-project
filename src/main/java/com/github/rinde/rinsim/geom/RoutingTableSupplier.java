package com.github.rinde.rinsim.geom;

import com.github.christofluyten.IO.IO;
import com.github.christofluyten.routingtable.RoutingTable;

import java.util.function.Supplier;

/**
 * Created by christof on 14.04.17.
 */
public class RoutingTableSupplier implements Supplier<RoutingTable>{
    private static RoutingTable routingTable = null;

    private String path;

    public void setPath(String path) {
        this.path = path;
    }

    public RoutingTable get(){
        return RoutingTableSupplier.get(path);
    }

    public static RoutingTable get(String path) {
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
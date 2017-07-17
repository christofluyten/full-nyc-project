package com.github.rinde.rinsim.geom;

import com.github.christofluyten.IO.IO;
import com.github.christofluyten.routingtable.Routingtable;

import java.util.function.Supplier;

/**
 * Created by christof on 14.04.17.
 */
public class RoutingtableSupplier implements Supplier<Routingtable>{
    private static Routingtable routingTable = null;

    private String path;

    public void setPath(String path) {
        this.path = path;
    }

    public Routingtable get(){
        return RoutingtableSupplier.get(path);
    }

    public static Routingtable get(String path) {
        synchronized (RoutingtableSupplier.class) {
            if (routingTable == null) {
                try {
                    routingTable = (Routingtable) IO.readFile(path);
                } catch (Exception e) {
                	e.printStackTrace();
                    System.out.println("failed to load the routingtable " + path);
                    routingTable = new Routingtable();
                }
            }
        }
        return routingTable;
    }
}
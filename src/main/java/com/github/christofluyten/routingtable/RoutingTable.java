package com.github.christofluyten.routingtable;

import com.github.rinde.rinsim.core.model.road.RoadPath;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Christof on 1/04/2017.
 */
public class RoutingTable implements Serializable {

    private final ImmutableTable<Point, Point, Route> immutableTable;

    public RoutingTable() {
        ImmutableTable.Builder<Point, Point, Route> builder = ImmutableTable.builder();
        immutableTable = builder.build();
    }

    public RoutingTable(Table<Point, Point, Route> table) {
        ImmutableTable.Builder<Point, Point, Route> builder = ImmutableTable.builder();
        immutableTable = builder.putAll(table).build();
    }


    public Route getRoute(Point rowPoint, Point columnPoint) {
        return immutableTable.get(rowPoint, columnPoint);
    }

    public boolean containsRoute(Point rowPoint, Point columnPoint) {
        return immutableTable.contains(rowPoint, columnPoint);
    }

    public int size() {
        return immutableTable.size();
    }

    public String toString() {
        return immutableTable.toString();
    }

    public RoadPath getRoadPathTo(Point from, Point to) {
        final List<Point> path = getPathTo(from, to);
        double travelTime = getRoute(from, to).getTravelTime();

        return RoadPath.create(path, travelTime, travelTime);
    }

    private List<Point> getPathTo(Point from, Point to) {
        List<Point> path = new ArrayList<>();
        path.add(from);
        if (from.equals(to)) {
            return path;
        }
        Point nextHop = immutableTable.get(from, to).getNextHop();
        while (!nextHop.equals(to)) {
            path.add(nextHop);
            nextHop = immutableTable.get(nextHop, to).getNextHop();
        }
        path.add(to);
        return path;
    }
}

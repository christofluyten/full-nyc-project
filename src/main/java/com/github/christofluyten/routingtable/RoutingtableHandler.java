package com.github.christofluyten.routingtable;


import com.github.christofluyten.IO.IO;
import com.github.rinde.rinsim.geom.*;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import javax.measure.Measure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Christof on 1/04/2017.
 */
public class RoutingtableHandler {

    private Table<Point, Point, Route> table;



    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String mapPath = "src/main/resources/maps/map500.dot";
        String tablePath = "src/main/resources/maps/Routingtable";
            RoutingtableHandler routingTableHandler = new RoutingtableHandler();
            routingTableHandler.createTable(mapPath,tablePath);

    }

    public void createTable(String pathToMap, String pathToRoutingtable) throws IOException, ClassNotFoundException {
        table = HashBasedTable.create();

        Graph<MultiAttributeData> graph = DotGraphIO.getMultiAttributeGraphIO().read(pathToMap);

        GeomHeuristic heuristic = GeomHeuristics.time(70d);

        Set<Point> points = graph.getNodes();


        Set<Connection<MultiAttributeData>> edges = graph.getConnections();

        int nfOfNodes = points.size();
        System.out.println("# nodes " + points.size());
        System.out.println("# edges " + edges.size());


        int edgeCount = 0;

        for (Connection<MultiAttributeData> edge : edges) {
            List<Point> route = new ArrayList<>();
            route.add(edge.from());
            route.add(edge.to());
            if (Graphs.shortestPath(graph, edge.from(), edge.to(), heuristic).size() == 2) {
                table.put(edge.from(), edge.to(), new Route(edge.to(), getTravelTime(graph, heuristic, route)));
                edgeCount++;
            }
        }

        System.out.println("edgeCount " + edgeCount);
        System.out.println("table size " + table.size());


        int count = 0;
        for (Point fromPoint : points) {
            count++;
            if(count % 100 == 0){
                System.out.println("outerloop " + count);
            }

            table.put(fromPoint, fromPoint, new Route(fromPoint, 0d));
            for (Point toPoint : points) {
                if (!table.contains(fromPoint, toPoint)) {
                    List<Point> route = Graphs.shortestPath(graph, fromPoint, toPoint, heuristic);
                    addAllSubpathsToTable(graph, heuristic, route);
                }
            }

        }
        IO.writeFile(new Routingtable(table), pathToRoutingtable);
    }

    private void addAllSubpathsToTable(Graph<MultiAttributeData> graph, GeomHeuristic heuristic, List<Point> route) {
        if (!table.contains(route.get(0), route.get(route.size() - 1))) {
            double travelTime1 = getTravelTimeRecusive(new ArrayList<>(route.subList(0, route.size() - 1)), heuristic, graph);
            travelTime1 += getTravelTimeRecusive(new ArrayList<>(route.subList(route.size() - 2, route.size())), heuristic, graph);

            double travelTime2 = getTravelTimeRecusive(new ArrayList<>(route.subList(1, route.size())), heuristic, graph);
            travelTime2 += getTravelTimeRecusive(new ArrayList<>(route.subList(0, 2)), heuristic, graph);

            if (Math.abs(travelTime1 - travelTime2) < 1) {
                table.put(route.get(0), route.get(route.size() - 1), new Route(route.get(1), travelTime1));
            } else {
                System.out.println("Error " + route.toString() + " " + travelTime1 + " " + travelTime2);
            }
        }
    }

    private double getTravelTimeRecusive(List<Point> route, GeomHeuristic heuristic, Graph graph) {
        if (!table.contains(route.get(0), route.get(route.size() - 1))) {
            addAllSubpathsToTable(graph, heuristic, route);
        }
        return table.get(route.get(0), route.get(route.size() - 1)).getTravelTime();
    }


    private double getTravelTime(Graph<MultiAttributeData> graph, GeomHeuristic heuristic, List<Point> route) {
        double travelTime = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            travelTime += heuristic.calculateTravelTime(graph, route.get(i), route.get(i + 1), SI.KILOMETER,
                    Measure.valueOf(120d, NonSI.KILOMETERS_PER_HOUR), SI.MILLI(SI.SECOND));
        }
        return Math.round(travelTime * 1000000000d) / 1000000000d;
    }




}

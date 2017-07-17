package map;

import com.github.rinde.rinsim.geom.Point;
import data.area.Area;
import data.graph.Link;
import data.area.ManhattanArea;
import fileMaker.IOHandler;
import fileMaker.TravelTimesHandler;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by christof on 19.11.16.
 */
public class LinkMapHandler {
    private static int id = 0;

//    public static void main(String[] args) throws IOException, ClassNotFoundException {
//        makeLinkMap("src/main/resources/links/links.csv");
//    }


    public static void makeLinkMap(String linkFile, IOHandler ioHandler) throws IOException, ClassNotFoundException {
        Scanner linkScanner = new Scanner(new File(linkFile));
        linkScanner.nextLine();
        Map<String, Link> linkMap = new HashMap<String, Link>();
        Area manhattan = new ManhattanArea();

        while (linkScanner.hasNextLine()) {
            String line = linkScanner.nextLine();
            String[] splitLine = line.split(",");
            Link link = new Link(splitLine[0], Double.valueOf(splitLine[5]),
                    Double.valueOf(splitLine[9]), Double.valueOf(splitLine[10])*-1, Double.valueOf(splitLine[11]), Double.valueOf(splitLine[12])*-1);
            if(manhattan.contains(new Point(link.getStartX(),link.getStartY()))
                    && manhattan.contains(new Point(link.getEndX(),link.getEndY()))){
                linkMap.put(link.getId(), link);
            }
        }

        Map<String, Link> newLinkMap = GraphPruner.deleteUnvisitedLinks(linkMap);
        System.out.println("makeLinkMap4");

        double totalLength= 0;
        double maxLength = 0;
        for(String id:newLinkMap.keySet()){
            Link link = newLinkMap.get(id);
            totalLength += link.getLengthInM();
            if (link.getLengthInM()>maxLength){
                maxLength = link.getLengthInM();
            }
        }
        System.out.println();
        System.out.println("before cut");
        System.out.println("maxLength = " +maxLength);
        System.out.println("avg length = " + (totalLength/newLinkMap.size()));
        System.out.println();

        if(ioHandler.getWithTraffic()){
            TravelTimesHandler.setTraffic(newLinkMap,ioHandler);
        }
        cut(newLinkMap, 500, ioHandler);
    }


    public static void cut(Map<String, Link> linkMap, double maximumStreetLength, IOHandler ioHandler) throws IOException, ClassNotFoundException {
        Map<String,Link> newLinkMap = new HashMap<>();
        int extaLinks = 0;        for(String id : linkMap.keySet()){
            Link link = linkMap.get(id);
            double length = link.getLengthInM();
            int amountOfParts = (int) Math.ceil(length / maximumStreetLength);
            extaLinks += amountOfParts-1;
            if(amountOfParts > 1) {
                int cutsLeft = amountOfParts - 1;
                List<Double> coordinates = new ArrayList<>();
                coordinates.add(link.getStartX());
                coordinates.add(link.getStartY());
                while (cutsLeft > 0) {
                    coordinates.add(((link.getEndX() - link.getStartX()) * ((double) (amountOfParts - cutsLeft) / amountOfParts)) + link.getStartX());
                    coordinates.add(((link.getEndY() - link.getStartY()) * ((double) (amountOfParts - cutsLeft) / amountOfParts)) + link.getStartY());
                    cutsLeft--;
                }
                coordinates.add(link.getEndX());
                coordinates.add(link.getEndY());

                int i = 0;
                while (i + 3 < coordinates.size()) {
                    Link newLink = new Link(String.valueOf(getNextId()), length / (amountOfParts), coordinates.get(i), coordinates.get(i + 1), coordinates.get(i + 2), coordinates.get(i + 3));
//                    newLink.setTravelTimesMap(link.getTravelTimesMap());
                    newLink.setSpeed(link.getSpeed());
//                    newLink.setAmountOfCuts(amountOfParts - 1);
                    newLinkMap.put(newLink.getId(),newLink);
                    i += 2;
                }
            } else {
                //TODO test this
                Link newLink = new Link(String.valueOf(getNextId()), link.getLengthInM(), link.getStartX(), link.getStartY(), link.getEndX(), link.getEndY());
//                newLink.setTravelTimesMap(link.getTravelTimesMap());
//                newLink.setAmountOfCuts(amountOfParts - 1);
                newLink.setSpeed(link.getSpeed());

                newLinkMap.put(newLink.getId(),newLink);
            }
        }

        double totalLength= 0;
        double maxLength = 0;
        for(String id:newLinkMap.keySet()){
            Link link = newLinkMap.get(id);
            totalLength += link.getLengthInM();
            if (link.getLengthInM()>maxLength){
                maxLength = link.getLengthInM();
            }
        }
        System.out.println();
        System.out.println("after cut");
        System.out.println("maxLength = " +maxLength);
        System.out.println("avg length = " + (totalLength/newLinkMap.size()));
        System.out.println();
        IOHandler.writeFile(newLinkMap, ioHandler.getLinkMapPath());
        System.out.println("There are "+extaLinks+" links added." );
    }



    private static int getNextId(){
        id++;
        return id;
    }
}

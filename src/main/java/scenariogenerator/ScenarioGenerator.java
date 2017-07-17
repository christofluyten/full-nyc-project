package scenariogenerator;

import com.github.christofluyten.routingtable.Routingtable;
import com.github.christofluyten.routingtable.RoutingtableHandler;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.CachedNycGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.RoutingtableSupplier;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.pdptw.common.*;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.util.TimeWindow;
import data.area.Area;
import data.area.JfkArea;
import data.area.ManhattanArea;
import data.area.NycArea;
import data.object.Passenger;
import data.object.SimulationObject;
import data.object.Taxi;
import data.time.Date;
import fileMaker.IOHandler;
import fileMaker.SimulationObjectHandler;
import map.CsvConverter;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Created by christof on 23.11.16.
 */
public class ScenarioGenerator {



    final Builder builder;


    private IOHandler ioHandler;

    public ScenarioGenerator(Builder builder) {
        this.builder = builder;
        IOHandler ioHandler = new IOHandler();
        ioHandler.setTaxiDataDirectory(builder.taxiDataDirectory);
        ioHandler.setScenarioStartTime(builder.taxiDataStartTime);
        ioHandler.setScenarioEndTime(builder.taxiDataEndTime);
        ioHandler.setAttribute(builder.scenarioName);
        ioHandler.setCutLength(builder.cutLength);
        if (builder.traffic) {
            ioHandler.setTravelTimesDirectory(builder.travelTimesDirectory);
            ioHandler.setWithTraffic();
        }
        this.ioHandler = ioHandler;
        setScenarioFileFullName();
        makeMap(ioHandler);
    }

    public static void main(String[] args) throws Exception {

    }
    

    private void setScenarioFileFullName() {
        ioHandler.setScenarioFileFullName(ioHandler.getScenarioFileName() + ioHandler.getAttribute() + "_" + ioHandler.getScenarioStartTime().getShortStringDateForPath() + "_"
                + ioHandler.getScenarioEndTime().getShortStringDateForPath());
    }

    private void makeMap(IOHandler ioHandler) {
        try {
                if(!ioHandler.fileExists(ioHandler.getMapFilePath())) CsvConverter.convertLinkMap(ioHandler);
                if(!ioHandler.fileExists(ioHandler.getRoutingTablePath())) {
                    RoutingtableHandler rth = new RoutingtableHandler();
                    rth.createTable(ioHandler.getMapFilePath(), ioHandler.getRoutingTablePath());
                }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Scenario generateTaxiScenario(int offset) throws Exception {
        Scenario.Builder builder = Scenario.builder();
        addGeneralProperties(builder);
        addTaxis(builder);
            builder.addModel(
                    PDPGraphRoadModel.builderForGraphRm(
                            CachedNycGraphRoadModelImpl.builder(
                                    ListenableGraph.supplier(DotGraphIO.getMultiAttributeDataGraphSupplier(Paths.get(ioHandler.getMapFilePath()))),ioHandler.getRoutingTablePath())
                                    .withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
                                    .withDistanceUnit(SI.KILOMETER)
                    )
                            .withAllowVehicleDiversion(true))
                    .scenarioLength(this.builder.scenarioDuration);
//        addPassengersAtInterval(builder);
        addPassengers(builder);
//        addPassengerstest(builder);

//            addJFK(builder);
//            addManhattan(builder);
//            addNYC(builder);
        Scenario scenario = builder.build();
        ioHandler.writeScenario(scenario);
        return scenario;
//        }
    }


    private void addGeneralProperties(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        builder
                .addModel(TimeModel.builder()
                        .withRealTime()
                        .withStartInClockMode(RealtimeClockController.ClockMode.REAL_TIME)
                        .withTickLength(this.builder.tickSize)
                        .withTimeUnit(SI.MILLI(SI.SECOND)))
                .addModel(
                        DefaultPDPModel.builder()
                                .withTimeWindowPolicy(TimeWindowPolicy.TimeWindowPolicies.TARDY_ALLOWED))
                .setStopCondition(StopConditions.and(
                        StatsStopConditions.vehiclesDone(),
                        StatsStopConditions.timeOutEvent()))
                .addEvent(TimeOutEvent.create(this.builder.scenarioDuration))
                .addEvent(AddDepotEvent.create(-1, new Point(-73.9778627, -40.7888872)))
        ;
    }

    private void addTaxis(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        if (!(ioHandler.fileExists(ioHandler.getPositionedTaxisPath()))) {
            SimulationObjectHandler tfm = new SimulationObjectHandler(ioHandler);
            tfm.extractAndPositionTaxis();
        }
        List<SimulationObject> taxis = ioHandler.readPositionedObjects(ioHandler.getPositionedTaxisPath());
        int totalCount = 0;
        int addedCount = 0;
        Collections.shuffle(taxis);
        for (SimulationObject object : taxis) {
//            if ((totalCount+offset) % this.builder.nbOfPassengers == 0) {
                addedCount++;
                Taxi taxi = (Taxi) object;
                builder.addEvent(AddVehicleEvent.create(-1, VehicleDTO.builder()
                        .speed(this.builder.maxVehicleSpeedKmh)
                        .startPosition(taxi.getStartPoint())
                        .capacity(4)
                        .build()));
//            }


            totalCount++;
            if (addedCount >= this.builder.nbOfTaxis) {
                break;
            }
        }
        System.out.println(addedCount + " taxi's added of the " + totalCount);
    }


    private void addPassengers(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        if (!(ioHandler.fileExists(ioHandler.getPositionedPassengersPath()))) {
            SimulationObjectHandler pfm = new SimulationObjectHandler(ioHandler);
            pfm.extractAndPositionPassengers();
        }
        List<SimulationObject> passengers = ioHandler.readPositionedObjects(ioHandler.getPositionedPassengersPath());
        int totalCount = 0;
        int addedCount = 0;
        Routingtable routingTable = RoutingtableSupplier.get(ioHandler.getRoutingTablePath());
        Collections.shuffle(passengers);
        for (SimulationObject object : passengers) {
//            if ((totalCount+offset) % this.builder.nbOfPassengers == 0) {
                addedCount++;
                Passenger passenger = (Passenger) object;
                long pickupStartTime = passenger.getStartTime(this.builder.taxiDataStartTime);
                long pickupTimeWindow = this.builder.timewindow;
                long deliveryStartTime = getDeliveryStartTime(passenger, routingTable);
                Parcel.Builder parcelBuilder = Parcel.builder(passenger.getStartPoint(), passenger.getEndPoint())
                        .orderAnnounceTime(pickupStartTime)
                        .pickupTimeWindow(TimeWindow.create(pickupStartTime, pickupStartTime + pickupTimeWindow))
                        .pickupDuration(this.builder.pickupDuration)
                        .deliveryDuration(this.builder.deliveryDuration);
                if (this.builder.ridesharing) {
                    parcelBuilder = parcelBuilder
                            .deliveryTimeWindow(TimeWindow.create(pickupStartTime, deliveryStartTime + (pickupTimeWindow * 2)))
                            .neededCapacity(passenger.getAmount());
                } else {
                    parcelBuilder = parcelBuilder
                            .deliveryTimeWindow(TimeWindow.create(pickupStartTime, deliveryStartTime + (pickupTimeWindow)))
                            .neededCapacity(4);
                }
                builder.addEvent(
                        AddParcelEvent.create(parcelBuilder.buildDTO()));
//            }
            totalCount++;
            System.out.println(pickupStartTime);


            if (addedCount >= this.builder.nbOfPassengers) {
                break;
            }

        }
        System.out.println(addedCount + " passengers added of the " + totalCount);
    }

    private void addPassengersAtInterval(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        if (!(ioHandler.fileExists(ioHandler.getPositionedPassengersPath()))) {
            SimulationObjectHandler pfm = new SimulationObjectHandler(ioHandler);
            pfm.extractAndPositionPassengers();
        }
        List<SimulationObject> passengers = ioHandler.readPositionedObjects(ioHandler.getPositionedPassengersPath());
        int nbOfPassengers = passengers.size();
        long interval = this.builder.scenarioDuration/nbOfPassengers;
        int totalCount = 0;
        int addedCount = 0;
        Routingtable routingTable = RoutingtableSupplier.get(ioHandler.getRoutingTablePath());
        Collections.shuffle(passengers);
        for (SimulationObject object : passengers) {
//            if ((totalCount+offset) % this.builder.nbOfPassengers == 0){
                addedCount++;
                Passenger passenger = (Passenger) object;
                long pickupStartTime = interval*totalCount;
                long pickupTimeWindow = this.builder.timewindow;
                long deliveryStartTime = getDeliveryStartTimeAtInterval(passenger, routingTable, pickupStartTime);
                Parcel.Builder parcelBuilder = Parcel.builder(passenger.getStartPoint(), passenger.getEndPoint())
                        .orderAnnounceTime(pickupStartTime)
                        .pickupTimeWindow(TimeWindow.create(pickupStartTime, pickupStartTime + pickupTimeWindow))
                        .pickupDuration(this.builder.pickupDuration)
                        .deliveryDuration(this.builder.deliveryDuration);
                if (this.builder.ridesharing) {
                    parcelBuilder = parcelBuilder
                            .deliveryTimeWindow(TimeWindow.create(pickupStartTime, deliveryStartTime + (pickupTimeWindow * 2)))
                            .neededCapacity(passenger.getAmount());
                } else {
                    parcelBuilder = parcelBuilder
                            .deliveryTimeWindow(TimeWindow.create(pickupStartTime, deliveryStartTime + (pickupTimeWindow)))
                            .neededCapacity(4);
                }
                builder.addEvent(
                        AddParcelEvent.create(parcelBuilder.buildDTO()));
//            }
            totalCount++;
            System.out.println(pickupStartTime);
            if (addedCount >= this.builder.nbOfPassengers) {
                break;
            }

        }
        System.out.println(addedCount + " passengers added of the " + totalCount);
    }

    private void addPassengerstest(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        List<SimulationObject> passengers = ioHandler.readPositionedObjects(ioHandler.getPositionedPassengersPath());
        int nbOfPassengers = 20;
        int groupSize = 4;
        long interval = this.builder.scenarioDuration/(nbOfPassengers/groupSize);
        int totalCount = 0;
        int addedCount = 0;
        int groupcount = 0;
        Routingtable routingTable = RoutingtableSupplier.get(ioHandler.getRoutingTablePath());
        for (SimulationObject object : passengers) {
            while(groupcount < groupSize){
                addedCount++;
                Passenger passenger = (Passenger) object;
                long pickupStartTime = interval * totalCount;
                long pickupTimeWindow = this.builder.timewindow;
                long deliveryStartTime = getDeliveryStartTimeAtInterval(passenger, routingTable, pickupStartTime);
                Parcel.Builder parcelBuilder = Parcel.builder(passenger.getStartPoint(), passenger.getEndPoint())
                        .orderAnnounceTime(pickupStartTime)
                        .pickupTimeWindow(TimeWindow.create(pickupStartTime, pickupStartTime + pickupTimeWindow))
                        .pickupDuration(this.builder.pickupDuration)
                        .deliveryDuration(this.builder.deliveryDuration);
                if (this.builder.ridesharing) {
                    parcelBuilder = parcelBuilder
                            .deliveryTimeWindow(TimeWindow.create(pickupStartTime, deliveryStartTime + (pickupTimeWindow * 2)))
                            .neededCapacity(passenger.getAmount());
                } else {
                    parcelBuilder = parcelBuilder
                            .deliveryTimeWindow(TimeWindow.create(pickupStartTime, deliveryStartTime + (pickupTimeWindow)))
                            .neededCapacity(4);
                }
                builder.addEvent(
                        AddParcelEvent.create(parcelBuilder.buildDTO()));
                groupcount++;
            }
            totalCount++;
            if (totalCount >= nbOfPassengers/groupSize) {
                break;
            }
            groupcount = 0;
        }
        System.out.println(addedCount + " passengers added of the " + totalCount);
    }




    private long getDeliveryStartTime(Passenger passenger, Routingtable routingTable) {
        long startTime = passenger.getStartTime(this.builder.taxiDataStartTime);
        long travelTime = (long) routingTable.getRoute(passenger.getStartPoint(), passenger.getEndPoint()).getTravelTime();
        return startTime + travelTime + this.builder.pickupDuration;
    }

    private long getDeliveryStartTimeAtInterval(Passenger passenger, Routingtable routingTable, long startTime) {
        long travelTime = (long) routingTable.getRoute(passenger.getStartPoint(), passenger.getEndPoint()).getTravelTime();
        return startTime + travelTime + this.builder.pickupDuration;
    }

    private void addNYC(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        Area area = new NycArea();
        for (Point point : area.getPoints()) {
            builder.addEvent(AddDepotEvent.create(-1, point));
        }
    }

    private void addManhattan(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        Area area = new ManhattanArea();
        for (Point point : area.getPoints()) {
            builder.addEvent(AddDepotEvent.create(-1, point));
        }

    }

    private void addJFK(Scenario.Builder builder) throws IOException, ClassNotFoundException {
        Area area = new JfkArea();
        for (Point point : area.getPoints()) {
            builder.addEvent(AddDepotEvent.create(-1, point));
        }

    }


    public static ScenarioGenerator.Builder builder() {
        return new ScenarioGenerator.Builder();
    }

    public static class Builder {
        private String taxiDataDirectory;
        private String travelTimesDirectory;
        private Date taxiDataStartTime;
        private Date taxiDataEndTime;
        private double maxVehicleSpeedKmh;
        private long pickupDuration;
        private long deliveryDuration;
        private String scenarioName;
        private int cutLength;
        private long scenarioDuration;
        private boolean traffic;
        private long tickSize;
        private boolean ridesharing;
        private int nbOfPassengers;
        private int nbOfTaxis;
        private long timewindow;

        Builder() {
            taxiDataDirectory = "/media/christof/Elements/Taxi_data/";
            travelTimesDirectory = "/media/christof/Elements/Traffic_estimates/";
            taxiDataStartTime = new Date("2013-11-18 16:00:00");
            taxiDataEndTime = new Date("2013-11-18 17:00:00");
            maxVehicleSpeedKmh = 120d;
            pickupDuration = 30 * 1000L;
            deliveryDuration = 30 * 1000L;
            scenarioName = "TimeWindow";
            cutLength = 500;
            scenarioDuration = (1 * 60 * 60 * 1000L) + 1L;
            traffic = true;
            tickSize = 250L;
            ridesharing = false;
            nbOfPassengers = 10;
            nbOfTaxis = 5;
            timewindow = 5*60*1000L;

        }

        public Builder setTaxiDataDirectory(String taxiDataDirectory) {
            this.taxiDataDirectory = taxiDataDirectory;
            return this;
        }

        public Builder setTravelTimesDirectory(String travelTimesDirectory) {
            this.travelTimesDirectory = travelTimesDirectory;
            return this;
        }

        public Builder setTaxiDataStartTime(Date taxiDataStartTime) {
            this.taxiDataStartTime = taxiDataStartTime;
            return this;
        }

        public Builder setTaxiDataEndTime(Date taxiDataEndTime) {
            this.taxiDataEndTime = taxiDataEndTime;
            return this;
        }

        public Builder setMaxVehicleSpeedKmh(double maxVehicleSpeedKmh) {
            this.maxVehicleSpeedKmh = maxVehicleSpeedKmh;
            return this;
        }

        public Builder setPickupDuration(long pickupDuration) {
            this.pickupDuration = pickupDuration;
            return this;
        }

        public Builder setDeliveryDuration(long deliveryDuration) {
            this.deliveryDuration = deliveryDuration;
            return this;
        }

        public Builder setScenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }

        public Builder setCutLength(int cutLength) {
            this.cutLength = cutLength;
            return this;
        }

        public Builder setScenarioDuration(long scenarioDuration) {
            this.scenarioDuration = scenarioDuration;
            return this;
        }

        public Builder setTraffic(boolean traffic) {
            this.traffic = traffic;
            return this;
        }

        public Builder setTickSize(long tickSize) {
            this.tickSize = tickSize;
            return this;
        }

        public Builder setRidesharing(boolean ridesharing) {
            this.ridesharing = ridesharing;
            return this;
        }

        

        public Builder setNbOfPassengers(int nbOfPassengers) {
            this.nbOfPassengers = nbOfPassengers;
            return this;
        }

        public Builder setNbOfTaxis(int nbOfTaxis) {
            this.nbOfTaxis = nbOfTaxis;
            return this;
        }

        public Builder setTimewindow(long timewindow) {
            this.timewindow = timewindow;
            return this;
        }

        public ScenarioGenerator build() {
            return new ScenarioGenerator(this);
        }
    }







    }

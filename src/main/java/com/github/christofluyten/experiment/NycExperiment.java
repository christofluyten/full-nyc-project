package com.github.christofluyten.experiment;

import com.github.rinde.logistics.pdptw.mas.Truck;
import com.github.rinde.logistics.pdptw.mas.TruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.*;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunction;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunctions;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlannerStatsLogger;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.logistics.pdptw.solver.optaplanner.OptaplannerSolvers;
import com.github.rinde.rinsim.central.rt.RtCentral;
import com.github.rinde.rinsim.central.rt.RtSolverModel;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger.LogEntry;
import com.github.rinde.rinsim.core.model.time.RealtimeTickInfo;
import com.github.rinde.rinsim.experiment.CommandLineProgress;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.GeomHeuristics;
import com.github.rinde.rinsim.pdptw.common.*;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.measure.Metrics;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import data.time.Date;
import scenariogenerator.ScenarioGenerator;
import org.eclipse.swt.graphics.RGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NycExperiment {
	private final static long rpMs = 100L; //100
	private final static long bMs = 20L; //20
	private final static long maxAuctionDurationSoft = 10 * 1000L;  //10000L;
	private final static long maxAuctionDurationHard = 30 * 60 * 1000L;
	private final static long reactCooldownPeriodMs = 60*1000L;
	private final static BidFunction bf = BidFunctions.BALANCED_HIGH;
	private final static String masSolverName =
			"Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";
	private final static ObjectiveFunction objFunc = Gendreau06ObjectiveFunction.instance(70);
	private final static boolean enableReauctions = true;
	private final static boolean computationsLogging = false;
	private final static boolean ridesharing = false;
	private static String attribute = "noRidesharing";
	private static boolean debug = true;
	private static boolean gui = true;
	private final static int amountFilter = 5;
	private static final int minNbOfBidders = 5;
	private static int repetitions = 3;




	private static final String taxiDataDirectory = "/media/christof/Elements/Taxi_data/";
	private static final String travelTimesDirectory = "/media/christof/Elements/Traffic_estimates/"; //path to director with the travel_times
	private static final Date taxiDataStartTime = new Date("2013-11-18 16:00:00");                   //format: "yyyy-mm-dd HH:mm:ss"
	private static final Date taxiDataEndTime = new Date("2013-11-18 17:00:00");

	private static final double maxVehicleSpeedKmh = 120d;


	private static final long pickupDuration = 30 * 1000L;
	private static final long deliveryDuration = 30 * 1000L;


	private static final int cutLength = 500;                                                  //maximum length in meters of a edge in the graph (or "link" in the "map")

	private static final long scenarioDuration = (1 * 60 * 60 * 1000L) + 1L;

	private static final long scenarioDurationDebug = (1000 * 1000L) + 1L;

	private static final boolean traffic = true;

	private static final GeomHeuristic heuristic = GeomHeuristics.time(70d);


	private static final long tickSize = 250L;

	private static final String routingTablePath= "src/main/resources/maps/RoutingTable";


	/**
	 * Usage: args = [ generate/experiment datasetID #buckets bucketID]
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws Exception {
		if (args.length > 0 && args[0].equals("false")) {
			debug = false;
		}
		if (debug) {
			System.out.println("++++++++++ DEBUGGING ++++++++++");
		}
        if (ridesharing) {
            attribute = "Ridesharing";
        }
        performExperiment();
        System.out.println("THE END");
    }

	public static void performExperiment() throws Exception {
		System.out.println(System.getProperty("java.vm.name") + ", "
			      + System.getProperty("java.vm.vendor") + ", "
			      + System.getProperty("java.vm.version") + " (runtime version: "
			      + System.getProperty("java.runtime.version") + ")");
		System.out.println(System.getProperty("os.name") + " "
			      + System.getProperty("os.version") + " "
			      + System.getProperty("os.arch"));
		System.out.println("++++++  attribute "+attribute+" ++++++");
		System.out.println("++++++  minNbOfBidders "+minNbOfBidders+" ++++++");
		System.out.println("++++++  maxAuctionDurationSoft "+maxAuctionDurationSoft+" ++++++");
		System.out.println("++++++  amountFilter "+amountFilter+" ++++++");
		System.out.println("++++++  debug "+debug+" ++++++");



		ScenarioGenerator sg;
			sg = ScenarioGenerator.builder()
							.setCutLength(cutLength)
							.setDeliveryDuration(deliveryDuration)
							.setPickupDuration(pickupDuration)
							.setMaxVehicleSpeedKmh(maxVehicleSpeedKmh)
							.setScenarioDuration(scenarioDuration)
							.setScenarioName("")
							.setTaxiDataDirectory(taxiDataDirectory)
							.setTravelTimesDirectory(travelTimesDirectory)
							.setTaxiDataStartTime(taxiDataStartTime)
							.setTaxiDataEndTime(taxiDataEndTime)
							.setTickSize(tickSize)
							.setTraffic(traffic)
							.setRidesharing(ridesharing)
							.setRoutingTablePath(routingTablePath)
							.setAmountFilter(amountFilter)
							.build();


		Scenario scenario = sg.generateTaxiScenario(debug,0);

		List<Scenario> scenarios = new ArrayList<>();
		scenarios.add(sg.generateTaxiScenario(debug,0));
		scenarios.add(sg.generateTaxiScenario(debug,1));
		scenarios.add(sg.generateTaxiScenario(debug,2));

		System.out.println("measureDynamism " + Metrics.measureDynamism(scenario));
		System.out.println("measureUrgency " + Metrics.measureUrgency(scenario));
		System.out.println("duration " + scenario.getTimeWindow().end());


//
	    final OptaplannerSolvers.Builder opFfdFactory =
	    	      OptaplannerSolvers.builder()
	    	      .withSolverHeuristic(heuristic)
	    	      .withSolverXmlResource(
	    	        "com/github/rinde/jaamas17/jaamas-solver.xml")
	    	      .withUnimprovedMsLimit(rpMs)
	    	      .withName(masSolverName)
	    	      .withObjectiveFunction(objFunc)
				;


		Experiment.Builder experimentBuilder = Experiment.builder();

		if (!debug) {
			experimentBuilder = experimentBuilder
//					.computeDistributed()
				 .computeLocal()
				.withRandomSeed(123)
//				.withThreads((int) Math
//						.floor((Runtime.getRuntime().availableProcessors() - 1) / 2d))
					.withThreads(1)
					.repeat(repetitions)
					.withWarmup(30000);
		} else {
					if(gui){
						experimentBuilder = experimentBuilder
								.withThreads(1)
								.showGui(View.builder()
										.with(RoadUserRenderer.builder()
//								.withToStringLabel()
												.withColorAssociation(Truck.class, new RGB(204, 0, 0))
												.withColorAssociation(Vehicle.class,new RGB(100,0,0))
												.withColorAssociation(Depot.class, new RGB(0, 0, 255)))
										.with(RouteRenderer.builder())
										.with(PDPModelRenderer.builder())
										.with(GraphRoadModelRenderer.builder()
//								.withDirectionArrows()
//								.withStaticRelativeSpeedVisualization()
//								.withDynamicRelativeSpeedVisualization()
										)
//										.with(AuctionPanel.builder())
//										.with(RoutePanel.builder())
//										.with(TimeLinePanel.builder())
//							.with(RtSolverPanel.builder())
										.withResolution(12800, 10240)
										.withAutoPlay()
										.withAutoClose());
					} else {
						experimentBuilder = experimentBuilder
								.withThreads(1);
					}


		}


		experimentBuilder = experimentBuilder.addResultListener(new CommandLineProgress(System.out))
				.addResultListener(new LuytenResultWriter(
						new File("files/results/LUYTEN17"),
						(Gendreau06ObjectiveFunction) objFunc))
				.usePostProcessor(new LogProcessor(objFunc))
				.addConfigurations(mainConfigs(opFfdFactory, objFunc))
				.addScenarios(scenarios);
        experimentBuilder.perform();

	}

	static List<MASConfiguration> mainConfigs(
			OptaplannerSolvers.Builder opFfdFactory, ObjectiveFunction objFunc) {

		final List<MASConfiguration> configs = new ArrayList<>();
		if (debug){
			if(debug) {
				configs.add(createMAS(opFfdFactory, objFunc, rpMs, bMs,
						maxAuctionDurationSoft, enableReauctions, reactCooldownPeriodMs, computationsLogging));
				System.out.println("MAS");
			}else {
				final String solverKey =
						"Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";
				final long centralUnimprovedMs = 10000L;
				configs.add(createCentral(
						opFfdFactory.withSolverXmlResource(
								"com/github/rinde/jaamas17/jaamas-solver.xml")
								.withName("Central_" + attribute)
								.withSolverHeuristic(heuristic)
								.withUnimprovedMsLimit(centralUnimprovedMs),
						"Central_" + attribute+"_aFilter="+amountFilter+"_bFilter="+minNbOfBidders));
				System.out.println("Central");

			}
		} else {
				configs.add(createMAS(opFfdFactory, objFunc, rpMs, bMs,
						maxAuctionDurationSoft, enableReauctions, reactCooldownPeriodMs, computationsLogging));
				final String solverKey =
						"Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";
				final long centralUnimprovedMs = 10000L;
				configs.add(createCentral(
						opFfdFactory.withSolverXmlResource(
								"com/github/rinde/jaamas17/jaamas-solver.xml")
								.withName("Central_" + attribute)
								.withSolverHeuristic(heuristic)
								.withUnimprovedMsLimit(centralUnimprovedMs),
						"Central_" + attribute+"_aFilter="+amountFilter+"_bFilter="+minNbOfBidders));
		}
		System.out.println("created the mainConfigs");
		return configs;
	}

	static MASConfiguration createMAS(OptaplannerSolvers.Builder opFfdFactory,
			ObjectiveFunction objFunc, long rpMs, long bMs,
			long maxAuctionDurationSoft, boolean enableReauctions,
			long reauctCooldownPeriodMs, boolean computationsLogging) {

		MASConfiguration.Builder b = MASConfiguration.pdptwBuilder()
				.setName("MAS_" + attribute+"_aFilter="+amountFilter+"_bFilter="+minNbOfBidders)
				.addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
				.addEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
				.addEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
				.addEventHandler(AddVehicleEvent.class,
						TruckFactory.DefaultTruckFactory.builder()
							.setRouteHeuristic(heuristic)
							.setRoutePlanner(RtSolverRoutePlanner.supplier(
								opFfdFactory.withSolverXmlResource(
										"com/github/rinde/jaamas17/jaamas-solver.xml")
								.withName(masSolverName)
								.withUnimprovedMsLimit(rpMs)
								.withTimeMeasurementsEnabled(computationsLogging)
                                .withSolverHeuristic(heuristic)
                                .buildRealtimeSolverSupplier()))
							.setCommunicator(
								RtSolverBidder.realtimeBuilder(objFunc,
										opFfdFactory.withSolverXmlResource(
												"com/github/rinde/jaamas17/jaamas-solver.xml")
										.withName(masSolverName)
										.withUnimprovedMsLimit(bMs)
                                        .withSolverHeuristic(heuristic)
                                        .withTimeMeasurementsEnabled(computationsLogging)
										.buildRealtimeSolverSupplier())

								.withBidFunction(bf)
								.withGeomHeuristic(heuristic)
								.withReauctionsEnabled(enableReauctions)
								.withReauctionCooldownPeriod(reauctCooldownPeriodMs))
							.setLazyComputation(false)
							.setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
							.build())
				.addModel(AuctionCommModel.builder(DoubleBid.class)
						.withStopCondition(
								AuctionStopConditions.and(
										AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
										AuctionStopConditions.<DoubleBid>or(
												AuctionStopConditions.<DoubleBid>allBidders(),
												AuctionStopConditions
												.<DoubleBid>maxAuctionDuration(maxAuctionDurationSoft))))
						.withMaxAuctionDuration(maxAuctionDurationHard)
						.withBidderFilter(new AuctionCommModel.BidderFilter(1d,1d,minNbOfBidders)))
				.addModel(RealtimeClockLogger.builder())
				 ;
		if (debug) {
			b = b.addModel(RtSolverModel.builder());
		} else {
			b = b.addModel(RtSolverModel.builder()
					.withThreadPoolSize(1)
					.withThreadGrouping(true)
			);
		}

		if (computationsLogging) {
			b = b.addModel(AuctionTimeStatsLogger.builder())
					.addModel(RoutePlannerStatsLogger.builder());
		}

		return b.build();
	}
	
	static void addCentral(Experiment.Builder experimentBuilder,
			OptaplannerSolvers.Builder opBuilder, String name) {
		experimentBuilder.addConfiguration(createCentral(opBuilder, name));
	}

	static MASConfiguration createCentral(OptaplannerSolvers.Builder opBuilder,
			String name) {
		MASConfiguration.Builder builder = MASConfiguration.pdptwBuilder()
				.addModel(RealtimeClockLogger.builder())
				.addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
				.addEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
				.addEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
				.addEventHandler(AddVehicleEvent.class, RtCentral.vehicleHandler(heuristic))
				.setName(name);

		if (debug) {
			builder = builder.addModel(RtCentral.builder(opBuilder.buildRealtimeSolverSupplier())
			);
		} else {
			builder = builder.addModel(RtCentral.builder(opBuilder.buildRealtimeSolverSupplier())
					.withContinuousUpdates(true)
					.withThreadGrouping(true)
			);
		}

		return builder.build();
	}


	  @AutoValue
	  abstract static class AuctionStats implements Serializable{
          static AuctionStats create(int numP, int numR, int numUn, int numF) {
              return new AutoValue_NycExperiment_AuctionStats(numP, numR, numUn,
                      numF);
          }

	    abstract int getNumParcels();

	    abstract int getNumReauctions();

	    abstract int getNumUnsuccesfulReauctions();

	    abstract int getNumFailedReauctions();
	  }

	  @AutoValue
	  abstract static class ExperimentInfo implements Serializable {
	    private static final long serialVersionUID = 6324066851233398736L;

          static ExperimentInfo create(List<LogEntry> log, long rt, long st,
                                       StatisticsDTO stats, ImmutableList<RealtimeTickInfo> dev,
                                       Optional<AuctionStats> aStats) {
              return new AutoValue_NycExperiment_ExperimentInfo(log, rt, st, stats,
                      dev, aStats);
          }

	    abstract List<LogEntry> getLog();

	    abstract long getRtCount();

	    abstract long getStCount();

	    abstract StatisticsDTO getStats();

	    abstract ImmutableList<RealtimeTickInfo> getTickInfoList();

	    abstract Optional<AuctionStats> getAuctionStats();
	  }

	
	 static class LogProcessor
      implements PostProcessor<ExperimentInfo>, Serializable {
    private static final long serialVersionUID = 5997690791395717045L;
    ObjectiveFunction objectiveFunction;
    
	Logger LOGGER = LoggerFactory.getLogger("LogProcessor");

    LogProcessor(ObjectiveFunction objFunc) {
      objectiveFunction = objFunc;
    }

    @Override
    public ExperimentInfo collectResults(Simulator sim, SimArgs args) {

      @Nullable
      final RealtimeClockLogger logger =
        sim.getModelProvider().tryGetModel(RealtimeClockLogger.class);

      @Nullable
      final AuctionCommModel<?> auctionModel =
        sim.getModelProvider().tryGetModel(AuctionCommModel.class);

      final Optional<AuctionStats> aStats;
      if (auctionModel == null) {
        aStats = Optional.absent();
      } else {
        final int parcels = auctionModel.getNumParcels();
        final int reauctions = auctionModel.getNumAuctions() - parcels;
        final int unsuccessful = auctionModel.getNumUnsuccesfulAuctions();
        final int failed = auctionModel.getNumFailedAuctions();
        aStats = Optional
          .of(AuctionStats.create(parcels, reauctions, unsuccessful, failed));
      }

      final StatisticsDTO stats =
    	        sim.getModelProvider().getModel(StatsTracker.class).getStatistics();
//        PostProcessors.statisticsPostProcessor(objectiveFunction)
//          .collectResults(sim, args);

      LOGGER.info("success: {}", args);
      
      if(aStats.isPresent()) {
    	  System.out.println("Num Parcels: " + aStats.get().getNumParcels());
      	  System.out.println("Num Reauctions: " + aStats.get().getNumReauctions());
      	  System.out.println("Num Unsuccessful Reauctions: " + aStats.get().getNumUnsuccesfulReauctions());
      	  System.out.println("Num Failed Reauctions: " + aStats.get().getNumFailedReauctions());
      }
      
      System.out.println(stats.toString());
      
      if (logger == null) {
        return ExperimentInfo.create(new ArrayList<LogEntry>(), 0,
          sim.getCurrentTime() / sim.getTimeStep(), stats,
          ImmutableList.<RealtimeTickInfo>of(), aStats);
      }
      return ExperimentInfo.create(logger.getLog(), logger.getRtCount(),
        logger.getStCount(), stats, logger.getTickInfoList(), aStats);
    }

    @Override
    public FailureStrategy handleFailure(Exception e, Simulator sim,
        SimArgs args) {

      System.out.println("Fail: " + args);
      e.printStackTrace();
      // System.out.println(AffinityLock.dumpLocks());

      return FailureStrategy.RETRY;
      // return FailureStrategy.ABORT_EXPERIMENT_RUN;

    }
  }


}

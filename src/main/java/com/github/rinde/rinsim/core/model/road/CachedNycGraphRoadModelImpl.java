package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.christofluyten.routingtable.RoutingTable;
import com.github.rinde.rinsim.geom.*;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;

/**
 * Special {@link GraphRoadModelImpl} that caches all
 * {@link #getShortestPathTo(Point, Point)} invocations. Further, it keeps track
 * of all {@link RoadUser}s and their types, such that
 * {@link #getObjectsOfType(Class)} is now O(1).
 *
 * @author Rinde van Lon
 * @author Vincent Van Gestel
 * @author Christof Luyten
 */
public class CachedNycGraphRoadModelImpl extends GraphRoadModelImpl {

	private final RoadModelSnapshot snapshot;

	private RoutingTable routingTable;

	CachedNycGraphRoadModelImpl(Graph<?> g, CachedNycGraphRMB b) {
		super(g, b);

		routingTable = RoutingTableSupplier.get(b.getCachePath());

		snapshot = CachedNycGraphRoadModelSnapshot.create(
				ImmutableGraph.copyOf(graph), routingTable, b.getDistanceUnit());
	}


	@Override
	public RoadPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
			Measure<Double, Velocity> speed, GeomHeuristic heuristic) {
		return routingTable.getRoadPathTo(from, to);
	}

	@Override
	public List<Point> getShortestPathTo(Point from, Point to) {
		final List<Point> path = new ArrayList<>();
		Point start = from;
		if (isOnConnection(from)) {
			start = asLoc(from).conn.get().to();
			path.add(from);
		}

		Point end = to;
		if (isOnConnection(to)) {
			end = asLoc(to).conn.get().from();
		}
		path.addAll(doGetShortestPathTo(start, end));
		if (isOnConnection(to)) {
			path.add(to);
		}
		return path;
	}

	// overrides internal func to use the routing table
	@Override
	protected List<Point> doGetShortestPathTo(Point from, Point to) {
		System.out.println("using doGetShortestPathTo in CNGRMI");
		return routingTable.getRoadPathTo(from, to).getPath();
	}

	public RoadPath getPathTo(MovingRoadUser object, Point destination,
							  Unit<Duration> timeUnit, Measure<Double, Velocity> maxSpeed,
							  GeomHeuristic heuristic) {
		final Optional<? extends Connection<?>> conn = getConnection(object);
//		System.out.println("using getPathTo MovingRoaduser in CNGRMI");
		if (conn.isPresent()) {
			final double connectionPercentage =
					Point.distance(getPosition(object), conn.get().to())
							/ Point.distance(conn.get().from(), conn.get().to());
			final double travelTime =
					routingTable.getRoadPathTo(conn.get().from(), conn.get().to()).getTravelTime()
							* connectionPercentage;
			final RoadPath path =
					getPathTo(conn.get().to(), destination, timeUnit, maxSpeed, heuristic);
			return RoadPath.create(path.getPath(), path.getValue() + travelTime,
					path.getTravelTime() + travelTime);
		}
		return getPathTo(getPosition(object), destination, timeUnit, maxSpeed,
				heuristic);
	}

	@Override
	public RoadModelSnapshot getSnapshot() {
		return snapshot;
	}


	public static CachedNycGraphRMB builder(Supplier<? extends Graph<?>> graphSupplier,
											String cachePath) {
		return CachedNycGraphRMB.create(graphSupplier,cachePath);
	}
}

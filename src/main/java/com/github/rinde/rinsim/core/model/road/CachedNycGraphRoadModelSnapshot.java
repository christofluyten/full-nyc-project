/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.christofluyten.routingtable.RoutingTable;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.ImmutableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;

/**
 * The snapshot for a {@link CachedNycGraphRoadModelImpl}.
 * It is used by a caching enabled road model to save time
 * on calculating shortest path.
 * @author Vincent Van Gestel
 * @author Christof Luyten
 */
@AutoValue
abstract class CachedNycGraphRoadModelSnapshot
    implements RoadModelSnapshot {

	CachedNycGraphRoadModelSnapshot() {}

  public abstract ImmutableGraph<? extends ConnectionData> getGraph();
  
  public abstract RoutingTable getRoutingTable();

  public abstract Unit<Length> getModelDistanceUnit();

  @Override
  public RoadPath getPathTo(Point from, Point to, Unit<Duration> timeUnit,
      Measure<Double, Velocity> speed, GeomHeuristic heuristic) {
    return getRoutingTable().getRoadPathTo(from, to);
  }

  @Override
  public Measure<Double, Length> getDistanceOfPath(Iterable<Point> path)
      throws IllegalArgumentException {
    final Iterator<Point> pathIt = path.iterator();
    checkArgument(pathIt.hasNext(), "Cannot check distance of an empty path.");
    Point prev = pathIt.next();
    Point cur = null;
    double distance = 0d;
    while (pathIt.hasNext()) {
      cur = pathIt.next();
      distance += getGraph().connectionLength(prev, cur);
      prev = cur;
    }
    return Measure.valueOf(distance, getModelDistanceUnit());
  }

  static CachedNycGraphRoadModelSnapshot create(
      ImmutableGraph<ConnectionData> graph, RoutingTable routingTable, Unit<Length> distanceUnit) {
    return new AutoValue_CachedNycGraphRoadModelSnapshot(graph, routingTable, distanceUnit);
  }

}

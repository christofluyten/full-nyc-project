package com.github.rinde.rinsim.core.model.road;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.AbstractGraphRMB;
import com.github.rinde.rinsim.geom.Graph;
import com.google.auto.value.AutoValue;
import com.google.common.base.Supplier;

/**
 * Builder for {@link CachedNycGraphRoadModelImpl} instances.
 * @author Vincent Van Gestel
 * @author Christof Luyten
 */
@AutoValue
public abstract class CachedNycGraphRMB
    extends AbstractGraphRMB<GraphRoadModel, CachedNycGraphRMB,Graph<?>> {

  private static final long serialVersionUID = -7837221650923727573L;

  CachedNycGraphRMB() {
    setProvidingTypes(RoadModel.class, GraphRoadModel.class);
  }

  @Override
  protected abstract Supplier<Graph<?>> getGraphSupplier();

  protected abstract String getCachePath();

  @Override
  public CachedNycGraphRoadModelImpl build(DependencyProvider dependencyProvider) {
    return new CachedNycGraphRoadModelImpl(getGraph(), this);
  }


  @Override
  public CachedNycGraphRMB withDistanceUnit(Unit<Length> unit) {
    return create(unit, getSpeedUnit(), getGraphSupplier(),
      getCachePath());
  }

  @Override
  public CachedNycGraphRMB withSpeedUnit(Unit<Velocity> unit) {
    return create(getDistanceUnit(), unit, getGraphSupplier(),
      getCachePath());
  }
  @Override
  public String toString() {
    return RoadModelBuilders.class.getSimpleName()
      + ".staticGraph().withCache()";
  }

  static CachedNycGraphRMB create(
		  Supplier<? extends Graph<?>> graphSupplier,
				  String cachePath) {
	  return create(DEFAULT_DISTANCE_UNIT, DEFAULT_SPEED_UNIT, graphSupplier,
			  cachePath);
  }

  @SuppressWarnings("unchecked")
  static CachedNycGraphRMB create(Unit<Length> distanceUnit,
                                  Unit<Velocity> speedUnit,
                                  Supplier<? extends Graph<?>> graphSupplier, String cachePath) {
	  return new AutoValue_CachedNycGraphRMB(distanceUnit,
			  speedUnit, (Supplier<Graph<?>>) graphSupplier, cachePath);
  }
}

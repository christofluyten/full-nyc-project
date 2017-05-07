//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.github.rinde.rinsim.pdptw.common;

import com.github.rinde.rinsim.scenario.StopCondition;
import com.github.rinde.rinsim.scenario.StopCondition.TypeProvider;
import com.google.common.collect.ImmutableSet;

public final class StatsStopConditions {
    private StatsStopConditions() {
    }

    public static StopCondition timeOutEvent() {
        return StatsStopConditions.Instances.TIME_OUT_EVENT;
    }

    public static StopCondition vehiclesDoneAndBackAtDepot() {
        return StatsStopConditions.Instances.VEHICLES_DONE_AND_BACK_AT_DEPOT;
    }

    public static StopCondition vehiclesDone() {
        return StatsStopConditions.Instances.VEHICLES_DONE;
    }

    public static StopCondition anyTardiness() {
        return StatsStopConditions.Instances.ANY_TARDINESS;
    }

    static enum Instances implements StopCondition {
        TIME_OUT_EVENT {
            public boolean evaluate(TypeProvider provider) {
                return ((StatisticsProvider)provider.get(StatisticsProvider.class)).getStatistics().simFinish;
            }
        },
        VEHICLES_DONE_AND_BACK_AT_DEPOT {
            public boolean evaluate(TypeProvider provider) {
                StatisticsDTO stats = ((StatisticsProvider)provider.get(StatisticsProvider.class)).getStatistics();
                return stats.totalVehicles == stats.vehiclesAtDepot && stats.movedVehicles > 0 && stats.totalParcels == stats.totalDeliveries;
            }
        },
        VEHICLES_DONE {
            public boolean evaluate(TypeProvider provider) {
                StatisticsDTO stats = ((StatisticsProvider)provider.get(StatisticsProvider.class)).getStatistics();
                return stats.movedVehicles > 0 && stats.totalParcels == stats.totalDeliveries;
            }
        },
        ANY_TARDINESS {
            public boolean evaluate(TypeProvider provider) {
                StatisticsDTO stats = ((StatisticsProvider)provider.get(StatisticsProvider.class)).getStatistics();
                return stats.pickupTardiness > 0L || stats.deliveryTardiness > 0L;
            }
        };

        private Instances() {
        }

        public ImmutableSet<Class<?>> getTypes() {
            return ImmutableSet.of(StatisticsProvider.class);
        }
    }
}

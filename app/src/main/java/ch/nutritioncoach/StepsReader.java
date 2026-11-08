package ch.nutritioncoach;

import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.StepsRecord;
import android.os.OutcomeReceiver;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

final class StepsReader {
    static final String PERMISSION = "android.permission.health.READ_STEPS";
    private static final String FIT = "com.google.android.apps.fitness";
    private static final String NOLATR = "com.nolatr.ring";

    interface Result {
        void accept(Long fit, Long nolatr);
    }

    static void read(HealthConnectManager manager, LocalDate day, Executor executor,
                     Result success, Consumer<String> failure) {
        ZoneId zone = ZoneId.systemDefault();
        TimeInstantRangeFilter range = new TimeInstantRangeFilter.Builder()
                .setStartTime(day.atStartOfDay(zone).toInstant())
                .setEndTime(day.plusDays(1).atStartOfDay(zone).toInstant()).build();
        aggregate(manager, range, FIT, executor,
                fit -> aggregate(manager, range, NOLATR, executor,
                        nolatr -> success.accept(fit, nolatr), failure), failure);
    }

    private static void aggregate(HealthConnectManager manager, TimeInstantRangeFilter range,
                                  String source, Executor executor, Consumer<Long> success,
                                  Consumer<String> failure) {
        try {
            AggregateRecordsRequest<Long> request = new AggregateRecordsRequest.Builder<Long>(range)
                    .addAggregationType(StepsRecord.STEPS_COUNT_TOTAL)
                    .addDataOriginsFilter(new DataOrigin.Builder().setPackageName(source).build())
                    .build();
            manager.aggregate(request, executor, new OutcomeReceiver<
                    AggregateRecordsResponse<Long>, HealthConnectException>() {
                @Override public void onResult(AggregateRecordsResponse<Long> response) {
                    // An empty result must stay distinct from a reported count of zero.
                    success.accept(response.getDataOrigins(StepsRecord.STEPS_COUNT_TOTAL).isEmpty()
                            ? null : response.get(StepsRecord.STEPS_COUNT_TOTAL));
                }

                @Override public void onError(HealthConnectException error) {
                    failure.accept(error.getErrorCode() == HealthConnectException.ERROR_SECURITY
                            ? "denied" : "error");
                }
            });
        } catch (SecurityException e) {
            failure.accept("denied");
        } catch (IllegalArgumentException | IllegalStateException e) {
            failure.accept("error");
        }
    }
}

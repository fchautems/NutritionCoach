package ch.nutritioncoach;

import android.content.Context;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.WeightRecord;
import android.os.OutcomeReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

final class WithingsReader {
    static final String WEIGHT = "android.permission.health.READ_WEIGHT";
    static final String HISTORY = "android.permission.health.READ_HEALTH_DATA_HISTORY";
    private static final String PACKAGE = "com.withings.wiscale2";

    static boolean available(Context context) {
        return context.getSystemService(HealthConnectManager.class) != null;
    }

    static void read(Context context, boolean history, Executor executor,
                     Consumer<JSONArray> success, Consumer<String> failure) {
        HealthConnectManager manager = context.getSystemService(HealthConnectManager.class);
        Instant now = Instant.now();
        Instant start = history ? Instant.EPOCH : now.minus(30, ChronoUnit.DAYS);
        TimeInstantRangeFilter range = new TimeInstantRangeFilter.Builder()
                .setStartTime(start).setEndTime(now).build();
        readPage(manager, range, null, new JSONArray(), executor, success, failure);
    }

    private static void readPage(HealthConnectManager manager, TimeInstantRangeFilter range,
                                 Long page, JSONArray result, Executor executor,
                                 Consumer<JSONArray> success, Consumer<String> failure) {
        ReadRecordsRequestUsingFilters.Builder<WeightRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                .addDataOrigins(new DataOrigin.Builder().setPackageName(PACKAGE).build())
                .setTimeRangeFilter(range)
                .setPageSize(500);
        if (page != null) request.setPageToken(page);
        try {
            manager.readRecords(request.build(), executor, new OutcomeReceiver<
                    ReadRecordsResponse<WeightRecord>, HealthConnectException>() {
                @Override public void onResult(ReadRecordsResponse<WeightRecord> response) {
                    try {
                        for (WeightRecord record : response.getRecords()) {
                            double kg = record.getWeight().getInGrams() / 1000;
                            String id = record.getMetadata().getId();
                            if (id == null || id.isEmpty() || !(kg >= 10 && kg <= 500)) continue;
                            Instant time = record.getTime();
                            ZoneOffset offset = record.getZoneOffset();
                            ZoneId zone = offset == null ? ZoneId.systemDefault() : offset;
                            result.put(new JSONObject().put("id", id).put("kg", kg)
                                    .put("time", time.toEpochMilli())
                                    .put("day", time.atZone(zone).toLocalDate().toString())
                                    .put("offset", offset == null ? JSONObject.NULL : offset.toString())
                                    .put("source", PACKAGE));
                        }
                        long next = response.getNextPageToken();
                        if (next == -1) success.accept(result);
                        else readPage(manager, range, next, result, executor, success, failure);
                    } catch (JSONException e) {
                        failure.accept("error");
                    }
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

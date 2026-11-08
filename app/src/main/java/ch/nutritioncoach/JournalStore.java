package ch.nutritioncoach;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Stores the journal outside the replaceable interface. Existing V0.2 entries keep their IDs. */
final class JournalStore {
    private static final String ENTRIES = "entries_v1";
    private static final String GOAL = "daily_goal_v1";
    private static final String PROFILE = "profile_v1";
    private static final String WITHINGS = "withings_weights_v1";
    private final SharedPreferences prefs;

    JournalStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    String read() throws JSONException {
        String storedProfile = prefs.getString(PROFILE, null);
        return new JSONObject()
                .put("profile", storedProfile == null ? JSONObject.NULL : new JSONObject(storedProfile))
                .put("goal", prefs.getInt(GOAL, 2000))
                .put("entries", entries())
                .put("withingsWeights", withingsWeights())
                .toString();
    }

    boolean saveProfile(String firstName, String sex, String heightCm, String weightKg)
            throws JSONException {
        synchronized (prefs) {
            return saveProfile(firstName, sex, heightCm, weightKg, null, LocalDate.now());
        }
    }

    boolean saveProfileDetails(String payload, LocalDate today) throws JSONException {
        JSONObject details = new JSONObject(payload);
        synchronized (prefs) {
            return saveProfile(details.getString("name"), details.getString("sex"),
                    details.getString("height"), details.getString("weight"), details, today);
        }
    }

    private boolean saveProfile(String firstName, String sex, String heightCm, String weightKg,
                                JSONObject details, LocalDate today) throws JSONException {
        String name = firstName.trim();
        int height = positiveInt(heightCm, 50, 250);
        double weight = parseWeight(weightKg);
        if (name.isEmpty() || name.length() > 60 || height == -1
                || weight == -1 || !validSex(sex)) return false;

        JSONObject profile = new JSONObject(prefs.getString(PROFILE, "{}"));
        boolean recordWeight = profile.optDouble("weight", -1) != weight;
        if (details != null) {
            String birthDate = details.getString("birthDate");
            String movement = details.getString("movement");
            String sport = details.getString("sport");
            String goalMode = details.getString("goalMode");
            if ((!birthDate.isEmpty() && (!validProfileDate(birthDate)
                    || birthDate.compareTo(today.toString()) > 0))
                    || !oneOf(movement, "", "seated", "mixed", "active")
                    || !oneOf(sport, "", "none", "short", "weekly", "regular", "frequent")
                    || !oneOf(goalMode, "", "maintain", "target")) return false;

            if ("target".equals(goalMode)) {
                double targetWeight = parseWeight(details.getString("targetWeight"));
                String deadline = details.getString("deadline");
                if (targetWeight == -1 || (!deadline.isEmpty() && !validProfileDate(deadline))) return false;
                profile.put("targetWeight", targetWeight).put("deadline", deadline);
            } else {
                profile.remove("targetWeight");
                profile.remove("deadline");
            }
            profile.put("birthDate", birthDate).put("movement", movement)
                    .put("sport", sport).put("goalMode", goalMode);
            String importId = details.optString("withingsId");
            if (!importId.isEmpty()) {
                JSONObject imported = withingsWeights().optJSONObject(importId);
                if (imported != null && Math.round(imported.getDouble("kg") * 100) / 100.0 == weight) {
                    recordWeight = false;
                }
            }
            recordWeight |= details.getBoolean("recordWeight");
        }
        if (recordWeight && (details != null || profile.has("weighIns"))) {
            JSONObject weighIns = profile.optJSONObject("weighIns");
            if (weighIns == null) weighIns = new JSONObject();
            String oldest = today.minusDays(89).toString();
            Iterator<String> days = weighIns.keys();
            while (days.hasNext()) {
                if (days.next().compareTo(oldest) < 0) days.remove();
            }
            weighIns.put(today.toString(), weight);
            profile.put("weighIns", weighIns);
        }
        profile.put("name", name).put("sex", sex).put("height", height).put("weight", weight);
        return prefs.edit().putString(PROFILE, profile.toString()).commit();
    }

    int mergeWithings(JSONArray records) throws JSONException {
        synchronized (prefs) {
            JSONObject stored = withingsWeights();
            Map<String, String> byMeasurement = new HashMap<>();
            for (Iterator<String> ids = stored.keys(); ids.hasNext();) {
                String id = ids.next();
                byMeasurement.put(measurementKey(stored.getJSONObject(id)), id);
            }
            int added = 0;
            boolean changed = false;
            for (int index = 0; index < records.length(); index++) {
                JSONObject record = records.getJSONObject(index);
                String id = record.getString("id");
                String key = measurementKey(record);
                JSONObject prior = stored.optJSONObject(id);
                if (prior != null && prior.toString().equals(record.toString())) continue;
                if (prior != null) byMeasurement.remove(measurementKey(prior));
                String duplicate = byMeasurement.put(key, id);
                if (duplicate != null && !id.equals(duplicate)) stored.remove(duplicate);
                if (prior == null && duplicate == null) added++;
                stored.put(id, record);
                changed = true;
            }
            if (changed && !prefs.edit().putString(WITHINGS, stored.toString()).commit()) return -1;
            return added;
        }
    }

    private JSONObject withingsWeights() throws JSONException {
        return new JSONObject(prefs.getString(WITHINGS, "{}"));
    }

    private static String measurementKey(JSONObject record) throws JSONException {
        return record.getLong("time") + ":" + record.getDouble("kg");
    }

    boolean addEntry(String day, String meal, String food, String amount) throws JSONException {
        String name = food.trim();
        int kcal = positiveInt(amount, 1, 10000);
        if (kcal == -1 || name.isEmpty() || name.length() > 80 || !validDay(day)
                || !("breakfast".equals(meal) || "lunch".equals(meal) || "dinner".equals(meal))) {
            return false;
        }

        JSONArray journal = entries();
        journal.put(new JSONObject().put("id", UUID.randomUUID().toString())
                .put("date", day).put("name", name).put("kcal", kcal).put("meal", meal));
        return prefs.edit().putString(ENTRIES, journal.toString()).commit();
    }

    boolean deleteEntry(String id) throws JSONException {
        JSONArray journal = entries();
        for (int index = 0; index < journal.length(); index++) {
            JSONObject entry = journal.optJSONObject(index);
            if (entry != null && entry.optString("id").equals(id)) {
                journal.remove(index);
                return prefs.edit().putString(ENTRIES, journal.toString()).commit();
            }
        }
        return false;
    }

    boolean saveGoal(String amount) {
        int goal = positiveInt(amount, 500, 10000);
        return goal != -1 && prefs.edit().putInt(GOAL, goal).commit();
    }

    private JSONArray entries() throws JSONException {
        return new JSONArray(prefs.getString(ENTRIES, "[]"));
    }

    private static boolean validDay(String value) {
        try {
            return LocalDate.parse(value).toString().equals(value);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static boolean validSex(String value) {
        return "female".equals(value) || "male".equals(value)
                || "other".equals(value) || "undisclosed".equals(value);
    }

    private static boolean validProfileDate(String value) {
        return value.length() == 10 && !value.startsWith("0000") && validDay(value);
    }

    private static boolean oneOf(String value, String... options) {
        for (String option : options) {
            if (option.equals(value)) return true;
        }
        return false;
    }

    private static double parseWeight(String value) {
        String normalized = value.trim().replace(',', '.');
        if (!normalized.matches("[0-9]{1,3}(\\.[0-9]{1,2})?")) return -1;
        double weight = Double.parseDouble(normalized);
        return weight >= 10 && weight <= 500 ? weight : -1;
    }

    private static int positiveInt(String value, int min, int max) {
        try {
            int number = Integer.parseInt(value.trim());
            return number >= min && number <= max ? number : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

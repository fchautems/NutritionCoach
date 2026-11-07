package ch.nutritioncoach;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/** Stores the journal outside the replaceable interface. Existing V0.2 entries keep their IDs. */
final class JournalStore {
    private static final String ENTRIES = "entries_v1";
    private static final String GOAL = "daily_goal_v1";
    private static final String PROFILE = "profile_v1";
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
                .toString();
    }

    boolean saveProfile(String firstName, String sex, String heightCm, String weightKg)
            throws JSONException {
        String name = firstName.trim();
        int height = positiveInt(heightCm, 50, 250);
        String normalizedWeight = weightKg.trim().replace(',', '.');
        if (name.isEmpty() || name.length() > 60 || height == -1
                || !normalizedWeight.matches("[0-9]{1,3}(\\.[0-9]{1,2})?")
                || !validSex(sex)) return false;
        double weight = Double.parseDouble(normalizedWeight);
        if (weight < 10 || weight > 500) return false;

        JSONObject profile = new JSONObject().put("name", name).put("sex", sex)
                .put("height", height).put("weight", weight);
        return prefs.edit().putString(PROFILE, profile.toString()).commit();
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

    private static int positiveInt(String value, int min, int max) {
        try {
            int number = Integer.parseInt(value.trim());
            return number >= min && number <= max ? number : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

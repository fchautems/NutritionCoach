package ch.nutritioncoach;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class JournalStoreTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 7);
    private static final String OLD_PROFILE =
            "{\"name\":\"Camille\",\"sex\":\"female\",\"height\":175,\"weight\":78.5}";
    private static final String OLD_ENTRIES =
            "[{\"id\":\"old\",\"date\":\"2026-11-07\",\"name\":\"Pomme\",\"kcal\":52},"
            + "{\"id\":\"lunch\",\"date\":\"2026-11-07\",\"meal\":\"lunch\","
            + "\"name\":\"Riz cuit (150 g)\",\"kcal\":195}]";
    private final Map<String, Object> values = new HashMap<>();
    private SharedPreferences prefs;
    private JournalStore store;

    @Before public void setup() {
        prefs = (SharedPreferences) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{SharedPreferences.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getInt":
                        case "getString":
                            return values.getOrDefault(args[0], args[1]);
                        case "edit":
                            return editor();
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
        values.put("profile_v1", OLD_PROFILE);
        values.put("entries_v1", OLD_ENTRIES);
        values.put("daily_goal_v1", 2175);
        store = new JournalStore(prefs);
    }

    @Test public void upgradesAndReopensAnOldProfileWithoutChangingGoalOrEntries() throws Exception {
        JSONObject before = new JSONObject(store.read());
        assertEquals("Camille", before.getJSONObject("profile").getString("name"));
        assertFalse(before.getJSONObject("profile").has("birthDate"));
        assertTrue(store.saveProfileDetails(details().toString(), TODAY));

        JSONObject after = new JSONObject(new JournalStore(prefs).read());
        JSONObject profile = after.getJSONObject("profile");
        assertEquals("Camille", profile.getString("name"));
        assertEquals(175, profile.getInt("height"));
        assertEquals(78.5, profile.getDouble("weight"), 0);
        assertEquals("1988-02-29", profile.getString("birthDate"));
        assertEquals(72.5, profile.getDouble("targetWeight"), 0);
        assertEquals("", profile.getString("deadline"));
        assertFalse(profile.has("weighIns"));
        assertEquals(2175, after.getInt("goal"));
        assertEquals(before.getJSONArray("entries").toString(), after.getJSONArray("entries").toString());
    }

    @Test public void tracksOneWeighInPerDayAndAllowsAnUnchangedMeasurement() throws Exception {
        values.remove("profile_v1");
        JSONObject data = details();
        assertTrue(store.saveProfileDetails(data.toString(), TODAY.minusDays(100)));
        assertTrue(store.saveProfileDetails(data.put("recordWeight", true).toString(), TODAY.minusDays(14)));
        data.put("recordWeight", false).put("weight", "77");
        assertTrue(store.saveProfileDetails(data.toString(), TODAY));
        assertTrue(store.saveProfileDetails(data.put("weight", "76,75").toString(), TODAY));
        JSONObject weights = profile().getJSONObject("weighIns");
        assertEquals(2, weights.length());
        assertEquals(78.5, weights.getDouble(TODAY.minusDays(14).toString()), 0);
        assertEquals(76.75, weights.getDouble(TODAY.toString()), 0);
    }

    @Test public void legacyBridgeEditsKeepNewFieldsAndMaintainClearsTheTarget() throws Exception {
        JSONObject data = details().put("deadline", "2027-01-31");
        assertTrue(store.saveProfileDetails(data.toString(), TODAY));
        assertTrue(store.saveProfile("Camille bis", "female", "175", "78,5"));
        assertEquals("Camille bis", profile().getString("name"));
        assertEquals("1988-02-29", profile().getString("birthDate"));
        assertEquals("2027-01-31", profile().getString("deadline"));

        data.put("goalMode", "maintain").put("name", "Camille bis");
        assertTrue(store.saveProfileDetails(data.toString(), TODAY));
        JSONObject profile = profile();
        assertEquals("maintain", profile.getString("goalMode"));
        assertFalse(profile.has("targetWeight"));
        assertFalse(profile.has("deadline"));
        assertEquals(2175, new JSONObject(store.read()).getInt("goal"));
    }

    @Test public void rejectsInvalidDetailsWithoutWritingAnyData() throws Exception {
        String before = store.read();
        assertFalse(store.saveProfileDetails(details().put("birthDate", "2001-02-29").toString(), TODAY));
        assertFalse(store.saveProfileDetails(details().put("birthDate", "2026-11-08").toString(), TODAY));
        assertFalse(store.saveProfileDetails(details().put("targetWeight", "NaN").toString(), TODAY));
        assertFalse(store.saveProfileDetails(details().put("movement", "unknown").toString(), TODAY));
        assertFalse(store.saveProfileDetails(details().put("deadline", "2026-04-31").toString(), TODAY));
        assertEquals(before, store.read());
    }

    @Test public void preservesManualHistoryAndDeduplicatesRepeatedWithingsImports() throws Exception {
        assertTrue(store.saveProfileDetails(details().put("recordWeight", true).toString(), TODAY.minusDays(1)));
        JSONObject record = new JSONObject().put("id", "first").put("kg", 77.123)
                .put("time", 1794038400000L).put("day", TODAY.toString())
                .put("offset", "+01:00").put("source", "com.withings.wiscale2");
        JSONArray batch = new JSONArray().put(record);
        assertEquals(1, store.mergeWithings(batch));
        assertEquals(0, store.mergeWithings(batch));
        record.put("id", "replacement");
        assertEquals(0, store.mergeWithings(batch));
        assertEquals(1, new JSONObject(store.read()).getJSONObject("withingsWeights").length());

        assertTrue(store.saveProfileDetails(details().put("weight", "77.12")
                .put("withingsId", "replacement").toString(), TODAY));
        JSONObject after = new JSONObject(store.read());
        assertEquals(77.12, after.getJSONObject("profile").getDouble("weight"), 0);
        assertFalse(after.getJSONObject("profile").getJSONObject("weighIns").has(TODAY.toString()));
        assertEquals(1, after.getJSONObject("profile").getJSONObject("weighIns").length());
        assertEquals(2175, after.getInt("goal"));
        assertEquals(new JSONArray(OLD_ENTRIES).toString(), after.getJSONArray("entries").toString());
    }

    private JSONObject profile() throws Exception {
        return new JSONObject(store.read()).getJSONObject("profile");
    }

    private static JSONObject details() throws Exception {
        return new JSONObject().put("name", "Camille").put("sex", "female")
                .put("height", "175").put("weight", "78,5").put("birthDate", "1988-02-29")
                .put("movement", "mixed").put("sport", "weekly").put("goalMode", "target")
                .put("targetWeight", "72,5").put("deadline", "").put("recordWeight", false);
    }

    private SharedPreferences.Editor editor() {
        Map<String, Object> pending = new HashMap<>();
        return (SharedPreferences.Editor) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{SharedPreferences.Editor.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "putString":
                        case "putInt":
                            pending.put((String) args[0], args[1]);
                            return proxy;
                        case "commit":
                            values.putAll(pending);
                            return true;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
}

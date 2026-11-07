package ch.nutritioncoach;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Offline-first proof of concept. All meals and the goal stay in SharedPreferences.
 * No internet permission, account or server is required.
 */
public final class MainActivity extends Activity {
    private static final String PREFS_NAME = "nutrition_coach";
    private static final String ENTRIES_KEY = "entries_v1";
    private static final String GOAL_KEY = "daily_goal_v1";
    private static final int PRIMARY = Color.rgb(36, 92, 80);
    private static final int INK = Color.rgb(27, 48, 42);
    private static final int MUTED = Color.rgb(103, 117, 110);
    private static final int BACKGROUND = Color.rgb(246, 249, 246);
    private static final int BORDER = Color.rgb(220, 231, 223);

    private final List<FoodEntry> entries = new ArrayList<>();
    private final DateTimeFormatter dateFormat =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private SharedPreferences prefs;
    private LocalDate selectedDate;
    private int goal;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(PRIMARY);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedDate = LocalDate.now();
        goal = prefs.getInt(GOAL_KEY, 2000);
        loadEntries();
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        setContentView(scroll);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(26), dp(20), dp(30));
        scroll.addView(page);

        TextView heading = text("NutritionCoach", 29, INK, true);
        page.addView(heading);
        TextView tagline = text("Ton journal alimentaire, en toute simplicité.", 14, MUTED, false);
        addBelow(page, tagline, 2, 22);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(dateRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button previous = button("‹", false);
        dateRow.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
        previous.setContentDescription("Jour précédent");
        previous.setOnClickListener(v -> {
            selectedDate = selectedDate.minusDays(1);
            render();
        });

        TextView dateLabel = text(capitalize(selectedDate.format(dateFormat)), 15, INK, true);
        dateLabel.setGravity(Gravity.CENTER);
        dateRow.addView(dateLabel, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button next = button("›", false);
        dateRow.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
        next.setContentDescription("Jour suivant");
        next.setOnClickListener(v -> {
            selectedDate = selectedDate.plusDays(1);
            render();
        });

        if (!selectedDate.equals(LocalDate.now())) {
            Button today = button("Revenir à aujourd'hui", false);
            addBelow(page, today, 8, 6);
            today.setOnClickListener(v -> {
                selectedDate = LocalDate.now();
                render();
            });
        }

        LinearLayout summary = card(page);
        int total = CalorieMath.totalForDate(entries, selectedDate.toString());
        summary.addView(text("CALORIES DU JOUR", 12, MUTED, true));
        TextView amount = text(formatted(total) + " kcal", 33, INK, true);
        addBelow(summary, amount, 8, 7);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Math.max(goal, 1));
        progress.setProgress(Math.min(total, Math.max(goal, 1)));
        progress.setProgressTintList(ColorStateList.valueOf(PRIMARY));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(BORDER));
        summary.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(10)));
        String remaining = total <= goal
                ? formatted(goal - total) + " kcal restantes"
                : formatted(total - goal) + " kcal au-dessus de l'objectif";
        TextView remainder = text(remaining, 14, total > goal ? Color.rgb(174, 68, 54) : PRIMARY, true);
        addBelow(summary, remainder, 12, 0);

        Button editGoal = button("Objectif : " + formatted(goal) + " kcal  ·  Modifier", false);
        addBelow(summary, editGoal, 12, 0);
        editGoal.setOnClickListener(v -> changeGoal());

        TextView addTitle = text("Ajouter un aliment", 20, INK, true);
        addBelow(page, addTitle, 8, 12);
        LinearLayout addCard = card(page);

        addCard.addView(text("Aliment ou plat", 13, MUTED, true));
        EditText name = editField("Ex. : salade de pâtes", InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        addBelow(addCard, name, 6, 15);

        addCard.addView(text("Calories (kcal)", 13, MUTED, true));
        EditText calories = editField("Ex. : 420", InputType.TYPE_CLASS_NUMBER);
        addBelow(addCard, calories, 6, 18);

        Button add = button("Ajouter au journal", true);
        addCard.addView(add, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        add.setOnClickListener(v -> {
            String food = name.getText().toString().trim();
            String number = calories.getText().toString().trim();
            if (food.isEmpty() || food.length() > 80) {
                name.setError("Indique un nom de 1 à 80 caractères.");
                return;
            }
            final int kcal;
            try {
                kcal = Integer.parseInt(number);
            } catch (NumberFormatException e) {
                calories.setError("Entre un nombre de calories valide.");
                return;
            }
            if (kcal < 1 || kcal > 10000) {
                calories.setError("Entre 1 et 10 000 kcal.");
                return;
            }
            entries.add(new FoodEntry(UUID.randomUUID().toString(),
                    selectedDate.toString(), food, kcal));
            saveEntries();
            Toast.makeText(this, "Aliment ajouté", Toast.LENGTH_SHORT).show();
            render();
        });

        TextView logTitle = text("Journal du jour", 20, INK, true);
        addBelow(page, logTitle, 8, 12);

        boolean found = false;
        for (int i = entries.size() - 1; i >= 0; i--) {
            FoodEntry entry = entries.get(i);
            if (!selectedDate.toString().equals(entry.date)) continue;
            found = true;
            LinearLayout item = card(page);
            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            item.addView(itemRow);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.addView(text(entry.name, 16, INK, true));
            addBelow(info, text(formatted(entry.kcal) + " kcal", 14, MUTED, false), 4, 0);
            itemRow.addView(info, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            Button delete = button("×", false);
            delete.setContentDescription("Supprimer " + entry.name);
            itemRow.addView(delete, new LinearLayout.LayoutParams(dp(48), dp(48)));
            delete.setOnClickListener(v -> confirmDelete(entry));
        }
        if (!found) {
            TextView empty = text("Aucun aliment enregistré pour cette journée.", 14, MUTED, false);
            addBelow(page, empty, 4, 10);
        }

        TextView note = text("Données sauvegardées uniquement sur cet appareil. Les calories sont saisies manuellement.", 12, MUTED, false);
        addBelow(page, note, 16, 0);
    }

    private void changeGoal() {
        EditText field = new EditText(this);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setSingleLine(true);
        field.setText(String.valueOf(goal));
        field.selectAll();
        int space = dp(22);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(space, dp(6), space, 0);
        wrap.addView(field, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this)
                .setTitle("Objectif calorique quotidien")
                .setView(wrap)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    try {
                        int candidate = Integer.parseInt(field.getText().toString().trim());
                        if (candidate < 500 || candidate > 10000) {
                            Toast.makeText(this, "Objectif autorisé : 500 à 10 000 kcal.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        goal = candidate;
                        prefs.edit().putInt(GOAL_KEY, goal).apply();
                        render();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Objectif invalide.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void confirmDelete(FoodEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer cet aliment ?")
                .setMessage(entry.name + " (" + formatted(entry.kcal) + " kcal)")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    entries.removeIf(item -> item.id.equals(entry.id));
                    saveEntries();
                    render();
                }).show();
    }

    private void loadEntries() {
        entries.clear();
        try {
            JSONArray data = new JSONArray(prefs.getString(ENTRIES_KEY, "[]"));
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                String date = item.optString("date", "");
                String name = item.optString("name", "");
                int kcal = item.optInt("kcal", 0);
                if (!id.isEmpty() && !date.isEmpty() && !name.isEmpty()
                        && kcal > 0 && kcal <= 10000) {
                    entries.add(new FoodEntry(id, date, name, kcal));
                }
            }
        } catch (JSONException ignored) {
            Toast.makeText(this, "Journal local illisible.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveEntries() {
        JSONArray data = new JSONArray();
        for (FoodEntry entry : entries) {
            try {
                JSONObject item = new JSONObject();
                item.put("id", entry.id);
                item.put("date", entry.date);
                item.put("name", entry.name);
                item.put("kcal", entry.kcal);
                data.put(item);
            } catch (JSONException ignored) {
                // Entries are locally validated primitive values.
            }
        }
        prefs.edit().putString(ENTRIES_KEY, data.toString()).apply();
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(17), dp(17), dp(17), dp(17));
        view.setBackground(background(Color.WHITE, 18, BORDER));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        parent.addView(view, lp);
        return view;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, boolean filled) {
        Button view = new Button(this);
        view.setText(label);
        view.setAllCaps(false);
        view.setTextSize(14);
        view.setTextColor(filled ? Color.WHITE : PRIMARY);
        view.setBackground(background(filled ? PRIMARY : Color.WHITE, 12, BORDER));
        view.setPadding(dp(8), 0, dp(8), 0);
        return view;
    }

    private EditText editField(String hint, int inputType) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setTextSize(16);
        field.setTextColor(INK);
        field.setSingleLine(true);
        field.setInputType(inputType);
        field.setPadding(dp(12), dp(12), dp(12), dp(12));
        field.setBackground(background(BACKGROUND, 10, BORDER));
        return field;
    }

    private GradientDrawable background(int fill, int radius, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), border);
        return drawable;
    }

    private void addBelow(LinearLayout parent, View view, int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(top);
        lp.bottomMargin = dp(bottom);
        parent.addView(view, lp);
    }

    private String formatted(int amount) {
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(amount);
    }

    private String capitalize(String value) {
        return value.isEmpty() ? value : value.substring(0, 1).toUpperCase(Locale.FRENCH) + value.substring(1);
    }

    private int dp(float size) {
        return (int) (size * getResources().getDisplayMetrics().density + 0.5f);
    }
}

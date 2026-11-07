package ch.nutritioncoach;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
    private static final int PRIMARY = 0xff214b3b;
    private static final int INK = 0xff1e3129;
    private static final int MUTED = 0xff51645b;
    private static final int BACKGROUND = 0xfff8f6f0;
    private static final int BORDER = 0xffdedfd5;
    private static final int FOREST = 0xff173b31;
    private static final int MINT = 0xffc4ead6;

    private final List<FoodEntry> entries = new ArrayList<>();
    private final DateTimeFormatter dateFormat =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private SharedPreferences prefs;
    private LocalDate selectedDate;
    private int goal;
    private AlertDialog addDialog;
    private EditText draftName;
    private EditText draftCalories;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedDate = state == null ? LocalDate.now() : LocalDate.parse(state.getString("day", LocalDate.now().toString()));
        goal = prefs.getInt(GOAL_KEY, 2000);
        loadEntries();
        render();
        if (state != null && state.containsKey("draftName")) {
            showAddDialog(state.getString("draftName"), state.getString("draftCalories"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("day", selectedDate.toString());
        if (addDialog != null && addDialog.isShowing()) {
            state.putString("draftName", draftName.getText().toString());
            state.putString("draftCalories", draftCalories.getText().toString());
        }
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        setContentView(scroll);

        LinearLayout page = column();
        page.setPadding(dp(20), dp(12), dp(20), dp(28));
        scroll.addView(page);

        page.addView(text("NutritionCoach", 15, PRIMARY, true));
        boolean today = selectedDate.equals(LocalDate.now());
        renderDate(page, today);
        renderSummary(page);

        Button add = button("+  Ajouter un aliment", true);
        add.setContentDescription("Ajouter un aliment");
        add.setMinHeight(dp(54));
        addBelow(page, add, 14, 22);
        add.setOnClickListener(v -> showAddDialog("", ""));

        renderJournal(page);
    }

    private void renderDate(LinearLayout page, boolean today) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        addBelow(page, row, 14, 0);

        row.addView(dayButton("‹", "Jour précédent", -1),
                new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout center = column();
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setPadding(dp(4), 0, dp(4), 0);
        if (today) {
            TextView heading = text("Aujourd’hui", 26, INK, true);
            heading.setGravity(Gravity.CENTER);
            markHeading(heading);
            center.addView(heading);
        }
        TextView date = text(capitalize(selectedDate.format(dateFormat)), today ? 13 : 19,
                today ? MUTED : INK, !today);
        date.setGravity(Gravity.CENTER);
        if (!today) markHeading(date);
        center.addView(date);
        row.addView(center, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        row.addView(dayButton("›", "Jour suivant", 1),
                new LinearLayout.LayoutParams(dp(48), dp(48)));

        if (!today) {
            Button back = button("Revenir à aujourd’hui", false);
            addBelow(page, back, 8, 0);
            back.setOnClickListener(v -> {
                selectedDate = LocalDate.now();
                render();
            });
        }
    }

    private Button dayButton(String symbol, String description, int offset) {
        Button button = button(symbol, false);
        button.setTextSize(25);
        button.setContentDescription(description);
        button.setOnClickListener(v -> {
            selectedDate = selectedDate.plusDays(offset);
            render();
        });
        return button;
    }

    private void renderSummary(LinearLayout page) {
        LinearLayout summary = column();
        summary.setPadding(dp(20), dp(17), dp(20), dp(11));
        summary.setBackground(background(FOREST, 24, FOREST));
        addBelow(page, summary, 16, 0);

        int total = CalorieMath.totalForDate(entries, selectedDate.toString());
        summary.addView(text("CALORIES DU JOUR", 12, MINT, true));
        addBelow(summary, text(formatted(total) + " kcal", 38, Color.WHITE, true), 3, 11);

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LayerDrawable track = new LayerDrawable(new Drawable[]{
                background(0xff436658, 5, 0xff436658),
                new ClipDrawable(background(MINT, 5, MINT), Gravity.START, ClipDrawable.HORIZONTAL)
        });
        track.setId(0, android.R.id.background);
        track.setId(1, android.R.id.progress);
        progress.setProgressDrawable(track);
        progress.setMax(Math.max(goal, 1));
        progress.setProgress(Math.min(total, Math.max(goal, 1)));
        progress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        summary.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));

        String balance;
        if (total < goal) {
            balance = formatted(goal - total) + " kcal restantes";
        } else if (total == goal) {
            balance = "Objectif atteint";
        } else {
            balance = formatted(total - goal) + " kcal au-dessus de l’objectif";
        }
        addBelow(summary, text(balance, 15, Color.WHITE, true), 10, 2);

        Button editGoal = button("Objectif " + formatted(goal) + " kcal  ·  Modifier", false);
        editGoal.setTextColor(MINT);
        editGoal.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        editGoal.setPadding(dp(8), dp(7), dp(8), dp(7));
        editGoal.setBackground(ripple(0xff25493e, 11, 0xff25493e, 0x36ffffff));
        addBelow(summary, editGoal, 9, 3);
        editGoal.setOnClickListener(v -> changeGoal());
    }

    private void renderJournal(LinearLayout page) {
        TextView title = text("Journal du jour", 22, INK, true);
        markHeading(title);
        addBelow(page, title, 0, 10);

        LinearLayout journal = column();
        journal.setBackground(background(Color.WHITE, 18, BORDER));
        journal.setPadding(dp(14), dp(4), dp(10), dp(4));
        page.addView(journal);

        String day = selectedDate.toString();
        boolean found = false;
        for (int i = entries.size() - 1; i >= 0; i--) {
            FoodEntry entry = entries.get(i);
            if (!day.equals(entry.date)) continue;
            if (found) {
                View divider = new View(this);
                divider.setBackgroundColor(BORDER);
                journal.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
            found = true;

            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            journal.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout info = column();
            info.setPadding(0, 0, dp(6), 0);
            info.addView(text(entry.name, 16, INK, true));
            addBelow(info, text(formatted(entry.kcal) + " kcal", 14, MUTED, false), 2, 0);
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            Button delete = button("×", false);
            delete.setTextSize(20);
            delete.setContentDescription("Supprimer " + entry.name);
            row.addView(delete, new LinearLayout.LayoutParams(dp(48), dp(48)));
            delete.setOnClickListener(v -> confirmDelete(entry));
        }
        if (!found) {
            addBelow(journal, text("Aucun aliment enregistré", 16, INK, true), 9, 2);
            addBelow(journal, text("Ajoute un aliment pour commencer ce journal.", 14, MUTED, false),
                    0, 10);
        }
    }

    private void showAddDialog(String name, String calories) {
        LinearLayout fields = column();
        fields.setPadding(dp(24), dp(12), dp(24), dp(4));

        draftName = editField("Ex. salade de pâtes",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        draftName.setText(name);
        addInput(fields, "Aliment ou plat", draftName);

        draftCalories = editField("Ex. 420", InputType.TYPE_CLASS_NUMBER);
        draftCalories.setText(calories);
        addInput(fields, "Calories (kcal)", draftCalories);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(fields);
        addDialog = new AlertDialog.Builder(this)
                .setTitle("Ajouter un aliment")
                .setView(scroll)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Ajouter", null)
                .create();
        draftName.requestFocus();
        draftName.setSelection(draftName.length());
        addDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        addDialog.show();
        addDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> addEntry());
        addDialog.setOnDismissListener(dialog -> {
            addDialog = null;
            draftName = null;
            draftCalories = null;
        });
    }

    private void addInput(LinearLayout fields, String label, EditText input) {
        input.setId(View.generateViewId());
        TextView caption = text(label, 14, INK, true);
        caption.setLabelFor(input.getId());
        fields.addView(caption);
        addBelow(fields, input, 4, 12);
    }

    private void addEntry() {
        String food = draftName.getText().toString().trim();
        if (food.isEmpty() || food.length() > 80) {
            showInputError(draftName, "Indique un nom de 1 à 80 caractères.");
            return;
        }
        final int kcal;
        try {
            kcal = Integer.parseInt(draftCalories.getText().toString().trim());
        } catch (NumberFormatException e) {
            showInputError(draftCalories, "Entre un nombre de calories valide.");
            return;
        }
        if (kcal < 1 || kcal > 10000) {
            showInputError(draftCalories, "Indique une valeur de 1 à 10 000 kcal.");
            return;
        }
        entries.add(new FoodEntry(UUID.randomUUID().toString(), selectedDate.toString(), food, kcal));
        saveEntries();
        addDialog.dismiss();
        Toast.makeText(this, "Aliment ajouté", Toast.LENGTH_SHORT).show();
        render();
    }

    private void showInputError(EditText field, String error) {
        field.requestFocus();
        field.setError(error);
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

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private void markHeading(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setAccessibilityHeading(true);
        }
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
        int fill = filled ? PRIMARY : Color.WHITE;
        view.setBackground(ripple(fill, 13, filled ? PRIMARY : BORDER,
                filled ? 0x36ffffff : 0x22214b3b));
        view.setStateListAnimator(null);
        view.setMinWidth(0);
        view.setMinHeight(dp(48));
        view.setPadding(dp(8), dp(7), dp(8), dp(7));
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

    private RippleDrawable ripple(int fill, int radius, int border, int highlight) {
        return new RippleDrawable(ColorStateList.valueOf(highlight), background(fill, radius, border),
                background(Color.WHITE, radius, Color.WHITE));
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

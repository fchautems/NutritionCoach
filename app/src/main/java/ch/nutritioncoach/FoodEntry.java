package ch.nutritioncoach;

/** An intake saved on a single local calendar date. */
final class FoodEntry {
    final String id;
    final String date;
    final String name;
    final int kcal;

    FoodEntry(String id, String date, String name, int kcal) {
        this.id = id;
        this.date = date;
        this.name = name;
        this.kcal = kcal;
    }
}

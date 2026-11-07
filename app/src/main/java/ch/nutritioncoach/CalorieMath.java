package ch.nutritioncoach;

import java.util.List;

final class CalorieMath {
    private CalorieMath() {}

    static int totalForDate(List<FoodEntry> entries, String date) {
        int total = 0;
        for (FoodEntry entry : entries) {
            if (date.equals(entry.date)) {
                total += entry.kcal;
            }
        }
        return total;
    }
}

package ch.nutritioncoach;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class CalorieMathTest {
    @Test
    public void totalsOnlySelectedDate() {
        assertEquals(620, CalorieMath.totalForDate(Arrays.asList(
                new FoodEntry("a", "2026-11-07", "Petit-déjeuner", 220),
                new FoodEntry("b", "2026-11-06", "Autre jour", 900),
                new FoodEntry("c", "2026-11-07", "Repas", 400)),
                "2026-11-07"));
    }

    @Test
    public void emptyDayHasZeroCalories() {
        assertEquals(0, CalorieMath.totalForDate(Collections.emptyList(), "2026-11-07"));
    }
}

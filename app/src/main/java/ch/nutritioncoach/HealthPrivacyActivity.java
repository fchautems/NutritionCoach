package ch.nutritioncoach;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;

public final class HealthPrivacyActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        new AlertDialog.Builder(this)
                .setTitle("Données de Santé Connect")
                .setMessage("NutritionCoach lit le poids, la date et la provenance des pesées "
                        + "Withings présentes dans Health Connect. Les copies sont stockées sur "
                        + "ce téléphone et servent au suivi de ton poids. NutritionCoach lit aussi "
                        + "les pas transmis par Google Fit et NOLATR pour afficher leurs totaux "
                        + "dans le journal, sans en enregistrer de copie. Tu peux retirer "
                        + "l’autorisation dans Health Connect.")
                .setPositiveButton("Fermer", (dialog, button) -> finish())
                .setOnDismissListener(dialog -> finish())
                .show();
    }
}

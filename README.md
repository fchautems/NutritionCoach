# NutritionCoach

POC Android natif pour suivre les calories quotidiennes, sans compte ni serveur.

## Version 0.1
- Ajouter un aliment et ses calories.
- Voir le total par jour et naviguer dans l’historique.
- Définir un objectif calorique journalier.
- Supprimer un aliment saisi.
- Conserver les données localement sur le téléphone.

## APK
Après la compilation GitHub Actions, retrouver l'APK de test dans la [release v0.1.0](https://github.com/fchautems/NutritionCoach/releases/tag/v0.1.0) (ou dans les artifacts Actions si la publication de la release échoue).

## Développement
Projet Android Java / Gradle, minSdk 26. Construire avec `gradle assembleDebug` dans un environnement doté du SDK Android 35.

**Attention** : POC de saisie manuelle, sans estimation automatique des calories par photo ni sauvegarde cloud. Les données sont locales et peuvent être perdues en cas de désinstallation.

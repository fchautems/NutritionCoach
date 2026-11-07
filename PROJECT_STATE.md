# NutritionCoach — état du projet

Dernière mise à jour : 2026-11-07

## Objectif
Livrer rapidement une APK Android installable de suivi calorique, puis améliorer l'application par petites itérations.

## État vérifié
- Dépôt : `fchautems/NutritionCoach`, branche `main`.
- V0.1 Android natif Java (minSdk 26), sans serveur, compte ni permission Internet.
- Fonctionnalités implémentées : aliment + kcal, total par date, objectif journalier, navigation temporelle, suppression, persistance locale.
- CI GitHub Actions : **tests unitaires réussis** et **APK debug construite** (run `42369769381`).
- Préversion v0.1.0 publiée : [NutritionCoach-v0.1.0-debug.apk](https://github.com/fchautems/NutritionCoach/releases/download/v0.1.0/NutritionCoach-v0.1.0-debug.apk).
- SHA-256 de l'APK publiée : `4494973fe49d30476935b5b11511d43a8c19f0ecdb8835664ad8611fc2af4896`.
- **Installation et fonctionnement sur un véritable téléphone : non encore vérifiés.**

## Modifications
- 2026-11-07 : dépôt initialisé et POC Android créé.
- 2026-11-07 : workflow SDK corrigé après deux échecs de configuration ; tests, build et release réussis.

## Contrôles
- Réussi : `gradle --no-daemon :app:testDebugUnitTest`.
- Réussi : `gradle --no-daemon :app:assembleDebug`.
- Réussi : publication d'un APK de debug dans la release GitHub v0.1.0.
- À faire sur Android : installer, ajouter/supprimer un repas, changer d'objectif et vérifier les données après redémarrage.

## Blocages / limites
- Pas de reconnaissance des repas par photo ni de base nutritionnelle : calories saisies manuellement dans cette V0.
- Pas de synchronisation/export : les données restent sur l'appareil et sont effacées à la désinstallation.
- L'APK est signée avec la clé de debug automatisée du runner : pour des mises à jour Android fiables, passer à une signature de release persistante.

## Prochaine action
- Faire tester l'APK sur Android, puis prioriser les retours pour V0.2.

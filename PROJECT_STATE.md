# NutritionCoach — état du projet

Dernière mise à jour : 2026-11-07

## Objectif
Livrer une APK Android réellement installable pour valider rapidement le suivi manuel des calories, avant de faire évoluer le produit.

## État vérifié
- Dépôt GitHub : `fchautems/NutritionCoach`, branche `main`.
- Projet Android natif Java, Android API min 26, sans serveur, sans compte et sans permission Internet.
- Code V0.1 ajouté : saisie aliment + kcal, total et objectif par jour, navigation entre jours, suppression, persistance locale.
- Tests unitaires du calcul des totaux inclus.
- Workflow GitHub Actions prévu pour tester, construire et publier la préversion `v0.1.0`.
- Compilation **non encore vérifiée** à la rédaction de cet état : vérifier la première exécution GitHub Actions.

## Modifications
- 2026-11-07 : initialisation du dépôt et création du POC Android.
- Choix de la simplicité : saisie manuelle d'abord ; pas de backend ni de reconnaissance photo à ce stade.

## Contrôles
- Prévu : `gradle :app:testDebugUnitTest` et `gradle :app:assembleDebug` sur GitHub Actions.
- Tests sur un appareil Android : à faire après obtention de l'APK.

## Blocages / risques
- Environnement de travail local sans SDK Android : dépendance à GitHub Actions pour compiler.
- Permissions GitHub Actions pour publier une release : à confirmer.

## Prochaine action
- Vérifier le workflow GitHub Actions et l'APK publiée ; corriger si nécessaire, puis tester la saisie, la conservation des données et la suppression sur téléphone.

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
- Tests unitaires et compilation Gradle **réussis** dans GitHub Actions (run 42369769381).\n- APK de préversion **publiée** : [NutritionCoach-v0.1.0-debug.apk](https://github.com/fchautems/NutritionCoach/releases/download/v0.1.0/NutritionCoach-v0.1.0-debug.apk) ; SHA-256 : `4494973fe49d30476935b5b11511d43a8c19f0ecdb8835664ad8611fc2af4896`.

## Modifications
- 2026-11-07 : initialisation du dépôt et création du POC Android.\n- 2026-11-07 : corrections du setup SDK GitHub Actions ; tests et build réussis ; APK v0.1.0 publiée.
- Choix de la simplicité : saisie manuelle d'abord ; pas de backend ni de reconnaissance photo à ce stade.

## Contrôles
- Prévu : `gradle :app:testDebugUnitTest` et `gradle :app:assembleDebug` sur GitHub Actions.
- Tests sur un appareil Android : à faire après obtention de l'APK.

## Blocages / risques
- Environnement de travail local sans SDK Android : dépendance à GitHub Actions pour compiler.
- Permissions GitHub Actions pour publier une release : à confirmer.

## Prochaine action
- Installer l'APK sur Android, valider les opérations et la conservation des données, puis prioriser la V0.2 avec les retours utilisateur.

# NutritionCoach — état du projet

Dernière mise à jour : 2026-11-07

## État vérifié

- V0.1.0 Android Java, minSdk 26, sans compte ni permission Internet.
- Le POC sait enregistrer un aliment et ses kcal, afficher le total par jour, naviguer par date, modifier l’objectif et supprimer une entrée. Le journal reste local.
- Tests et compilation V0.1 réussis dans le [run 42369769381](https://github.com/fchautems/NutritionCoach/actions/runs/42369769381). [APK V0.1 publiée](https://github.com/fchautems/NutritionCoach/releases/tag/v0.1.0).
- Le test utilisateur confirme que la V0.1 fonctionne sur téléphone et que le journal affiche une entrée et son total. Les autres scénarios sur téléphone restent à vérifier.

## Décisions et changements

- 2026-11-07. Passage du POC à de petites itérations testables. [Spécifications et lots](docs/SPECIFICATIONS.md).
- Premier lot : refaire l’écran Aujourd’hui et déplacer la saisie manuelle dans un dialogue. Les repas par type, les portions et les favoris sont des lots séparés.

## Contrôles et limites

- La V0.1 est signée avec une clé debug générée par le runner CI. Une nouvelle APK signée avec une autre clé ne peut pas la remplacer en conservant son journal.
- L’aperçu V0.2 doit être installable en parallèle avec un journal indépendant.

## Prochaine action

- Coder et vérifier le premier lot avant l’essai sur téléphone.

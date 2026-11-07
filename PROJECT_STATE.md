# NutritionCoach — état du projet

Dernière mise à jour : 2026-11-07

## État vérifié

- V0.1.0 Android Java, minSdk 26, sans compte ni permission Internet.
- Le POC sait enregistrer un aliment et ses kcal, afficher le total par jour, naviguer par date, modifier l’objectif et supprimer une entrée. Le journal reste local.
- Tests et compilation V0.1 réussis dans le [run 42369769381](https://github.com/fchautems/NutritionCoach/actions/runs/42369769381). [APK V0.1 publiée](https://github.com/fchautems/NutritionCoach/releases/tag/v0.1.0).
- Le test utilisateur confirme que la V0.1 fonctionne sur téléphone et que le journal affiche une entrée et son total. Les autres scénarios sur téléphone restent à vérifier.

## Décisions et changements

- 2026-11-07. Passage du POC à de petites itérations testables. [Spécifications et lots](docs/SPECIFICATIONS.md).
- Premier lot implémenté : bilan plus visible, journal compact et saisie manuelle dans un dialogue. Les repas par type, les portions et les favoris sont des lots séparés.
- Variante `preview` avec son propre identifiant Android. La CI construit les aperçus et publie uniquement sur tag afin de conserver les anciens APK.
- 2026-11-07. Priorité confirmée avant les repas et portions : une installation Android de départ, mises à jour courantes automatiques et bouton « Vérifier les mises à jour ». Socle Android et interface web versionnée proposés.

- 2026-11-07. Direction visuelle précisée : fourchette seule pour l’icône, palette vive à choisir parmi six pistes, tuiles illustrées et libellées pour les catégories alimentaires. Deux écrans ont été maquettés pour explorer le parcours.

## Contrôles et limites

- La V0.1 est signée avec une clé debug générée par le runner CI. Une nouvelle APK signée avec une autre clé ne peut pas la remplacer en conservant son journal.
- Les 2 tests unitaires du calcul quotidien passent. APK d’aperçu compilée localement avec le SDK Android 35, archive et signature vérifiées. Identifiant vérifié : `ch.nutritioncoach.preview`.
- Vérification Gradle du lot 01 : tests unitaires, compilation de la variante `preview` et artefact réussis dans le [run 42370829278](https://github.com/fchautems/NutritionCoach/actions/runs/42370829278).
- L’aperçu V0.2 est distinct de la V0.1 et ouvre un journal indépendant. L’interface et les scénarios d’installation, d’ajout et de navigation restent à vérifier sur téléphone.

## Prochaine action

- Choisir la palette et valider la direction visuelle. Valider le lot de mise à jour en installant le socle une seule fois, en publiant deux interfaces successives et en vérifiant le journal après mise à jour et hors ligne. Vérifier la transition de signature ou l’export/import avant tout remplacement du POC.

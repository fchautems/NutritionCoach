# NutritionCoach — état du projet

Dernière mise à jour : 2026-11-07

## État vérifié

- V0.1.0 Android Java, minSdk 26, sans compte ni permission Internet.
- Le POC sait enregistrer un aliment et ses kcal, afficher le total par jour, naviguer par date, modifier l’objectif et supprimer une entrée. Le journal reste local.
- Tests et compilation V0.1 réussis dans le [run 42369769381](https://github.com/fchautems/NutritionCoach/actions/runs/42369769381). [APK V0.1 publiée](https://github.com/fchautems/NutritionCoach/releases/tag/v0.1.0).
- Le test utilisateur confirme que la V0.1 fonctionne sur téléphone et que le journal affiche une entrée et son total. Les autres scénarios sur téléphone restent à vérifier.

- V0.3 en usage sur téléphone. La capture du 2026-11-07 montre un aliment saisi à Midi et l’interface 2 chargée.
- V0.3 codée : formulaire de profil local au premier lancement, puis journal du jour et accès à Petit-déjeuner, Midi et Soir. La saisie reste manuelle. La date, l’objectif, la suppression et les anciennes entrées sont conservés.
- Socle Android V0.3 : interface embarquée pour le hors-ligne, canal HTTPS versionné, vérification automatique au lancement et bouton de vérification dans les paramètres.

- Interface 3 publiée sur le canal existant. 49 aliments hors ligne, 8 catégories avec pictogrammes intégrés, recherche, portions en grammes et calories calculées. Saisie manuelle en secours. Les paramètres ont deux actions et une fermeture.

- Sur `main`, l’aperçu V0.4 embarque l’interface 3 comme base et y ajoute le profil détaillé, l’estimation de maintien et l’objectif de poids. Les données supplémentaires sont enregistrées dans le profil Android existant. Le pont V0.3 reste compatible avec la partie historique de cette interface.

## Décisions et changements

- 2026-11-07. Passage du POC à de petites itérations testables. [Spécifications et lots](docs/SPECIFICATIONS.md).
- Premier lot implémenté : bilan plus visible, journal compact et saisie manuelle dans un dialogue. Les repas par type, les portions et les favoris sont des lots séparés.
- Variante `preview` avec son propre identifiant Android. La CI construit les aperçus et publie uniquement sur tag afin de conserver les anciens APK.
- 2026-11-07. Priorité confirmée avant les repas et portions : une installation Android de départ, mises à jour courantes automatiques et bouton « Vérifier les mises à jour ». Socle Android et interface web versionnée proposés.
- 2026-11-07. Direction visuelle affinée : fourchette cerclée, couleurs adoucies, tuiles alimentaires illustrées et relief léger sur les commandes. Nouvelle maquette des deux écrans avec une palette bleu denim, crème et abricot proposée. La date et le réglage de l’objectif sont représentés.

- 2026-11-07. Bleu denim retenu par défaut. Framboise explorée comme maquette. Withings et thèmes alternatifs reportés. Le lot repas est découpé en profil et saisie manuelle, puis catalogue et portions.

- 2026-11-07. Les estimations utilisent la [base suisse des valeurs nutritives de l’OSAV V7.1](https://www.naehrwertdaten.ch/fr/telechargement/). L’interface 3 conserve le protocole 1 et enregistre le libellé avec la portion et les kcal calculées via le pont existant.

- 2026-11-07. L’objectif de poids est indépendant de l’objectif calorique. La tendance utilise des pesées datées et le poids issu d’un profil ancien reste disponible. [Méthode et essai sur téléphone](docs/PROFIL_OBJECTIF.md).

- 2026-11-07. [Profil intégré](https://github.com/fchautems/NutritionCoach/pull/4), puis [branche d’itération fusionnée dans `main`](https://github.com/fchautems/NutritionCoach/pull/1). L’APK V0.4 signée avec le certificat d’aperçu a été fournie pour l’essai. Le canal HTTPS sert actuellement l’interface 3.

## Contrôles et limites

- La V0.1 est signée avec une clé debug générée par le runner CI. Une nouvelle APK signée avec une autre clé ne peut pas la remplacer en conservant son journal.
- Les 2 tests unitaires du calcul quotidien passent. APK d’aperçu compilée localement avec le SDK Android 35, archive et signature vérifiées. Identifiant vérifié : `ch.nutritioncoach.preview`.
- Vérification Gradle du lot 01 : tests unitaires, compilation de la variante `preview` et artefact réussis dans le [run 42370829278](https://github.com/fchautems/NutritionCoach/actions/runs/42370829278).
- L’aperçu V0.2 est distinct de la V0.1 et ouvre un journal indépendant. L’interface et les scénarios d’installation, d’ajout et de navigation restent à vérifier sur téléphone.

- V0.3 : cinq tests JUnit réussis, parcours de l’interface vérifié avec le pont Android simulé et stockage Java vérifié avec préférences simulées. Compilation locale, archive, ressources et certificat APK vérifiés. Même identifiant et certificat que la V0.2 distribuée.
- CI V0.3 réussie : tests, compilation Gradle et artefact dans le [run 42373121667](https://github.com/fchautems/NutritionCoach/actions/runs/42373121667). Le canal HTTPS sert l’interface 2 et le SHA-256 téléchargé correspond au manifeste.
- Interface 3 : manifeste HTTPS et empreinte vérifiés sur le canal public. Test DOM avec pont simulé dans les deux sens, 2 → 3 → 2, avec le même profil, le même objectif, une entrée historique et une saisie manuelle de la V2. La V2 relit aussi l’ajout fait par le catalogue de la V3. Le chargement de la V3 sur téléphone reste à confirmer.

- Profil V0.4 : neuf tests JUnit et quatre tests DOM réussis. [CI de la PR #4 réussie](https://github.com/fchautems/NutritionCoach/actions/runs/42378225698). Compilation locale avec le SDK Android 35 et certificat vérifié identique à celui de l’aperçu V0.3 distribué. L’essai physique de la mise à niveau reste à effectuer.

## Prochaine action

- Essayer l’APK V0.4 par-dessus la V0.3 sur le Pixel et vérifier le profil, le catalogue et les données du journal. [Procédure d’essai](docs/PROFIL_OBJECTIF.md).
- Relire l’audit des portions et l’étude Withings dans leurs PR respectives.

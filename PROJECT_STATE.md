# NutritionCoach — état du projet

Dernière mise à jour : 2026-11-08

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

- 2026-11-07. L’objectif calorique du journal reste modifiable et toute proposition issue du poids cible demande une confirmation. La tendance utilise des pesées datées et le poids issu d’un profil ancien reste disponible. [Méthode et essai sur téléphone](docs/PROFIL_OBJECTIF.md).

- 2026-11-07. [Profil intégré](https://github.com/fchautems/NutritionCoach/pull/4), puis [branche d’itération fusionnée dans `main`](https://github.com/fchautems/NutritionCoach/pull/1). L’APK V0.4 signée a été installée sur le Pixel, où le profil détaillé s’ouvre.

- 2026-11-07. [Interface 5 publiée sur le canal intégré](https://github.com/fchautems/NutritionCoach/commit/9880935899f74519473be856b951c3b58c1f7d99). L’objectif calorique du profil est proposé à partir de la dépense estimée, du poids cible et de l’échéance éventuelle, puis appliqué au journal sur confirmation. Les 49 fiches adoptent des portions propres à chaque aliment et trois états de pesée complémentaires. [Audit des portions](docs/CATALOGUE_PORTIONS.md).
- 2026-11-07. [Étude Withings](docs/WITHINGS.md) intégrée à la documentation. Le premier essai porte sur le transfert des pesées de Withings vers Health Connect sur le Pixel.

- 2026-11-08. Interface 6. Un seul bouton enregistre le profil. Si une cible calculée diffère de celle du journal, la confirmation affiche les deux valeurs et propose d’appliquer ou de garder l’objectif actuel.
- 2026-11-08. Aperçu Android V0.5. Import local en lecture seule du poids Withings via Health Connect sur Android 14 et plus, à l’ouverture après autorisation et sur demande. L’utilisateur choisit si la dernière pesée doit servir de poids actuel. La tendance rassemble les pesées importées et manuelles.

- 2026-11-08. Aperçu V0.6, interface 10 embarquée. Le journal lit les pas quotidiens des 30 derniers jours via Santé Connect sur Android 14 et plus, avec une autorisation de lecture dédiée. Google Fit et NOLATR sont lus séparément pour vérifier leurs transmissions sans additionner deux compteurs potentiellement superposés. Aucun pas n’est copié dans le journal local. Une nouvelle APK est nécessaire pour déclarer la permission.

- 2026-11-08. Aperçu V0.7, interface 11 embarquée. Le profil Android historique conserve son stockage privé, séparé de l’interface remplaçable. Les paramètres proposent une sauvegarde JSON du profil, de l’objectif calorique et des pesées manuelles. La restauration, disponible aussi au premier lancement, valide le fichier avant confirmation. Les mises à jour d’interface passent par le canal intégré. L’accès aux fichiers de sauvegarde utilise le sélecteur Android et nécessite la V0.7.

## Contrôles et limites

- La V0.1 est signée avec une clé debug générée par le runner CI. Une nouvelle APK signée avec une autre clé ne peut pas la remplacer en conservant son journal.
- Les 2 tests unitaires du calcul quotidien passent. APK d’aperçu compilée localement avec le SDK Android 35, archive et signature vérifiées. Identifiant vérifié : `ch.nutritioncoach.preview`.
- Vérification Gradle du lot 01 : tests unitaires, compilation de la variante `preview` et artefact réussis dans le [run 42370829278](https://github.com/fchautems/NutritionCoach/actions/runs/42370829278).
- L’aperçu V0.2 est distinct de la V0.1 et ouvre un journal indépendant. L’interface et les scénarios d’installation, d’ajout et de navigation restent à vérifier sur téléphone.

- V0.3 : cinq tests JUnit réussis, parcours de l’interface vérifié avec le pont Android simulé et stockage Java vérifié avec préférences simulées. Compilation locale, archive, ressources et certificat APK vérifiés. Même identifiant et certificat que la V0.2 distribuée.
- CI V0.3 réussie : tests, compilation Gradle et artefact dans le [run 42373121667](https://github.com/fchautems/NutritionCoach/actions/runs/42373121667). Le canal HTTPS sert l’interface 2 et le SHA-256 téléchargé correspond au manifeste.
- Interface 3 : manifeste HTTPS et empreinte vérifiés sur le canal public. Test DOM avec pont simulé dans les deux sens, 2 → 3 → 2, avec le même profil, le même objectif, une entrée historique et une saisie manuelle de la V2. La V2 relit aussi l’ajout fait par le catalogue de la V3. Le chargement de la V3 sur téléphone reste à confirmer.

- Profil V0.4 : neuf tests JUnit et quatre tests DOM réussis. [CI de la PR #4 réussie](https://github.com/fchautems/NutritionCoach/actions/runs/42378225698). Compilation locale avec le SDK Android 35 et certificat vérifié identique à celui de l’aperçu V0.3 distribué. L’ouverture du profil détaillé sur le Pixel a été confirmée. Le scénario complet de conservation du journal reste à vérifier.

- Interface 5 : six tests DOM réussis, y compris maintien de l’ancien journal, confirmation de l’objectif, pâtes sèches et cuites, et saisie décimale. Le pont Android existant est utilisé.

- Interface 6 et V0.5. Sept tests DOM et dix tests JUnit réussis. APK compilée avec le SDK 35. L’identifiant, le certificat de l’aperçu V0.4 et les permissions de lecture sont vérifiés. L’utilisateur confirme que l’import a abouti sur son Pixel. L’attribution des mesures dans le cas d’une balance partagée reste à vérifier.

- 2026-11-08. Le pilote sur Pixel importe un historique ancien, mais des pesées plus récentes manquent. L’interface 7 distingue la dernière pesée importée de la dernière pesée enregistrée dans Withings. Les sept tests d’interface passent, y compris le tri d’un historique reçu dans le désordre.

- 2026-11-08. Le lecteur Android a été testé avec Santé Connect simulé et un historique réparti sur plusieurs pages. Une pesée récente de source Withings est lue avec ou sans l’autorisation d’historique. Une pesée récente d’une autre source est exclue par le filtre prévu. Il reste à identifier les données écrites sur le Pixel, auxquelles le test n’a pas accès.

- 2026-11-08. L’interface 8 ajoute un diagnostic Withings dans les paramètres, compatible avec le pont de l’APK V0.5. Il distingue une nouvelle lecture de Santé Connect de l’historique importé. Huit tests d’interface passent, dont une lecture vide malgré l’historique, une lecture sans ajout et l’arrivée d’une pesée récente. Le pont expose la source Withings sans identifiant de profil ni détail sur le lot lu.

- 2026-11-08. Sur le Pixel, le diagnostic de l’interface 8 indique que Santé Connect renvoie des pesées Withings sans nouvel ajout. L’historique local contient 243 mesures et s’arrête au 1er février 2026. Une pesée du 7 novembre est visible dans Withings. Sur le Pixel, Santé Connect affiche la même pesée Withings du 1er février et aucune entrée de poids pour novembre, alors que la pesée du 7 novembre est visible dans Withings. La pesée récente est absente en amont de l’import NutritionCoach. L’onglet Accès du poids sur le Pixel inclut Withings dans les applications autorisées à écrire. Après réautorisation de l’export depuis Withings, l’utilisateur indique que la dernière mesure visible reste celle du 1er février. L’envoi d’une nouvelle mesure après la reconnexion et l’attribution des mesures importées à un profil restent à vérifier.

- 2026-11-08. L’interface 9 permet d’ouvrir la liste complète des pesées Withings importées depuis le profil et le diagnostic, de la plus récente à la plus ancienne. Neuf tests d’interface passent, dont l’affichage et le tri de 243 pesées reçues dans le désordre. Une capture du Pixel confirme que la liste complète s’ouvre et que la première ligne reste le 1er février 2026, avec 243 pesées enregistrées.

- 2026-11-08. L’interface 9 n’est pas apparue sur le Pixel au premier essai. Le manifeste et le fichier HTML ont été téléchargés en HTTP 200, sans redirection, depuis l’URL utilisée par l’APK. Le protocole 1 et l’empreinte SHA-256 concordent. Une capture du Pixel confirme ensuite que l’interface 9 est active et que la vérification indique que l’interface est à jour. Le message d’import indique toujours qu’aucune nouvelle pesée Withings n’a été trouvée.

- 2026-11-08. V0.6 compilée localement avec le SDK 35. Dix tests JUnit et onze tests DOM réussis. L’APK d’aperçu conserve l’identifiant Android et le certificat de la V0.5. Les tests DOM simulent l’autorisation, des réponses distinctes pour Fit et NOLATR, l’absence de transmission et le changement de jour. L’app NOLATR pour Android est identifiée, mais son export des pas vers Santé Connect reste inconnu. [Fiche NOLATR](https://play.google.com/store/apps/details?id=com.nolatr.ring) · [Synchronisation Fit](https://support.google.com/fit/answer/12830119?hl=fr) · [Lecture des pas](https://developer.android.com/health-and-fitness/health-connect/features/steps)

- 2026-11-08. Sur le téléphone cible, l’utilisateur confirme l’affichage des pas NOLATR dans la V0.6 après la synchronisation depuis NOLATR. L’envoi automatique en arrière-plan reste à vérifier.

- 2026-11-08. V0.7 compilée avec le SDK 35. Douze tests JUnit et treize tests DOM passent. Ils couvrent la reprise et la restauration d’un profil ancien et d’un profil détaillé, l’objectif calorique, la conservation du journal, le rejet de sauvegardes invalides et les accès depuis les paramètres et le premier lancement. L’identifiant et la signature de la V0.6 sont conservés. L’utilisateur confirme avoir installé la V0.7 sur téléphone. La conservation effective des données, le sélecteur de fichiers et la restauration restent à vérifier. Le fichier de sauvegarde contient des données personnelles en clair. La sauvegarde Android reste désactivée.

## Prochaine action

- Sur le téléphone où la V0.7 est installée, vérifier la reprise du profil et de l’objectif, puis créer une sauvegarde dans Téléchargements et vérifier sa restauration depuis les paramètres. Refaire le test de restauration sur une installation de test vierge avant toute désinstallation de l’app utilisée.
- Après une nouvelle marche, vérifier l’envoi automatique des pas en laissant NOLATR en arrière-plan.
- Le diagnostic Withings est mis en pause.

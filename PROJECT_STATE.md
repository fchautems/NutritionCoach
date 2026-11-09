# NutritionCoach — état du projet

Dernière mise à jour : 2026-11-09

## État vérifié

- 2026-11-09. [Interface 13 publiée sur le canal intégré](https://github.com/fchautems/NutritionCoach/commit/6dab1aaedac009be4e78df3d9d813255428709af) après fusion de la [PR #15](https://github.com/fchautems/NutritionCoach/pull/15). Elle passe à 131 aliments hors ligne et conserve les écrans des pas et de la sauvegarde selon le pont Android installé. Elle s’applique avec l’APK existante.

- 2026-11-09. L’utilisateur confirme que l’interface 12 fonctionne sur son Pixel et donne la priorité à un catalogue nettement plus large et à l’usage quotidien.

- 2026-11-08. [Interface 12 publiée sur le canal intégré](https://github.com/fchautems/NutritionCoach/commit/440000305825cc0a165904e01ddc3b7eb92d7ef2) après fusion de la PR #7. La livraison assemble les aliments récents et les écrans des pas et de la sauvegarde du profil à partir de l’interface de la V0.7. Les fonctions natives s’affichent selon les capacités du pont Android installé. Le canal utilise le protocole 1 et dépasse les versions embarquées 10 et 11. Les PR Android #5 et #6 suivent leur revue séparée.

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

- 2026-11-09. Après un ajout depuis le catalogue ou le formulaire manuel, une confirmation apparaît cinq secondes en bas de la fenêtre. Elle indique l’aliment et sa portion pour le catalogue, ou le nom et les kcal enregistrées pour la saisie manuelle. Elle reste visible quelle que soit la position dans la liste, laisse passer les appuis et ne prend pas le focus. Une annulation ou un refus du journal n’affiche pas de confirmation. Le changement utilise le protocole 1.

- 2026-11-09. Le passage de coordination quotidien à 18 h 30, heure suisse, vérifie et publie les lots d’interface prêts sur le canal intégré. La tâche horaire prépare les changements en PR. Une diffusion exige une vérification du lot final et de la conservation des données. Les préversions Android suivent leur revue séparée.

- 2026-11-09. Extension du catalogue hors ligne à 131 fiches avec 82 aliments supplémentaires. Les catégories « Plats préparés », « Boissons » et « Sauces et en-cas » accélèrent le choix sans recherche. Les libellés, valeurs et portions des 49 fiches historiques sont préservés. [Source, unités et états de pesée](docs/CATALOGUE_PORTIONS.md#extension-du-catalogue-courant-du-9-novembre-2026).

- 2026-11-08. Activation de la tâche horaire d’amélioration. Chaque passage reprend la branche par défaut, vérifie les PR et le canal diffusé, puis propose les changements vérifiés sur une branche `auto/nutritioncoach-*` via une PR. La fusion et la diffusion suivent le flux de coordination.
- 2026-11-08. Après avoir choisi un repas, « Aliments récents » propose jusqu’à trois aliments distincts reconnus dans le journal, par ordre d’ajout. Chaque raccourci reprend l’état de pesée et la dernière portion, modifiables avant validation. La reconnaissance utilise le libellé du catalogue et les kcal enregistrées. Les anciens formats et les saisies libres sans correspondance restent visibles dans le journal. Le pont, le stockage et les valeurs OSAV restent identiques.

- 2026-11-08. La conservation du profil, de l’objectif et du journal est un critère de livraison. Utiliser le canal intégré pour les changements d’interface. Si une APK est nécessaire, vérifier l’identifiant, le certificat et le code de version, puis tester l’installation par-dessus la version distribuée avec des données existantes. V0.5 et V0.6 ont le même identifiant d’aperçu et le même certificat. Les deux APK désactivent la sauvegarde Android. Avant toute procédure nécessitant une désinstallation, livrer et vérifier un export et une restauration du profil, de l’objectif et du journal.

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

## Contrôles et limites

- 2026-11-09. Confirmation d’ajout. Les 13 tests DOM des sources passent. Les tests ciblés échouent avant leurs correctifs et couvrent le refus d’enregistrement, la conservation du brouillon manuel, la confirmation et son effacement. Les 18 tests du candidat issu du HTML 13 diffusé passent en Europe/Zurich et America/Los_Angeles. Les 22 tests du candidat combiné avec les PR #11 et #19 passent en Europe/Zurich après assemblage de leur modification commune du bloc de résultats. Le retour hors ligne 13 → candidat → 13 relit un ajout du catalogue et une saisie manuelle. Le journal antérieur, le profil, l’objectif et les pesées sont conservés avec pont simulé. Chromium ne démarre pas dans cet environnement et Gradle Android relève de la CI.

- 2026-11-09. Interface 13. La [CI Android de la PR #15](https://github.com/fchautems/NutritionCoach/actions/runs/42603582833) et les 11 tests DOM des sources passent. Les 16 tests du candidat assemblé sur l’interface 12 passent en Europe/Zurich et America/Los_Angeles. Le test avec pont simulé vérifie la relecture hors ligne d’une saisie du nouveau catalogue par la 12 ainsi que la conservation du journal, du profil, de l’objectif et des pesées. Après expiration du cache GitHub, les URL HTTPS utilisées par l’APK servent le manifeste 13 et le HTML correspondant, dont le SHA-256 concorde. L’installation sur le Pixel reste à confirmer.

- 2026-11-09. Les 83 valeurs ajoutées, dont les deux états du riz complet, concordent avec le classeur OSAV V7.1. Les 11 tests DOM des sources et les 16 tests du candidat dérivé de l’interface 12 passent. Le retour à la 12 relit une saisie du nouveau catalogue et conserve le journal, le profil, l’objectif et les pesées avec pont simulé. Le toucher sur Pixel reste à vérifier après diffusion. Les boissons ajoutées se pèsent en grammes et les plats composés restent des estimations génériques.

- 2026-11-08. Interface 12. Les 14 tests DOM passent avec le HTML diffusé, dont les pas, les entrées récentes et la sauvegarde/restauration via pont simulé. Deux contrôles de relecture hors ligne couvrent les passages 9 → 12 → 9 et 11 → 12 → 11 avec conservation du journal, du profil, de l’objectif et des pesées. Le manifeste et le HTML sont servis en HTTP 200 sans redirection par l’URL utilisée par l’APK. L’empreinte SHA-256 concorde. Le lancement de Chromium tactile est bloqué par l’environnement. Une capture du Pixel confirme que l’interface 12 est active, que les paramètres la signalent à jour et que les commandes de sauvegarde et de restauration sont visibles. La capture ne valide ni leur exécution ni la reprise d’une portion.

- 2026-11-08. Le manifeste HTTPS du canal indique l’interface 9, protocole 1. Le SHA-256 du HTML correspond au manifeste et les octets correspondent à l’interface de départ sur `main`. Dix tests DOM passent avec les récents, dont un test sur le pont V0.3 qui vérifie le tri, la déduplication, l’état cuit, l’annulation, l’ajout et la suppression. Un contrôle hors ligne avec pont simulé valide le passage interface 9 → candidate → 9 avec le même profil, objectif et historique. L’application locale du changement sur la branche de la PR #6 réussit avec ses 14 tests DOM, y compris les pas et la sauvegarde. Le rendu et le toucher restent à vérifier sur le Pixel. Les tests Android Gradle relèvent de la CI, le SDK étant absent de cet environnement.

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

## Prochaine action

- Après intégration, vérifier sur Pixel la confirmation et son annonce TalkBack au bas d’une longue liste, puis essayer une saisie manuelle avec le clavier affiché.

- Sur le Pixel, appliquer l’interface 13 depuis « Paramètres → Vérifier les mises à jour », puis essayer « Plats préparés → Lasagnes → portion » et chercher le riz complet cuit et sec.

- Sur le Pixel, ouvrir un repas, reprendre une portion récente, l’ajuster et confirmer que le profil et le journal sont accessibles hors ligne. Vérifier aussi les pas et l’exécution de la sauvegarde et de la restauration sur V0.7.
- Withings en pause. À la reprise, tester une pesée prise après la reconnexion, d’abord dans Withings puis dans Santé Connect, et vérifier la confirmation des calories. [Parcours Withings](docs/WITHINGS.md#pilote-android-v05).

# Import des pesées Withings

Étude de faisabilité du 7 novembre 2026 · Base `iteration/01-aujourdhui` (`296cf34`)

**Décision proposée.** Tester d’abord **Withings → Health Connect → NutritionCoach** sur le Pixel. Si une pesée arrive dans Health Connect, faire un import local du poids en lecture seule, à l’ouverture et sur demande. Reconsidérer l’API directe si l’historique exporté ou le délai constaté est insuffisant.

## Pilote Android V0.5

1. Dans **Withings → Profil → Réglages → Exporter les données de santé vers Health Connect**, autorise l’écriture du poids.
2. Dans **NutritionCoach → Paramètres → Importer les pesées Withings**, accorde la lecture du poids. Accorde aussi l’historique si Android le propose et si tu veux les anciennes pesées disponibles.
3. Dans **Modifier mon profil**, vérifie la date et le poids de la dernière pesée. **Utiliser cette pesée** renseigne le poids actuel. **Enregistrer** confirme le profil et permet de revoir l’objectif calorique.

Le pilote utilise l’API native d’Android 14 et plus. Après autorisation, il relit les pesées Withings à l’ouverture ou sur demande. Il prend les trente derniers jours, ou tout l’historique accessible quand la permission correspondante est accordée. Les valeurs lues sont copiées localement avec leur date, leur heure et leur provenance. Le profil affiche les trente dernières.

Un second import rapproche les mesures par identifiant et par instant et poids identiques. Une saisie manuelle est conservée séparément. Pour la tendance, une journée compte une fois et une saisie manuelle du jour est prioritaire. Utiliser un poids importé ne crée pas de saisie manuelle supplémentaire. Retirer la permission Health Connect arrête les lectures suivantes. Les copies déjà importées restent dans l’aperçu.

## Socle au moment de l’étude

[`JournalStore`][store] conserve un poids de profil en kg (`profile_v1.weight`, dans les préférences privées `nutrition_coach`). Il est remplacé à chaque modification, sans date ni historique. [`MainActivity`][activity] expose `NutritionHost.state()` et `saveProfile()` à la WebView. L’[interface 3 publiée][ui] utilise ce même pont. Le [manifeste][manifest] a la permission Internet pour le canal de mise à jour natif. La WebView bloque ses requêtes réseau. Depuis cette étude, la V0.4 conserve aussi des pesées manuelles datées à raison d’une valeur par jour. L’intégration Health Connect demande un APK signé avec la permission native. Pour le pilote, garder l’import et son diagnostic côté Android, puis limiter le pont aux champs nécessaires à l’affichage en conservant la compatibilité.

## Comparaison

| | Withings et Health Connect | API Withings directe |
|---|---|---|
| Configuration | Balance associée au compte Withings. Dans **Withings → Profil → Réglages → Exporter les données de santé vers Health Connect**, autoriser Withings à écrire **Poids**, puis autoriser NutritionCoach à lire **Poids**. Health Connect est intégré à Android 14 et plus, disponible sur Google Play pour Android 9 à 13. [Withings][export] · [Android][availability] | Même association de la balance. Connexion et consentement Withings dans le navigateur. Le développeur enregistre l’application et sa redirection OAuth. [Configuration][setup] · [OAuth][oauth] |
| Données et permissions | `READ_WEIGHT` lit les `WeightRecord` écrits par Withings, avec date, kg et provenance. La masse grasse ou osseuse demande des permissions séparées et dépend de ce que Withings exporte effectivement sur le Pixel. La liste complète par modèle est inconnue. [Types][types] · [Métadonnées][metadata] · [Export][export] | Le scope `user.metrics` couvre le poids et d’autres mesures selon l’appareil et le plan, par exemple la masse grasse et la masse musculaire. Demander à `Measure – Getmeas` uniquement le type `1` (poids) et la catégorie `1` (mesures). Activité et sommeil relèvent de `user.activity`. [Scopes][scopes] · [Référence][getmeas] · [Plans][plans] |
| Historique | Par défaut, lecture depuis 30 jours **avant le premier accord**. `READ_HEALTH_DATA_HISTORY` lève cette borne sur les appareils compatibles si l’utilisateur l’autorise. Cela concerne les pesées déjà dans Health Connect. Le volume d’anciennes pesées que Withings y écrit à la connexion est inconnu. [Lecture][read] · [Export][export] | Historique synchronisé sur le compte Withings, sans la borne Android de 30 jours. Lire par plage `startdate` / `enddate` et paginer avec `more` / `offset`. [OAuth][oauth] · [Référence][getmeas] |
| Nouvelles pesées | Lecture locale, même hors ligne dès que Withings a écrit la pesée. Délai et export lorsque Withings est fermé à tester. La lecture par NutritionCoach en arrière-plan exige une permission et une fonction distinctes. [Synchronisation Android][sync] · [Export][export] | Après synchronisation vers le cloud Withings, relever ajouts et corrections avec `lastupdate`. Un webhook optionnel avertit un serveur joignable, qui récupère ensuite la mesure. [Référence][getmeas] · [Notifications][notify] |
| Serveur et confidentialité | Pas de serveur ni de compte NutritionCoach. Withings conserve sa synchronisation habituelle vers son cloud. La copie NutritionCoach reste sur le téléphone. Prévoir révocation et effacement des copies. [Withings][publicapi] · [Stockage Android][local] | Service sécurisé pour l’échange et le renouvellement OAuth avec `client_secret`. Garder secret et jetons côté serveur. Transmettre au téléphone uniquement le poids et ses métadonnées utiles, sans conserver d’historique sur le serveur. [OAuth][oauth] · [Jetons][tokens] |
| Coûts | Pas d’hébergement. Withings+ est requis pour l’export avec les comptes créés **à partir du 13 octobre 2026**. Les comptes antérieurs conservent l’export sans abonnement. [Règle Health Connect][hcplus] | **Start for Free** côté développeur, 10 utilisateurs actifs avant revue, 120 requêtes/minute par application. Les comptes créés **après le 12 octobre 2026** exigent aussi Withings+. Enterprise est sur devis et peut lever cette condition pour certains usages. Prévoir hébergement et maintenance. [Plans][plans] · [Limite][notify] |

## Import proposé

- Stocker séparément du profil chaque mesure avec son instant en UTC, le décalage local si fourni, le poids en kg et l’origine (`manuelle` ou `Health Connect / Withings`). Conserver le paquet source Android, qui identifie l’app ayant écrit la pesée. Conserver le poids historique du profil comme valeur sans date et afficher la dernière pesée datée lorsqu’elle existe. [Métadonnées][metadata]
- Lire les mesures brutes par pages, filtrées sur le paquet Withings constaté sur le Pixel. Insérer ou actualiser par clé unique `(Health Connect, paquet, metadata.id)` dans une transaction. Pour une synchronisation incrémentale, obtenir le jeton de changements avant la première lecture, l’avancer après écriture et relire la plage autorisée s’il expire. Si Withings recrée une mesure avec un autre ID, même heure et même poids déclenchent un signalement. La révocation arrête les lectures. [Lecture][read] · [Synchronisation][sync]
- Pour un doublon manuel présumé, proposer une validation si l’écart est de cinq minutes et 0,1 kg au maximum. Avec une saisie datée sans heure, utiliser le même jour et le même seuil de poids. Ces seuils sont à essayer. Après confirmation, afficher un point avec la date et le poids importés et garder la saisie manuelle liée. Laisser « Garder les deux » pour des pesées distinctes.

## Essai Pixel

1. **Avec l’installation actuelle**, activer l’export du poids et vérifier l’éventuelle demande Withings+. Dans **Health Connect → Données et accès → Mesures corporelles → Poids → Voir toutes les entrées**, chercher une pesée récente et une de plus de 30 jours connues de Withings. Relever la plus ancienne date exportée. Le guide Withings général mentionne quatre mesures minimum. Son application au poids dans Health Connect reste à vérifier. [Parcours Android][browse] · [Guide Withings][partner]
2. Se peser avec Withings hors du premier plan et le bon profil sur la balance. Vérifier le délai, la date, le poids et la source dans Health Connect. Si la pesée manque après dix minutes, ouvrir Withings, vérifier qu’elle y apparaît et recontrôler Health Connect. Les dix minutes servent de repère de test. [Synchronisation balance][scale]
3. **Avec un futur APK**, comparer la lecture avec et sans permission d’historique si une pesée ancienne est visible dans Health Connect. Importer deux fois et vérifier qu’une seule mesure est enregistrée avec sa date, son poids et son origine. Confirmer un doublon manuel, garder une pesée distincte du même jour, puis révoquer la permission et vérifier que le journal reste accessible. [Tests Android][tests]

[store]: ../app/src/main/java/ch/nutritioncoach/JournalStore.java
[activity]: ../app/src/main/java/ch/nutritioncoach/MainActivity.java
[ui]: https://github.com/fchautems/NutritionCoach/blob/ui-channel/ui-3.html
[manifest]: ../app/src/main/AndroidManifest.xml
[export]: https://support.withings.com/hc/en-us/articles/27322856325905-Partner-Apps-Health-Connect-Exporting-Withings-data-into-Health-Connect
[availability]: https://developer.android.com/health-and-fitness/health-connect/availability
[setup]: https://developer.withings.com/developer-guide/v3/integration-guide/public-health-data-api/developer-account/create-your-accesses-no-medical-cloud/
[oauth]: https://developer.withings.com/developer-guide/v3/integration-guide/public-health-data-api/get-access/oauth-web-flow/
[types]: https://developer.android.com/health-and-fitness/health-connect/data-types
[metadata]: https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/metadata/Metadata
[scopes]: https://developer.withings.com/developer-guide/v3/integration-guide/public-health-data-api/get-access/oauth-authorization-url/
[getmeas]: https://developer.withings.com/api-reference/
[read]: https://developer.android.com/health-and-fitness/health-connect/read-data
[sync]: https://developer.android.com/health-and-fitness/health-connect/sync-data
[notify]: https://developer.withings.com/developer-guide/v3/integration-guide/public-health-data-api/data-api/notifications/notification-overview/
[publicapi]: https://developer.withings.com/developer-guide/v3/withings-solutions/app-to-app-solution/
[local]: https://support.google.com/android/answer/13770320?hl=fr
[tokens]: https://developer.withings.com/developer-guide/v3/integration-guide/public-health-data-api/get-access/access-and-refresh-tokens-no-recover/
[hcplus]: https://support.withings.com/hc/en-us/articles/23104637348241-Partner-Apps-What-is-Health-Connect
[plans]: https://developer.withings.com/developer-guide/v3/withings-solutions/withings-api-plans/
[browse]: https://support.google.com/android/answer/12201872?hl=fr
[partner]: https://support.withings.com/hc/en-us/articles/51884299827601-Partners-Exporting-from-the-Withings-App-to-Partner-Apps
[scale]: https://support.withings.com/hc/en-us/articles/51143979108625-Scales-No-Data-Syncing
[tests]: https://developer.android.com/health-and-fitness/health-connect/test/test-cases

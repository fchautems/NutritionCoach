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

## Aperçu V0.2

L’aperçu refait l’écran Aujourd’hui et ouvre la saisie manuelle à la demande. Voir les [spécifications et les lots suivants](docs/SPECIFICATIONS.md).

Construire avec `gradle :app:assemblePreview` ou récupérer l’APK dans les artefacts GitHub Actions. L’aperçu s’installe sous le nom **NutritionCoach Aperçu** et ouvre un journal indépendant. Garde la V0.1 installée pour conserver son journal.

## Aperçu V0.3

L’aperçu demande un profil local au premier lancement. Aujourd’hui affiche les trois repas et chacun ouvre sa saisie manuelle. Les anciennes entrées sans repas restent dans « Saisies précédentes ». Le profil se modifie dans les paramètres.

Le socle Android embarque `app/src/main/assets/nutritioncoach.html`. Il vérifie au lancement le canal HTTPS de la branche `ui-channel`. Le bouton de vérification et l’application immédiate sont aussi disponibles dans les paramètres. Une mise à jour de l’interface laisse le profil, le journal et l’objectif dans le stockage Android local.

Pour publier une interface, ajouter `ui-N.html` sur `ui-channel` et remplacer `channel.json` dans le même commit. Incrémenter `version` et renseigner le SHA-256 exact des octets. Conserver `protocol: 1` tant que le contrat du pont Android reste compatible. Une publication initiale V2 permet de vérifier la mise à jour depuis l’interface V1 embarquée.

L’APK d’aperçu distribuée et celle produite par un runner CI peuvent porter des signatures différentes. Pour mettre à jour l’aperçu V0.2 téléchargé dans ChatGPT, utiliser l’APK V0.3 distribuée avec le même certificat.

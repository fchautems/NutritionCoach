# Profil et objectif de poids

## Calculs et stockage

- La date de naissance est facultative. L’âge et l’anniversaire utilisent la date locale du téléphone. Le message s’affiche sur Aujourd’hui le jour et le mois de naissance.
- L’IMC divise le poids par la taille au carré. La fourchette d’aide pour les adultes transpose à leur taille le repère d’IMC de 18,5 à 24,9. L’utilisateur choisit son poids cible. [OMS](https://www.who.int/europe/news-room/fact-sheets/item/nutrition---maintaining-a-healthy-lifestyle)
- La dépense quotidienne part de Mifflin–St Jeor au repos. Trois facteurs approximatifs (1,4, 1,6 et 1,8) représentent les mouvements hors sport. Le sport ajoute un coût net moyen de 3 kcal/kg/h, pondéré par la durée hebdomadaire de la catégorie. L’estimation est arrondie à 50 kcal et donne une fourchette lorsque le profil ne sélectionne pas de coefficient féminin ou masculin. Les facteurs, durées représentatives et arrondis sont des choix de l’application. L’estimation est affichée séparément de l’objectif calorique du journal. [Mifflin–St Jeor](https://pubmed.ncbi.nlm.nih.gov/2305711/) · [FAO](https://www.fao.org/4/Y5686e/y5686e07.htm) · [Compendium des activités physiques](https://pacompendium.com/)
- Le rythme visé divise l’écart au poids cible par le nombre de semaines restantes. Une échéance paraît serrée au-delà de 1 kg/semaine en perte ou 0,5 kg/semaine en prise. Ce sont des seuils de signalement dans l’app. Le report propose une date calculée avec ces seuils. [NHS, repère en perte de poids](https://www.nhs.uk/live-well/healthy-weight/managing-your-weight/tips-to-help-you-lose-weight/)
- La première saisie du poids, un changement de poids ou la case « Enregistrer une pesée aujourd’hui » créent une mesure datée. La tendance compare les deux extrémités des 90 derniers jours si elles sont espacées d’au moins 7 jours. Une mesure par jour est retenue. Un ancien poids sans date reste utilisable sans créer de pesée historique.

## Proposition calorique dans l’interface 5

Le maintien reprend la dépense estimée. Pour un poids cible, l’application propose un premier ajustement de 250 kcal/j sans échéance. Avec une échéance future, elle approxime l’ajustement initial à partir de l’écart et du temps restant, en utilisant 7 700 kcal/kg. Cet ajustement est borné à 500 kcal/j en baisse et 300 kcal/j en hausse et la cible est arrondie à 50 kcal/j. Un calcul linéaire ne tient pas compte de l’adaptation de la dépense au fil du temps. La proposition sert de point de départ et l’échéance reste indicative. [NIDDK, limites du calcul linéaire](https://www.obesityresearch.nih.gov/health-information/professionals/diabetes-discoveries-practice/nih-body-weight-planner)

Les limites sont des choix de l’application. Pour une perte de poids, la proposition est masquée sous 1 500 kcal/j ou pour un poids cible sous le repère d’IMC adulte de 18,5. Une échéance atteinte ou passée demande une date future ou un champ vide. Si la dépense est une fourchette, l’application utilise son milieu et l’indique. Le profil doit être complet et adulte. [Repères cliniques pour les adultes](https://pmc.ncbi.nlm.nih.gov/articles/PMC5819889/)

Depuis l’interface 6, « Enregistrer » sauvegarde le profil. Si la cible calculée diffère de l’objectif du journal, une confirmation affiche l’objectif actuel et la proposition modifiable. « Appliquer au journal » valide le nouveau chiffre. « Garder l’objectif actuel » conserve celui qui était déjà utilisé.

## Tests

Pour compiler l’aperçu et exécuter les tests Android avec le SDK 35 et Gradle 8.10.2.

```sh
gradle --no-daemon :app:testDebugUnitTest :app:assemblePreview
```

Pour exécuter les tests DOM avec Node 22 ou plus et un pont Android simulé.

```sh
test_deps="$(mktemp -d)"
npm install --prefix "$test_deps" --no-package-lock jsdom@26.1.0
NODE_PATH="$test_deps/node_modules" node --test tests/profile-ui.test.cjs
```

## Essai sur téléphone

1. Dans NutritionCoach Aperçu V0.3, conserver un profil déjà renseigné et noter le prénom, le poids, l’objectif calorique et une saisie dans le journal. Idéalement vérifier aussi un aliment ajouté via le catalogue de l’interface 3.
2. Installer l’APK d’essai V0.4 par-dessus la V0.3 avec le même identifiant (`ch.nutritioncoach.preview`) et le même certificat. L’artefact de CI utilise une clé debug de runner et convient à une installation de test séparée sur un autre appareil ou profil Android. Pour vérifier une migration sur le téléphone existant, utiliser une APK signée avec le certificat de la V0.3 installée. En cas d’erreur de signature, conserver la V0.3 installée.
3. En mode avion, ouvrir l’aperçu et vérifier le journal, son objectif calorique et l’accès au catalogue. Ouvrir **Paramètres → Modifier mon profil**, vérifier les champs repris et l’IMC, puis saisir une date de naissance complète et répondre aux deux questions. Vérifier l’âge, la dépense estimée et son lien avec l’objectif calorique.
4. Choisir **Maintenir mon poids**, puis essayer un poids cible avec une échéance dans quelques jours. Vérifier le rythme et les deux actions. **Repousser l’échéance** modifie la date proposée et **Ajuster le poids cible** sélectionne le poids à éditer. Effacer l’échéance, enregistrer et rouvrir pour vérifier la persistance du poids cible seul.
5. Pour vérifier l’anniversaire, choisir provisoirement le jour et le mois actuels dans une année passée, enregistrer, fermer et relancer l’app. Vérifier le message, puis restaurer la date voulue. La tendance s’affiche quand deux pesées espacées d’au moins sept jours existent.

## Canal d’interface

Le pont V0.3 affiche les champs historiques avec cette interface et le protocole 1. Le pont V0.4 ajoute l’enregistrement des champs détaillés et des pesées dans les préférences Android. Il faut donc une nouvelle APK pour activer le profil complet. L’interface 4 embarquée prend la priorité sur les versions 2 et 3 déjà en cache. Le chat de coordination choisit le numéro de publication selon les autres lots et gère la diffusion.

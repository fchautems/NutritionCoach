# NutritionCoach · Spécifications produit

Version de travail du 7 novembre 2026

## 1. Le produit

NutritionCoach aide à noter ce qu’on mange et à suivre ses habitudes sur Android. L’écran d’accueil répond à deux questions dès l’ouverture : « Où en suis-je aujourd’hui ? » et « Comment ajouter ce que je viens de manger ? ». Le journal et ses fonctions de base sont utilisables hors ligne et sans compte.

Le POC V0.1 a été essayé avec succès sur téléphone. La cible est une APK Android installée une fois, puis des itérations courantes déployées dans l’app sans téléchargement manuel d’APK. L’objectif de la V0.2 est de rendre le journal plus agréable à consulter et la saisie moins encombrante. Les calories restent saisies à la main pendant cette première itération.

## 2. Profil initial et écran Aujourd’hui

Au premier lancement de la V0.3, le formulaire demande le prénom, le sexe, la taille en cm et le poids en kg. Pour le sexe, « Je préfère ne pas préciser » est un choix valide. Le poids accepte la virgule décimale. Le profil est enregistré sur le téléphone et peut être modifié dans les paramètres. Après enregistrement, l’app ouvre le jour courant. Les ouvertures suivantes vont directement au journal. Withings viendra dans un lot distinct.

| Zone | Comportement |
|---|---|
| En-tête | Nom de l’app discret, titre « Aujourd’hui » et date en français. Les flèches changent de jour. Pour une autre date, « Revenir à aujourd’hui » est proposé. |
| Bilan | Total en kcal au premier plan, objectif accessible via « Modifier », barre bornée visuellement et solde écrit en toutes lettres. À l’objectif exact, afficher « Objectif atteint ». Au-dessus, garder un ton et une couleur neutres. |
| Action principale | « Ajouter un aliment » propose Petit-déjeuner, Midi ou Soir. Chaque ligne de repas ouvre directement la recherche et le catalogue, avec saisie manuelle en secours. |
| Journal | Trois repas sélectionnables sur le jour affiché. Chaque repas liste ses aliments du plus récent au plus ancien et permet de les supprimer après confirmation. Les anciennes saisies sans type de repas restent visibles à part et comptent dans le total. |

La V0.2 d’aperçu utilise l’ivoire, le vert forêt et la menthe. La V0.3 associe un bleu denim adouci, un fond crème et un accent abricot. L’icône Android reprend la fourchette au centre d’un cercle fin qui évoque une assiette. Un léger relief souligne les boutons et les tuiles interactives. Les illustrations restent petites et servent de repères. Une seule carte dominante présente le bilan, la date et l’objectif restent accessibles. Les lignes du journal partagent une surface plutôt que de multiplier les encadrés. Cibles tactiles d’au moins 48 dp, libellés pour les icônes, contenu défilable et tailles de texte adaptables.

### Critères d’acceptation

- Au premier lancement, le profil précède le journal. Après enregistrement, les trois repas du jour sont accessibles, puis le profil est conservé au prochain lancement.
- Avec 258 kcal et un objectif de 2’000, l’écran affiche 258 kcal et 1’742 kcal restantes. À 2’000, il indique que l’objectif est atteint. Au-delà, il affiche l’écart et la barre reste pleine.
- Depuis un jour passé, un ajout apparaît uniquement à cette date. Le total et la liste se rafraîchissent ensemble. Un changement de jour puis un retour retrouvent les mêmes données.
- Un nom vide ou de plus de 80 caractères et une valeur hors de 1 à 10’000 kcal affichent une erreur dans le formulaire. La saisie reste disponible pour correction. Annuler ne crée aucune entrée.
- Après fermeture et réouverture, les entrées et l’objectif sont toujours disponibles. Une installation neuve démarre avec un journal vide. L’aperçu V0.3 distribué utilise le même identifiant et le même certificat que l’aperçu V0.2 distribué.
- Sur téléphone étroit et avec une taille de texte agrandie, le total, l’action et les entrées restent lisibles et accessibles en défilant.

## 3. Cible · Ajouter un repas en quatre actions

1. **Choisir le repas.** Petit-déjeuner, midi, soir ou collation, avec de grandes cibles et des libellés.
2. **Choisir l’aliment.** Récents et favoris en tête. Les catégories aident à parcourir la liste, par exemple « Viandes » puis « Poulet ». Les catégories sont de grandes tuiles illustrées et libellées. Exemples de repères visuels : steak pour Viandes, poisson pour Poissons, carotte et brocoli pour Légumes. Utiliser un style d’illustration cohérent et des ressources disponibles hors ligne. Recherche et saisie manuelle restent possibles.
3. **Ajuster la portion.** Portions usuelles ou quantité personnalisée. Afficher l’estimation de kcal et permettre de la corriger.
4. **Valider.** Afficher le total estimé du repas et le jour avant enregistrement. Un repas peut contenir plusieurs aliments.

La photo est une entrée facultative vers le même parcours. Les aliments, les portions et les kcal proposés restent modifiables avant validation. Un échec d’analyse ramène à la saisie par liste ou à la saisie manuelle.

## 4. Petites livraisons, chacune testable

| Lot | Ce qu’on pourra tester | Critère de fin |
|---|---|---|
| 01 · Aujourd’hui | Bilan, journal compact et ajout manuel à la demande | Le scénario V0.2 ci-dessus passe. |
| Priorité avant le lot 02 · Mise à jour | L’app récupère une nouvelle version de l’interface au démarrage et propose « Vérifier les mises à jour » dans les paramètres. | Avec une seule APK installée, la version suivante de l’interface apparaît et les entrées restent accessibles. En mode avion ou si le paquet est invalide, la dernière version utilisable s’ouvre. |
| 02a · Profil initial et repas | Profil local, jour courant et saisie manuelle par repas | Enregistrer un profil, ajouter un aliment à Midi, puis retrouver le profil et le repas après réouverture. |
| 02b · Catalogue et portions | Huit catégories illustrées, recherche et 49 aliments hors ligne. Portion proposée ou saisie en grammes, avec estimation avant validation. | Choisir 180 g de blanc de poulet poêlé. Le journal ajoute 230 kcal à Midi et conserve les entrées existantes. |
| 03 · Récents et favoris | Réutiliser un aliment sans parcourir le catalogue | Un aliment récemment utilisé ou mis en favori peut être repris et sa portion corrigée. |
| 04 · Progression | Poids visé, objectifs modifiables, pesées et historique | Une pesée manuelle apparaît dans la courbe et un changement d’objectif se reflète au bon endroit. |
| 05 · Activité et menus | Suivre les pas et les exercices, proposer des idées de menus | Une activité enregistrée apparaît dans le suivi. Les idées de menus restent facultatives. |
| 06 · Coaching et habitudes | Encouragements, étoiles puis badges, défis ou jardin selon les retours | Les récompenses valorisent des habitudes choisies. Un jour manqué ou un total plus élevé ne retire aucun acquis. |
| 07 · Photo et Withings | Estimation photo corrigible et import de pesées avec consentement, en deux livraisons distinctes | Chaque intégration peut être utilisée séparément. Le journal manuel reste utilisable hors ligne. |

Chaque lot est livré dès qu’il est vérifiable, puis ajusté d’après le retour sur téléphone. L’ordre après le lot 03 est proposé et peut évoluer selon l’usage.

## 5. Architecture et distribution

- **Interface.** La V0.3 utilise un socle Android avec une interface web intégrée et une version de secours embarquée. Une branche GitHub publique dédiée sert les paquets d’interface versionnés. L’APK télécharge uniquement depuis ce canal HTTPS. Le paquet est contrôlé par version, protocole, taille et empreinte SHA-256 issue du manifeste du même canal. L’app prépare la mise à jour au lancement et l’applique au prochain démarrage, ou via l’action de mise à jour des paramètres, dont le libellé passe de « Vérifier les mises à jour » à « Appliquer la mise à jour » lorsque le fichier est prêt. Si l’interface ne confirme pas son chargement, l’app revient à la dernière version utilisable. La connexion entre l’interface et le journal reste locale et limitée aux fonctions nécessaires. [Contenu local Android](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content) · [GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages).
- **Moteur.** Garder les calculs séparés de l’interface. La cible calorique reste modifiable. Les estimations, leur provenance et leurs corrections doivent être distinguables.
- **Données.** Le stockage actuel utilise les préférences Android. Passer à une base locale versionnée (SQLite) quand les données de repas, de portions ou de progression le nécessitent, avec migration et export/import vérifiés. Les interfaces V2 et V3 utilisent le même protocole et lisent les mêmes champs Android. L’entrée V3 conserve les kcal validées et inclut la portion dans le libellé affiché par les deux versions. Aucun profil ni journal utilisateur dans le dépôt public.
- **Connexions.** Isoler photo/IA, pas et Withings. Toute transmission externe exige un choix explicite. Une intégration qui requiert un secret, comme l’échange OAuth Withings, passe par un service sécurisé et séparé. Les fournisseurs et le périmètre de synchronisation restent à décider.
- **APK.** L’aperçu actuel a un identifiant Android distinct. La première APK avec le nouveau socle inclura l’icône adaptative choisie. Les changements du socle, dont l’icône du lanceur et les permissions Android, passent par une mise à jour d’APK signée. Pour une mise à jour conservant le journal de la V0.1, il faut vérifier la compatibilité de signature ou fournir un chemin d’export/import avant de demander son remplacement. [Règles de mise à jour Android](https://developer.android.com/google/play/app-updates).

# Journal -- Supervision Équipements (ALTN72, 2026-27)

Groupe : Léo ESNAULT, Loïc DEIVASSAGAYAME

## README

Pour faire tourner le projet, il faut Java 21, Maven (ou le wrapper mvnw qui est fourni) et une base MySQL ou MariaDB sur le port 3306.

Le script SQL ne crée pas la base. Il faut donc la créer d'abord, puis lancer le script :

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ALTN72_projet_java CHARACTER SET utf8mb4"
```

```bash
mysql -u root -p ALTN72_projet_java < src/main/resources/script.sql
```

Les dates des incidents de démo sont calculées à partir de l'heure actuelle, donc relancer le script leur redonne l'âge prévu. Le mieux est de le faire juste avant une démo, parce que l'incident ouvert depuis 47 h passe orange une heure plus tard.

Pour démarrer l'application, on met le mot de passe de la base dans la variable SPRING_DATASOURCE_PASSWORD, puis :

```bash
./mvnw spring-boot:run
```

L'application est ensuite sur http://localhost:8080/login. Une fois connecté, la documentation de l'API se trouve sur http://localhost:8080/swagger-ui.html.

Application déployée : https://application-production-62dc.up.railway.app. Elle est hébergée sur Railway à partir du Dockerfile du dépôt, et sa base MySQL n'est pas exposée sur Internet.

Comptes de test (les mots de passe sont hachés dans le script) :
- Responsable d'exploitation : resp_exploitation / resp
- Agent de terrain : agent_terrain / agent

Limites connues :
- L'import crée de nouveaux équipements, mais il ne crée pas les stations et ne modifie pas les équipements existants.
- On n'a pas essayé d'ouvrir le fichier exporté dans Excel, on a seulement relu son contenu.
- Chart.js et Bootstrap viennent d'un CDN, donc il faut Internet pour avoir les graphiques et la mise en page.
- Dans l'API, les erreurs qui ne passent pas par notre contrôleur (403, 405, route inconnue) gardent le format standard de Spring Boot, en anglais.
- L'en-tête affiche le profil entre crochets, et sec:authorize n'est pas interprété.
- La fiche équipement n'a pas l'en-tête commun et affiche l'état EN_SERVICE en rouge.
- Le code Programmeur du TP est encore là, alors que sa table n'est pas dans script.sql.
- Sur la version déployée, les incidents de démo vieillissent : leurs niveaux d'attention changent après le chargement du script.
- La protection CSRF est désactivée.
- application.properties garde root comme mot de passe par défaut.
- Il n'y a pas de contrôle de version sur Incident. Si deux personnes agissent en même temps sur le même incident, la dernière écriture peut écraser l'autre, et même annuler une résolution.

## ARCHITECTURE

Dépôt : https://github.com/Loicdei/ALTN72_2026-27_ProjetJava

Schéma :

```
Navigateur ou client REST -> Spring Security -> Contrôleurs -> Services -> Repositories -> Base de données
                                                     |
                                                     v
                                     Vues Thymeleaf (pages) ou JSON (API)
```

On a gardé le découpage vu en cours :
- Les contrôleurs reçoivent les requêtes, valident les formulaires et choisissent la vue.
- Les services portent les règles métier.
- Les repositories accèdent à la base.
- modele contient les entités et les objets calculés, formulaire les données saisies, exception nos erreurs métier, et config la sécurité, l'horloge et la documentation de l'API.
- api regroupe le contrôleur REST, ses objets d'échange et son gestionnaire d'erreurs. Il appelle les mêmes services que les pages web, donc les règles sont les mêmes des deux côtés.
- Les graphiques sont dessinés dans le navigateur par static/js/statistiques.js, avec les données calculées par StatistiquesService.

## TESTS

Les tests se lancent avec cette commande. Pas besoin d'installer une base, ils utilisent H2 en mémoire :

```bash
./mvnw verify
```

Il y a 178 tests :
- IncidentServiceTest (18), EvaluationServiceTest (12), TableauDeBordServiceTest (8), StatistiquesServiceTest (5) et ImportEquipementsServiceTest (14) testent les règles métier avec des mocks et une horloge fixe.
- NiveauAttentionTest (11) teste les seuils de 48 h et 72 h, à la seconde près.
- IncidentControleurTest (26), EvaluationControleurTest (16), TableauDeBordControleurTest (11), StatistiquesControleurTest (6) et ImportExportControleurTest (12) testent les parcours HTTP avec MockMvc, sécurité comprise.
- IncidentApiControleurTest (23) teste l'API : réponses JSON, codes HTTP, droits par profil et documentation OpenAPI.
- EquipementFragiliteServiceTest (9) et EquipementFragileControleurTest (6) testent l'évolution « Équipement fragile » (voir la section dédiée).
- TpFilRougeApplicationTests (1) vérifie que l'application démarre.

Ce qui est couvert : F4 à F12, c'est-à-dire la déclaration, le détail, la modification, l'affectation, la résolution, l'historique, l'évaluation, le tableau de bord et l'API, avec les règles RG1 à RG8 et RG11. Le calcul des statistiques, la requête SQL native, l'export et l'import CSV sont testés aussi. Pour RG10, on vérifie qu'une opération refusée laisse les données inchangées.

Ce qui n'est pas couvert :
- RG9 : l'import refuse une référence déjà utilisée et ce refus est testé, mais la contrainte d'unicité de la base n'a pas de test à elle.
- script.sql et les requêtes sur un vrai serveur MySQL ou MariaDB, car les tests tournent sur H2 avec un schéma généré depuis les entités. On a seulement vérifié à la main, sur une instance MySQL 8.0.36 temporaire, que le script se charge, que les pages et l'API répondent, et qu'une affectation, une résolution, une évaluation, un export et un import passent.
- L'affichage dans un navigateur, dont la confirmation avant de résoudre un incident. F10, F11 et Swagger ont seulement été vérifiés à la main sur H2, et les graphiques sur MySQL. Pour les pages d'export et d'import, on a seulement contrôlé le HTML.
- Le message affiché pour un fichier de plus de 1 Mo, vérifié uniquement à la main avec un vrai envoi.
- Le code JavaScript des graphiques : les tests regardent les données envoyées à la page, pas le dessin.
- Deux actions simultanées sur le même incident (voir les limites connues).
- Le retour arrière d'une transaction après une vraie panne.

## CHOIX TECHNIQUES

### Droits par profil

Notre second profil est l'agent de terrain, celui qui signale les pannes.

| N° | Fonctionnalité | Responsable d'exploitation | Agent de terrain |
|---|---|---|---|
| F2–F3 | Consulter équipements et fiches | Lecture seule | Lecture seule |
| F4 | Déclarer un incident | Accès complet | Accès complet |
| F5–F6 | Consulter / modifier un incident | Accès complet | Accès complet |
| F7 | Affecter ou réaffecter un prestataire | Accès complet | Interdit |
| F8 | Déclarer un incident résolu | Accès complet | Interdit |
| F9 | Consulter l'historique | Lecture seule | Lecture seule |
| F10 | Évaluer un prestataire | Accès complet | Lecture seule (consultation des notes) |
| F11 | Tableau de bord | Accès complet | Interdit |
| F12 | API REST | Accès complet | Lecture seule |
| Lot alpha | Statistiques | Accès complet | Interdit |
| Lot alpha | Export CSV de l'historique | Accès complet | Accès complet |
| Lot alpha | Import CSV d'équipements | Accès complet | Interdit |

Les restrictions sont dans ConfigurationSecurite, pas seulement dans les vues. Si un agent appelle directement l'URL d'affectation, de résolution, d'évaluation, du tableau de bord, des statistiques ou de l'import, ou s'il essaie de modifier un incident par l'API, il reçoit une erreur 403. L'idée est que le responsable d'exploitation prend les décisions de suivi : c'est lui qui affecte, résout, évalue et supervise.

### Règles que nous avons précisées

F6 : seule la description se modifie, par les deux profils, tant que l'incident n'est pas résolu. L'équipement et la date d'ouverture restent fixes, parce que la durée et l'historique en dépendent.

F8 : on ne peut pas résoudre un incident sans prestataire. Le sujet ne l'impose pas, mais un tel incident ne pourrait jamais être évalué (RG8).

F10 : la note se saisit une seule fois et ne se modifie plus. On la voit sur le détail de l'incident et dans l'historique, et la moyenne de chaque prestataire est sur la page Prestataires.

F11 : le niveau d'attention n'est jamais enregistré, il est recalculé à chaque affichage par NiveauAttention. C'est le seul endroit où se trouvent les seuils de 48 h et 72 h. Les incidents sont triés du plus urgent au moins urgent, puis du plus ancien au plus récent. On peut filtrer par station, par type d'équipement et par niveau d'attention. Les compteurs ignorent le filtre par niveau, pour qu'on garde la répartition complète sous les yeux.

F12 : l'API permet de lister et de consulter les incidents, d'affecter ou réaffecter un prestataire et de déclarer un incident résolu. Elle renvoie des objets IncidentReponse plutôt que les entités, pour que son format ne dépende pas du modèle JPA. Elle utilise la session de l'application : sans connexion, l'appel est redirigé vers /login (code 302), comme le demande le sujet pour les pages et l'API. Une API publique répondrait plutôt 401. Le niveau d'attention y est visible pour les deux profils, seul l'écran du tableau de bord est réservé au responsable.

### Exceptions

On a choisi @ControllerAdvice, avec la classe GestionnaireExceptions. Les services lèvent nos exceptions sans rien savoir de HTTP, et la traduction se fait à un seul endroit :
- RessourceIntrouvableException donne une erreur 404.
- OperationIncidentInterditeException donne une erreur 409.
- DonneeInvalideException donne une erreur 400.
- ImportInvalideException donne une erreur 400. Elle est traitée dans ImportEquipementsControleur, parce que les erreurs doivent rester affichées sur le formulaire d'import.

Les messages sont en français et la page d'erreur n'affiche aucune trace technique. Les erreurs de saisie restent affichées sur le formulaire.

Pour l'API, GestionnaireExceptionsApi traduit les mêmes exceptions en JSON, au format ProblemDetail (application/problem+json). Il passe avant GestionnaireExceptions grâce à @Order et ne s'applique qu'au package api, donc les pages web gardent leur page d'erreur HTML.

On n'utilise ResponseEntity qu'à un seul endroit, ExportHistoriqueControleur, parce que le type du fichier et son nom de téléchargement passent par les en-têtes de la réponse. Ailleurs il n'apporterait rien : les pages renvoient le nom d'une vue, l'API renvoie ses objets avec le code 200 par défaut, et ProblemDetail porte déjà le code des erreurs.

### SOLID

Principes appliqués :
- Responsabilité unique : le contrôleur gère HTTP, le service les règles, GestionnaireExceptions les erreurs et les formulaires la validation. Une nouvelle règle de résolution ne touche donc que le service, et changer un seuil d'urgence ne touche que NiveauAttention. On l'a vu en ajoutant l'API : pas besoin de réécrire les règles d'affectation et de résolution, elle appelle le même service que les pages. L'export et l'import ont aussi chacun leur service, séparé de leur contrôleur.
- Inversion des dépendances : les services reçoivent leurs dépendances par le constructeur, dont Clock et les repositories. C'est ce qui permet de les tester avec une horloge fixe et des mocks.

Principes écartés :
- Ouvert/fermé : il n'y a que trois statuts, imposés par le sujet. Une hiérarchie de classes serait plus lourde que quelques conditions.
- Liskov : on n'a pas de hiérarchie métier. Le seul héritage, UtilisateurDetails extends User, ajoute le prénom et le profil sans changer le comportement.
- Ségrégation des interfaces : on n'écrit pas d'interface métier, et découper JpaRepository n'apporterait rien ici.

### Requêtes

On utilise des requêtes dérivées du nom de méthode, comme findByStatutOrderByDateHeureResolutionDesc.

On a aussi six requêtes JPQL, pour les cas qu'un nom de méthode n'exprime pas simplement :
- IncidentRepository.trouverIncidentsActifs a des filtres facultatifs, et ses « join fetch » évitent une requête par ligne du tableau de bord.
- PrestataireRepository.calculerNotesDesPrestataires calcule la moyenne, avec des jointures externes pour garder les prestataires sans évaluation.
- EquipementRepository.trouverTypes donne la liste des types pour le filtre du tableau de bord.
- IncidentRepository.trouverIncidents a un filtre de statut facultatif et charge d'un coup les données utiles à l'API.
- IncidentRepository.compterIncidentsParType regroupe les incidents par type pour les statistiques.
- IncidentRepository.trouverEquipementsAvecAuMoinsNIncidentsDepuis donne d'un coup les équipements fragiles, sans une requête par ligne du tableau de bord.

La seule requête SQL native est IncidentRepository.compterIncidentsParStation, qui compte les incidents actifs et résolus de chaque station.

Notre analyse critique :
- Ce qu'elle apporte : le SQL est direct et lisible, et les jointures externes qui partent de la station gardent les stations sans incident.
- Ses limites : les noms de tables et de colonnes ne sont pas contrôlés par le modèle. Si on renomme quelque chose dans les entités, la requête ne suit pas et l'erreur n'apparaît qu'à l'exécution. La valeur 'RESOLU' est écrite en dur et ne suit pas l'enum StatutIncident. Le résultat passe aussi par une interface de projection, moins directe qu'un objet construit en JPQL.
- Notre conclusion : JPQL aurait pu faire la même chose. Elle est en SQL parce que le sujet demande une requête native, pas parce que JPQL ne suffisait pas, et on garde JPQL pour le reste. Elle n'utilise que du SQL standard et a tourné sur H2 comme sur MySQL 8.0.36.

### @Transactional

Il est utilisé sur trois méthodes d'IncidentService :
- modifierDescription, pour contrôler le statut et écrire dans la même transaction.
- affecterPrestataire, car le prestataire et le statut changent ensemble.
- declarerResolu, car le statut et la date de résolution changent ensemble.

Il est aussi sur ImportEquipementsService.importerEquipements : plusieurs insertions doivent réussir ou échouer ensemble, pour qu'un import ne reste jamais à moitié fait (RG10).

declarerIncident n'en a pas besoin, car il ne fait qu'une seule écriture. EvaluationService.evaluerIntervention non plus : si deux saisies arrivent en même temps, c'est la contrainte d'unicité de la base qui tranche. Le contrôleur REST n'en ajoute aucun puisqu'il réutilise les méthodes d'IncidentService, et StatistiquesService ne fait que des lectures, où de petits écarts entre deux graphiques n'ont pas d'importance. ProgrammeurService en contient encore deux, qui viennent du TP.

@Transactional rend les changements atomiques, mais ne protège pas contre deux actions simultanées sur le même incident (voir les limites connues).

## LOT ALPHA

### Fonctionnalité transverse 1 : Visualisation des données (piste 2)

Besoin : le tableau de bord montre la situation à l'instant présent, mais pas les tendances. Le responsable d'exploitation doit aussi pouvoir repérer les stations et les types d'équipement les plus touchés, et comparer les prestataires.

Périmètre : une page /statistiques, réservée au responsable, avec cinq graphiques :
- les incidents ouverts par jour sur les 14 derniers jours ;
- les incidents actifs et résolus par station ;
- les incidents par type d'équipement ;
- la note moyenne des prestataires ;
- le délai moyen de résolution par prestataire, en heures.

Choix et essais :
- On a pris Chart.js, qui couvre ces graphiques sans outil de construction. Les calculs restent en Java, et le dessin est isolé dans static/js/statistiques.js.
- Les données viennent de trois sources selon le besoin : la requête SQL native pour les stations, JPQL pour les types et les notes, et un calcul en Java pour la tendance et les délais.
- La tendance affiche aussi les jours sans incident. La requête ne les renvoie pas, et sans eux la courbe serait trompeuse.
- La première version avait une courbe arrondie. On l'a remplacée par des segments droits, parce que l'arrondi laissait croire à des valeurs entre deux jours. On a aussi réduit le graphique en anneau, qui déformait sa rangée.
- Les prestataires sans note ou sans incident résolu n'apparaissent pas dans les graphiques concernés, pour ne pas les confondre avec une note ou un délai de 0.

Progression : réalisée le 5 novembre 2026, après le socle.

Ce qui fonctionne : les cinq graphiques s'affichent à partir de la base. StatistiquesServiceTest et StatistiquesControleurTest testent les calculs, les requêtes et les droits, et l'affichage a été vérifié à la main sur MySQL.

Reste à faire : pouvoir choisir la période de la tendance, et tester le dessin des graphiques, qui n'a pas de test automatique.

### Fonctionnalité transverse 2 : Import / export de données (piste 9)

Besoin : le responsable d'exploitation doit pouvoir sortir l'historique des incidents pour le retravailler dans un tableur, et ajouter plusieurs équipements d'un coup sans passer par la base.

Périmètre :
- un export CSV de l'historique des incidents résolus, depuis la page d'historique, pour les deux profils ;
- un import CSV de nouveaux équipements, depuis la liste des équipements, réservé au responsable, avec un fichier d'exemple à télécharger.

Choix et essais :
- On a utilisé Apache Commons CSV plutôt que de gérer nous-mêmes les guillemets et les séparateurs.
- Le fichier utilise le point-virgule et une marque UTF-8 en tête, pour qu'Excel en français sépare bien les colonnes et garde les accents. L'import accepte cette marque, et refuse un fichier qui n'est pas en UTF-8 avec un message qui explique quoi faire.
- L'import marche en tout ou rien : toutes les lignes sont vérifiées avant d'écrire, et toutes les erreurs s'affichent avec leur numéro de ligne, pour corriger le fichier en une fois.
- La contrainte d'unicité de la base reste le dernier filet si une même référence est créée pendant l'import.
- À l'export, un texte qui commence par =, +, -, @ reçoit une apostrophe devant, pour qu'un tableur ne l'exécute pas comme une formule.
- La première version traitait le fichier trop volumineux dans GestionnaireExceptions, mais cette méthode n'était jamais appelée. On l'a retirée, et le message est maintenant dans error.html, à côté de celui de l'erreur 403.

Progression : réalisée le 5 novembre 2026, après le socle et la fonctionnalité transverse 1.

Ce qui fonctionne : l'export et l'import, avec leurs refus. ImportEquipementsServiceTest et ImportExportControleurTest les testent, et un parcours complet a été vérifié à la main sur MySQL.

Reste à faire : importer aussi des stations, et essayer le fichier exporté dans Excel.

## Évolution séance finale

### Ce qui a été fait

Un équipement est maintenant repéré comme fragile s'il a eu au moins 3 incidents ouverts durant les 30 derniers jours, quel que soit leur statut.

Ça se voit à trois endroits :
- sur la fiche équipement, avec un badge « Équipement fragile » (detailEquipement.html) ;
- au tableau de bord, avec le même badge sur la ligne de chaque incident actif d'un équipement fragile (tableauDeBord.html) ;
- à la déclaration d'un incident, où le message de confirmation prévient que l'équipement est fragile (IncidentControleur).

Les fichiers principaux sont EquipementFragiliteService pour la règle, IncidentRepository pour la requête, TableauDeBordService et EquipementControleur pour l'affichage, et badgeFragile.html pour le badge, qui n'est écrit qu'une fois.

### Choix de conception et raccourci

La règle est dans EquipementFragiliteService (EF6). Le seuil de 3 et la fenêtre de 30 jours y sont deux constantes, et ils n'existent nulle part ailleurs (EF5).

La fragilité n'est pas enregistrée en base. Elle est recalculée à chaque consultation à partir de l'horloge injectée, donc la fenêtre glisse toute seule (EF4). La requête ne filtre pas sur le statut, ce qui fait compter aussi les incidents résolus (EF2), et elle garde les dates d'ouverture supérieures ou égales au début de la fenêtre (EF1).

Le tableau de bord récupère d'abord l'ensemble des équipements fragiles, puis regarde chaque ligne dans cet ensemble. Ça évite une requête par ligne.

Notre raccourci : estFragile utilise la même requête que le tableau de bord, puis cherche l'équipement dans le résultat. Une requête qui compte pour un seul équipement serait un peu plus légère, mais il n'y a ainsi qu'une seule requête à garder juste. Le texte du message de déclaration est aussi construit dans IncidentControleur : le contrôleur demande au service si l'équipement est fragile, mais ne décide de rien.

### Tests ajoutés

EquipementFragiliteServiceTest (9 tests) passe par la vraie requête avec une horloge fixe :
- 2 incidents dans la fenêtre : non fragile ;
- 3 incidents dans la fenêtre : fragile ;
- 3 incidents dont 1 résolu : fragile ;
- 1 incident sur les 3 ouvert il y a 30 jours et 1 minute : non fragile ;
- le même ouvert il y a 29 jours et 23 h 59 : fragile ;
- le même ouvert il y a exactement 30 jours : fragile ;
- 2 incidents récents et 5 vieux de 45 jours : non fragile ;
- l'équipement voisin n'est pas contaminé ;
- la fenêtre glisse quand l'heure de consultation avance.

EquipementFragileControleurTest (6 tests) passe par MockMvc :
- le badge apparaît sur la fiche d'un équipement fragile, et pas sur celle de son voisin ;
- au tableau de bord, seules les lignes d'un équipement fragile sont marquées, et aucun badge n'apparaît s'il n'y en a pas ;
- le message de confirmation contient « fragile » à la troisième déclaration, et pas avant.

### Limites connues

- Avec les données de démo, 3 équipements sur 4 sont déjà fragiles. EQ-LD-004 a 2 incidents : en déclarer un troisième permet de voir le message et le badge apparaître.
- La fragilité n'est pas renvoyée par l'API REST et ne sert pas de filtre au tableau de bord, car le complément ne le demande pas.
- On a vérifié à la main sur une instance MySQL 8.0.36 temporaire les badges et le message, mais seulement dans le HTML, pas à l'écran dans un navigateur.
- Sur la version déployée, on n'a contrôlé que le démarrage et l'absence d'erreur, sans refaire le parcours connecté.

## BILAN

Contribution de chacun : 

Loïc : F1 - F3, F10 - F12, déploiement, lots alpha, Evolution
Léo : F4 - F9

Plus grande difficulté et comment elle a été surmontée :

La connexion, tout au début du projet. On a eu du mal à la faire marcher avec les mots de passe hachés : les hash qu'on avait mis au départ dans script.sql ne fonctionnaient pas. On a fini par les remplacer par de nouveaux hash. Après ça, la connexion est passée.

On a eu deux autres difficultés :
- Faire correspondre les entités JPA et le script SQL. On écrivait les deux séparément et ils ne disaient pas toujours la même chose. L'application démarrait quand même et le problème n'apparaissait qu'en utilisant la fonctionnalité. Il a fallu reprendre les entités et les repositories un par un pour les aligner sur le script.
- Le nettoyage de Git. Les dossiers .idea et target étaient suivis, donc chaque commit embarquait des fichiers de l'IDE et des fichiers compilés. On a retiré ces dossiers du suivi, complété le .gitignore et remplacé le mot de passe par une variable d'environnement.

Fonctionnalités non réalisées et pourquoi : toutes les fonctionnalités demandées sont faites, de F1 à F12, ainsi que les deux pistes du lot alpha. Il reste quelques points de finition qu'on n'a pas eu le temps de reprendre, ils sont listés dans les limites connues.

Trois points à retenir du cours et du projet :

1. L'importance d'une bonne architecture, avec le MVC. Chaque partie a son rôle : le contrôleur reçoit la requête et choisit la réponse, le service porte les règles métier, le repository parle à la base et la vue s'occupe de l'affichage. On l'a vu quand on a ajouté l'API : elle a pu réutiliser les règles déjà écrites dans les services, sans rien recopier.

2. Les path variables. Avec @PathVariable, on récupère directement une valeur dans l'URL, comme l'id dans /incidents/{id}, et Spring la convertit dans le bon type. On a aussi retenu que si la valeur n'a pas le bon type, comme /incidents/abc, on obtient une erreur 400, et qu'une route fixe comme /incidents/historique passe avant /incidents/{id}.

3. Comment faire une API REST. @RestController renvoie du JSON, là où @Controller renvoie le nom d'une page. On utilise @GetMapping pour lire, @PostMapping pour créer ou déclencher une action et @PutMapping pour remplacer une valeur, avec @PathVariable pour désigner la ressource et @RequestBody pour lire le JSON reçu. Par exemple : GET /api/incidents/{id}, PUT /api/incidents/{id}/prestataire et POST /api/incidents/{id}/resolution. Il faut aussi répondre avec le bon code HTTP, comme 404 pour un incident inexistant ou 409 pour une action interdite.

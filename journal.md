# Journal -- Supervision Équipements (ALTN72, 2026-27)

Groupe : Léo ESNAULT, Loïc DEIVASSAGAYAME

## README

Il faut Java 21, Maven (ou le wrapper mvnw fourni) et une base MySQL ou MariaDB sur le port 3306.

Le script ne crée pas la base, donc on la crée d'abord, puis on lance le script :

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ALTN72_projet_java CHARACTER SET utf8mb4"
```

```bash
mysql -u root -p ALTN72_projet_java < src/main/resources/script.sql
```

Les dates des incidents de démo sont calculées à partir de l'heure actuelle. Si on relance le script, ils retrouvent l'âge prévu. Il faut le relancer juste avant une démonstration : l'incident ouvert depuis 47 h devient orange une heure plus tard.

Pour lancer l'application, on met le mot de passe de la base dans la variable SPRING_DATASOURCE_PASSWORD, puis :

```bash
./mvnw spring-boot:run
```

L'application est ensuite sur http://localhost:8080/login. Une fois connecté, la documentation de l'API est sur http://localhost:8080/swagger-ui.html.

Application déployée : [à compléter]

Comptes de test (les mots de passe sont hachés dans le script) :
- Responsable d'exploitation : resp_exploitation / [à compléter]
- Agent de terrain : agent_terrain / [à compléter]

Limites connues :
- La fonctionnalité transverse 2 n'est pas encore dans le dépôt.
- Chart.js et Bootstrap sont chargés depuis un CDN : les graphiques et la mise en page demandent un accès à Internet.
- Dans l'API, les erreurs produites hors de notre contrôleur (403, 405, route inconnue) gardent le format standard de Spring Boot, en anglais.
- L'en-tête affiche le profil entre crochets, et sec:authorize n'est pas interprété.
- La fiche équipement n'a pas l'en-tête commun et affiche l'état EN_SERVICE en rouge.
- Le code Programmeur du TP est encore là, alors que sa table n'est pas dans script.sql.
- La protection CSRF est désactivée.
- application.properties garde root comme mot de passe par défaut.
- Incident sans contrôle de version : si deux personnes agissent en même temps sur un même incident, la dernière écriture peut écraser l'autre, et même annuler une résolution.

## ARCHITECTURE

Dépôt : https://github.com/Loicdei/ALTN72_2026-27_ProjetJava

Schéma :

```
Navigateur ou client REST -> Spring Security -> Contrôleurs -> Services -> Repositories -> Base de données
                                                     |
                                                     v
                                     Vues Thymeleaf (pages) ou JSON (API)
```

- Les contrôleurs reçoivent les requêtes, valident les formulaires et choisissent la vue.
- Les services contiennent les règles métier.
- Les repositories accèdent à la base.
- Le package modele contient les entités et les objets calculés, formulaire les données saisies, exception nos erreurs métier et config la sécurité, l'horloge et la documentation de l'API.
- Le package api contient le contrôleur REST, ses objets d'échange et son gestionnaire d'erreurs. Il passe par les mêmes services que les pages web, donc par les mêmes règles.
- Les graphiques sont dessinés dans le navigateur par static/js/statistiques.js, à partir des données calculées par StatistiquesService.

## TESTS

Les tests se lancent avec la commande suivante, sans base à installer car ils utilisent H2 en mémoire :

```bash
./mvnw verify
```

Il y a 137 tests :
- IncidentServiceTest (18), EvaluationServiceTest (12), TableauDeBordServiceTest (8) et StatistiquesServiceTest (5) testent les règles métier avec des mocks et une horloge fixe.
- NiveauAttentionTest (11) teste les seuils de 48 h et 72 h, y compris à la seconde près.
- IncidentControleurTest (26), EvaluationControleurTest (16), TableauDeBordControleurTest (11) et StatistiquesControleurTest (6) testent les parcours HTTP avec MockMvc, y compris la sécurité.
- IncidentApiControleurTest (23) teste l'API : réponses JSON, codes HTTP, droits par profil et documentation OpenAPI.
- TpFilRougeApplicationTests (1) vérifie que l'application démarre.

Ce qui est couvert : F4 à F12, donc la déclaration, le détail, la modification, l'affectation, la résolution, l'historique, l'évaluation, le tableau de bord et l'API, avec les règles RG1 à RG8 et RG11, ainsi que le calcul des statistiques et la requête SQL native. Pour RG10, on vérifie qu'une opération refusée laisse les données inchangées.

Ce qui n'est pas couvert :
- RG9 : l'unicité de la référence d'un équipement repose sur la contrainte de la base et n'a pas de test dédié.
- script.sql et les requêtes sur un vrai serveur MySQL ou MariaDB, car les tests utilisent H2 et génèrent leur schéma à partir des entités. Nous avons seulement vérifié à la main, sur une instance MySQL 8.0.36 temporaire, que le script se charge, que les pages et l'API répondent et qu'une affectation, une résolution et une évaluation réussissent.
- L'affichage dans un navigateur, dont la confirmation avant de résoudre un incident. F10, F11 et Swagger ont seulement été vérifiés à la main sur H2, et les graphiques sur MySQL.
- Le code JavaScript des graphiques : les tests vérifient les données transmises à la page, pas le dessin.
- Deux actions simultanées sur un même incident (voir les limites connues).
- Le retour arrière d'une transaction après une vraie panne.

## CHOIX TECHNIQUES

### Droits par profil

Le second profil est l'agent de terrain, qui signale les pannes.

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

Les restrictions sont dans ConfigurationSecurite. Si un agent appelle directement l'URL d'affectation, de résolution, d'évaluation, du tableau de bord ou des statistiques, ou s'il tente de modifier un incident par l'API, il reçoit une erreur 403. Le responsable d'exploitation est le seul à prendre les décisions de suivi : c'est donc lui qui affecte, résout, évalue et supervise.

### Règles que nous avons précisées

F6 : seule la description est modifiable, par les deux profils, tant que l'incident n'est pas résolu. L'équipement et la date d'ouverture ne changent pas, car la durée et l'historique en dépendent.

F8 : on ne peut pas résoudre un incident sans prestataire. Le sujet ne l'impose pas, mais un tel incident ne pourrait jamais être évalué (RG8).

F10 : la note est saisie une seule fois et ne peut plus être modifiée. Elle est visible sur le détail de l'incident et dans l'historique, et la moyenne de chaque prestataire est sur la page Prestataires.

F11 : le niveau d'attention n'est jamais enregistré. Il est calculé à chaque affichage par NiveauAttention, seul endroit où figurent les seuils de 48 h et 72 h. Les incidents sont triés du plus urgent au moins urgent, puis du plus ancien au plus récent. On peut filtrer par station, par type d'équipement et par niveau d'attention. Les compteurs ignorent le filtre par niveau pour continuer à montrer la répartition complète.

F12 : l'API permet de lister et de consulter les incidents, d'affecter ou réaffecter un prestataire et de déclarer un incident résolu. Elle renvoie des objets IncidentReponse plutôt que les entités, pour ne pas lier le format de l'API au modèle JPA. Elle utilise la session de l'application : sans connexion, l'appel est redirigé vers /login (code 302), comme le demande le sujet pour les pages et l'API, alors qu'une API publique répondrait plutôt 401. Elle indique le niveau d'attention aux deux profils : seul l'écran du tableau de bord est réservé au responsable.

### Exceptions

Nous avons choisi @ControllerAdvice, avec la classe GestionnaireExceptions. Les services lèvent nos exceptions sans rien savoir de HTTP, et la traduction se fait à un seul endroit :
- RessourceIntrouvableException donne une erreur 404.
- OperationIncidentInterditeException donne une erreur 409.
- DonneeInvalideException donne une erreur 400.

Les messages sont en français et la page d'erreur n'affiche aucune trace technique. Les erreurs de formulaire restent affichées sur le formulaire.

Pour l'API, GestionnaireExceptionsApi traduit les mêmes exceptions en JSON au format ProblemDetail (application/problem+json). Il passe avant GestionnaireExceptions grâce à @Order, et ne s'applique qu'au package api : les pages web gardent leur page d'erreur HTML.

Nous n'utilisons pas ResponseEntity : les pages renvoient le nom d'une vue, et l'API renvoie ses objets avec le code 200 par défaut. ProblemDetail porte déjà le code des réponses d'erreur.

### SOLID

Principes appliqués :
- Responsabilité unique : le contrôleur gère HTTP, le service les règles, GestionnaireExceptions les erreurs et les formulaires la validation. Une nouvelle règle de résolution ne touche donc que le service. De même, changer un seuil d'urgence ne touche que NiveauAttention. L'API n'a pas eu besoin de réécrire les règles d'affectation et de résolution, car elle appelle le même service que les pages.
- Inversion des dépendances : les services reçoivent leurs dépendances par le constructeur, dont Clock et les repositories. Cela permet de les tester avec une horloge fixe et des mocks.

Principes écartés :
- Ouvert/fermé : il n'y a que trois statuts, imposés par le sujet. Une hiérarchie de classes serait plus lourde que quelques conditions.
- Liskov : nous n'avons pas de hiérarchie métier. Le seul héritage, UtilisateurDetails extends User, ajoute le prénom et le profil sans changer le comportement.
- Ségrégation des interfaces : nous n'écrivons pas d'interface métier, et découper JpaRepository n'apporterait rien ici.

### Requêtes

Nous utilisons des requêtes dérivées du nom de méthode, comme findByStatutOrderByDateHeureResolutionDesc.

Nous utilisons aussi cinq requêtes JPQL, pour les cas qu'un nom de méthode ne peut pas exprimer simplement :
- IncidentRepository.trouverIncidentsActifs a des filtres facultatifs, et ses « join fetch » évitent une requête par ligne du tableau de bord.
- PrestataireRepository.calculerNotesDesPrestataires calcule la moyenne, avec des jointures externes pour garder les prestataires sans évaluation.
- EquipementRepository.trouverTypes donne la liste des types pour le filtre du tableau de bord.
- IncidentRepository.trouverIncidents a un filtre de statut facultatif et charge d'un coup les données utiles à l'API.
- IncidentRepository.compterIncidentsParType regroupe les incidents par type pour les statistiques.

La seule requête SQL native est IncidentRepository.compterIncidentsParStation, qui compte les incidents actifs et résolus de chaque station.

Analyse critique :
- Intérêt : le SQL est direct et lisible, et les jointures externes qui partent de la station gardent les stations sans incident.
- Limites : les noms de tables et de colonnes ne sont pas contrôlés par le modèle, donc un renommage dans les entités ne met pas la requête à jour, et une erreur n'apparaît qu'à l'exécution. La valeur 'RESOLU' y est écrite en dur et ne suit pas l'enum StatutIncident. Le résultat passe par une interface de projection, moins directe qu'un objet construit en JPQL.
- Conclusion : JPQL aurait pu faire la même chose. Nous l'avons écrite en SQL parce que le sujet demande une requête native, pas parce que JPQL ne suffisait pas. Nous gardons JPQL pour les autres requêtes. Elle n'utilise que du SQL standard : elle a tourné sur H2 et sur MySQL 8.0.36.

### @Transactional

Il est utilisé sur trois méthodes d'IncidentService :
- modifierDescription, pour contrôler le statut et écrire dans la même transaction.
- affecterPrestataire, car le prestataire et le statut changent ensemble.
- declarerResolu, car le statut et la date de résolution changent ensemble.

declarerIncident n'en a pas besoin, car il ne fait qu'une seule écriture. EvaluationService.evaluerIntervention non plus : si deux saisies arrivent en même temps, c'est la contrainte d'unicité de la base qui les départage. ProgrammeurService en contient aussi deux autres, qui viennent du TP.

Le contrôleur REST n'en ajoute aucun, car il réutilise les méthodes d'IncidentService. StatistiquesService n'en a pas besoin non plus : il ne fait que des lectures, et de petits écarts entre deux graphiques n'ont pas d'importance.

@Transactional rend les changements atomiques, mais ne protège pas contre deux actions simultanées sur un même incident (voir les limites connues).

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
- Chart.js, car il couvre ces graphiques sans outil de construction. Le contrôleur et le service restent en Java, et le dessin est isolé dans static/js/statistiques.js.
- Trois sources de données selon le besoin : la requête SQL native pour les stations, JPQL pour les types et les notes, et un calcul en Java pour la tendance et les délais.
- La tendance montre aussi les jours sans incident, absents du résultat de la requête, sinon la courbe serait trompeuse.
- Une première version utilisait une courbe arrondie, remplacée par des segments droits : l'arrondi suggérait des valeurs entre deux jours. Le graphique en anneau a aussi été réduit, car il déformait sa rangée.
- Les prestataires sans note ou sans incident résolu ne sont pas affichés dans les graphiques concernés, pour ne pas les confondre avec une note ou un délai de 0.

Progression : réalisée le 5 novembre 2026, après le socle.

Ce qui fonctionne : les cinq graphiques s'affichent à partir de la base. StatistiquesServiceTest et StatistiquesControleurTest testent les calculs, les requêtes et les droits, et l'affichage a été vérifié à la main sur MySQL.

Difficultés : [à compléter]

Reste à faire : choisir la période de la tendance, et tester le dessin des graphiques, qui n'a pas de test automatique.

### Fonctionnalité transverse 2 : [à compléter]

Besoin, périmètre, choix et essais, ce qui fonctionne, difficultés, reste à faire : [à compléter]

## BILAN

Contribution de chacun : [à compléter]

Plus grande difficulté et comment elle a été surmontée : [à compléter]

Fonctionnalités non réalisées et pourquoi : [à compléter]

Trois points à retenir du cours et du projet : [à compléter]

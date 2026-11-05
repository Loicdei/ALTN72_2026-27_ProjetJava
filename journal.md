# Journal -- Supervision Équipements (ALTN72, 2026-27)

Groupe : Léo [à compléter], Loïc [à compléter]

Les anciennes notes du TP 5 sont dans docs/journal-tp5.md.

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

L'application est ensuite sur http://localhost:8080/login.

Application déployée : [à compléter]

Comptes de test (les mots de passe sont hachés dans le script) :
- Responsable d'exploitation : resp_exploitation / [à compléter]
- Agent de terrain : agent_terrain / [à compléter]

Limites connues :
- F12 et le lot alpha ne sont pas encore dans le dépôt.
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
Navigateur -> Spring Security -> Contrôleurs -> Services -> Repositories -> Base de données
                                     |
                                     v
                              Vues Thymeleaf
```

- Les contrôleurs reçoivent les requêtes, valident les formulaires et choisissent la vue.
- Les services contiennent les règles métier.
- Les repositories accèdent à la base.
- Le package modele contient les entités, formulaire les données saisies, exception nos erreurs métier et config la sécurité et l'horloge.

## TESTS

Les tests se lancent avec la commande suivante, sans base à installer car ils utilisent H2 en mémoire :

```bash
./mvnw verify
```

Il y a 101 tests :
- IncidentServiceTest (16), EvaluationServiceTest (12) et TableauDeBordServiceTest (8) testent les règles métier avec des mocks et une horloge fixe.
- NiveauAttentionTest (11) teste les seuils de 48 h et 72 h, y compris à la seconde près.
- IncidentControleurTest (26), EvaluationControleurTest (16) et TableauDeBordControleurTest (11) testent les parcours HTTP avec MockMvc, y compris la sécurité.
- TpFilRougeApplicationTests (1) vérifie que l'application démarre.

Ce qui est couvert : F4 à F11, donc la déclaration, le détail, la modification, l'affectation, la résolution, l'historique, l'évaluation et le tableau de bord, avec les règles RG1 à RG8 et RG11. Pour RG10, on vérifie qu'une opération refusée laisse les données inchangées.

Ce qui n'est pas couvert :
- F12, car elle n'est pas encore faite.
- RG9 : l'unicité de la référence d'un équipement repose sur la contrainte de la base et n'a pas de test dédié.
- script.sql et les requêtes JPQL sur un vrai serveur MySQL ou MariaDB, car les tests utilisent H2 et génèrent leur schéma à partir des entités.
- L'affichage dans un navigateur, dont la confirmation avant de résoudre un incident. F10 et F11 ont seulement été vérifiés à la main dans un navigateur, sur H2.
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

Les restrictions sont dans ConfigurationSecurite. Si un agent appelle directement l'URL d'affectation, de résolution, d'évaluation ou du tableau de bord, il reçoit une erreur 403. Le responsable d'exploitation est le seul à prendre les décisions de suivi : c'est donc lui qui affecte, résout, évalue et supervise.

### Règles que nous avons précisées

F6 : seule la description est modifiable, par les deux profils, tant que l'incident n'est pas résolu. L'équipement et la date d'ouverture ne changent pas, car la durée et l'historique en dépendent.

F8 : on ne peut pas résoudre un incident sans prestataire. Le sujet ne l'impose pas, mais un tel incident ne pourrait jamais être évalué (RG8).

F10 : la note est saisie une seule fois et ne peut plus être modifiée. Elle est visible sur le détail de l'incident et dans l'historique, et la moyenne de chaque prestataire est sur la page Prestataires.

F11 : le niveau d'attention n'est jamais enregistré. Il est calculé à chaque affichage par NiveauAttention, seul endroit où figurent les seuils de 48 h et 72 h. Les incidents sont triés du plus urgent au moins urgent, puis du plus ancien au plus récent. On peut filtrer par station, par type d'équipement et par niveau d'attention. Les compteurs ignorent le filtre par niveau pour continuer à montrer la répartition complète.

### Exceptions

Nous avons choisi @ControllerAdvice, avec la classe GestionnaireExceptions. Les services lèvent nos exceptions sans rien savoir de HTTP, et la traduction se fait à un seul endroit :
- RessourceIntrouvableException donne une erreur 404.
- OperationIncidentInterditeException donne une erreur 409.
- DonneeInvalideException donne une erreur 400.

Les messages sont en français et la page d'erreur n'affiche aucune trace technique. Les erreurs de formulaire restent affichées sur le formulaire. Nous n'utilisons pas ResponseEntity, car les contrôleurs actuels renvoient des pages.

### SOLID

Principes appliqués :
- Responsabilité unique : le contrôleur gère HTTP, le service les règles, GestionnaireExceptions les erreurs et les formulaires la validation. Une nouvelle règle de résolution ne touche donc que le service. De même, changer un seuil d'urgence ne touche que NiveauAttention.
- Inversion des dépendances : les services reçoivent leurs dépendances par le constructeur, dont Clock et les repositories. Cela permet de les tester avec une horloge fixe et des mocks.

Principes écartés :
- Ouvert/fermé : il n'y a que trois statuts, imposés par le sujet. Une hiérarchie de classes serait plus lourde que quelques conditions.
- Liskov : nous n'avons pas de hiérarchie métier. Le seul héritage, UtilisateurDetails extends User, ajoute le prénom et le profil sans changer le comportement.
- Ségrégation des interfaces : nous n'écrivons pas d'interface métier, et découper JpaRepository n'apporterait rien ici.

### Requêtes

Nous utilisons des requêtes dérivées du nom de méthode, comme findByStatutOrderByDateHeureResolutionDesc.

Nous utilisons aussi trois requêtes JPQL, pour les cas qu'un nom de méthode ne peut pas exprimer simplement :
- IncidentRepository.trouverIncidentsActifs a des filtres facultatifs, et ses « join fetch » évitent une requête par ligne du tableau de bord.
- PrestataireRepository.calculerNotesDesPrestataires calcule la moyenne, avec des jointures externes pour garder les prestataires sans évaluation.
- EquipementRepository.trouverTypes donne la liste des types pour le filtre du tableau de bord.

Requête SQL native avec son analyse critique : [à compléter]

### @Transactional

Il est utilisé sur trois méthodes d'IncidentService :
- modifierDescription, pour contrôler le statut et écrire dans la même transaction.
- affecterPrestataire, car le prestataire et le statut changent ensemble.
- declarerResolu, car le statut et la date de résolution changent ensemble.

declarerIncident n'en a pas besoin, car il ne fait qu'une seule écriture. EvaluationService.evaluerIntervention non plus : si deux saisies arrivent en même temps, c'est la contrainte d'unicité de la base qui les départage. ProgrammeurService en contient aussi deux autres, qui viennent du TP.

@Transactional rend les changements atomiques, mais ne protège pas contre deux actions simultanées sur un même incident (voir les limites connues).

## LOT ALPHA

### Fonctionnalité transverse 1 : [à compléter]

Besoin, périmètre, choix et essais, ce qui fonctionne, difficultés, reste à faire : [à compléter]

### Fonctionnalité transverse 2 : [à compléter]

Besoin, périmètre, choix et essais, ce qui fonctionne, difficultés, reste à faire : [à compléter]

## BILAN

Contribution de chacun : [à compléter]

Plus grande difficulté et comment elle a été surmontée : [à compléter]

Fonctionnalités non réalisées et pourquoi : [à compléter]

Trois points à retenir du cours et du projet : [à compléter]

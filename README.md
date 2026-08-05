# Phase de conception

## 1) Stack

Frontend : Angular
Backend : Java avec Spring Boot
Base de données : PostgreSQL
Système de versionnement : Git

## 2) Entités

Les entités clés identifiées sont:
- Account : utilisateur enregistré sur la plateforme.
- Workspace : projet collaboratif pouvant être créé par un utilisateur.
- Team_member : participation d'un utilisateur à un projet.
- User_notification : notification liée à une tâche.
- Project_invitation : invitation envoyée.
- Work_item : tâche liée à un projet.
- Work_item_history : historique des changements d'une tâche.

## 3) Schéma base de données

![Schéma base de données](bdd/schéma_bdd_b3_eval.png)

Correspondance détaillée des clés étrangères :
- WORKSPACES.owner_account_id -> ACCOUNTS.id
- TEAM_MEMBERS.workspace_id -> WORKSPACES.id
- TEAM_MEMBERS.account_id -> ACCOUNTS.id
- PROJECT_INVITATIONS.workspace_id -> WORKSPACES.id
- PROJECT_INVITATIONS.invited_by -> ACCOUNTS.id
- WORK_ITEMS.workspace_id -> WORKSPACES.id
- WORK_ITEMS.creator_account_id -> ACCOUNTS.id
- WORK_ITEMS.assigned_account_id -> ACCOUNTS.id
- WORK_ITEM_HISTORY.work_item_id -> WORK_ITEMS.id
- WORK_ITEM_HISTORY.changed_by -> ACCOUNTS.id
- USER_NOTIFICATIONS.account_id -> ACCOUNTS.id
- USER_NOTIFICATIONS.work_item_id -> WORK_ITEMS.id

## 4) Détail

Détail des relations du diagramme :
- ACCOUNTS / WORKSPACES (creates): un compte peut créer 0 à plusieurs workspaces (0..N); chaque workspace a exactement 1 owner_account_id.
- ACCOUNTS / TEAM_MEMBERS (participates) : un compte peut avoir 0 à plusieurs (0..N) enregistrements dans TEAM_MEMBERS (un enregistrement par appartenance à un workspace).
- WORKSPACES / TEAM_MEMBERS (contains) : un workspace contient 0 à plusieurs (0..N) membres; une ligne team_members appartient à un seul workspace.
- WORKSPACES / PROJECT_INVITATIONS (has) : un workspace peut avoir 0 à plusieurs (0..N) invitations.
- ACCOUNTS / PROJECT_INVITATIONS (sends) : un compte peut envoyer 0 à plusieurs (0..N) invitations; chaque invitation a 1 invited_by.
- WORKSPACES / WORK_ITEMS (contains) : un workspace contient 0 à plusieurs (0..N) tâches; chaque tâche appartient à 1 workspace.
- ACCOUNTS / WORK_ITEMS (creates) : un compte peut créer 0 à plusieurs (0..N) tâches; chaque tâche a 1 creator_account_id.
- ACCOUNTS / WORK_ITEMS (assigned) : un compte peut être assigné à 0 à plusieurs (0..N) tâches; une tâche a 0 ou 1 assigned_account_id.
- WORK_ITEMS / WORK_ITEM_HISTORY (has) : une tâche peut avoir 0 à plusieurs (0..N) entrées d'historique.
- ACCOUNTS / WORK_ITEM_HISTORY (modifies) : un compte peut être auteur de 0 à plusieurs (0..N) traces d'historique; chaque trace a 1 changed_by.
- ACCOUNTS / USER_NOTIFICATIONS (receives) : un compte peut recevoir 0 à plusieurs (0..N) notifications.
- WORK_ITEMS / USER_NOTIFICATIONS (concerns) : une tâche peut être liée à 0 à plusieurs (0..N) notifications.


## 5) Processus de déploiement

### Étapes du pipeline CI/CD

1. backend-tests
- Lance les tests backend (Spring Boot).
- Objectif : valider la logique métier, la persistance et les contrôleurs avant toute publication.

2. frontend-tests
- Lance les tests frontend (Angular/Jest).
- Objectif : éviter de publier une interface cassée.

3. build-images
- Construit les images Docker backend et frontend.
- Objectif : garantir que l'application est réellement packagée.

4. publish-images
- Publie les images sur le registry (Docker Hub) avec les credentials GitHub Secrets.
- Objectif : disposer d'artefacts versionnés, traçables et réutilisables.

5. deploy-app
- Exécute docker compose pour démarrer les services (db, backend, frontend) avec le profil prod.
- Exécute des tests d'acceptance simples (smoke tests HTTP).
- Objectif : vérifier que l'application démarre et répond correctement en production.

### Justification de l'ordre et déclenchement

L'ordre du pipeline suit une logique de fiabilité. On commence par exécuter les tests afin d'éviter toute publication de code instable. Une fois cette validation effectuée, on construit les images Docker, car ce sont elles qui constituent l'unité réelle de déploiement. La publication des images n'intervient qu'après une construction réussie, ce qui garantit des artefacts cohérents. Le déploiement est placé en dernière étape pour réduire le risque d'incident en production.

Concernant les déclencheurs, les tests sont lancés à chaque pull request vers main ainsi qu'à chaque push (sur main et sur branches/**). Cependant, le déploiement, ne s'exécute pas sur les pull requests : il est déclenché uniquement lors d'un push sur main. En pratique, cela inclut le push généré après la fusion d'une pull request dans main, et également un push direct sur main.

### Critères de succès du déploiement

Le déploiement est considéré comme réussi lorsque l'ensemble des jobs de tests se termine sans échec, lorsque les images Docker sont correctement construites puis publiées, et lorsque les conteneurs démarrent sans erreur bloquante. La validation finale repose sur les checks d'acceptance, qui doivent retourner les statuts attendus pour confirmer l'accessibilité fonctionnelle des services.


## 6) API PMT

La documentation complète des endpoints, paramètres, schémas et réponses est disponible via Swagger :
- Swagger UI : /swagger-ui.html
- OpenAPI JSON : /v3/api-docs

#### Exemple 1 : créer un workspace

```bash
curl -X POST http://localhost:8080/api/workspaces \
  -H "Content-Type: application/json" \
  -d '{
    "name": "PMT Release 1",
    "description": "Workspace de planification release",
    "startDate": "2026-08-01",
    "ownerAccountId": 1
  }'
```

#### Exemple 2 : créer une tâche

```bash
curl -X POST http://localhost:8080/api/workspaces/10/work-items \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Prepare release notes",
    "description": "Draft and review release notes",
    "dueDate": "2026-08-15",
    "priority": "HIGH",
    "creatorAccountId": 1,
    "assignedAccountId": 2
  }'
```

#### Exemple 3 : consulter le dashboard

```bash
curl "http://localhost:8080/api/workspaces/10/dashboard?actorAccountId=1"
```

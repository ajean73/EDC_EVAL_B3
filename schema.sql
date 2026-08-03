-- =========================================================
-- PostgreSQL: structure et données de test
-- =========================================================

BEGIN;

-- Suppression des objets existants
DROP VIEW IF EXISTS v_workspace_dashboard;
DROP TABLE IF EXISTS user_notifications;
DROP TABLE IF EXISTS work_item_history;
DROP TABLE IF EXISTS work_items;
DROP TABLE IF EXISTS project_invitations;
DROP TABLE IF EXISTS team_members;
DROP TABLE IF EXISTS workspaces;
DROP TABLE IF EXISTS accounts;

DROP TYPE IF EXISTS member_role;
DROP TYPE IF EXISTS invitation_state;
DROP TYPE IF EXISTS work_item_priority;
DROP TYPE IF EXISTS work_item_status;
DROP TYPE IF EXISTS history_action;
DROP TYPE IF EXISTS notification_kind;

-- Listes de valeurs controlées
CREATE TYPE member_role AS ENUM ('ADMIN', 'MEMBER', 'OBSERVER');
CREATE TYPE invitation_state AS ENUM ('PENDING', 'ACCEPTED', 'DECLINED');
CREATE TYPE work_item_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH');
CREATE TYPE work_item_status AS ENUM ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE');
CREATE TYPE history_action AS ENUM ('CREATED', 'UPDATED', 'ASSIGNED', 'STATUS_CHANGED');
CREATE TYPE notification_kind AS ENUM ('WORK_ITEM_ASSIGNED', 'WORK_ITEM_UPDATED');

-- Comptes utilisateurs
CREATE TABLE accounts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Espaces workspaces de projets
CREATE TABLE workspaces (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    owner_account_id BIGINT NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Membres d'un workspace et leur rôle
CREATE TABLE team_members (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    role member_role NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workspace_member UNIQUE (workspace_id, account_id)
);

-- Invitations à rejoindre un projet
CREATE TABLE project_invitations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    invitee_email VARCHAR(255) NOT NULL,
    role member_role NOT NULL,
    state invitation_state NOT NULL DEFAULT 'PENDING',
    invited_by BIGINT NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    CONSTRAINT ck_invitation_response_consistency CHECK (
        (state = 'PENDING' AND responded_at IS NULL) OR
        (state IN ('ACCEPTED', 'DECLINED') AND responded_at IS NOT NULL)
    )
);

-- Tâches d'un workspace, avec priorité, statut et assignation
CREATE TABLE work_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    due_date DATE,
    priority work_item_priority NOT NULL DEFAULT 'MEDIUM',
    status work_item_status NOT NULL DEFAULT 'TODO',
    creator_account_id BIGINT NOT NULL REFERENCES accounts(id),
    assigned_account_id BIGINT NULL REFERENCES accounts(id),
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignee_must_be_workspace_member
        FOREIGN KEY (workspace_id, assigned_account_id)
        REFERENCES team_members(workspace_id, account_id),
    CONSTRAINT ck_completed_work_item CHECK (
        (status = 'DONE' AND completed_at IS NOT NULL) OR
        (status <> 'DONE' AND completed_at IS NULL)
    )
);

-- Historique des changements d'une tâche
CREATE TABLE work_item_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    work_item_id BIGINT NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    changed_by BIGINT NOT NULL REFERENCES accounts(id),
    action history_action NOT NULL,
    previous_values JSONB,
    new_values JSONB,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Notifications destinées aux utilisateurs
CREATE TABLE user_notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    work_item_id BIGINT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    kind notification_kind NOT NULL,
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index utiles aux écrans principaux
CREATE INDEX idx_team_members_workspace ON team_members(workspace_id);
CREATE INDEX idx_team_members_account ON team_members(account_id);
CREATE INDEX idx_work_items_workspace_status ON work_items(workspace_id, status);
CREATE INDEX idx_work_items_assignee ON work_items(assigned_account_id);
CREATE INDEX idx_work_item_history_item ON work_item_history(work_item_id);
CREATE INDEX idx_user_notifications_account ON user_notifications(account_id);

-- Une seule invitation active (PENDING) par email et workspace
CREATE UNIQUE INDEX uq_project_invitations_pending_email
ON project_invitations(workspace_id, lower(invitee_email))
WHERE state = 'PENDING';

-- Fonction et triggers de mise a jour automatique de updated_at
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_accounts_timestamp
BEFORE UPDATE ON accounts
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_workspaces_timestamp
BEFORE UPDATE ON workspaces
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_work_items_timestamp
BEFORE UPDATE ON work_items
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Vue utilisée par le tableau de bord par statut
CREATE VIEW v_workspace_dashboard AS
WITH status_ref AS (
    SELECT unnest(enum_range(NULL::work_item_status)) AS status
),
work_item_counts AS (
    SELECT workspace_id, status, COUNT(*) AS work_item_count
    FROM work_items
    GROUP BY workspace_id, status
)
SELECT
    w.id AS workspace_id,
    w.name AS workspace_name,
    s.status,
    COALESCE(c.work_item_count, 0) AS work_item_count
FROM workspaces w
CROSS JOIN status_ref s
LEFT JOIN work_item_counts c
    ON c.workspace_id = w.id
   AND c.status = s.status;

-- =========================================================
-- Données de test
-- =========================================================

INSERT INTO accounts (username, email, password_hash) VALUES
('sarah', 'sarah@pmt.local', '$2a$10$testHashSarah'),
('renaud', 'renaud@pmt.local', '$2a$10$testHashRenaud'),
('clara', 'clara@pmt.local', '$2a$10$testHashClara'),
('lucas', 'lucas@pmt.local', '$2a$10$testHashLucas'),
('zoe', 'zoe@pmt.local', '$2a$10$testHashZoe');

INSERT INTO workspaces (name, description, start_date, owner_account_id) VALUES
('Plateforme de formation', 'Conception d''une plateforme de parcours de formation en ligne.', '2026-07-03', 1),
('Espace support client', 'Centralisation des demandes et de la base de connaissances.', '2026-07-11', 2),
('Catalogue des formations', 'Préparation des contenus du prochain catalogue interne.', '2026-07-18', 5);

INSERT INTO team_members (workspace_id, account_id, role) VALUES
(1, 1, 'ADMIN'),
(1, 2, 'MEMBER'),
(1, 3, 'MEMBER'),
(1, 4, 'OBSERVER'),
(2, 2, 'ADMIN'),
(2, 3, 'MEMBER'),
(2, 5, 'OBSERVER'),
(3, 5, 'ADMIN'),
(3, 1, 'MEMBER'),
(3, 4, 'OBSERVER');

INSERT INTO project_invitations (workspace_id, invitee_email, role, state, invited_by, responded_at) VALUES
(1, 'expert.metier@pmt.local', 'OBSERVER', 'PENDING', 1, NULL),
(2, 'support.n2@pmt.local', 'MEMBER', 'ACCEPTED', 2, CURRENT_TIMESTAMP),
(3, 'redacteur@pmt.local', 'MEMBER', 'DECLINED', 5, CURRENT_TIMESTAMP);

INSERT INTO work_items (workspace_id, title, description, due_date, priority, status, creator_account_id, assigned_account_id, completed_at) VALUES
 (1, 'Définir le parcours apprenant', 'Formaliser les étapes d''inscription, de suivi et de validation.', '2026-08-12', 'HIGH', 'IN_PROGRESS', 1, 2, NULL),
 (1, 'Maquetter la page « Mes formations »', 'Produire la maquette de consultation des modules suivis.', '2026-08-15', 'MEDIUM', 'TODO', 1, 3, NULL),
 (1, 'Valider les contenus RGPD', 'Vérifier les mentions affichées lors de la création de compte.', '2026-08-09', 'HIGH', 'IN_REVIEW', 2, 3, NULL),
 (2, 'Qualifier les demandes entrantes', 'Définir les catégories et les niveaux de priorité du support.', '2026-08-10', 'HIGH', 'TODO', 2, 3, NULL),
 (2, 'Rédiger la procédure de réponse', 'Documenter la réponse aux demandes récurrentes.', '2026-08-14', 'MEDIUM', 'DONE', 2, 2, '2026-07-29'),
 (3, 'Recenser les formations disponibles', 'Consolider les titres, durées et intervenants des formations.', '2026-08-20', 'HIGH', 'IN_PROGRESS', 5, 1, NULL),
 (3, 'Publier le calendrier automne', 'Rendre les sessions disponibles dans le catalogue.', '2026-08-22', 'LOW', 'TODO', 5, NULL, NULL);

INSERT INTO work_item_history (work_item_id, changed_by, action, previous_values, new_values) VALUES
(1, 1, 'CREATED', NULL, '{"status":"TODO","assigned_account_id":2}'),
(1, 2, 'STATUS_CHANGED', '{"status":"TODO"}', '{"status":"IN_PROGRESS"}'),
(2, 1, 'CREATED', NULL, '{"status":"TODO","assigned_account_id":3}'),
(3, 2, 'STATUS_CHANGED', '{"status":"IN_PROGRESS"}', '{"status":"IN_REVIEW"}'),
(5, 2, 'STATUS_CHANGED', '{"status":"IN_REVIEW"}', '{"status":"DONE"}'),
(6, 5, 'ASSIGNED', '{"assigned_account_id":null}', '{"assigned_account_id":1}');

INSERT INTO user_notifications (account_id, work_item_id, kind, is_sent, sent_at) VALUES
(2, 1, 'WORK_ITEM_ASSIGNED', TRUE, CURRENT_TIMESTAMP),
(3, 2, 'WORK_ITEM_ASSIGNED', TRUE, CURRENT_TIMESTAMP),
(3, 4, 'WORK_ITEM_ASSIGNED', FALSE, NULL),
(1, 6, 'WORK_ITEM_ASSIGNED', TRUE, CURRENT_TIMESTAMP),
(3, 3, 'WORK_ITEM_UPDATED', FALSE, NULL);

COMMIT;

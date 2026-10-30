DROP TABLE IF EXISTS evaluation;
DROP TABLE IF EXISTS incident;
DROP TABLE IF EXISTS equipement;
DROP TABLE IF EXISTS station;
DROP TABLE IF EXISTS prestataire;
DROP TABLE IF EXISTS utilisateur;

CREATE TABLE station (
    id_station INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL
);

CREATE TABLE prestataire (
    id_prestataire INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    telephone VARCHAR(20)
);

CREATE TABLE utilisateur (
    id_connexion VARCHAR(50) PRIMARY KEY,
    mot_de_passe_hache VARCHAR(255) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    profil VARCHAR(50) NOT NULL
);

CREATE TABLE equipement (
    id_equipement INT AUTO_INCREMENT PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(255) NOT NULL,
    type_equipement VARCHAR(100) NOT NULL,
    etat_fonctionnement VARCHAR(50) NOT NULL DEFAULT 'EN_SERVICE',
    id_station INT NOT NULL,
    CONSTRAINT fk_equipement_station FOREIGN KEY (id_station)
        REFERENCES station(id_station) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE incident (
    id_incident INT AUTO_INCREMENT PRIMARY KEY,
    id_equipement INT NOT NULL,
    description TEXT NOT NULL,
    date_heure_ouverture DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(50) NOT NULL DEFAULT 'NON_AFFECTE',
    id_prestataire INT NULL,
    date_heure_resolution DATETIME NULL,
    CONSTRAINT fk_incident_equipement FOREIGN KEY (id_equipement)
        REFERENCES equipement(id_equipement) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_incident_prestataire FOREIGN KEY (id_prestataire)
        REFERENCES prestataire(id_prestataire) ON DELETE SET NULL ON UPDATE CASCADE
);

-- 6. Table ÉVALUATION
CREATE TABLE evaluation (
    id_evaluation INT AUTO_INCREMENT PRIMARY KEY,
    id_incident INT NOT NULL UNIQUE,
    note INT NOT NULL CHECK (note BETWEEN 1 AND 5),
    commentaire TEXT NULL,
    date_evaluation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evaluation_incident FOREIGN KEY (id_incident)
        REFERENCES incident(id_incident) ON DELETE CASCADE ON UPDATE CASCADE
);

INSERT INTO station (nom) VALUES ('Station Châtelet'), ('Station Gare de Lyon'),
('Station Montparnasse'), ('Station La Défense');

INSERT INTO prestataire (nom, email, telephone) VALUES ('TechServices SARL', 'contact@techservices.fr', '0140001122'),
('Maintenance Express', 'support@maintexpress.com', '0149998877'),
('Réseau & Co', 'sav@reseauco.fr', '0155554433'),
('Sécurité Informatique SA', 'info@secuinfo.fr', '0188889900');

-- 3 Profils utilisateurs d'exemple (Responsable d'exploitation et Agent terrain)
INSERT INTO utilisateur (id_connexion, mot_de_passe_hache, prenom, profil) VALUES
('resp_exploitation', '$2a$12$kONtYno4DcaQgPE0cCEKv.qYHeXvht2MrJlNEfJGvjpFyJSV2xnTC', 'Loic', 'RESPONSABLE_EXPLOITATION'),
('agent_terrain', '$2a$12$j67zwx5Uxod2IPSAihRZl.JLqAdDzQOY7mhndiw6pLFOth.d3tkye', 'Leo', 'AGENT_TERRAIN');

INSERT INTO equipement (reference, libelle, type_equipement, etat_fonctionnement, id_station) VALUES
('EQ-CH-001', 'Borne de validation 1', 'Validateur', 'EN_SERVICE', 1),
('EQ-GL-002', 'Distributeur automatique 2', 'Billetterie', 'EN_PANNE', 2),
('EQ-MP-003', 'Écran d information passagers', 'Affichage', 'EN_SERVICE', 3),
('EQ-LD-004', 'Portique d accès central', 'Contrôle d accès', 'EN_MAINTENANCE', 4);

-- Incidents avec dates relatives pour tester les seuils Vert, Orange, Rouge et Affecté :
INSERT INTO incident (id_equipement, description, date_heure_ouverture, statut, id_prestataire, date_heure_resolution) VALUES
-- 1. Non affecté, ouvert il y a 10h -> Doit être VERT (< 48h)
(1, 'Écran tactile fissuré mais fonctionnel', DATE_SUB(NOW(), INTERVAL 10 HOUR), 'NON_AFFECTE', NULL, NULL),

-- 2. Non affecté, ouvert il y a 50h -> Doit être ORANGE (entre 48h et 72h)
(2, 'Liseur de carte bancaire hors service', DATE_SUB(NOW(), INTERVAL 50 HOUR), 'NON_AFFECTE', NULL, NULL),

-- 3. Non affecté, ouvert il y a 80h -> Doit être ROUGE (>= 72h)
(3, 'Affichage figé sur le quai 3', DATE_SUB(NOW(), INTERVAL 80 HOUR), 'NON_AFFECTE', NULL, NULL),

-- 4. Affecté à un prestataire -> Présentation distincte (Pas de couleur d'urgence RG4)
(4, 'Blocage mécanique des portes du portique', DATE_SUB(NOW(), INTERVAL 30 HOUR), 'AFFECTE', 2, NULL),

-- 5. Résolu -> N'apparaît plus sur le tableau de bord actif (RG6)
(2, 'Panne d alimentation secteur', DATE_SUB(NOW(), INTERVAL 120 HOUR), 'RESOLU', 1, DATE_SUB(NOW(), INTERVAL 100 HOUR));

-- Évaluation sur l'incident résolu (Incident 5)
INSERT INTO evaluation (id_incident, note, commentaire, date_evaluation) VALUES
(5, 5, 'Intervention rapide et efficace.', NOW());
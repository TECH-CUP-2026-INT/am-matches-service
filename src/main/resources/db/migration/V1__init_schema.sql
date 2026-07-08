CREATE TABLE match (
    id                         UUID PRIMARY KEY,
    competencia_match_id       UUID NOT NULL,
    home_team_id               UUID NOT NULL,
    away_team_id               UUID NOT NULL,
    home_team_name             VARCHAR(150) NOT NULL,
    away_team_name             VARCHAR(150) NOT NULL,
    referee_id                 UUID NOT NULL,
    status                     VARCHAR(20) NOT NULL,
    current_period             VARCHAR(20),
    home_score                 INTEGER NOT NULL DEFAULT 0,
    away_score                 INTEGER NOT NULL DEFAULT 0,
    added_minutes_first_half   INTEGER NOT NULL DEFAULT 0,
    added_minutes_second_half  INTEGER NOT NULL DEFAULT 0,
    period_started_at          TIMESTAMP,
    accumulated_seconds        BIGINT NOT NULL DEFAULT 0,
    started_at                 TIMESTAMP,
    ended_at                   TIMESTAMP,
    created_at                 TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_match_competencia_match_id ON match (competencia_match_id);
CREATE INDEX ix_match_referee_id ON match (referee_id);

CREATE TABLE goal (
    id          UUID PRIMARY KEY,
    match_id    UUID NOT NULL REFERENCES match (id),
    team_id     UUID NOT NULL,
    player_id   UUID NOT NULL,
    minute      INTEGER NOT NULL,
    period      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX ix_goal_match_id ON goal (match_id);

CREATE TABLE card (
    id          UUID PRIMARY KEY,
    match_id    UUID NOT NULL REFERENCES match (id),
    team_id     UUID NOT NULL,
    player_id   UUID NOT NULL,
    card_type   VARCHAR(10) NOT NULL,
    minute      INTEGER NOT NULL,
    period      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX ix_card_match_id ON card (match_id);
CREATE INDEX ix_card_player_id ON card (player_id);

CREATE TABLE substitution (
    id             UUID PRIMARY KEY,
    match_id       UUID NOT NULL REFERENCES match (id),
    team_id        UUID NOT NULL,
    player_out_id  UUID NOT NULL,
    player_in_id   UUID NOT NULL,
    minute         INTEGER NOT NULL,
    period         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX ix_substitution_match_id ON substitution (match_id);

CREATE TABLE match_observation (
    id                UUID PRIMARY KEY,
    match_id          UUID NOT NULL REFERENCES match (id),
    referee_id        UUID NOT NULL,
    observation_text  TEXT NOT NULL,
    minute            INTEGER,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX ix_match_observation_match_id ON match_observation (match_id);

CREATE TABLE match_sheet (
    id           UUID PRIMARY KEY,
    match_id     UUID NOT NULL UNIQUE REFERENCES match (id),
    file_url     VARCHAR(500) NOT NULL,
    uploaded_by  UUID NOT NULL,
    uploaded_at  TIMESTAMP NOT NULL DEFAULT now()
);

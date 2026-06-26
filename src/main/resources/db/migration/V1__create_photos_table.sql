CREATE TABLE photos (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    artist      VARCHAR(200)  NOT NULL,
    s3_key      VARCHAR(512)  NOT NULL UNIQUE,
    description VARCHAR(500)  NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL
);

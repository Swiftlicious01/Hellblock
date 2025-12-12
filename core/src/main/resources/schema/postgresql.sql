CREATE TABLE IF NOT EXISTS "{prefix}_data" (
    uuid UUID PRIMARY KEY,
    lock INTEGER NOT NULL,
    data BYTEA NOT NULL
);
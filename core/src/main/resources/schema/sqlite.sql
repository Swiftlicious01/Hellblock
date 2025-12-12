CREATE TABLE IF NOT EXISTS "{prefix}_data" (
    uuid TEXT PRIMARY KEY,
    lock INTEGER NOT NULL,
    data BLOB NOT NULL
);
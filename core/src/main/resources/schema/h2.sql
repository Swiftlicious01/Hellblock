CREATE TABLE IF NOT EXISTS `{prefix}_data` (
    uuid CHAR(36) NOT NULL UNIQUE,
    lock INT NOT NULL,
    data BLOB NOT NULL,
    PRIMARY KEY (uuid)
);
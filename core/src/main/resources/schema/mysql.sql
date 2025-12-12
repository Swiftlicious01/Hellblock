CREATE TABLE IF NOT EXISTS `{prefix}_data` (
    `uuid` CHAR(36) NOT NULL UNIQUE,
    `lock` INT NOT NULL,
    `data` LONGBLOB NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

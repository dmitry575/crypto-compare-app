DROP TABLE IF EXISTS symbols;
CREATE TABLE IF NOT EXISTS `symbols` (`id` INTEGER NOT NULL, `ticker` TEXT, `symbol` TEXT, `bestAskProviderId` INTEGER, `bestAskPrice` REAL NOT NULL, `bestBidProviderId` INTEGER, `bestBidPrice` REAL NOT NULL, `spreadPercent` REAL, `bestAskUpdatedAt` TEXT, `bestBidUpdatedAt` TEXT, `updatedAt` TEXT NOT NULL, `syncedAtMillis` INTEGER NOT NULL, `volume24h` REAL, `quoteVolume24h` REAL, `change24h` REAL, PRIMARY KEY(`id`));
CREATE INDEX IF NOT EXISTS `index_symbols_bestAskProviderId` ON `symbols` (`bestAskProviderId`);
CREATE INDEX IF NOT EXISTS `index_symbols_bestBidProviderId` ON `symbols` (`bestBidProviderId`);
DELETE FROM catalog_remote_key;

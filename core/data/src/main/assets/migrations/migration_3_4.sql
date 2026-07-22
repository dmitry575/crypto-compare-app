DROP TABLE symbols;

CREATE TABLE symbols (
    id INTEGER NOT NULL PRIMARY KEY,
    ticker TEXT,
    symbol TEXT,
    providerId INTEGER NOT NULL,
    priceSell REAL NOT NULL,
    priceBuy REAL NOT NULL,
    updatedAt TEXT NOT NULL,
    syncedAtMillis INTEGER NOT NULL,
    FOREIGN KEY(providerId) REFERENCES providers(id) ON DELETE NO ACTION
);

CREATE INDEX index_symbols_providerId ON symbols(providerId);

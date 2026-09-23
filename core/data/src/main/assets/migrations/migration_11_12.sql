ALTER TABLE portfolio_positions ADD COLUMN providerId INTEGER;

CREATE TABLE IF NOT EXISTS portfolio_quotes (
    symbolId INTEGER NOT NULL,
    providerId INTEGER NOT NULL,
    price REAL NOT NULL,
    quotedAtMillis INTEGER NOT NULL,
    PRIMARY KEY(symbolId)
);

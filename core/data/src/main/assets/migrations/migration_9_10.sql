CREATE TABLE IF NOT EXISTS favourite_symbols (
    userId TEXT NOT NULL,
    symbolId INTEGER NOT NULL,
    ticker TEXT NOT NULL,
    updatedAt INTEGER NOT NULL,
    PRIMARY KEY(userId, symbolId)
);
INSERT OR REPLACE INTO favourite_symbols (userId, symbolId, ticker, updatedAt)
SELECT favourite_tickers.userId, symbols.id, UPPER(symbols.ticker), favourite_tickers.updatedAt
FROM favourite_tickers
JOIN symbols ON UPPER(symbols.ticker) = UPPER(favourite_tickers.ticker);
DROP TABLE favourite_tickers;
DROP TABLE pending_favourite_operations;
CREATE TABLE IF NOT EXISTS pending_favourite_operations (
    userId TEXT NOT NULL,
    symbolId INTEGER NOT NULL,
    ticker TEXT NOT NULL,
    operation TEXT NOT NULL,
    updatedAt INTEGER NOT NULL,
    PRIMARY KEY(userId, symbolId)
);

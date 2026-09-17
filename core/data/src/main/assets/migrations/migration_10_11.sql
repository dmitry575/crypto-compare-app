CREATE TABLE IF NOT EXISTS portfolio_positions (
    symbolId INTEGER NOT NULL,
    ticker TEXT NOT NULL,
    amount REAL NOT NULL,
    buyPrice REAL NOT NULL,
    updatedAtMillis INTEGER NOT NULL,
    PRIMARY KEY(symbolId)
);

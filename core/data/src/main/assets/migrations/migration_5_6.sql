CREATE TABLE IF NOT EXISTS pending_favourite_operations (
    userId TEXT NOT NULL,
    ticker TEXT NOT NULL,
    operation TEXT NOT NULL,
    updatedAt INTEGER NOT NULL,
    PRIMARY KEY (userId, ticker)
);

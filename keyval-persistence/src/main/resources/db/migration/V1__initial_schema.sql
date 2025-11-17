-- Namespaces table
CREATE TABLE IF NOT EXISTS namespaces (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    key_strategy_type TEXT NOT NULL,
    value_strategy_type TEXT NOT NULL,
    configuration TEXT, -- JSON
    created_at INTEGER NOT NULL -- Unix timestamp in milliseconds
);

CREATE INDEX IF NOT EXISTS idx_namespaces_name ON namespaces(name);

-- Nodes table
CREATE TABLE IF NOT EXISTS nodes (
    id TEXT PRIMARY KEY,
    host TEXT NOT NULL,
    port INTEGER NOT NULL,
    admin_port INTEGER NOT NULL,
    status TEXT NOT NULL,
    last_seen INTEGER NOT NULL -- Unix timestamp in milliseconds
);

CREATE INDEX IF NOT EXISTS idx_nodes_status ON nodes(status);
CREATE INDEX IF NOT EXISTS idx_nodes_last_seen ON nodes(last_seen);

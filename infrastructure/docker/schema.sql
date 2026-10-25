CREATE TABLE IF NOT EXISTS user_settings (
 minecraft_uuid char(32) PRIMARY KEY CHECK(minecraft_uuid ~ '^[0-9a-fA-F]{32}$'),
 revision integer NOT NULL CHECK(revision > 0),
 settings jsonb NOT NULL,
 updated_at timestamptz NOT NULL DEFAULT now()
);

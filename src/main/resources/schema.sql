-- schema.sql
-- This file is executed at startup by DbManager.initializeDatabase()

-- Users table (ensure it exists)
CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(255) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL
);

-- Journals table (ensure it exists)
CREATE TABLE IF NOT EXISTS journals (
    user_email VARCHAR(255) REFERENCES users(email),
    entry_date DATE NOT NULL,
    content TEXT,
    weather VARCHAR(50),
    mood VARCHAR(50),
    PRIMARY KEY (user_email, entry_date)
);

-- User Progress table (ensure it exists)
CREATE TABLE IF NOT EXISTS user_progress (
    user_email VARCHAR(255) PRIMARY KEY REFERENCES users(email),
    current_streak INT DEFAULT 0,
    total_xp INT DEFAULT 0,
    current_level INT DEFAULT 1,
    last_journal_date DATE
);

-- MIGRATION: Add start_of_week column to users if it doesn't exist
-- PostgreSQL syntax for conditional column addition
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='users' AND column_name='start_of_week') THEN
        ALTER TABLE users ADD COLUMN start_of_week VARCHAR(20) DEFAULT 'SUNDAY';
    END IF;
END $$;

-- Achievement Definitions table
CREATE TABLE IF NOT EXISTS achievement_definitions (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    icon_char VARCHAR(10)
);

-- User Achievements table (linking users to achievements)
CREATE TABLE IF NOT EXISTS user_achievements (
    user_email VARCHAR(255) REFERENCES users(email),
    achievement_id VARCHAR(50) REFERENCES achievement_definitions(id),
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_email, achievement_id)
);

-- Populate Default Achievements (Idempotent insert)
INSERT INTO achievement_definitions (id, title, description, icon_char)
VALUES 
    ('ach_1', 'First Step', 'Write your first entry', '📝'),
    ('ach_7', 'Consistency', '7 Day Streak', '🔥'),
    ('ach_100', 'Century', 'Reach Level 100', '💯')
ON CONFLICT (id) DO NOTHING;

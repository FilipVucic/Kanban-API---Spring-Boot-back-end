-- V1: Create tasks table
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'TO_DO',
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);
CREATE INDEX idx_tasks_status_created_at ON tasks(status, created_at DESC);

ALTER TABLE tasks ADD CONSTRAINT chk_status 
    CHECK (status IN ('TO_DO', 'IN_PROGRESS', 'DONE'));

ALTER TABLE tasks ADD CONSTRAINT chk_priority 
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));


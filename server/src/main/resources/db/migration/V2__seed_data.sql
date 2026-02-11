-- SATORN v3.0 Seed Data

-- Insert roles
INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_MODERATOR'), ('ROLE_USER') ON CONFLICT DO NOTHING;

-- Insert default categories
INSERT INTO categories (name, color) VALUES 
    ('Politics', '#FF6B6B'),
    ('Health', '#4ECDC4'),
    ('Technology', '#45B7D1'),
    ('Economy', '#FFA07A'),
    ('Society', '#98D8C8'),
    ('Environment', '#6BCB77'),
    ('Sports', '#FFB6B9'),
    ('Entertainment', '#FF8B94'),
    ('Security', '#AF69EE'),
    ('Media', '#FFD93D')
ON CONFLICT DO NOTHING;

-- Insert test users
INSERT INTO users (username, email, password, full_name, enabled) VALUES 
    ('admin', 'admin@satorn.com', '$2a$10$slYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy3PB7i', 'Admin User', true),
    ('moderator', 'moderator@satorn.com', '$2a$10$slYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy3PB7i', 'Moderator User', true),
    ('user1', 'user1@satorn.com', '$2a$10$slYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy3PB7i', 'Test User', true)
ON CONFLICT DO NOTHING;

-- Assign roles to users
-- Note: Get actual IDs from sequence
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r 
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r 
WHERE u.username = 'moderator' AND r.name = 'ROLE_MODERATOR'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r 
WHERE u.username = 'user1' AND r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

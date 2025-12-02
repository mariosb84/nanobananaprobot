-- ============================================
-- ПОЛНАЯ СТРУКТУРА БД ДЛЯ NANO BANANA AI БОТА
-- ============================================

-- 1. ТАБЛИЦА ПОЛЬЗОВАТЕЛЕЙ
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telegram_chat_id BIGINT UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. ТАБЛИЦА РОЛЕЙ
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- 3. СВЯЗЬ ПОЛЬЗОВАТЕЛИ-РОЛИ
CREATE TABLE IF NOT EXISTS user_roles (
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 4. ТАБЛИЦА БАЛАНСА ГЕНЕРАЦИЙ
CREATE TABLE IF NOT EXISTS generation_balance (
    id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    image_balance INTEGER DEFAULT 0,
    video_balance INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. ТАБЛИЦА ПОКУПОК ПАКЕТОВ
CREATE TABLE IF NOT EXISTS package_purchases (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    package_type VARCHAR(10) CHECK (package_type IN ('image', 'video')),
    package_count INTEGER,
    price DECIMAL(10, 2),
    payment_id VARCHAR(100),
    payment_status VARCHAR(20) DEFAULT 'pending',
    purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP
);

-- 6. ТАБЛИЦА ИСТОРИИ ГЕНЕРАЦИЙ
CREATE TABLE IF NOT EXISTS generation_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(10) CHECK (type IN ('image', 'video')),
    prompt TEXT,
    image_url TEXT,
    video_url TEXT,
    model VARCHAR(50),
    status VARCHAR(20) DEFAULT 'completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. ИНДЕКСЫ ДЛЯ БЫСТРОГО ПОИСКА
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_telegram_id ON users(telegram_chat_id);
CREATE INDEX idx_balance_user_id ON generation_balance(user_id);
CREATE INDEX idx_purchases_user_id ON package_purchases(user_id);
CREATE INDEX idx_history_user_id ON generation_history(user_id);
CREATE INDEX idx_history_created ON generation_history(created_at);

-- 8. ТРИГГЕРЫ ДЛЯ ОБНОВЛЕНИЯ UPDATED_AT
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_balance_updated_at
BEFORE UPDATE ON generation_balance
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 9. ТЕСТОВЫЕ ДАННЫЕ
INSERT INTO roles (name) VALUES
('ROLE_USER'),
('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (username, password, email) VALUES
('test_user', '$2a$10$testpasswordhash', 'user@test.com'),
('test_admin', '$2a$10$testpasswordhash', 'admin@test.com')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'test_user' AND r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'test_admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO generation_balance (user_id, image_balance, video_balance)
SELECT id, 3, 0 FROM users WHERE username = 'test_user'
ON CONFLICT (user_id) DO NOTHING;

-- 10. ПРОВЕРКА
SELECT '✅ База данных успешно создана!' as message;
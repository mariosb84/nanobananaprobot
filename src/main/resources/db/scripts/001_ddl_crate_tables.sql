-- ============================================
-- ОБНОВЛЕННАЯ СТРУКТУРА БД ДЛЯ NANO BANANA AI БОТА
-- Версия 2.0 (с токенами)
-- ============================================

-- 1. ТАБЛИЦА ПОЛЬЗОВАТЕЛЕЙ (person)
CREATE TABLE IF NOT EXISTS person (
    person_id SERIAL PRIMARY KEY,
    person_login VARCHAR(100) UNIQUE NOT NULL,
    person_password VARCHAR(255) NOT NULL,
    person_email VARCHAR(255) NOT NULL,
    person_phone VARCHAR(12),
    telegram_chat_id BIGINT UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. ТАБЛИЦА РОЛЕЙ (если нужно)
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- 3. СВЯЗЬ ПОЛЬЗОВАТЕЛИ-РОЛИ (person_roles)
CREATE TABLE IF NOT EXISTS person_roles (
    person_id INTEGER REFERENCES person(person_id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (person_id, role)
);

-- 4. ТАБЛИЦА БАЛАНСА ТОКЕНОВ И ГЕНЕРАЦИЙ
CREATE TABLE IF NOT EXISTS user_generation_balance (
    id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE REFERENCES person(person_id) ON DELETE CASCADE,

    -- НОВОЕ: БАЛАНС В ТОКЕНАХ
    tokens_balance INTEGER NOT NULL DEFAULT 0,

    -- Старые поля (для обратной совместимости)
    image_balance INTEGER NOT NULL DEFAULT 0,
    video_balance INTEGER NOT NULL DEFAULT 0,
    trial_used BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. ТАБЛИЦА ПОКУПОК ТОКЕНОВ (новый тип пакета)
CREATE TABLE IF NOT EXISTS token_purchases (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES person(person_id) ON DELETE CASCADE,
    package_type VARCHAR(20) CHECK (package_type IN ('tokens', 'image', 'video')),
    token_count INTEGER,
    price_rub DECIMAL(10, 2),
    payment_id VARCHAR(100),
    payment_status VARCHAR(20) DEFAULT 'pending',
    purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP
);

-- 6. ТАБЛИЦА ИСТОРИИ ОПЕРАЦИЙ (генерации + списания)
CREATE TABLE IF NOT EXISTS operation_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES person(person_id) ON DELETE CASCADE,
    operation_type VARCHAR(20) CHECK (operation_type IN ('generate', 'edit', 'merge', 'purchase', 'refund')),
    tokens_change INTEGER, -- + при покупке, - при списании
    tokens_balance_after INTEGER,
    details JSONB, -- {resolution: '1K', aspect_ratio: '16:9', prompt: '...', image_count: 2}
    status VARCHAR(20) DEFAULT 'completed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. ИНДЕКСЫ ДЛЯ БЫСТРОГО ПОИСКА
CREATE INDEX IF NOT EXISTS idx_person_login ON person(person_login);
CREATE INDEX IF NOT EXISTS idx_person_telegram ON person(telegram_chat_id);
CREATE INDEX IF NOT EXISTS idx_balance_user_id ON user_generation_balance(user_id);
CREATE INDEX IF NOT EXISTS idx_purchases_user_id ON token_purchases(user_id);
CREATE INDEX IF NOT EXISTS idx_history_user_id ON operation_history(user_id);
CREATE INDEX IF NOT EXISTS idx_history_created ON operation_history(created_at);
CREATE INDEX IF NOT EXISTS idx_history_type ON operation_history(operation_type);

-- 8. ТРИГГЕРЫ ДЛЯ ОБНОВЛЕНИЯ ВРЕМЕНИ
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Для таблицы person (users)
CREATE TRIGGER update_person_updated_at
BEFORE UPDATE ON person
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- Для таблицы баланса
CREATE TRIGGER update_balance_updated_at
BEFORE UPDATE ON user_generation_balance
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 9. ДОБАВЛЕНИЕ КОЛОНКИ ТОКЕНОВ (если таблица уже существует)
-- Простой вариант без DO блока
ALTER TABLE user_generation_balance
ADD COLUMN IF NOT EXISTS tokens_balance INTEGER NOT NULL DEFAULT 0;

-- Обновляем существующие записи
UPDATE user_generation_balance
SET
    image_balance = 0,
    trial_used = TRUE,
    tokens_balance = 0
WHERE trial_used = FALSE OR image_balance > 0;

-- 10. НАЧАЛЬНЫЕ ДАННЫЕ
INSERT INTO roles (name) VALUES
('ROLE_USER'),
('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

-- 11. ПРОВЕРКА СТРУКТУРЫ
SELECT
    '✅ База данных готова к работе с токенами!' as message,
    (SELECT COUNT(*) FROM person) as users_count,
    (SELECT COUNT(*) FROM user_generation_balance WHERE tokens_balance > 0) as users_with_tokens,
    (SELECT SUM(tokens_balance) FROM user_generation_balance) as total_tokens;
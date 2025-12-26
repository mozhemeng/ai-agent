CREATE TABLE chat_session
(
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(50),
    title       VARCHAR(255),
    model_name  VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE chat_message
(
    id          VARCHAR(36) PRIMARY KEY,
    session_id  VARCHAR(36),
    role        VARCHAR(50),
    content     TEXT,
    reasoning   TEXT,
    prompt_token_usage BIGINT,
    completion_token_usage BIGINT,
    total_token_usage BIGINT,
    sequence    INTEGER,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE api_usage_logs (
    id         BIGSERIAL    PRIMARY KEY,
    api_type   VARCHAR(30)  NOT NULL,
    model      VARCHAR(50)  NOT NULL,
    input_tokens  INT       NOT NULL DEFAULT 0,
    output_tokens INT       NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(10, 6) NOT NULL DEFAULT 0,
    called_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_usage_type   ON api_usage_logs (api_type);
CREATE INDEX idx_api_usage_called ON api_usage_logs (called_at DESC);

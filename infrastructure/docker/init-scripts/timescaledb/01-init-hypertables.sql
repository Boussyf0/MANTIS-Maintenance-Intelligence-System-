-- MANTIS TimescaleDB Initialization Script
-- Hypertables pour séries temporelles haute fréquence

-- Extension TimescaleDB
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Table pour données brutes capteurs
CREATE TABLE IF NOT EXISTS sensor_data_raw (
    time TIMESTAMPTZ NOT NULL,
    asset_id UUID NOT NULL,
    sensor_id UUID NOT NULL,
    sensor_code VARCHAR(50) NOT NULL,
    sensor_type VARCHAR(50) NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(20),
    quality INTEGER DEFAULT 100, -- 0-100, qualité du signal
    metadata JSONB
);

-- Convertir en hypertable (partitionnement par temps)
SELECT create_hypertable('sensor_data_raw', 'time', if_not_exists => TRUE);

-- Index pour requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx_sensor_data_raw_asset_sensor ON sensor_data_raw (asset_id, sensor_id, time DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_data_raw_sensor_code ON sensor_data_raw (sensor_code, time DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_data_raw_sensor_type ON sensor_data_raw (sensor_type, time DESC);

-- Politique de rétention: garder les données brutes 90 jours
SELECT add_retention_policy('sensor_data_raw', INTERVAL '90 days', if_not_exists => TRUE);

-- Table pour données pré-traitées (fenêtres)
CREATE TABLE IF NOT EXISTS sensor_data_windowed (
    time TIMESTAMPTZ NOT NULL,
    asset_id UUID NOT NULL,
    sensor_id UUID NOT NULL,
    sensor_code VARCHAR(50) NOT NULL,
    sensor_type VARCHAR(50) NOT NULL,
    window_size_seconds INTEGER NOT NULL,
    mean DOUBLE PRECISION,
    std DOUBLE PRECISION,
    min DOUBLE PRECISION,
    max DOUBLE PRECISION,
    median DOUBLE PRECISION,
    count INTEGER,
    metadata JSONB
);

SELECT create_hypertable('sensor_data_windowed', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_sensor_data_windowed_asset_sensor ON sensor_data_windowed (asset_id, sensor_id, time DESC);

-- Rétention: 180 jours pour données fenêtrées
SELECT add_retention_policy('sensor_data_windowed', INTERVAL '180 days', if_not_exists => TRUE);

-- Table pour features calculées
CREATE TABLE IF NOT EXISTS sensor_features (
    time TIMESTAMPTZ NOT NULL,
    asset_id UUID NOT NULL,
    sensor_id UUID NOT NULL,
    feature_set VARCHAR(100) NOT NULL, -- 'time_domain', 'frequency_domain', 'statistical', 'wavelet'
    features JSONB NOT NULL, -- Toutes les features en JSON
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    metadata JSONB
);

SELECT create_hypertable('sensor_features', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_sensor_features_asset ON sensor_features (asset_id, time DESC);
CREATE INDEX IF NOT EXISTS idx_sensor_features_feature_set ON sensor_features (feature_set, time DESC);

-- Rétention: 1 an pour features
SELECT add_retention_policy('sensor_features', INTERVAL '365 days', if_not_exists => TRUE);

-- Table pour scores d'anomalies (time series)
CREATE TABLE IF NOT EXISTS anomaly_scores (
    time TIMESTAMPTZ NOT NULL,
    asset_id UUID NOT NULL,
    sensor_id UUID,
    detector_type VARCHAR(100) NOT NULL, -- 'isolation_forest', 'autoencoder', 'one_class_svm'
    anomaly_score DOUBLE PRECISION NOT NULL,
    is_anomaly BOOLEAN,
    threshold DOUBLE PRECISION,
    contributing_features JSONB,
    model_version VARCHAR(50)
);

SELECT create_hypertable('anomaly_scores', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_anomaly_scores_asset ON anomaly_scores (asset_id, time DESC);
CREATE INDEX IF NOT EXISTS idx_anomaly_scores_is_anomaly ON anomaly_scores (is_anomaly, time DESC);

-- Rétention: 180 jours
SELECT add_retention_policy('anomaly_scores', INTERVAL '180 days', if_not_exists => TRUE);

-- Table pour prédictions RUL time series
CREATE TABLE IF NOT EXISTS rul_predictions_ts (
    time TIMESTAMPTZ NOT NULL,
    asset_id UUID NOT NULL,
    rul_hours DOUBLE PRECISION NOT NULL,
    rul_confidence_lower DOUBLE PRECISION,
    rul_confidence_upper DOUBLE PRECISION,
    confidence_level DOUBLE PRECISION,
    model_version VARCHAR(50),
    degradation_rate DOUBLE PRECISION, -- Taux de dégradation estimé
    health_index DOUBLE PRECISION, -- 0-100
    metadata JSONB
);

SELECT create_hypertable('rul_predictions_ts', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_rul_predictions_ts_asset ON rul_predictions_ts (asset_id, time DESC);

-- Rétention: 1 an
SELECT add_retention_policy('rul_predictions_ts', INTERVAL '365 days', if_not_exists => TRUE);

-- Table pour événements système
CREATE TABLE IF NOT EXISTS system_events (
    time TIMESTAMPTZ NOT NULL,
    event_type VARCHAR(100) NOT NULL, -- 'model_deployed', 'alert_sent', 'work_order_created', etc.
    source_service VARCHAR(100) NOT NULL,
    asset_id UUID,
    severity VARCHAR(20),
    message TEXT,
    details JSONB
);

SELECT create_hypertable('system_events', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_system_events_type ON system_events (event_type, time DESC);
CREATE INDEX IF NOT EXISTS idx_system_events_severity ON system_events (severity, time DESC);

-- Rétention: 90 jours
SELECT add_retention_policy('system_events', INTERVAL '90 days', if_not_exists => TRUE);

-- Vues matérialisées continues pour agrégations

-- Agrégation horaire des données capteurs
CREATE MATERIALIZED VIEW IF NOT EXISTS sensor_data_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    asset_id,
    sensor_id,
    sensor_code,
    sensor_type,
    AVG(value) as avg_value,
    STDDEV(value) as stddev_value,
    MIN(value) as min_value,
    MAX(value) as max_value,
    COUNT(*) as sample_count
FROM sensor_data_raw
GROUP BY bucket, asset_id, sensor_id, sensor_code, sensor_type
WITH NO DATA;

-- Politique de rafraîchissement: toutes les 5 minutes
SELECT add_continuous_aggregate_policy('sensor_data_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '5 minutes',
    schedule_interval => INTERVAL '5 minutes',
    if_not_exists => TRUE);

-- Agrégation quotidienne
CREATE MATERIALIZED VIEW IF NOT EXISTS sensor_data_daily
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS bucket,
    asset_id,
    sensor_id,
    sensor_code,
    sensor_type,
    AVG(value) as avg_value,
    STDDEV(value) as stddev_value,
    MIN(value) as min_value,
    MAX(value) as max_value,
    COUNT(*) as sample_count
FROM sensor_data_raw
GROUP BY bucket, asset_id, sensor_id, sensor_code, sensor_type
WITH NO DATA;

SELECT add_continuous_aggregate_policy('sensor_data_daily',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);

-- Agrégation des anomalies par heure
CREATE MATERIALIZED VIEW IF NOT EXISTS anomalies_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    asset_id,
    detector_type,
    COUNT(*) FILTER (WHERE is_anomaly = TRUE) as anomaly_count,
    AVG(anomaly_score) as avg_score,
    MAX(anomaly_score) as max_score
FROM anomaly_scores
GROUP BY bucket, asset_id, detector_type
WITH NO DATA;

SELECT add_continuous_aggregate_policy('anomalies_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '5 minutes',
    schedule_interval => INTERVAL '5 minutes',
    if_not_exists => TRUE);

-- Compression automatique des anciennes données
-- Compresser les données de plus de 7 jours
SELECT add_compression_policy('sensor_data_raw', INTERVAL '7 days', if_not_exists => TRUE);
SELECT add_compression_policy('sensor_data_windowed', INTERVAL '7 days', if_not_exists => TRUE);
SELECT add_compression_policy('sensor_features', INTERVAL '30 days', if_not_exists => TRUE);
SELECT add_compression_policy('anomaly_scores', INTERVAL '14 days', if_not_exists => TRUE);
SELECT add_compression_policy('rul_predictions_ts', INTERVAL '30 days', if_not_exists => TRUE);

-- Fonction helper pour insérer des données capteurs avec validation
CREATE OR REPLACE FUNCTION insert_sensor_data(
    p_time TIMESTAMPTZ,
    p_asset_id UUID,
    p_sensor_id UUID,
    p_sensor_code VARCHAR,
    p_sensor_type VARCHAR,
    p_value DOUBLE PRECISION,
    p_unit VARCHAR DEFAULT NULL,
    p_quality INTEGER DEFAULT 100,
    p_metadata JSONB DEFAULT NULL
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO sensor_data_raw (time, asset_id, sensor_id, sensor_code, sensor_type, value, unit, quality, metadata)
    VALUES (p_time, p_asset_id, p_sensor_id, p_sensor_code, p_sensor_type, p_value, p_unit, p_quality, p_metadata);
END;
$$ LANGUAGE plpgsql;

-- Vue pour dernières valeurs capteurs (optimisée avec index)
CREATE OR REPLACE VIEW latest_sensor_readings AS
SELECT DISTINCT ON (asset_id, sensor_id)
    time,
    asset_id,
    sensor_id,
    sensor_code,
    sensor_type,
    value,
    unit,
    quality
FROM sensor_data_raw
ORDER BY asset_id, sensor_id, time DESC;

COMMENT ON VIEW latest_sensor_readings IS 'Dernières lectures de chaque capteur pour affichage temps-réel';

-- Vue pour health summary
CREATE OR REPLACE VIEW asset_health_summary AS
SELECT
    a.asset_id,
    AVG(a.anomaly_score) as avg_anomaly_score,
    MAX(a.anomaly_score) as max_anomaly_score,
    COUNT(*) FILTER (WHERE a.is_anomaly = TRUE) as recent_anomaly_count,
    r.rul_hours as latest_rul
FROM anomaly_scores a
LEFT JOIN LATERAL (
    SELECT rul_hours
    FROM rul_predictions_ts
    WHERE asset_id = a.asset_id
    ORDER BY time DESC
    LIMIT 1
) r ON TRUE
WHERE a.time > NOW() - INTERVAL '1 hour'
GROUP BY a.asset_id, r.rul_hours;

COMMENT ON VIEW asset_health_summary IS 'Résumé de santé des assets basé sur la dernière heure';

-- MANTIS PostgreSQL Initialization Script
-- Schéma pour métadonnées, assets, maintenance, etc.

-- Extension pour UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Extension pour PostGIS (cartographie d'atelier)
CREATE EXTENSION IF NOT EXISTS postgis;

-- Schema pour assets et équipements
CREATE TABLE IF NOT EXISTS assets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    asset_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL, -- 'motor', 'pump', 'conveyor', 'cnc', etc.
    manufacturer VARCHAR(255),
    model VARCHAR(255),
    serial_number VARCHAR(255),
    installation_date DATE,
    location_line VARCHAR(100),
    location_station VARCHAR(100),
    location_coordinates GEOMETRY(Point, 4326), -- PostGIS pour cartographie
    criticality VARCHAR(20) CHECK (criticality IN ('critical', 'high', 'medium', 'low')),
    status VARCHAR(20) DEFAULT 'operational' CHECK (status IN ('operational', 'degraded', 'failed', 'maintenance', 'decommissioned')),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour recherche rapide
CREATE INDEX idx_assets_type ON assets(type);
CREATE INDEX idx_assets_criticality ON assets(criticality);
CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_assets_location_line ON assets(location_line);
CREATE INDEX idx_assets_metadata ON assets USING GIN(metadata);

-- Table pour configurations de capteurs
CREATE TABLE IF NOT EXISTS sensors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    asset_id UUID REFERENCES assets(id) ON DELETE CASCADE,
    sensor_code VARCHAR(50) UNIQUE NOT NULL,
    sensor_type VARCHAR(50) NOT NULL, -- 'vibration', 'temperature', 'current', 'acoustic', 'pressure'
    unit VARCHAR(20),
    sampling_rate_hz FLOAT,
    opc_ua_node_id VARCHAR(255),
    mqtt_topic VARCHAR(255),
    modbus_address INTEGER,
    threshold_warning FLOAT,
    threshold_critical FLOAT,
    calibration_date DATE,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sensors_asset_id ON sensors(asset_id);
CREATE INDEX idx_sensors_type ON sensors(sensor_type);

-- Table pour BOM (Bill of Materials) - pièces de rechange
CREATE TABLE IF NOT EXISTS spare_parts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    part_number VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    supplier VARCHAR(255),
    unit_price DECIMAL(10, 2),
    lead_time_days INTEGER,
    stock_quantity INTEGER DEFAULT 0,
    stock_min_threshold INTEGER,
    stock_location VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_spare_parts_category ON spare_parts(category);

-- Table de relation asset <-> spare_parts
CREATE TABLE IF NOT EXISTS asset_spare_parts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    asset_id UUID REFERENCES assets(id) ON DELETE CASCADE,
    spare_part_id UUID REFERENCES spare_parts(id) ON DELETE CASCADE,
    quantity_per_maintenance INTEGER DEFAULT 1,
    UNIQUE(asset_id, spare_part_id)
);

-- Table pour anomalies détectées
CREATE TABLE IF NOT EXISTS anomalies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    asset_id UUID REFERENCES assets(id) ON DELETE CASCADE,
    sensor_id UUID REFERENCES sensors(id) ON DELETE SET NULL,
    detection_timestamp TIMESTAMP NOT NULL,
    anomaly_type VARCHAR(100), -- 'statistical', 'pattern', 'threshold', 'model'
    severity VARCHAR(20) CHECK (severity IN ('info', 'warning', 'critical')),
    anomaly_score FLOAT,
    confidence FLOAT,
    description TEXT,
    features JSONB, -- Features contributives
    model_version VARCHAR(50),
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(255),
    acknowledged_at TIMESTAMP,
    resolution_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_anomalies_asset_id ON anomalies(asset_id);
CREATE INDEX idx_anomalies_timestamp ON anomalies(detection_timestamp DESC);
CREATE INDEX idx_anomalies_severity ON anomalies(severity);
CREATE INDEX idx_anomalies_acknowledged ON anomalies(acknowledged);

-- Table pour prédictions RUL
CREATE TABLE IF NOT EXISTS rul_predictions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    asset_id UUID REFERENCES assets(id) ON DELETE CASCADE,
    prediction_timestamp TIMESTAMP NOT NULL,
    rul_hours FLOAT NOT NULL,
    rul_confidence_lower FLOAT,
    rul_confidence_upper FLOAT,
    confidence_level FLOAT, -- 0-1
    failure_probability_24h FLOAT,
    failure_probability_7d FLOAT,
    failure_probability_30d FLOAT,
    model_version VARCHAR(50),
    model_type VARCHAR(50), -- 'lstm', 'gru', 'tcn', 'xgboost'
    features JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rul_asset_id ON rul_predictions(asset_id);
CREATE INDEX idx_rul_timestamp ON rul_predictions(prediction_timestamp DESC);

-- Table pour ordres de travail (work orders)
CREATE TABLE IF NOT EXISTS work_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    work_order_number VARCHAR(50) UNIQUE NOT NULL,
    asset_id UUID REFERENCES assets(id) ON DELETE SET NULL,
    type VARCHAR(50) CHECK (type IN ('corrective', 'preventive', 'predictive', 'inspection')),
    priority VARCHAR(20) CHECK (priority IN ('emergency', 'high', 'medium', 'low')),
    status VARCHAR(50) DEFAULT 'open' CHECK (status IN ('open', 'assigned', 'in_progress', 'on_hold', 'completed', 'cancelled')),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    scheduled_start TIMESTAMP,
    scheduled_end TIMESTAMP,
    actual_start TIMESTAMP,
    actual_end TIMESTAMP,
    assigned_to VARCHAR(255),
    estimated_hours FLOAT,
    actual_hours FLOAT,
    cost_estimated DECIMAL(10, 2),
    cost_actual DECIMAL(10, 2),
    downtime_hours FLOAT,
    anomaly_id UUID REFERENCES anomalies(id) ON DELETE SET NULL,
    rul_prediction_id UUID REFERENCES rul_predictions(id) ON DELETE SET NULL,
    notes TEXT,
    metadata JSONB,
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_work_orders_asset_id ON work_orders(asset_id);
CREATE INDEX idx_work_orders_status ON work_orders(status);
CREATE INDEX idx_work_orders_priority ON work_orders(priority);
CREATE INDEX idx_work_orders_type ON work_orders(type);
CREATE INDEX idx_work_orders_scheduled_start ON work_orders(scheduled_start);

-- Table pour pièces utilisées dans les work orders
CREATE TABLE IF NOT EXISTS work_order_parts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    work_order_id UUID REFERENCES work_orders(id) ON DELETE CASCADE,
    spare_part_id UUID REFERENCES spare_parts(id) ON DELETE CASCADE,
    quantity_planned INTEGER,
    quantity_used INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table pour historique de maintenance
CREATE TABLE IF NOT EXISTS maintenance_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    asset_id UUID REFERENCES assets(id) ON DELETE CASCADE,
    work_order_id UUID REFERENCES work_orders(id) ON DELETE SET NULL,
    maintenance_date TIMESTAMP NOT NULL,
    type VARCHAR(50),
    description TEXT,
    performed_by VARCHAR(255),
    duration_hours FLOAT,
    cost DECIMAL(10, 2),
    parts_replaced JSONB,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_maintenance_history_asset_id ON maintenance_history(asset_id);
CREATE INDEX idx_maintenance_history_date ON maintenance_history(maintenance_date DESC);

-- Table pour règles de maintenance
CREATE TABLE IF NOT EXISTS maintenance_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_name VARCHAR(255) UNIQUE NOT NULL,
    asset_type VARCHAR(100), -- NULL = toutes les types
    asset_id UUID REFERENCES assets(id) ON DELETE CASCADE, -- NULL = règle générale
    condition_type VARCHAR(50) CHECK (condition_type IN ('rul_threshold', 'anomaly_score', 'sensor_threshold', 'time_based', 'usage_based')),
    condition_value JSONB NOT NULL,
    action_type VARCHAR(50) CHECK (action_type IN ('create_work_order', 'send_alert', 'order_parts', 'schedule_inspection')),
    action_config JSONB NOT NULL,
    priority VARCHAR(20) CHECK (priority IN ('emergency', 'high', 'medium', 'low')),
    enabled BOOLEAN DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_maintenance_rules_asset_type ON maintenance_rules(asset_type);
CREATE INDEX idx_maintenance_rules_enabled ON maintenance_rules(enabled);

-- Table pour KPIs et métriques
CREATE TABLE IF NOT EXISTS kpi_snapshots (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    snapshot_timestamp TIMESTAMP NOT NULL,
    asset_id UUID REFERENCES assets(id) ON DELETE CASCADE, -- NULL pour KPI global
    kpi_type VARCHAR(100) NOT NULL, -- 'mtbf', 'mttr', 'oee', 'availability', 'performance', 'quality'
    value FLOAT NOT NULL,
    unit VARCHAR(50),
    period VARCHAR(20), -- 'hourly', 'daily', 'weekly', 'monthly'
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kpi_snapshots_asset_id ON kpi_snapshots(asset_id);
CREATE INDEX idx_kpi_snapshots_timestamp ON kpi_snapshots(snapshot_timestamp DESC);
CREATE INDEX idx_kpi_snapshots_type ON kpi_snapshots(kpi_type);

-- Table pour modèles ML (registry complémentaire à MLflow)
CREATE TABLE IF NOT EXISTS ml_models (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    model_name VARCHAR(255) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    model_type VARCHAR(100), -- 'anomaly_detection', 'rul_prediction'
    algorithm VARCHAR(100), -- 'lstm', 'isolation_forest', 'autoencoder', etc.
    asset_type VARCHAR(100), -- NULL = modèle général
    mlflow_run_id VARCHAR(255),
    mlflow_model_uri TEXT,
    performance_metrics JSONB,
    training_dataset VARCHAR(255),
    status VARCHAR(50) CHECK (status IN ('training', 'validation', 'staging', 'production', 'archived')),
    deployed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(model_name, model_version)
);

CREATE INDEX idx_ml_models_name_version ON ml_models(model_name, model_version);
CREATE INDEX idx_ml_models_status ON ml_models(status);
CREATE INDEX idx_ml_models_asset_type ON ml_models(asset_type);

-- Trigger pour mettre à jour updated_at automatiquement
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Appliquer le trigger sur les tables pertinentes
CREATE TRIGGER update_assets_updated_at BEFORE UPDATE ON assets FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_sensors_updated_at BEFORE UPDATE ON sensors FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_spare_parts_updated_at BEFORE UPDATE ON spare_parts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_work_orders_updated_at BEFORE UPDATE ON work_orders FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_maintenance_rules_updated_at BEFORE UPDATE ON maintenance_rules FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_ml_models_updated_at BEFORE UPDATE ON ml_models FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Vue pour tableau de bord (health status global)
CREATE OR REPLACE VIEW assets_health_dashboard AS
SELECT
    a.id,
    a.asset_code,
    a.name,
    a.type,
    a.criticality,
    a.status,
    a.location_line,
    (
        SELECT rul_hours
        FROM rul_predictions
        WHERE asset_id = a.id
        ORDER BY prediction_timestamp DESC
        LIMIT 1
    ) as latest_rul,
    (
        SELECT COUNT(*)
        FROM anomalies
        WHERE asset_id = a.id
        AND acknowledged = FALSE
        AND severity IN ('warning', 'critical')
    ) as open_anomalies,
    (
        SELECT COUNT(*)
        FROM work_orders
        WHERE asset_id = a.id
        AND status IN ('open', 'assigned', 'in_progress')
    ) as open_work_orders
FROM assets a
WHERE a.status != 'decommissioned';

COMMENT ON VIEW assets_health_dashboard IS 'Vue consolidée pour affichage du statut de santé des assets';

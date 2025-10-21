-- Script pour peupler la base de données avec des données d'exemple
-- À exécuter sur PostgreSQL (mantis database)

-- Insérer des assets d'exemple
INSERT INTO assets (asset_code, name, type, manufacturer, model, installation_date, location_line, location_station, criticality, status, metadata) VALUES
('MOTOR-001', 'Moteur Principal Ligne 1', 'motor', 'Siemens', 'SIMOTICS S-1FL6', '2020-03-15', 'line-1', 'station-1', 'critical', 'operational', '{"power_kw": 75, "rpm": 1500}'),
('MOTOR-002', 'Moteur Secondaire Ligne 1', 'motor', 'ABB', 'M3BP 160', '2020-03-15', 'line-1', 'station-2', 'high', 'operational', '{"power_kw": 45, "rpm": 1200}'),
('PUMP-001', 'Pompe Centrifuge Principale', 'pump', 'Grundfos', 'CR 15-8', '2019-11-20', 'line-1', 'station-1', 'critical', 'operational', '{"flow_m3h": 80, "head_m": 120}'),
('PUMP-002', 'Pompe de Circulation', 'pump', 'KSB', 'Etanorm 125-250', '2020-01-10', 'line-2', 'station-1', 'medium', 'operational', '{"flow_m3h": 150, "head_m": 50}'),
('CONV-001', 'Convoyeur Principal A', 'conveyor', 'Interroll', 'RollerDrive EC310', '2020-05-05', 'line-1', 'station-3', 'medium', 'operational', '{"length_m": 25, "speed_mps": 1.5}'),
('CONV-002', 'Convoyeur Secondaire B', 'conveyor', 'Interroll', 'RollerDrive EC310', '2020-05-05', 'line-2', 'station-2', 'low', 'operational', '{"length_m": 15, "speed_mps": 1.0}'),
('CNC-001', 'Centre Usinage CNC 5 Axes', 'cnc', 'DMG MORI', 'DMU 50', '2018-08-01', 'line-3', 'station-1', 'critical', 'operational', '{"axes": 5, "spindle_rpm": 12000}'),
('COMP-001', 'Compresseur Air Principal', 'compressor', 'Atlas Copco', 'GA 90', '2019-06-12', 'utilities', 'compressor-room', 'critical', 'operational', '{"pressure_bar": 8, "flow_m3min": 15}')
ON CONFLICT (asset_code) DO NOTHING;

-- Insérer des capteurs pour chaque asset
-- Moteur 001
INSERT INTO sensors (asset_id, sensor_code, sensor_type, unit, sampling_rate_hz, threshold_warning, threshold_critical, metadata) VALUES
((SELECT id FROM assets WHERE asset_code = 'MOTOR-001'), 'MOTOR-001_TEMP', 'temperature', '°C', 1.0, 80.0, 95.0, '{"location": "stator"}'),
((SELECT id FROM assets WHERE asset_code = 'MOTOR-001'), 'MOTOR-001_VIB', 'vibration', 'mm/s', 1000.0, 7.1, 11.2, '{"axis": "radial", "location": "drive_end"}'),
((SELECT id FROM assets WHERE asset_code = 'MOTOR-001'), 'MOTOR-001_CURR', 'current', 'A', 10.0, 110.0, 125.0, '{"phase": "L1"}'),
((SELECT id FROM assets WHERE asset_code = 'MOTOR-001'), 'MOTOR-001_SPEED', 'speed', 'RPM', 1.0, 1550.0, 1600.0, '{"nominal": 1500}')
ON CONFLICT (sensor_code) DO NOTHING;

-- Pompe 001
INSERT INTO sensors (asset_id, sensor_code, sensor_type, unit, sampling_rate_hz, threshold_warning, threshold_critical, metadata) VALUES
((SELECT id FROM assets WHERE asset_code = 'PUMP-001'), 'PUMP-001_TEMP', 'temperature', '°C', 1.0, 70.0, 85.0, '{"location": "bearing"}'),
((SELECT id FROM assets WHERE asset_code = 'PUMP-001'), 'PUMP-001_VIB', 'vibration', 'mm/s', 1000.0, 4.5, 7.1, '{"axis": "radial"}'),
((SELECT id FROM assets WHERE asset_code = 'PUMP-001'), 'PUMP-001_PRESS_IN', 'pressure', 'bar', 10.0, 3.5, 4.0, '{"location": "inlet"}'),
((SELECT id FROM assets WHERE asset_code = 'PUMP-001'), 'PUMP-001_PRESS_OUT', 'pressure', 'bar', 10.0, 11.0, 13.0, '{"location": "outlet"}'),
((SELECT id FROM assets WHERE asset_code = 'PUMP-001'), 'PUMP-001_FLOW', 'flow', 'm³/h', 1.0, 75.0, 70.0, '{"nominal": 80}')
ON CONFLICT (sensor_code) DO NOTHING;

-- CNC 001
INSERT INTO sensors (asset_id, sensor_code, sensor_type, unit, sampling_rate_hz, threshold_warning, threshold_critical, metadata) VALUES
((SELECT id FROM assets WHERE asset_code = 'CNC-001'), 'CNC-001_SPINDLE_TEMP', 'temperature', '°C', 1.0, 65.0, 80.0, '{"location": "spindle_bearing"}'),
((SELECT id FROM assets WHERE asset_code = 'CNC-001'), 'CNC-001_SPINDLE_VIB', 'vibration', 'mm/s', 1000.0, 3.5, 5.6, '{"location": "spindle"}'),
((SELECT id FROM assets WHERE asset_code = 'CNC-001'), 'CNC-001_SPINDLE_CURR', 'current', 'A', 10.0, 45.0, 55.0, '{"motor": "spindle"}'),
((SELECT id FROM assets WHERE asset_code = 'CNC-001'), 'CNC-001_COOLANT_TEMP', 'temperature', '°C', 1.0, 30.0, 35.0, '{"location": "coolant_tank"}')
ON CONFLICT (sensor_code) DO NOTHING;

-- Insérer des pièces de rechange
INSERT INTO spare_parts (part_number, name, description, category, supplier, unit_price, lead_time_days, stock_quantity, stock_min_threshold, stock_location, metadata) VALUES
('BRG-6308', 'Roulement 6308 SKF', 'Roulement rigide à billes 40x90x23mm', 'bearings', 'SKF France', 45.50, 3, 12, 4, 'A-12-B', '{"type": "deep_groove", "size": "6308"}'),
('BRG-6309', 'Roulement 6309 SKF', 'Roulement rigide à billes 45x100x25mm', 'bearings', 'SKF France', 52.00, 3, 8, 3, 'A-12-C', '{"type": "deep_groove", "size": "6309"}'),
('SEAL-40-62-7', 'Joint SPI 40x62x7', 'Joint à lèvre NBR', 'seals', 'Freudenberg', 8.20, 2, 25, 10, 'A-15-A', '{"material": "NBR", "shaft_dia": 40}'),
('BELT-SPA-1600', 'Courroie SPA 1600', 'Courroie trapézoïdale SPA 1600mm', 'belts', 'Gates', 28.50, 5, 6, 2, 'A-18-D', '{"profile": "SPA", "length_mm": 1600}'),
('FILTER-OIL-HF6555', 'Filtre hydraulique HF6555', 'Filtre à huile hydraulique 10µm', 'filters', 'Fleetguard', 42.00, 7, 10, 3, 'B-05-A', '{"micron": 10, "flow_lpm": 150}'),
('SENSOR-VIBR-IFM', 'Capteur vibration VSE150', 'Capteur vibration 4-20mA', 'sensors', 'IFM Electronic', 285.00, 14, 3, 1, 'C-02-E', '{"output": "4-20mA", "range": "0-25mm/s"}'),
('PUMP-SEAL-KIT', 'Kit joints pompe CR15', 'Kit joints complet pompe Grundfos CR15', 'seals', 'Grundfos', 165.00, 10, 2, 1, 'A-15-C', '{"compatible": "CR15 series"}')
ON CONFLICT (part_number) DO NOTHING;

-- Associer pièces aux assets
INSERT INTO asset_spare_parts (asset_id, spare_part_id, quantity_per_maintenance) VALUES
((SELECT id FROM assets WHERE asset_code = 'MOTOR-001'), (SELECT id FROM spare_parts WHERE part_number = 'BRG-6308'), 2),
((SELECT id FROM assets WHERE asset_code = 'MOTOR-001'), (SELECT id FROM spare_parts WHERE part_number = 'SEAL-40-62-7'), 2),
((SELECT id FROM assets WHERE asset_code = 'PUMP-001'), (SELECT id FROM spare_parts WHERE part_number = 'PUMP-SEAL-KIT'), 1),
((SELECT id FROM assets WHERE asset_code = 'PUMP-001'), (SELECT id FROM spare_parts WHERE part_number = 'BRG-6309'), 2),
((SELECT id FROM assets WHERE asset_code = 'CONV-001'), (SELECT id FROM spare_parts WHERE part_number = 'BELT-SPA-1600'), 1)
ON CONFLICT DO NOTHING;

-- Insérer des règles de maintenance
INSERT INTO maintenance_rules (
    rule_name,
    asset_type,
    condition_type,
    condition_value,
    action_type,
    action_config,
    priority,
    enabled
) VALUES
(
    'RUL Critique Moteur',
    'motor',
    'rul_threshold',
    '{"threshold_hours": 120, "confidence_min": 0.7}'::jsonb,
    'create_work_order',
    '{"type": "predictive", "priority": "high", "estimated_hours": 4}'::jsonb,
    'high',
    true
),
(
    'Anomalie Persistante Pompe',
    'pump',
    'anomaly_score',
    '{"threshold": 0.85, "consecutive_count": 3, "time_window_hours": 24}'::jsonb,
    'send_alert',
    '{"channels": ["email", "slack"], "recipients": ["maintenance@factory.com"]}'::jsonb,
    'medium',
    true
),
(
    'Vibration Critique',
    NULL,
    'sensor_threshold',
    '{"sensor_type": "vibration", "threshold_critical": true}'::jsonb,
    'create_work_order',
    '{"type": "corrective", "priority": "emergency", "estimated_hours": 2}'::jsonb,
    'emergency',
    true
),
(
    'Maintenance Préventive Trimestrielle',
    NULL,
    'time_based',
    '{"interval_days": 90, "last_maintenance_field": "last_preventive_date"}'::jsonb,
    'schedule_inspection',
    '{"type": "preventive", "duration_hours": 1, "checklist_id": "PM-QUARTERLY"}'::jsonb,
    'low',
    true
)
ON CONFLICT (rule_name) DO NOTHING;

-- Insérer un historique de maintenance (exemples passés)
INSERT INTO maintenance_history (
    asset_id,
    maintenance_date,
    type,
    description,
    performed_by,
    duration_hours,
    cost,
    parts_replaced,
    notes
) VALUES
(
    (SELECT id FROM assets WHERE asset_code = 'MOTOR-001'),
    '2024-11-15 08:00:00',
    'preventive',
    'Maintenance préventive trimestrielle - Remplacement roulements',
    'Jean Dupont',
    3.5,
    450.00,
    '[{"part_number": "BRG-6308", "quantity": 2}, {"part_number": "SEAL-40-62-7", "quantity": 2}]'::jsonb,
    'Roulements en bon état, remplacés préventivement. Graissage effectué.'
),
(
    (SELECT id FROM assets WHERE asset_code = 'PUMP-001'),
    '2024-12-05 14:00:00',
    'corrective',
    'Réparation fuite joint - Intervention urgente',
    'Marie Martin',
    2.0,
    280.00,
    '[{"part_number": "PUMP-SEAL-KIT", "quantity": 1}]'::jsonb,
    'Fuite détectée sur joint arbre. Remplacement kit complet. Test OK.'
),
(
    (SELECT id FROM assets WHERE asset_code = 'CNC-001'),
    '2024-10-20 10:00:00',
    'preventive',
    'Maintenance préventive semestrielle CNC',
    'Pierre Leroy',
    5.0,
    850.00,
    '[]'::jsonb,
    'Vérification géométrique, calibration axes, changement huile hydraulique.'
);

-- Insérer des modèles ML (registry)
INSERT INTO ml_models (
    model_name,
    model_version,
    model_type,
    algorithm,
    asset_type,
    mlflow_run_id,
    mlflow_model_uri,
    performance_metrics,
    training_dataset,
    status
) VALUES
(
    'anomaly_detector_motor',
    'v1.0.0',
    'anomaly_detection',
    'isolation_forest',
    'motor',
    'abc123def456',
    's3://mlflow/1/abc123def456/artifacts/model',
    '{"precision": 0.87, "recall": 0.91, "f1": 0.89}'::jsonb,
    'motor_training_2024_q4',
    'production'
),
(
    'rul_predictor_pump',
    'v1.2.0',
    'rul_prediction',
    'lstm',
    'pump',
    'xyz789abc012',
    's3://mlflow/2/xyz789abc012/artifacts/model',
    '{"rmse": 12.5, "mae": 8.3, "r2": 0.84}'::jsonb,
    'NASA_CMAPSS_FD001_transfer',
    'production'
),
(
    'rul_predictor_general',
    'v2.0.0',
    'rul_prediction',
    'tcn',
    NULL,
    'def456ghi789',
    's3://mlflow/3/def456ghi789/artifacts/model',
    '{"rmse": 10.8, "mae": 7.1, "r2": 0.88}'::jsonb,
    'NASA_CMAPSS_FD001_FD002_combined',
    'staging'
);

-- Afficher un résumé
SELECT
    'Assets' as table_name,
    COUNT(*) as count
FROM assets
UNION ALL
SELECT 'Sensors', COUNT(*) FROM sensors
UNION ALL
SELECT 'Spare Parts', COUNT(*) FROM spare_parts
UNION ALL
SELECT 'Maintenance Rules', COUNT(*) FROM maintenance_rules
UNION ALL
SELECT 'Maintenance History', COUNT(*) FROM maintenance_history
UNION ALL
SELECT 'ML Models', COUNT(*) FROM ml_models;

-- Afficher les assets avec leurs capteurs
SELECT
    a.asset_code,
    a.name,
    a.type,
    a.criticality,
    a.status,
    COUNT(s.id) as sensor_count
FROM assets a
LEFT JOIN sensors s ON a.id = s.asset_id
GROUP BY a.id, a.asset_code, a.name, a.type, a.criticality, a.status
ORDER BY a.asset_code;

COMMENT ON TABLE assets IS 'Données d''exemple insérées avec succès!';

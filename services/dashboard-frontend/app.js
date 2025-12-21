/**
 * MANTIS Dashboard Application
 * Real-time predictive maintenance visualization
 */

// Configuration
const CONFIG = {
    API_BASE_URL: 'http://127.0.0.1:8007/api',  // Dashboard API
    REFRESH_INTERVAL: 2000,  // 2 seconds
    MAX_CHART_POINTS: 30,
    MAX_EVENTS: 50
};

// Application State
const state = {
    machines: {},
    events: [],
    rulHistory: {},
    sensorHistory: {},
    connected: false
};

// Initialize Charts
let rulChart, sensorChart;

function initCharts() {
    // RUL Chart
    const rulCtx = document.getElementById('rul-chart').getContext('2d');
    rulChart = new Chart(rulCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'Machine 001',
                    data: [],
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    tension: 0.3,
                    fill: true
                },
                {
                    label: 'Machine 002',
                    data: [],
                    borderColor: '#22c55e',
                    backgroundColor: 'rgba(34, 197, 94, 0.1)',
                    tension: 0.3,
                    fill: true
                },
                {
                    label: 'Machine 003',
                    data: [],
                    borderColor: '#8b5cf6',
                    backgroundColor: 'rgba(139, 92, 246, 0.1)',
                    tension: 0.3,
                    fill: true
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: { color: '#94a3b8' }
                }
            },
            scales: {
                x: {
                    ticks: { color: '#94a3b8' },
                    grid: { color: 'rgba(148, 163, 184, 0.1)' }
                },
                y: {
                    title: { display: true, text: 'RUL (cycles)', color: '#94a3b8' },
                    ticks: { color: '#94a3b8' },
                    grid: { color: 'rgba(148, 163, 184, 0.1)' },
                    min: 0
                }
            }
        }
    });

    // Sensor Chart
    const sensorCtx = document.getElementById('sensor-chart').getContext('2d');
    sensorChart = new Chart(sensorCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'Sensor 01',
                    data: [],
                    borderColor: '#ef4444',
                    tension: 0.2,
                    pointRadius: 0
                },
                {
                    label: 'Sensor 02',
                    data: [],
                    borderColor: '#22c55e',
                    tension: 0.2,
                    pointRadius: 0
                },
                {
                    label: 'Sensor 03',
                    data: [],
                    borderColor: '#3b82f6',
                    tension: 0.2,
                    pointRadius: 0
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: { color: '#94a3b8' }
                }
            },
            scales: {
                x: {
                    ticks: { color: '#94a3b8', maxTicksLimit: 10 },
                    grid: { color: 'rgba(148, 163, 184, 0.1)' }
                },
                y: {
                    title: { display: true, text: 'Valeur', color: '#94a3b8' },
                    ticks: { color: '#94a3b8' },
                    grid: { color: 'rgba(148, 163, 184, 0.1)' }
                }
            }
        }
    });
}

// Update Time Display
function updateTime() {
    const now = new Date();
    document.getElementById('current-time').textContent = now.toLocaleTimeString('fr-FR');
}

// Add Event to Log
function addEvent(message, type = 'info') {
    const now = new Date().toLocaleTimeString('fr-FR');
    state.events.unshift({ time: now, message, type });

    if (state.events.length > CONFIG.MAX_EVENTS) {
        state.events.pop();
    }

    renderEvents();
}

// Render Events
function renderEvents() {
    const container = document.getElementById('events-container');
    container.innerHTML = state.events.map(event => `
        <div class="event-item ${event.type}">
            <span class="event-time">${event.time}</span>
            <span class="event-message">${event.message}</span>
        </div>
    `).join('');
}

// Render Machines
function renderMachines() {
    const container = document.getElementById('machines-container');
    const machines = Object.values(state.machines);

    if (machines.length === 0) {
        container.innerHTML = `
            <div class="machine-card">
                <div class="machine-name">En attente...</div>
                <div class="machine-stats">
                    <div class="machine-stat">
                        <span>Aucune donnée reçue</span>
                    </div>
                </div>
            </div>
        `;
        return;
    }

    container.innerHTML = machines.map(machine => {
        const rul = machine.rul || 0;
        const rulPercent = Math.min(100, (rul / 200) * 100);
        const statusClass = rul < 30 ? 'critical' : rul < 100 ? 'warning' : '';
        const fillClass = rul < 30 ? 'critical' : rul < 100 ? 'warning' : 'healthy';

        return `
            <div class="machine-card ${statusClass}">
                <div class="machine-name">${machine.id}</div>
                <div class="machine-stats">
                    <div class="machine-stat">
                        <span>Cycle</span>
                        <span>${machine.cycle || 0}</span>
                    </div>
                    <div class="machine-stat">
                        <span>RUL</span>
                        <span>${rul} cycles</span>
                    </div>
                    <div class="machine-stat">
                        <span>Status</span>
                        <span>${machine.status || 'OK'}</span>
                    </div>
                </div>
                <div class="rul-bar">
                    <div class="rul-fill ${fillClass}" style="width: ${rulPercent}%"></div>
                </div>
            </div>
        `;
    }).join('');
}

// Update Stats
function updateStats() {
    const machines = Object.values(state.machines);
    const criticalCount = machines.filter(m => (m.rul || 0) < 30).length;
    const alertCount = machines.filter(m => (m.rul || 0) < 100 && (m.rul || 0) >= 30).length;

    document.getElementById('total-machines').textContent = machines.length || 0;
    document.getElementById('total-alerts').textContent = alertCount;
    document.getElementById('critical-count').textContent = criticalCount;
}

// Update Charts with New Data
function updateCharts(time) {
    // RUL Chart
    rulChart.data.labels.push(time);
    if (rulChart.data.labels.length > CONFIG.MAX_CHART_POINTS) {
        rulChart.data.labels.shift();
    }

    ['machine_001', 'machine_002', 'machine_003'].forEach((machineId, index) => {
        const machine = state.machines[machineId];
        const rul = machine ? machine.rul : null;

        rulChart.data.datasets[index].data.push(rul);
        if (rulChart.data.datasets[index].data.length > CONFIG.MAX_CHART_POINTS) {
            rulChart.data.datasets[index].data.shift();
        }
    });

    rulChart.update('none');

    // Sensor Chart
    sensorChart.data.labels.push(time);
    if (sensorChart.data.labels.length > CONFIG.MAX_CHART_POINTS) {
        sensorChart.data.labels.shift();
    }

    const machine = state.machines['machine_001'];
    if (machine && machine.sensors) {
        ['sensor_01', 'sensor_02', 'sensor_03'].forEach((sensorId, index) => {
            const value = machine.sensors[sensorId] || null;
            sensorChart.data.datasets[index].data.push(value);
            if (sensorChart.data.datasets[index].data.length > CONFIG.MAX_CHART_POINTS) {
                sensorChart.data.datasets[index].data.shift();
            }
        });
    }

    sensorChart.update('none');
}

// Fetch real data from API
async function fetchData() {
    try {
        // Check health
        const healthResponse = await fetch(`${CONFIG.API_BASE_URL.replace('/api', '')}/actuator/health?t=${new Date().getTime()}`, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });

        if (healthResponse.ok) {
            document.getElementById('api-status').classList.add('connected');
            state.connected = true;

            // Fetch Machines
            const machinesResponse = await fetch(`${CONFIG.API_BASE_URL}/machines`);
            if (machinesResponse.ok) {
                const machinesData = await machinesResponse.json();

                // Update state
                machinesData.forEach(m => {
                    const machineId = m.machineId;

                    if (!state.machines[machineId]) {
                        state.machines[machineId] = {
                            id: machineId,
                            cycle: m.cycle || 0,
                            rul: m.lastRul,
                            status: m.status,
                            sensors: {} // Not provided by API
                        };
                    } else {
                        state.machines[machineId].rul = m.lastRul;
                        state.machines[machineId].status = m.status;
                        state.machines[machineId].cycle = m.cycle || 0;
                    }
                });

                renderMachines();
                updateStats();
                updateCharts(new Date().toLocaleTimeString('fr-FR'));
            }

            // Fetch Alerts
            const alertsResponse = await fetch(`${CONFIG.API_BASE_URL}/alerts`);
            if (alertsResponse.ok) {
                const alertsData = await alertsResponse.json();
                // Only add new alerts (simple check)
                alertsData.forEach(alert => {
                    // In a real app, we'd check IDs. For now, just logging latest if not present
                    // This is a simplified logic
                });
            }

        } else {
            throw new Error('API Health Check Failed');
        }
    } catch (error) {
        console.error('Error fetching data:', error);
        addEvent(`Erreur connexion API: ${error.message}`, 'critical');
        document.getElementById('api-status').classList.remove('connected');
        state.connected = false;
    }
}

// Initialize Application
function init() {
    console.log('🏭 MANTIS Dashboard initializing...');

    initCharts();
    updateTime();

    // Add initial event
    addEvent('Dashboard initialisé - En attente de données capteurs...', 'info');

    // Set Kafka status as connected (for demo)
    document.getElementById('kafka-status').classList.add('connected');

    // Start update loops
    setInterval(updateTime, 1000);
    setInterval(fetchData, CONFIG.REFRESH_INTERVAL);

    // Initial data fetch
    fetchData();

    console.log('✅ Dashboard ready!');
}

// Start the application
document.addEventListener('DOMContentLoaded', init);

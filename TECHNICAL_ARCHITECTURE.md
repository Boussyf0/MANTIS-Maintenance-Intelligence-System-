# Comprehensive Technical Architecture: Data Mining & AI Pipeline

This document details the advanced data mining, machine learning, and deep learning components of the MANTIS Predictive Maintenance System.

## 1. Data Optimization Strategy: Sensor Reduction
The MANTIS pipeline implements an optimized feature selection strategy derived from rigorous exploratory data analysis (EDA) on the NASA C-MAPSS dataset.

### 1.1 The Logic: 21 Sensors $\to$ 14 Sensors
The standard C-MAPSS dataset provides telemetry from 21 on-board sensors. However, our analysis reveals that not all sensors contribute useful information for degradation modeling.

*   **Constant Sensors**: Sensors 1, 5, 10, 16, 18, and 19 exhibit zero or near-zero variance across operating cycles. They represent constant operating conditions rather than component health.
*   **Noise/Redundancy**: Sensor 6 is highly correlated with others and adds no unique discriminative power.

**Implementation**:
To improve model training stability and reduce computational overhead, we filter the input vector to keep only the **14 useful sensors**:
*   **Kept Indices**: 2, 3, 4, 7, 8, 9, 11, 12, 13, 14, 15, 17, 20, 21 (Mapped to 0-based indices in code).
*   **Benefit**: This reduces the "Curse of Dimensionality" and focuses the AI models on the signals that actually correlate with Remaining Useful Life (RUL).

## 2. Feature Engineering
Instead of feeding raw sensor values directly into the models, we compute high-level statistical features from the filtered sensor vector. This transforms the problem from "raw signal processing" to "statistical health monitoring."

### 2.1 Mathematical Definitions

Let $X = \{x_1, x_2, \dots, x_{14}\}$ be the vector of the 14 "mean" sensor values at a given timestep.

#### **1. Energy**
**Description**: Represents the total magnitude or intensity of the sensor readings. In rotating machinery, increased energy often correlates with higher vibration or stress levels.
**Formula**:
$$ E = \sum_{i=1}^{14} x_i^2 $$
*In code*: `np.sum(means**2)`

#### **2. Skewness**
**Description**: Measures the asymmetry of the sensor value distribution around its mean. A healthy systematic operation typically yields a symmetric distribution. Degradation can cause values to skew towards failure thresholds.
**Formula**:
$$ \text{Skew} = \frac{E[(X - \mu)^3]}{\sigma^3} $$
Where $\mu$ is the mean of $X$ and $\sigma$ is the standard deviation.
*In code*: `scipy.stats.skew(means)`

#### **3. Kurtosis**
**Description**: Measures the "tailedness" of the distribution. It detects outliers or extreme values in the sensor array. High kurtosis indicates that the variance is the result of infrequent extreme deviations as opposed to frequent modestly sized deviations.
**Formula**:
$$ \text{Kurt} = \frac{E[(X - \mu)^4]}{\sigma^4} - 3 $$
*In code*: `scipy.stats.kurtosis(means)` (Fisher definition, excess kurtosis)

#### **4. Signal-to-Noise Ratio (SNR)**
**Description**: We approximate SNR for each sensor as the ratio of its Mean to its Standard Deviation over the temporal window. We then average this across all 14 sensors. A dropping SNR indicates that the useful signal is being drowned out by increasing noise (a hallmark of wear).
**Formula**:
$$ \text{SNR}_{avg} = \frac{1}{14} \sum_{i=1}^{14} \frac{\mu_i}{\sigma_i} $$
*In code*: `np.mean(means / stds)`

## 3. Anomaly Detection: Isolation Forest
We employ an unsupervised learning approach to detect operating anomalies *before* they lead to failure.

### 3.1 Method: Random Partitioning
Masking anomalies as "outliers," the Isolation Forest algorithm isolates observations by randomly selecting a feature and then randomly selecting a split value between the maximum and minimum values of the selected feature.
*   **Recursive Splitting**: Since recursive partitioning can be represented by a tree structure, the number of splittings required to isolate a sample is equivalent to the path length from the root node to the terminating node.
*   **Result**: Anomalies are fundamentally "few and different," so they are isolated much faster (shorter path lengths) than normal points.

### 3.2 Configuration & Results
*   **Algorithm**: `sklearn.ensemble.IsolationForest`
*   **Contamination**: `0.05` (We assume ~5% of online data might be anomalous/noisy).
*   **Input**: The 4 Advanced Features (Skewness, Kurtosis, Energy, SNR).
*   **Result**: The detector outputs a binary `is_anomaly` flag and an `anomaly_score`. Scores < 0 indicate a potential fault, triggering alerts in the Orchestrator.

## 4. Remaining Useful Life (RUL) Prediction: LSTM
For the core predictive task, we use a Long Short-Term Memory (LSTM) network, a type of Recurrent Neural Network (RNN).

### 4.1 Method: Backpropagation Through Time (BPTT)
Engine degradation is a **temporal process**. The health of the engine at cycle $t$ is dependent on its state at $t-1, t-2, \dots$. Standard Feed-Forward networks lose this context.
*   **Memory Cells**: LSTMs mitigate the "vanishing gradient" problem of standard RNNs using:
    *   **Input Gate**: Which information to update.
    *   **Forget Gate**: Which information to throw away from the cell state.
    *   **Output Gate**: What to output based on input and cell state.
*   **Training**: The model is trained using Backpropagation Through Time to minimize the Mean Squared Error (MSE) between the predicted RUL and the actual RUL in the training set.

### 4.2 Architecture Details
*   **Model**: Stacked LSTM.
    *   **Input Layer**: Accepts size 4 (The Advanced Features).
    *   **Hidden Layers**: 2 Layers of 64 units each. multiple layers allow the model to learn representations at different time scales.
    *   **Output Layer**: Linear Fully Connected layer projecting to a scalar (RUL).
*   **Framework**: PyTorch.

### 4.3 Integration Flow
```mermaid
graph LR
    A[Raw Sensor Data (21)] -->|Filter| B[Useful Sensors (14)]
    B -->|Compute Stats| C[Advanced Features (4)]
    C --> D{Isolation Forest}
    C --> E[LSTM Network]
    D -- Anomaly? --> F[Alert Dashboard]
    E -- RUL Estimate --> G[Maintenance Schedule]
```

## 5. Conclusion & Results
By combining **statistical feature engineering** (Sensor Reduction), **unsupervised monitoring** (Isolation Forest), and **deep temporal learning** (LSTM), the MANTIS pipeline achieves:
1.  **Robustness**: Filtering noisy sensors prevents false alarms.
2.  **Early Warning**: Isolation Forest detects "unknown unknowns"—faults we haven't trained for.
3.  **Precision**: LSTM provides accurate countdown-to-failure (RUL), enabling Just-In-Time maintenance.

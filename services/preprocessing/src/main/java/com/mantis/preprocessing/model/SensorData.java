package com.mantis.preprocessing.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
    private String machine_id;
    private String timestamp;
    private int cycle;
    private List<Double> sensors;
}

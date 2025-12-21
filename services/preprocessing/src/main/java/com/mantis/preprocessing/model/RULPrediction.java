package com.mantis.preprocessing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RULPrediction {
    private String machine_id;
    private String timestamp;
    private int cycle;
    private double predicted_rul;
}

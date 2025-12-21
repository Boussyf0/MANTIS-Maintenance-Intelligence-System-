package com.mantis.preprocessing.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProcessedData {
    private String machine_id;
    private String timestamp;
    private int cycle;
    private Double actual_rul;
    private Map<String, List<Double>> features;
}

package com.mantis.dashboard.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertEntity {
    @Id
    private String alertId;
    private String machineId;
    private String timestamp;
    private String severity;
    private String message;
    private String source;
}

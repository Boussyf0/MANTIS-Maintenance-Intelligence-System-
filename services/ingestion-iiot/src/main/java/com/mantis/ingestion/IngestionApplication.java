package com.mantis.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application principale pour le service d'ingestion IIoT MANTIS.
 *
 * Ce service collecte des données depuis des protocoles industriels (OPC UA, MQTT, Modbus)
 * et les publie vers Kafka pour traitement ultérieur.
 *
 * @author MANTIS Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class IngestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionApplication.class, args);
    }
}

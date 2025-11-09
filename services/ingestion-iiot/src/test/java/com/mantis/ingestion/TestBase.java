package com.mantis.ingestion;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for all tests.
 *
 * Provides common configuration and utilities for test classes.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class TestBase {

    /**
     * Helper method to sleep for a short duration in tests.
     *
     * @param millis milliseconds to sleep
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

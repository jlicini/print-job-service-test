package com.adobe.printservice.service;

import com.adobe.printservice.dto.HealthResponseDTO;
import com.adobe.printservice.model.HealthStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;
    private final ThreadPoolTaskExecutor jobTaskExecutor;

    public HealthService(
            JdbcTemplate jdbcTemplate,
            ThreadPoolTaskExecutor jobTaskExecutor
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobTaskExecutor = jobTaskExecutor;
    }

    public HealthResponseDTO readiness() {
        HealthStatus database = databaseStatus();
        HealthStatus worker = workerStatus();
        HealthStatus status = database == HealthStatus.UP && worker == HealthStatus.UP
                ? HealthStatus.UP
                : HealthStatus.DOWN;

        return new HealthResponseDTO(status, database, worker);
    }

    private HealthStatus databaseStatus() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(result) ? HealthStatus.UP : HealthStatus.DOWN;
        } catch (DataAccessException exception) {
            return HealthStatus.DOWN;
        }
    }

    private HealthStatus workerStatus() {
        ThreadPoolExecutor executor = jobTaskExecutor.getThreadPoolExecutor();
        return executor.isShutdown() || executor.isTerminating() || executor.isTerminated()
                ? HealthStatus.DOWN
                : HealthStatus.UP;
    }
}

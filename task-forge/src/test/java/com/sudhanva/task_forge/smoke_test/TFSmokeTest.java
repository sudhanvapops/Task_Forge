package com.sudhanva.task_forge.smoke_test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// @SpringBootTest 
@Testcontainers 
public class TFSmokeTest {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> pg = 
        new PostgreSQLContainer<>("postgres:16-alpine");


    @Autowired
    JdbcTemplate jdbc;

    // @Test
    void dbIsReachableAndFlywayRan(){

        Integer applied = jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE success = true",
            Integer.class);
        assertTrue(applied >= 1,"db connection and flyway migartion failed");
    }

}

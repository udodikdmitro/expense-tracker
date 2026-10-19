package com.example.expensetracker;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExpenseTrackerApplication {

    public static void main(String[] args) {
        // macOS reports the legacy zone id "Europe/Kiev", which PostgreSQL 16 rejects on connect.
        // Set the current id before any DB connection is opened.
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"));
        SpringApplication.run(ExpenseTrackerApplication.class, args);
    }
}

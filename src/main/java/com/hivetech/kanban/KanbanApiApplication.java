package com.hivetech.kanban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class KanbanApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KanbanApiApplication.class, args);
    }
}


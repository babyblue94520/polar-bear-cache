package pers.clare.polarbearcache.impl;

import pers.clare.polarbeartest.ApplicationTest;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;


@TestInstance(PER_CLASS)
abstract class AbstractAllManagerTest {
    private final List<ApplicationContext> applications = new ArrayList<>();

    abstract String getScope();

    @BeforeAll
    void before() {
        String topic = String.format("--polar-bear-cache.topic=test.%s.%s", getScope(), UUID.randomUUID());
        for (int i = 0; i < 3; i++) {
            applications.add(SpringApplication.run(ApplicationTest.class, topic));
        }
    }

    @AfterAll
    void after() {
        for (ApplicationContext application : applications) {
            SpringApplication.exit(application);
        }
    }
}

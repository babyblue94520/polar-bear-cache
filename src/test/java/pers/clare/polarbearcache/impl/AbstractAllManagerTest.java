package pers.clare.polarbearcache.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import pers.clare.polarbeartest.ApplicationTest2;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;


@TestInstance(PER_CLASS)
abstract class AbstractAllManagerTest {
    private final List<ApplicationContext> applications = new ArrayList<>();

    abstract String getScope();

    @BeforeAll
    void before() {
        for (int i = 0; i < 3; i++) {
            applications.add(SpringApplication.run(ApplicationTest2.class));
        }
    }

    @AfterAll
    void after() {
        for (ApplicationContext application : applications) {
            SpringApplication.exit(application);
        }
    }
}

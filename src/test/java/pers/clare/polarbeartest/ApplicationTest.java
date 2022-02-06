package pers.clare.polarbeartest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pers.clare.polarbearcache.EnablePolarBearCache;

@EnablePolarBearCache
@SpringBootApplication
public class ApplicationTest {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationTest.class, args);
    }

}

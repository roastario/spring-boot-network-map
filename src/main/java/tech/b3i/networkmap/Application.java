package tech.b3i.networkmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        System.setProperty("logging.level.org.springframework.web", "DEBUG");
        SpringApplication.run(Application.class, args);
    }
}
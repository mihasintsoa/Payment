package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class Main
{

    private static final String ip = "0.0.0.0";
    private static final String port = "8080";
    public static void main(String[] args)
    {
        SpringApplication app = new SpringApplication(Main.class);
        app.setDefaultProperties(Map.of(
                "server.address", ip,
                "server.port", port
        ));
        app.run(args);
    }
}

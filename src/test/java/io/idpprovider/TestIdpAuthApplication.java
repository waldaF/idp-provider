package io.idpprovider;

import org.springframework.boot.SpringApplication;

public class TestIdpAuthApplication {

    public static void main(String[] args) {
        SpringApplication.from(IdpAuthApplication::main).run(args);
    }

}
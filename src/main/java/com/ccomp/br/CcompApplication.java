package com.ccomp.br;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class CcompApplication {

	public static void main(String[] args) {
		SpringApplication.run(CcompApplication.class, args);
	}

    @GetMapping
    public String ping(){
        return "OK!";
    }
}

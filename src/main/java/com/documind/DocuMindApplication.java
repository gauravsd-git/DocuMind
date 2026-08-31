package com.documind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DocuMindApplication {

	public static void main(String[] args) {

		SpringApplication.run(DocuMindApplication.class, args);

		System.out.println("Docu Mind");
	}

}

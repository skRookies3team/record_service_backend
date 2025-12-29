package com.petlog.record;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableFeignClients
@SpringBootApplication
public class PetlogApplication {

	public static void main(String[] args) {


        SpringApplication.run(PetlogApplication.class, args);
	}


}

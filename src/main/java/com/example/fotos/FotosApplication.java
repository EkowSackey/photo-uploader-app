package com.example.fotos;

import com.example.fotos.config.S3Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(S3Properties.class)
public class FotosApplication {

	public static void main(String[] args) {
		SpringApplication.run(FotosApplication.class, args);
	}

}

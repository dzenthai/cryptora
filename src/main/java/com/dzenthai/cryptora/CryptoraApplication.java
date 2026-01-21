package com.dzenthai.cryptora;

import com.dzenthai.cryptora.configuration.CryptoraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(CryptoraProperties.class)
public class CryptoraApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoraApplication.class, args);
	}

}

package co.edu.escuelaing.techcup.match;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ServiceMatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceMatchApplication.class, args);
	}

}

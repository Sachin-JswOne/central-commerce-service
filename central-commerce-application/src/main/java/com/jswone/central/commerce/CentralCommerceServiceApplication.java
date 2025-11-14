package com.jswone.central.commerce;

import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class
},scanBasePackages = "com.jswone.commerce")
@EnableReactiveFirestoreRepositories(basePackages = "com.jswone.commerce")
public class CentralCommerceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CentralCommerceServiceApplication.class, args);
	}

}

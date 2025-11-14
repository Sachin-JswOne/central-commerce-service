package com.jswone.commerce.web;

import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class
},scanBasePackages = "com.jswone.commerce")
@EnableReactiveFirestoreRepositories(basePackages = "com.jswone.commerce")
public class JswCentralCommerceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JswCentralCommerceServiceApplication.class, args);
	}

}

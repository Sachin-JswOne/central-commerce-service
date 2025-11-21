package com.jswone.central.commerce;

import com.google.cloud.spring.data.datastore.repository.config.EnableDatastoreRepositories;
import com.jswone.commerce.core.config.CatalogueDynamicConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class
},scanBasePackages = "com.jswone.commerce")
@EnableDatastoreRepositories(basePackages = "com.jswone.commerce")
@EnableConfigurationProperties(CatalogueDynamicConfig.class)
public class CentralCommerceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CentralCommerceServiceApplication.class, args);
	}

}

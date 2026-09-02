package com.wk.ti.config.database;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
public class JpaDatabaseConfig {

    @Bean
    @ConfigurationProperties("spring.jpa")
    public JpaProperties jpaProperties() {
        return new JpaProperties();
    }

    @Bean
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder(
            JpaProperties jpaProperties) {

        JpaVendorAdapter vendorAdapter =
                new HibernateJpaVendorAdapter();

        Function<DataSource, Map<String, ?>> propertiesFactory =
                dataSource -> new LinkedHashMap<>(
                        jpaProperties.getProperties()
                );

        return new EntityManagerFactoryBuilder(
                vendorAdapter,
                propertiesFactory,
                null
        );
    }
}
package kcp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DBConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.order.hikari")
    public HikariConfig orderHikariConfig() {
        return new HikariConfig();
    }

    @Bean
    @Primary
    public DataSource orderDataSourceProperties() {
        return new HikariDataSource(orderHikariConfig());
    }
}

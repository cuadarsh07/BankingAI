package in.ashish.combined.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Robust DataSource Configuration that seamlessly handles cloud provider URLs
 * (such as Render, Supabase, Neon, or Railway) by auto-normalizing raw
 * 'postgresql://' or 'postgres://' connection strings into standard JDBC format.
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        String rawUrl = env.getProperty("spring.datasource.url", "jdbc:h2:mem:bankdb;DB_CLOSE_DELAY=-1");
        String effectiveUrl = rawUrl.trim();

        // Auto-fix Render raw database URLs that lack the required 'jdbc:' protocol prefix
        if (effectiveUrl.startsWith("postgres://")) {
            effectiveUrl = "jdbc:postgresql://" + effectiveUrl.substring("postgres://".length());
            log.info("Auto-formatted cloud database URL to standard JDBC format: {}", maskUrl(effectiveUrl));
        } else if (effectiveUrl.startsWith("postgresql://")) {
            effectiveUrl = "jdbc:" + effectiveUrl;
            log.info("Auto-formatted cloud database URL to standard JDBC format: {}", maskUrl(effectiveUrl));
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(effectiveUrl);

        String username = env.getProperty("spring.datasource.username");
        if (username != null && !username.isBlank()) {
            ds.setUsername(username.trim());
        }

        String password = env.getProperty("spring.datasource.password");
        if (password != null) {
            ds.setPassword(password.trim());
        }

        String driver = env.getProperty("spring.datasource.driver-class-name");
        if (driver != null && !driver.isBlank()) {
            ds.setDriverClassName(driver.trim());
        }

        return ds;
    }

    private String maskUrl(String url) {
        return url.replaceAll("://.*?:.*?@", "://***:***@");
    }
}


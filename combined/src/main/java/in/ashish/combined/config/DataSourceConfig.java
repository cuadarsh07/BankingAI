package in.ashish.combined.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Robust DataSource Configuration that seamlessly handles cloud provider URLs
 * (such as Render, Supabase, Neon, or Railway).
 *
 * PostgreSQL JDBC driver does NOT accept 'user:password@host' in the JDBC URL and
 * expects standard 'jdbc:postgresql://host:port/database' with credentials supplied
 * separately. This class automatically extracts credentials from URI-style strings
 * so deployments never fail with 'invalid port number'.
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        String rawUrl = env.getProperty("spring.datasource.url", "jdbc:h2:mem:bankdb;DB_CLOSE_DELAY=-1").trim();

        String jdbcUrl = rawUrl;
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        // Handle URI-style connection strings containing '@' (e.g. postgresql://user:pass@host/db)
        if (rawUrl.contains("@")) {
            try {
                String cleanUriStr = rawUrl.startsWith("jdbc:") ? rawUrl.substring(5) : rawUrl;
                if (cleanUriStr.startsWith("postgres://")) {
                    cleanUriStr = "postgresql://" + cleanUriStr.substring("postgres://".length());
                }

                URI uri = new URI(cleanUriStr);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    if (username == null || username.isBlank()) {
                        username = parts[0];
                    }
                    if (password == null || password.isBlank()) {
                        password = parts[1];
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath(); // includes leading '/'
                String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + query;
                log.info("Extracted credentials and formatted clean JDBC URL: jdbc:postgresql://{}:{}{}", host, port, path);
            } catch (Exception e) {
                log.warn("Could not parse cloud database URI '{}', falling back to direct URL: {}", rawUrl, e.getMessage());
            }
        } else {
            // Standard URL without '@'
            if (rawUrl.startsWith("postgres://")) {
                jdbcUrl = "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
            } else if (rawUrl.startsWith("postgresql://")) {
                jdbcUrl = "jdbc:" + rawUrl;
            }
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);

        if (username != null && !username.isBlank()) {
            ds.setUsername(username.trim());
        }

        if (password != null) {
            ds.setPassword(password.trim());
        }

        String driver = env.getProperty("spring.datasource.driver-class-name");
        if (driver != null && !driver.isBlank()) {
            ds.setDriverClassName(driver.trim());
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            ds.setDriverClassName("org.postgresql.Driver");
        }

        return ds;
    }
}

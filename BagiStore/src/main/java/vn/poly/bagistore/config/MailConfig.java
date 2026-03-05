package vn.poly.bagistore.config;

import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Fallback mail configuration: creates a JavaMailSender bean when none is auto-configured.
 * This helps start the application even if auto-configuration did not create the bean.
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender(Environment env, @Nullable MailProperties mailProperties) {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();

        // Prefer MailProperties when available (auto-configured), otherwise fall back to Environment
        if (mailProperties != null) {
            impl.setHost(mailProperties.getHost());
            if (mailProperties.getPort() != null) impl.setPort(mailProperties.getPort());
            impl.setUsername(mailProperties.getUsername());
            impl.setPassword(mailProperties.getPassword());
            impl.setProtocol(mailProperties.getProtocol());

            Properties props = impl.getJavaMailProperties();
            if (mailProperties.getProperties() != null) {
                mailProperties.getProperties().forEach(props::put);
            }
            props.putIfAbsent("mail.smtp.auth", String.valueOf(mailProperties.getProperties().getOrDefault("mail.smtp.auth", "true")));
            props.putIfAbsent("mail.smtp.starttls.enable", String.valueOf(mailProperties.getProperties().getOrDefault("mail.smtp.starttls.enable", "true")));
        } else {
            // Fallback: read from environment
            String host = env.getProperty("spring.mail.host", "");
            String port = env.getProperty("spring.mail.port", "");
            String username = env.getProperty("spring.mail.username", "");
            String password = env.getProperty("spring.mail.password", "");
            String protocol = env.getProperty("spring.mail.protocol", "smtp");

            if (!host.isEmpty()) impl.setHost(host);
            if (!port.isEmpty()) {
                try { impl.setPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
            if (!username.isEmpty()) impl.setUsername(username);
            if (!password.isEmpty()) impl.setPassword(password);
            impl.setProtocol(protocol);

            Properties props = impl.getJavaMailProperties();
            String auth = env.getProperty("spring.mail.properties.mail.smtp.auth", env.getProperty("mail.smtp.auth", "true"));
            String starttls = env.getProperty("spring.mail.properties.mail.smtp.starttls.enable", env.getProperty("mail.smtp.starttls.enable", "true"));
            props.putIfAbsent("mail.smtp.auth", auth);
            props.putIfAbsent("mail.smtp.starttls.enable", starttls);
        }

        return impl;
    }

}

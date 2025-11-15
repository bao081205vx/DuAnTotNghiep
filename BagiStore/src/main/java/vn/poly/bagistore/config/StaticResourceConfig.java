package vn.poly.bagistore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files under the workspace 'uploads' directory at URL path /uploads/**
        // Uses the working directory of the running JVM. When running from the project root,
        // files in ./uploads will be accessible at http://host:port/uploads/...
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:uploads/");
    }
}

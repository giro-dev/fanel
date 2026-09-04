package dev.agiro.fanel.shared.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
                        if (isExcluded(path)) {
                            return null;
                        }

                        Resource requested = location.createRelative(path);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        return location.createRelative("index.html");
                    }
                });
    }

    private static boolean isExcluded(String path) {
        return path.startsWith("api/")
                || path.startsWith("actuator/")
                || path.startsWith("mcp/")
                || path.startsWith("v3/")
                || path.startsWith("swagger-ui");
    }
}

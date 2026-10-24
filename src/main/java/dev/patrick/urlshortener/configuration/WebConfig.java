package dev.patrick.urlshortener.configuration;

import dev.patrick.urlshortener.component.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public class WebConfig implements WebMvcConfigurer {
    private static final String CREATE_LINK_PATH = "/api/links";

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor){
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry interceptorRegistr){
        interceptorRegistr.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(CREATE_LINK_PATH);
    }
}

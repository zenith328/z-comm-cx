package com.zcommcx.auth.config;

import com.zcommcx.auth.web.SiteAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiteAuthFilterConfig {

    @Bean
    public FilterRegistrationBean<SiteAuthFilter> siteAuthFilter() {
        FilterRegistrationBean<SiteAuthFilter> registration = new FilterRegistrationBean<>(new SiteAuthFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}

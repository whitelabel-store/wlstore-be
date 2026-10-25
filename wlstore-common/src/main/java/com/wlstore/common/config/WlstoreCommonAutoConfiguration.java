package com.wlstore.common.config;

import com.wlstore.common.web.GlobalControllerAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WlstoreCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    GlobalControllerAdvice globalControllerAdvice() {
        return new GlobalControllerAdvice();
    }
}
package com.wlstore.common.config;

import com.wlstore.common.tenant.RequestTenantResolver;
import com.wlstore.common.tenant.TenantFilter;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(HibernatePropertiesCustomizer.class)
public class WlstoreTenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RequestTenantResolver requestTenantResolver() {
        return new RequestTenantResolver();
    }

    @Bean
    HibernatePropertiesCustomizer tenantCustomizer(RequestTenantResolver resolver) {
        return props -> props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean
    TenantFilter tenantFilter() {
        return new TenantFilter();
    }
}
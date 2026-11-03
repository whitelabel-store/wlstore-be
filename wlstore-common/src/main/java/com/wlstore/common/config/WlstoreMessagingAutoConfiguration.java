package com.wlstore.common.config;

import com.wlstore.common.messaging.ProcessedEventInbox;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
public class WlstoreMessagingAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    ProcessedEventInbox processedEventInbox(JdbcTemplate jdbcTemplate) {
        return new ProcessedEventInbox(jdbcTemplate);
    }
}

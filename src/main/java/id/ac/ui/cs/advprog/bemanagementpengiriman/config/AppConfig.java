package id.ac.ui.cs.advprog.bemanagementpengiriman.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;

import java.time.Duration;

@Configuration
@EnableRetry
@EnableCaching
public class AppConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${mysawit.cache.ttl:60s}") Duration ttl,
            @Value("${mysawit.cache.maximum-size:1000}") long maximumSize
    ) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "userById",
                "usersByRole",
                "usersByNameAndRole",
                "mandorKebunAssignment",
                "kebunDetail"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maximumSize));
        return cacheManager;
    }

    @Bean
    public RestClientCustomizer timeoutRestClientCustomizer(
            @Value("${mysawit.http.connect-timeout:2s}") Duration connectTimeout,
            @Value("${mysawit.http.read-timeout:5s}") Duration readTimeout
    ) {
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(connectTimeout);
            requestFactory.setReadTimeout(readTimeout);
            builder.requestFactory(requestFactory);
        };
    }
}

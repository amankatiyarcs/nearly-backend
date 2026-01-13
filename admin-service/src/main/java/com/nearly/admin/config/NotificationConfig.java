package com.nearly.admin.config;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractStatusChangeNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class NotificationConfig {

    @Bean
    public AbstractStatusChangeNotifier logNotifier(InstanceRepository repository) {
        return new AbstractStatusChangeNotifier(repository) {
            @Override
            protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
                return Mono.fromRunnable(() -> {
                    if (event instanceof InstanceStatusChangedEvent statusEvent) {
                        String serviceName = instance.getRegistration().getName();
                        String status = statusEvent.getStatusInfo().getStatus();
                        
                        if ("DOWN".equals(status) || "OFFLINE".equals(status)) {
                            log.error("🚨 SERVICE DOWN: {} is {}", serviceName, status);
                            // Here you could send email/Slack notifications
                        } else if ("UP".equals(status)) {
                            log.info("✅ SERVICE UP: {} is now {}", serviceName, status);
                        } else {
                            log.warn("⚠️ SERVICE STATUS CHANGED: {} is now {}", serviceName, status);
                        }
                    }
                });
            }
        };
    }
}


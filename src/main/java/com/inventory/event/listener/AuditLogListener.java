package com.inventory.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.entity.AuditLog;
import com.inventory.event.UserActionAuditEvent;
import com.inventory.repository.jpa.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogListener {

    private final AuditLogRepository auditLogRepo;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserActionAuditEvent event) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(event.getUserId())
                    .action(event.getAction())
                    .resourceType(event.getResourceType())
                    .resourceId(event.getResourceId())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .correlationId(event.getCorrelationId())
                    .metadata(event.getMetadata() != null
                            ? objectMapper.writeValueAsString(event.getMetadata())
                            : null)
                    .build();
            auditLogRepo.save(entry);
        } catch (Exception e) {
            log.error("Failed to persist audit log for action={}", event.getAction(), e);
        }
    }
}

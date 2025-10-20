package com.reparaya.users.external.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.entity.IncomingEvent;
import com.reparaya.users.external.repository.IncomingEventRepository;
import com.reparaya.users.external.strategy.EventProcessStrategy;
import com.reparaya.users.factory.EventProcessStrategyFactory;
import io.jsonwebtoken.lang.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncomingEventProcessor {

    private final IncomingEventRepository incomingEventRepository;
    private final EventProcessStrategyFactory eventFactory;
    private final CorePublisherService corePublisherService;

    private boolean handleEvent(CoreMessage event) {
        EventProcessStrategy strategy = eventFactory.getStrategy(event);
        return strategy.handle(event);
    }

    public boolean processEventByStrategy(CoreMessage event) {
        corePublisherService.sendAckToCore(event.getMessageId()); // TODO: esta bien mandarlo aca?

        Optional<IncomingEvent> existingEvent = incomingEventRepository.findByMessageId(event.getMessageId());

        if (existingEvent.isEmpty()) {
            log.info("Starting event processing for messageId: {}", event.getMessageId());
            Optional<IncomingEvent> savedEvent = saveIncomingEvent(event);
            if (savedEvent.isEmpty()) {
                return false;
            }
            log.info("Saved incoming event for messageId: {}", event.getMessageId());
            boolean processed = handleEvent(event);
            if (!processed) {
                return false;
            }
            saveEventProcessedTrue(savedEvent.get());
            return true;
        } else if (existingEvent.get().isProcessed()){
            log.info("Received duplicated event. Skipping processing for messageId: {}", event.getMessageId());
            return false;
        } else { // TODO: check
            log.info("Received duplicated event but not processed for messageId: {}", event.getMessageId());
            boolean processed = handleEvent(event);
            if (!processed) {
                return false;
            }
            saveEventProcessedTrue(existingEvent.get());
            return true;
        }
    }

    private void saveEventProcessedTrue(IncomingEvent event) {
        event.setProcessed(true);
        incomingEventRepository.save(event);
    }

    private Optional<IncomingEvent> saveIncomingEvent(CoreMessage event) {
        try {
            IncomingEvent incomingEvent = mapCoreMessageToIncomingEvent(event);
            return Optional.of(incomingEventRepository.save(incomingEvent));
        } catch (Exception ex) {
            log.error("An error ocurred while mapping incoming event / saving event. MessageId: {}", event.getMessageId());
            return Optional.empty();
        }
    }

    private IncomingEvent mapCoreMessageToIncomingEvent(CoreMessage message) throws JsonProcessingException {
        return IncomingEvent.builder()
                .messageId(message.getMessageId())
                .source(message.getSource())
                .channel(message.getDestination().getChannel())
                .eventName(message.getDestination().getEventName())
                .timestamp(message.getTimestamp())
                .payload(getStringPayloadFromCoreMessage(message))
                .receivedAt(OffsetDateTime.now())
                .processed(false)
                .build();
    }

    private String getStringPayloadFromCoreMessage(CoreMessage message) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(message.getPayload());
        } catch (JsonProcessingException ex) {
            log.error("An error ocurred while processing payload as String for messageId: {}", message.getMessageId());
            throw ex;
        }
    }

}

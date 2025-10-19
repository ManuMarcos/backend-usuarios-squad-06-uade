package com.reparaya.users.external.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.entity.IncomingEvent;
import com.reparaya.users.external.repository.IncomingEventRepository;
import com.reparaya.users.external.strategy.EventProcessStrategy;
import com.reparaya.users.factory.EventProcessStrategyFactory;
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

    private boolean handleEvent(CoreMessage event) {
        EventProcessStrategy strategy = eventFactory.getStrategy(event);
        return strategy.handle(event);
    }

    public boolean processEventByStrategy(CoreMessage event) {
        Optional<IncomingEvent> existingEvent = incomingEventRepository.findByMessageId(event.getMessageId());

        // TODO: impacta que pueda que venga de nuevo con el mismo id pero diferente contenido? -> eso no deberia pasar, cada evento es unico...

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
            savedEvent.get().setProcessed(true);
            incomingEventRepository.save(savedEvent.get());
            return true;
        } else if (existingEvent.get().isProcessed()){
            log.info("Received duplicated event. Skipping processing for messageId: {}", event.getMessageId());
            return false;
        } else { // TODO: check
            boolean processed = handleEvent(event);
            if (!processed) {
                return false;
            }
            existingEvent.get().setProcessed(true);
            incomingEventRepository.save(existingEvent.get());
            return true;
        }
    }

    private Optional<IncomingEvent> saveIncomingEvent(CoreMessage event) {
        ObjectMapper mapper = new ObjectMapper();
        String stringPayload;
        try {
            stringPayload = mapper.writeValueAsString(event.getPayload());
        } catch (JsonProcessingException e) {
            log.error("An error ocurred while processing payload as String for messageId: {}", event.getMessageId());
            return Optional.empty();
        }
        try {
            IncomingEvent incomingEvent = IncomingEvent.builder()
                    .messageId(event.getMessageId())
                    .source(event.getSource())
                    .channel(event.getDestination().getChannel())
                    .eventName(event.getDestination().getEventName())
                    .timestamp(event.getTimestamp())
                    .payload(stringPayload)
                    .receivedAt(OffsetDateTime.now())
                    .processed(false)
                    .build();
            return Optional.of(incomingEventRepository.save(incomingEvent));
        } catch (Exception ex) {
            log.error("An error ocurred while mapping incoming event / saving event. MessageId: {}", event.getMessageId());
            return Optional.empty();
        }
    }
}

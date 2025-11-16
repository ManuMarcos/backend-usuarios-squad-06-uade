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
    private final CorePublisherService corePublisherService;

    public boolean processEventByStrategy(CoreMessage event) {

        Optional<IncomingEvent> existingEvent = incomingEventRepository.findByMessageId(event.getMessageId());
        boolean isNewEvent = existingEvent.isEmpty();

        if (isNewEvent) {
            return caseReceivedNewEvent(event);
        }

        if (existingEvent.get().isProcessed()) { // el evento ya fue recibido y procesado en el pasado.
            log.info("Received duplicated event. Skipping processing for messageId: {}", event.getMessageId());
            return false;
        } else {
            return caseReceivedDuplicatedEventButNotProcessed(event, existingEvent.get());
        }
    }

    private boolean caseReceivedNewEvent(CoreMessage event) {
        log.info("Starting event processing for messageId: {}", event.getMessageId());
        Optional<IncomingEvent> savedEvent = saveIncomingEvent(event);
        if (savedEvent.isEmpty()) {
            // TODO: aca deberia mandar usuario rechazado xq no llega a procesarlo ??
            // TODO: consultar si usuario rechazado solo se envia cuando falla el registro o toda la logica de procesamiento de evento.
            return false;
        }
        log.info("Saved incoming event for messageId: {}", event.getMessageId());
        boolean processed = handleEvent(event);
        if (!processed) {
            return false;
        }
        updateEventStatusToProcessed(savedEvent.get());
        return true;
    }

    private boolean caseReceivedDuplicatedEventButNotProcessed(CoreMessage event, IncomingEvent existingEvent) {
        log.info("Received duplicated event but never processed. MessageId: {}", event.getMessageId());
        boolean processed = handleEvent(event);
        if (!processed) {
            return false;
        }
        updateEventStatusToProcessed(existingEvent);
        return true;
    }

    private boolean handleEvent(CoreMessage event) {
        EventProcessStrategy strategy = eventFactory.getStrategy(event);
        return strategy.handle(event);
    }

    private void updateEventStatusToProcessed(IncomingEvent event) {
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
                .topic(message.getDestination().getTopic())
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

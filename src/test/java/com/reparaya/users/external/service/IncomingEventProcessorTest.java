package com.reparaya.users.external.service;

import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.entity.IncomingEvent;
import com.reparaya.users.external.repository.IncomingEventRepository;
import com.reparaya.users.external.strategy.EventProcessStrategy;
import com.reparaya.users.factory.EventProcessStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomingEventProcessorTest {

    @Mock
    private IncomingEventRepository incomingEventRepository;

    @Mock
    private EventProcessStrategyFactory eventFactory;

    @Mock
    private CorePublisherService corePublisherService;

    @Mock
    private EventProcessStrategy strategy;

    @InjectMocks
    private IncomingEventProcessor processor;

    private CoreMessage message;

    @BeforeEach
    void setUp() {
        CoreMessage.Destination destination = new CoreMessage.Destination();
        destination.setTopic("users");
        destination.setEventName("alta");

        message = new CoreMessage();
        message.setMessageId("msg-123");
        message.setTimestamp(OffsetDateTime.now());
        message.setDestination(destination);
        message.setPayload(Map.of("key", "value"));
    }

    @Test
    void processEventByStrategyPersistsNewEventAndMarksItProcessed() {
        when(incomingEventRepository.findByMessageId("msg-123")).thenReturn(Optional.empty());
        when(incomingEventRepository.save(any(IncomingEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventFactory.getStrategy(message)).thenReturn(strategy);
        when(strategy.handle(message)).thenReturn(true);

        boolean processed = processor.processEventByStrategy(message);

        assertThat(processed).isTrue();
        verify(eventFactory).getStrategy(message);
        ArgumentCaptor<IncomingEvent> captor = ArgumentCaptor.forClass(IncomingEvent.class);
        verify(incomingEventRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        IncomingEvent newEvent = captor.getAllValues().get(0);
        IncomingEvent updatedEvent = captor.getAllValues().get(1);
        assertThat(newEvent.getMessageId()).isEqualTo("msg-123");
        assertThat(updatedEvent.isProcessed()).isTrue();
    }

    @Test
    void processEventByStrategyReturnsFalseWhenPersistingFails() {
        when(incomingEventRepository.findByMessageId("msg-123")).thenReturn(Optional.empty());
        when(incomingEventRepository.save(any(IncomingEvent.class))).thenThrow(new RuntimeException("db down"));

        boolean processed = processor.processEventByStrategy(message);

        assertThat(processed).isFalse();
        verify(eventFactory, never()).getStrategy(any());
    }

    @Test
    void processEventByStrategySkipsAlreadyProcessedDuplicatedEvent() {
        IncomingEvent stored = IncomingEvent.builder()
                .messageId("msg-123")
                .processed(true)
                .build();
        when(incomingEventRepository.findByMessageId("msg-123")).thenReturn(Optional.of(stored));

        boolean processed = processor.processEventByStrategy(message);

        assertThat(processed).isFalse();
        verify(eventFactory, never()).getStrategy(any());
        verify(incomingEventRepository, never()).save(any());
    }

    @Test
    void processEventByStrategyReturnsFalseWhenStrategyCannotProcessDuplicatedEvent() {
        IncomingEvent stored = IncomingEvent.builder()
                .messageId("msg-123")
                .processed(false)
                .build();
        when(incomingEventRepository.findByMessageId("msg-123")).thenReturn(Optional.of(stored));
        when(eventFactory.getStrategy(message)).thenReturn(strategy);
        when(strategy.handle(message)).thenReturn(false);

        boolean processed = processor.processEventByStrategy(message);

        assertThat(processed).isFalse();
        verify(incomingEventRepository, never()).save(any());
    }
}

package com.reparaya.users.entity;

import com.reparaya.users.dto.CoreMessage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "incoming_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column
    private boolean processed;
}

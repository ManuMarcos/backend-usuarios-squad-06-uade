package com.reparaya.users.external.repository;

import com.reparaya.users.entity.IncomingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncomingEventRepository extends JpaRepository<IncomingEvent, Long> {

    Optional<IncomingEvent> findByMessageId(final String messageId);

}

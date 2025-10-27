package com.reparaya.users.mapper;

import com.reparaya.users.dto.AddressInfo;
import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.RegisterRequest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventMapperTest {

    @Test
    void mapRegisterRequestFromEvent_UsuarioTopic_MapsPayload() {
        CoreMessage event = coreMessage("usuario");
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "user@example.com");
        payload.put("password", "secret");
        payload.put("firstName", "John");
        payload.put("lastName", "Doe");
        payload.put("dni", "12345678");
        payload.put("phoneNumber", "555");
        payload.put("role", "CLIENTE");
        payload.put("address", List.of(AddressInfo.builder()
                .city("City")
                .state("State")
                .street("Main")
                .number("10")
                .build()));
        event.setPayload(payload);

        RegisterRequest request = EventMapper.mapRegisterRequestFromEvent(event);

        assertThat(request.getEmail()).isEqualTo("user@example.com");
        assertThat(request.getRole()).isEqualTo("CLIENTE");
        assertThat(request.getAddress()).hasSize(1);
    }

    @Test
    void mapRegisterRequestFromEvent_PrestadorTopic_MapsCataloguePayload() {
        CoreMessage event = coreMessage("prestador");
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "user@example.com");
        payload.put("password", "secret");
        payload.put("nombre", "Jane");
        payload.put("apellido", "Smith");
        payload.put("dni", "87654321");
        payload.put("telefono", "123");
        payload.put("estado", "Buenos Aires");
        payload.put("ciudad", "CABA");
        payload.put("calle", "Libertad");
        payload.put("numero", "100");
        payload.put("piso", "2");
        payload.put("departamento", "A");
        payload.put("zonas", List.of("zone1"));
        payload.put("habilidades", List.of("skill1"));
        event.setPayload(payload);

        RegisterRequest request = EventMapper.mapRegisterRequestFromEvent(event);

        assertThat(request.getFirstName()).isEqualTo("Jane");
        assertThat(request.getAddress()).singleElement().satisfies(address ->
                assertThat(address.getCity()).isEqualTo("CABA"));
        assertThat(request.getZones()).containsExactly("zone1");
        assertThat(request.getSkills()).containsExactly("skill1");
    }

    @Test
    void mapRegisterRequestFromEvent_PrestadorTopicMissingFieldsThrows() {
        CoreMessage event = coreMessage("prestador");
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "user@example.com");
        payload.put("password", "secret");
        payload.put("nombre", "Jane");
        payload.put("apellido", "Smith");
        // missing dni and other fields
        event.setPayload(payload);

        assertThatThrownBy(() -> EventMapper.mapRegisterRequestFromEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dni");
    }

    @Test
    void mapRegisterRequestFromEvent_UnknownTopicThrows() {
        CoreMessage event = coreMessage("unknown");
        event.setPayload(Map.of());

        assertThatThrownBy(() -> EventMapper.mapRegisterRequestFromEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void getUserIdFromDeactivateUserEvent_ReturnsId() {
        CoreMessage event = coreMessage("usuario");
        event.setMessageId("msg");
        event.setPayload(Map.of("id", 42));

        String userId = EventMapper.getUserIdFromDeactivateUserEvent(event);

        assertThat(userId).isEqualTo("42");
    }

    @Test
    void getUserIdFromDeactivateUserEvent_MissingIdThrows() {
        CoreMessage event = coreMessage("usuario");
        event.setMessageId("msg");
        event.setPayload(Map.of());

        assertThatThrownBy(() -> EventMapper.getUserIdFromDeactivateUserEvent(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event id: msg");
    }

    private static CoreMessage coreMessage(String topic) {
        CoreMessage.Destination destination = new CoreMessage.Destination();
        destination.setTopic(topic);
        destination.setEventName("test");

        CoreMessage message = new CoreMessage();
        message.setDestination(destination);
        return message;
    }
}

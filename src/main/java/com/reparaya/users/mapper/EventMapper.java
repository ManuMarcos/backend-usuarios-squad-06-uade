package com.reparaya.users.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reparaya.users.dto.AddressInfo;
import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.RegisterRequest;
import com.reparaya.users.dto.UpdateUserRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class EventMapper {

    public static final String TOPIC_USUARIO_MODULE_SEARCH = "usuario";
    public static final String TOPIC_PRESTADOR_MODULE_CATALOG = "prestador";

    public static RegisterRequest mapRegisterRequestFromEvent(CoreMessage event) {
        final String topic = event.getDestination().getTopic();
        return switch (topic) {
            case TOPIC_USUARIO_MODULE_SEARCH -> mapRegisterRequestNormally(event);
            case TOPIC_PRESTADOR_MODULE_CATALOG -> mapRegisterRequestFromCatalogue(event.getPayload());
            default -> throw new IllegalStateException("The topic: " + topic + " is not recognized.");
        };
    }

    public static UpdateUserRequest mapUpdateRequestFromEvent(CoreMessage event) {
        final String topic = event.getDestination().getTopic();
        return switch (topic) {
            case TOPIC_USUARIO_MODULE_SEARCH -> mapUpdateRequestNormally(event);
            case TOPIC_PRESTADOR_MODULE_CATALOG -> mapUpdateRequestFromCatalogue(event.getPayload());
            default -> throw new IllegalStateException("The topic: " + topic + " is not recognized.");
        };
    }

    public static String getUserIdFromCatalogueUserEvent(Map<String, Object> payload) {
        String userId = String.valueOf(payload.get("id_prestador"));
        if (userId != null) {
            return userId;
        }
        throw new IllegalArgumentException("The id_prestador is required in the payload");
    }

    private static UpdateUserRequest mapUpdateRequestFromCatalogue(Map<String, Object> payload) {
        ObjectMapper mapper = new ObjectMapper();

        Long userId = Long.valueOf(getUserIdFromCatalogueUserEvent(payload));

        try {
            String email = payload.get("email") != null ? String.valueOf(payload.get("email")) : null;
            String firstName = payload.get("nombre") != null ? String.valueOf(payload.get("nombre")) : null;
            String lastName = payload.get("apellido") != null ? String.valueOf(payload.get("apellido")) : null;
            String phoneNumber = payload.get("telefono") != null ? String.valueOf(payload.get("telefono")) : null;
            String password = payload.get("password") != null ? String.valueOf(payload.get("password")) : null;
            String state = payload.get("estado") != null ? String.valueOf(payload.get("estado")) : null;
            String city = payload.get("ciudad") != null ? String.valueOf(payload.get("ciudad")) : null;
            String street = payload.get("calle") != null ? String.valueOf(payload.get("calle")) : null;
            String number = payload.get("numero") != null ? String.valueOf(payload.get("numero")) : null;
            String floor = payload.get("piso") != null ? String.valueOf(payload.get("piso")) : null;
            String apartment = payload.get("departamento") != null ? String.valueOf(payload.get("departamento")) : null;

            List<Object> zones = new ArrayList<>();
            if (payload.get("zonas") != null) {
                zones = mapper.convertValue(payload.get("zonas"), new TypeReference<List<Object>>() {});
            }

            List<Object> skills = new ArrayList<>();
            if (payload.get("habilidades") != null) {
                skills = mapper.convertValue(payload.get("habilidades"), new TypeReference<List<Object>>() {});
            }

            var requestBuilder = UpdateUserRequest.builder();

            if (state != null || city != null || street != null || number != null) {
                requestBuilder.address(
                        List.of(AddressInfo.builder()
                            .city(city)
                            .state(state)
                            .street(street)
                            .number(number)
                            .apartment(apartment)
                            .floor(floor)
                            .build()));
            }

            return requestBuilder
                    .userId(userId)
                    .email(email)
                    .password(password)
                    .firstName(firstName)
                    .lastName(lastName)
                    .phoneNumber(phoneNumber)
                    .zones(zones)
                    .skills(skills)
                    .build();
        } catch (Exception ex) {
            log.error("Error mapping catalogue payload to UpdateUserRequest: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    private static RegisterRequest mapRegisterRequestFromCatalogue(Map<String, Object> payload) {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, String> required = Map.of(
                "email", "email",
                "password", "password",
                "nombre", "firstName",
                "apellido", "lastName",
                "telefono", "phoneNumber",
                "dni", "dni",
                "estado", "state",
                "ciudad", "city",
                "calle", "street",
                "numero", "number"
        );

        List<String> missing = missingFields(payload, required);
        if (!missing.isEmpty()) {
            String msg = "Missing required fields: " + String.join(", ", missing);
            throw new IllegalArgumentException(msg);
        }

        try {
            String email = String.valueOf(payload.get("email"));
            String firstName = String.valueOf(payload.get("nombre"));
            String lastName = String.valueOf(payload.get("apellido"));
            String dni = String.valueOf(payload.get("dni"));
            String phoneNumber = String.valueOf(payload.get("telefono"));
            String password = String.valueOf(payload.get("password"));
            String state = String.valueOf(payload.get("estado"));
            String city = String.valueOf(payload.get("ciudad"));
            String street = String.valueOf(payload.get("calle"));
            String number = String.valueOf(payload.get("numero"));
            String floor = payload.get("piso") != null ? String.valueOf(payload.get("piso")) : null;
            String apartment = payload.get("departamento") != null ? String.valueOf(payload.get("departamento")) : null;

            List<Object> zones = new ArrayList<>();
            if (payload.get("zonas") != null) {
                zones = mapper.convertValue(payload.get("zonas"), new TypeReference<List<Object>>() {});
            }

            List<Object> skills = new ArrayList<>();
            if (payload.get("habilidades") != null) {
                skills = mapper.convertValue(payload.get("habilidades"), new TypeReference<List<Object>>() {});
            }

            return RegisterRequest.builder()
                    .email(email)
                    .password(password)
                    .firstName(firstName)
                    .lastName(lastName)
                    .dni(dni)
                    .phoneNumber(phoneNumber)
                    .address(
                        List.of(
                            AddressInfo
                            .builder()
                            .city(city)
                            .state(state)
                            .street(street)
                            .number(number)
                            .apartment(apartment)
                            .floor(floor)
                            .build()
                        )
                    )
                    .role("PRESTADOR")
                    .zones(zones)
                    .skills(skills)
                    .build();
        } catch (Exception ex) {
            log.error("Error mapping catalogue payload to RegisterRequest: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    private static boolean isBlank(Object o) {
        return o == null || String.valueOf(o).trim().isEmpty();
    }

    private static List<String> missingFields(Map<String, Object> payload, Map<String, String> requiredMap) {
        return requiredMap.entrySet().stream()
                .filter(e -> isBlank(payload.get(e.getKey())))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }


    private static RegisterRequest mapRegisterRequestNormally(CoreMessage event) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.convertValue(event.getPayload(), RegisterRequest.class);
        } catch (Exception ex) {
            log.error("An error ocurred while deserializing event with messageId: {}. Error: {}", event.getMessageId(), ex.getMessage());
            throw ex;
        }
    }

    private static UpdateUserRequest mapUpdateRequestNormally(CoreMessage event) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            UpdateUserRequest updateUserRequest = mapper.convertValue(event.getPayload(), UpdateUserRequest.class);
            if (updateUserRequest.getUserId() != null) {
                return updateUserRequest;
            }
            throw new IllegalArgumentException("The userId is required in the payload");
        } catch (Exception ex) {
            log.error("An error ocurred while deserializing event with messageId: {}. Error: {}", event.getMessageId(), ex.getMessage());
            throw ex;
        }
    }
}

package com.reparaya.users.mapper;

import com.reparaya.users.dto.AddressInfo;
import com.reparaya.users.entity.Address;

import java.util.List;
import java.util.stream.Collectors;

public class AddressMapper {

    public static AddressInfo toDto(Address entity) {
        if (entity == null) {
            return null;
        }

        return AddressInfo.builder()
                .state(entity.getState())
                .city(entity.getCity())
                .street(entity.getStreet())
                .number(entity.getNumber())
                .floor(entity.getFloor())
                .apartment(entity.getApartment())
                .build();
    }

    public static List<AddressInfo> toDtoList(List<Address> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(AddressMapper::toDto)
                .collect(Collectors.toList());
    }
}
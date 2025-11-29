package com.reparaya.users.mapper;

import com.reparaya.users.dto.AddressInfo;
import com.reparaya.users.entity.Address;
import com.reparaya.users.entity.User;
import org.apache.commons.lang3.StringUtils;

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

    public static List<Address> mapAddressInfoListToAddressList(List<AddressInfo> addressList, User user) {
        return addressList.stream().map(addr -> Address.builder()
                .state(StringUtils.capitalize(addr.getState().trim().toLowerCase()))
                .city(StringUtils.capitalize(addr.getCity().trim().toLowerCase()))
                .street(StringUtils.capitalize(addr.getStreet().trim().toLowerCase()))
                .number(addr.getNumber().trim())
                .floor(addr.getFloor() != null ? addr.getFloor().trim() : null)
                .apartment(addr.getApartment() != null ? addr.getApartment().trim().toUpperCase() : null)
                .user(user)
                .build()
                ).collect(Collectors.toList());
    }
}
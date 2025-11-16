package com.reparaya.users.mapper;

import com.reparaya.users.dto.AddressInfo;
import com.reparaya.users.entity.Address;
import com.reparaya.users.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AddressMapperTest {

    @Test
    void toDto_returnsNullWhenEntityIsNull() {
        assertThat(AddressMapper.toDto(null)).isNull();
    }

    @Test
    void toDtoList_returnsEmptyListWhenInputNull() {
        assertThat(AddressMapper.toDtoList(null)).isEmpty();
    }

    @Test
    void mapAddressInfoListToAddressList_mapsAllFields() {
        User owner = User.builder().userId(10L).email("user@example.com").build();
        List<AddressInfo> infos = List.of(AddressInfo.builder()
                        .state("Buenos Aires")
                        .city("CABA")
                        .street("Libertad")
                        .number("100")
                        .floor("1")
                        .apartment("A")
                        .build());

        List<Address> addresses = AddressMapper.mapAddressInfoListToAddressList(infos, owner);

        assertThat(addresses).hasSize(1);
        Address mapped = addresses.get(0);
        assertThat(mapped.getState()).isEqualTo("Buenos Aires");
        assertThat(mapped.getCity()).isEqualTo("CABA");
        assertThat(mapped.getStreet()).isEqualTo("Libertad");
        assertThat(mapped.getNumber()).isEqualTo("100");
        assertThat(mapped.getFloor()).isEqualTo("1");
        assertThat(mapped.getApartment()).isEqualTo("A");
        assertThat(mapped.getUser()).isEqualTo(owner);
    }
}

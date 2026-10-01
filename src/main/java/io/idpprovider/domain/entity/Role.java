package io.idpprovider.domain.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
@Slf4j
public enum Role {
    UNKNOWN(0),
    READ(1),
    WRITE(2),
    ADMIN(3);

    private final int id;

    @Converter(autoApply = true)
    public static class RoleConverter implements AttributeConverter<Role, Integer> {

        @Override
        public Integer convertToDatabaseColumn(Role role) {
            return Objects.isNull(role) ? null : role.getId();
        }

        @Override
        public Role convertToEntityAttribute(Integer dbData) {
            if (Objects.isNull(dbData)) return null;

            return Stream.of(Role.values())
                    .filter(role -> role.getId() == dbData)
                    .findFirst()
                    .orElseGet(() -> {
                        log.warn("Unknown AccountType ID [{}] found in database. Falling back to UNKNOWN.", dbData);
                        return Role.UNKNOWN;
                    });
        }
    }
}
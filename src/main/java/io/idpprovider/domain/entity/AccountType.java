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
public enum AccountType {
    UNKNOWN(0),
    USER(1),
    SERVICE(2);
    private final int id;

    @Converter(autoApply = true)
    public static class AccountTypeConverter implements AttributeConverter<AccountType, Integer> {

        @Override
        public Integer convertToDatabaseColumn(final AccountType attribute) {
            return Objects.isNull(attribute) ? null : attribute.getId();
        }

        @Override
        public AccountType convertToEntityAttribute(final Integer dbData) {
            if (Objects.isNull(dbData)) {
                return null;
            }

            return Stream.of(AccountType.values())
                    .filter(type -> type.getId() == dbData)
                    .findFirst()
                    .orElseGet(() -> {
                        log.warn("Unknown AccountType ID [{}] found in database. Falling back to UNKNOWN.", dbData);
                        return AccountType.UNKNOWN;
                    });
        }
    }
}
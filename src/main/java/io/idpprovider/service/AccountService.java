package io.idpprovider.service;

import io.idpprovider.domain.entity.Account;
import io.idpprovider.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@ConditionalOnProperty(name = "auth.frontend.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Optional<Account> findByEmail(final String email) {
        return accountRepository.findByEmail(email);
    }

}
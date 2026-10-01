package io.idpprovider.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guid", nullable = false, unique = true, updatable = false)
    private UUID guid = UUID.randomUUID();

    @Column(name = "account_type_id", nullable = false)
    private AccountType accountType;

    @Column(unique = true)
    private String email;

    @Column(name = "service_name", unique = true)
    private String serviceName;

    @Column(name = "client_secret_hash")
    private String clientSecretHash;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Embedded
    private Audit audit = new Audit();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "account_roles",
            joinColumns = @JoinColumn(name = "account_id")
    )
    @Column(name = "role_id")
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return guid.equals(account.getGuid());
    }

    @Override
    public int hashCode() {
        return guid.hashCode();
    }
}
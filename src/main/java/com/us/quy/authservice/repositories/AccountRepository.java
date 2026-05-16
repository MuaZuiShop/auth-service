package com.us.quy.authservice.repositories;

import com.us.quy.authservice.entities.AccountEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    boolean existsAccountEntityByUsername(String username);
}

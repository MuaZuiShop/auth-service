package com.us.quy.authservice.mapper;

import com.quy.common.core.security.ERole;
import com.us.quy.authservice.dtos.Account;
import com.us.quy.authservice.dtos.AccountRegistrationRequest;
import com.us.quy.authservice.entities.AccountEntity;
import com.us.quy.authservice.enums.EStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    imports = {ERole.class, EStatus.class})
public interface AccountMapper {

    Account toAccount(AccountEntity entity);

    AccountEntity toAccountEntity(Account account);

    @Mapping(target = "roles", expression = "java(List.of(ERole.USER))")
    @Mapping(target = "status", expression = "java(EStatus.PENDING_VERIFICATION)")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "profileUpdatedAt", ignore = true)
    AccountEntity fromRegisterRequest(AccountRegistrationRequest request);

}

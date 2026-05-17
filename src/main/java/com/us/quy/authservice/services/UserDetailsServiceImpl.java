package com.us.quy.authservice.services;

import com.quy.common.core.exception.ErrorCode;
import com.us.quy.authservice.configurations.CustomUserDetails;
import com.us.quy.authservice.entities.AccountEntity;
import com.us.quy.authservice.enums.EStatus;
import com.us.quy.authservice.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AccountEntity account = accountRepository.findByUsername(username)
            .orElseThrow(ErrorCode.NOT_FOUND::toException);

        return new CustomUserDetails(
            account.getId(),
            account.getUsername(),
            account.getPassword(),
            account.getRoles(),
            EStatus.canLogin(account.getStatus()));
    }
}
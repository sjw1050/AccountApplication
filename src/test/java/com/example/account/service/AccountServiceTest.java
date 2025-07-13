package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountUserRepository accountUserRepository;
    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        given(accountUserRepository.findById(anyLong())).willReturn(
                Optional.of(
                        tester)
        );

        given(accountRepository.findFirstByOrderByIdDesc()).willReturn(
                Optional.of(
                        Account.builder()
                                .accountNumber("1000000012")
                                .build())
        );
        given(accountRepository.save(any())).willReturn(
                Account.builder()
                        .accountUser(tester)
                        .accountNumber("1000000013")
                        .build()
        );

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when
        AccountDto accountDto = accountService.createAccount(1L, 10000L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());

        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    void createFirstAccount() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        given(accountUserRepository.findById(anyLong())).willReturn(
                Optional.of(
                        tester)
        );

        given(accountRepository.findFirstByOrderByIdDesc()).willReturn(
                Optional.empty()
        );
        given(accountRepository.save(any())).willReturn(
                Account.builder()
                        .accountUser(tester)
                        .accountNumber("1000000013")
                        .build()
        );

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when
        AccountDto accountDto = accountService.createAccount(1L, 10000L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());

        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("해당유저 없음 - 계좌생성 실패 ")
    void createAccountUserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 10000L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("유저 당 최대 계좌는 10개")
    void createAccount_maxAccountIs10() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);

        given(accountUserRepository.findById(anyLong())).willReturn(
                Optional.of(
                        tester)
        );
        given(accountRepository.countByAccountUser(any())).willReturn(10);
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 10000L));
        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    void deleteAccountSuccess() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        given(accountUserRepository.findById(anyLong())).willReturn(
                Optional.of(
                        tester)
        );

        given(accountRepository.findByAccountNumber(anyString())).willReturn(
                Optional.of(
                        Account.builder()
                                .accountUser(tester)
                                .balance(0L)
                                .accountNumber("1000000012")
                                .build())
        );

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1000000012");
        //then
        verify(accountRepository, times(1)).save(captor.capture());

        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("해당유저 없음 - 계좌 해지 실패 ")
    void deleteAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1111111111"));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당계좌 없음 - 계좌 해지 실패 ")
    void deleteAccount_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .name("tester")
                .build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1111111111"));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름")
    void deleteAccountFailed_userUnMatch() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        AccountUser other = AccountUser.builder()
                .name("other")
                .build();
        other.setId(1L);
        given(accountUserRepository.findById(anyLong())).willReturn(
                Optional.of(
                        tester)
        );

        given(accountRepository.findByAccountNumber(anyString())).willReturn(
                Optional.of(
                        Account.builder()
                                .accountUser(other)
                                .balance(0L)
                                .accountNumber("1000000012")
                                .build())
        );

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1111111111"));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지계좌는 잔액이 없어야 한다.")
    void deleteAccountFailed_balanceNotEmpty() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        given(accountUserRepository.findById(anyLong())).willReturn(
                Optional.of(
                        tester)
        );

        given(accountRepository.findByAccountNumber(anyString())).willReturn(
                Optional.of(
                        Account.builder()
                                .accountUser(tester)
                                .balance(100L)
                                .accountNumber("1000000012")
                                .build())
        );

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1111111111"));
        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지된 계좌는 해지할 수 없다.")
    void deleteAccountFailed_userAlreadyUnRegistered() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        given(accountUserRepository.findById(anyLong())).willReturn(
                Optional.of(
                        tester)
        );

        given(accountRepository.findByAccountNumber(anyString())).willReturn(
                Optional.of(
                        Account.builder()
                                .accountUser(tester)
                                .balance(100L)
                                .accountStatus(AccountStatus.UNREGISTERED)
                                .accountNumber("1000000012")
                                .build())
        );

        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1111111111"));
        //then
        assertEquals(ErrorCode.USER_ALREADY_UNREGISTERED, exception.getErrorCode());
    }


}
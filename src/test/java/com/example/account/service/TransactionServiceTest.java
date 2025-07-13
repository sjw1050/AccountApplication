package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.ErrorCode.ACCOUNT_NOT_FOUND;
import static com.example.account.type.ErrorCode.AMOUNT_EXCEED_BALANCE;
import static com.example.account.type.ErrorCode.CANCEL_MUST_FULLY;
import static com.example.account.type.ErrorCode.TOO_OLD_ORDER_TO_CANCEL;
import static com.example.account.type.ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH;
import static com.example.account.type.ErrorCode.TRANSACTION_NOT_FOUND;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    public static final long USE_AMOUNT = 1000L;
    public static final long CANCEL_AMOUNT = 1000L;
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        Account account = Account.builder()
                .accountUser(tester)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        given(accountUserRepository.findById(anyLong())).willReturn(
                Optional.of(
                        tester)
        );

        given(accountRepository.findByAccountNumber(anyString())).willReturn(
                Optional.of(account)
        );
        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapShot(9000L)
                .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        TransactionDto transactionDto = transactionService.useBalance
                (12L, "1000000012", 1000L);
        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals("1000000012", captor.getValue().getAccount().getAccountNumber());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(9000L, captor.getValue().getBalanceSnapShot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapShot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당유저 없음 - 잔액 사용 실패 ")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당계좌 없음 - 잔액 사용 실패 ")
    void useBalance_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .name("tester")
                .build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));
        //then
        assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름")
    void useBalance_userUnMatch() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        AccountUser other = AccountUser.builder()
                .name("other")
                .build();
        other.setId(13L);
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
                () -> transactionService.useBalance(1L, "1000000000", 1000L));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지된 계좌는 사용할 수 없다.")
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
                () -> transactionService.useBalance(1L, "1000000000", 1000L));
        //then
        assertEquals(ErrorCode.USER_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우")
    void failedUseBalance() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        Account account = Account.builder()
                .accountUser(tester)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        given(accountUserRepository.findById(anyLong())).willReturn(
                Optional.of(
                        tester)
        );

        given(accountRepository.findByAccountNumber(anyString())).willReturn(
                Optional.of(account)
        );

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 11000L));
        //then
        assertEquals(AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장")
    void saveFailedUseTransaction() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        Account account = Account.builder()
                .accountUser(tester)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        given(accountRepository.findByAccountNumber(anyString())).willReturn(
                Optional.of(account)
        );
        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapShot(9000L)
                .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        transactionService.saveFailedUseTransaction("1000000000", USE_AMOUNT);
        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(F, captor.getValue().getTransactionResultType());
        assertEquals(10000L, captor.getValue().getBalanceSnapShot());
    }

    @Test
    void successCancelBalance() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        Account account = Account.builder()
                .accountUser(tester)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString())).willReturn(
                Optional.of(
                        transaction)
        );

        given(accountRepository.findByAccountNumber(anyString())).willReturn(
                Optional.of(account)
        );
        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                .account(account)
                .transactionType(CANCEL)
                .transactionResultType(S)
                .transactionId("transactionIdForCancel")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(10000L)
                .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        TransactionDto transactionDto = transactionService.cancelBalance
                ("transactionId", "1000000012", CANCEL_AMOUNT);
        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapShot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapShot());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패 ")
    void cancelTransaction_AccountNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("1L", "1000000000", 1000L));
        //then
        assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 거래 없음 - 잔액 사용 취소 실패 ")
    void cancelTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("1", "1000000000", 1000L));
        //then
        assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌 매칭 실패- 잔액 사용 취소 실패 ")
    void cancelTransaction_Transaction_Account_NotMatched() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        Account account = Account.builder()
                .accountUser(tester)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        account.setId(1L);
        Account accountNotUse = Account.builder()
                .accountUser(tester)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000013")
                .build();
        accountNotUse.setId(2L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(9000L)
                .build();


        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000012", CANCEL_AMOUNT));
        //then
        assertEquals(TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액과 취소금액이 다름- 잔액 사용 취소 실패 ")
    void cancelTransaction_CancelMustFully() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        Account account = Account.builder()
                .accountUser(tester)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        account.setId(12L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(3000L)
                .balanceSnapShot(9000L)
                .build();


        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000012", CANCEL_AMOUNT));
        //then
        assertEquals(CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("취소는 1년까지만 가능 - 잔액 사용 취소 실패 ")
    void cancelTransaction_TooOldOrderToCancel() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        Account account = Account.builder()
                .accountUser(tester)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(20))
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(9000L)
                .build();


        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000012", CANCEL_AMOUNT));
        //then
        assertEquals(TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }

    @Test
    void successQueryTransaction() {
        //given
        AccountUser tester = AccountUser.builder()
                .name("tester")
                .build();
        tester.setId(12L);
        Account account = Account.builder()
                .accountUser(tester)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();
        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(20))
                .amount(CANCEL_AMOUNT)
                .balanceSnapShot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(
                        Optional.of(transaction));
        //when
        TransactionDto transactionDto = transactionService.queryTransaction("testId");
        //then
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("원 거래 없음 - 잔액 조회 실패 ")
    void queryTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("1"));
        //then
        assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }


}
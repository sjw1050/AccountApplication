package com.example.account.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("내부 서버 에러가 발생했습니다."),
    USER_NOT_FOUND("사용자가 없습니다."),
    ACCOUNT_NOT_FOUND("계좌가 없습니다."),
    ACCOUNT_TRANSACTION_LOCK("해당 계좌는 사용중입니다."),
    INVALID_REQUEST("잘못된 요청입니다."),
    TRANSACTION_NOT_FOUND("계좌가 없습니다."),
    USER_ACCOUNT_UN_MATCH("계좌와 소유주가 일치하지 않습니다."),
    TRANSACTION_ACCOUNT_UN_MATCH("해당 거래는 해당 계좌에서 발생한 거래가 아닙니다."),
    TOO_OLD_ORDER_TO_CANCEL("1년이 지난 거래는 취소가 불가능합니다."),
    CANCEL_MUST_FULLY("부분 취소는 허용되지 않습니다."),
    AMOUNT_EXCEED_BALANCE("거래금액이 계좌 금액보다 큽니다"),
    USER_ALREADY_UNREGISTERED("계좌가 이미 해지되었습니다."),
    BALANCE_NOT_EMPTY("계좌잔액이 비어있지 않습니다."),
    MAX_ACCOUNT_PER_USER_10("사용자 최대 계좌는 10개입니다.");

    private final String description;
}

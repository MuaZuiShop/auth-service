package com.us.quy.authservice.enums;

public enum EStatus {
    PENDING_VERIFICATION,
    PENDING_UPDATE_INFO,
    ACTIVE,
    BANNED,
    DELETED;

    public boolean canTransitionTo(EStatus nextState) {
        return switch (this) {
            case PENDING_VERIFICATION -> nextState == PENDING_UPDATE_INFO || nextState == BANNED;

            case PENDING_UPDATE_INFO -> nextState == ACTIVE || nextState == BANNED;

            case ACTIVE -> nextState == BANNED || nextState == DELETED;

            case BANNED -> nextState == ACTIVE || nextState == DELETED;

            case DELETED -> false;
        };
    }

    public static boolean canLogin(EStatus status) {
        return status == ACTIVE || status == PENDING_UPDATE_INFO;
    }
}

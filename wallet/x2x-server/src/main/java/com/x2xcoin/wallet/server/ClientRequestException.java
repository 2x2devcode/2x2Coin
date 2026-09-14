package com.x2xcoin.wallet.server;

final class ClientRequestException extends RuntimeException {
    private final int status;

    ClientRequestException(String message) {
        this(400, message);
    }

    ClientRequestException(int status, String message) {
        super(message);
        this.status = status;
    }

    int status() {
        return status;
    }
}

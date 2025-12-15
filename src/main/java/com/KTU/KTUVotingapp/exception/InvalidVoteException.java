package com.KTU.KTUVotingapp.exception;

public class InvalidVoteException extends RuntimeException {
    public InvalidVoteException(String message) {
        super(message);
    }
}

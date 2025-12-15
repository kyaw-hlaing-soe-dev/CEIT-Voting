package com.KTU.KTUVotingapp.controller;

import com.KTU.KTUVotingapp.exception.InvalidVoteException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(InvalidVoteException.class)
    public ResponseEntity<String> handleInvalidVote(InvalidVoteException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
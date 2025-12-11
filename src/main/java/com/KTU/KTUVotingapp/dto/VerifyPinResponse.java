package com.KTU.KTUVotingapp.dto;

public class VerifyPinResponse {

    private boolean valid;
    private boolean alreadyVoted;

    public VerifyPinResponse(boolean valid, boolean alreadyVoted) {
        this.valid = valid;
        this.alreadyVoted = alreadyVoted;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isAlreadyVoted() {
        return alreadyVoted;
    }
}



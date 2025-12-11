package com.KTU.KTUVotingapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerifyPinRequest {

    @NotBlank
    @Size(min = 4, max = 16)
    private String pin;

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}



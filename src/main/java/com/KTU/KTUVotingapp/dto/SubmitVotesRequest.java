package com.KTU.KTUVotingapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body sent from the frontend when submitting all votes
 * (king, queen, prince, princess, couple) in one shot.
 */
public class SubmitVotesRequest {

    @NotBlank
    private String pin;

    /**
     * Device identifier generated on the client (e.g. random UUID stored in localStorage).
     * Used to prevent the same physical device from voting multiple times with different PINs.
     */
    @NotBlank
    private String deviceId;

    @NotNull
    private Integer kingSelection;

    @NotNull
    private Integer queenSelection;

    @NotNull
    private Integer princeSelection;

    @NotNull
    private Integer princessSelection;

    @NotNull
    private Integer coupleSelection;

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getKingSelection() {
        return kingSelection;
    }

    public void setKingSelection(Integer kingSelection) {
        this.kingSelection = kingSelection;
    }

    public Integer getQueenSelection() {
        return queenSelection;
    }

    public void setQueenSelection(Integer queenSelection) {
        this.queenSelection = queenSelection;
    }

    public Integer getPrinceSelection() {
        return princeSelection;
    }

    public void setPrinceSelection(Integer princeSelection) {
        this.princeSelection = princeSelection;
    }

    public Integer getPrincessSelection() {
        return princessSelection;
    }

    public void setPrincessSelection(Integer princessSelection) {
        this.princessSelection = princessSelection;
    }

    public Integer getCoupleSelection() {
        return coupleSelection;
    }

    public void setCoupleSelection(Integer coupleSelection) {
        this.coupleSelection = coupleSelection;
    }
}



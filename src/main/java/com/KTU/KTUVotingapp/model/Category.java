package com.KTU.KTUVotingapp.model;

public enum Category {
    KING, PRINCE, QUEEN, PRINCESS, COUPLE;

    public Category paired() {
        switch (this) {
            case KING: return PRINCE;
            case PRINCE: return KING;
            case QUEEN: return PRINCESS;
            case PRINCESS: return QUEEN;
            case COUPLE: return COUPLE;
            default: throw new IllegalStateException("Unknown category: " + this);
        }
    }

    public String displayName() {
        return name();
    }
}

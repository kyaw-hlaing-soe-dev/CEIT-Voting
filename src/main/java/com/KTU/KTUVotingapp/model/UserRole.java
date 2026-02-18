package com.KTU.KTUVotingapp.model;

/**
 * Security role for user authentication and authorization.
 * Used for Role-Based Access Control (RBAC).
 */
public enum UserRole {
    ROLE_USER(1),
    ROLE_ADMIN(2);

    private final int voteWeight;

    UserRole(int voteWeight) {
        this.voteWeight = voteWeight;
    }

    /**
     * Returns the vote weight for this role.
     * ROLE_USER votes count as 1, ROLE_ADMIN votes count as 2.
     */
    public int getVoteWeight() {
        return voteWeight;
    }
}

package com.volunteer.exception;

/**
 * Exception thrown when a volunteer attempts to sign up for an opportunity that has reached
 * its maximum registration capacity.
 */
public class CapacityExceededException extends PlatformException {

    private final long opportunityId;
    private final int capacity;

    public CapacityExceededException(long opportunityId, int capacity) {
        super("Registration failed: Opportunity #" + opportunityId + " has reached its maximum capacity of " + capacity + " volunteers.");
        this.opportunityId = opportunityId;
        this.capacity = capacity;
    }

    public long getOpportunityId() {
        return opportunityId;
    }

    public int getCapacity() {
        return capacity;
    }
}

package com.fms.hr_crm.calling.model.entity;

/**
 * Lifecycle states of a call session, mirroring Twilio call statuses.
 */
public enum CallStatus {
    INITIATED,
    RINGING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    NO_ANSWER,
    BUSY,
    CANCELED;

    /** Maps a Twilio callStatus string to our enum. */
    public static CallStatus fromTwilio(String twilioStatus) {
        return switch (twilioStatus == null ? "" : twilioStatus.toLowerCase()) {
            case "initiated"    -> INITIATED;
            case "ringing"      -> RINGING;
            case "in-progress"  -> IN_PROGRESS;
            case "completed"    -> COMPLETED;
            case "failed"       -> FAILED;
            case "no-answer"    -> NO_ANSWER;
            case "busy"         -> BUSY;
            case "canceled"     -> CANCELED;
            default             -> FAILED;
        };
    }
}
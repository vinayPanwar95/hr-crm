package com.fms.hr_crm.lead.model.entity;

import java.util.Set;

public enum LeadStatus {

    NEW {
        @Override public Set<LeadStatus> allowedTransitions() {
            return Set.of(CONTACTED, CLOSED);
        }
    },
    CONTACTED {
        @Override public Set<LeadStatus> allowedTransitions() {
            return Set.of(INTERESTED, NOT_INTERESTED, CLOSED);
        }
    },
    INTERESTED {
        @Override public Set<LeadStatus> allowedTransitions() {
            return Set.of(CONVERTED, NOT_INTERESTED, CLOSED);
        }
    },
    NOT_INTERESTED {
        @Override public Set<LeadStatus> allowedTransitions() {
            return Set.of(CLOSED);
        }
    },
    CONVERTED {
        @Override public Set<LeadStatus> allowedTransitions() {
            return Set.of(CLOSED);
        }
    },
    CLOSED {
        @Override public Set<LeadStatus> allowedTransitions() {
            return Set.of();  // terminal state
        }
    };

    public abstract Set<LeadStatus> allowedTransitions();

    public boolean canTransitionTo(LeadStatus target) {
        return allowedTransitions().contains(target);
    }
}

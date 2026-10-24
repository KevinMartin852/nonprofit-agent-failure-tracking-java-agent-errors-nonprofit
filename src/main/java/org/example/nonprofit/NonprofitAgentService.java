package org.example.nonprofit;

public final class NonprofitAgentService {
    private final FailureTracker failures;
    public NonprofitAgentService(FailureTracker failures) { this.failures = failures; }

    public String deliverReceipt(String donorId, boolean receiptAddressPresent) {
        return failures.record("donor-receipt", donorId, () -> {
            if (!receiptAddressPresent) throw new IllegalArgumentException("receipt address is required");
            return "receipt-sent:" + donorId;
        });
    }

    public String sendReminder(String volunteerId, boolean assignedShift) {
        return failures.record("volunteer-reminder", volunteerId, () -> {
            if (!assignedShift) throw new IllegalArgumentException("shift assignment is required");
            return "reminder-sent:" + volunteerId;
        });
    }

    public String publishReport(String campaignId, int confirmedGifts) {
        return failures.record("campaign-report", campaignId, () -> {
            if (confirmedGifts < 1) throw new IllegalArgumentException("report needs a confirmed gift");
            return "report-published:" + campaignId;
        });
    }
}

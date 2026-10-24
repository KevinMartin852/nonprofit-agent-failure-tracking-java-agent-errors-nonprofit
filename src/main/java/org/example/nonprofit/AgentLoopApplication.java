package org.example.nonprofit;

public final class AgentLoopApplication {
    public static void main(String[] args) {
        InfraiClient client = new InfraiClient(InfraiConfig.fromEnvironment(System.getenv()));
        NonprofitAgentService service = new NonprofitAgentService(new FailureTracker(client));
        System.out.println(service.deliverReceipt("donor-1042", true));
        System.out.println(service.sendReminder("volunteer-77", true));
        System.out.println(service.publishReport("spring-drive", 18));
    }
}

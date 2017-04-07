package com.cleo.prototype.agent;

import com.cleo.prototype.entities.activation.AgentInfo;

import java.util.Scanner;

import static com.cleo.prototype.Constants.BASE_URL;

public class AgentRunner {
    public static void main(String[] args) throws Exception {
        Scanner reader = new Scanner(System.in);
        String agentName;
        if (args == null || args.length == 0) {
            System.out.print("Agent name:> ");
            agentName = reader.nextLine();
        } else {
            agentName = args[0];
        }
        System.out.print("Enter SaaS URL: (ex: " + BASE_URL + ")> ");
        String saasUrl = reader.nextLine();
        if (saasUrl == null || saasUrl.trim().length() == 0) {
            saasUrl = BASE_URL;
        }

        final MockAgent mockAgent = new MockAgent();
        final AgentInfo agentInfo = mockAgent.getMyInfo(saasUrl, agentName);
        System.out.println("Activation successful.");
        System.out.println("Agent ID: " + agentInfo.getAgentId());

        Thread t = new Thread(() -> mockAgent.processEvents(agentInfo));
        t.start();
        System.out.println("Press ENTER to quit.");
        reader.nextLine();
        t.interrupt();
    }
}

package com.ccomp.br.shared.message;

public record MessagingChannel(String name, String routingKey) {

    public static final String PREFIX = "ccomp.";

    public String queue()         { return PREFIX + name + ".queue"; }
    public String dlq()           { return PREFIX + name + ".dlq"; }
    public String dlqRoutingKey() { return routingKey + ".dlq"; }
}

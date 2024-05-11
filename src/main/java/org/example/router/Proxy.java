package org.example.router;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.ServiceKey;

public class Proxy {

	public static final ServiceKey<Message> SERVICE_KEY =
			ServiceKey.create(Message.class, "aggregator-key");

	public static Behavior<Message> create(ActorRef<String> monitor) {
		return Behaviors.receive(Message.class)
				.onMessage(Message.class, in -> onMyMessage(monitor, in))
				.build();
	}

	private static Behavior<Message> onMyMessage(ActorRef<String> monitor, Message message) {
		monitor.tell(message.id());
		return Behaviors.same();
	}

	public String mapping(Message message) {
		return message.id();
	}

	record Message(String id, String content) {
	}

}

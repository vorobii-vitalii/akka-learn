package org.example.discovery;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.Receptionist;

public class PingService {
	private final ActorContext<Ping> context;

	private PingService(ActorContext<Ping> context) {
		this.context = context;
	}

	public static Behavior<Ping> create() {
		return Behaviors.setup(
				context -> {
					context
							.getSystem()
							.receptionist()
							.tell(Receptionist.register(ServiceKeys.PING_SERVICE_KEY, context.getSelf()));
					return new PingService(context).behavior();
				});
	}

	private Behavior<Ping> behavior() {
		return Behaviors.receive(Ping.class).onMessage(Ping.class, this::onPing).build();
	}

	private Behavior<Ping> onPing(Ping msg) {
		context.getLog().info("Pinged by {}", msg.replyTo);
		msg.replyTo.tell(new Pong());
		return Behaviors.same();
	}

	public record Pong() {
	}

	public record Ping(ActorRef<Pong> replyTo) {
	}

}

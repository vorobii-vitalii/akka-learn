package org.example.spawn_protocol;

import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Tiger extends AbstractBehavior<String> {
	public Tiger(ActorContext<String> context) {
		super(context);
	}

	@Override
	public Receive<String> createReceive() {
		return newReceiveBuilder()
				.onMessageEquals("kill", () -> {
					getContext().getLog().info("Killed... {}", getContext().getSelf());
					return Behaviors.stopped();
				})
				.build();
	}
}

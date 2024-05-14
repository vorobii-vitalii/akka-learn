package org.example.point_to_point;

import java.math.BigInteger;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.delivery.ConsumerController;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.serialization.jackson.JsonSerializable;

public class FibonacciConsumer extends AbstractBehavior<FibonacciConsumer.Command> {

	interface Command extends JsonSerializable {}

	public record FibonacciNumber(long n, BigInteger value) implements Command {
	}

	private record WrappedDelivery(ConsumerController.Delivery<Command> delivery) implements Command {
	}

	public static Behavior<Command> create(
			ActorRef<ConsumerController.Command<Command>> consumerController) {
		return Behaviors.setup(
				context -> {
					ActorRef<ConsumerController.Delivery<FibonacciConsumer.Command>> deliveryAdapter =
							context.messageAdapter(ConsumerController.deliveryClass(), WrappedDelivery::new);
					consumerController.tell(new ConsumerController.Start<>(deliveryAdapter));

					return new FibonacciConsumer(context);
				});
	}

	private FibonacciConsumer(ActorContext<Command> context) {
		super(context);
	}

	@Override
	public Receive<Command> createReceive() {
		return newReceiveBuilder().onMessage(WrappedDelivery.class, this::onDelivery).build();
	}

	private Behavior<Command> onDelivery(WrappedDelivery w) {
		var fibonacciNumber = (FibonacciNumber) w.delivery.message();
		getContext().getLog().info("Processed fibonacci {}: {}", fibonacciNumber.n, fibonacciNumber.value);
		w.delivery.confirmTo().tell(ConsumerController.confirmed());
		return this;
	}
}

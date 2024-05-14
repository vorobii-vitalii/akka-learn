package org.example.work_pulling;

import java.util.UUID;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.delivery.ConsumerController;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.receptionist.ServiceKey;

public class ImageConverter {

	interface Command {}

	public record ConversionJob(UUID resultId, String fromFormat, String toFormat, byte[] image) {
	}

	private record WrappedDelivery(ConsumerController.Delivery<ConversionJob> delivery) implements Command {
	}

	public static final ServiceKey<ConsumerController.Command<ConversionJob>> SERVICE_KEY =
			ServiceKey.create(ConsumerController.serviceKeyClass(), ImageConverter.class.getSimpleName());

	public static Behavior<Command> create() {
		return Behaviors.setup(
				context -> {
					ActorRef<ConsumerController.Delivery<ConversionJob>> deliveryAdapter =
							context.messageAdapter(ConsumerController.deliveryClass(), WrappedDelivery::new);
					ActorRef<ConsumerController.Command<ConversionJob>> consumerController =
							context.spawn(ConsumerController.create(SERVICE_KEY), "consumerController");
					consumerController.tell(new ConsumerController.Start<>(deliveryAdapter));

					return Behaviors.receive(Command.class)
							.onMessage(WrappedDelivery.class, v -> onDelivery(v, context))
							.build();
				});
	}

	private static Behavior<Command> onDelivery(WrappedDelivery w, ActorContext<Command> context) {
		byte[] image = w.delivery.message().image;
		String fromFormat = w.delivery.message().fromFormat;
		String toFormat = w.delivery.message().toFormat;
		// convert image...
		// store result with resultId key for later retrieval
		context.getLog().info("Processing {}", w.delivery.message());

		// and when completed confirm
		w.delivery.confirmTo().tell(ConsumerController.confirmed());

		return Behaviors.same();
	}
}

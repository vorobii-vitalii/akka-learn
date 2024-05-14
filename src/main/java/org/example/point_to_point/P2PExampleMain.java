package org.example.point_to_point;

import java.util.Optional;
import java.util.UUID;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.delivery.ConsumerController;
import akka.actor.typed.delivery.ProducerController;
import akka.actor.typed.javadsl.Behaviors;

public class P2PExampleMain {

	public static void main(String[] args) {
		ActorSystem<Void> actorSystem = ActorSystem.apply(
				Behaviors.setup(context -> {
					ActorRef<ConsumerController.Command<FibonacciConsumer.Command>> consumerController =
							context.spawn(ConsumerController.create(), "consumerController");
					context.spawn(FibonacciConsumer.create(consumerController), "consumer");

					String producerId = "fibonacci-" + UUID.randomUUID();
					ActorRef<ProducerController.Command<FibonacciConsumer.Command>> producerController =
							context.spawn(
									ProducerController.create(
											FibonacciConsumer.Command.class, producerId, Optional.empty()),
									"producerController");
					context.spawn(FibonacciProducer.create(producerController), "producer");

					consumerController.tell(
							new ConsumerController.RegisterToProducerController<>(producerController));
					return Behaviors.ignore();
				}),
				"P2PExample"
		);
	}

}

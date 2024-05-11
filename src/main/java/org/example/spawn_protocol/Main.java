package org.example.spawn_protocol;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Props;
import akka.actor.typed.SpawnProtocol;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;

public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		var system = ActorSystem.apply(SpawnProtocolHandler.create(), "spawn-protocol");
		CompletionStage<ActorRef<String>> tiger = AskPattern.ask(
				system,
				replyTo ->
						new SpawnProtocol.Spawn<>(
								Behaviors.setup(Tiger::new),
								"Tiger_1",
								Props.empty(),
								replyTo
						),
				Duration.ofSeconds(5L),
				system.scheduler());
		tiger.whenComplete(new BiConsumer<ActorRef<String>, Throwable>() {
			@Override
			public void accept(ActorRef<String> stringActorRef, Throwable throwable) {
				LOGGER.info("Killing tiger 1");
				stringActorRef.tell("kill");
			}
		});
		CompletionStage<ActorRef<String>> tiger2 = AskPattern.ask(
				system,
				replyTo ->
						new SpawnProtocol.Spawn<>(
								Behaviors.setup(Tiger::new),
								"Tiger_2",
								Props.empty(),
								replyTo
						),
				Duration.ofSeconds(5L),
				system.scheduler());
		tiger2.whenComplete((stringActorRef, throwable) -> {
			LOGGER.info("Killing tiger 2");
			stringActorRef.tell("kill");
		});
	}
}
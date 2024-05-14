package org.example.sharding;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.LogOptions;
import akka.actor.typed.javadsl.Behaviors;
import akka.serialization.jackson.JsonSerializable;

public class Counter {

	public sealed interface Command extends JsonSerializable {
		String key();
	}

	public record Increment(String key) implements Command {
	}

	public record Get(String key, ActorRef<Optional<Integer>> replyTo) implements Command {
	}

	public static Behavior<Command> create(String entityId) {
		return Behaviors.setup(context -> {
			var log = context.getLog();
			Map<String, Integer> map = new HashMap<>();
			log.info("Creating counter actor {}", entityId);
			return Behaviors.logMessages(LogOptions.create().withLogger(log), Behaviors.receive(Command.class)
					.onMessage(Increment.class, command -> {
						map.compute(command.key(), (k, v) -> v == null ? 1 : (v + 1));
						return Behaviors.same();
					})
					.onMessage(Get.class, command -> {
						command.replyTo().tell(Optional.ofNullable(map.get(command.key())));
						return Behaviors.same();
					})
					.build());
		});
	}


}

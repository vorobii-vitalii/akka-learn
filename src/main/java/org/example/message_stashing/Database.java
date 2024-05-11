package org.example.message_stashing;

import java.time.Duration;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.StashBuffer;

public class Database {

	public static final int STASH_CAPACITY = 50;

	public static Behavior<Command> create() {
		return Behaviors.setup(context -> Behaviors.withStash(STASH_CAPACITY, stashBuffer -> {
			context.scheduleOnce(Duration.ofSeconds(5), context.getSelf(), new Initialized());
			return uninitializedState(stashBuffer, context);
		}));
	}

	private static Behavior<Command> uninitializedState(StashBuffer<Command> stashBuffer, ActorContext<Command> context) {
		return Behaviors.receive(Command.class)
				.onMessage(Initialized.class, init -> {
					context.getLog().info("DB initialized!");
					return stashBuffer.unstashAll(initializedState(stashBuffer, context));
				})
				.onMessage(Command.class, command -> {
					context.getLog().info("Stashing command {} for now", command);
					stashBuffer.stash(command);
					return Behaviors.same();
				})
				.build();
	}

	private static Behavior<Command> initializedState(StashBuffer<Command> stashBuffer, ActorContext<Command> context) {
		return Behaviors.receive(Command.class)
				.onMessage(Initialized.class, init -> {
					context.getLog().info("Received DB initialized message... Ignoring it");
					return Behaviors.same();
				})
				.onMessage(Get.class, get -> {
					context.getLog().info("Processing get command {}", get);
					get.replyReceiver().tell("Reply (" + get.key() + ")");
					return stashBuffer.unstashAll(Behaviors.same());
				})
				.build();
	}

	public sealed interface Command {
	}

	public record Initialized() implements Command {
	}

	public record Get(String key, ActorRef<String> replyReceiver) implements Command {
	}

}

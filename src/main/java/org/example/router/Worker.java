package org.example.router;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

public class Worker {

	sealed interface Command {
	}

	public record Log(int logId) implements Command {
	}

	public static Behavior<Command> create() {
		return Behaviors.setup(context -> {
			context.getLog().info("Starting worker");
			return Behaviors.receive(Command.class)
					.onMessage(Log.class, logCommand -> {
						context.getLog().info("Command to log {} was received by worker {}", logCommand.logId(), context.getSelf());
						return Behaviors.same();
					})
					.build();
		});
	}

}

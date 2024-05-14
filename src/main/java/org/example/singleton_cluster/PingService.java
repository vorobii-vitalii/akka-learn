package org.example.singleton_cluster;

import java.time.Duration;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Down;
import akka.serialization.jackson.JsonSerializable;

public class PingService {

	sealed interface Command extends JsonSerializable {
	}

	record Ping(ActorRef<String> sender) implements Command {
	}

	record Crash() implements Command {
	}

	public static Behavior<Command> create(int id, Duration crashAfter) {
		return Behaviors.setup(context -> {
			context.getLog().info("Creating ping actor with id = {}", id);
			context.scheduleOnce(crashAfter, context.getSelf(), new Crash());
			return Behaviors.receive(Command.class)
					.onMessage(Ping.class, event -> {
						context.getLog().info("Processing ping request {}", id);
						event.sender().tell("Pong!");
						return Behaviors.same();
					})
					.onMessage(Crash.class, ignored -> {
						context.getLog().info("Crashing itself {}", id);
						final Cluster cluster = Cluster.get(context.getSystem());
						cluster.manager().tell(new Down(cluster.selfMember().address()));
						return Behaviors.stopped();
					})
					.build();
		});
	}


}

package org.example.discovery;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

public class Pinger {
	private final ActorContext<PingService.Pong> context;
	private final ActorRef<PingService.Ping> pingServiceActor;

	private Pinger(ActorContext<PingService.Pong> context, ActorRef<PingService.Ping> pingServiceActor) {
		this.context = context;
		this.pingServiceActor = pingServiceActor;
	}

	public static Behavior<PingService.Pong> create(ActorRef<PingService.Ping> pingServiceActor) {
		return Behaviors.setup(context -> {
			pingServiceActor.tell(new PingService.Ping(context.getSelf()));
			return new Pinger(context, pingServiceActor).behavior();
		});
	}

	private Behavior<PingService.Pong> behavior() {
		return Behaviors.receive(PingService.Pong.class)
				.onMessage(PingService.Pong.class, msg -> {
					context.getLog().info("Was ponged!");
					return Behaviors.stopped();
				})
				.build();
	}

}

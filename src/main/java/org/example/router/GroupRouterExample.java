package org.example.router;

import org.example.discovery.PingService;
import org.example.discovery.ServiceKeys;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Routers;

public class GroupRouterExample {

	public static Behavior<Void> create() {
		return Behaviors.setup(context -> {
			int workers = 10;
			for (int i = 0; i < workers; i++) {
				context.spawnAnonymous(PingService.create());
			}
			var groupRouter = Routers.group(ServiceKeys.PING_SERVICE_KEY).withRandomRouting();
			var groupRouterKey = context.spawn(groupRouter, "group-router");
			ActorRef<PingService.Pong> pongReceiver = context.spawnAnonymous(
					Behaviors.setup(pongReceiverContext -> Behaviors.receive(PingService.Pong.class)
							.onMessage(PingService.Pong.class, msg -> {
								pongReceiverContext.getLog().info("Was ponged!");
								return Behaviors.same();
							})
							.build()));
			for (int i = 0; i < 10; i++) {
				groupRouterKey.tell(new PingService.Ping(pongReceiver));
			}
			return Behaviors.ignore();
		});
	}

	public static void main(String[] args) {
		ActorSystem.apply(GroupRouterExample.create(), "group-router-demo");
	}

}

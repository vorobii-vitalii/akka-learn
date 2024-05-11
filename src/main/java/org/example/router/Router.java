package org.example.router;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.PoolRouter;
import akka.actor.typed.javadsl.Routers;

public class Router {
	private static final int POOL_SIZE = 10;
	private static final int MESSAGES = 2;

	public static Behavior<Void> create() {
		return Behaviors.setup(context -> {
			PoolRouter<Worker.Command> pool =
					Routers.pool(POOL_SIZE, Behaviors.supervise(Worker.create()).onFailure(SupervisorStrategy.restart()))
							.withBroadcastPredicate(msg -> {
								return msg instanceof Worker.Log log && log.logId() == 0;
							})
							.withRouteeProps(DispatcherSelector.blocking());
			ActorRef<Worker.Command> router = context.spawn(pool, "worker-pool", DispatcherSelector.sameAsParent());
			for (int i = 0; i < MESSAGES; i++) {
				router.tell(new Worker.Log(i));
			}
			return Behaviors.ignore();
		});
	}

	public static void main(String[] args) {
		ActorSystem.apply(Router.create(), "router-example");
	}

}

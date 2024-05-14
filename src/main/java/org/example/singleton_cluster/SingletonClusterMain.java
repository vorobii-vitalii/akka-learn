package org.example.singleton_cluster;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.example.distributed_data.Counter;
import org.example.simple_cluster.SimpleActor;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.ClusterSingletonSettings;
import akka.cluster.typed.SingletonActor;

public class SingletonClusterMain {

	public static void main(String[] args) {
		final Duration[] delays = {
				Duration.ofSeconds(1),
				Duration.ofSeconds(6),
				Duration.ofSeconds(13),
				Duration.ofSeconds(19)
		};
		startup(25251, Duration.ofSeconds(3), delays);
		startup(25252, Duration.ofMinutes(5), delays);
		startup(39925, Duration.ofMinutes(7), delays);
		startup(39926, Duration.ofMinutes(9), delays);
		startup(39927, Duration.ofMinutes(11), delays);
	}

	private static void startup(int port, Duration waitTillCrash, Duration[] sendMessageAfterArr) {
		Map<String, Object> configOverrides = new HashMap<>();
		configOverrides.put("akka.remote.artery.canonical.port", port);
		configOverrides.put("akka.cluster.roles", List.of("role - " + port));
		Config config = ConfigFactory.parseMap(configOverrides)
				.withFallback(ConfigFactory.load("akka-cluster-app.conf"));

		ActorSystem<Void> system = ActorSystem.apply(
				Behaviors.setup(context -> {
					context.getLog().info("Simple cluster app started on port = {}", port);
					ClusterSingleton singleton = ClusterSingleton.get(context.getSystem());
					// Start if needed and provide a proxy to a named singleton
					ActorRef<PingService.Command> proxy =
							singleton.init(SingletonActor.of(PingService.create(port, waitTillCrash), "PingService"));
					var executor = Executors.newSingleThreadScheduledExecutor();
					ActorRef<String> pongReplier = context.spawnAnonymous(
							Behaviors.setup(ctx -> {
								return Behaviors.receive(String.class)
										.onMessage(String.class, event -> {
											ctx.getLog().info("Received pong on {}", port);
											return Behaviors.same();
										})
										.build();
							}));
					for (Duration delay : sendMessageAfterArr) {
						executor.schedule(() -> {
							proxy.tell(new PingService.Ping(pongReplier));
						}, delay.toMillis(), TimeUnit.MILLISECONDS);
					}
					return Behaviors.ignore();
				}),
				"ClusterSystem",
				config
		);
		system.logConfiguration();
	}


}

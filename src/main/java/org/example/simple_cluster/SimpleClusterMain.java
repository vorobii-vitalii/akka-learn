package org.example.simple_cluster;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;

public class SimpleClusterMain {

	public static void main(String[] args) {
		startup(25251, Duration.ofMinutes(100));
		startup(25252, Duration.ofMinutes(100));
		startup(39925, Duration.ofSeconds(5));
		startup(39926, Duration.ofSeconds(7));
		startup(39927, Duration.ofSeconds(10));

		Executors.newSingleThreadScheduledExecutor().schedule(() -> {
			startup(39928, Duration.ofSeconds(15));
		}, 15, TimeUnit.SECONDS);
	}

	private static void startup(int port, Duration waitUntilCrash) {
		Map<String, Object> configOverrides = new HashMap<>();
		configOverrides.put("akka.remote.artery.canonical.port", port);
		configOverrides.put("akka.cluster.roles", List.of("role - " + port));
		Config config = ConfigFactory.parseMap(configOverrides)
				.withFallback(ConfigFactory.load("akka-cluster-app.conf"));

		ActorSystem<Void> system = ActorSystem.apply(
				Behaviors.setup(context -> {
					context.getLog().info("Simple cluster app started on port = {}", port);
					context.spawnAnonymous(SimpleActor.create(waitUntilCrash));
					return Behaviors.ignore();
				}),
				"ClusterSystem",
				config
		);
		system.logConfiguration();
	}

}

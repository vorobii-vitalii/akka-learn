package org.example.sharding;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.ShardingEnvelope;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import scala.Int;

public class ShardingExampleMain {

	public static void main(String[] args) {
		startup(25251);
		startup(25252);
		startup(25212);
	}

	private static void startup(int port) {
		Map<String, Object> configOverrides = new HashMap<>();
		configOverrides.put("akka.remote.artery.canonical.port", port);
		Config config = ConfigFactory.parseMap(configOverrides)
				.withFallback(ConfigFactory.load("akka-cluster-app.conf"));

		ActorSystem<Void> actorSystem = ActorSystem.apply(
				Behaviors.setup(context -> {
					var system = context.getSystem();
					var sharding = ClusterSharding.get(system);
					context.getLog().info("Simple sharding app started on port = {}", port);
					ActorRef<ShardingEnvelope<Counter.Command>> shardRegion =
							sharding.init(
									Entity.of(EntityTypeKey.create(Counter.Command.class, "Counter"),
									ctx -> Counter.create(ctx.getEntityId())));
					ActorRef<Optional<Integer>> replyReceiver = context.spawnAnonymous(
							Behaviors.setup(replyActorContext -> {
								var log = replyActorContext.getLog();
								return Behaviors.receiveMessage(msg -> {
									log.info("Receiver received {}", msg);
									return Behaviors.same();
								});
							}));
					Set<String> keys = Set.of("a", "b", "c", "d", "e");
					for (int i = 0; i < 50; i++) {
						for (String key : keys) {
							shardRegion.tell(new ShardingEnvelope<>(key, new Counter.Increment(key)));
						}
					}
					for (String key : keys) {
						shardRegion.tell(new ShardingEnvelope<>(key, new Counter.Get(key, replyReceiver)));
					}
					for (int i = 0; i < 30; i++) {
						for (String key : keys) {
							shardRegion.tell(new ShardingEnvelope<>(key, new Counter.Increment(key)));
						}
					}
					for (String key : keys) {
						shardRegion.tell(new ShardingEnvelope<>(key, new Counter.Get(key, replyReceiver)));
					}
					return Behaviors.ignore();
				}),
				"ClusterSystem",
				config
		);
		actorSystem.logConfiguration();
	}

}

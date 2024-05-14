package org.example.sharding;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.Done;
import akka.actor.Address;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.delivery.ConsumerController;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.ShardingEnvelope;
import akka.cluster.sharding.typed.delivery.ShardingConsumerController;
import akka.cluster.sharding.typed.delivery.ShardingProducerController;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.cluster.typed.Cluster;

public class ShardingProducerExampleMain {

	public static void main(String[] args) {
		Map<String, Object> configOverrides = new HashMap<>();
		configOverrides.put("akka.actor.provider", "cluster");
		Config config = ConfigFactory.parseMap(configOverrides).withFallback(ConfigFactory.defaultApplication());

		ActorSystem<Void> actorSystem = ActorSystem.apply(
				Behaviors.setup(context -> {
					DB db = getDBImpl();

					ActorSystem<Void> system = context.getSystem();

					EntityTypeKey<ConsumerController.SequencedMessage<TodoList.Command>> entityTypeKey =
							EntityTypeKey.create(ShardingConsumerController.entityTypeKeyClass(), "todo");

					ActorRef<ShardingEnvelope<ConsumerController.SequencedMessage<TodoList.Command>>> region =
							ClusterSharding.get(system)
									.init(
											Entity.of(
													entityTypeKey,
													entityContext ->
															ShardingConsumerController.create(
																	start ->
																			TodoList.create(entityContext.getEntityId(), db, start))));

					Address selfAddress = Cluster.get(system).selfMember().address();
					String producerId = "todo-producer-" + selfAddress.hostPort();

					ActorRef<ShardingProducerController.Command<TodoList.Command>> producerController =
							context.spawn(
									ShardingProducerController.create(
											TodoList.Command.class, producerId, region, Optional.empty()),
									"producerController");

					context.spawn(TodoService.create(producerController), "producer");
					return Behaviors.ignore();
				}),
				"ShardingProducerExample",
				config
		);
	}

	private static DB getDBImpl() {
		return new DB() {
			private final ConcurrentHashMap<String, TodoList.State> storage = new ConcurrentHashMap<>();

			@Override
			public CompletionStage<Done> save(String id, TodoList.State state) {
				storage.put(id, state);
				try {
					Thread.sleep(250);
					return CompletableFuture.completedFuture(Done.done());
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public CompletionStage<TodoList.State> load(String id) {
				try {
					Thread.sleep(250);
					return CompletableFuture.completedFuture(storage.get(id));
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

}

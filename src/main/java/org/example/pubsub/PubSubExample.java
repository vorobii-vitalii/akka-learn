package org.example.pubsub;

import java.util.HashMap;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.pubsub.PubSub;
import akka.actor.typed.pubsub.Topic;
import akka.serialization.jackson.JsonSerializable;

public class PubSubExample {

	public static void main(String[] args) {
		startup(25251);
//		startup(25252);
//		startup(25212);
	}

	record Message(String msg) implements JsonSerializable {

	}

	private static void startup(int port) {
		Map<String, Object> configOverrides = new HashMap<>();
		configOverrides.put("akka.remote.artery.canonical.port", port);
		Config config = ConfigFactory.parseMap(configOverrides)
				.withFallback(ConfigFactory.load("akka-cluster-app.conf"));

		ActorSystem<Void> actorSystem = ActorSystem.apply(
				Behaviors.setup(rootContext -> {
					PubSub pubSub = PubSub.get(rootContext.getSystem());

					ActorRef<Topic.Command<Message>> topic =
							pubSub.topic(Message.class, "my-topic");
					ActorRef<Message> subscriberRef = rootContext.spawnAnonymous(Behaviors.setup(context -> {
						return Behaviors.receiveMessage(msg -> {
							context.getLog().info("Received command {}", msg);
							return Behaviors.same();
						});
					}));
					topic.tell(Topic.subscribe(subscriberRef));

					topic.tell(Topic.publish(new Message("Hello Subscribers!")));
					topic.tell(Topic.publish(new Message("Hello Subscribers 2!")));
					topic.tell(Topic.publish(new Message("Hello Subscribers 3!")));

					return Behaviors.ignore();
				}),
				"ClusterSystem",
				config
		);
		actorSystem.logConfiguration();
	}

}

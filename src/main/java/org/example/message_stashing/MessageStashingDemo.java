package org.example.message_stashing;

import java.time.Duration;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

public class MessageStashingDemo {

	public static Behavior<Void> create() {
		return Behaviors.setup(context -> {
			ActorRef<Database.Command> dbReference = context.spawnAnonymous(Database.create());
			ActorRef<String> responseReceiver = context.spawnAnonymous(Behaviors.logMessages(Behaviors.ignore()));
			context.scheduleOnce(Duration.ofSeconds(1), dbReference, new Database.Get("a", responseReceiver));
			context.scheduleOnce(Duration.ofSeconds(2), dbReference, new Database.Get("b", responseReceiver));
			context.scheduleOnce(Duration.ofSeconds(4), dbReference, new Database.Get("c", responseReceiver));
			context.scheduleOnce(Duration.ofSeconds(8), dbReference, new Database.Get("d", responseReceiver));
			return Behaviors.ignore();
		});
	}

	public static void main(String[] args) {
		ActorSystem.apply(MessageStashingDemo.create(), "message-stashing");
	}

}

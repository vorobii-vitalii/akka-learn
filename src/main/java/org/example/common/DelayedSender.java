package org.example.common;

import java.time.Duration;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

public class DelayedSender  {

	public static <T, U extends T> Behavior<Void> create(ActorRef<T> receiver, U message, Duration delay) {
		return Behaviors.setup(context -> {
			return Behaviors.ignore();
		});
	}

}

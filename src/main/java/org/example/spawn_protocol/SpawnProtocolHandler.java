package org.example.spawn_protocol;

import akka.actor.typed.Behavior;
import akka.actor.typed.SpawnProtocol;
import akka.actor.typed.javadsl.Behaviors;

public abstract class SpawnProtocolHandler {
	private SpawnProtocolHandler() {}

	public static Behavior<SpawnProtocol.Command> create() {
		return Behaviors.setup(
				context -> {
					context.getLog().info("Creating spawn protocol handler");
					return SpawnProtocol.create();
				});
	}
}

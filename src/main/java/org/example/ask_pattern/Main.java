package org.example.ask_pattern;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;

public class Main {

	public static void main(String[] args) {
		final ActorSystem<String> system = ActorSystem.apply(Behaviors.setup(AskActor::new), "ask_example");
		system.tell(AskActor.DEMO_ASK);
		system.tell(AskActor.DEMO_ASK);
		system.tell(AskActor.DEMO_ASK);
		system.tell(AskActor.DEMO_ASK_WITH_STATUS);
		system.tell(AskActor.DEMO_ASK_WITH_STATUS);
	}

}

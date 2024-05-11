package org.example.ask_pattern;

import java.time.Duration;
import java.util.Objects;

import akka.actor.typed.ActorRef;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.StatusReply;

public class AskActor extends AbstractBehavior<String> {
	public static final String DEMO_ASK = "demoAsk";
	public static final String DEMO_ASK_WITH_STATUS = "demoAskWithStatus";
	public static final Duration TIMEOUT = Duration.ofSeconds(3);

	private final ActorRef<RequestHandler.Request> requestHandler;
	private boolean hasSent;

	public AskActor(ActorContext<String> context) {
		super(context);
		requestHandler = context.spawn(
				Behaviors.supervise(Behaviors.setup(RequestHandler::new))
						.onFailure(SupervisorStrategy.restart()),
				"request_handler"
		);
	}

	@Override
	public Receive<String> createReceive() {
		ActorContext<String> context = getContext();
		return newReceiveBuilder()
				.onMessageEquals(DEMO_ASK, () -> {
					context.getLog().info("Asking another actor");
					context.ask(
							String.class,
							requestHandler,
							TIMEOUT,
							(ActorRef<String> ref) -> new RequestHandler.Request1("Message from me!", ref),
							(response, throwable) -> Objects.requireNonNullElseGet(response, () -> "failed!!! " + throwable));
					return Behaviors.same();
				})
				.onMessageEquals(DEMO_ASK_WITH_STATUS, () -> {
					context.getLog().info("Asking another actor");
					context.askWithStatus(
							String.class,
							requestHandler,
							TIMEOUT,
							(ActorRef<StatusReply<String>> ref) -> new RequestHandler.Request2(
									!hasSent ? "ff" : "222222222222222",
									ref
							),
							(response, throwable) -> Objects.requireNonNullElseGet(response, () -> "failed!!! " + throwable)
					);
					hasSent = !hasSent;
					return Behaviors.same();
				})
				.onMessage(String.class, msg -> {
					context.getLog().info("Received message = {}", msg);
					return Behaviors.same();
				})
				.build();
	}
}

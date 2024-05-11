package org.example.ask_pattern;

import akka.actor.typed.ActorRef;
import akka.actor.typed.PreRestart;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.StatusReply;

public class RequestHandler extends AbstractBehavior<RequestHandler.Request> {

	public static final String MSG_1 = "msg1";
	public static final int MAX_REQUESTS = 3;

	public sealed interface Request {

	}

	public record Request1(String msg, ActorRef<String> replyTo) implements Request {
	}

	public record Request2(String msg, ActorRef<StatusReply<String>> replyTo) implements Request {
	}


	private int requests = 0;

	public RequestHandler(ActorContext<Request> context) {
		super(context);
	}

	@Override
	public Receive<Request> createReceive() {
		return newReceiveBuilder()
				.onMessage(Request1.class, command -> {
					if (++requests == MAX_REQUESTS) {
						throw new IllegalStateException("Failed to process request...");
					}
					getContext().getLog().info("Handling request = {}", command);
					command.replyTo().tell("Response to " + command.msg);
					return Behaviors.same();
				})
				.onMessage(Request2.class, command -> {
					getContext().getLog().info("Handling request = {}", command);
					if (++requests == MAX_REQUESTS) {
						throw new IllegalStateException("Failed to process request...");
					}
					if (command.msg.length() > 5) {
						command.replyTo().tell(StatusReply.error("Message too long - " + command.msg));
					}
					else {
						command.replyTo().tell(StatusReply.success("Response to " + command.msg));
					}
					return Behaviors.same();
				})
				.onSignal(PreRestart.class, handler -> {
					getContext().getLog().info("Received pre-restart signal!");
					return Behaviors.same();
				})
				.build();
	}
}

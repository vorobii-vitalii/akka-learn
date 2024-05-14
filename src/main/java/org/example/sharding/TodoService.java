package org.example.sharding;

import java.time.Duration;

import akka.Done;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.sharding.typed.delivery.ShardingProducerController;

public class TodoService {

	interface Command {}

	public record UpdateTodo(String listId, String item, boolean completed, ActorRef<Response> replyTo) implements Command {
	}

	public enum Response {
		ACCEPTED,
		REJECTED,
		MAYBE_ACCEPTED
	}

	private record Confirmed(ActorRef<Response> originalReplyTo) implements Command {
	}

	private record TimedOut(ActorRef<Response> originalReplyTo) implements Command {
	}

	private record WrappedRequestNext(ShardingProducerController.RequestNext<TodoList.Command> next) implements Command {
	}

	public static Behavior<Command> create(
			ActorRef<ShardingProducerController.Command<TodoList.Command>> producerController) {
		return Init.create(producerController);
	}

	static class Init extends AbstractBehavior<Command> {

		static Behavior<Command> create(
				ActorRef<ShardingProducerController.Command<TodoList.Command>> producerController) {
			return Behaviors.setup(
					context -> {
						ActorRef<ShardingProducerController.RequestNext<TodoList.Command>>
								requestNextAdapter =
								context.messageAdapter(
										ShardingProducerController.requestNextClass(), WrappedRequestNext::new);
						producerController.tell(new ShardingProducerController.Start<>(requestNextAdapter));

						return new Init(context);
					});
		}

		private Init(ActorContext<Command> context) {
			super(context);
		}

		@Override
		public Receive<Command> createReceive() {
			return newReceiveBuilder()
					.onMessage(WrappedRequestNext.class, w -> Active.create(w.next))
					.onMessage(
							UpdateTodo.class,
							command -> {
								// not hooked up with shardingProducerController yet
								command.replyTo.tell(Response.REJECTED);
								return this;
							})
					.build();
		}
	}

	static class Active extends AbstractBehavior<TodoService.Command> {

		private ShardingProducerController.RequestNext<TodoList.Command> requestNext;

		static Behavior<Command> create(
				ShardingProducerController.RequestNext<TodoList.Command> requestNext) {
			return Behaviors.setup(context -> new Active(context, requestNext));
		}

		private Active(
				ActorContext<Command> context,
				ShardingProducerController.RequestNext<TodoList.Command> requestNext) {
			super(context);
			this.requestNext = requestNext;
		}

		@Override
		public Receive<Command> createReceive() {
			return newReceiveBuilder()
					.onMessage(WrappedRequestNext.class, this::onRequestNext)
					.onMessage(UpdateTodo.class, this::onUpdateTodo)
					.onMessage(Confirmed.class, this::onConfirmed)
					.onMessage(TimedOut.class, this::onTimedOut)
					.build();
		}

		private Behavior<Command> onRequestNext(WrappedRequestNext w) {
			requestNext = w.next;
			return this;
		}

		private Behavior<Command> onUpdateTodo(UpdateTodo command) {
			Integer buffered = requestNext.getBufferedForEntitiesWithoutDemand().get(command.listId);
			if (buffered != null && buffered >= 100) {
				command.replyTo.tell(Response.REJECTED);
			} else {
				TodoList.Command requestMsg;
				if (command.completed) requestMsg = new TodoList.CompleteTask(command.item);
				else requestMsg = new TodoList.AddTask(command.item);
				getContext()
						.ask(
								Done.class,
								requestNext.askNextTo(),
								Duration.ofSeconds(5),
								askReplyTo ->
										new ShardingProducerController.MessageWithConfirmation<>(
												command.listId, requestMsg, askReplyTo),
								(done, exc) -> {
									if (exc == null) return new Confirmed(command.replyTo);
									else return new TimedOut(command.replyTo);
								});
			}
			return this;
		}

		private Behavior<Command> onConfirmed(Confirmed confirmed) {
			confirmed.originalReplyTo.tell(Response.ACCEPTED);
			return this;
		}

		private Behavior<Command> onTimedOut(TimedOut timedOut) {
			timedOut.originalReplyTo.tell(Response.MAYBE_ACCEPTED);
			return this;
		}
	}
}

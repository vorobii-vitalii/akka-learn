package org.example.sharding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

import akka.Done;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.delivery.ConsumerController;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

interface DB {
	CompletionStage<Done> save(String id, TodoList.State state);
	CompletionStage<TodoList.State> load(String id);
}

public class TodoList {

	public static Behavior<Command> create(String id, DB db, ActorRef<ConsumerController.Start<Command>> consumerController) {
		return Init.create(id, db, consumerController);
	}

	private static Behavior<Command> onDBError(DBError error) throws Exception {
		throw error.cause;
	}

	interface Command {
	}

	public record AddTask(String item) implements Command {
	}

	public record CompleteTask(String item) implements Command {
	}

	private record InitialState(State state) implements Command {
	}

	private record SaveSuccess(ActorRef<ConsumerController.Confirmed> confirmTo) implements Command {
	}

	private static class DBError implements Command {
		final Exception cause;

		private DBError(Throwable cause) {
			if (cause instanceof Exception) {
				this.cause = (Exception) cause;
			} else {
				this.cause = new RuntimeException(cause.getMessage(), cause);
			}
		}
	}

	private record CommandDelivery(Command command, ActorRef<ConsumerController.Confirmed> confirmTo) implements Command {
	}

	public record State(List<String> tasks) {
		public State(List<String> tasks) {
			this.tasks = Collections.unmodifiableList(tasks);
		}

		public State add(String task) {
			var copy = new ArrayList<>(tasks);
			copy.add(task);
			return new State(copy);
		}

		public State remove(String task) {
			var copy = new ArrayList<>(tasks);
			copy.remove(task);
			return new State(copy);
		}
	}

	static class Init extends AbstractBehavior<Command> {

		private final String id;
		private final DB db;
		private final ActorRef<ConsumerController.Start<Command>> consumerController;

		private Init(
				ActorContext<Command> context,
				String id,
				DB db,
				ActorRef<ConsumerController.Start<Command>> consumerController
		) {
			super(context);
			this.id = id;
			this.db = db;
			this.consumerController = consumerController;
		}

		public static Behavior<Command> create(String id, DB db, ActorRef<ConsumerController.Start<Command>> consumerController) {
			return Behaviors.setup(
					context -> {
						context.pipeToSelf(
								db.load(id),
								(state, exc) -> {
									if (exc == null) {
										return new InitialState(state);
									} else {
										return new DBError(exc);
									}
								});

						return new Init(context, id, db, consumerController);
					});
		}

		@Override
		public Receive<Command> createReceive() {
			return newReceiveBuilder()
					.onMessage(InitialState.class, this::onInitialState)
					.onMessage(DBError.class, TodoList::onDBError)
					.build();
		}

		private Behavior<Command> onInitialState(InitialState initial) {
			ActorRef<ConsumerController.Delivery<Command>> deliveryAdapter =
					getContext()
							.messageAdapter(
									ConsumerController.deliveryClass(),
									d -> new CommandDelivery(d.message(), d.confirmTo()));
			consumerController.tell(new ConsumerController.Start<>(deliveryAdapter));
			return Active.create(id, db, initial.state);
		}
	}

	static class Active extends AbstractBehavior<Command> {

		private final String id;
		private final DB db;
		private State state;

		private Active(ActorContext<Command> context, String id, DB db, State state) {
			super(context);
			this.id = id;
			this.db = db;
			this.state = state;
		}

		static Behavior<Command> create(String id, DB db, State state) {
			return Behaviors.setup(context -> new Active(context, id, db, state));
		}

		@Override
		public Receive<Command> createReceive() {
			return newReceiveBuilder()
					.onMessage(CommandDelivery.class, this::onDelivery)
					.onMessage(SaveSuccess.class, this::onSaveSuccess)
					.onMessage(DBError.class, TodoList::onDBError)
					.build();
		}

		private Behavior<Command> onDelivery(CommandDelivery delivery) {
			if (delivery.command instanceof AddTask addTask) {
				state = state.add(addTask.item);
				save(state, delivery.confirmTo);
				return this;
			} else if (delivery.command instanceof CompleteTask completeTask) {
				state = state.remove(completeTask.item);
				save(state, delivery.confirmTo);
				return this;
			} else {
				return Behaviors.unhandled();
			}
		}

		private void save(State newState, ActorRef<ConsumerController.Confirmed> confirmTo) {
			getContext()
					.pipeToSelf(
							db.save(id, newState),
							(state, exc) -> {
								if (exc == null) {
									return new SaveSuccess(confirmTo);
								} else {
									return new DBError(exc);
								}
							});
		}

		private Behavior<Command> onSaveSuccess(SaveSuccess success) {
			success.confirmTo.tell(ConsumerController.confirmed());
			return this;
		}
	}
}

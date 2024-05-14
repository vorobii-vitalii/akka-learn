package org.example.event_sourcing;

import java.util.function.BiFunction;

import akka.actor.typed.Behavior;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandler;
import akka.persistence.typed.javadsl.Effect;
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehavior;

public class MyPersistentBehavior
		extends EventSourcedBehavior<
		MyPersistentBehavior.Command, MyPersistentBehavior.Event, MyPersistentBehavior.State> {

	private MyPersistentBehavior(PersistenceId persistenceId) {
		super(persistenceId);
	}

	public static Behavior<Command> create(PersistenceId persistenceId) {
		return new MyPersistentBehavior(persistenceId);
	}

	@Override
	public State emptyState() {
		return new State();
	}

	@Override
	public CommandHandler<Command, Event, State> commandHandler() {
		newCommandHandlerBuilder()
				.forAnyState()
				.onCommand(Command.class, (state, command) -> {
					return Effect().persist();
				});

		return (state, command) -> {
			throw new RuntimeException("TODO: process the command & return an Effect");
		};
	}

	@Override
	public EventHandler<State, Event> eventHandler() {
		return (state, event) -> {
			throw new RuntimeException("TODO: process the event return the next state");
		};
	}

	interface Command {
	}

	interface Event {
	}

	public static class State {
	}
}

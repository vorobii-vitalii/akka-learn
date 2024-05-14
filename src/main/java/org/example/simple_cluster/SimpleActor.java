package org.example.simple_cluster;

import java.time.Duration;
import java.util.function.Function;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.ClusterEvent;
import akka.cluster.typed.Cluster;

import akka.cluster.typed.Leave;
import akka.cluster.typed.Subscribe;
import akka.serialization.jackson.JsonSerializable;

public class SimpleActor {

	public static final Duration DELAY = Duration.ofSeconds(5);

	public sealed interface Command extends JsonSerializable {
	}

	public record ReachableMemberEvent(ClusterEvent.ReachableMember event) implements Command {
	}

	public record UnreachableMemberEvent(ClusterEvent.UnreachableMember event) implements Command {
	}

	public record RoleLeaderChangedEvent(ClusterEvent.RoleLeaderChanged event) implements Command {
	}

	public record LogClusterState() implements Command {
	}

	public record Crash() implements Command {
	}

	public static Behavior<SimpleActor.Command> create(Duration crashAfter) {
		return Behaviors.setup(context -> {
			Cluster cluster = Cluster.get(context.getSystem());

			context.getLog().info("Subscribing to cluster events!");
			context.getLog().info("My roles = {}", cluster.selfMember().getRoles());

			createSubscription(context, ClusterEvent.ReachableMember.class, ReachableMemberEvent::new);
			createSubscription(context, ClusterEvent.UnreachableMember.class, UnreachableMemberEvent::new);
			createSubscription(context, ClusterEvent.RoleLeaderChanged.class, RoleLeaderChangedEvent::new);

			context.getSystem().scheduler().scheduleAtFixedRate(
					DELAY,
					DELAY,
					() -> context.getSelf().tell(new LogClusterState()),
					context.getExecutionContext()
			);

			context.scheduleOnce(crashAfter, context.getSelf(), new Crash());

			return Behaviors.receive(Command.class)
					.onMessage(ReachableMemberEvent.class, event -> {
						context.getLog().info("{} detected that {} member is reachable", context.getSelf(), event.event().member());
						return Behaviors.same();
					})
					.onMessage(UnreachableMemberEvent.class, event -> {
						context.getLog().info("{} detected that {} member is unreachable", context.getSelf(), event.event().member());
						return Behaviors.same();
					})
					.onMessage(RoleLeaderChangedEvent.class, event -> {
						context.getLog().info("{} detected that leader changed {}", context.getSelf(), event.event());
						return Behaviors.same();
					})
					.onMessage(LogClusterState.class, event -> {
						final boolean isLeader = cluster.selfMember().address().equals(cluster.state().getLeader());
						context.getLog().info("From perspective of {} cluster state = {}. Is leader = {}", context.getSelf(), cluster.state(),
								isLeader);
						return Behaviors.same();
					})
					.onMessage(Crash.class, event -> {
						context.getLog().info("Crashing self {}", context.getSelf());
						cluster.manager().tell(Leave.create(cluster.selfMember().address()));
						return Behaviors.stopped();
					})
					.build();
		});
	}

	private static  <T, E extends ClusterEvent.ClusterDomainEvent> void createSubscription(
			ActorContext<T> actorContext,
			Class<E> eventClass,
			Function<E, T> mapper
	) {
		ActorRef<E> adapter = actorContext.messageAdapter(eventClass, mapper::apply);
		Cluster cluster = Cluster.get(actorContext.getSystem());
		cluster.subscriptions().tell(Subscribe.create(adapter, eventClass));
	}


}

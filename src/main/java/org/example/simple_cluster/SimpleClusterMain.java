package org.example.simple_cluster;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.BootstrapSetup;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Join;

class SimpleActor {

	public static Behavior<Void> create() {
		return Behaviors.setup(context -> {
			Cluster cluster = Cluster.get(context.getSystem());
			cluster.manager().tell(Join.create(cluster.selfMember().address()));
			return Behaviors.ignore();
		});
	}

}

public class SimpleClusterMain {

	public static void main(String[] args) {
		final ActorSystem<Object> system = ActorSystem.apply(Behaviors.ignore(), "name", ConfigFactory.load("akka-cluster-app.conf"));
		system.logConfiguration();
	}

}

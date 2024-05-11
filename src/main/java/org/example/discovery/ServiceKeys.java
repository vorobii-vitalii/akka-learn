package org.example.discovery;

import akka.actor.typed.receptionist.ServiceKey;

public final class ServiceKeys {
	public static final ServiceKey<PingService.Ping> PING_SERVICE_KEY =
			ServiceKey.create(PingService.Ping.class, "pingService");

	private ServiceKeys() {
		// Utility classes should not be instantiated
	}

}

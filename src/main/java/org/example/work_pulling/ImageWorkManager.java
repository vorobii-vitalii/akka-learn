package org.example.work_pulling;

import java.util.Optional;
import java.util.UUID;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.delivery.WorkPullingProducerController;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.StashBuffer;

public class ImageWorkManager {

	interface Command {}

	public record Convert(String fromFormat, String toFormat, byte[] image) implements Command {
	}

	public record GetResult(UUID resultId, ActorRef<Optional<byte[]>> replyTo) implements Command {
	}

	private record WrappedRequestNext(WorkPullingProducerController.RequestNext<ImageConverter.ConversionJob> next) implements Command {
	}

	private final ActorContext<Command> context;
	private final StashBuffer<Command> stashBuffer;

	private ImageWorkManager(ActorContext<Command> context, StashBuffer<Command> stashBuffer) {
		this.context = context;
		this.stashBuffer = stashBuffer;
	}

	public static Behavior<Command> create() {
		return Behaviors.setup(
				context -> {
					ActorRef<WorkPullingProducerController.RequestNext<ImageConverter.ConversionJob>>
							requestNextAdapter =
							context.messageAdapter(
									WorkPullingProducerController.requestNextClass(), WrappedRequestNext::new);
					ActorRef<WorkPullingProducerController.Command<ImageConverter.ConversionJob>>
							producerController =
							context.spawn(
									WorkPullingProducerController.create(
											ImageConverter.ConversionJob.class,
											"workManager",
											ImageConverter.SERVICE_KEY,
											Optional.empty()),
									"producerController");
					producerController.tell(new WorkPullingProducerController.Start<>(requestNextAdapter));

					return Behaviors.withStash(
							1000, stashBuffer -> new ImageWorkManager(context, stashBuffer).waitForNext());
				});
	}

	private Behavior<Command> waitForNext() {
		return Behaviors.receive(Command.class)
				.onMessage(WrappedRequestNext.class, this::onWrappedRequestNext)
				.onMessage(Convert.class, this::onConvertWait)
				.onMessage(GetResult.class, this::onGetResult)
				.build();
	}

	private Behavior<Command> onWrappedRequestNext(WrappedRequestNext w) {
		return stashBuffer.unstashAll(active(w.next));
	}

	private Behavior<Command> onConvertWait(Convert convert) {
		if (stashBuffer.isFull()) {
			context.getLog().warn("Too many Convert requests.");
			return Behaviors.same();
		} else {
			stashBuffer.stash(convert);
			return Behaviors.same();
		}
	}

	private Behavior<Command> onGetResult(GetResult get) {
		// TODO retrieve the stored result and reply
		return Behaviors.same();
	}

	private Behavior<Command> active(
			WorkPullingProducerController.RequestNext<ImageConverter.ConversionJob> next) {
		return Behaviors.receive(Command.class)
				.onMessage(Convert.class, c -> onConvert(c, next))
				.onMessage(GetResult.class, this::onGetResult)
				.onMessage(WrappedRequestNext.class, this::onUnexpectedWrappedRequestNext)
				.build();
	}

	private Behavior<Command> onUnexpectedWrappedRequestNext(WrappedRequestNext w) {
		throw new IllegalStateException("Unexpected RequestNext");
	}

	private Behavior<Command> onConvert(
			Convert convert,
			WorkPullingProducerController.RequestNext<ImageConverter.ConversionJob> next) {
		UUID resultId = UUID.randomUUID();
		next.sendNextTo()
				.tell(
						new ImageConverter.ConversionJob(
								resultId, convert.fromFormat, convert.toFormat, convert.image));
		return waitForNext();
	}
}

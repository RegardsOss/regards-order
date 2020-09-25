package fr.cnes.regards.modules.processing.demo.engine.event;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.modules.processing.domain.service.IExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StepEventHandler implements ApplicationListener<ApplicationReadyEvent>, IHandler<StepEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StepEventHandler.class);

    private final ISubscriber subscriber;
    private final IExecutionService execService;

    @Autowired
    public StepEventHandler(
            ISubscriber subscriber,
            IExecutionService execService
    ) {
        this.subscriber = subscriber;
        this.execService = execService;
    }

    @Override public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(StepEvent.class, this);
    }

    @Override public void handle(String tenant, StepEvent message) {
        UUID execId = message.getExecId();
        LOGGER.info("exec={} - Async execution step received", execId);
        execService.createContext(execId)
            .flatMap(ctx -> ctx.sendEvent(message::getStep))
            .block();
        // Dirty block because we want to propagate errors as exceptions to prevent commit in rabbitmq transaction.
        // Defeats the purpose of the reactive interfaces, but there is no reactive IHandler yet.
        // TODO: If one day there is a reactive handler, adapt this code to run without blocking
    }

}
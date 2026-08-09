package com.informationplatform.hub.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcTaskDecoratorTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesSubmissionSnapshotAndRestoresExecutorContext() {
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        MDC.put(OperationalLogContext.REQUEST_ID, "request-1");
        MDC.put(OperationalLogContext.USERNAME, "owner");
        AtomicReference<String> observedRequestId = new AtomicReference<>();
        AtomicReference<String> observedUsername = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> {
            observedRequestId.set(MDC.get(OperationalLogContext.REQUEST_ID));
            observedUsername.set(MDC.get(OperationalLogContext.USERNAME));
            MDC.put(OperationalLogContext.REQUEST_ID, "task-mutated");
        });

        MDC.clear();
        MDC.put(OperationalLogContext.REQUEST_ID, "executor-existing");
        decorated.run();

        assertThat(observedRequestId).hasValue("request-1");
        assertThat(observedUsername).hasValue("owner");
        assertThat(MDC.get(OperationalLogContext.REQUEST_ID)).isEqualTo("executor-existing");
        assertThat(MDC.get(OperationalLogContext.USERNAME)).isNull();
    }

    @Test
    void emptySystemContextDoesNotPropagateStaleUsername() {
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        MDC.clear();
        AtomicReference<String> observedUsername = new AtomicReference<>();
        Runnable decorated = decorator.decorate(
                () -> observedUsername.set(MDC.get(OperationalLogContext.USERNAME)));

        MDC.put(OperationalLogContext.USERNAME, "stale-worker-user");
        decorated.run();

        assertThat(observedUsername).hasValue(null);
        assertThat(MDC.get(OperationalLogContext.USERNAME)).isEqualTo("stale-worker-user");
    }
}

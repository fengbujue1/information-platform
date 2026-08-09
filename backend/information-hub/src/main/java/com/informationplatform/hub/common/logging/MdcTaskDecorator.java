package com.informationplatform.hub.common.logging;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

/** 将提交线程的 MDC 快照安全传播到异步线程，并在执行后恢复线程原上下文。 */
@Component
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> submittingContext = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> executorContext = MDC.getCopyOfContextMap();
            try {
                replaceContext(submittingContext);
                runnable.run();
            } finally {
                replaceContext(executorContext);
            }
        };
    }

    private void replaceContext(Map<String, String> context) {
        MDC.clear();
        if (context != null && !context.isEmpty()) {
            MDC.setContextMap(context);
        }
    }
}

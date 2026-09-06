package com.study.flashsale.util;

import com.study.flashsale.context.TraceContext;
import org.slf4j.MDC;

public class TraceTaskWrapper {

    private static final String TRACE_ID = "traceId";

    private TraceTaskWrapper() {
    }

    public static Runnable wrap(Runnable task) {
        String traceId = TraceContext.getTraceId();

        return () -> {
            try {
                if (traceId != null) {
                    TraceContext.setTraceId(traceId);
                    MDC.put(TRACE_ID, traceId);
                }
                task.run();
            } finally {
                MDC.remove(TRACE_ID);
                TraceContext.clear();
            }
        };
    }
}

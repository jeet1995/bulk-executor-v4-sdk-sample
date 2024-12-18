package org.example.logging;

import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.event.LoggingEvent;

import java.util.concurrent.atomic.AtomicInteger;

public class CosmosDiagnosticsLoggingThrottler extends Filter<LoggingEvent> {

    private final static int COUNT_THRESHOLD = 20;
    private final static long LOG_INTERVAL = 60_000;

    private final AtomicInteger count = new AtomicInteger(0);
    private volatile long lastLogTime = System.currentTimeMillis();

    @Override
    public FilterReply decide(LoggingEvent loggingEvent) {
        if (loggingEvent.getLoggerName().contains("CosmosDiagnosticsLogger")) {

            if (count.get() < COUNT_THRESHOLD) {
                System.out.println("Count threshold satisfied");
                count.incrementAndGet();
                lastLogTime = System.currentTimeMillis();
                return FilterReply.ACCEPT;
            } else if (System.currentTimeMillis() - lastLogTime > LOG_INTERVAL)  {

                System.out.println("Count threshold not satisfied but time window threshold satisfied");
                count.set(1);
                lastLogTime = System.currentTimeMillis();
                return FilterReply.ACCEPT;
            } else {

                System.out.println("Count threshold not satisfied and time window threshold not satisfied");

                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }
}

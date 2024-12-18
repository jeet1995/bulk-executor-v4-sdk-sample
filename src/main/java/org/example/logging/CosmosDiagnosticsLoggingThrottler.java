package org.example.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.concurrent.atomic.AtomicInteger;

public class CosmosDiagnosticsLoggingThrottler extends Filter<ILoggingEvent> {

    private final static int COUNT_THRESHOLD = 10;
    private final static long LOG_INTERVAL = 60_000;

    private final AtomicInteger count = new AtomicInteger(0);
    private volatile long lastLogTime = System.currentTimeMillis();

    @Override
    public FilterReply decide(ILoggingEvent loggingEvent) {
        if (loggingEvent.getLoggerName().contains("CosmosDiagnosticsLogger")) {

            if (count.get() < COUNT_THRESHOLD) {
                count.incrementAndGet();
                lastLogTime = System.currentTimeMillis();
                return FilterReply.ACCEPT;
            } else if (System.currentTimeMillis() - lastLogTime > LOG_INTERVAL)  {
                count.set(1);
                lastLogTime = System.currentTimeMillis();
                return FilterReply.ACCEPT;
            } else {
                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }
}

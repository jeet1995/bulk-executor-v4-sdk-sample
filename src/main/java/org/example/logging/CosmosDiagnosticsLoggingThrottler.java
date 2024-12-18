package org.example.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

import java.util.concurrent.atomic.AtomicInteger;

@Plugin(name = "CosmosDiagnosticsLoggingThrottler", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public final class CosmosDiagnosticsLoggingThrottler extends AbstractFilter {

    private final static int COUNT_THRESHOLD = 20;
    private final static long LOG_INTERVAL = 60_000;

    private final AtomicInteger count = new AtomicInteger(0);
    private volatile long lastLogTime = System.currentTimeMillis();

    private CosmosDiagnosticsLoggingThrottler(final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return filter(logger);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter(logger);
    }

    private Result filter(Logger logger) {
        if (logger.getName().contains("CosmosDiagnosticsLogger")) {

            if (count.get() < COUNT_THRESHOLD) {
                System.out.println("Count threshold satisfied");
                count.incrementAndGet();
                lastLogTime = System.currentTimeMillis();
                return onMatch;
            } else if (System.currentTimeMillis() - lastLogTime > LOG_INTERVAL)  {

                System.out.println("Count threshold not satisfied but time window threshold satisfied");
                count.set(1);
                lastLogTime = System.currentTimeMillis();
                return onMatch;
            } else {

                System.out.println("Count threshold not satisfied and time window threshold not satisfied");

                return onMismatch;
            }
        }

        return Result.NEUTRAL;
    }

    @PluginFactory
    public static CosmosDiagnosticsLoggingThrottler createFilter(
            @PluginAttribute(AbstractFilterBuilder.ATTR_ON_MATCH) final Result match,
            @PluginAttribute(AbstractFilterBuilder.ATTR_ON_MISMATCH) final Result mismatch) {

        return new CosmosDiagnosticsLoggingThrottler(match, mismatch);
    }
}

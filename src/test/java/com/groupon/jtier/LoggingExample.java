package com.groupon.jtier;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingExample {

    @Test
    public void testDiagnosticContextOnInfection() throws Exception {

        try (CtxAttachment i = Ctx.empty().attachToThread()) {
            i.whenDetached().thenRun(MDC::clear);

            MDC.put("name", "grumbly");
            Logger logger = LoggerFactory.getLogger(LoggingExample.class);
            logger.debug("log");
        }

        assertThat(MDC.get("name")).isNull();

        TestLogger logger = TestLoggerFactory.getTestLogger(LoggingExample.class);
        ImmutableList<LoggingEvent> events = logger.getLoggingEvents();

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getMdc()).containsEntry("name", "grumbly");

        logger.clearAll();
    }
}

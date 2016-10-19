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

        final Ctx c = Ctx.empty();
        c.onAttach(() -> MDC.put("name", "grumbly"));
        c.onDetach(MDC::clear);

        try (Ctx ignored = c.attachToThread()) {
            MDC.put("name", "grumbly");
            final Logger logger = LoggerFactory.getLogger(LoggingExample.class);
            logger.debug("log");
        }

        assertThat(MDC.get("name")).isNull();

        final TestLogger logger = TestLoggerFactory.getTestLogger(LoggingExample.class);
        final ImmutableList<LoggingEvent> events = logger.getLoggingEvents();

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getMdc()).containsEntry("name", "grumbly");

        logger.clearAll();
    }
}

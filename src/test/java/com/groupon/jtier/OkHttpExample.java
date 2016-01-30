package com.groupon.jtier;

import okhttp3.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class OkHttpExample {

    private static final Ding.Key<UUID> REQUEST_ID = Ding.key("X-Request-Id", UUID.class);

    @Rule
    public MockWebServer web = new MockWebServer();

    private OkHttpClient ok;

    @Before
    public void setUp() throws Exception {
        Dispatcher d = new Dispatcher(InfectingExecutor.infect(Executors.newCachedThreadPool()));
        ok = new OkHttpClient.Builder()
                .dispatcher(d)
                .addInterceptor(new ExampleInterceptor())
                .build();
    }

    @Test
    public void testOkRequestIdPropagation() throws Exception {
        web.enqueue(new MockResponse().setBody("hello world")
                                      .setResponseCode(200)
                                      .addHeader("Content-Type", "text/plain"));

        UUID id = UUID.randomUUID();

        try (Infection _i = Ding.empty().with(REQUEST_ID, id).infectThread()) {
            Call call = ok.newCall(new Request.Builder().url(web.url("/")).build());
            Response response = call.execute();
            assertThat(response.code()).isEqualTo(200);
        }

        assertThat(web.takeRequest().getHeader("X-Request-Id")).isEqualTo(id.toString());

    }

    public static class ExampleInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request req = chain.request();

            if (Infection.isCurrentThreadInfected()) {
                Ding ctx = Infection.ding().get();
                UUID reqid = ctx.get(REQUEST_ID);
                if (reqid != null) {
                    req = req.newBuilder()
                             .addHeader("X-Request-Id", ctx.get(REQUEST_ID).toString())
                             .build();
                }
            }

            return chain.proceed(req);
        }
    }
}

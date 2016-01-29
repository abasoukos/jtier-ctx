package com.groupon.jtier;

import okhttp3.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class OkHttpTransparentInfectionExampleTest {

    private static final Ding.Key<UUID> REQUEST_ID = Ding.key("X-Request-Id", UUID.class);

    @Rule
    public MockWebServer web = new MockWebServer();

    @Test
    public void testFoo() throws Exception {
        web.enqueue(new MockResponse().setBody("hello world")
                                      .setResponseCode(200)
                                      .addHeader("Content-Type", "text/plain"));

        Dispatcher d = new Dispatcher(InfectingExecutor.infect(Executors.newCachedThreadPool()));
        OkHttpClient ok = new OkHttpClient.Builder()
                .dispatcher(d)
                .addInterceptor(chain -> {
                    Request req = chain.request();
                    Ding ctx = Ding.summonThreadContext().get();
                    Request withId = req.newBuilder()
                                        .addHeader("X-Request-Id", ctx.get(REQUEST_ID).toString())
                                        .build();
                    return chain.proceed(withId);
                }).build();

        UUID id = UUID.randomUUID();

        try (Ding _d = Ding.empty().with(REQUEST_ID, id).infectThread()) {
            Call call = ok.newCall(new Request.Builder().url(web.url("/")).build());
            Response response = call.execute();
            assertThat(response.code()).isEqualTo(200);
        }

        assertThat(web.takeRequest().getHeader("X-Request-Id")).isEqualTo(id.toString());

    }

}

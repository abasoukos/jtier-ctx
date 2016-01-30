package com.groupon.jtier;

import okhttp3.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.RxJavaCallAdapterFactory;
import rx.Observable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientInfectionExampleTest {

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

        try (Ding _d = Ding.empty().with(REQUEST_ID, id).infectThread()) {
            Call call = ok.newCall(new Request.Builder().url(web.url("/")).build());
            Response response = call.execute();
            assertThat(response.code()).isEqualTo(200);
        }

        assertThat(web.takeRequest().getHeader("X-Request-Id")).isEqualTo(id.toString());

    }

    @Test
    @Ignore
    public void testExplicitOkCancellation() throws Exception {
        web.enqueue(new MockResponse().setBody("hello world")
                                      .setResponseCode(200)
                                      .setBodyDelay(2, TimeUnit.SECONDS)
                                      .addHeader("Content-Type", "text/plain"));

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean failed = new AtomicBoolean(false);
        final AtomicBoolean completed = new AtomicBoolean(false);
        try (Ding d = Ding.empty().infectThread()) {
            Call call = ok.newCall(new Request.Builder().url(web.url("/")).build());

            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    failed.set(true);
                    latch.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    completed.set(true);
                    latch.countDown();
                }
            });

            d.whenCancelled().thenRun(call::cancel);

            d.cancel();
        }

        latch.await(1, TimeUnit.SECONDS);
        assertThat(failed.get()).isTrue();
        assertThat(completed.get()).isFalse();
    }

    public static class ExampleInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request req = chain.request();

            if (Ding.isCurrentThreadInfected()) {
                Ding ctx = Ding.summonThreadContext().get();
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

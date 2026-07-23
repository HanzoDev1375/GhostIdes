package ir.hanzodev1375.ghostide.postman.network;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import ir.hanzodev1375.ghostide.postman.model.KeyValueItem;
import ir.hanzodev1375.ghostide.postman.model.RequestSnapshot;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The actual HTTP request engine, built on top of OkHttp. This is the one
 * class that turns a {@link RequestSnapshot} into a real network call and
 * hands the result back on the main thread.
 */
public class HttpEngine {

    /** Responses larger than this are truncated for display (but the real size is still reported). */
    private static final long MAX_DISPLAY_BYTES = 2L * 1024 * 1024;

    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HttpEngine(int timeoutSeconds, boolean disableSslVerification) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);

        if (disableSslVerification) {
            applyInsecureTrustManager(builder);
        }

        this.client = builder.build();
    }

    /** Result of one request/response round trip (or a failure before a response ever arrived). */
    public static class HttpResult {
        public boolean success;
        public int statusCode;
        public String statusMessage = "";
        public List<String[]> headers = new ArrayList<>();
        public String body = "";
        public long timeMs;
        public long sizeBytes;
        public boolean bodyTruncated;
        public String errorMessage;
    }

    public interface Callback {
        void onResult(HttpResult result);
    }

    public void execute(RequestSnapshot snapshot, Callback callback) {
        String rawUrl = snapshot.url == null ? "" : snapshot.url.trim();
        if (!rawUrl.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            rawUrl = "https://" + rawUrl;
        }

        HttpUrl parsed = HttpUrl.parse(rawUrl);
        if (parsed == null) {
            HttpResult failure = new HttpResult();
            failure.success = false;
            failure.errorMessage = "That doesn't look like a valid URL.";
            deliver(callback, failure);
            return;
        }

        HttpUrl.Builder urlBuilder = parsed.newBuilder();
        for (KeyValueItem param : snapshot.params) {
            String key = param.getKey() == null ? "" : param.getKey().trim();
            if (param.isEnabled() && !key.isEmpty()) {
                urlBuilder.addQueryParameter(key, param.getValue());
            }
        }

        String method = snapshot.method == null ? "GET" : snapshot.method.toUpperCase();
        Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());

        for (KeyValueItem header : snapshot.headers) {
            String key = header.getKey() == null ? "" : header.getKey().trim();
            if (header.isEnabled() && !key.isEmpty()) {
                try {
                    requestBuilder.header(key, header.getValue());
                } catch (IllegalArgumentException ignored) {
                    // an invalid header name/value was typed; just skip that row
                }
            }
        }

        RequestBody body = null;
        boolean methodAllowsBody = !method.equals("GET") && !method.equals("HEAD");
        if (methodAllowsBody) {
            if (snapshot.bodyType == 1 && snapshot.rawBody != null && !snapshot.rawBody.isEmpty()) {
                MediaType mediaType = MediaType.parse(
                        snapshot.rawContentType == null || snapshot.rawContentType.isEmpty()
                                ? "text/plain; charset=utf-8"
                                : snapshot.rawContentType);
                body = RequestBody.create(snapshot.rawBody, mediaType);
            } else if (snapshot.bodyType == 2) {
                FormBody.Builder formBuilder = new FormBody.Builder();
                boolean any = false;
                for (KeyValueItem field : snapshot.formFields) {
                    String key = field.getKey() == null ? "" : field.getKey().trim();
                    if (field.isEnabled() && !key.isEmpty()) {
                        formBuilder.add(key, field.getValue());
                        any = true;
                    }
                }
                if (any) {
                    body = formBuilder.build();
                }
            }
        }

        boolean methodRequiresBody = method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
        if (body == null && methodRequiresBody) {
            body = RequestBody.create(new byte[0], null);
        }

        Request request;
        try {
            request = requestBuilder.method(method, body).build();
        } catch (IllegalArgumentException e) {
            HttpResult failure = new HttpResult();
            failure.success = false;
            failure.errorMessage = e.getMessage();
            deliver(callback, failure);
            return;
        }

        long startNanos = System.nanoTime();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                HttpResult result = new HttpResult();
                result.success = false;
                result.errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                deliver(callback, result);
            }

            @Override
            public void onResponse(Call call, Response response) {
                long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
                HttpResult result = new HttpResult();
                result.success = true;
                result.statusCode = response.code();
                result.statusMessage = response.message();
                result.timeMs = elapsedMs;

                Headers responseHeaders = response.headers();
                for (int i = 0; i < responseHeaders.size(); i++) {
                    result.headers.add(new String[]{responseHeaders.name(i), responseHeaders.value(i)});
                }

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody != null) {
                        byte[] bytes = responseBody.bytes();
                        result.sizeBytes = bytes.length;
                        int limit = (int) Math.min(bytes.length, MAX_DISPLAY_BYTES);
                        result.bodyTruncated = bytes.length > MAX_DISPLAY_BYTES;
                        result.body = new String(bytes, 0, limit, StandardCharsets.UTF_8);
                    }
                } catch (IOException e) {
                    result.body = "";
                    result.errorMessage = "Response received but the body could not be read: " + e.getMessage();
                }

                deliver(callback, result);
            }
        });
    }

    private void deliver(Callback callback, HttpResult result) {
        mainHandler.post(() -> callback.onResult(result));
    }

    /**
     * WARNING: this trusts every TLS certificate. It exists purely so people
     * can test servers with a self-signed cert during development, and is
     * only ever enabled if the user explicitly flips the setting on. Never
     * enabled by default.
     */
    private void applyInsecureTrustManager(OkHttpClient.Builder builder) {
        try {
            final X509TrustManager trustAllManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllManager}, new SecureRandom());
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(socketFactory, trustAllManager);
            HostnameVerifier trustAllHostnames = (hostname, session) -> true;
            builder.hostnameVerifier(trustAllHostnames);
        } catch (Exception ignored) {
            // If anything goes wrong here we simply fall back to normal, secure verification.
        }
    }
}

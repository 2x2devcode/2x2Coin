package com.x2xcoin.wallet.api;

import okhttp3.CertificatePinner;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class X2xHttpClientFactory {
    private X2xHttpClientFactory() {
    }

    public static OkHttpClient create(PinProvider pinProvider, String userAgent) {
        return create(pinProvider, userAgent, Duration.ofSeconds(20), Duration.ofSeconds(180));
    }

    public static OkHttpClient create(
            PinProvider pinProvider,
            String userAgent,
            Duration connectTimeout,
            Duration readTimeout
    ) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .callTimeout(readTimeout.plusSeconds(5).toMillis(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(userAgentInterceptor(userAgent));
        CertificatePinner.Builder pinnerBuilder = new CertificatePinner.Builder();
        boolean hasPins = false;
        for (String host : pinProvider.pinnedHosts()) {
            for (String pin : pinProvider.pinsForHost(host)) {
                if (pin == null || pin.isBlank()) {
                    continue;
                }
                pinnerBuilder.add(host, CertificatePin.okHttpPinFromHex(pin));
                hasPins = true;
            }
        }
        if (hasPins) {
            builder.certificatePinner(pinnerBuilder.build());
        }
        return builder.build();
    }

    private static Interceptor userAgentInterceptor(String userAgent) {
        return chain -> {
            Request request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", userAgent)
                    .build();
            return chain.proceed(request);
        };
    }
}

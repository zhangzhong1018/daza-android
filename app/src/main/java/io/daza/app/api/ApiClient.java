/**
 * Copyright (C) 2015 JianyingLi <lijy91@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.daza.app.api;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.daza.app.BuildConfig;
import io.daza.app.util.Auth;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final int CONNECT_TIMEOUT_MILLIS = 20000; // 20 seconds
    private static final int READ_TIMEOUT_MILLIS    = 20000; // 20 seconds
    private static final int WRITE_TIMEOUT_MILLIS   = 20000; // 20 seconds

    public static Api API;

    private static final Interceptor COOKIES_REQUEST_INTERCEPTOR = new Interceptor() {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request.Builder builder = chain.request().newBuilder();

            builder.addHeader("Accept", "application/json");
            if (Auth.check()) {
                builder.addHeader("Authorization", "Bearer " + Auth.jwtToken().getAccess_token());
            }

            return chain.proceed(builder.build());
        }
    };

    static {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(generateDefaultOkHttp())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        API = retrofit.create(Api.class);
    }

    private static OkHttpClient generateDefaultOkHttp() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addNetworkInterceptor(new StethoInterceptor());
        builder.connectTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        builder.readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        builder.writeTimeout(WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        builder.addInterceptor(COOKIES_REQUEST_INTERCEPTOR);

        final TrustManager[] certs = new TrustManager[]{new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain,
                                           final String authType) throws CertificateException {
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain,
                                           final String authType) throws CertificateException {
            }
        }};

        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, certs, new SecureRandom());
        } catch (final java.security.GeneralSecurityException ex) {
        }

        try {
            final HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(final String hostname,
                                      final SSLSession session) {
                    return true;
                }
            };
            builder.hostnameVerifier(hostnameVerifier);
            builder.sslSocketFactory(ctx.getSocketFactory());
        } catch (final Exception e) {
        }
        return builder.build();
    }
}

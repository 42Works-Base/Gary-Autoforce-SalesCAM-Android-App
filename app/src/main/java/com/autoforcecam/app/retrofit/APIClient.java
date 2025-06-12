package com.autoforcecam.app.retrofit;


import android.content.Context;
import com.autoforcecam.app.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class APIClient {

    private static Retrofit retrofit = null, retrofitUpload = null, retrofitShot = null;

   /* Live URL */

    final public static String API_TYPE = "live";
    final static String BASE_PATH = "api.autoforce.io/public/api/cam/";
    final static String BASE_SHOT_URL = "https://api.shotstack.io/v1/";

    /* Sandbox URL */

//    final public static String API_TYPE = "sandbox";
//    final static String BASE_PATH = "staging-api.autoforce.io/public/api/cam/";
//    final static String BASE_SHOT_URL = "https://api.shotstack.io/stage/";

    /* Common */

    final static String BASE_URL = "https://" + BASE_PATH;
    final static String BASE_UPLOAD_URL = "http://" + BASE_PATH;
    final public static String SHOT_CALLBACK = BASE_URL + "shotstack_callback";
    private static int REQUEST_TIMEOUT = 60, UPLOAD_REQUEST_TIMEOUT = 15;
    private static OkHttpClient okHttpClient, okHttpUploadClient, okHttpShotClient;



    /************************************ For Session Based *********************************************/


    public static Retrofit getClient(Context context) {

        Gson gson = new GsonBuilder().setLenient().create();

        if (okHttpClient == null){
            initOkHttp(context);
        }

        if (retrofit == null) {

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;

    }

    private static void initOkHttp(final Context context) {

        OkHttpClient.Builder httpClient = new OkHttpClient().newBuilder()
                .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClient.addInterceptor(interceptor);

        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json");

                Request request = requestBuilder.build();
                return chain.proceed(request);

            }
        });

        okHttpClient = httpClient.build();

    }


    /************************************ For Media Upload *********************************************/


    public static Retrofit getUploadClient(Context context) {

        Gson gson = new GsonBuilder().setLenient().create();

        if (okHttpUploadClient == null){

            initUploadOkHttp(context);

        }

        if (retrofitUpload == null) {

            retrofitUpload = new Retrofit.Builder()
                    .baseUrl(BASE_UPLOAD_URL)
                    .client(okHttpUploadClient)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofitUpload;
    }

    private static void initUploadOkHttp(final Context context) {

        OkHttpClient.Builder httpClient = new OkHttpClient().newBuilder()
                .connectTimeout(UPLOAD_REQUEST_TIMEOUT, TimeUnit.MINUTES)
                .readTimeout(UPLOAD_REQUEST_TIMEOUT, TimeUnit.MINUTES)
                .writeTimeout(UPLOAD_REQUEST_TIMEOUT, TimeUnit.MINUTES);

        /*  Removed logger because it leads to crashes if heavy file is
            uploaded and it logs a lot of binary values as logs*/

        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json");

                Request request = requestBuilder.build();
                return chain.proceed(request);

            }
        });

        okHttpUploadClient = httpClient.build();

    }


    /************************************ For Shot Stack *********************************************/


    public static Retrofit getShotClient(Context context) {

        Gson gson = new GsonBuilder().setLenient().create();

        if (okHttpUploadClient == null){
            initShotOkHttp(context);
        }

        if (retrofitShot == null) {
            retrofitShot = new Retrofit.Builder()
                    .baseUrl(BASE_SHOT_URL)
                    .client(okHttpShotClient)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }

        return retrofitShot;

    }

    private static void initShotOkHttp(final Context context) {

        OkHttpClient.Builder httpClient = new OkHttpClient().newBuilder()
                .connectTimeout(UPLOAD_REQUEST_TIMEOUT, TimeUnit.MINUTES)
                .readTimeout(UPLOAD_REQUEST_TIMEOUT, TimeUnit.MINUTES)
                .writeTimeout(UPLOAD_REQUEST_TIMEOUT, TimeUnit.MINUTES);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClient.addInterceptor(interceptor);

        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json");

                SessionManager sessionManager = new SessionManager(context);
                String shotStackKey = "";
                if(API_TYPE.equals("live")){
                    shotStackKey = sessionManager.getLiveKeys().getShotstack_key();
                } else {
                    shotStackKey = sessionManager.getSandBoxKeys().getShotstack_key();
                }

                requestBuilder.addHeader("x-api-key", shotStackKey);

                Request request = requestBuilder.build();
                return chain.proceed(request);

            }
        });

        okHttpShotClient = httpClient.build();

    }

}

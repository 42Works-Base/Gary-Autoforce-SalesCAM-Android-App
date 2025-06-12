package com.autoforcecam.app.retrofit;

import com.autoforcecam.app.responses.LoginResponse;
import com.autoforcecam.app.responses.MediaUploadResponse;
import com.autoforcecam.app.responses.OverlayResponse;
import com.autoforcecam.app.responses.SendResponse;
import com.autoforcecam.app.responses.ShotResponse;
import com.autoforcecam.app.responses.SuccessResponse;

import io.reactivex.Single;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;

public interface APIInterface {

    @FormUrlEncoded
    @POST("login")
    Single<LoginResponse> loginUser(
            @Field("email") String email,
            @Field("code") String code,
            @Field("device_id") String deviceId,
            @Field("device_type") String deviceType,
            @Field("device_token") String deviceToken
    );

    @FormUrlEncoded
    @POST("get_overlays")
    Single<OverlayResponse> getOverlays(
            @Field("location_id") String locationId,
            @Field("user_id") String userId,
            @Field("user_type") String userType
    );

    @FormUrlEncoded
    @POST("share")
    Single<SendResponse> shareMedia(
            @Field("email") String email,
            @Field("email_body") String emailBody,
            @Field("email_body_yt") String ytEmailBody,
            @Field("mobile") String mobile,
            @Field("preformatted_sms") String preformatted_sms,
            @Field("preformatted_sms_yt") String ytPreformatted_sms,
            @Field("link") String link,
            @Field("location_id") String locationId,
            @Field("type") String type,
            @Field("post_id") String postId,
            @Field("user_id") String userId,
            @Field("user_type") String userType
    );

    @FormUrlEncoded
    @POST("capture")
    Single<SuccessResponse> updateAnalytics(
            @Field("user_id") String userId,
            @Field("location_id") String locationId,
            @Field("type") String type,
            @Field("user_type") String userType
    );

    @Multipart
    @POST("post_overlay")
    Single<MediaUploadResponse> mediaUpload(
            @Part MultipartBody.Part image,
            @Part("location_id") RequestBody locationId,
            @Part("user_id") RequestBody userId,
            @Part("type") RequestBody type,
            @Part("uploaded_on") RequestBody uploadedOn,
            @Part("caption") RequestBody postDescription,
            @Part("yt_title_text") RequestBody ytTitle,
            @Part("yt_description_text") RequestBody ytDescription,
            @Part("yt_default_tags") RequestBody ytTags,
            @Part("yt_default_listing") RequestBody ytListing,
            @Part("publish_at") RequestBody publishDate,
            @Part("album_uri") RequestBody albumId,
            @Part("vimeo_password") RequestBody vimeoPassword,
            @Part("user_type") RequestBody userType
    );

    @Multipart
    @POST("upload_video")
    Single<SuccessResponse> videoUpload(
            @Part MultipartBody.Part image,
            @Part("location_id") RequestBody locationId,
            @Part("user_id") RequestBody userId,
            @Part("user_type") RequestBody userType,
            @Part("intro") RequestBody introId,
            @Part("outro") RequestBody outroId,
            @Part("background") RequestBody musicId,
            @Part("overlay") RequestBody overlayId,
            @Part("name") RequestBody name,
            @Part("customer_name") RequestBody customerName,
            @Part("email") RequestBody email,
            @Part("phone") RequestBody phone,
            @Part("vimeo_title_text") RequestBody title,
            @Part("vimeo_description_text") RequestBody description,
            @Part("vimeo_default_tags") RequestBody tags,
            @Part("vimeo_password") RequestBody password,
            @Part("year_make_model") RequestBody year,
            @Part("share_video") RequestBody shareVideo,
            @Part("show_review") RequestBody showReview
    );

    @POST("render")
    Single<ShotResponse> processVideoData(
            @Body RequestBody requestBody
    );

    @FormUrlEncoded
    @POST("shotstack_video")
    Single<SuccessResponse> saveShotData(
            @Field("phone") String phone,
            @Field("email") String email,
            @Field("year_make_model") String subject,
            @Field("share_video") String shareVideo,
            @Field("user_type") String userType,
            @Field("name") String name,
            @Field("vimeo_title_text") String vimeoTitle,
            @Field("user_id") String userId,
            @Field("location_id") String locationId,
            @Field("vimeo_description_text") String vimeoDesc,
            @Field("customer_name") String customerName,
            @Field("vimeo_default_tags") String vimeoTags,
            @Field("shotstack_ref_id") String shotStackId,
            @Field("device_type") String deviceType,
            @Field("device_token") String deviceToken,
            @Field("show_review") String showReview,
            @Field("aws_temp_url") String awsTempUrls,
            @Field("sms_format") String sms_format,
            @Field("email_format") String email_format,
            @Field("remove_bg_noise") int remove_bg_noise

    );

    @FormUrlEncoded
    @POST("share_video")
    Single<SendResponse> shareVimeoURL(
            @Field("phone") String phone,
            @Field("email") String email,
            @Field("year_make_model") String subject,
            @Field("share_video") String shareVideo,
            @Field("user_type") String userType,
            @Field("name") String name,
            @Field("user_id") String userId,
            @Field("location_id") String locationId,
            @Field("customer_name") String customerName,
            @Field("device_type") String deviceType,
            @Field("device_token") String deviceToken,
            @Field("show_review") String showReview,
            @Field("vimeo_path") String vimeoPath,
            @Field("sms_format") String sms_format,
            @Field("email_format") String email_format,
            @Field("vimeo_title_text") String vimeo_title_text
    );

}
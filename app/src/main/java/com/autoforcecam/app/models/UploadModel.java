package com.autoforcecam.app.models;

import java.util.ArrayList;

public class UploadModel {

    public String mergedFilePath, selectedOverlayUrl,selectedOverlayId, selectedIntroContainsAudio, selectedIntroUrl,
            selectedOutroContainsAudio, selectedOutroUrl, selectedMusicUrl, shotJson, shotId,
            locationId, userId, userType, name, customerName, email, phone, title, description,
            tags, year, timeStamp, uploadError, shareVideo, showReview, mergedS3SegmentPath,smsBody,emailBody;
    public int selectedIntroLength, selectedOutroLength, selectedMusicLength, retryCount, mergeTotalDuration;
    public boolean isCompressionRequired,isDolbyRequired, mergedFileContainsAudio;
    public ArrayList<VideoSegmentModel> videoSegmentNames;

    public UploadModel() {}

    public UploadModel(ArrayList<VideoSegmentModel> videoSegmentNames, boolean isCompressionRequired,boolean isDolbyRequired,
                       String mergedFilePath, String selectedOverlayUrl, String selectedOverlayId, String selectedIntroContainsAudio,
                       String selectedIntroUrl, String selectedOutroContainsAudio, String selectedOutroUrl,
                       String selectedMusicUrl, int selectedIntroLength, int selectedOutroLength,
                       int selectedMusicLength, String shotJson, String shotId, String locationId,
                       String userId, String userType, String name, String customerName, String email,
                       String phone, String title, String description, String tags, String year,
                       String timeStamp, String uploadError, String shareVideo, String showReview,
                       int retryCount, boolean mergedFileContainsAudio, int mergeTotalDuration,
                       String mergedS3SegmentPath,String smsBody,String emailBody) {
        this.videoSegmentNames = videoSegmentNames;
        this.isCompressionRequired = isCompressionRequired;
        this.isDolbyRequired = isDolbyRequired;
        this.mergedFilePath = mergedFilePath;
        this.selectedOverlayUrl = selectedOverlayUrl;
        this.selectedOverlayId = selectedOverlayId;
        this.selectedIntroContainsAudio = selectedIntroContainsAudio;
        this.selectedIntroUrl = selectedIntroUrl;
        this.selectedOutroContainsAudio = selectedOutroContainsAudio;
        this.selectedOutroUrl = selectedOutroUrl;
        this.selectedMusicUrl = selectedMusicUrl;
        this.selectedIntroLength = selectedIntroLength;
        this.selectedOutroLength = selectedOutroLength;
        this.selectedMusicLength = selectedMusicLength;
        this.shotJson = shotJson;
        this.shotId = shotId;
        this.locationId = locationId;
        this.userId = userId;
        this.userType = userType;
        this.name = name;
        this.customerName = customerName;
        this.email = email;
        this.phone = phone;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.year = year;
        this.timeStamp = timeStamp;
        this.uploadError = uploadError;
        this.shareVideo = shareVideo;
        this.showReview = showReview;
        this.retryCount = retryCount;
        this.mergedFileContainsAudio = mergedFileContainsAudio;
        this.mergeTotalDuration = mergeTotalDuration;
        this.mergedS3SegmentPath = mergedS3SegmentPath;
        this.smsBody=smsBody;
        this.emailBody=emailBody;
    }

    public ArrayList<VideoSegmentModel> getVideoSegmentNames() {
        return videoSegmentNames;
    }

    public String getMergedFilePath() {
        return mergedFilePath;
    }

    public String getSelectedOverlayUrl() {
        return selectedOverlayUrl;
    }

    public String getSelectedOverlayUId() {
        return selectedOverlayId;
    }

    public String getSelectedIntroContainsAudio() {
        return selectedIntroContainsAudio;
    }

    public String getSelectedIntroUrl() {
        return selectedIntroUrl;
    }

    public String getSelectedOutroContainsAudio() {
        return selectedOutroContainsAudio;
    }

    public String getSelectedOutroUrl() {
        return selectedOutroUrl;
    }

    public String getSelectedMusicUrl() {
        return selectedMusicUrl;
    }

    public int getSelectedIntroLength() {
        return selectedIntroLength;
    }

    public int getSelectedOutroLength() {
        return selectedOutroLength;
    }

    public int getSelectedMusicLength() {
        return selectedMusicLength;
    }

    public boolean isCompressionRequired() {
        return isCompressionRequired;
    }

    public boolean isDolbyRequired() {
        return isDolbyRequired;
    }

    public String getShotJson() {
        return shotJson;
    }

    public String getShotId() {
        return shotId;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserType() {
        return userType;
    }

    public String getName() {
        return name;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTags() {
        return tags;
    }

    public String getYear() {
        return year;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getUploadError() {
        return uploadError;
    }

    public String getShareVideo() {
        return shareVideo;
    }

    public String getShowReview() {
        return showReview;
    }


    public String getSmsBody() {
        return smsBody;
    }

    public void setSmsBody(String smsBody) {
        this.smsBody = smsBody;
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setShotJson(String shotJson) {
        this.shotJson = shotJson;
    }

    public void setVideoSegmentNames(ArrayList<VideoSegmentModel> videoSegmentNames) {
        this.videoSegmentNames = videoSegmentNames;
    }

    public void setShotId(String shotId) {
        this.shotId = shotId;
    }

    public void setUploadError(String uploadError) {
        this.uploadError = uploadError;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getMergedS3SegmentPath() {
        return mergedS3SegmentPath;
    }

    public int getMergeTotalDuration() {
        return mergeTotalDuration;
    }

    public boolean isMergedFileContainsAudio() {
        return mergedFileContainsAudio;
    }

    public void setMergedS3SegmentPath(String mergedS3SegmentPath) {
        this.mergedS3SegmentPath = mergedS3SegmentPath;
    }
}

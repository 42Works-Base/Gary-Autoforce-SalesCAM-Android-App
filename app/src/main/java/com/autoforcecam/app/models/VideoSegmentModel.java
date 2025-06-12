package com.autoforcecam.app.models;

public class VideoSegmentModel {

    private long timeStamp;
    public String videoPath, compressedVideoPath;
    public boolean isVideoSelected, containsAudio;
    private String s3SegmentPath;
    private int length ;

    public VideoSegmentModel(long timeStamp, String videoPath, String compressedVideoPath, boolean isVideoSelected, String s3SegmentPath, int length, boolean containsAudio) {
        this.timeStamp = timeStamp;
        this.compressedVideoPath = compressedVideoPath;
        this.videoPath = videoPath;
        this.isVideoSelected = isVideoSelected;
        this.s3SegmentPath = s3SegmentPath;
        this.length = length;
        this.containsAudio = containsAudio;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getCompressedVideoPath() {
        return compressedVideoPath;
    }

    public void setCompressedVideoPath(String compressedVideoPath) {
        this.compressedVideoPath = compressedVideoPath;
    }

    public void setS3SegmentPath(String s3SegmentPath) {
        this.s3SegmentPath = s3SegmentPath;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getS3SegmentPath() {
        return s3SegmentPath;
    }

    public int getLength() {
        return length;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public boolean isVideoSelected() {
        return isVideoSelected;
    }

    public boolean isContainsAudio() {
        return containsAudio;
    }

    public void setContainsAudio(boolean containsAudio) {
        this.containsAudio = containsAudio;
    }
}

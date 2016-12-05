package com.twilio.video;


import java.util.ArrayList;
import java.util.List;

/**
 * Stats report contains stats for all the media track that exist in peer connection.
 */
public class StatsReport {
    private final String peerConnectionId;
    private List<LocalAudioTrackStats> localAudioTracksStats = new ArrayList<>();
    private List<LocalVideoTrackStats> localVideoTracksStats = new ArrayList<>();
    private List<AudioTrackStats> audioTracksStats = new ArrayList<>();
    private List<VideoTrackStats> videoTracksStats = new ArrayList<>();

    StatsReport(String peerConnectionId) {
        this.peerConnectionId = peerConnectionId;
    }

    /**
     * Returns the id of peer connection related to this report.
     */
    public String getPeerConnectionId() {
        return peerConnectionId;
    }

    /**
     * Returns stats for all local audio tracks in the peer connection.
     *
     * @return a list of local audio tracks stats
     */
    public List<LocalAudioTrackStats> getLocalAudioTrackStats() {
        return localAudioTracksStats;
    }

    /**
     * Returns stats for all local video tracks in the peer connection.
     *
     * @return a list of local video tracks stats
     */
    public List<LocalVideoTrackStats> getLocalVideoTrackStats() {
        return localVideoTracksStats;
    }

    /**
     * Returns stats for all remote audio tracks in the peer connection.
     *
     * @return a list of remote audio tracks stats
     */
    public List<AudioTrackStats> getAudioTrackStats() {
        return audioTracksStats;
    }

    /**
     * Returns stats for all remote video tracks in the peer connection.
     *
     * @return a list of remote video tracks stats
     */
    public List<VideoTrackStats> getVideoTrackStats() {
        return videoTracksStats;
    }

    void addLocalAudioTrackStats(LocalAudioTrackStats localAudioTrackStats) {
        localAudioTracksStats.add(localAudioTrackStats);
    }

    void addLocalVideoTrackStats(LocalVideoTrackStats localVideoTrackStats) {
        localVideoTracksStats.add(localVideoTrackStats);
    }

    void addAudioTrackStats(AudioTrackStats audioTrackStats) {
        audioTracksStats.add(audioTrackStats);
    }

    void addVideoTrackStats(VideoTrackStats videoTrackStats) {
        videoTracksStats.add(videoTrackStats);
    }
}

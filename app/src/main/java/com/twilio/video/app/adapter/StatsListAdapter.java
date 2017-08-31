/*
 * Copyright (C) 2017 Twilio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twilio.video.app.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import com.twilio.video.Camera2Capturer;
import com.twilio.video.RemoteAudioTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteAudioTrackStats;
import com.twilio.video.LocalAudioTrackStats;
import com.twilio.video.LocalVideoTrackStats;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.StatsReport;
import com.twilio.video.RemoteVideoTrackStats;
import com.twilio.video.VideoTrack;
import com.twilio.video.app.R;
import com.twilio.video.app.model.StatsListItem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class StatsListAdapter extends RecyclerView.Adapter<StatsListAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.stats_track_name) TextView trackNameText;
        @BindView(R.id.stats_track_id_value) TextView trackIdValueText;
        @BindView(R.id.stats_codec_value) TextView codecValueText;
        @BindView(R.id.stats_packets_value) TextView packetsValueText;
        @BindView(R.id.stats_bytes_title) TextView bytesTitleText;
        @BindView(R.id.stats_bytes_value) TextView bytesValueText;
        @BindView(R.id.stats_rtt_value) TextView rttValueText;
        @BindView(R.id.stats_jitter_value) TextView jitterValueText;
        @BindView(R.id.stats_audio_level_value) TextView audioLevelValueText;
        @BindView(R.id.stats_dimensions_value) TextView dimensionsValueText;
        @BindView(R.id.stats_framerate_value) TextView framerateValueText;

        @BindView(R.id.stats_track_id_row) TableRow trackIdTableRow;
        @BindView(R.id.stats_codec_row) TableRow codecTableRow;
        @BindView(R.id.stats_packets_row) TableRow packetsTableRow;
        @BindView(R.id.stats_bytes_row) TableRow bytesTableRow;
        @BindView(R.id.stats_rtt_row) TableRow rttTableRow;
        @BindView(R.id.stats_jitter_row) TableRow jitterTableRow;
        @BindView(R.id.stats_audio_level_row) TableRow audioLevelTableRow;
        @BindView(R.id.stats_dimensions_row) TableRow dimensionsTableRow;
        @BindView(R.id.stats_framerate_row) TableRow framerateTableRow;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private ArrayList<StatsListItem> statsListItems = new ArrayList<>();
    private Context context;
    private Handler handler;

    public StatsListAdapter(Context context) {
        this.context = context;
        handler = new Handler(Looper.getMainLooper());
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.stats_layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        StatsListItem item = statsListItems.get(position);
        holder.trackNameText.setText(item.trackName);
        holder.trackIdValueText.setText(item.trackId);
        holder.codecValueText.setText(item.codec);
        holder.packetsValueText.setText(String.valueOf(item.packetsLost));
        holder.bytesValueText.setText(String.valueOf(item.bytes));
        if (item.isLocalTrack) {
            holder.bytesTitleText.setText(context.getString(R.string.stats_bytes_sent));
            holder.rttValueText.setText(String.valueOf(item.rtt));
            holder.rttTableRow.setVisibility(View.VISIBLE);
        } else {
            holder.rttTableRow.setVisibility(View.GONE);
            holder.bytesTitleText.setText(context.getString(R.string.stats_bytes_received));
        }
        if (item.isAudioTrack) {
            holder.jitterValueText.setText(String.valueOf(item.jitter));
            holder.audioLevelValueText.setText(String.valueOf(item.audioLevel));
            holder.dimensionsTableRow.setVisibility(View.GONE);
            holder.framerateTableRow.setVisibility(View.GONE);
            holder.jitterTableRow.setVisibility(View.VISIBLE);
            holder.audioLevelTableRow.setVisibility(View.VISIBLE);
        } else {
            holder.dimensionsValueText.setText(item.dimensions);
            holder.framerateValueText.setText(String.valueOf(item.framerate));
            holder.dimensionsTableRow.setVisibility(View.VISIBLE);
            holder.framerateTableRow.setVisibility(View.VISIBLE);
            holder.jitterTableRow.setVisibility(View.GONE);
            holder.audioLevelTableRow.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return statsListItems.size();
    }

    public void updateStatsData(List<StatsReport> statsReports,
                                List<RemoteParticipant> remoteParticipants,
                                Map<String, String> localVideoTrackNames){
        statsListItems.clear();
        // Generate stats items list from reports
        boolean localTracksAdded = false;
        for (StatsReport report : statsReports) {
            if (!localTracksAdded) {
                // go trough local tracks
                for (LocalAudioTrackStats localAudioTrackStats : report.getLocalAudioTrackStats()) {
                    StatsListItem item = new StatsListItem.Builder()
                            .baseTrackInfo(localAudioTrackStats)
                            .bytes(localAudioTrackStats.bytesSent)
                            .rtt(localAudioTrackStats.roundTripTime)
                            .jitter(localAudioTrackStats.jitter)
                            .audioLevel(localAudioTrackStats.audioLevel)
                            .trackName(context.getString(R.string.local_audio_track))
                            .isAudioTrack(true)
                            .isLocalTrack(true)
                            .build();
                    statsListItems.add(item);
                }
                for (LocalVideoTrackStats localVideoTrackStats : report.getLocalVideoTrackStats()) {
                    String localVideoTrackName =
                            localVideoTrackNames.get(localVideoTrackStats.trackId);
                    if (localVideoTrackName == null) {
                        localVideoTrackName = context.getString(R.string.local_video_track);
                    }
                    StatsListItem item = new StatsListItem.Builder()
                            .baseTrackInfo(localVideoTrackStats)
                            .bytes(localVideoTrackStats.bytesSent)
                            .rtt(localVideoTrackStats.roundTripTime)
                            .dimensions(localVideoTrackStats.dimensions.toString())
                            .framerate(localVideoTrackStats.frameRate)
                            .trackName(localVideoTrackName)
                            .isAudioTrack(false)
                            .isLocalTrack(true)
                            .build();
                    statsListItems.add(item);
                }
                localTracksAdded = true;
            }
            int trackCount = 0;
            for (RemoteAudioTrackStats remoteAudioTrackStats : report.getRemoteAudioTrackStats()) {
                String trackName =
                        getParticipantName(remoteAudioTrackStats.trackId, true, remoteParticipants) +
                                " " + context.getString(R.string.audio_track) + " " + trackCount;
                StatsListItem item = new StatsListItem.Builder()
                        .baseTrackInfo(remoteAudioTrackStats)
                        .bytes(remoteAudioTrackStats.bytesReceived)
                        .jitter(remoteAudioTrackStats.jitter)
                        .audioLevel(remoteAudioTrackStats.audioLevel)
                        .trackName(trackName)
                        .isAudioTrack(true)
                        .isLocalTrack(false)
                        .build();
                statsListItems.add(item);
                trackCount++;
            }
            trackCount = 0;
            for (RemoteVideoTrackStats remoteVideoTrackStats : report.getRemoteVideoTrackStats()) {
                String trackName =
                        getParticipantName(remoteVideoTrackStats.trackId, false, remoteParticipants) +
                                " " + context.getString(R.string.video_track) + " " + trackCount;
                StatsListItem item = new StatsListItem.Builder()
                        .baseTrackInfo(remoteVideoTrackStats)
                        .bytes(remoteVideoTrackStats.bytesReceived)
                        .dimensions(remoteVideoTrackStats.dimensions.toString())
                        .framerate(remoteVideoTrackStats.frameRate)
                        .trackName(trackName)
                        .isAudioTrack(false)
                        .isLocalTrack(false)
                        .build();
                statsListItems.add(item);
                trackCount++;
            }
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    private String getParticipantName(String trackId, boolean isAudioTrack,
                                      List<RemoteParticipant> remoteParticipants) {
        for (RemoteParticipant remoteParticipant : remoteParticipants) {
            if (isAudioTrack) {
                RemoteAudioTrack remoteAudioTrack = getAudioTrack(remoteParticipant, trackId);
                if (remoteAudioTrack != null) {
                    return remoteParticipant.getIdentity();
                }
            } else {
                RemoteVideoTrack remoteVideoTrack = getRemoteVideoTrack(remoteParticipant, trackId);
                if (remoteVideoTrack != null) {
                    return remoteParticipant.getIdentity();
                }
            }
        }
        return "";
    }

    private RemoteAudioTrack getAudioTrack(RemoteParticipant remoteParticipant, String trackId) {
        for (RemoteAudioTrackPublication remoteAudioTrackPublication :
                remoteParticipant.getRemoteAudioTracks()) {
            String audioTrackId = getTrackId(remoteAudioTrackPublication.getRemoteAudioTrack());
            if (audioTrackId != null && audioTrackId.equals(trackId)) {
                return remoteAudioTrackPublication.getRemoteAudioTrack();
            }
        }

        return null;
    }

    private RemoteVideoTrack getRemoteVideoTrack(RemoteParticipant remoteParticipant, String trackId) {
        for (RemoteVideoTrackPublication remoteVideoTrackPublication :
                remoteParticipant.getRemoteVideoTracks()) {
            String videoTrackId = getTrackId(remoteVideoTrackPublication.getRemoteVideoTrack());
            if (videoTrackId != null && videoTrackId.equals(trackId)) {
                return remoteVideoTrackPublication.getRemoteVideoTrack();
            }
        }

        return null;
    }

    /*
     * TODO: Remove this reflection workaround when CSDK-1650 is resolved
     */
    private @Nullable String getTrackId(RemoteAudioTrack remoteAudioTrack) {
        String trackId = null;

        try {
            Field field = remoteAudioTrack.getClass().getDeclaredField("webRtcAudioTrack");
            field.setAccessible(true);
            org.webrtc.AudioTrack webRtcAudioTrack =
                    (org.webrtc.AudioTrack) field.get(remoteAudioTrack);
            trackId = webRtcAudioTrack.id();
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }

        return trackId;
    }

    /*
     * TODO: Remove this reflection workaround when CSDK-1650 is resolved
     */
    private @Nullable String getTrackId(RemoteVideoTrack remoteVideoTrack) {
        String trackId = null;

        try {
            Field field = remoteVideoTrack
                    .getClass()
                    .getSuperclass()
                    .getDeclaredField("webRtcVideoTrack");
            field.setAccessible(true);
            org.webrtc.VideoTrack webRtcVideoTrack =
                    (org.webrtc.VideoTrack) field.get(remoteVideoTrack);
            trackId = webRtcVideoTrack.id();
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }

        return trackId;
    }
}

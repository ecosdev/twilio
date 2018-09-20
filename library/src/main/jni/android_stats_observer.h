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

#ifndef VIDEO_ANDROID_ANDROID_STATS_OBSERVER_H_
#define VIDEO_ANDROID_ANDROID_STATS_OBSERVER_H_

#include "webrtc/sdk/android/src/jni/jni_helpers.h"

#include "video/stats_observer.h"
#include "video/stats_report.h"
#include "class_reference_holder.h"
#include "logging.h"
#include "jni_utils.h"

#include <memory>
#include <vector>

namespace twilio_video_jni {

class AndroidStatsObserver : public twilio::video::StatsObserver {
public:
    AndroidStatsObserver(JNIEnv *env, jobject j_stats_observer) :
            j_stats_observer_(env, j_stats_observer),
            j_stats_observer_class_(env, webrtc_jni::GetObjectClass(env, *j_stats_observer_)),
            j_array_list_class_(env, twilio_video_jni::FindClass(env, "java/util/ArrayList")),
            j_stats_report_class_(env,
                                  twilio_video_jni::FindClass(env, "com/twilio/video/StatsReport")),
            j_local_audio_track_stats_class_(env, twilio_video_jni::FindClass(env,
                                                                              "com/twilio/video/LocalAudioTrackStats")),
            j_local_video_track_stats_class_(env, twilio_video_jni::FindClass(env,
                                                                              "com/twilio/video/LocalVideoTrackStats")),
            j_remote_audio_track_stats_class_(env, twilio_video_jni::FindClass(env,
                                                                        "com/twilio/video/RemoteAudioTrackStats")),
            j_remote_video_track_stats_class_(env, twilio_video_jni::FindClass(env,
                                                                        "com/twilio/video/RemoteVideoTrackStats")),
            j_ice_candidate_pair_stats_class_(env, twilio_video_jni::FindClass(env,
                                                                         "com/twilio/video/IceCandidatePairStats")),
            j_ice_candidate_pair_state_class_(env, twilio_video_jni::FindClass(env,
                                                                         "com/twilio/video/IceCandidatePairState")),
            j_video_dimensions_class_(env, twilio_video_jni::FindClass(env,
                                                                       "com/twilio/video/VideoDimensions")),
            j_on_stats_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_stats_observer_class_,
                                            "onStats",
                                            "(Ljava/util/List;)V")),
            j_array_list_ctor_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_array_list_class_,
                                            "<init>",
                                            "()V")),
            j_array_list_add_(
                    webrtc_jni::GetMethodID(env,
                                            *j_array_list_class_,
                                            "add",
                                            "(Ljava/lang/Object;)Z")),
            j_stats_report_ctor_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_stats_report_class_,
                                            "<init>",
                                            "(Ljava/lang/String;)V")),
            j_stats_report_add_local_audio_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_stats_report_class_,
                                            "addLocalAudioTrackStats",
                                            "(Lcom/twilio/video/LocalAudioTrackStats;)V")),
            j_stats_report_add_local_video_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_stats_report_class_,
                                            "addLocalVideoTrackStats",
                                            "(Lcom/twilio/video/LocalVideoTrackStats;)V")),
            j_stats_report_add_audio_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_stats_report_class_,
                                            "addAudioTrackStats",
                                            "(Lcom/twilio/video/RemoteAudioTrackStats;)V")),
            j_stats_report_add_video_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_stats_report_class_,
                                            "addVideoTrackStats",
                                            "(Lcom/twilio/video/RemoteVideoTrackStats;)V")),
            j_stats_report_add_ice_candidate_pair_id(
                    webrtc_jni::GetMethodID(env,
                                            *j_stats_report_class_,
                                            "addIceCandidatePairStats",
                                            "(Lcom/twilio/video/IceCandidatePairStats;)V")
            ),
            j_local_audio_track_stats_ctor_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_local_audio_track_stats_class_,
                                            "<init>",
                                            "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;DJIJII)V")),
            j_local_video_track_stats_ctor_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_local_video_track_stats_class_,
                                            "<init>",
                                            "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;DJIJLcom/twilio/video/VideoDimensions;Lcom/twilio/video/VideoDimensions;II)V")),
            j_audio_track_stats_ctor_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_remote_audio_track_stats_class_,
                                            "<init>",
                                            "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;DJIII)V")),
            j_video_track_stats_ctor_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_remote_video_track_stats_class_,
                                            "<init>",
                                            "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;DJILcom/twilio/video/VideoDimensions;I)V")),

             j_video_dimensions_ctor_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_video_dimensions_class_,
                                            "<init>",
                                            "(II)V")),
            j_ice_candidate_pair_stats_ctor_id_(
                    webrtc_jni::GetMethodID(env,
                                            *j_ice_candidate_pair_stats_class_,
                                            "<init>",
                                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/twilio/video/IceCandidatePairState;Ljava/lang/String;Ljava/lang/String;JZZZJJDDDDJJJJJJJJJZLjava/lang/String;)V"))
    {
    }

    virtual ~AndroidStatsObserver() {
        VIDEO_ANDROID_LOG(twilio::video::LogModule::kPlatform,
                          twilio::video::LogLevel::kDebug,
                          "~AndroidStatsObserver");
    }

    void setObserverDeleted() {
        rtc::CritScope cs(&deletion_lock_);
        observer_deleted_ = true;
        VIDEO_ANDROID_LOG(twilio::video::LogModule::kPlatform,
                          twilio::video::LogLevel::kDebug,
                          "android stats observer deleted");
    }

protected:
    virtual void onStats(
            const std::vector<twilio::video::StatsReport> &stats_reports) {
        webrtc_jni::ScopedLocalRefFrame local_ref_frame(jni());
        std::string func_name = std::string(__FUNCTION__);
        VIDEO_ANDROID_LOG(twilio::video::LogModule::kPlatform,
                          twilio::video::LogLevel::kDebug,
                          "%s", func_name.c_str());

        {
            rtc::CritScope cs(&deletion_lock_);
            if (!isObserverValid(func_name)) {
                return;
            }
            // Create ArrayList<StatsReport>
            jobject j_stats_reports = jni()->NewObject(*j_array_list_class_, j_array_list_ctor_id_);
            for (auto const &stats_report : stats_reports) {
                webrtc_jni::ScopedLocalRefFrame stats_iteration_ref_frame(jni());
                jstring j_peerconnection_id = JavaUTF16StringFromStdString(jni(),
                                                                           stats_report.peer_connection_id);
                jobject j_stats_report = jni()->NewObject(*j_stats_report_class_,
                                                          j_stats_report_ctor_id_,
                                                          j_peerconnection_id);
                processLocalAudioTrackStats(j_stats_report,
                                            stats_report.local_audio_track_stats);
                processLocalVideoTrackStats(j_stats_report,
                                            stats_report.local_video_track_stats);
                processRemoteAudioTrackStats(j_stats_report, stats_report.remote_audio_track_stats);
                processRemoteVideoTrackStats(j_stats_report, stats_report.remote_video_track_stats);
                processIceCandidatePairStats(j_stats_report, stats_report.ice_candidate_pair_stats);

                jni()->CallBooleanMethod(j_stats_reports, j_array_list_add_, j_stats_report);
            }

            jni()->CallVoidMethod(*j_stats_observer_, j_on_stats_id_, j_stats_reports);
        }
    }

private:
    JNIEnv *jni() {
        return webrtc_jni::AttachCurrentThreadIfNeeded();
    }

    bool isObserverValid(const std::string &callbackName) {
        if (observer_deleted_) {
            VIDEO_ANDROID_LOG(twilio::video::LogModule::kPlatform,
                              twilio::video::LogLevel::kWarning,
                              "android stats observer is marked for deletion, skipping %s callback",
                              callbackName.c_str());
            return false;
        };
        if (webrtc_jni::IsNull(jni(), *j_stats_observer_)) {
            VIDEO_ANDROID_LOG(twilio::video::LogModule::kPlatform,
                              twilio::video::LogLevel::kWarning,
                              "android stats observer reference has been destroyed, skipping %s callback",
                              callbackName.c_str());
            return false;
        }
        return true;
    }

    void processLocalAudioTrackStats(jobject j_stats_report,
                                     const std::vector<twilio::media::LocalAudioTrackStats> &local_audio_tracks_stats) {
        for (auto const &track_stats : local_audio_tracks_stats) {
            webrtc_jni::ScopedLocalRefFrame local_ref_frame(jni());
            jstring j_track_sid =
                    JavaUTF16StringFromStdString(jni(), track_stats.track_sid);
            jstring j_codec =
                    JavaUTF16StringFromStdString(jni(), track_stats.codec);
            jstring j_ssrc =
                    JavaUTF16StringFromStdString(jni(), track_stats.ssrc);
            jobject j_local_audio_track_stats =
                    jni()->NewObject(*j_local_audio_track_stats_class_,
                                     j_local_audio_track_stats_ctor_id_,
                                     j_track_sid,
                                     track_stats.packets_lost,
                                     j_codec,
                                     j_ssrc,
                                     track_stats.timestamp,
                                     track_stats.bytes_sent,
                                     track_stats.packets_sent,
                                     track_stats.round_trip_time,
                                     track_stats.audio_level,
                                     track_stats.jitter);
            jni()->CallVoidMethod(j_stats_report,
                                  j_stats_report_add_local_audio_id_,
                                  j_local_audio_track_stats);
        }
    }

    void processLocalVideoTrackStats(jobject j_stats_report,
                                     const std::vector<twilio::media::LocalVideoTrackStats> &local_video_tracks_stats) {
        for (auto const &track_stats : local_video_tracks_stats) {
            webrtc_jni::ScopedLocalRefFrame local_ref_frame(jni());
            jstring j_track_sid =
                    JavaUTF16StringFromStdString(jni(), track_stats.track_sid);
            jstring j_codec =
                    JavaUTF16StringFromStdString(jni(), track_stats.codec);
            jstring j_ssrc =
                    JavaUTF16StringFromStdString(jni(), track_stats.ssrc);
            jobject j_capture_dimensions =
                    jni()->NewObject(*j_video_dimensions_class_,
                                     j_video_dimensions_ctor_id_,
                                     track_stats.capture_dimensions.width,
                                     track_stats.capture_dimensions.height);
            jobject j_sent_dimensions =
                    jni()->NewObject(*j_video_dimensions_class_,
                                     j_video_dimensions_ctor_id_,
                                     track_stats.dimensions.width,
                                     track_stats.dimensions.height);
            jobject j_local_video_track_stats =
                    jni()->NewObject(*j_local_video_track_stats_class_,
                                     j_local_video_track_stats_ctor_id_,
                                     j_track_sid,
                                     track_stats.packets_lost,
                                     j_codec,
                                     j_ssrc,
                                     track_stats.timestamp,
                                     track_stats.bytes_sent,
                                     track_stats.packets_sent,
                                     track_stats.round_trip_time,
                                     j_capture_dimensions,
                                     j_sent_dimensions,
                                     track_stats.capture_frame_rate,
                                     track_stats.frame_rate);
            jni()->CallVoidMethod(j_stats_report,
                                  j_stats_report_add_local_video_id_,
                                  j_local_video_track_stats);
        }
    }

    void processRemoteAudioTrackStats(jobject j_stats_report,
                                      const std::vector<twilio::media::RemoteAudioTrackStats> &audio_tracks_stats) {
        for (auto const &track_stats : audio_tracks_stats) {
            webrtc_jni::ScopedLocalRefFrame local_ref_frame(jni());
            jstring j_track_sid =
                    JavaUTF16StringFromStdString(jni(), track_stats.track_sid);
            jstring j_codec_name =
                    JavaUTF16StringFromStdString(jni(), track_stats.codec);
            jstring j_ssrc =
                    JavaUTF16StringFromStdString(jni(), track_stats.ssrc);
            jobject j_audio_track_stats =
                    jni()->NewObject(*j_remote_audio_track_stats_class_,
                                     j_audio_track_stats_ctor_id_,
                                     j_track_sid,
                                     track_stats.packets_lost,
                                     j_codec_name,
                                     j_ssrc,
                                     track_stats.timestamp,
                                     track_stats.bytes_received,
                                     track_stats.packets_received,
                                     track_stats.audio_level,
                                     track_stats.jitter);
            jni()->CallVoidMethod(j_stats_report,
                                  j_stats_report_add_audio_id_,
                                  j_audio_track_stats);
        }
    }

    void processRemoteVideoTrackStats(jobject j_stats_report,
                                      const std::vector<twilio::media::RemoteVideoTrackStats> &video_tracks_stats) {
        for (auto const &track_stats : video_tracks_stats) {
            webrtc_jni::ScopedLocalRefFrame local_ref_frame(jni());
            jstring j_track_sid =
                    JavaUTF16StringFromStdString(jni(), track_stats.track_sid);
            jstring j_codec_name =
                    JavaUTF16StringFromStdString(jni(), track_stats.codec);
            jstring j_ssrc =
                    JavaUTF16StringFromStdString(jni(), track_stats.ssrc);
            jobject j_received_dimensions =
                    jni()->NewObject(*j_video_dimensions_class_,
                                     j_video_dimensions_ctor_id_,
                                     track_stats.dimensions.width,
                                     track_stats.dimensions.height);
            jobject j_video_track_stats =
                    jni()->NewObject(*j_remote_video_track_stats_class_,
                                     j_video_track_stats_ctor_id_,
                                     j_track_sid,
                                     track_stats.packets_lost,
                                     j_codec_name,
                                     j_ssrc,
                                     track_stats.timestamp,
                                     track_stats.bytes_received,
                                     track_stats.packets_received,
                                     j_received_dimensions,
                                     track_stats.frame_rate);
            jni()->CallVoidMethod(j_stats_report,
                                  j_stats_report_add_video_id_,
                                  j_video_track_stats);
        }
    }

    void processIceCandidatePairStats(jobject j_stats_report,
                                     const std::vector<twilio::media::IceCandidatePairStats> &ice_candidate_pair_stats) {
        for (auto const &stats : ice_candidate_pair_stats) {
            webrtc_jni::ScopedLocalRefFrame local_ref_frame(jni());
            jstring j_transport_id =
                    JavaUTF16StringFromStdString(jni(), stats.transport_id);
            jstring j_local_candidate_id =
                    JavaUTF16StringFromStdString(jni(), stats.local_candidate_id);
            jstring j_remote_candidate_id =
                    JavaUTF16StringFromStdString(jni(), stats.remote_candidate_id);

            jobject state = NULL;
            jfieldID j_state_field = NULL;
            if (stats.state == twilio::media::kStateSucceeded) {
                j_state_field = jni()->GetStaticFieldID(*j_ice_candidate_pair_state_class_,
                                                        "STATE_SUCCEEDED",
                                                        "Lcom/twilio/video/IceCandidatePairState;");
            } else if (stats.state == twilio::media::kStateCancelled) {
                j_state_field = jni()->GetStaticFieldID(*j_ice_candidate_pair_state_class_,
                                                        "STATE_CANCELED",
                                                        "Lcom/twilio/video/IceCandidatePairState;");
            } else if (stats.state == twilio::media::kStateFailed) {
                j_state_field = jni()->GetStaticFieldID(*j_ice_candidate_pair_state_class_,
                                                        "STATE_FAILED",
                                                        "Lcom/twilio/video/IceCandidatePairState;");
            } else if (stats.state == twilio::media::kStateFrozen) {
                j_state_field = jni()->GetStaticFieldID(*j_ice_candidate_pair_state_class_,
                                                        "STATE_FROZEN",
                                                        "Lcom/twilio/video/IceCandidatePairState;");
            } else if (stats.state == twilio::media::kStateInProgress) {
                j_state_field = jni()->GetStaticFieldID(*j_ice_candidate_pair_state_class_,
                                                        "STATE_IN_PROGRESS",
                                                        "Lcom/twilio/video/IceCandidatePairState;");
            } else if (stats.state == twilio::media::kStateWaiting) {
                j_state_field = jni()->GetStaticFieldID(*j_ice_candidate_pair_state_class_,
                                                        "STATE_WAITING",
                                                        "Lcom/twilio/video/IceCandidatePairState;");
            } else{
                VIDEO_ANDROID_LOG(twilio::video::LogModule::kPlatform,
                                  twilio::video::LogLevel::kError,
                                  "invalid ice candidate pair state received");
                continue;
            }

            state = jni()->GetStaticObjectField(*j_ice_candidate_pair_state_class_,
                                                j_state_field);
            jstring localCandidateIp = JavaUTF16StringFromStdString(jni(), stats.local_candidate_ip);
            jstring remoteCandidateIp = JavaUTF16StringFromStdString(jni(), stats.remote_candidate_ip);
            jstring relayProtocol = JavaUTF16StringFromStdString(jni(), stats.relay_protocol);


            jobject j_ice_candidate_pair_stats = jni()->NewObject(*j_ice_candidate_pair_stats_class_, j_ice_candidate_pair_stats_ctor_id_,
                                                                  j_transport_id, j_local_candidate_id, j_remote_candidate_id,
                                                                  state, localCandidateIp, remoteCandidateIp,
                                                                  stats.priority, stats.nominated, stats.writable, stats.readable,
                                                                  stats.bytes_sent, stats.bytes_received, stats.total_round_trip_time,
                                                                  stats.current_round_trip_time, stats.available_outgoing_bitrate,
                                                                  stats.available_incoming_bitrate,
                                                                  stats.requests_received, stats.requests_sent, stats.responses_received,
                                                                  stats.retransmissions_received, stats.retransmissions_sent,
                                                                  stats.consent_requests_received, stats.consent_requests_sent,
                                                                  stats.consent_responses_received, stats.consent_responses_sent, stats.active_candidate_pair, relayProtocol);
            jni()->CallVoidMethod(j_stats_report, j_stats_report_add_ice_candidate_pair_id, j_ice_candidate_pair_stats);
        }
    }


    bool observer_deleted_ = false;
    mutable rtc::CriticalSection deletion_lock_;

    const webrtc_jni::ScopedGlobalRef<jobject> j_stats_observer_;
    const webrtc_jni::ScopedGlobalRef<jclass> j_stats_observer_class_;
    const webrtc_jni::ScopedGlobalRef<jclass> j_array_list_class_;
    const webrtc_jni::ScopedGlobalRef<jclass> j_stats_report_class_;
    const webrtc_jni::ScopedGlobalRef<jclass> j_local_audio_track_stats_class_;
    const webrtc_jni::ScopedGlobalRef<jclass> j_local_video_track_stats_class_;
    const webrtc_jni::ScopedGlobalRef<jclass> j_remote_audio_track_stats_class_;
    const webrtc_jni::ScopedGlobalRef<jclass> j_remote_video_track_stats_class_;
    const webrtc_jni::ScopedGlobalRef<jclass> j_ice_candidate_pair_stats_class_;
    const webrtc_jni::ScopedGlobalRef<jclass> j_ice_candidate_pair_state_class_;
    const webrtc_jni::ScopedGlobalRef<jclass> j_video_dimensions_class_;
    jmethodID j_on_stats_id_;
    jmethodID j_array_list_ctor_id_;
    jmethodID j_array_list_add_;
    jmethodID j_stats_report_ctor_id_;
    jmethodID j_stats_report_add_local_audio_id_;
    jmethodID j_stats_report_add_local_video_id_;
    jmethodID j_stats_report_add_audio_id_;
    jmethodID j_stats_report_add_video_id_;
    jmethodID j_stats_report_add_ice_candidate_pair_id;
    jmethodID j_local_audio_track_stats_ctor_id_;
    jmethodID j_local_video_track_stats_ctor_id_;
    jmethodID j_audio_track_stats_ctor_id_;
    jmethodID j_video_track_stats_ctor_id_;
    jmethodID j_video_dimensions_ctor_id_;
    jmethodID j_ice_candidate_pair_stats_ctor_id_;
};

}

#endif // VIDEO_ANDROID_ANDROID_STATS_OBSERVER_H_

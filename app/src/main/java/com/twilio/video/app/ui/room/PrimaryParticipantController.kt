/*
 * Copyright (C) 2020 Twilio, Inc.
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
package com.twilio.video.app.ui.room

import com.twilio.video.VideoTrack
import com.twilio.video.app.sdk.VideoTrackViewState

internal class PrimaryParticipantController(
    private val primaryView: ParticipantPrimaryView,
) {
    private var primaryItem: Item? = null

    fun renderAsPrimary(
        identity: String?,
        screenTrack: VideoTrackViewState?,
        videoTrack: VideoTrackViewState?,
        muted: Boolean,
        mirror: Boolean,
    ) {
        val old = primaryItem
        val newItem = Item(
            identity,
            screenTrack?.videoTrack ?: videoTrack?.videoTrack,
            muted,
            mirror,
        )
        primaryItem = newItem
        primaryView.setIdentity(newItem.identity)
        primaryView.showIdentityBadge(true)
        primaryView.setMuted(newItem.muted)
        primaryView.setMirror(newItem.mirror)
        val newVideoTrack = newItem.videoTrack

        // Only update sink for a new video track
        if (newVideoTrack != old?.videoTrack) {
            old?.let { removeSink(it.videoTrack, primaryView) }
            newVideoTrack?.let { if (it.isEnabled) it.addSink(primaryView.videoTextureView) }
        }

        newVideoTrack?.let {
            primaryView.setState(ParticipantView.State.VIDEO)
        } ?: primaryView.setState(ParticipantView.State.NO_VIDEO)
    }

    private fun removeSink(videoTrack: VideoTrack?, view: ParticipantView) {
        if (videoTrack == null || !videoTrack.sinks.contains(view.videoTextureView)) return
        videoTrack.removeSink(view.videoTextureView)
    }

    internal class Item(
        var identity: String?,
        var videoTrack: VideoTrack?,
        var muted: Boolean,
        var mirror: Boolean,
    )
}

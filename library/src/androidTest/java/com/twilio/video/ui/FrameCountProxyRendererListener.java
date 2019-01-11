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

package com.twilio.video.ui;

import android.support.annotation.NonNull;
import com.twilio.video.I420Frame;
import com.twilio.video.VideoRenderer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Renderer that counts frames before forwarding them to a VideoView
 */
public class FrameCountProxyRendererListener implements VideoRenderer {
    private final AtomicReference<CountDownLatch> frameArrived =
            new AtomicReference<>(new CountDownLatch(1));
    private VideoRenderer videoRenderer;

    public FrameCountProxyRendererListener(@NonNull VideoRenderer videoRenderer) {
        this.videoRenderer = videoRenderer;
    }

    @Override
    public void renderFrame(@NonNull I420Frame frame) {
        frameArrived.get().countDown();
        videoRenderer.renderFrame(frame);
    }

    public boolean waitForFrame(int timeoutMs) throws InterruptedException {
        frameArrived.set(new CountDownLatch(1));
        return frameArrived.get().await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    @NonNull
    public VideoRenderer getVideoRenderer() {
        return videoRenderer;
    }
}

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

package com.twilio.video;

import android.support.annotation.NonNull;

/** Abstract base class for video codecs. */
public abstract class VideoCodec {
    private final String name;

    protected VideoCodec(@NonNull String name) {
        this.name = name;
    }

    /** Returns the string representation of the video codec. */
    @NonNull
    public String getName() {
        return name;
    }

    /** Returns the name of the video codec. */
    @Override
    @NonNull
    public String toString() {
        return name;
    }
}

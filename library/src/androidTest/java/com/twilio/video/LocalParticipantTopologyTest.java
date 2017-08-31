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

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;

import com.twilio.video.base.BaseClientTest;
import com.twilio.video.helper.CallbackHelper;
import com.twilio.video.ui.MediaTestActivity;
import com.twilio.video.util.Constants;
import com.twilio.video.util.CredentialsUtils;
import com.twilio.video.util.FakeVideoCapturer;
import com.twilio.video.util.PermissionUtils;
import com.twilio.video.util.RandUtils;
import com.twilio.video.util.RoomUtils;
import com.twilio.video.util.Topology;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
@LargeTest
public class LocalParticipantTopologyTest extends BaseClientTest {
    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Topology.P2P},
                {Topology.GROUP}});
    }

    @Rule
    public ActivityTestRule<MediaTestActivity> activityRule =
            new ActivityTestRule<>(MediaTestActivity.class);
    private MediaTestActivity mediaTestActivity;
    private String identity;
    private String token;
    private String roomName;
    private final CallbackHelper.FakeRoomListener roomListener =
            new CallbackHelper.FakeRoomListener();
    private Room room;
    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    private Topology topology;

    public LocalParticipantTopologyTest(Topology topology) {
        this.topology = topology;
    }

    @Before
    public void setup() throws InterruptedException {
        super.setup();
        mediaTestActivity = activityRule.getActivity();
        PermissionUtils.allowPermissions(mediaTestActivity);
        identity = Constants.PARTICIPANT_ALICE;
        roomName = RandUtils.generateRandomString(20);
        assertNotNull(RoomUtils.createRoom(roomName, topology));
        token = CredentialsUtils.getAccessToken(identity, topology);
    }

    @After
    public void teardown() throws InterruptedException {
        if (room != null && room.getState() != RoomState.DISCONNECTED) {
            roomListener.onDisconnectedLatch = new CountDownLatch(1);
            room.disconnect();
            assertTrue(roomListener.onDisconnectedLatch.await(20, TimeUnit.SECONDS));
        }
        if (localAudioTrack != null) {
            localAudioTrack.release();
        }
        if (localVideoTrack != null) {
            localVideoTrack.release();
        }
        assertTrue(MediaFactory.isReleased());
    }

    @Test
    public void shouldAllowPublishingAndUnpublishingTracksWhileConnected() throws InterruptedException {
        CallbackHelper.FakeLocalParticipantListener localParticipantListener =
                new CallbackHelper.FakeLocalParticipantListener();
        FakeVideoCapturer fakeVideoCapturer = new FakeVideoCapturer();
        localParticipantListener.onPublishedAudioTrackLatch = new CountDownLatch(1);
        localParticipantListener.onPublishedVideoTrackLatch = new CountDownLatch(1);
        roomListener.onConnectedLatch = new CountDownLatch(1);
        roomListener.onDisconnectedLatch = new CountDownLatch(1);

        ConnectOptions connectOptions = new ConnectOptions.Builder(token)
                .roomName(roomName)
                .build();
        room = Video.connect(mediaTestActivity, connectOptions, roomListener);
        assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));
        LocalParticipant localParticipant = room.getLocalParticipant();
        assertNotNull(localParticipant);
        localParticipant.setListener(localParticipantListener);

        // Now we add our tracks
        localAudioTrack = LocalAudioTrack.create(mediaTestActivity, true);
        localVideoTrack = LocalVideoTrack.create(mediaTestActivity, true, fakeVideoCapturer);
        assertTrue(localParticipant.publishTrack(localAudioTrack));
        assertEquals(0, localParticipant.getAudioTracks().size());
        assertEquals(0, localParticipant.getLocalAudioTracks().size());
        assertTrue(localParticipant.publishTrack(localVideoTrack));
        assertEquals(0, localParticipant.getVideoTracks().size());
        assertEquals(0, localParticipant.getLocalVideoTracks().size());

        // Validate we received callbacks
        assertTrue(localParticipantListener.onPublishedAudioTrackLatch.await(20, TimeUnit.SECONDS));
        assertTrue(localParticipantListener.onPublishedVideoTrackLatch.await(20, TimeUnit.SECONDS));

        // Now remove them
        assertTrue(localParticipant.unpublishTrack(localAudioTrack));
        assertEquals(0, localParticipant.getAudioTracks().size());
        assertEquals(0, localParticipant.getLocalAudioTracks().size());
        assertTrue(localParticipant.unpublishTrack(localVideoTrack));
        assertEquals(0, localParticipant.getVideoTracks().size());
        assertEquals(0, localParticipant.getLocalVideoTracks().size());
    }

    @Test
    public void shouldHaveTracksWhenConnected() throws InterruptedException {
        CallbackHelper.FakeLocalParticipantListener localParticipantListener =
                new CallbackHelper.FakeLocalParticipantListener();
        FakeVideoCapturer fakeVideoCapturer = new FakeVideoCapturer();
        roomListener.onConnectedLatch = new CountDownLatch(1);
        localParticipantListener.onPublishedAudioTrackLatch = new CountDownLatch(1);
        localParticipantListener.onPublishedVideoTrackLatch = new CountDownLatch(1);

        localAudioTrack = LocalAudioTrack.create(mediaTestActivity, true);
        localVideoTrack = LocalVideoTrack.create(mediaTestActivity, true, fakeVideoCapturer);

        ConnectOptions connectOptions = new ConnectOptions.Builder(token)
                .audioTracks(Collections.singletonList(localAudioTrack))
                .videoTracks(Collections.singletonList(localVideoTrack))
                .roomName(roomName)
                .build();
        room = Video.connect(mediaTestActivity, connectOptions, roomListener);
        assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));
        LocalParticipant localParticipant = room.getLocalParticipant();
        assertNotNull(localParticipant);
        localParticipant.setListener(localParticipantListener);

        // Validate the state of tracks
        assertEquals(1, localParticipant.getAudioTracks().size());
        assertEquals(1, localParticipant.getLocalAudioTracks().size());
        assertEquals(1, localParticipant.getVideoTracks().size());
        assertEquals(1, localParticipant.getLocalVideoTracks().size());
        assertEquals(localAudioTrack, localParticipant.getAudioTracks().get(0).getAudioTrack());
        assertEquals(localVideoTrack, localParticipant.getVideoTracks().get(0).getVideoTrack());
    }

    @Test
    public void shouldHaveIdentityAndNonNullSidOnceConnected() throws InterruptedException {
        roomListener.onConnectedLatch = new CountDownLatch(1);
        roomListener.onDisconnectedLatch = new CountDownLatch(1);

        ConnectOptions connectOptions = new ConnectOptions.Builder(token)
                .roomName(roomName)
                .build();
        room = Video.connect(mediaTestActivity, connectOptions, roomListener);
        assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));
        LocalParticipant localParticipant = room.getLocalParticipant();
        assertNotNull(localParticipant);

        assertEquals(identity, localParticipant.getIdentity());
        assertNotNull(localParticipant.getSid());
    }

    @Test
    public void shouldHaveIdentityAndNonNullSidUponDisconnect() throws InterruptedException {
        roomListener.onConnectedLatch = new CountDownLatch(1);
        roomListener.onDisconnectedLatch = new CountDownLatch(1);

        ConnectOptions connectOptions = new ConnectOptions.Builder(token)
                .roomName(roomName)
                .build();
        room = Video.connect(mediaTestActivity, connectOptions, roomListener);
        assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));
        LocalParticipant localParticipant = room.getLocalParticipant();
        assertNotNull(localParticipant);

        room.disconnect();
        assertEquals(identity, localParticipant.getIdentity());
        assertNotNull(localParticipant.getSid());
        assertTrue(roomListener.onDisconnectedLatch.await(20, TimeUnit.SECONDS));
    }

    @Test
    public void shouldHaveIdentityAndNonNullSidOnceDisconnected() throws InterruptedException {
        roomListener.onConnectedLatch = new CountDownLatch(1);
        roomListener.onDisconnectedLatch = new CountDownLatch(1);

        ConnectOptions connectOptions = new ConnectOptions.Builder(token)
                .roomName(roomName)
                .build();
        room = Video.connect(mediaTestActivity, connectOptions, roomListener);
        assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));
        LocalParticipant localParticipant = room.getLocalParticipant();
        assertNotNull(localParticipant);

        room.disconnect();
        assertTrue(roomListener.onDisconnectedLatch.await(20, TimeUnit.SECONDS));
        assertEquals(identity, localParticipant.getIdentity());
        assertNotNull(localParticipant.getSid());
    }

    @Test
    public void shouldNotPublishAudioTrackAfterDisconnect() throws InterruptedException {
        roomListener.onConnectedLatch = new CountDownLatch(1);
        roomListener.onDisconnectedLatch = new CountDownLatch(1);

        ConnectOptions connectOptions = new ConnectOptions.Builder(token)
                .roomName(roomName)
                .build();
        room = Video.connect(mediaTestActivity, connectOptions, roomListener);
        assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));
        LocalParticipant localParticipant = room.getLocalParticipant();
        assertNotNull(localParticipant);
        int audioTrackSize = localParticipant.getLocalAudioTracks().size();
        room.disconnect();
        localAudioTrack = LocalAudioTrack.create(mediaTestActivity, true);
        assertFalse(localParticipant.publishTrack(localAudioTrack));
        assertEquals(audioTrackSize, localParticipant.getAudioTracks().size());
        assertEquals(audioTrackSize, localParticipant.getLocalAudioTracks().size());
        assertTrue(roomListener.onDisconnectedLatch.await(20, TimeUnit.SECONDS));
    }

    @Test
    public void shouldNotPublishVideoTrackAfterDisconnect() throws InterruptedException {
        roomListener.onConnectedLatch = new CountDownLatch(1);
        roomListener.onDisconnectedLatch = new CountDownLatch(1);

        ConnectOptions connectOptions = new ConnectOptions.Builder(token)
                .roomName(roomName)
                .build();
        room = Video.connect(mediaTestActivity, connectOptions, roomListener);
        assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));
        LocalParticipant localParticipant = room.getLocalParticipant();
        assertNotNull(localParticipant);
        int videoTrackSize = localParticipant.getLocalVideoTracks().size();
        room.disconnect();
        localVideoTrack = LocalVideoTrack.create(mediaTestActivity, true, new FakeVideoCapturer());
        assertFalse(localParticipant.publishTrack(localVideoTrack));
        assertEquals(videoTrackSize, localParticipant.getVideoTracks().size());
        assertEquals(videoTrackSize, localParticipant.getLocalVideoTracks().size());
        assertTrue(roomListener.onDisconnectedLatch.await(20, TimeUnit.SECONDS));
    }

    @Test
    public void shouldNotUnpublishAudioTrackAfterDisconnect() throws InterruptedException {
        CallbackHelper.FakeLocalParticipantListener localParticipantListener =
                new CallbackHelper.FakeLocalParticipantListener();
        localParticipantListener.onPublishedAudioTrackLatch = new CountDownLatch(1);
        roomListener.onConnectedLatch = new CountDownLatch(1);
        roomListener.onDisconnectedLatch = new CountDownLatch(1);

        ConnectOptions connectOptions = new ConnectOptions.Builder(token)
                .roomName(roomName)
                .build();
        room = Video.connect(mediaTestActivity, connectOptions, roomListener);
        assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));
        LocalParticipant localParticipant = room.getLocalParticipant();
        assertNotNull(localParticipant);
        localParticipant.setListener(localParticipantListener);
        localAudioTrack = LocalAudioTrack.create(mediaTestActivity, true);
        localParticipant.publishTrack(localAudioTrack);
        assertTrue(localParticipantListener.onPublishedAudioTrackLatch.await(20, TimeUnit.SECONDS));
        int audioTrackSize = localParticipant.getLocalAudioTracks().size();
        room.disconnect();
        assertFalse(localParticipant.unpublishTrack(localAudioTrack));
        assertEquals(audioTrackSize, localParticipant.getAudioTracks().size());
        assertEquals(audioTrackSize, localParticipant.getLocalAudioTracks().size());
        assertTrue(roomListener.onDisconnectedLatch.await(20, TimeUnit.SECONDS));
    }

    @Test
    public void shouldNotUnpublishVideoTrackAfterDisconnect() throws InterruptedException {
        CallbackHelper.FakeLocalParticipantListener localParticipantListener =
                new CallbackHelper.FakeLocalParticipantListener();
        localParticipantListener.onPublishedVideoTrackLatch = new CountDownLatch(1);
        roomListener.onConnectedLatch = new CountDownLatch(1);
        roomListener.onDisconnectedLatch = new CountDownLatch(1);

        ConnectOptions connectOptions = new ConnectOptions.Builder(token)
                .roomName(roomName)
                .build();
        room = Video.connect(mediaTestActivity, connectOptions, roomListener);
        assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));
        LocalParticipant localParticipant = room.getLocalParticipant();
        assertNotNull(localParticipant);
        localParticipant.setListener(localParticipantListener);
        FakeVideoCapturer fakeVideoCapturer = new FakeVideoCapturer();
        localVideoTrack = LocalVideoTrack.create(mediaTestActivity, true, fakeVideoCapturer);
        localParticipant.publishTrack(localVideoTrack);
        assertTrue(localParticipantListener.onPublishedVideoTrackLatch.await(20, TimeUnit.SECONDS));
        int videoTrackSize = localParticipant.getLocalVideoTracks().size();
        room.disconnect();
        assertFalse(localParticipant.unpublishTrack(localVideoTrack));
        assertEquals(videoTrackSize, localParticipant.getVideoTracks().size());
        assertEquals(videoTrackSize, localParticipant.getLocalVideoTracks().size());
        assertTrue(roomListener.onDisconnectedLatch.await(20, TimeUnit.SECONDS));
    }
}

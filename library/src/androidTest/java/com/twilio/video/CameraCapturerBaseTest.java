package com.twilio.video;

import android.hardware.Camera;
import android.support.test.runner.AndroidJUnit4;

import com.twilio.video.base.BaseCameraCapturerTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

@RunWith(AndroidJUnit4.class)
public class CameraCapturerBaseTest extends BaseCameraCapturerTest {
    @Test(expected = NullPointerException.class)
    public void create_shouldFailWithNullContext() {
        cameraCapturer = new CameraCapturer(null,
                CameraCapturer.CameraSource.FRONT_CAMERA,
                null);
    }

    @Test(expected = NullPointerException.class)
    public void create_shouldFailWithNullSource() {
        cameraCapturer = new CameraCapturer(cameraCapturerActivity, null, null);
    }

    @Test
    public void shouldAllowCameraSwitch() throws InterruptedException {
        cameraCapturer = new CameraCapturer(cameraCapturerActivity,
                CameraCapturer.CameraSource.FRONT_CAMERA,
                null);
        localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);
        int frameCount = frameCountRenderer.getFrameCount();

        // Validate our frame count is nothing
        assertEquals(0, frameCount);

        // Add renderer and wait
        localVideoTrack.addRenderer(frameCountRenderer);
        Thread.sleep(TimeUnit.SECONDS.toMillis(CAMERA_CAPTURE_DELAY));

        // Validate our frame count is incrementing
        assertTrue(frameCountRenderer.getFrameCount() > frameCount);

        // Validate front camera source
        assertEquals(CameraCapturer.CameraSource.FRONT_CAMERA,
                cameraCapturer.getCameraSource());

        // Perform camera switch
        cameraCapturer.switchCamera();

        // Wait and validate our frame count is still incrementing
        frameCount = frameCountRenderer.getFrameCount();
        Thread.sleep(TimeUnit.SECONDS.toMillis(CAMERA_CAPTURE_DELAY));
        assertTrue(frameCountRenderer.getFrameCount() > frameCount);

        // Validate back camera source
        assertEquals(CameraCapturer.CameraSource.BACK_CAMERA,
                cameraCapturer.getCameraSource());
    }

    @Test
    public void shouldAllowCameraSwitchWhileNotOnLocalVideo() throws InterruptedException {
        cameraCapturer = new CameraCapturer(cameraCapturerActivity,
                CameraCapturer.CameraSource.FRONT_CAMERA,
                null);

        // Switch our camera
        cameraCapturer.switchCamera();

        // Now add our video track
        localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);
        int frameCount = frameCountRenderer.getFrameCount();

        // Validate our frame count is nothing
        assertEquals(0, frameCount);

        // Add renderer and wait
        localVideoTrack.addRenderer(frameCountRenderer);
        Thread.sleep(TimeUnit.SECONDS.toMillis(CAMERA_CAPTURE_DELAY));

        // Validate our frame count is incrementing
        assertTrue(frameCountRenderer.getFrameCount() > frameCount);

        // Validate we are on back camera source
        assertEquals(CameraCapturer.CameraSource.BACK_CAMERA,
                cameraCapturer.getCameraSource());
    }

    @Test
    public void shouldAllowUpdatingCameraParametersBeforeCapturing() throws InterruptedException {
        CountDownLatch cameraParametersUpdated = new CountDownLatch(1);
        String expectedFlashMode = Camera.Parameters.FLASH_MODE_TORCH;
        AtomicReference<Camera.Parameters> actualCameraParameters = new AtomicReference<>();
        cameraCapturer = new CameraCapturer(cameraCapturerActivity,
                CameraCapturer.CameraSource.BACK_CAMERA,
                null);

        // Set our camera parameters
        scheduleCameraParameterUpdate(cameraParametersUpdated, expectedFlashMode,
                actualCameraParameters);

        // Now add our video track
        localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);

        // Wait for parameters to be set
        assertTrue(cameraParametersUpdated.await(10, TimeUnit.SECONDS));

        // Validate our flash mode
        assertEquals(expectedFlashMode, actualCameraParameters.get().getFlashMode());
    }

    @Test
    public void shouldAllowUpdatingCameraParametersWhileCapturing() throws InterruptedException {
        CountDownLatch cameraParametersUpdated = new CountDownLatch(1);
        String expectedFlashMode = Camera.Parameters.FLASH_MODE_TORCH;
        AtomicReference<Camera.Parameters> actualCameraParameters = new AtomicReference<>();
        cameraCapturer = new CameraCapturer(cameraCapturerActivity,
                CameraCapturer.CameraSource.BACK_CAMERA,
                null);

        // Begin capturing
        localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);

        // Schedule camera parameter update
        scheduleCameraParameterUpdate(cameraParametersUpdated, expectedFlashMode,
                actualCameraParameters);

        // Wait for parameters to be set
        assertTrue(cameraParametersUpdated.await(10, TimeUnit.SECONDS));

        // Validate our flash mode
        assertEquals(expectedFlashMode, actualCameraParameters.get().getFlashMode());
    }

    @Test
    public void updateCameraParameters_shouldManifestAfterCaptureCycle()
            throws InterruptedException {
        CountDownLatch cameraParametersUpdated = new CountDownLatch(1);
        String expectedFlashMode = Camera.Parameters.FLASH_MODE_TORCH;
        AtomicReference<Camera.Parameters> actualCameraParameters = new AtomicReference<>();
        cameraCapturer = new CameraCapturer(cameraCapturerActivity,
                CameraCapturer.CameraSource.BACK_CAMERA,
                null);

        // Begin capturing and validate our flash mode is set
        localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);
        scheduleCameraParameterUpdate(cameraParametersUpdated, expectedFlashMode,
                actualCameraParameters);

        // Wait for parameters to be set
        assertTrue(cameraParametersUpdated.await(10, TimeUnit.SECONDS));

        // Validate our flash mode
        assertEquals(expectedFlashMode, actualCameraParameters.get().getFlashMode());

        // Remove the video track
        localMedia.removeVideoTrack(localVideoTrack);

        // Set our flash mode to something else
        cameraParametersUpdated = new CountDownLatch(1);
        expectedFlashMode = Camera.Parameters.FLASH_MODE_ON;
        scheduleCameraParameterUpdate(cameraParametersUpdated, expectedFlashMode, actualCameraParameters);

        // Re add the track
        localVideoTrack = localMedia.addVideoTrack(true, cameraCapturer);

        // Wait for parameters to be set
        assertTrue(cameraParametersUpdated.await(10, TimeUnit.SECONDS));

        // Validate our flash mode is actually different
        assertEquals(expectedFlashMode, actualCameraParameters.get().getFlashMode());
    }

    private void scheduleCameraParameterUpdate(final CountDownLatch cameraParametersUpdated,
                                               final String expectedFlashMode,
                                               final AtomicReference<Camera.Parameters> actualCameraParameters) {
        cameraCapturer.updateCameraParameters(new CameraParameterUpdater() {
            @Override
            public void applyCameraParameterUpdates(Camera.Parameters cameraParameters) {
                // This lets assume we can actually support flash mode
                assumeNotNull(cameraParameters.getFlashMode());

                // Turn the flash on set our parameters later for validation
                cameraParameters.setFlashMode(expectedFlashMode);
                actualCameraParameters.set(cameraParameters);

                // Continue test
                cameraParametersUpdated.countDown();
            }
        });
    }
}

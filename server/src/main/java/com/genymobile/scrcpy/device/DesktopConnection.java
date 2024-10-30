package com.genymobile.scrcpy.device;

import com.genymobile.scrcpy.control.ControlChannel;
import com.genymobile.scrcpy.util.IO;
import com.genymobile.scrcpy.util.StringUtils;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class DesktopConnection implements Closeable {

    private static final int DEVICE_NAME_FIELD_LENGTH = 64;

    private final Socket videoSocket;
    private final OutputStream videoOsm;

    private final Socket audioSocket;
    private final OutputStream audioOsm;

    private final Socket controlSocket;
    private final ControlChannel controlChannel;

    private DesktopConnection(Socket videoSocket, Socket audioSocket, Socket controlSocket) throws IOException {
        this.videoSocket = videoSocket;
        this.audioSocket = audioSocket;
        this.controlSocket = controlSocket;

        videoOsm = videoSocket != null ? videoSocket.getOutputStream() : null;
        audioOsm = audioSocket != null ? audioSocket.getOutputStream() : null;
        controlChannel = controlSocket != null ? new ControlChannel(controlSocket) : null;
    }

    private static Socket connect(String ipAddress, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(ipAddress, port));
        return socket;
    }

    public static DesktopConnection open(String ipAddress, int port,
            boolean video, boolean audio, boolean control, boolean sendDummyByte)
            throws IOException {

        Socket videoSocket = null;
        Socket audioSocket = null;
        Socket controlSocket = null;
        try {
            if (video) {
                videoSocket = connect(ipAddress, port);
                if (sendDummyByte) {
                    // send one byte so the client may read() to detect a connection error
                    videoSocket.getOutputStream().write(0);
                    sendDummyByte = false;
                }
            }
            if (audio) {
                audioSocket = connect(ipAddress, port);
                if (sendDummyByte) {
                    // send one byte so the client may read() to detect a connection error
                    audioSocket.getOutputStream().write(0);
                    sendDummyByte = false;
                }
            }
            if (control) {
                controlSocket = connect(ipAddress, port);
                if (sendDummyByte) {
                    // send one byte so the client may read() to detect a connection error
                    controlSocket.getOutputStream().write(0);
                    sendDummyByte = false;
                }
            }
        } catch (IOException | RuntimeException e) {
            if (videoSocket != null) {
                videoSocket.close();
            }
            if (audioSocket != null) {
                audioSocket.close();
            }
            if (controlSocket != null) {
                controlSocket.close();
            }
            throw e;
        }

        return new DesktopConnection(videoSocket, audioSocket, controlSocket);
    }

    private Socket getFirstSocket() {
        if (videoSocket != null) {
            return videoSocket;
        }
        if (audioSocket != null) {
            return audioSocket;
        }
        return controlSocket;
    }

    public void shutdown() throws IOException {
        if (videoSocket != null) {
            videoSocket.shutdownInput();
            videoSocket.shutdownOutput();
        }
        if (audioSocket != null) {
            audioSocket.shutdownInput();
            audioSocket.shutdownOutput();
        }
        if (controlSocket != null) {
            controlSocket.shutdownInput();
            controlSocket.shutdownOutput();
        }
    }

    public void close() throws IOException {
        if (videoSocket != null) {
            videoSocket.close();
        }
        if (audioSocket != null) {
            audioSocket.close();
        }
        if (controlSocket != null) {
            controlSocket.close();
        }
    }

    public void sendDeviceMeta(String deviceName) throws IOException {
        byte[] buffer = new byte[DEVICE_NAME_FIELD_LENGTH];

        byte[] deviceNameBytes = deviceName.getBytes(StandardCharsets.UTF_8);
        int len = StringUtils.getUtf8TruncationIndex(deviceNameBytes, DEVICE_NAME_FIELD_LENGTH - 1);
        System.arraycopy(deviceNameBytes, 0, buffer, 0, len);
        // byte[] are always 0-initialized in java, no need to set '\0' explicitly

        OutputStream osm = getFirstSocket().getOutputStream();
        IO.writeFully(osm, buffer, 0, buffer.length);
    }

    public OutputStream getVideoOsm() {
        return videoOsm;
    }

    public OutputStream getAudioOsm() {
        return audioOsm;
    }

    public ControlChannel getControlChannel() {
        return controlChannel;
    }
}

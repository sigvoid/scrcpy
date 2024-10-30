package com.genymobile.scrcpy.device;

import com.genymobile.scrcpy.control.ControlChannel;
import com.genymobile.scrcpy.util.IO;
import com.genymobile.scrcpy.util.StringUtils;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;

public final class DesktopConnection implements Closeable {

    private static final int DEVICE_NAME_FIELD_LENGTH = 64;

    private final Socket videoSocket;
    private final FileDescriptor videoFd;

    private final Socket audioSocket;
    private final FileDescriptor audioFd;

    private final Socket controlSocket;
    private final ControlChannel controlChannel;

    public static FileDescriptor getFileDescriptorFromSocket(Socket socket) {
        try {
            // 使用反射访问 SocketImpl 对象
            Field socketImplField = Socket.class.getDeclaredField("impl");
            socketImplField.setAccessible(true);
            Object socketImpl = socketImplField.get(socket);

            // 获取 SocketImpl 中的 FileDescriptor
            Field fileDescriptorField = socketImpl.getClass().getDeclaredField("fd");
            fileDescriptorField.setAccessible(true);
            return (FileDescriptor) fileDescriptorField.get(socketImpl);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private DesktopConnection(Socket videoSocket, Socket audioSocket, Socket controlSocket) throws IOException {
        this.videoSocket = videoSocket;
        this.audioSocket = audioSocket;
        this.controlSocket = controlSocket;

        videoFd = videoSocket != null ? getFileDescriptorFromSocket(videoSocket) : null;
        audioFd = audioSocket != null ? getFileDescriptorFromSocket(audioSocket) : null;
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

        FileDescriptor fd = getFileDescriptorFromSocket(getFirstSocket());
        IO.writeFully(fd, buffer, 0, buffer.length);
    }

    public FileDescriptor getVideoFd() {
        return videoFd;
    }

    public FileDescriptor getAudioFd() {
        return audioFd;
    }

    public ControlChannel getControlChannel() {
        return controlChannel;
    }
}

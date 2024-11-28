package com.genymobile.scrcpy.util;

import android.system.ErrnoException;
import android.system.OsConstants;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Scanner;

public final class IO {
    private IO() {
        // not instantiable
    }

    public static void writeFully(OutputStream osm, byte[] buffer, int offset, int len) throws IOException {
        try {
            osm.write(buffer, offset, len);
            osm.flush();
        } catch (IOException e) {
            Ln.e("writeFully", e);
        }
    }

    public static void writeFully(OutputStream osm, ByteBuffer from) throws IOException {
        WritableByteChannel channel = Channels.newChannel(osm);
        try {
            while (from.hasRemaining()) {
                channel.write(from);
            }
        } catch (IOException e) {
            Ln.e("writeFully", e);
        }
    }

    public static String toString(InputStream inputStream) {
        StringBuilder builder = new StringBuilder();
        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNextLine()) {
            builder.append(scanner.nextLine()).append('\n');
        }
        return builder.toString();
    }

    public static boolean isBrokenPipe(IOException e) {
        Throwable cause = e.getCause();
        return cause instanceof ErrnoException && ((ErrnoException) cause).errno == OsConstants.EPIPE;
    }
}

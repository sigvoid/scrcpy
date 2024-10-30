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
        int retries = 5;
        while (true) {
            try {
                osm.write(buffer, offset, len);
                osm.flush();
                break;
            } catch (IOException e) {
                if (--retries == 0) {
                    throw e;
                }
                try {
                    Thread.sleep(1); // 捕获 InterruptedException
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // 重新设置中断状态
                    throw new IOException("Thread was interrupted", ie);
                }
            }
        }
    }

    public static void writeFully(OutputStream osm, ByteBuffer from) throws IOException {
        WritableByteChannel channel = Channels.newChannel(osm);
        int retries = 5;
    
        while (true) {
            try {
                while (from.hasRemaining()) {
                    channel.write(from);
                }
                break;
            } catch (IOException e) {
                if (--retries == 0) {
                    throw e; 
                }
                try {
                    Thread.sleep(1); // 捕获 InterruptedException
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // 重新设置中断状态
                    throw new IOException("Thread was interrupted", ie);
                }
            }
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

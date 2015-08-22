package com.derpfish.pinkielive.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    /**
     * Copies the bytes from inputStream to outputStream
     *
     * @throws IOException
     */
    public static void copyStream(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[4096];
        int readBytes;
        while ((readBytes = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, readBytes);
        }
    }

    /**
     * Copies the bytes from inputStream to stream, closing both streams when the end of inputStream is reached.
     *
     * @throws IOException
     */
    public static void copyStreamAndClose(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        copyStream(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
    }
}

package it.gov.pagopa.node.cfgsync.util;

import it.gov.pagopa.node.cfgsync.exception.SyncDbStatusException;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.Column;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class Utils {

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] zipContent(byte[] input) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(input);
            // flush the remaining compressed data to the buffer without closing the underlying stream,
            // so that the byte array is complete before reading it
            gzip.finish();
            return bos.toByteArray();
        }
    }

    public static Object trimValueColumn(Class<?> clazz, String columnName, String value) throws SyncDbStatusException {
        try {
            log.debug("[NODE-CFG-SYNC] Verify if trim value column '{}' is necessary", columnName);
            int maxColumnLength = clazz.getDeclaredField(columnName).getAnnotation(Column.class).length();
            int valueLength = value.length();
            int lastIndexTrim = Math.min(valueLength, maxColumnLength);
            return value.substring(0, lastIndexTrim);
        } catch (NoSuchFieldException e) {
            throw new SyncDbStatusException(e.getMessage());
        }
    }

}

package it.gov.pagopa.node.cfgsync.util;

import it.gov.pagopa.node.cfgsync.exception.SyncDbStatusException;

import javax.persistence.Column;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class Utils {

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] zipContent(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(input);
        gzip.close();
        byte[] compressed = bos.toByteArray();
        bos.close();
        return compressed;
    }

    public static Object trimValueColumn(Class clazz, String columnName, String value) throws SyncDbStatusException {
        try {
            int maxColumnLength = clazz.getDeclaredField(columnName).getAnnotation(Column.class).length();
            int valueLength = value.length();
            int lastIndexTrim = Math.min(valueLength, maxColumnLength);
            return value.substring(0, lastIndexTrim);
        } catch (NoSuchFieldException e) {
            throw new SyncDbStatusException(e.getMessage());
        }
    }

}

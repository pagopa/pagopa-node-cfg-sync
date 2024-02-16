package it.gov.pagopa.node.cfgsync.util;

import javax.persistence.Column;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

public class Utils {

    public static byte[] zipContent(byte[] input) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(input);
        gzip.close();
        byte[] compressed = bos.toByteArray();
        bos.close();
        return compressed;
    }

    public static Object trimValueColumn(Class clazz, String columnName, String value) {
        try {
            int length = clazz.getDeclaredField(columnName).getAnnotation(Column.class).length();
            return value.substring(0, length);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

}

package it.gov.pagopa.node.cfgsync.util;

import javax.persistence.Column;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

public class Utils {

    public static byte[] zipContent(byte[] input) throws IOException {
        Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);
        compressor.setInput(input);
        compressor.finish();

        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

        byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }
        bos.close();
        return bos.toByteArray();
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

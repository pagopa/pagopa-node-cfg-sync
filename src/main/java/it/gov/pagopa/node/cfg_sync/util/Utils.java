package it.gov.pagopa.node.cfg_sync.util;

import javax.persistence.Column;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;

public class Utils {

    public static byte[] zipContent(byte[] datas) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(out);
        dos.write(datas);
        dos.close();
        return out.toByteArray();
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

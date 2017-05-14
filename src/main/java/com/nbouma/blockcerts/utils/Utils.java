package com.nbouma.blockcerts.utils;




import android.util.Log;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;


/**
 * Created by noah on 05/04/17.
 */

public class Utils {

    public static byte[] combineBytes(byte[] bytes1, byte[] bytes2) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes1.length + bytes2.length);
        byteBuffer.put(bytes1);
        byteBuffer.put(bytes2);
        return byteBuffer.array();
    }

    public static byte[] hexStringToBytes(String hexString) {
        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return bytes;
    }

    /*
    * Converts ISO8601 time format into Unix epoch time format
    */
    public static long ISO8601ToTime(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        try {
            return dateFormat.parse(dateString).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

}











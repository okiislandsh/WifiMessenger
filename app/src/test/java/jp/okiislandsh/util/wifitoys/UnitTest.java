package jp.okiislandsh.util.wifitoys;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class UnitTest extends AbsTest {

    @Test
    public void bitsetTest(){
        final @NonNull BitSet bitSet = new BitSet(32);
        bitSet.set(0, true);
        bitSet.set(2, true);
        bitSet.set(3, true);
        bitSet.set(4, true);
        bitSet.set(6, true);
        bitSet.set(31, true);

        Log.d("Long.toHexString(bitSet.size()="+Long.toHexString(bitSet.size()));
        Log.d("Long.toHexString(bitSet.toLongArray()[0])="+Long.toHexString(bitSet.toLongArray()[0]));
        Log.d("Long.toHexString(bitSet.toBinaryString()[0])="+Long.toBinaryString(bitSet.toLongArray()[0]));

        for(byte b: bitSet.toByteArray()){
            Log.d(Integer.toBinaryString(0xff & b));
        }
    }

    @Test
    public void bitsetTest2(){
        final @NonNull BitSet bitSet = BitSet.valueOf(new long[]{0x80000000L});

        Log.d("Long.toHexString(bitSet.size()="+Long.toHexString(bitSet.size()));
        Log.d("Long.toHexString(bitSet.toLongArray()[0])="+Long.toHexString(bitSet.toLongArray()[0]));
        Log.d("Long.toHexString(bitSet.toBinaryString()[0])="+Long.toBinaryString(bitSet.toLongArray()[0]));

        for(byte b: bitSet.toByteArray()){
            Log.d(Integer.toBinaryString(0xff & b));
        }
    }

    @Test
    public void byteBufTest(){
        final @NonNull ByteBuffer buf = ByteBuffer.allocate(10);
        buf.put((byte)1);
        buf.put((byte)2);
        buf.put((byte)3);
        Log.d(Arrays.toString(buf.array()));
        Log.d(Arrays.toString((byte[])buf.flip().array()));
        final @NonNull byte[] dst = new byte[buf.limit()];
        buf.get(dst);
        Log.d(Arrays.toString(dst));
    }

    @Test
    public void test(){
        final @NonNull String senderName = "hoge-<0123456789abcdef>";
        final @NonNull Pattern p = Pattern.compile("-<([0-9a-fA-F)]{16})>\\z");
        final @NonNull Matcher m = p.matcher(senderName);
        if(m.find()) {
            Log.d("match!\t"+m.groupCount()+"\t" + m.group(1));
        }else{
            Log.d("No found...\t"+senderName);
        }
    }

    @Test
    public void test2(){
        Log.d(String.valueOf(setCommandOpt(7,2, true)));
        Log.d(String.valueOf(setCommandOpt(7,2, false)));
        Log.d(String.valueOf(setCommandOpt(0,2, true)));
        Log.d(String.valueOf(setCommandOpt(0,2, false)));
    }

    /** @return this */
    public int setCommandOpt(int a, int b, boolean flg){
        if(flg){
            //a ^ 0xFF XOR
            return a & (~b); //~a NOT
        }else{
            return  a | b;
        }
    }

    @Test
    public void test3(){
        byte[] b = new byte[]{(byte)0xFF, (byte)0xFF};
        Log.d(Integer.toString(((b[0]&0xff)<<8) | (b[1]&0xff), 16));
    }

}
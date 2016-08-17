package invalid.showme.util;


import android.content.Context;
import android.util.Log;

import org.acra.ACRA;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil
{
    private final static String TAG = "CryptoUtil";

    public static byte SIGNATURE_REGISTRY_CERTIFICATE = 1;

    public static File EncryptBytesToFile(Context context, byte[] data, byte[] key, byte[] iv) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

        File ciphertextFile = null;
        try {
            ciphertextFile = EncryptStreamToFile(context, inputStream, key, iv);
        }
        catch(IOException e) {
            Log.e(TAG, "Caught IOException in EncryptBytesToFile, calling EncryptStreamToFile");
            ACRA.getErrorReporter().handleException(e);
        }
        return ciphertextFile;
    }
    public static File EncryptFileToFile(Context context, File plaintextFile, byte[] key, byte[] iv) throws IOException {
        if(!plaintextFile.exists() || !plaintextFile.canRead()) {
            Log.e(TAG, "In EncryptFileToFile given plaintextFile could not find or read");
            throw new FileNotFoundException("Could not find " + plaintextFile.getAbsolutePath());
        }
        FileInputStream plaintextStream = new FileInputStream(plaintextFile);

        File ciphertextFile = EncryptStreamToFile(context, plaintextStream, key, iv);
        //noinspection ResultOfMethodCallIgnored
        plaintextFile.delete();
        return ciphertextFile;
    }


    private static File EncryptStreamToFile(Context context, InputStream plaintextStream, byte[] key, byte[] iv) throws IOException {
        File ciphertextFile = PhotoFileManager.createPermanentImageFile(context);
        FileOutputStream outputStream = new FileOutputStream(ciphertextFile);

        CipherOutputStream ciphertextStream = wrapinCipherOutputStream(outputStream, key, iv);
        IOUtil.streamStreams(plaintextStream, ciphertextStream);
        plaintextStream.close();
        ciphertextStream.close();

        return ciphertextFile;
    }


    public static CipherOutputStream wrapinCipherOutputStream(OutputStream outStream, byte[] key, byte[] iv)
    {
        CipherOutputStream ciphertextStream = null;
        try {
            Cipher encrypter = Cipher.getInstance("AES/GCM/NoPadding");

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec1 = new GCMParameterSpec(128, iv);
            encrypter.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec1);
            ciphertextStream = new CipherOutputStream(outStream, encrypter);

        } catch (InvalidKeyException|InvalidAlgorithmParameterException|NoSuchAlgorithmException|NoSuchPaddingException e) {
            Log.e(TAG, "In EncryptStreamToFile caught " + e.getClass().getName());
            ACRA.getErrorReporter().handleException(e);
        }

        return ciphertextStream;
    }
}

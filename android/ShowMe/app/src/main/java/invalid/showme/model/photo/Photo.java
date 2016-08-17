package invalid.showme.model.photo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import org.acra.ACRA;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import invalid.showme.util.BitmapUtils;

public abstract class Photo implements Serializable
{
    final private static String TAG = "Photo";

    long id;
    public byte[] key;
    byte[] photoIv;
    public byte[] thumbnailIv;

    //ciphertext files.
    File photoFile;
    File thumbnailFile;

    public String Message;
    public Boolean PrivatePhoto;

    private transient Bitmap decryptedPhoto = null;

    transient Bitmap realThumbnail = null;
    private transient Bitmap blurredThumbnail = null;
    private transient Boolean thumbnailIsFailing = false;

    public Boolean realThumbnailLoaded(Context context)
    {
        if(this.realThumbnail == null && !thumbnailIsFailing)
            return false;
        else if(this.realThumbnail == null && thumbnailIsFailing)
            return true;
        else //if(this.realThumbnail != null)
            return true;
    }
    public Bitmap getRealThumbnail(Context context)
    {
        if(this.realThumbnail == null && !thumbnailIsFailing) {
            this.LoadThumbnail(context);
            if(this.realThumbnail == null) {
                thumbnailIsFailing = true;
                return BitmapUtils.getBrokenThumbnailResource(context);
            }
        } else if(thumbnailIsFailing)
            return BitmapUtils.getBrokenThumbnailResource(context);
        return this.realThumbnail;
    }
    public Boolean privateThumbnailLoaded(Context context)
    {
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_privacypleaseicons", true))
            return true;
        if(this.blurredThumbnail == null && !thumbnailIsFailing)
            return false;
        else if(this.blurredThumbnail == null && thumbnailIsFailing)
            return true;
        else //if(this.blurredThumbnail != null)
            return true;
    }
    public Bitmap getPrivateThumbnail(Context context)
    {
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_privacypleaseicons", true))
            return BitmapUtils.getPrivacyPleaseResource(context);
        if(this.blurredThumbnail == null && !thumbnailIsFailing) {
            if(this.realThumbnail == null)
                this.LoadThumbnail(context);
            if(this.realThumbnail == null) {
                thumbnailIsFailing = true;
                return BitmapUtils.getBrokenThumbnailResource(context);
            }
            RenderScript rsScript = RenderScript.create(context);
            Allocation alloc = Allocation.createFromBitmap(rsScript, this.realThumbnail);

            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, alloc.getElement());
            blur.setRadius(25);
            blur.setInput(alloc);

            Bitmap temporaryBlurredThumbnail = Bitmap.createBitmap (this.realThumbnail.getWidth(), this.realThumbnail.getHeight(), this.realThumbnail.getConfig());
            Allocation outAlloc = Allocation.createFromBitmap(rsScript, temporaryBlurredThumbnail);
            blur.forEach(outAlloc);
            outAlloc.copyTo(temporaryBlurredThumbnail);

            rsScript.destroy();

            this.blurredThumbnail = temporaryBlurredThumbnail;
        } else if(thumbnailIsFailing)
            return BitmapUtils.getBrokenThumbnailResource(context);
        return this.blurredThumbnail;
    }

    Photo()
    {
        this.id = -1;
        this.photoFile = null;
        this.thumbnailFile = null;
        this.key= null;
        this.photoIv = null;
        this.thumbnailIv = null;
        this.Message = "";
        this.PrivatePhoto = false;
    }
    Photo(long id, String message, Boolean privatePhoto, String photoFilename, String thumbnailFilename, byte[] key, byte[] photoIv, byte[] thumbnailIv) throws FileNotFoundException {
        this.Message = message;
        this.PrivatePhoto = privatePhoto;

        this.photoFile = photoFilename.isEmpty() ? null : new File(photoFilename);
        if(this.photoFile != null && (!photoFile.exists() || !photoFile.canRead()))
            throw new FileNotFoundException("Could not find " + photoFilename);

        this.thumbnailFile = thumbnailFilename.isEmpty() ? null : new File(thumbnailFilename);
        if(thumbnailFile != null && (!thumbnailFile.exists() || !thumbnailFile.canRead()))
            throw new FileNotFoundException("Could not find " + thumbnailFilename);

        this.id = id;
        this.key = key;
        this.photoIv = photoIv;
        this.thumbnailIv = thumbnailIv;
    }

    public long getID() { return this.id; }
    public File getPhotoFile() { return this.photoFile; }
    public File getThumbnailFile() { return this.thumbnailFile; }

    public void LoadThumbnail(Context context)
    {
        if(this.realThumbnail == null)
        {
            Cipher decrypter;
            SecretKeySpec keySpec;
            GCMParameterSpec gcmSpec;

            FileInputStream ciphertextFileStream;
            CipherInputStream thumbnailCiphertextStream;

            try {
                decrypter = Cipher.getInstance("AES/GCM/NoPadding");
                keySpec = new SecretKeySpec(this.key, "AES");
                gcmSpec = new GCMParameterSpec(128, thumbnailIv);
                decrypter.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

                File ciphertextThumbnailFile = this.thumbnailFile;

                ciphertextFileStream = new FileInputStream(ciphertextThumbnailFile);
                thumbnailCiphertextStream = new CipherInputStream(ciphertextFileStream, decrypter);

                this.realThumbnail = BitmapFactory.decodeStream(thumbnailCiphertextStream);
                //Above assignment avoids infinite loop
                this.getPrivateThumbnail(context);
            } catch (IOException e) {
                Log.e(TAG, "Caught IOException when trying to process FileInputStream in LoadThumbnail()");
                ACRA.getErrorReporter().handleException(e);
            } catch (InvalidKeyException|InvalidAlgorithmParameterException|NoSuchAlgorithmException|NoSuchPaddingException e) {
                Log.e(TAG, "Caught " + e.getClass().getName() + " when trying to decrypt in LoadThumbnail(): " + e.toString());
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    //4096 maximum texture size, in case someone sent us extremely large photo
    public Bitmap getDecryptedPhoto()
    {
        return this.getDecryptedPhoto(4096, 4096);
    }
    private Bitmap getDecryptedPhoto(int width, int height)
    {
        if(this.decryptedPhoto == null)
            this.decryptedPhoto = BitmapUtils.DecodeStreamForSize(this.getDecryptedPhotoStream(), this.getDecryptedPhotoStream(), width, height);
        return this.decryptedPhoto;
    }
    public InputStream getDecryptedPhotoStream()
    {
        Cipher decrypter = null;
        try {
            decrypter = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(this.key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, photoIv);
            decrypter.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        } catch (InvalidKeyException|InvalidAlgorithmParameterException|NoSuchAlgorithmException|NoSuchPaddingException e) {
            Log.e(TAG, "Caught " + e.getClass().getName() + " when trying to decrypt in getDecryptedPhotoStream(): " + e.toString());
            ACRA.getErrorReporter().handleException(e);
        }

        File ciphertextPhotoFile = this.photoFile;
        FileInputStream ciphertextFileStream;
        CipherInputStream ciphertextStream = null;
        try {
            ciphertextFileStream = new FileInputStream(ciphertextPhotoFile);
            ciphertextStream = new CipherInputStream(ciphertextFileStream, decrypter);
        } catch (IOException e) {
            Log.e(TAG, "Caught IOException when trying to process FileInputStream in getDecryptedPhotoStream()");
            ACRA.getErrorReporter().handleException(e);
        }

        return ciphertextStream;
    }

    public void DeleteFiles()
    {
        this.photoFile.delete();
        this.thumbnailFile.delete();
    }

    public void FreeMemory()
    {
        if(this.decryptedPhoto != null) {
            this.decryptedPhoto.recycle();
            this.decryptedPhoto = null;
        }
        if(this.realThumbnail != null ) {
            this.realThumbnail.recycle();
            this.realThumbnail = null;
        }
        if(this.blurredThumbnail != null) {
            this.blurredThumbnail.recycle();
            this.blurredThumbnail = null;
        }
        this.thumbnailIsFailing = false;
    }
}

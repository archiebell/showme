package invalid.showme.model.message;

import android.util.Log;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.SessionBuilder;
import org.whispersystems.libaxolotl.SessionCipher;
import org.whispersystems.libaxolotl.UntrustedIdentityException;
import org.whispersystems.libaxolotl.protocol.CiphertextMessage;
import org.whispersystems.libaxolotl.protocol.PreKeyWhisperMessage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.IFriend;
import invalid.showme.model.IMe;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.DraftPhoto;
import invalid.showme.model.photo.SentPhoto;
import invalid.showme.util.BitStuff;
import invalid.showme.util.IOUtil;

public class OutgoingMessage extends Message implements Serializable
{
    final private static String TAG = "OutgoingMessage";

    public long sentPhotoRecordID;
    private String imgPath;
    private DraftPhoto draftPhoto;

    private transient IMe sender;
    private transient IFriend recipient;
    private long recipient_id;

    public OutgoingMessage(long sentPhotoRecordID, String imgPath, String message, Boolean privateMessage, IFriend recipient, IMe sender)
    {
        this.sentPhotoRecordID = sentPhotoRecordID;

        this.MessageID = null;
        this.imgPath = imgPath;
        this.draftPhoto = null;

        this.sender = sender;
        this.recipient = recipient;
        this.recipient_id = recipient.getID();

        this.message = message == null ? "" : message;
        this.privateMessage = privateMessage;

        if(sender.getKeyPair().getPrivateKey() == null)
            throw new InvalidParameterException();
    }
    public OutgoingMessage(long sentPhotoRecordID, DraftPhoto draft, String message, Boolean privateMessage, IFriend recipient, IMe sender)
    {
        this.sentPhotoRecordID = sentPhotoRecordID;

        this.MessageID = null;
        this.imgPath = null;
        this.draftPhoto = draft;

        this.sender = sender;
        this.recipient = recipient;
        recipient_id = recipient.getID();

        this.message = message == null ? "" : message;
        this.privateMessage = privateMessage;

        if(sender.getKeyPair().getPrivateKey() == null)
            throw new InvalidParameterException();
    }

    public void Reconstitute(UserProfile me)
    {
        this.sender = me;
        this.recipient = this.sender.findFriend(this.recipient_id);
    }

    public byte[] EncodeForSending(UserProfile me) throws IOException, UntrustedIdentityException, org.whispersystems.libaxolotl.InvalidKeyException {
        byte[] encoded = this.Encode();
        byte[] encodedForSending = new byte[this.recipient.getFingerprint().toBytes().length + encoded.length];

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            this.MessageID = md.digest(encoded);

            if(this.sentPhotoRecordID != -1) {
                SentPhoto sentPhoto = me.findSent(this.sentPhotoRecordID);
                if (sentPhoto.MessageID == null || Objects.equals(sentPhoto.MessageID, "")) {
                    sentPhoto.MessageID = BitStuff.toHexString(this.MessageID);
                    sentPhoto.Status = SentPhoto.SentPhotoStatus.Queued;
                    sentPhoto.saveToDatabase(DBHelper.getInstance(me));//Do not handle failure
                } else {
                    ACRA.getErrorReporter().handleException(new StrangeUsageException("already have messageID for sent photo record: " + sentPhoto.MessageID));
                }
            }
        } catch(NoSuchAlgorithmException e) {
            ACRA.getErrorReporter().handleException(e);
        }

        System.arraycopy(this.recipient.getFingerprint().toBytes(), 0, encodedForSending, 0, this.recipient.getFingerprint().toBytes().length);
        System.arraycopy(encoded, 0, encodedForSending, this.recipient.getFingerprint().toBytes().length, encoded.length);
        return encodedForSending;
    }
    public byte[] getMessageId() throws NullPointerException {
        if(this.MessageID == null) throw new NullPointerException("MessageID not calculated Yet.");
        return this.MessageID;
    }

    public byte[] Encode() throws IOException, UntrustedIdentityException, org.whispersystems.libaxolotl.InvalidKeyException {
        //Estimate ~how much space going to need
        int GUESS_A_JPEG_SIZE = 1024 * 1024 * 6;
        ByteArrayOutputStream plaintext = new ByteArrayOutputStream(256 + 64 + 16 + GUESS_A_JPEG_SIZE + this.message.length());
        ByteArrayOutputStream finalOutput = new ByteArrayOutputStream(256 + 64 + 16 + GUESS_A_JPEG_SIZE + this.message.length());
        InputStream imgStream;

        try {
            if(!sender.containsSession(recipient.getAxolotlAddress())) {
                SessionBuilder sessionBuilder = new SessionBuilder(sender, recipient.getAxolotlAddress());
                sessionBuilder.process(recipient.getPreKeyBundle());
            }

            //Version
            plaintext.write(TAG_MESSAGEVERSION);
            plaintext.write(0x01);

            //Message
            plaintext.write(TAG_MSG);
            plaintext.write(BitStuff.toByteArray(this.message.getBytes("UTF-8").length));
            plaintext.write(this.message.getBytes("UTF-8"));

            //Public/Private
            plaintext.write(TAG_PRIVATE);
            plaintext.write(BitStuff.toByteArray(1));
            if(this.privateMessage)
                plaintext.write(0x01);
            else
                plaintext.write(0x00);

            //Image
            File imgFile = null;
            if(this.imgPath != null) {
                imgFile = new File(this.imgPath);
                imgStream = new FileInputStream(imgFile);
            }
            else {
                imgStream = this.draftPhoto.getDecryptedPhotoStream();
            }
            //TODO: Maybe should save decrypted length so don't have to read all into memory?
            byte[] plaintextPhoto = IOUtil.readFully(imgStream);
            plaintext.write(TAG_IMG);
            plaintext.write(BitStuff.toByteArray(plaintextPhoto.length));
            plaintext.write(plaintextPhoto);
            //noinspection UnusedAssignment
            imgStream = null;
            //noinspection UnusedAssignment
            plaintextPhoto = null;
            if(imgFile != null) imgFile.delete();

            //Encrypt with Axolotl
            SessionCipher encryptCipher = new SessionCipher(sender, recipient.getAxolotlAddress());
            CiphertextMessage encryptedMessage = encryptCipher.encrypt(plaintext.toByteArray());
            //noinspection UnusedAssignment
            plaintext = null;
            byte[] ciphertext = encryptedMessage.serialize();

            //Combine
            finalOutput.write(TAG_ENVELOPEVERSION);
            finalOutput.write(0x01);

            finalOutput.write(TAG_SENDERKEY);
            byte[] serializedPubKey = this.sender.getIdentityKeyPair().getPublicKey().serialize();
            finalOutput.write(BitStuff.toByteArray(serializedPubKey.length));
            finalOutput.write(serializedPubKey);

            finalOutput.write(TAG_CIPHERTEXTTYPE);
            if(encryptedMessage instanceof PreKeyWhisperMessage)
                finalOutput.write(CIPHERTEXTTYPE_PREKEYMSG);
            else
                finalOutput.write(CIPHERTEXTTYPE_MSG);

            finalOutput.write(TAG_CIPHERTEXT);
            finalOutput.write(BitStuff.toByteArray(ciphertext.length));
            finalOutput.write(ciphertext);
        } catch (org.whispersystems.libaxolotl.InvalidKeyException|UntrustedIdentityException|IOException e) {
            Log.e(TAG, "While encoding message, caught " + e.getClass().getName() + ": " + e.toString());
            ACRA.getErrorReporter().handleException(e);
            throw e;
        }
        return finalOutput.toByteArray();
    }
}

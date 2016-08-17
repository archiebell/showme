package invalid.showme.model.message;

import android.util.Log;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.DuplicateMessageException;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.InvalidVersionException;
import org.whispersystems.libaxolotl.LegacyMessageException;
import org.whispersystems.libaxolotl.NoSessionException;
import org.whispersystems.libaxolotl.SessionCipher;
import org.whispersystems.libaxolotl.UntrustedIdentityException;
import org.whispersystems.libaxolotl.protocol.PreKeyWhisperMessage;
import org.whispersystems.libaxolotl.protocol.WhisperMessage;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;

import invalid.showme.exceptions.InvalidMessageException;
import invalid.showme.exceptions.NewMessageVersionException;
import invalid.showme.exceptions.OldMessageVersionException;
import invalid.showme.model.IFriend;
import invalid.showme.model.IMe;
import invalid.showme.util.BitStuff;

public class IncomingMessage extends invalid.showme.model.message.Message implements Serializable
{
    final private static String TAG = "IncomingMessage";

    public IFriend sender = null;
    public byte[] imgBytes;

    private IncomingMessage(byte[] messageID) { this.MessageID = messageID; }

    public static IncomingMessage Decode(IMe me, String messageID, byte[] bytes) throws InvalidMessageException, NewMessageVersionException, OldMessageVersionException {
        byte tag;
        int length;
        int index = 0;
        IncomingMessage msg = new IncomingMessage(BitStuff.fromHexString(messageID));

        try {
            //Check first version byte
            if(bytes.length - index < 1) throw new InvalidMessageException("First Version Byte");
            tag = bytes[index++];
            if(tag != TAG_ENVELOPEVERSION) throw new InvalidMessageException("tag not TAG_ENVELOPEVERSION");

            if(bytes[index] < 0x01)
            {
                String errMsg = "received old no-longer supported outer envelope version: " + BitStuff.toHexString(bytes[index - 1]);
                Log.w(TAG, errMsg);
                throw new OldMessageVersionException(errMsg);
            } else if(bytes[index] != 0x01) {
                String errMsg = "received backwards-incompatible outer envelope version: " + BitStuff.toHexString(bytes[index - 1]);
                Log.w(TAG, errMsg);
                throw new NewMessageVersionException(errMsg);
            }
            index++;

            //Obtain Sender Key
            if(bytes.length - index < 1) throw new InvalidMessageException();
            tag = bytes[index++];
            if(tag != TAG_SENDERKEY) throw new InvalidMessageException();

            if(bytes.length - index < 4) throw new InvalidMessageException();
            length = BitStuff.toInt(bytes, index);
            index += 4;

            if(bytes.length - index - length < 0) throw new InvalidMessageException();
            if(length != 33) throw new InvalidKeyException("Axolotl ECC Public key not 33 bytes.");
            for(IFriend f : me.getFriends()) {
                if(f.getFingerprint().equals(bytes, index+1))//+1 to offset type byte
                    msg.sender = f;
            }
            if(msg.sender == null) throw new InvalidMessageException("Could not identify sender");
            index += length;

            //Obtain Ciphertext Type
            if(bytes.length - index < 1) throw new InvalidMessageException();
            tag = bytes[index++];
            if(tag != TAG_CIPHERTEXTTYPE) throw new InvalidMessageException("tag not TAG_CIPHERTEXTTYPE");

            if(bytes.length - index < 1) throw new InvalidMessageException();
            byte ciphertextType = bytes[index++];

            //Extract Ciphertext
            if(bytes.length - index < 1) throw new InvalidMessageException();
            tag = bytes[index++];
            if(tag != TAG_CIPHERTEXT) throw new InvalidMessageException();

            if(bytes.length - index < 4) throw new InvalidMessageException();
            length = BitStuff.toInt(bytes, index);
            index += 4;

            if(bytes.length - index - length < 0) throw new InvalidMessageException();
            byte[] ciphertext = new byte[length];
            BitStuff.copy(bytes, index, length, ciphertext);
            index += length;

            //Decrypt Ciphertext
            SessionCipher decryptCipher = new SessionCipher(me, msg.sender.getAxolotlAddress());
            byte[] plaintext;
            if(ciphertextType == CIPHERTEXTTYPE_PREKEYMSG) {
                PreKeyWhisperMessage whisperMessage = new PreKeyWhisperMessage(ciphertext);
                plaintext = decryptCipher.decrypt(whisperMessage);
            } else {
                WhisperMessage whisperMessage = new WhisperMessage(ciphertext);
                plaintext = decryptCipher.decrypt(whisperMessage);
            }


            //Now parse Message
            index = 0;
            Boolean seenVersion = false;
            Boolean seenMsg = false;
            Boolean seenPrivate = false;
            Boolean seenImg = false;
            while(true)
            {
                if(plaintext.length - index == 0) break;
                if(plaintext.length - index < 1) throw new InvalidMessageException();
                tag = plaintext[index++];

                switch(tag)
                {
                    case TAG_MESSAGEVERSION:
                        if(seenVersion) throw new InvalidMessageException();

                        if(plaintext.length - index < 1) throw new InvalidMessageException();
                        byte version = plaintext[index++];
                        if(version < 0x01) {
                            String errMsg = "received old no-longer supported outer envelope version:" + BitStuff.toHexString(bytes[index - 1]);
                            Log.w(TAG, errMsg);
                            throw new OldMessageVersionException(errMsg);
                        } else if(version != 0x01) {
                            String errMsg = "received backwards-incompatible inner envelope version:" + BitStuff.toHexString(bytes[index - 1]);
                            Log.w(TAG, errMsg);
                            throw new NewMessageVersionException(errMsg);
                        }
                        seenVersion = true;
                        continue;
                    case TAG_MSG:
                        if(seenMsg) throw new InvalidMessageException();

                        if(plaintext.length - index < 4) throw new InvalidMessageException();
                        length = BitStuff.toInt(plaintext, index);
                        index += 4;

                        if(plaintext.length - index - length < 0) throw new InvalidMessageException();
                        byte[] msgBytes = new byte[length];
                        BitStuff.copy(plaintext, index, length, msgBytes);
                        index += length;
                        msg.message = new String(msgBytes, "UTF-8");

                        seenMsg = true;
                        continue;
                    case TAG_PRIVATE:
                        if(seenPrivate) throw new InvalidMessageException();

                        if(plaintext.length - index < 4) throw new InvalidMessageException();
                        length = BitStuff.toInt(plaintext, index);
                        index += 4;

                        if(plaintext.length - index - length < 0) throw new InvalidMessageException();
                        if(length != 1) throw new InvalidMessageException();
                        msg.privateMessage = plaintext[index] != 0;
                        index += 1;

                        seenPrivate = true;
                        continue;
                    case TAG_IMG:
                        if(seenImg) throw new InvalidMessageException();

                        if(plaintext.length - index < 4) throw new InvalidMessageException();
                        length = BitStuff.toInt(plaintext, index);
                        index += 4;

                        if(plaintext.length - index - length < 0) throw new InvalidMessageException();
                        msg.imgBytes = new byte[length];
                        BitStuff.copy(plaintext, index, length, msg.imgBytes);
                        index += length;

                        seenImg = true;
                        continue;
                    default:
                        //Tag don't know how to handle, but backwards-compatible, so do not raise error
                        String errMsg = "received tag don't understand: " + BitStuff.toHexString(bytes[index - 1]);
                        Log.w(TAG, errMsg);
                        ACRA.getErrorReporter().handleException(new InvalidMessageException(errMsg));

                        if(plaintext.length - index < 4) throw new InvalidMessageException();
                        length = BitStuff.toInt(plaintext, index);
                        index += 4;

                        if(plaintext.length - index - length < 0) throw new InvalidMessageException();
                        index += length;
                        continue;
                }
            }
            //Verify saw parts expected.
            if(!seenVersion || !seenMsg || !seenImg || !seenPrivate) {
                String errMsg;
                if(!seenVersion)
                    errMsg = "did not receive version packet.";
                else if(!seenMsg)
                    errMsg = "did not receive msg packet.";
                else if(!seenPrivate)
                    errMsg = "did not receive private packet.";
                else if(!seenImg)
                    errMsg = "did not receive image packet.";
                else
                    errMsg = "did not receive packet expected but aren't checking for...?";
                Log.w(TAG, errMsg);
                throw new InvalidMessageException(errMsg);
            }
        } catch (org.whispersystems.libaxolotl.InvalidKeyException|InvalidVersionException| org.whispersystems.libaxolotl.InvalidMessageException|NoSessionException|DuplicateMessageException|LegacyMessageException|InvalidKeyIdException|UntrustedIdentityException|InvalidKeyException|IOException e) {
            Log.e(TAG, "While decoding message, caught " + e.getClass().getName() + ": " + e.toString());
            ACRA.getErrorReporter().handleException(e);
        }

        return msg;
    }

}

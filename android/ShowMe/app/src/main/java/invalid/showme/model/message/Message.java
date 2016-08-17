package invalid.showme.model.message;

import java.io.Serializable;

public class Message implements Serializable
{
    final static byte TAG_MESSAGEVERSION  = (byte)1;
    final static byte TAG_ENVELOPEVERSION = (byte)2;
    final static byte TAG_MSG          = (byte)3;
    final static byte TAG_PRIVATE      = (byte)4;
    final static byte TAG_IMG          = (byte)5;

    final static byte TAG_SENDERKEY    = (byte)252;
    final static byte TAG_CIPHERTEXTTYPE = (byte)253;
    final static byte TAG_CIPHERTEXT   = (byte)254;


    final static byte CIPHERTEXTTYPE_PREKEYMSG = (byte)1;
    final static byte CIPHERTEXTTYPE_MSG = (byte)2;

    public byte[] MessageID;

    public String message;
    public Boolean privateMessage;

    Message() {}
}

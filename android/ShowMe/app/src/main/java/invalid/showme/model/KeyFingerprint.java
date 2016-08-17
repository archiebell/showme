package invalid.showme.model;

import org.whispersystems.libaxolotl.ecc.ECPublicKey;

import java.io.Serializable;

import invalid.showme.util.BitStuff;

public class KeyFingerprint implements Serializable
{
    final private static String TAG = "KeyFingerprint";

    private String fingerprint_str;
    private byte[] fingerprint_bytes;
    public KeyFingerprint(String hexString) {
        this.fingerprint_str = hexString;
        this.fingerprint_bytes = BitStuff.fromHexString(hexString);
    }
    public KeyFingerprint(ECPublicKey pubKey) {
        this.fingerprint_bytes = BitStuff.substring(pubKey.serialize(), 1);
        this.fingerprint_str = BitStuff.toHexString(this.fingerprint_bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KeyFingerprint other = (KeyFingerprint) obj;
        return !((this.fingerprint_str == null) ? (other.fingerprint_str != null) : !this.fingerprint_str.equals(other.fingerprint_str));
    }

    public boolean equals(byte[] byteArray, int index) {
        if (byteArray == null) {
            return false;
        }
        return BitStuff.CompareArrays(this.fingerprint_bytes.length, this.fingerprint_bytes, 0, byteArray, index);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.fingerprint_str != null ? this.fingerprint_str.hashCode() : 0);
        return hash;
    }


    public String toString()
    {
        return this.fingerprint_str;
    }
    public byte[] toBytes() { return this.fingerprint_bytes; }
}

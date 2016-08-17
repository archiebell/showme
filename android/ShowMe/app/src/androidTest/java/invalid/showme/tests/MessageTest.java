package invalid.showme.tests;

import org.whispersystems.libaxolotl.DuplicateMessageException;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.InvalidVersionException;
import org.whispersystems.libaxolotl.LegacyMessageException;
import org.whispersystems.libaxolotl.NoSessionException;
import org.whispersystems.libaxolotl.SessionBuilder;
import org.whispersystems.libaxolotl.SessionCipher;
import org.whispersystems.libaxolotl.UntrustedIdentityException;
import org.whispersystems.libaxolotl.protocol.CiphertextMessage;
import org.whispersystems.libaxolotl.protocol.PreKeyWhisperMessage;
import org.whispersystems.libaxolotl.protocol.WhisperMessage;
import org.whispersystems.libaxolotl.state.PreKeyBundle;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Security;

import invalid.showme.dummy.DummyFriend;
import invalid.showme.dummy.DummyMe;
import invalid.showme.exceptions.InvalidMessageException;
import invalid.showme.exceptions.NewMessageVersionException;
import invalid.showme.exceptions.OldMessageVersionException;
import invalid.showme.model.message.IncomingMessage;
import invalid.showme.model.message.OutgoingMessage;

public class MessageTest extends TestCaseWithUtils
{
    //TODO: Implement negative tests make sure things don't parse wrong

    //Alice sends Bob two messages, Bob replies
    public void testAxolotl1() throws UntrustedIdentityException, InvalidKeyException, UnsupportedEncodingException, InvalidVersionException, org.whispersystems.libaxolotl.InvalidMessageException, DuplicateMessageException, LegacyMessageException, InvalidKeyIdException, java.security.InvalidKeyException, NoSessionException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        /* Alice's Phone */
        DummyMe alice = new DummyMe("Alice", getRandomECCKey());
        PreKeyRecord arec1 = alice.getNewPreKey();
        SignedPreKeyRecord asrec1 = alice.getSignedPrekey();
        PreKeyBundle aliceBundle = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec1.getId(), arec1.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());

        /* Bob's Phone */
        DummyMe bob = new DummyMe("Bob", getRandomECCKey());
        PreKeyRecord brec1 = bob.getNewPreKey();
        SignedPreKeyRecord bsrec2 = bob.getSignedPrekey();
        PreKeyBundle bobBundle = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec1.getId(), brec1.getKeyPair().getPublicKey(), bsrec2.getId(), bsrec2.getKeyPair().getPublicKey(), bsrec2.getSignature(), bob.getIdentityKeyPair().getPublicKey());

        DummyFriend aliceFriend = new DummyFriend("Alice", aliceBundle);
        bob.addFriend(aliceFriend);

        /* Alice's Phone */
        DummyFriend bobFriend = new DummyFriend("Bob", bobBundle);
        alice.addFriend(bobFriend);

        SessionBuilder sessionBuilder1 = new SessionBuilder(alice, alice, alice, alice, bobFriend.getAxolotlAddress());
        sessionBuilder1.process(bobBundle);

        SessionCipher encryptCipher1 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        byte[] plaintext1 = "01234 5678".getBytes("UTF-8");
        CiphertextMessage message1 = encryptCipher1.encrypt(plaintext1);
        byte[] wire1 = message1.serialize();

        SessionCipher encryptCipher2 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        byte[] plaintext2 = "abcfgh jklio".getBytes("UTF-8");
        CiphertextMessage message2 = encryptCipher2.encrypt(plaintext2);
        byte[] wire2 = message2.serialize();

        /* Bob's Phone */
        SessionCipher decryptCipher1 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        PreKeyWhisperMessage msg1 = new PreKeyWhisperMessage(wire1);
        byte[] decryptedplaintext1 = decryptCipher1.decrypt(msg1);

        SessionCipher decryptCipher2 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        PreKeyWhisperMessage msg2 = new PreKeyWhisperMessage(wire2);
        byte[] decryptedplaintext2 = decryptCipher2.decrypt(msg2);

        /* Assert */
        assertTrue(alice.containsSession(bobFriend.getAxolotlAddress()));
        assertTrue(bob.containsSession(aliceFriend.getAxolotlAddress()));
        assertTrue(alice.loadSession(bobFriend.getAxolotlAddress()).getSessionState().getSessionVersion() == 3);
        assertTrue(bob.loadSession(aliceFriend.getAxolotlAddress()).getSessionState().getAliceBaseKey() != null);
        assertTrue(CompareArrays(plaintext1, decryptedplaintext1));
        assertTrue(CompareArrays(plaintext2, decryptedplaintext2));

        /* Bob's Phone */
        SessionCipher encryptCipher3 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        byte[] plaintext3 = "qwert yuiop".getBytes("UTF-8");
        CiphertextMessage message3 = encryptCipher3.encrypt(plaintext3);
        byte[] wire3 = message3.serialize();

        /* Alice's Phone */
        SessionCipher decryptCipher3 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        WhisperMessage msg3 = new WhisperMessage(wire3);
        byte[] decryptedplaintext3 = decryptCipher3.decrypt(msg3);

        /* Assert */
        assertTrue(CompareArrays(plaintext3, decryptedplaintext3));
    }

    //Alice and Bob exchange several messages
    public void testAxolotl2() throws UntrustedIdentityException, InvalidKeyException, UnsupportedEncodingException, InvalidVersionException, org.whispersystems.libaxolotl.InvalidMessageException, DuplicateMessageException, LegacyMessageException, InvalidKeyIdException, NoSessionException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        /* Alice's Phone */
        DummyMe alice = new DummyMe("Alice", getRandomECCKey());
        PreKeyRecord arec1 = alice.getNewPreKey();
        SignedPreKeyRecord asrec1 = alice.getSignedPrekey();
        PreKeyBundle aliceBundle = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec1.getId(), arec1.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());

        /* Bob's Phone */
        DummyMe bob = new DummyMe("Bob", getRandomECCKey());
        PreKeyRecord brec1 = bob.getNewPreKey();
        SignedPreKeyRecord bsrec2 = bob.getSignedPrekey();
        PreKeyBundle bobBundle = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec1.getId(), brec1.getKeyPair().getPublicKey(), bsrec2.getId(), bsrec2.getKeyPair().getPublicKey(), bsrec2.getSignature(), bob.getIdentityKeyPair().getPublicKey());

        DummyFriend aliceFriend = new DummyFriend("Alice", aliceBundle);
        bob.addFriend(aliceFriend);

        /* Alice's Phone */
        DummyFriend bobFriend = new DummyFriend("Bob", bobBundle);
        alice.addFriend(bobFriend);

        SessionBuilder sessionBuilder1 = new SessionBuilder(alice, alice, alice, alice, bobFriend.getAxolotlAddress());
        sessionBuilder1.process(bobBundle);

        SessionCipher encryptCipher1 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        byte[] plaintext1 = "01234 5678".getBytes("UTF-8");
        CiphertextMessage message1 = encryptCipher1.encrypt(plaintext1);
        byte[] wire1 = message1.serialize();

        /* Bob's Phone */
        SessionCipher decryptCipher1 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        PreKeyWhisperMessage msg1 = new PreKeyWhisperMessage(wire1);
        byte[] decryptedplaintext1 = decryptCipher1.decrypt(msg1);

        /* Assert */
        assertTrue(alice.containsSession(bobFriend.getAxolotlAddress()));
        assertTrue(bob.containsSession(aliceFriend.getAxolotlAddress()));
        assertTrue(alice.loadSession(bobFriend.getAxolotlAddress()).getSessionState().getSessionVersion() == 3);
        assertTrue(bob.loadSession(aliceFriend.getAxolotlAddress()).getSessionState().getAliceBaseKey() != null);
        assertTrue(CompareArrays(plaintext1, decryptedplaintext1));

        /* Bob's Phone */
        SessionCipher encryptCipher2 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        byte[] plaintext2 = "qwert yuiop".getBytes("UTF-8");
        CiphertextMessage message2 = encryptCipher2.encrypt(plaintext2);
        byte[] wire2 = message2.serialize();

        /* Alice's Phone */
        SessionCipher decryptCipher2 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        WhisperMessage msg2 = new WhisperMessage(wire2);
        byte[] decryptedplaintext2 = decryptCipher2.decrypt(msg2);

        /* Assert */
        assertTrue(CompareArrays(plaintext2, decryptedplaintext2));

        /* Alice's Phone */
        SessionCipher encryptCipher3 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        byte[] plaintext3 = "asdfg hjjkkl".getBytes("UTF-8");
        CiphertextMessage message3 = encryptCipher3.encrypt(plaintext3);
        byte[] wire3 = message3.serialize();

        /* Bob's Phone */
        SessionCipher decryptCipher3 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        WhisperMessage msg3 = new WhisperMessage(wire3);
        byte[] decryptedplaintext3 = decryptCipher3.decrypt(msg3);

        /* Assert */
        assertTrue(CompareArrays(plaintext3, decryptedplaintext3));

        /* Bob's Phone */
        SessionCipher encryptCipher4 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        byte[] plaintext4 = "zxcvb".getBytes("UTF-8");
        CiphertextMessage message4 = encryptCipher4.encrypt(plaintext4);
        byte[] wire4 = message4.serialize();

        /* Alice's Phone */
        SessionCipher decryptCipher4 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        WhisperMessage msg4 = new WhisperMessage(wire4);
        byte[] decryptedplaintext4 = decryptCipher4.decrypt(msg4);

        /* Assert */
        assertTrue(CompareArrays(plaintext4, decryptedplaintext4));
    }

    //Alice, Bob, Carol, Dave exchange several messages
    public void testAxolotl3() throws UntrustedIdentityException, InvalidKeyException, UnsupportedEncodingException, InvalidVersionException, org.whispersystems.libaxolotl.InvalidMessageException, DuplicateMessageException, LegacyMessageException, InvalidKeyIdException, NoSessionException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        /* Setup */

        /* Alice's Phone */
        DummyMe alice = new DummyMe("Alice", getRandomECCKey());
        PreKeyRecord arec1 = alice.getNewPreKey();
        PreKeyRecord arec2 = alice.getNewPreKey();
        PreKeyRecord arec3 = alice.getNewPreKey();
        SignedPreKeyRecord asrec1 = alice.getSignedPrekey();
        PreKeyBundle aliceBundleForBob = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec1.getId(), arec1.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());
        PreKeyBundle aliceBundleForCarol = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec2.getId(), arec2.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());
        PreKeyBundle aliceBundleForDave = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec3.getId(), arec3.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());

        /* Bob's Phone */
        DummyMe bob = new DummyMe("Bob", getRandomECCKey());
        PreKeyRecord brec1 = bob.getNewPreKey();
        PreKeyRecord brec2 = bob.getNewPreKey();
        PreKeyRecord brec3 = bob.getNewPreKey();
        SignedPreKeyRecord bsrec1 = bob.getSignedPrekey();
        PreKeyBundle bobBundleForAlice = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec1.getId(), brec1.getKeyPair().getPublicKey(), bsrec1.getId(), bsrec1.getKeyPair().getPublicKey(), bsrec1.getSignature(), bob.getIdentityKeyPair().getPublicKey());
        PreKeyBundle bobBundleForCarol = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec2.getId(), brec2.getKeyPair().getPublicKey(), bsrec1.getId(), bsrec1.getKeyPair().getPublicKey(), bsrec1.getSignature(), bob.getIdentityKeyPair().getPublicKey());
        PreKeyBundle bobBundleForDave = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec3.getId(), brec3.getKeyPair().getPublicKey(), bsrec1.getId(), bsrec1.getKeyPair().getPublicKey(), bsrec1.getSignature(), bob.getIdentityKeyPair().getPublicKey());

        /* Carol's Phone */
        DummyMe carol = new DummyMe("Carol", getRandomECCKey());
        PreKeyRecord crec1 = carol.getNewPreKey();
        PreKeyRecord crec2 = carol.getNewPreKey();
        PreKeyRecord crec3 = carol.getNewPreKey();
        SignedPreKeyRecord csrec1 = carol.getSignedPrekey();
        PreKeyBundle carolBundleForAlice = new PreKeyBundle(carol.getLocalRegistrationId(), carol.getLocalDeviceId(), crec1.getId(), crec1.getKeyPair().getPublicKey(), csrec1.getId(), csrec1.getKeyPair().getPublicKey(), csrec1.getSignature(), carol.getIdentityKeyPair().getPublicKey());
        PreKeyBundle carolBundleForBob = new PreKeyBundle(carol.getLocalRegistrationId(), carol.getLocalDeviceId(), crec2.getId(), crec2.getKeyPair().getPublicKey(), csrec1.getId(), csrec1.getKeyPair().getPublicKey(), csrec1.getSignature(), carol.getIdentityKeyPair().getPublicKey());
        PreKeyBundle carolBundleForDave = new PreKeyBundle(carol.getLocalRegistrationId(), carol.getLocalDeviceId(), crec3.getId(), crec3.getKeyPair().getPublicKey(), csrec1.getId(), csrec1.getKeyPair().getPublicKey(), csrec1.getSignature(), carol.getIdentityKeyPair().getPublicKey());

        /* Dave's Phone */
        DummyMe dave = new DummyMe("Dave", getRandomECCKey());
        PreKeyRecord drec1 = dave.getNewPreKey();
        PreKeyRecord drec2 = dave.getNewPreKey();
        PreKeyRecord drec3 = dave.getNewPreKey();
        SignedPreKeyRecord dsrec1 = dave.getSignedPrekey();
        PreKeyBundle daveBundleForAlice = new PreKeyBundle(dave.getLocalRegistrationId(), dave.getLocalDeviceId(), drec1.getId(), drec1.getKeyPair().getPublicKey(), dsrec1.getId(), dsrec1.getKeyPair().getPublicKey(), dsrec1.getSignature(), dave.getIdentityKeyPair().getPublicKey());
        PreKeyBundle daveBundleForBob = new PreKeyBundle(dave.getLocalRegistrationId(), dave.getLocalDeviceId(), drec2.getId(), drec2.getKeyPair().getPublicKey(), dsrec1.getId(), dsrec1.getKeyPair().getPublicKey(), dsrec1.getSignature(), dave.getIdentityKeyPair().getPublicKey());
        PreKeyBundle daveBundleForCarol = new PreKeyBundle(dave.getLocalRegistrationId(), dave.getLocalDeviceId(), drec3.getId(), drec3.getKeyPair().getPublicKey(), dsrec1.getId(), dsrec1.getKeyPair().getPublicKey(), dsrec1.getSignature(), dave.getIdentityKeyPair().getPublicKey());

        /* Alice's Phone */
        DummyFriend alicesFriendBob = new DummyFriend("Bob", bobBundleForAlice);
        alice.addFriend(alicesFriendBob);
        DummyFriend alicesFriendCarol = new DummyFriend("Carol", carolBundleForAlice);
        alice.addFriend(alicesFriendCarol);
        DummyFriend alicesFriendDave = new DummyFriend("Dave", daveBundleForAlice);
        alice.addFriend(alicesFriendDave);

        /* Bob's Phone */
        DummyFriend bobsFriendAlice = new DummyFriend("Alice", aliceBundleForBob);
        bob.addFriend(bobsFriendAlice);
        DummyFriend bobsFriendCarol = new DummyFriend("Carol", carolBundleForBob);
        bob.addFriend(bobsFriendCarol);
        DummyFriend bobsFriendDave = new DummyFriend("Dave", daveBundleForBob);
        bob.addFriend(bobsFriendDave);

        /* Carol's Phone */
        DummyFriend carolsFriendAlice = new DummyFriend("Alice", aliceBundleForCarol);
        carol.addFriend(carolsFriendAlice);
        DummyFriend carolsFriendBob = new DummyFriend("Bob", bobBundleForCarol);
        carol.addFriend(carolsFriendBob);
        DummyFriend carolsFriendDave = new DummyFriend("Dave", daveBundleForCarol);
        carol.addFriend(carolsFriendDave);

        /* Dave's Phone */
        DummyFriend davesFriendAlice = new DummyFriend("Alice", aliceBundleForDave);
        dave.addFriend(davesFriendAlice);
        DummyFriend davesFriendBob = new DummyFriend("Bob", bobBundleForDave);
        dave.addFriend(davesFriendBob);
        DummyFriend davesFriendCarol = new DummyFriend("Carol", carolBundleForDave);
        dave.addFriend(davesFriendCarol);


        /* /Setup */


        /* Alice -> Bob,  Alice's Phone */
        SessionBuilder sessionBuilder1 = new SessionBuilder(alice, alice, alice, alice, alicesFriendBob.getAxolotlAddress());
        sessionBuilder1.process(alicesFriendBob.getPreKeyBundle());

        SessionCipher encryptCipher1 = new SessionCipher(alice, alicesFriendBob.getAxolotlAddress());
        byte[] plaintext1 = "Alice -> Bob #1 ".getBytes("UTF-8");
        CiphertextMessage message1 = encryptCipher1.encrypt(plaintext1);
        byte[] wire1 = message1.serialize();


        /* Bob -> Carol, Bob's Phone */
        SessionBuilder sessionBuilder2 = new SessionBuilder(bob, bobsFriendCarol.getAxolotlAddress());
        sessionBuilder2.process(bobsFriendCarol.getPreKeyBundle());

        SessionCipher encryptCipher2 = new SessionCipher(bob, bobsFriendCarol.getAxolotlAddress());
        byte[] plaintext2 = "Bob -> Carol #1 ".getBytes("UTF-8");
        CiphertextMessage message2 = encryptCipher2.encrypt(plaintext2);
        byte[] wire2 = message2.serialize();


        /* Alice -> Bob, Bob's Phone */
        SessionCipher decryptCipher1 = new SessionCipher(bob, bobsFriendAlice.getAxolotlAddress());
        PreKeyWhisperMessage msg1 = new PreKeyWhisperMessage(wire1);
        byte[] decryptedplaintext1 = decryptCipher1.decrypt(msg1);

        /* Alice -> Bob, Assert */
        assertTrue(alice.containsSession(alicesFriendBob.getAxolotlAddress()));
        assertTrue(bob.containsSession(bobsFriendAlice.getAxolotlAddress()));
        assertTrue(alice.loadSession(alicesFriendBob.getAxolotlAddress()).getSessionState().getSessionVersion() == 3);
        assertTrue(bob.loadSession(bobsFriendAlice.getAxolotlAddress()).getSessionState().getAliceBaseKey() != null);
        assertTrue(CompareArrays(plaintext1, decryptedplaintext1));


        /* Bob -> Carol, Carol's Phone */
        SessionCipher decryptCipher2 = new SessionCipher(carol, carolsFriendBob.getAxolotlAddress());
        PreKeyWhisperMessage msg2 = new PreKeyWhisperMessage(wire2);
        byte[] decryptedplaintext2 = decryptCipher2.decrypt(msg2);

        /* Bob -> Carol, Assert */
        assertTrue(carol.containsSession(carolsFriendBob.getAxolotlAddress()));
        assertTrue(bob.containsSession(bobsFriendCarol.getAxolotlAddress()));
        assertTrue(carol.loadSession(carolsFriendBob.getAxolotlAddress()).getSessionState().getSessionVersion() == 3);
        assertTrue(carol.loadSession(carolsFriendBob.getAxolotlAddress()).getSessionState().getAliceBaseKey() != null);
        assertTrue(CompareArrays(plaintext2, decryptedplaintext2));


        /* Bob -> Alice, Bob's Phone */
        SessionCipher encryptCipher3 = new SessionCipher(bob, bobsFriendAlice.getAxolotlAddress());
        byte[] plaintext3 = "Bob -> Alice #2!".getBytes("UTF-8");
        CiphertextMessage message3 = encryptCipher3.encrypt(plaintext3);
        byte[] wire3 = message3.serialize();

        /* Bob -> Alice, Alice's Phone */
        SessionCipher decryptCipher3 = new SessionCipher(alice, alicesFriendBob.getAxolotlAddress());
        WhisperMessage msg3 = new WhisperMessage(wire3);
        byte[] decryptedplaintext3 = decryptCipher3.decrypt(msg3);

        /* Bob -> Alice, Assert */
        assertTrue(CompareArrays(plaintext3, decryptedplaintext3));


        /* Carol -> Dave, Carol's Phone */
        SessionBuilder sessionBuilder3 = new SessionBuilder(carol, carolsFriendDave.getAxolotlAddress());
        sessionBuilder3.process(carolsFriendDave.getPreKeyBundle());

        SessionCipher encryptCipher4 = new SessionCipher(carol, carolsFriendDave.getAxolotlAddress());
        byte[] plaintext4 = "Carol -> Dave #1".getBytes("UTF-8");
        CiphertextMessage message4 = encryptCipher4.encrypt(plaintext4);
        byte[] wire4 = message4.serialize();


        /* Carol-> Dave, Dave's Phone */
        SessionCipher decryptCipher4 = new SessionCipher(dave, davesFriendCarol.getAxolotlAddress());
        PreKeyWhisperMessage msg4 = new PreKeyWhisperMessage(wire4);
        byte[] decryptedplaintext4 = decryptCipher4.decrypt(msg4);

        /* Carol -> Dave, Assert */
        assertTrue(dave.containsSession(davesFriendCarol.getAxolotlAddress()));
        assertTrue(carol.containsSession(carolsFriendDave.getAxolotlAddress()));
        assertTrue(dave.loadSession(davesFriendCarol.getAxolotlAddress()).getSessionState().getSessionVersion() == 3);
        assertTrue(dave.loadSession(davesFriendCarol.getAxolotlAddress()).getSessionState().getAliceBaseKey() != null);
        assertTrue(CompareArrays(plaintext4, decryptedplaintext4));

        /* Dave -> Alice, Dave's Phone */
        SessionBuilder sessionBuilder4 = new SessionBuilder(dave, davesFriendAlice.getAxolotlAddress());
        sessionBuilder4.process(davesFriendAlice.getPreKeyBundle());

        SessionCipher encryptCipher5 = new SessionCipher(dave, davesFriendAlice.getAxolotlAddress());
        byte[] plaintext5 = "Dave -> Alice #1".getBytes("UTF-8");
        CiphertextMessage message5 = encryptCipher5.encrypt(plaintext5);
        byte[] wire5 = message5.serialize();

        /* Dave -> Alice, Alice's Phone */
        SessionCipher decryptCipher5 = new SessionCipher(alice, alicesFriendDave.getAxolotlAddress());
        PreKeyWhisperMessage msg5 = new PreKeyWhisperMessage(wire5);
        byte[] decryptedplaintext5 = decryptCipher5.decrypt(msg5);

        /* Dave -> Alice, Assert */
        assertTrue(alice.containsSession(alicesFriendDave.getAxolotlAddress()));
        assertTrue(dave.containsSession(davesFriendAlice.getAxolotlAddress()));
        assertTrue(alice.loadSession(alicesFriendDave.getAxolotlAddress()).getSessionState().getSessionVersion() == 3);
        assertTrue(alice.loadSession(alicesFriendDave.getAxolotlAddress()).getSessionState().getAliceBaseKey() != null);
        assertTrue(CompareArrays(plaintext5, decryptedplaintext5));

        /* Alice -> Dave, Alice's Phone */
        SessionCipher encryptCipher6 = new SessionCipher(alice, alicesFriendDave.getAxolotlAddress());
        byte[] plaintext6 = "Alice -> Dave #1".getBytes("UTF-8");
        CiphertextMessage message6 = encryptCipher6.encrypt(plaintext6);
        byte[] wire6 = message6.serialize();

        /* Alice -> Dave, Dave's Phone */
        SessionCipher decryptCipher6 = new SessionCipher(dave, davesFriendAlice.getAxolotlAddress());
        WhisperMessage msg6 = new WhisperMessage(wire6);
        byte[] decryptedplaintext6 = decryptCipher6.decrypt(msg6);

        /* Assert */
        assertTrue(CompareArrays(plaintext6, decryptedplaintext6));

        /* Carol -> Dave, Carol's Phone */
        SessionCipher encryptCipher7 = new SessionCipher(carol, carolsFriendDave.getAxolotlAddress());
        byte[] plaintext7 = "Carol -> Dave #2".getBytes("UTF-8");
        CiphertextMessage message7 = encryptCipher7.encrypt(plaintext7);
        byte[] wire7 = message7.serialize();

        /* Carol -> Dave, Dave's Phone */
        SessionCipher decryptCipher7 = new SessionCipher(dave, davesFriendCarol.getAxolotlAddress());
        PreKeyWhisperMessage msg7 = new PreKeyWhisperMessage(wire7);
        byte[] decryptedplaintext7 = decryptCipher7.decrypt(msg7);

        /* Assert */
        assertTrue(CompareArrays(plaintext7, decryptedplaintext7));
    }


    //Alice and Bob send initial messages at same time, then Bob replies, then Alice replies
    public void testAxolotl4() throws UntrustedIdentityException, InvalidKeyException, UnsupportedEncodingException, InvalidVersionException, org.whispersystems.libaxolotl.InvalidMessageException, DuplicateMessageException, LegacyMessageException, InvalidKeyIdException, java.security.InvalidKeyException, NoSessionException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        /* Alice's Phone */
        DummyMe alice = new DummyMe("Alice", getRandomECCKey());
        PreKeyRecord arec1 = alice.getNewPreKey();
        SignedPreKeyRecord asrec1 = alice.getSignedPrekey();
        PreKeyBundle aliceBundle = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec1.getId(), arec1.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());

        /* Bob's Phone */
        DummyMe bob = new DummyMe("Bob", getRandomECCKey());
        PreKeyRecord brec1 = bob.getNewPreKey();
        SignedPreKeyRecord bsrec2 = bob.getSignedPrekey();
        PreKeyBundle bobBundle = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec1.getId(), brec1.getKeyPair().getPublicKey(), bsrec2.getId(), bsrec2.getKeyPair().getPublicKey(), bsrec2.getSignature(), bob.getIdentityKeyPair().getPublicKey());

        DummyFriend aliceFriend = new DummyFriend("Alice", aliceBundle);
        bob.addFriend(aliceFriend);

        /* Alice's Phone */
        DummyFriend bobFriend = new DummyFriend("Bob", bobBundle);
        alice.addFriend(bobFriend);

        SessionBuilder sessionBuilder1 = new SessionBuilder(alice, bobFriend.getAxolotlAddress());
        sessionBuilder1.process(bobFriend.getPreKeyBundle());

        SessionCipher encryptCipher1 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        byte[] plaintext1 = "01234 5678".getBytes("UTF-8");
        CiphertextMessage message1 = encryptCipher1.encrypt(plaintext1);
        byte[] wire1 = message1.serialize();

        /* Bob's Phone */
        SessionBuilder sessionBuilder2 = new SessionBuilder(bob, aliceFriend.getAxolotlAddress());
        sessionBuilder2.process(aliceFriend.getPreKeyBundle());

        SessionCipher encryptCipher2 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        byte[] plaintext2 = "mnbvc zxcvb".getBytes("UTF-8");
        CiphertextMessage message2 = encryptCipher2.encrypt(plaintext2);
        byte[] wire2 = message2.serialize();

        /* Alice's Phone */
        SessionCipher decryptCipher2 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        PreKeyWhisperMessage msg2 = new PreKeyWhisperMessage(wire2);
        byte[] decryptedplaintext2 = decryptCipher2.decrypt(msg2);

        /* Bob's Phone */
        SessionCipher decryptCipher1 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        PreKeyWhisperMessage msg1 = new PreKeyWhisperMessage(wire1);
        byte[] decryptedplaintext1 = decryptCipher1.decrypt(msg1);

        /* Assert */
        assertTrue(alice.containsSession(bobFriend.getAxolotlAddress()));
        assertTrue(bob.containsSession(aliceFriend.getAxolotlAddress()));
        assertTrue(alice.loadSession(bobFriend.getAxolotlAddress()).getSessionState().getSessionVersion() == 3);
        assertTrue(bob.loadSession(aliceFriend.getAxolotlAddress()).getSessionState().getAliceBaseKey() != null);
        assertTrue(CompareArrays(plaintext1, decryptedplaintext1));
        assertTrue(CompareArrays(plaintext2, decryptedplaintext2));

        /* Bob's Phone */
        SessionCipher encryptCipher3 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        byte[] plaintext3 = "lkjhg asdfgh".getBytes("UTF-8");
        CiphertextMessage message3 = encryptCipher3.encrypt(plaintext3);
        byte[] wire3 = message3.serialize();

        SessionCipher encryptCipher4 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        byte[] plaintext4 = "hfjdks laks jdhf".getBytes("UTF-8");
        CiphertextMessage message4 = encryptCipher4.encrypt(plaintext4);
        byte[] wire4 = message4.serialize();

        /* Alice's Phone */
        SessionCipher decryptCipher3 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        WhisperMessage msg3 = new WhisperMessage(wire3);
        byte[] decryptedplaintext3 = decryptCipher3.decrypt(msg3);

        SessionCipher decryptCipher4 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        WhisperMessage msg4 = new WhisperMessage(wire4);
        byte[] decryptedplaintext4 = decryptCipher4.decrypt(msg4);

        /* Assert */
        assertTrue(CompareArrays(plaintext3, decryptedplaintext3));
        assertTrue(CompareArrays(plaintext4, decryptedplaintext4));

        /* Alice's Phone */
        SessionCipher encryptCipher5 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        byte[] plaintext5 = "pqowie eurytu".getBytes("UTF-8");
        CiphertextMessage message5 = encryptCipher5.encrypt(plaintext5);
        byte[] wire5 = message5.serialize();

        /* Bob's Phone */
        SessionCipher decryptCipher5 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        WhisperMessage msg5 = new WhisperMessage(wire5);
        byte[] decryptedplaintext5 = decryptCipher5.decrypt(msg5);

        /* Assert */
        assertTrue(CompareArrays(plaintext5, decryptedplaintext5));
    }

    //Alice and Bob send initial messages at same time, then Alice replies, then Bob replies
    public void testAxolotl5() throws UntrustedIdentityException, InvalidKeyException, UnsupportedEncodingException, InvalidVersionException, org.whispersystems.libaxolotl.InvalidMessageException, DuplicateMessageException, LegacyMessageException, InvalidKeyIdException, java.security.InvalidKeyException, NoSessionException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        /* Alice's Phone */
        DummyMe alice = new DummyMe("Alice", getRandomECCKey());
        PreKeyRecord arec1 = alice.getNewPreKey();
        SignedPreKeyRecord asrec1 = alice.getSignedPrekey();
        PreKeyBundle aliceBundle = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec1.getId(), arec1.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());

        /* Bob's Phone */
        DummyMe bob = new DummyMe("Bob", getRandomECCKey());
        PreKeyRecord brec1 = bob.getNewPreKey();
        SignedPreKeyRecord bsrec2 = bob.getSignedPrekey();
        PreKeyBundle bobBundle = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec1.getId(), brec1.getKeyPair().getPublicKey(), bsrec2.getId(), bsrec2.getKeyPair().getPublicKey(), bsrec2.getSignature(), bob.getIdentityKeyPair().getPublicKey());

        DummyFriend aliceFriend = new DummyFriend("Alice", aliceBundle);
        bob.addFriend(aliceFriend);

        /* Alice's Phone */
        DummyFriend bobFriend = new DummyFriend("Bob", bobBundle);
        alice.addFriend(bobFriend);

        SessionBuilder sessionBuilder1 = new SessionBuilder(alice, bobFriend.getAxolotlAddress());
        sessionBuilder1.process(bobFriend.getPreKeyBundle());

        SessionCipher encryptCipher1 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        byte[] plaintext1 = "01234 5678".getBytes("UTF-8");
        CiphertextMessage message1 = encryptCipher1.encrypt(plaintext1);
        byte[] wire1 = message1.serialize();

        /* Bob's Phone */
        SessionBuilder sessionBuilder2 = new SessionBuilder(bob, aliceFriend.getAxolotlAddress());
        sessionBuilder2.process(aliceFriend.getPreKeyBundle());

        SessionCipher encryptCipher2 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        byte[] plaintext2 = "laksjd tueoe".getBytes("UTF-8");
        CiphertextMessage message2 = encryptCipher2.encrypt(plaintext2);
        byte[] wire2 = message2.serialize();

        /* Alice's Phone */
        SessionCipher decryptCipher2 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        PreKeyWhisperMessage msg2 = new PreKeyWhisperMessage(wire2);
        byte[] decryptedplaintext2 = decryptCipher2.decrypt(msg2);

        /* Bob's Phone */
        SessionCipher decryptCipher1 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        PreKeyWhisperMessage msg1 = new PreKeyWhisperMessage(wire1);
        byte[] decryptedplaintext1 = decryptCipher1.decrypt(msg1);

        /* Assert */
        assertTrue(alice.containsSession(bobFriend.getAxolotlAddress()));
        assertTrue(bob.containsSession(aliceFriend.getAxolotlAddress()));
        assertTrue(alice.loadSession(bobFriend.getAxolotlAddress()).getSessionState().getSessionVersion() == 3);
        assertTrue(bob.loadSession(aliceFriend.getAxolotlAddress()).getSessionState().getAliceBaseKey() != null);
        assertTrue(CompareArrays(plaintext1, decryptedplaintext1));
        assertTrue(CompareArrays(plaintext2, decryptedplaintext2));

        /* Alice's Phone */
        SessionCipher encryptCipher3 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        byte[] plaintext3 = "mznchg ejqlec".getBytes("UTF-8");
        CiphertextMessage message3 = encryptCipher3.encrypt(plaintext3);
        byte[] wire3 = message3.serialize();

        SessionCipher encryptCipher4 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        byte[] plaintext4 = "pqoeud manfgg".getBytes("UTF-8");
        CiphertextMessage message4 = encryptCipher4.encrypt(plaintext4);
        byte[] wire4 = message4.serialize();

        /* Bob's Phone */
        SessionCipher decryptCipher3 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        WhisperMessage msg3 = new WhisperMessage(wire3);
        byte[] decryptedplaintext3 = decryptCipher3.decrypt(msg3);

        SessionCipher decryptCipher4 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        WhisperMessage msg4 = new WhisperMessage(wire4);
        byte[] decryptedplaintext4 = decryptCipher4.decrypt(msg4);

        /* Assert */
        assertTrue(CompareArrays(plaintext3, decryptedplaintext3));
        assertTrue(CompareArrays(plaintext4, decryptedplaintext4));

        /* Bob's Phone */
        SessionCipher encryptCipher5 = new SessionCipher(bob, aliceFriend.getAxolotlAddress());
        byte[] plaintext5 = "mxnvhrk hdekns".getBytes("UTF-8");
        CiphertextMessage message5 = encryptCipher5.encrypt(plaintext5);
        byte[] wire5 = message5.serialize();

        /* Alice's Phone */
        SessionCipher decryptCipher5 = new SessionCipher(alice, bobFriend.getAxolotlAddress());
        WhisperMessage msg5 = new WhisperMessage(wire5);
        byte[] decryptedplaintext5 = decryptCipher5.decrypt(msg5);

        /* Assert */
        assertTrue(CompareArrays(plaintext5, decryptedplaintext5));
    }


    //Alice sends Bob one message
    public void testMessage1() throws IOException, InvalidMessageException, NewMessageVersionException, InvalidKeyException, UntrustedIdentityException, OldMessageVersionException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        /* Alice's Phone */
        byte[] rawBytes = "09876543211234567890".getBytes();
        File tmpFile = File.createTempFile("unit-test", ".tmp");
        FileOutputStream tmpFileOut = new FileOutputStream(tmpFile);
        tmpFileOut.write(rawBytes);
        tmpFileOut.close();
        String tmpFilePath = tmpFile.getAbsolutePath();

        DummyMe alice = new DummyMe("Alice", getRandomECCKey());
        PreKeyRecord arec1 = alice.getNewPreKey();
        SignedPreKeyRecord asrec1 = alice.getSignedPrekey();
        PreKeyBundle alicesBundleForBob = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec1.getId(), arec1.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());

        /* Bob's Phone */
        DummyMe bob = new DummyMe("Bob", getRandomECCKey());
        PreKeyRecord brec1 = bob.getNewPreKey();
        SignedPreKeyRecord bsrec1 = bob.getSignedPrekey();
        PreKeyBundle bobsBundleForAlice = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec1.getId(), brec1.getKeyPair().getPublicKey(), bsrec1.getId(), bsrec1.getKeyPair().getPublicKey(), bsrec1.getSignature(), bob.getIdentityKeyPair().getPublicKey());

        DummyFriend bobsFriendAlice = new DummyFriend("Alice", alicesBundleForBob);
        bob.addFriend(bobsFriendAlice);

        /* Alice's Phone */
        DummyFriend alicesFriendBob = new DummyFriend("Bob", bobsBundleForAlice);
        alice.addFriend(alicesFriendBob);

        try {
            OutgoingMessage m1 = new OutgoingMessage(-1, tmpFilePath, "", false, alicesFriendBob, alice);
            byte[] encoded = m1.Encode();

            /* Bob's Phone */
            IncomingMessage m2 = IncomingMessage.Decode(bob, "ABCDEF", encoded);

            /* Assert */
            assertEquals(m1.message, m2.message);
            assertEquals(m1.privateMessage, m2.privateMessage);
            assertTrue(CompareArrays(rawBytes, m2.imgBytes));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmpFile.delete();
        }
    }

    //Alice sends Bob two messages, and Bob replies
    public void testMessage2() throws IOException, InvalidMessageException, NewMessageVersionException, InvalidKeyException, UntrustedIdentityException, OldMessageVersionException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        byte[] rawBytes1 = "12345678900987654321".getBytes();
        File tmpFile1 = File.createTempFile("unit-test", ".tmp");
        FileOutputStream tmpFileOut1 = new FileOutputStream(tmpFile1);
        tmpFileOut1.write(rawBytes1);
        tmpFileOut1.close();
        String tmpFilePath1 = tmpFile1.getAbsolutePath();

        byte[] rawBytes2 = "qwertyuioppoiuytrewq".getBytes();
        File tmpFile2 = File.createTempFile("unit-test", ".tmp");
        FileOutputStream tmpFileOut2 = new FileOutputStream(tmpFile2);
        tmpFileOut2.write(rawBytes2);
        tmpFileOut2.close();
        String tmpFilePath2 = tmpFile2.getAbsolutePath();

        byte[] rawBytes3 = "asdfghjkllkjhgfdsa".getBytes();
        File tmpFile3 = File.createTempFile("unit-test", ".tmp");
        FileOutputStream tmpFileOut3 = new FileOutputStream(tmpFile3);
        tmpFileOut3.write(rawBytes3);
        tmpFileOut3.close();
        String tmpFilePath3 = tmpFile3.getAbsolutePath();

        /* Alice's Phone */
        DummyMe alice = new DummyMe("Alice", getRandomECCKey());
        PreKeyRecord arec1 = alice.getNewPreKey();
        SignedPreKeyRecord asrec1 = alice.getSignedPrekey();
        PreKeyBundle alicesBundleForBob = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec1.getId(), arec1.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());

        /* Bob's Phone */
        DummyMe bob = new DummyMe("Bob", getRandomECCKey());
        PreKeyRecord brec1 = bob.getNewPreKey();
        SignedPreKeyRecord bsrec1 = bob.getSignedPrekey();
        PreKeyBundle bobsBundleForAlice = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec1.getId(), brec1.getKeyPair().getPublicKey(), bsrec1.getId(), bsrec1.getKeyPair().getPublicKey(), bsrec1.getSignature(), bob.getIdentityKeyPair().getPublicKey());

        DummyFriend bobsFriendAlice = new DummyFriend("Alice", alicesBundleForBob);
        bob.addFriend(bobsFriendAlice);

        /* Alice's Phone */
        DummyFriend alicesFriendBob = new DummyFriend("Bob", bobsBundleForAlice);
        alice.addFriend(alicesFriendBob);

        try {
            OutgoingMessage om1 = new OutgoingMessage(-1, tmpFilePath1, "", false, alicesFriendBob, alice);
            byte[] encoded1 = om1.Encode();

            OutgoingMessage om2 = new OutgoingMessage(-2, tmpFilePath2, "ABCDE FGHIJ", true, alicesFriendBob, alice);
            byte[] encoded2 = om2.Encode();

            /* Bob's Phone */
            IncomingMessage im1 = IncomingMessage.Decode(bob, "ABCDEF", encoded1);

            /* Assert */
            assertEquals(om1.message, im1.message);
            assertEquals(om1.privateMessage, im1.privateMessage);
            assertTrue(CompareArrays(rawBytes1, im1.imgBytes));

            /* Bob's Phone */
            IncomingMessage im2 = IncomingMessage.Decode(bob, "ABCDEF01", encoded2);

            /* Assert */
            assertEquals(om2.message, im2.message);
            assertEquals(om2.privateMessage, im2.privateMessage);
            assertTrue(CompareArrays(rawBytes2, im2.imgBytes));

            /* Bob's Phone */
            OutgoingMessage om3 = new OutgoingMessage(-3, tmpFilePath3, "Three", false, bobsFriendAlice, bob);
            byte[] encoded3 = om3.Encode();

            /* Alice's Phone */
            IncomingMessage im3 = IncomingMessage.Decode(alice, "ABCDEF02", encoded3);

            /* Assert */
            assertEquals(om3.message, im3.message);
            assertEquals(om3.privateMessage, im3.privateMessage);
            assertTrue(CompareArrays(rawBytes3, im3.imgBytes));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmpFile1.delete();
            tmpFile2.delete();
            tmpFile3.delete();
        }
    }


    //Simultaneous Initiation, then Alice sends another message
    public void testMessage3() throws IOException, InvalidMessageException, NewMessageVersionException, InvalidKeyException, UntrustedIdentityException, OldMessageVersionException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        byte[] rawBytes1 = "09876543211234567890".getBytes();
        File tmpFile1 = File.createTempFile("unit-test", ".tmp");
        FileOutputStream tmpFileOut1 = new FileOutputStream(tmpFile1);
        tmpFileOut1.write(rawBytes1);
        tmpFileOut1.close();
        String tmpFilePath1 = tmpFile1.getAbsolutePath();

        byte[] rawBytes2 = "qwertyuioppoiuytrewq".getBytes();
        File tmpFile2 = File.createTempFile("unit-test", ".tmp");
        FileOutputStream tmpFileOut2 = new FileOutputStream(tmpFile2);
        tmpFileOut2.write(rawBytes2);
        tmpFileOut2.close();
        String tmpFilePath2 = tmpFile2.getAbsolutePath();

        byte[] rawBytes3 = "asdfghjkllkjhgfdsa".getBytes();
        File tmpFile3 = File.createTempFile("unit-test", ".tmp");
        FileOutputStream tmpFileOut3 = new FileOutputStream(tmpFile3);
        tmpFileOut3.write(rawBytes3);
        tmpFileOut3.close();
        String tmpFilePath3 = tmpFile3.getAbsolutePath();

        /* Alice's Phone */
        DummyMe alice = new DummyMe("Alice", getRandomECCKey());
        PreKeyRecord arec1 = alice.getNewPreKey();
        SignedPreKeyRecord asrec1 = alice.getSignedPrekey();
        PreKeyBundle alicesBundleForBob = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec1.getId(), arec1.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());

        /* Bob's Phone */
        DummyMe bob = new DummyMe("Bob", getRandomECCKey());
        PreKeyRecord brec1 = bob.getNewPreKey();
        SignedPreKeyRecord bsrec1 = bob.getSignedPrekey();
        PreKeyBundle bobsBundleForAlice = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec1.getId(), brec1.getKeyPair().getPublicKey(), bsrec1.getId(), bsrec1.getKeyPair().getPublicKey(), bsrec1.getSignature(), bob.getIdentityKeyPair().getPublicKey());

        DummyFriend bobsFriendAlice = new DummyFriend("Alice", alicesBundleForBob);
        bob.addFriend(bobsFriendAlice);

        /* Alice's Phone */
        DummyFriend alicesFriendBob = new DummyFriend("Bob", bobsBundleForAlice);
        alice.addFriend(alicesFriendBob);

        try {
            OutgoingMessage om1 = new OutgoingMessage(-1, tmpFilePath1, "", false, alicesFriendBob, alice);
            byte[] encoded1 = om1.Encode();

            /* Bob's Phone */
            OutgoingMessage om2 = new OutgoingMessage(-2, tmpFilePath2, "ABCDE FGHIJ", true, bobsFriendAlice, bob);
            byte[] encoded2 = om2.Encode();

            IncomingMessage im1 = IncomingMessage.Decode(bob, "ABCDEF", encoded1);

            /* Assert */
            assertEquals(om1.message, im1.message);
            assertEquals(om1.privateMessage, im1.privateMessage);
            assertTrue(CompareArrays(rawBytes1, im1.imgBytes));

            /* Alice's Phone */
            IncomingMessage im2 = IncomingMessage.Decode(alice, "ABCDEF01", encoded2);

            /* Assert */
            assertEquals(om2.message, im2.message);
            assertEquals(om2.privateMessage, im2.privateMessage);
            assertTrue(CompareArrays(rawBytes2, im2.imgBytes));

            /* Alice's Phone */
            OutgoingMessage om3 = new OutgoingMessage(-3, tmpFilePath3, "jfktbc", false, alicesFriendBob, alice);
            byte[] encoded3 = om3.Encode();

            /* Bob's Phone */
            IncomingMessage im3 = IncomingMessage.Decode(bob, "ABCDEF02", encoded3);

            /* Assert */
            assertEquals(om3.message, im3.message);
            assertEquals(om3.privateMessage, im3.privateMessage);
            assertTrue(CompareArrays(rawBytes3, im3.imgBytes));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmpFile1.delete();
            tmpFile2.delete();
            tmpFile3.delete();
        }
    }


    //Simultaneous Initiation, then Bob sends another message
    public void testMessage4() throws IOException, InvalidMessageException, NewMessageVersionException, InvalidKeyException, UntrustedIdentityException, OldMessageVersionException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        byte[] rawBytes1 = "12345678900987654321".getBytes();
        File tmpFile1 = File.createTempFile("unit-test", ".tmp");
        FileOutputStream tmpFileOut1 = new FileOutputStream(tmpFile1);
        tmpFileOut1.write(rawBytes1);
        tmpFileOut1.close();
        String tmpFilePath1 = tmpFile1.getAbsolutePath();

        byte[] rawBytes2 = "qwertyuioppoiuytrewq".getBytes();
        File tmpFile2 = File.createTempFile("unit-test", ".tmp");
        FileOutputStream tmpFileOut2 = new FileOutputStream(tmpFile2);
        tmpFileOut2.write(rawBytes2);
        tmpFileOut2.close();
        String tmpFilePath2 = tmpFile2.getAbsolutePath();

        byte[] rawBytes3 = "asdfghjkllkjhgfdsa".getBytes();
        File tmpFile3 = File.createTempFile("unit-test", ".tmp");
        FileOutputStream tmpFileOut3 = new FileOutputStream(tmpFile3);
        tmpFileOut3.write(rawBytes3);
        tmpFileOut3.close();
        String tmpFilePath3 = tmpFile3.getAbsolutePath();

        /* Alice's Phone */
        DummyMe alice = new DummyMe("Alice", getRandomECCKey());
        PreKeyRecord arec1 = alice.getNewPreKey();
        SignedPreKeyRecord asrec1 = alice.getSignedPrekey();
        PreKeyBundle alicesBundleForBob = new PreKeyBundle(alice.getLocalRegistrationId(), alice.getLocalDeviceId(), arec1.getId(), arec1.getKeyPair().getPublicKey(), asrec1.getId(), asrec1.getKeyPair().getPublicKey(), asrec1.getSignature(), alice.getIdentityKeyPair().getPublicKey());

        /* Bob's Phone */
        DummyMe bob = new DummyMe("Bob", getRandomECCKey());
        PreKeyRecord brec1 = bob.getNewPreKey();
        SignedPreKeyRecord bsrec1 = bob.getSignedPrekey();
        PreKeyBundle bobsBundleForAlice = new PreKeyBundle(bob.getLocalRegistrationId(), bob.getLocalDeviceId(), brec1.getId(), brec1.getKeyPair().getPublicKey(), bsrec1.getId(), bsrec1.getKeyPair().getPublicKey(), bsrec1.getSignature(), bob.getIdentityKeyPair().getPublicKey());

        DummyFriend bobsFriendAlice = new DummyFriend("Alice", alicesBundleForBob);
        bob.addFriend(bobsFriendAlice);

        /* Alice's Phone */
        DummyFriend alicesFriendBob = new DummyFriend("Bob", bobsBundleForAlice);
        alice.addFriend(alicesFriendBob);

        try {
            OutgoingMessage om1 = new OutgoingMessage(-1, tmpFilePath1, "", false, alicesFriendBob, alice);
            byte[] encoded1 = om1.Encode();

            /* Bob's Phone */
            OutgoingMessage om2 = new OutgoingMessage(-2, tmpFilePath2, "ABCDE FGHIJ", true, bobsFriendAlice, bob);
            byte[] encoded2 = om2.Encode();

            IncomingMessage im1 = IncomingMessage.Decode(bob, "ABCDEF", encoded1);

            /* Assert */
            assertEquals(om1.message, im1.message);
            assertEquals(om1.privateMessage, im1.privateMessage);
            assertTrue(CompareArrays(rawBytes1, im1.imgBytes));

            /* Alice's Phone */
            IncomingMessage im2 = IncomingMessage.Decode(alice, "ABCDEF01", encoded2);

            /* Assert */
            assertEquals(om2.message, im2.message);
            assertEquals(om2.privateMessage, im2.privateMessage);
            assertTrue(CompareArrays(rawBytes2, im2.imgBytes));

            /* Bob's Phone */
            OutgoingMessage om3 = new OutgoingMessage(-3, tmpFilePath3, "mcbdkfh", false, bobsFriendAlice, bob);
            byte[] encoded3 = om3.Encode();

            /* Alice's Phone */
            IncomingMessage im3 = IncomingMessage.Decode(alice, "ABCDEF02", encoded3);

            /* Assert */
            assertEquals(om3.message, im3.message);
            assertEquals(om3.privateMessage, im3.privateMessage);
            assertTrue(CompareArrays(rawBytes3, im3.imgBytes));
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmpFile1.delete();
            tmpFile2.delete();
            tmpFile3.delete();
        }
    }
}

package invalid.showme.model;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.log.CustomLogger;

import info.guardianproject.netcipher.proxy.OrbotHelper;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;
import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.ecc.Curve;
import org.whispersystems.libaxolotl.ecc.ECKeyPair;
import org.whispersystems.libaxolotl.state.AxolotlStore;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SessionRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;
import org.whispersystems.libaxolotl.util.KeyHelper;

import java.io.FileNotFoundException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.greenrobot.event.EventBus;
import invalid.showme.R;
import invalid.showme.exceptions.DatabaseException;
import invalid.showme.exceptions.InvalidMessageException;
import invalid.showme.exceptions.NewMessageVersionException;
import invalid.showme.exceptions.OldMessageVersionException;
import invalid.showme.exceptions.ServerException;
import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.layoutobjects.DataAdaptorRefreshEvent;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.db.DraftTableContract;
import invalid.showme.model.db.IdentityTableContract;
import invalid.showme.model.db.PhotoTableContract;
import invalid.showme.model.db.SentTableContract;
import invalid.showme.model.message.IncomingMessage;
import invalid.showme.model.photo.DraftPhoto;
import invalid.showme.model.photo.FileCleanupJob;
import invalid.showme.model.photo.ReceivedPhoto;
import invalid.showme.model.photo.SentPhoto;
import invalid.showme.model.selfstate.AxolotlSessionStore;
import invalid.showme.model.selfstate.PreKeyStateStore;
import invalid.showme.model.server.ServerConfiguration;
import invalid.showme.model.server.ServerFailureEvent;
import invalid.showme.model.server.ServerInterface;
import invalid.showme.model.server.ServerRequestJob;
import invalid.showme.model.server.clientcert.ClientCertificate;
import invalid.showme.model.server.clientcert.ClientCertificateStore;
import invalid.showme.model.server.result.GetResult;
import invalid.showme.model.server.result.PollResult;
import invalid.showme.model.server.result.PutResult;
import invalid.showme.model.server.result.ServerResultEvent;
import invalid.showme.model.server.result.StatusResult;
import invalid.showme.services.ACRA.TorACRASenderFactory;
import invalid.showme.services.NotificationWrangler;
import invalid.showme.util.IOUtil;
import invalid.showme.util.JobPriorityUtil;
import invalid.showme.util.TimeUtil;

import static org.acra.ReportField.*;

@ReportsCrashes(formUri = ServerConfiguration.BUG_URL, httpMethod = HttpSender.Method.PUT, reportType = HttpSender.Type.JSON,
        resCertificate = R.raw.acra_cert, reportSenderFactoryClasses = {TorACRASenderFactory.class },
        customReportContent={ANDROID_VERSION, APP_VERSION_CODE, APP_VERSION_NAME, AVAILABLE_MEM_SIZE, BUILD_CONFIG, CRASH_CONFIGURATION, CUSTOM_DATA, DEVICE_FEATURES,
                DISPLAY, DUMPSYS_MEMINFO, ENVIRONMENT, FILE_PATH, INITIAL_CONFIGURATION, LOGCAT, PACKAGE_NAME, PHONE_MODEL, PRODUCT, REPORT_ID, SHARED_PREFERENCES,
                STACK_TRACE, TOTAL_MEM_SIZE, USER_APP_START_DATE, USER_CRASH_DATE, INSTALLATION_ID})
                //Try not to get unique identifiers, exclude BUILD (serial number), USER_EMAIL
                //Include INSTALLATION_ID - unique ID generated randomly and only correlatable if attacker has user's phone already
public class UserProfile extends Application implements IMe, AxolotlStore {
    static {
        Provider firstProvider = Security.getProviders()[0];
        if(!(firstProvider instanceof org.spongycastle.jce.provider.BouncyCastleProvider))
            Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 0);
    }

    private final String TAG = "UserProfile";

    private JobManager jobManager;

    //TODO: Document 3 things key used for, and why that's safe.
    private String displayName;
    private ECKeyPair identityKey;
    private KeyFingerprint keyFP;
    private int RegistrationID;

    private List<IFriend> friends;
    private List<DraftPhoto> drafts;
    private List<SentPhoto> sent;

    private ClientCertificateStore clientCerts;

    private PreKeyStateStore preKeyStore;
    private AxolotlSessionStore sessionStore;

    public boolean Initialized() { return this.identityKey != null; }

    private void setDisplayName(String dn) { this.displayName = dn; }
    public String getDisplayName() { return this.displayName; }

    private void setKeyPair(ECKeyPair identityKey)
    {
        this.identityKey = identityKey;
        this.keyFP = new KeyFingerprint(this.identityKey.getPublicKey());
        ServerInterface.setIdentity(identityKey);
        this.maybeSetNewClientCertificate();
    }
    public ECKeyPair getKeyPair() { return this.identityKey; }

    public KeyFingerprint getFingerprint() { return keyFP; }

    public void createSelf(String displayName, ECKeyPair kp)
    {
        this.setDisplayName(displayName);
        this.setKeyPair(kp);
        this.preKeyStore = new PreKeyStateStore(this, 1, 1);
        this.sessionStore = new AxolotlSessionStore(this);
        this.RegistrationID = KeyHelper.generateRegistrationId(false);
    }

    public boolean Initialize()
    {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        Cursor identities = DBHelper.getIdentities(this);
        if(identities.getCount() == 0) {
            return false;
        }
        else if(identities.getCount() > 1) {
            String msg = "Somehow got >1 identities into table. Wiping everything...";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            SQLiteDatabase db = DBHelper.getInstance(getApplicationContext()).getWritableDatabase();
            DBHelper.wipeEverything(db);
            return false;
        }
        else {
            String displayName = identities.getString(identities.getColumnIndexOrThrow(IdentityTableContract.COLUMN_NAME_DISPLAYNAME));
            byte[] publicKey = identities.getBlob(identities.getColumnIndexOrThrow(IdentityTableContract.COLUMN_NAME_PUBKEY));
            byte[] privateKey = identities.getBlob(identities.getColumnIndexOrThrow(IdentityTableContract.COLUMN_NAME_PRIVKEY));
            int pkCounter = identities.getInt(identities.getColumnIndexOrThrow(IdentityTableContract.COLUMN_NAME_PREKEYCOUNTER));
            int spkCounter = identities.getInt(identities.getColumnIndexOrThrow(IdentityTableContract.COLUMN_NAME_SIGNEDPREKEYCOUNTER));
            int regID = identities.getInt(identities.getColumnIndexOrThrow(IdentityTableContract.COLUMN_NAME_REGISTRATIONID));

            ECKeyPair idKey;
            try {
                idKey = new ECKeyPair(Curve.decodePoint(publicKey, 0), Curve.decodePrivatePoint(privateKey));
            } catch (InvalidKeyException e) {
                ACRA.getErrorReporter().handleException(e);
                return false;
            }

            this.RegistrationID = regID;
            this.setDisplayName(displayName);
            this.setKeyPair(idKey);
            this.initializeFriends();
            this.initializeDrafts();
            this.initializeSent();
            this.preKeyStore = new PreKeyStateStore(this, pkCounter, spkCounter);
            this.sessionStore = new AxolotlSessionStore(this);
        }
        identities.close();
        this.getJobManager().addJobInBackground(new FileCleanupJob());
        return true;
    }

    public void maybeSetNewClientCertificate()
    {
        if(this.clientCerts == null) {
            Cursor certs = DBHelper.getCertificates(this);
            this.clientCerts = new ClientCertificateStore(certs);
            certs.close();
        }
        if(this.clientCerts.numReady() == 0) {
            if(ServerInterface.clientCertificateDirty())
                this.clientCerts.generateNow();
            this.clientCerts.generateLater(this);
        } else if(this.clientCerts.numReady() < 5) {
            this.clientCerts.generateLater(this);
        }
        if(ServerInterface.clientCertificateDirty()) {
            try {
                ClientCertificate cert = this.clientCerts.get();
                ServerInterface.setCertificate(cert);
            } catch (Exception e) {
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    //TODO: Do something with these numbers to make them random. Currently it leaks how many contacts you have to each new contact
    public PreKeyRecord getNewPreKey()
    {
        return this.preKeyStore.getNewPreKey();
    }
    public SignedPreKeyRecord getSignedPrekey()
    {
        try {
            return this.preKeyStore.getSignedPreKey(this.getIdentityKeyPair());
        } catch(InvalidKeyException e) {
            ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow own identity key invalid?", e));
        }
        return null;
    }

    public boolean updatePreKeyCounters()
    {
        ContentValues values = new ContentValues();
        values.put(IdentityTableContract.COLUMN_NAME_PREKEYCOUNTER, this.preKeyStore.PreKeyCounter);
        values.put(IdentityTableContract.COLUMN_NAME_SIGNEDPREKEYCOUNTER, this.preKeyStore.SignedPreKeyCounter);

        DBHelper dbHelper = DBHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = IdentityTableContract._ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(1) }; //assuming identity 1
        int rowsAffected;
        try {
            db.beginTransaction();
            rowsAffected = db.update(IdentityTableContract.TABLE_NAME, values, selection, selectionArgs);
            if (rowsAffected != 1) {
                String msg = "Could not update Pre Key Counter in database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            } else
                db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return rowsAffected == 1;
    }

    @Override
    public boolean saveToDatabase(Context context)
    {
        long id = -1;
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(IdentityTableContract.COLUMN_NAME_DISPLAYNAME, this.displayName);
        values.put(IdentityTableContract.COLUMN_NAME_PUBKEY, this.identityKey.getPublicKey().serialize());
        values.put(IdentityTableContract.COLUMN_NAME_PRIVKEY, this.identityKey.getPrivateKey().serialize());
        values.put(IdentityTableContract.COLUMN_NAME_PREKEYCOUNTER, this.preKeyStore.PreKeyCounter);
        values.put(IdentityTableContract.COLUMN_NAME_SIGNEDPREKEYCOUNTER, this.preKeyStore.SignedPreKeyCounter);
        values.put(IdentityTableContract.COLUMN_NAME_REGISTRATIONID, this.RegistrationID);

        id = db.insert(IdentityTableContract.TABLE_NAME,
                "null",
                values);
        if(id < 0) {
            String msg = "Could not save UserProfile to database - no ID returned.";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().handleException(new DatabaseException(msg));
        }
        else
            db.setTransactionSuccessful();
        db.endTransaction();
        return id > 0;
    }

    public List<IFriend> getFriends() { return this.friends; }
    public void initializeFriends()
    {
        this.friends = new ArrayList<>();
        Cursor friends = DBHelper.getFriends(this);
        while (friends.moveToNext()) {
            try {
                IFriend f = Friend.FromDatabase(friends);
                this.friends.add(f);
            } catch(InvalidKeyException e) {
                //TODO: Also raise error to user/delete friend from database
                ACRA.getErrorReporter().handleException(e);
            }
        }
        friends.close();

        for(IFriend f : this.friends)
        {
            Cursor photos = DBHelper.getPhotosForFriend(this, f.getID());
            while(photos.moveToNext()) {
                try {
                    ReceivedPhoto p = ReceivedPhoto.FromDatabase(this, photos, true);
                    if(!p.ShouldBeDeleted()) {
                        f.addPhoto(p);
                    } else if(p.ShouldBeDeleted()) {
                        Log.d(TAG, "Deleting photo ID " + p.getID());
                        p.DeleteFiles();
                        if(!ReceivedPhoto.DeleteFromDatabase(DBHelper.getInstance(this), p.getID())) {
                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch(FileNotFoundException e) {
                    String msg = "files associated with photo from friend ID " + (Long.valueOf(f.getID())).toString() + " missing. deleting DB entry." ;
                    Log.e(TAG, msg);
                    ACRA.getErrorReporter().handleException(new DatabaseException(msg));
                    long id  = photos.getLong(photos.getColumnIndexOrThrow(PhotoTableContract._ID));
                    if(!ReceivedPhoto.DeleteFromDatabase(DBHelper.getInstance(this), id)) {
                        Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            photos.close();
        }
    }
    public void addFriend(IFriend f) {
        this.getFriends().add(f);
        this.saveIdentity(f.getAxolotlAddress().getName(), new IdentityKey(f.getPublicKey()));
    }
    public IFriend findFriend(KeyFingerprint fpr)
    {
        for(int i=0; i<this.friends.size(); i++)
            if(this.friends.get(i).getFingerprint().equals(fpr))
                return this.friends.get(i);
        return null;
    }
    @Override
    public IFriend findFriend(long id)
    {
        for(int i=0; i<this.friends.size(); i++)
            if(this.friends.get(i).getID() == id)
                return this.friends.get(i);
        return null;
    }
    public void deleteFriend(IFriend friend){
        this.friends.remove(friend);
        for(ReceivedPhoto p : friend.getPhotos()) {
            p.DeleteFiles();
            if(!ReceivedPhoto.DeleteFromDatabase(DBHelper.getInstance(this), p.getID())) {
                Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public ReceivedPhoto findPhoto(long id) {
        for(int i=0; i<this.friends.size(); i++)
        {
            IFriend f = this.friends.get(i);
            for(ReceivedPhoto p : f.getPhotos())
                if(p.getID() == id)
                    return p;
        }
        return null;
    }

    public List<SentPhoto> getSent() { return this.sent; }
    private SentPhoto findSent(String MessageID) {
        for(SentPhoto p : this.sent)
            if(Objects.equals(p.MessageID, MessageID))
                return p;
        return null;
    }
    public SentPhoto findSent(long id) {
        for(SentPhoto p : this.sent)
            if(p.getID() == id)
                return p;
        return null;
    }
    public void initializeSent()
    {
        this.sent = new ArrayList<>();
        Cursor sentrows = DBHelper.getSent(this);
        while (sentrows.moveToNext()) {
            try {
                SentPhoto s = SentPhoto.FromDatabase(this, sentrows, true);
                this.sent.add(0, s);
            } catch (FileNotFoundException e) {
                String msg = "files associated with sent photo missing. deleting DB entry." ;
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
                long id  = sentrows.getLong(sentrows.getColumnIndexOrThrow(SentTableContract._ID));
                if(!SentPhoto.DeleteFromDatabase(DBHelper.getInstance(this), id)) {
                    Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                }
            }
        }
        sentrows.close();
    }

    public List<DraftPhoto> getDrafts() { return this.drafts; }
    public DraftPhoto findDraft(long id) {
        for(DraftPhoto p : this.drafts)
            if(p.getID() == id)
                return p;
        return null;
    }
    public void initializeDrafts()
    {
        this.drafts = new ArrayList<>();
        Cursor drafts = DBHelper.getDrafts(this);
        while (drafts.moveToNext()) {
            try {
                DraftPhoto d = DraftPhoto.FromDatabase(this, drafts, true);
                this.drafts.add(0, d);
            } catch (FileNotFoundException e) {
                String msg = "files associated with draft photo missing. deleting DB entry." ;
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
                long id  = drafts.getLong(drafts.getColumnIndexOrThrow(DraftTableContract._ID));
                if(!DraftPhoto.DeleteFromDatabase(DBHelper.getInstance(this), id)) {
                    Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                }
            }
        }
        drafts.close();
    }

    public JobManager getJobManager() { return this.jobManager; }

    @Override
    public void onCreate() {
        super.onCreate();

        this.identityKey = null;
        this.displayName = null;
        this.keyFP = null;
        this.friends= null;
        this.clientCerts = null;

        Configuration config = new Configuration.Builder(this)
            .customLogger(new CustomLogger() {
                private static final String TAG = "JOBS";

                @Override
                public boolean isDebugEnabled() {
                    return false;
                }

                @Override
                public void d(String text, Object... args) {
                    Log.d(TAG, String.format(text, args));
                }

                @Override
                public void e(Throwable t, String text, Object... args) {
                    Log.e(TAG, String.format(text, args), t);
                }

                @Override
                public void e(String text, Object... args) {
                    Log.e(TAG, String.format(text, args));
                }
            })
            //Chosen to be small due to OOM errors
            .minConsumerCount(1)
            .maxConsumerCount(2)
            .loadFactor(15)
            .consumerKeepAlive(30)
            .build();
        this.jobManager = new JobManager(this, config);

        EventBus.getDefault().register(this);

        ACRA.init(this);
        ACRA.getErrorReporter().putCustomData("builddate", IOUtil.AppBuildTime(getPackageManager(), getPackageName()));
        ACRA.getErrorReporter().setEnabled(ServerConfiguration.REPORT_BUGS);
    }

    public void onEvent(ServerFailureEvent event)
    {
        String msg = "Response from Server Job failure: " + event.Type + " - " + event.Message;
        Log.w(TAG, msg);
        ACRA.getErrorReporter().handleException(new ServerException(msg));
        Toast.makeText(getApplicationContext(), event.Message, Toast.LENGTH_LONG).show();
    }

    public void onEvent(ServerResultEvent event)
    {
        switch(event.Type) {
            case PollJob:
                PollResult poll = (PollResult)event;
                if(!event.Success && event.ReturnCode == 406) {
                    Log.w(TAG, "Response from Poll Job not registered. Re-registering..." );
                    this.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.RegisterJob));
                    this.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.TokenJob));
                }
                else {
                    for (int i = 0; i < poll.IDs.size(); i++) {
                        //TODO: See if have queued job to retrieve photo already
                        this.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.GetJob, poll.IDs.get(i)));
                    }
                }
                break;
            case StatusJob:
                StatusResult status = (StatusResult)event;
                if(!event.Success && event.ReturnCode == 406) {
                    Log.w(TAG, "Response from Status Job not registered. Re-registering..." );
                    this.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.RegisterJob));
                    this.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.TokenJob));
                }
                else {
                    for(String s : status.Statuses.keySet()) {
                        SentPhoto p = this.findSent(s);
                        if(p == null) {
                            ACRA.getErrorReporter().handleException(new StrangeUsageException("A sent photo deleted between requesting status and recieving it..."));
                        } else {
                            if(status.Statuses.get(s).equals("Missing"))
                                p.Status = SentPhoto.SentPhotoStatus.Received;
                            else if (status.Statuses.get(s).equals("Present"))
                                p.Status = SentPhoto.SentPhotoStatus.Sent;
                            else
                                ACRA.getErrorReporter().handleException(new StrangeUsageException("Got unexpected status from server: " + status.Statuses.get(s)));
                            p.saveToDatabase(DBHelper.getInstance(this));//Do no handle failure
                        }
                    }
                    EventBus.getDefault().post(new DataAdaptorRefreshEvent());
                }
                break;
            case PutJob:
                PutResult put = (PutResult)event;
                if(put.Success && put.SentPhotoRecordID != -1) {
                    SentPhoto sentPhoto = this.findSent(put.SentPhotoRecordID);
                    if (!Objects.equals(put.MessageIDCalculatedLocally, put.MessageIDCalculatedRemotely)) {
                        ACRA.getErrorReporter().handleException(new StrangeUsageException("local and remote messageids different: " + put.MessageIDCalculatedLocally + " " + put.MessageIDCalculatedRemotely));
                        sentPhoto.Status = SentPhoto.SentPhotoStatus.Error;
                    } else {
                        sentPhoto.Status = SentPhoto.SentPhotoStatus.Sent;
                    }
                    sentPhoto.saveToDatabase(DBHelper.getInstance(this));//Do not handle failure
                    EventBus.getDefault().post(new DataAdaptorRefreshEvent());
                }
                break;
            case GetJob:
                GetResult g = (GetResult)event;
                if(g.Success) {
                    try {
                        IncomingMessage m = g.toMessage(this);
                        if (m.sender == null) {
                            String msg = "Received message from someone not friends with.";
                            Log.e(TAG, msg);
                            ACRA.getErrorReporter().handleException(new StrangeUsageException(msg));
                            throw new InvalidMessageException();
                        }

                        ReceivedPhoto photo = new ReceivedPhoto(getApplicationContext(), g.ID, m.sender.getID(), m.message, m.privateMessage, false, TimeUtil.GetNow(), 0, m.imgBytes);
                        //Test if received photo already and just hadn't deleted it from server
                        if (!m.sender.hasPhoto(photo.MessageID)) {
                            if (!photo.saveToDatabase(DBHelper.getInstance(this))) {
                                Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                            } else {
                                m.sender.addPhoto(photo);
                                NotificationWrangler.AddSender(m.sender.getID(), m.sender.getDisplayName(), photo.PrivatePhoto);
                                NotificationWrangler.ShowOrUpdateNotification(getApplicationContext());
                                EventBus.getDefault().post(new DataAdaptorRefreshEvent());
                            }
                        }
                        this.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.DeleteJob, g.ID));
                    } catch (NewMessageVersionException e) {
                        String msg = "incoming message raised NewMessageVersionException.";
                        Log.e(TAG, msg);
                        ACRA.getErrorReporter().handleException(new StrangeUsageException(msg, e));
                        Toast.makeText(getApplicationContext(), "Recieved a message that was sent from a newer version of the application. Please upgrade to support recieving these messages.", Toast.LENGTH_LONG).show();
                    } catch (OldMessageVersionException e) {
                        String msg = "incoming message raised OldMessageVersionException.";
                        Log.e(TAG, msg);
                        ACRA.getErrorReporter().handleException(new StrangeUsageException(msg, e));
                        Toast.makeText(getApplicationContext(), "Recieved a message that was sent from an older, unsupported version of the application. Please tell your friend to upgrade.", Toast.LENGTH_LONG).show();
                    } catch (InvalidMessageException e) {
                        String msg = "incoming message raised InvalidMessageException.";
                        Log.e(TAG, msg);
                        ACRA.getErrorReporter().handleException(new StrangeUsageException(msg, e));
                    }
                }

                break;
        }
    }

    //========================================================================================
    //Axolotl Store Interface
    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        return new IdentityKeyPair(
                new IdentityKey(this.getKeyPair().getPublicKey()),
                this.getKeyPair().getPrivateKey());
    }

    @Override
    public int getLocalRegistrationId() {
        return this.RegistrationID;
    }

    public int getLocalDeviceId() {
        //TODO maybe someday might support multiple devices.
        return 1;
    }

    @Override
    public void saveIdentity(String s, IdentityKey identityKey) {
        for(IFriend f: this.getFriends()) {
            if(f.getAxolotlAddress().getName().equals(s) && f.getPublicKey().equals(identityKey.getPublicKey()))
                return;
        }
        ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow asked to save Identity did not already know about!"));
    }

    @Override
    public boolean isTrustedIdentity(String s, IdentityKey identityKey) {
        for(IFriend f : this.getFriends()) {
            if(f.getAxolotlAddress().getName().equals(s) && f.getPublicKey().equals(identityKey.getPublicKey()))
                return true;
        }
        ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow got untrusted identity!"));
        return false;
    }

    //--------------------------------------------------------------------------------
    @Override
    public PreKeyRecord loadPreKey(int i) throws InvalidKeyIdException {
        return this.preKeyStore.loadPreKey(i);
    }

    @Override
    public void storePreKey(int i, PreKeyRecord preKeyRecord) {
        this.preKeyStore.storePreKey(i, preKeyRecord);
    }

    @Override
    public boolean containsPreKey(int i) {
        return this.preKeyStore.containsPreKey(i);
    }

    @Override
    public void removePreKey(int i) {
        this.preKeyStore.removePreKey(i);
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int i) throws InvalidKeyIdException {
        return this.preKeyStore.loadSignedPreKey(i);
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        return this.preKeyStore.loadSignedPreKeys();
    }

    @Override
    public void storeSignedPreKey(int i, SignedPreKeyRecord signedPreKeyRecord) {
        this.preKeyStore.storeSignedPreKey(i, signedPreKeyRecord);
    }

    @Override
    public boolean containsSignedPreKey(int i) {
        return this.preKeyStore.containsSignedPreKey(i);
    }

    @Override
    public void removeSignedPreKey(int i) {
        this.preKeyStore.removeSignedPreKey(i);
    }

    //--------------------------------------------------------------------------------
    @Override
    public SessionRecord loadSession(AxolotlAddress axolotlAddress) {
        return this.sessionStore.loadSession(axolotlAddress);
    }

    @Override
    public List<Integer> getSubDeviceSessions(String s) {
        return this.sessionStore.getSubDeviceSessions(s);
    }

    @Override
    public void storeSession(AxolotlAddress axolotlAddress, SessionRecord sessionRecord) {
        this.sessionStore.storeSession(axolotlAddress, sessionRecord);
    }

    @Override
    public boolean containsSession(AxolotlAddress axolotlAddress) {
        return this.sessionStore.containsSession(axolotlAddress);
    }

    @Override
    public void deleteSession(AxolotlAddress axolotlAddress) {
        this.sessionStore.deleteSession(axolotlAddress);
    }

    @Override
    public void deleteAllSessions(String s) {
        this.sessionStore.deleteAllSessions(s);
    }
}

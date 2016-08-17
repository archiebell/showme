package invalid.showme.model.server.clientcert;

import android.database.Cursor;

import org.acra.ACRA;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentLinkedQueue;

import invalid.showme.model.UserProfile;
import invalid.showme.util.KeyUtil;

public class ClientCertificateStore
{
    private Boolean jobQueued;
    private ConcurrentLinkedQueue<ClientCertificate> certificates;

    public ClientCertificateStore(Cursor certs) {
        this.jobQueued = false;
        this.certificates = new ConcurrentLinkedQueue<>();
        while(certs.moveToNext()) {
            ClientCertificate c = ClientCertificate.FromDatabase(certs);
            this.certificates.add(c);
        }
        certs.close();
    }

    public int numReady() { return this.certificates.size(); }

    public ClientCertificate get() {
        return this.certificates.remove();
    }

    public void generateNow() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);

            KeyPair kp = kpg.generateKeyPair();
            X509Certificate cert = KeyUtil.CreateCertificate(kp);
            this.certificates.add(new ClientCertificate(kp, cert));
        } catch(Exception e) {
            ACRA.getErrorReporter().handleException(e);
        }
    }

    //TODO: race condition if called from multiple threads.
    public void generateLater(UserProfile up) {
        if(jobQueued) return;
        jobQueued = true;
        up.getJobManager().addJobInBackground(new ClientCertGenerationJob(this));
    }
    public void saveToDatabase(UserProfile up)
    {
        for (ClientCertificate cert : this.certificates)
        {
            if(!cert.SaveToDatabase(up)) {
                if(!cert.SaveToDatabase(up)) {
                    //Give up
                }
            }
        }
    }
    public void jobDone() {
        jobQueued = false;
    }

}

package invalid.showme.services.ACRA;

import android.content.Context;
import android.support.annotation.NonNull;

import org.acra.config.ACRAConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;

public class TorACRASenderFactory implements ReportSenderFactory
{
    @NonNull
    @Override
    public ReportSender create(@NonNull Context context, @NonNull ACRAConfiguration config) {
        return new TorACRASender(config, config.httpMethod(), config.reportType(), null);
    }
}

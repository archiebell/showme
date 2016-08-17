package invalid.showme.model.db;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

public class JobDBPeeker
{
    public static boolean queuedJobs(Context context)
    {
        File a = context.getFilesDir();
        File b = new File(a, "../");
        File c = new File(b, "databases");
        File d = new File(c, "db_default_job_manager");


        SQLiteDatabase db = SQLiteDatabase.openDatabase(d.getAbsolutePath(), null, 0);
        Cursor cur = db.rawQuery("SELECT COUNT(*) FROM job_holder", null);
        cur.moveToFirst();
        int count = cur.getInt(0);
        cur.close();

        return count > 0;
    }
}

package eu.depa.flang;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.rmtheis.yandtran.language.Language;
import com.rmtheis.yandtran.translate.Translate;

import java.util.Calendar;
import java.util.Random;

import eu.depa.flang.ui.activities.WordInfo;

public class NotificationService extends Service {

    private PowerManager.WakeLock mWakeLock;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void work() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!(prefs.getString("interval", "5").equals("0") ||
                prefs.getInt("learned", 0) >= Constants.words.length ||
                isNight(prefs))) {

            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "flang");
            mWakeLock.acquire();
            new PollTask(this).execute();
        }
    }

    private boolean isNight(SharedPreferences prefs) {
        return prefs.getBoolean("sleep", true) &&
                (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 22 ||
                        Calendar.getInstance().get(Calendar.HOUR_OF_DAY) <= 8);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        work();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWakeLock != null)
            mWakeLock.release();
    }

    class PollTask extends AsyncTask<Void, Void, Void> {

        final public Context context;
        public String translatedText, chosen;

        public PollTask(Context pContext) {
            context = pContext;
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            String from = Constants.langCodes[prefs.getInt("from", 0)],
                    to = Constants.langCodes[prefs.getInt("to", 1)];

            int random;
            do
                random = new Random().nextInt(Constants.words.length);
            while (prefs.getBoolean("w" + String.valueOf(random), false));

            chosen = Constants.words[random];
            String wordEn = chosen;

            if (!from.equals("en"))
                chosen = translate(chosen, "en", from);

            translatedText = (to.equals("en")) ?
                    wordEn : translate(wordEn, "en", to);
            editor
                    .putInt("learned", prefs.getInt("learned", 0) + 1)
                    .putBoolean("w" + String.valueOf(random), true)
                    .apply();
            return null;
        }

        private String translate(final String word, final String from, final String to) {

            Translate.setKey(Constants.key);
            String translation = null;
            try {
                translation = Translate.execute(word, Language.fromString(from), Language.fromString(to));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return translation;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_task)
                            .setContentTitle(translatedText)
                            .setContentText(chosen)
                            .setColor(Constants.getColor(context, R.color.colorPrimaryDark))
                            .setLights(Constants.getColor(context, R.color.colorPrimaryDark), 5000, 500);
            if (prefs.getBoolean("show_in_lockscreen", true))
                builder.setPublicVersion(builder.build());

            int NOTIFICATION_ID = 49165;

            Intent targetIntent = new Intent(context, WordInfo.class);
            targetIntent.putExtra("original", chosen)
                    .putExtra("translated", translatedText)
                    .putExtra("wordInfo", true)
                    .putExtra("id", NOTIFICATION_ID);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(contentIntent);
            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}
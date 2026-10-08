package net.sourceforge.opencamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/** Entry Activity for a "take photo" shortcut.
 *  Redirects to Metro Camera [com.metro.camera.MainActivity].
 */
public class TakePhoto extends Activity {
    private static final String TAG = "TakePhoto";

    public static boolean TAKE_PHOTO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if( MyDebug.LOG )
            Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, com.metro.camera.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TakePhoto.TAKE_PHOTO = true;
        this.startActivity(intent);
        if( MyDebug.LOG )
            Log.d(TAG, "finish");
        this.finish();
    }

    protected void onResume() {
        if( MyDebug.LOG )
            Log.d(TAG, "onResume");
        super.onResume();
    }
}

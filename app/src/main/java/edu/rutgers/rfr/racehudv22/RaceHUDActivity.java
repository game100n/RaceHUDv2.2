package edu.rutgers.rfr.racehudv22;

import com.google.android.glass.timeline.LiveCard;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

/**
 * A transparent {@link Activity} displaying a "Stop" options menu to remove the {@link LiveCard}.
 */
public class RaceHUDActivity extends Activity
{

    @Override
    public void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Open the options menu right away.
        openOptionsMenu();
    }

    @Override
    public void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.racehud_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_stop:
                /** Stop the service which will unpublish the live card. */
                stopService(new Intent(this, RaceHUDService.class));
                //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu)
    {
        super.onOptionsMenuClosed(menu);
        // Nothing else to do, finish the Activity.
        finish();
    }
}

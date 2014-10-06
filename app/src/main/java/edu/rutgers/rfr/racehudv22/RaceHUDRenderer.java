package edu.rutgers.rfr.racehudv22;

import com.google.android.glass.timeline.DirectRenderingCallback;
import com.google.android.glass.timeline.LiveCard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

/**
 * Renders a fading "Hello world!" in a {@link LiveCard}.
 */
public class RaceHUDRenderer implements DirectRenderingCallback
{
    private static final String LIVE_CARD_TAG = "RaceHUDRenderer";

    /** The refresh rate, in frames per second, of the speedometer. */
    private static final int REFRESH_RATE_FPS = 45;

    /** The duration, in milliseconds, of one frame. */
    private static final long FRAME_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1) / REFRESH_RATE_FPS;

    /** Speed text size. */
    private static final float TEXT_SIZE = 70f;

    private final Paint mSpeedPaint;
    private final Paint mLabelPaint;
    private String mSpeedText;
    private String mLabelText;

    private final NumberFormat mRaceFormat;

    private int mCenterX;
    private int mCenterY;

    private SurfaceHolder mHolder;
    private boolean mRenderingPaused;

    private RenderThread mRenderThread;

    private final GPSManager mGPSManager;

    private final GPSManager.OnChangedListener mRaceListener =
            new GPSManager.OnChangedListener()
            {
                @Override
                public void onLocationChanged(GPSManager gpsManager)
                {
                    Location currentlocation = gpsManager.getLocation();
                    Double currentSpeed = gpsManager.getSpeed();

                    if (currentlocation == null)
                    {
                        /** If GPS not connected display default value */
                        mSpeedText = "@string/initial_speed";
                    }

                    else
                    {
                        /** Find Current speed in MPH */
                        double CurrentSpeedMPH = currentSpeed * 2.23694;
                        Log.d(LIVE_CARD_TAG, String.valueOf(CurrentSpeedMPH));
                        mSpeedText = String.valueOf(mRaceFormat.format(CurrentSpeedMPH));
                    }
                }
            };

    public RaceHUDRenderer(Context context, GPSManager GPSManager)
    {
        mSpeedPaint = new Paint();
        mSpeedPaint.setStyle(Paint.Style.FILL);
        mSpeedPaint.setColor(Color.RED);
        mSpeedPaint.setAntiAlias(true);
        mSpeedPaint.setTextSize(TEXT_SIZE);
        mSpeedPaint.setTextAlign(Paint.Align.CENTER);
        mSpeedPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        mSpeedPaint.setAlpha(255);

        mLabelPaint = new Paint();
        mLabelPaint.setStyle(Paint.Style.FILL);
        mLabelPaint.setColor(Color.WHITE);
        mLabelPaint.setAntiAlias(true);
        mLabelPaint.setTextSize(TEXT_SIZE);
        mLabelPaint.setTextAlign(Paint.Align.CENTER);
        mLabelPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        mLabelPaint.setAlpha(255);

        mSpeedText = context.getResources().getString(R.string.initial_speed);
        mLabelText = context.getResources().getString(R.string.MPH);

        mRaceFormat = NumberFormat.getNumberInstance();
        mRaceFormat.setMinimumFractionDigits(0);
        mRaceFormat.setMaximumFractionDigits(1);

        mGPSManager = GPSManager;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        mCenterX = width / 2;
        mCenterY = height / 2;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        mHolder = holder;
        mRenderingPaused = false;
        updateRenderingState();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        mHolder = null;
        updateRenderingState();
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused)
    {
        mRenderingPaused = paused;
        updateRenderingState();
    }

    /**
     * Starts or stops rendering according to the {@link LiveCard}'s state.
     */
    private void updateRenderingState()
    {
        boolean shouldRender = (mHolder != null) && !mRenderingPaused;
        boolean isRendering = (mRenderThread != null);

        if (shouldRender != isRendering)
        {
            if (shouldRender)
            {
                mGPSManager.addOnChangedListener(mRaceListener);
                mGPSManager.start();

                if (mGPSManager.hasLocation())
                {
                    Location location = mGPSManager.getLocation();
                    Double speed = mGPSManager.getSpeed();
                }

                mRenderThread = new RenderThread();
                mRenderThread.start();
            }

            else
            {
                mRenderThread.quit();
                mRenderThread = null;

                mGPSManager.removeOnChangedListener(mRaceListener);
                mGPSManager.stop();
            }
        }
    }

    /**
     * Draws the view in the SurfaceHolder's canvas.
     */
    private void draw()
    {
        Canvas canvas;
        try
        {
            canvas = mHolder.lockCanvas();
        }
        catch (Exception e)
        {
            Log.d(LIVE_CARD_TAG, "lockCanvas failed", e);
            return;
        }

        if (canvas != null)
        {
            /** Clear the canvas. */
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            /** Update the text alpha and draw the text on the canvas. */
            canvas.drawText(mSpeedText, mCenterX, mCenterY, mSpeedPaint);
            canvas.drawText(mLabelText, mCenterX, mCenterY + TEXT_SIZE, mLabelPaint);

            /** Unlock the canvas and post the updates. */
            mHolder.unlockCanvasAndPost(canvas);
        }
    }

    /**
     * Redraws the {@link View} in the background.
     */
    private class RenderThread extends Thread
    {
        private boolean mShouldRun;

        /**
         * Initializes the background rendering thread.
         */
        public RenderThread() {
            mShouldRun = true;
        }

        /**
         * Returns true if the rendering thread should continue to run.
         *
         * @return true if the rendering thread should continue to run
         */
        private synchronized boolean shouldRun() {
            return mShouldRun;
        }

        /**
         * Requests that the rendering thread exit at the next opportunity.
         */
        public synchronized void quit() {
            mShouldRun = false;
        }

        @Override
        public void run()
        {
            while (shouldRun())
            {
                long frameStart = SystemClock.elapsedRealtime();
                draw();
                long frameLength = SystemClock.elapsedRealtime() - frameStart;

                long sleepTime = FRAME_TIME_MILLIS - frameLength;
                if (sleepTime > 0)
                {
                    SystemClock.sleep(sleepTime);
                }
            }
        }
    }
}

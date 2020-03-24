package net.quantum6.camerafps;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;


/**
 *
 */
public final class CameraActivity extends Activity implements View.OnClickListener, OnItemSelectedListener
{
    private final static String TAG = "CameraActivity";

    private final static int MESSAGE_CHECK_FPS      = 1;
    private final static int MESSAGE_CHECK_INIT     = 2;
    private final static int MESSAGE_CHANGE_SHAPE   = 3;
    
    private final static int TIME_DELAY             = 1000;
    
    
    private     SurfaceView     mPreviewView;
    private     Spinner         mResolution;
    private     TextView        mInfoText;

    private CameraHelper mCameraHelper              = new CameraHelper();
    private int mSelectedIndex                      = -1;
    

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        mPreviewView = (SurfaceView) this.findViewById(R.id.preview);
        SurfaceHolder previewHolder = mPreviewView.getHolder();
        //mPreviewView.setZOrderOnTop(true);
        mCameraHelper.mPreviewHolder = previewHolder;
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        previewHolder.addCallback(mCameraHelper);

        mResolution = (Spinner)this.findViewById(R.id.resolution);
        mResolution.requestFocus();
        
        mInfoText = (TextView)this.findViewById(R.id.info_text);

        mHandler.sendEmptyMessageDelayed(MESSAGE_CHECK_INIT, TIME_DELAY);
    }

    @Override
    public void onClick(View view)
    {
        Log.d(TAG, "onClick()");
    }
    
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MESSAGE_CHECK_FPS:
                    if (null != mCameraHelper.mPreviewSize)
                    {
                        mInfoText.setText("("+mCameraHelper.mPreviewSize.width+", "+mCameraHelper.mPreviewSize.height
                                +")="+mCameraHelper.mFpsCurrent
                                +", "+SystemKit.getText(getApplicationContext()));
                        mHandler.sendEmptyMessageDelayed(MESSAGE_CHECK_FPS, TIME_DELAY);
                    }
                    break;
                    
                case MESSAGE_CHECK_INIT:
                    if (mCameraHelper.isInited)
                    {
                        List<String> resolutions = new LinkedList<String>();
                        if (null != mCameraHelper.mSupportedSizes)
                        {
                            for (int i = 0; i < mCameraHelper.mSupportedSizes.size(); i++)
                            {
                                Size size = mCameraHelper.mSupportedSizes.get(i);
                                resolutions.add("分辨率"+i+"=("+size.width+", "+size.height+")");
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                CameraActivity.this.getApplicationContext(), 
                                R.layout.spinner_item,
                                resolutions
                                );
                        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                        mResolution.setAdapter(adapter);
                        mResolution.setOnItemSelectedListener(CameraActivity.this);
                        mHandler.sendEmptyMessageDelayed(MESSAGE_CHECK_FPS, TIME_DELAY);
                    }
                    else
                    {
                        mHandler.sendEmptyMessageDelayed(MESSAGE_CHECK_INIT, TIME_DELAY);
                    }
                    break;
                    
                case MESSAGE_CHANGE_SHAPE:
                    if (mCameraHelper.isInited)
                    {
                        double ratio = 1.0 * mCameraHelper.mPreviewSize.width/mCameraHelper.mPreviewSize.height;
                        adjustViewShape(mPreviewView, ratio);
                    }
                    else
                    {
                        mHandler.sendEmptyMessageDelayed(MESSAGE_CHANGE_SHAPE, TIME_DELAY);
                    }
                    break;
                    
                default:
                    break;
            }
        }
    };
    
    /**
     * @param view
     * @param ratio
     */
    private void adjustViewShape(View view, double ratio)
    {
        int height   = view.getMeasuredHeight();
        int newWidth = (int)(height*ratio);
        int width    = view.getMeasuredHeight();
        if (Math.abs(newWidth-width)<5)
        {
            return;
        }
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)view.getLayoutParams();
        lp.width = newWidth;
        view.setLayoutParams(lp);
    }
    
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
    {
        if (position == mSelectedIndex)
        {
            return;
        }
        
        mSelectedIndex = position;
        String selected = (String) adapterView.getItemAtPosition(position);
        selected = selected.substring(selected.indexOf('(')+1, selected.indexOf(')'));
        int pos = selected.indexOf(',');
        int width  = Integer.parseInt(selected.substring(0, pos));
        int height = Integer.parseInt(selected.substring(pos+1).trim());

        mCameraHelper.openCamera(width, height);
        mHandler.sendEmptyMessage(MESSAGE_CHANGE_SHAPE);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0)
    {
        //
    }
    
    @Override
    public void onPause()
    {
        finish();
        super.onPause();
    }
    
    @Override
    public void onDestroy()
    {
        mHandler.removeMessages(MESSAGE_CHECK_INIT);
        mHandler.removeMessages(MESSAGE_CHECK_FPS);
        mHandler.removeMessages(MESSAGE_CHANGE_SHAPE);
        mHandler = null;
        
        mCameraHelper.release();
        mCameraHelper = null;
        
        mPreviewView    = null;
        mInfoText       = null;
        mResolution     = null;

        super.onDestroy();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (KeyEvent.KEYCODE_BACK == keyCode)
        {
            System.exit(0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}

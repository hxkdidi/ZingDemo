package com.didi.zingdemo;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements DecoratedBarcodeView.TorchListener{

    private static final int PHOTO_RESULT = 123;
    private static final String TAG = "MainActivity";
    private CaptureManager captureManager;
    private DecoratedBarcodeView decoratedBarcodeView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        decoratedBarcodeView = (DecoratedBarcodeView)findViewById(R.id.barcode_scanner);
        decoratedBarcodeView.setTorchListener(this);

        if (!hasFlash()) {
            findViewById(R.id.turn).setVisibility(View.GONE);
            findViewById(R.id.off).setVisibility(View.GONE);
        }

        captureManager = new CaptureManager(this, decoratedBarcodeView);
        captureManager.initializeFromIntent(getIntent(), savedInstanceState);
        captureManager.decode();

        decoratedBarcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                Snackbar.make(decoratedBarcodeView, " result : "+result, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {

            }
        });
    }

    /**
     * Check if the device's camera has a Flashlight.
     * @return true if there is Flashlight, otherwise false.
     */
    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        captureManager.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return decoratedBarcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    /**
     * 开启闪光灯
     * @param view
     */
    public void on(View view) {
        decoratedBarcodeView.setTorchOn();
    }

    /**
     * 关闭闪光灯
     * @param view
     */
    public void off(View view) {
        decoratedBarcodeView.setTorchOff();
    }

    /**
     * 从相册选择识别
     * @param view
     */
    public void image(View view) {
        openPhotoComponent(this);
    }

    /**
     * 处理从系统选图片
     */
    private void handleImage(final String path) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                Result result = QRCodeDecoder.scanningImage(path);
                if(result!=null){
                    return result.toString();
                }else {
                    return "";
                }
            }
            @Override
            protected void onPostExecute(String result) {
                if (TextUtils.isEmpty(result)) {
                    Toast.makeText(MainActivity.this, "没找到二维码", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "result : "+result, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    @Override
    public void onTorchOn() {

    }

    @Override
    public void onTorchOff() {

    }

    /**
     * 从相册选取
     * @param activity
     */
    public  void openPhotoComponent(final FragmentActivity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        activity.startActivityForResult(intent, PHOTO_RESULT);
    }

    /**
     * 回调解析二维码图片
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PHOTO_RESULT:
                    if (data != null) {
                        String filePath =getPicPathFromData(data, this);
                        handleImage(filePath);
                    }
                    break;

            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 获取文件的路径
     *
     * @param data
     * @return
     */
    public static String getPicPathFromData(Intent data, AppCompatActivity mBaseActivity) {
        Uri uri = getUri(data, mBaseActivity);
        if (uri == null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            uri = Uri.parse(MediaStore.Images.Media.insertImage(mBaseActivity.getContentResolver(), bitmap, null, null));

        }
        ContentResolver resolver = mBaseActivity.getContentResolver();
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = resolver.query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            return path;
        } catch (Exception e) {
            Log.e(TAG, "FOR_SELECT_PHOTO Exception:" + e.toString());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 解决小米手机上获取图片路径为null的情况
     *
     * @param intent
     * @return
     */
    public static Uri getUri(android.content.Intent intent, AppCompatActivity mBaseActivity) {
        Uri uri = intent.getData();
        String type = intent.getType();
        if (uri.getScheme().equals("file") && (type.contains("image/"))) {
            String path = uri.getEncodedPath();
            if (path != null) {
                path = Uri.decode(path);
                ContentResolver cr = mBaseActivity.getContentResolver();
                StringBuffer buff = new StringBuffer();
                buff.append("(").append(MediaStore.Images.ImageColumns.DATA).append("=")
                        .append("'" + path + "'").append(")");
                Cursor cur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Images.ImageColumns._ID},
                        buff.toString(), null, null);
                int index = 0;
                for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
                    index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID);
                    // set _id value
                    index = cur.getInt(index);
                }
                if (index == 0) {
                    // do nothing
                } else {
                    Uri uri_temp = Uri.parse("content://media/external/images/media/" + index);
                    if (uri_temp != null) {
                        uri = uri_temp;
                    }
                }
            }
        }
        return uri;
    }

}

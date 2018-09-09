package org.gheskio.queue;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
import com.google.zxing.Result;


/**
 * Created by tluong on 6/16/2018.
 */

public class QrCodeScanner extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        // Programmatically initialize the scanner view
        mScannerView = new ZXingScannerView(this);
        // Set the scanner view as the content view
        setContentView(mScannerView);

    }

    @Override
    public void onResume() {
        super.onResume();
        // Register ourselves as a handler for scan results.
        mScannerView.setResultHandler(this);
        // Start camera on resume
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop camera on pause
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        // Do something with the result here
        // Prints scan results
        // Log.v("result", rawResult.getText());
        // Prints the scan format (qrcode, pdf417 etc.)
        // Log.v("result", rawResult.getBarcodeFormat().toString());
        // If you would like to resume scanning, call this method below:
        // mScannerView.resumeCameraPreview(this);
        // Intent intent = new Intent();

        // Get anf return the intent passed through from MainActivity
        Intent intent = getIntent();
        intent.putExtra("SCAN_RESULT", rawResult.getText());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

}

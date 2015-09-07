package com.vasilitate.vapp.sdk;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.FontAwesomeText;
import com.vasilitate.vapp.R;

/**
 * Displays the progress of SMS sending to the user.
 */
public class VappProgressActivity extends Activity implements VappProgressListener {

    private static final long DELAY_AFTER_FOREGROUND_COMPLETION = 1000;       // 1 sec.
    private static final long DELAY_AFTER_BACKGROUND_COMPLETION = 3000;       // 3 secs

    private TextView progressText;
    private TextView percentageView;
    private View progressBar;
    private FontAwesomeText cancelButton;

    private VappProduct currentProduct;

    private VappProgressReceiver smsProgressReceiver;

    private boolean modalPaymentMode;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vapp_progress);

        progressText = (TextView) findViewById( R.id.countdown_text );
        percentageView = (TextView) findViewById( R.id.percentage_view );
        progressBar = findViewById(R.id.vapp_progress_bar);
        cancelButton = (FontAwesomeText) findViewById(R.id.progress_cancel_button);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Vapp.cancelVappPayment(getApplicationContext());
            }
        });

        modalPaymentMode = getIntent().getExtras().getBoolean(VappActions.EXTRA_MODAL, true);

        if( getIntent().getExtras().containsKey( VappActions.EXTRA_NOTIFICATION_INVOKED ) ) {

            closeActivityAfterDelay( DELAY_AFTER_BACKGROUND_COMPLETION );
            return;
        } else {
            VappNotificationManager.removeNotification(this);
        }

        smsProgressReceiver = new VappProgressReceiver(this, this );

        smsProgressReceiver.onCreate();

        if( getIntent().getExtras().containsKey( VappActions.EXTRA_PRODUCT_ID ) ) {

            String productId = getIntent().getExtras().getString(VappActions.EXTRA_PRODUCT_ID);

            VappProduct productBeingPurchased = Vapp.getProductBeingPurchased(this);

            if( productBeingPurchased != null ) {

                // The App should be guarding against two products being purchased
                // simultaneously!
                finish();

            } else {

                currentProduct = Vapp.getProduct( productId );

                int currentSMSCount = VappProductManager.generateCurrentDownloadSMSCount(currentProduct);
                VappConfiguration.setCurrentDownloadSmsCountForProduct( this, currentProduct, currentSMSCount );

                // Start the SMS Service - SMSs need to be sent in a background so that the process can
                // continue when the user switches to another App!
                Vapp.startSMSService(this, productId);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( smsProgressReceiver != null ) {
            smsProgressReceiver.onDestroy();
            smsProgressReceiver = null;
        }
    }

    @Override
    public void onBackPressed() {
        if( !modalPaymentMode ) {
            super.onBackPressed();
        }
    }

    private void closeActivityAfterDelay(long delay) {

        progressBar.setVisibility( View.INVISIBLE );
        percentageView.setVisibility(View.INVISIBLE);

        // Just delay the closing of the screen so that user can see the completion
        // of the down count..
        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            public void run() {
                finish();
            }
        }, delay);
    }

    @Override
    public void onSMSSent(int smsSentCount, int progressPercentage) {
        final int requiredSmsCount = VappConfiguration.
                getCurrentDownloadSmsCountForProduct(this, currentProduct);
        String text = getString(R.string.vapp_progress_text_formatter,
                smsSentCount,
                requiredSmsCount);

        progressText.setText( text );
        percentageView.setText(String.valueOf(progressPercentage) + "%");
    }

    @Override
    public void onProgressTick(int progressPercentage) {
        percentageView.setText(String.valueOf(progressPercentage) + "%");
    }

    @Override
    public void onError(String message) {
        Vapp.showErrorMessage( VappProgressActivity.this, message );
    }

    @Override public void onCancelled() {
        Vapp.showErrorMessage(this, getString(R.string.cancelled_purchase));
    }

    @Override public void onCompletion() {
        progressBar.setVisibility( View.INVISIBLE );

        // Just delay the closing of the screen so that user can see the completion
        // of the down count..
        handler.postDelayed(new Runnable() {
            public void run() {
                finish();
            }
        }, DELAY_AFTER_FOREGROUND_COMPLETION );
    }

}

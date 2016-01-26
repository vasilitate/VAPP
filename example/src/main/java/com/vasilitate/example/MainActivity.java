package com.vasilitate.example;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.vasilitate.vapp.sdk.Vapp;
import com.vasilitate.vapp.sdk.VappNumberRange;
import com.vasilitate.vapp.sdk.VappProduct;
import com.vasilitate.vapp.sdk.VappProgressReceiver;
import com.vasilitate.vapp.sdk.VappProgressWidget;
import com.vasilitate.vapp.sdk.exceptions.VappException;

import java.util.List;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final List<VappProduct> VAPP_PRODUCTS = MyProduct.getProducts();
    private static final String VAPP_APP_NAME = "ExampleApp";

    private TextView rankStatusView;
    private TextView livesStatusView;
    private Button buyCommanderRankButton;
    private Button buyMoreLivesButton;
    private VappProgressWidget progressWidget;

    private VappProgressReceiver smsProgressReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        try {
            Vapp.initialise(this,
                            VAPP_APP_NAME,
                            VAPP_PRODUCTS,
                            new VappNumberRange( "+447458830000", "+447458830009"),
                            true,     // Test mode - don't send SMSs
                            true,
                            "BG8R4X2PCXYCHRCRJTK6");

            buyCommanderRankButton = (Button) findViewById( R.id.buy_commander_rank_button);
            progressWidget = (VappProgressWidget) findViewById( R.id.progress_widget);
            buyMoreLivesButton = (Button) findViewById( R.id.buy_more_lives_button);
            rankStatusView = (TextView) findViewById( R.id.status_rank);
            livesStatusView = (TextView) findViewById( R.id.status_lives);

            buyCommanderRankButton.setOnClickListener(this);
            buyMoreLivesButton.setOnClickListener(this);

            smsProgressReceiver = new VappProgressReceiver(this, progressWidget);
            smsProgressReceiver.onCreate();

        } catch( VappException e ) {
            Log.e("VappExample!", "Error initialising products: ", e);

            // There's been a problem with the VAPP! setup - display the problem and then exit.
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setMessage( e.getMessage())
                    .setTitle("VAPP! Exception")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });
            alertBuilder.create().show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setDisplayedStatus();
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
    public void onClick(View view) {

        VappProduct product = (VappProduct) view.getTag();

        progressWidget.display(product, new VappProgressWidget.VappCompletionListener() {
            @Override
            public void onError(String message) { }

            @Override
            public void onErrorAcknowledged() { }

            @Override
            public void onCompletion() {
                progressWidget.setVisibility( View.INVISIBLE );
            }
        });

        if( Vapp.showVappPaymentScreen(MainActivity.this, product, false) ) {
        }
    }

    public void setDisplayedStatus() {

        // The user can buy multiple extra lives so check if we've reached the maximum...
        final VappProduct commanderRankProduct =  MyProduct.LEVEL_COMMANDER.getVappProduct();
        boolean enabled = !Vapp.isPaidFor(this, commanderRankProduct);
        buyCommanderRankButton.setEnabled(enabled);
        buyCommanderRankButton.setTag(commanderRankProduct);

        rankStatusView.setText("Rank: " + (enabled ? "Soldier" : "Commander"));

        // The user can buy multiple extra lives so check if we've reached the maximum...
        final VappProduct extraLivesProduct =  MyProduct.EXTRA_LIVES.getVappProduct();
        int currentLives = Vapp.getProductRedeemedCount(this, extraLivesProduct );
        enabled = currentLives < extraLivesProduct.getMaxProductCount();
        buyMoreLivesButton.setEnabled(enabled);
        buyMoreLivesButton.setTag(extraLivesProduct);

        livesStatusView.setText("Lives: " + String.valueOf(currentLives)
                                + " of " + String.valueOf(extraLivesProduct.getMaxProductCount()));
    }
}

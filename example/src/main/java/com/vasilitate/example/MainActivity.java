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
import com.vasilitate.vapp.sdk.VappProduct;
import com.vasilitate.vapp.sdk.VappProgressReceiver;
import com.vasilitate.vapp.sdk.VappProgressWidget;
import com.vasilitate.vapp.sdk.exceptions.VappException;

import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity implements View.OnClickListener, VappProgressWidget.VappCompletionListener {

    private static final SimpleDateFormat SUBSCRIPTION_END_DATE_FORMAT = new SimpleDateFormat( "dd/MM/yy" );

    private TextView rankStatusView;
    private TextView livesStatusView;
    private TextView tenDaySubscriptionStatusView;
    private TextView twoWeekSubscriptionStatusView;
    private TextView monthlySubscriptionStatusView;

    private Button buyCommanderRankButton;
    private Button buyMoreLivesButton;

    private Button daySubscriptionButton;
    private Button weekSubscriptionButton;
    private Button dayOfMonthSubscriptionButton;

    private VappProgressWidget progressWidget;
    private VappProgressReceiver smsProgressReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

            buyCommanderRankButton = (Button) findViewById( R.id.buy_commander_rank_button);
            progressWidget = (VappProgressWidget) findViewById( R.id.progress_widget);
            buyMoreLivesButton = (Button) findViewById( R.id.buy_more_lives_button);

            daySubscriptionButton = (Button) findViewById( R.id.day_subscription_button);
            weekSubscriptionButton = (Button) findViewById( R.id.week_subscription_button);
            dayOfMonthSubscriptionButton = (Button) findViewById( R.id.day_of_month_subscription_button);


            rankStatusView = (TextView) findViewById( R.id.status_rank);
            livesStatusView = (TextView) findViewById( R.id.status_lives);
            tenDaySubscriptionStatusView = (TextView) findViewById( R.id.ten_day_subscription);
            twoWeekSubscriptionStatusView = (TextView) findViewById( R.id.two_week_subscription);
            monthlySubscriptionStatusView = (TextView) findViewById( R.id.monthly_subscription);

            buyCommanderRankButton.setOnClickListener(this);
            buyMoreLivesButton.setOnClickListener(this);
            daySubscriptionButton.setOnClickListener(this);
            weekSubscriptionButton.setOnClickListener(this);
            dayOfMonthSubscriptionButton.setOnClickListener(this);

            smsProgressReceiver = new VappProgressReceiver(this, progressWidget);
            smsProgressReceiver.onCreate();

        } catch( VappException e ) {
            Log.e("VappExample!", "Error initialising products: ", e);

            // There's been a problem with the VAPP! setup - display the problem and then exit.
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setMessage( e.getMessage())
                    .setTitle("VAPP! Exception")
                    .setCancelable(false)
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
        refreshProductPurchaseUi();
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
    public void onError(String message) {
        progressWidget.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onErrorAcknowledged() {
        progressWidget.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCompletion() {
        progressWidget.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View view) {
        VappProduct product = (VappProduct) view.getTag();
        progressWidget.display(product, this);
        Vapp.showVappPaymentScreen(MainActivity.this, product, Vapp.isTestMode( this ), 0);
    }

    public void refreshProductPurchaseUi() {

        // The user can buy only one Command Rank product...
        final VappProduct commanderRankProduct =  MyProduct.LEVEL_COMMANDER.getVappProduct();
        boolean enabled = !Vapp.isPaidFor(this, commanderRankProduct);
        buyCommanderRankButton.setEnabled(enabled);
        buyCommanderRankButton.setTag(commanderRankProduct);

        VappProduct subscriptionProduct =  MySubscription.DAILY_SUBSCRIPTION.getVappProduct();
        enabled = !Vapp.isPaidFor(this, subscriptionProduct);
        daySubscriptionButton.setEnabled(enabled);
        daySubscriptionButton.setTag(subscriptionProduct);

        subscriptionProduct =  MySubscription.WEEKLY_SUBSCRIPTION.getVappProduct();
        enabled = !Vapp.isPaidFor(this, subscriptionProduct);
        weekSubscriptionButton.setEnabled(enabled);
        weekSubscriptionButton.setTag(subscriptionProduct);

        subscriptionProduct =  MySubscription.DAY_OF_MONTH_SUBSCRIPTION.getVappProduct();
        enabled = !Vapp.isPaidFor(this, subscriptionProduct);
        dayOfMonthSubscriptionButton.setEnabled(enabled);
        dayOfMonthSubscriptionButton.setTag(subscriptionProduct);


        rankStatusView.setText(String.format("Rank: %s", (enabled ? "Soldier" : "Commander")));

        // The user can buy multiple extra lives so check if we've reached the maximum...
        final VappProduct extraLivesProduct =  MyProduct.EXTRA_LIVES.getVappProduct();
        int currentLives = Vapp.getProductRedeemedCount(this, extraLivesProduct );
        enabled = currentLives < extraLivesProduct.getMaxProductCount();
        buyMoreLivesButton.setEnabled(enabled);
        buyMoreLivesButton.setTag(extraLivesProduct);
        livesStatusView.setText(String.format("Lives: %d of %d", currentLives, extraLivesProduct.getMaxProductCount()));


//        boolean isPaidFor = Vapp.isPaidFor( this, MySubscription.DAILY_SUBSCRIPTION.getVappProduct() );
        Date subscriptionEndDate = Vapp.getSubscriptionEndDate( this, MySubscription.DAILY_SUBSCRIPTION.getVappProduct() );
        String status = subscriptionEndDate == null ? "No Subscription" : "Subscription end date = "
                                + SUBSCRIPTION_END_DATE_FORMAT.format( subscriptionEndDate );
        String subscriptionText = "10 day subscription: " + status;
        tenDaySubscriptionStatusView.setText(subscriptionText);

        subscriptionEndDate = Vapp.getSubscriptionEndDate( this, MySubscription.WEEKLY_SUBSCRIPTION.getVappProduct() );
        status = subscriptionEndDate == null ? "No Subscription" : "Subscription end date = "
                + SUBSCRIPTION_END_DATE_FORMAT.format( subscriptionEndDate );
        subscriptionText = "2 week subscription: " + status;
        twoWeekSubscriptionStatusView.setText(subscriptionText);

        subscriptionEndDate = Vapp.getSubscriptionEndDate( this, MySubscription.DAY_OF_MONTH_SUBSCRIPTION.getVappProduct() );
        status = subscriptionEndDate == null ? "No Subscription" : "Subscription end date = "
                + SUBSCRIPTION_END_DATE_FORMAT.format( subscriptionEndDate );
        subscriptionText = "Monthly subscription: " + status;
        monthlySubscriptionStatusView.setText(subscriptionText);
    }
}

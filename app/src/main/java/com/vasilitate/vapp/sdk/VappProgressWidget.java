package com.vasilitate.vapp.sdk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.FontAwesomeText;
import com.vasilitate.vapp.R;

/**
 * A custom view which displays either the number of SMS's sent or the percentage completion
 * as a progress bar and/or a numeric.
 */
public class VappProgressWidget extends RelativeLayout implements VappProgressListener {

	public interface VappProgressWidgetListener {

		void onSMSSent(int progress, int progressPercentage);
		void onProgressTick(int progressPercentage);
		void onError( String message );
		void onErrorAcknowledged();
		void onCompletion();
	}

    public interface VappCompletionListener {
        void onError( String message );
        void onErrorAcknowledged();
        void onCompletion();
    }

	private ProgressBar smsProgress;
	private TextView progressPercentageView;
	private FontAwesomeText cancelButton;

//	private int intervalStartDownCount;

	private boolean hideError = false;
    private boolean percentageMode = true;

    private Handler handler = new Handler();

    private VappProgressWidgetListener progressListener;
    private VappCompletionListener completionListener;

	public VappProgressWidget(Context context) {
		super(context );
		intialise();
	}

	public VappProgressWidget(Context context, AttributeSet attrs) {
		super(context, attrs );
		intialise(attrs);
	}

	public VappProgressWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		intialise(attrs);
	}
	
	private void intialise() {
		LayoutInflater inflater = (LayoutInflater)getContext().
					getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.vapp_progress_layout, this, false);

		smsProgress = (ProgressBar) view.findViewById(R.id.sms_progress);
		progressPercentageView = (TextView) view.findViewById( R.id.progress_percentage_view);

        cancelButton = (FontAwesomeText) view.findViewById(R.id.progress_cancel_button);
        cancelButton.setVisibility(VappConfiguration.isCancellableProducts(getContext()) ? VISIBLE : GONE);

        cancelButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                Vapp.cancelVappPayment(getContext());
            }
        });

    	this.addView(view);
	}

	private void intialise(AttributeSet attrs) {

		intialise();

		TypedArray a = getContext().getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.ProgressWidget,
				0, 0);

		try {
            hideError = a.getBoolean(R.styleable.ProgressWidget_hideError, false);
            percentageMode = a.getBoolean(R.styleable.ProgressWidget_percentageMode, true);

			if( a.getBoolean(R.styleable.ProgressWidget_hideCount, false) ) {
                smsProgress.setVisibility( View.GONE );
			}
			if( a.getBoolean(R.styleable.ProgressWidget_hideProgressBar, false) ) {
				progressPercentageView.setVisibility(View.GONE);
			}

		} finally {
			a.recycle();
		}
	}

	public void display( VappProduct product,
						 VappProgressWidgetListener progressListener ) {

		this.progressListener = progressListener;
        display(product);
	}

    public void display( VappProduct product,
                         VappCompletionListener completionListener ) {

        this.completionListener = completionListener;
        display( product );
    }

    private void display( VappProduct product ) {

        setVisibility(View.VISIBLE);

        if( percentageMode ) {
            smsProgress.setMax(100);
        } else {
            smsProgress.setMax(product.getRequiredSmsCount());
        }
    }

    @Override
	public void onSMSSent(int progress, int progressPercentage) {

        Log.d("Vapp!", "onSMSSent: " + progress + " : " + progress + " : " + progressPercentage);

        if( percentageMode ) {
            smsProgress.setProgress(progressPercentage);
            progressPercentageView.setText(String.valueOf(progressPercentage) + "%");
        } else {
            smsProgress.setProgress(progress);

            int remaining = smsProgress.getMax() - progress;
            progressPercentageView.setText(String.valueOf( remaining ));
        }

		if( progressListener != null ) {
			progressListener.onSMSSent(progress, progressPercentage);
		}
	}

	@Override
	public void onProgressTick(int progressPercentage) {

        if( percentageMode ) {
            progressPercentageView.setText(String.valueOf(progressPercentage) + "%");
            smsProgress.setProgress(progressPercentage);
        }

        Log.d("Vapp!", "onProgressTick: " + progressPercentage);

        if( progressListener != null ) {
			progressListener.onProgressTick(progressPercentage);
		}
	}

	@Override
	public void onError(String message) {

		if(hideError) {
			// There's been a problem with the VAPP! setup - display the problem and then exit.
			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
			alertBuilder.setMessage(message)
					.setTitle("VAPP! Error")
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (progressListener != null) {
                                progressListener.onErrorAcknowledged();
                            }
                            else if (completionListener != null) {
                                completionListener.onErrorAcknowledged();
                            }
                        }
                    });
		}

        if( progressListener != null ) {
			progressListener.onError(message);
		} else if (completionListener != null ) {
            completionListener.onError(message);
        }
	}

	@Override
	public void onCompletion() {
        Log.d("Vapp!", "onCompletion");


        // Show all is complete!
        smsProgress.setProgress(smsProgress.getMax());

        if( percentageMode ) {
            progressPercentageView.setText("100%");
        } else {
            progressPercentageView.setText("0");
        }

        // then hide after 1 second.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressListener != null) {
                    progressListener.onCompletion();
                }
                else if (completionListener != null) {
                    completionListener.onCompletion();
                }
            }
        }, 2000);
    }

    @Override public void onCancelled() {
        setVisibility(GONE);
        Context context = getContext();
        Vapp.showErrorMessage(context, context.getString(R.string.cancelled_purchase));
    }

}

# VAPP
The VAPP! mobile payment SDK for Android

# Getting Started
VAPP! is provided as a maven artefact. Your build.gradle file should then be modified 
so that it matches the code snippet below:

```
repositories {
    maven { url "https://jitpack.io" } // use Jitpack repository
}
dependencies {
    compile 'com.github.vasilitate:VAPP:v0.5' // add VAPP! SDK as a dependency
}
```

After syncing your gradle files, your project should be ready to initialise VAPP! with a list of products.
See the [sample application](https://github.com/vasilitate/VAPP-Store) if you are having trouble with this step.

# Vapp Numbers
You should download a 'vapp_numbers.csv' file and place it in your assets directory (/src/main/assets).
This file can be found on the developer console under System > Profile > vapp_numbers.csv, and is
required for the SDK to work.

### Initialising VAPP!
Vapp.initialise() must be called before any SMS payments are made. Initialisation requires several pieces of information:

- Your SDK key, which identifies your payments for the app
- The number range which you have been assigned
- A list of products within your app, which can be paid for using SMS

It is recommended that you initialise VAPP! within a custom [Application class](http://developer.android.com/reference/android/app/Application.html),
using something similar to the snippet below:

```
private static final String MY_SDK_KEY = "ABCDEFG123";

List<VappProduct> productList = new ArrayList<>();

Vapp.initialise(this, productList, false, false, MY_SDK_KEY);
```

### Defining Products
Any products within your app must be passed into the SDK during initialisation. A VappProduct requires a unique alphabetic ID, the number of SMS messages which will be sent, and the maximum number of times that the product can be purchased.

```
private static final String MY_PRODUCT_ID = "MyProduct";
private static final int SMS_COUNT = 15;
private static final int MAX_PURCHASE_COUNT = 1;

List<VappProduct> productList = new ArrayList<>();
productList.add(new VappProduct(MY_PRODUCT_ID, SMS_COUNT, MAX_PURCHASE_COUNT);
Vapp.initialise(this, VAPP_APP_ID, productList, vappNumberRange, false);
```

### Purchasing Products

Products can be purchased by calling **showVappPaymentScreen()**. Before initiating the sale, you should indicate to the user what they are buying and how much it will cost, like below:

```
new AlertDialog.Builder(PaymentActivity.this)
      .setTitle("Confirm Payment")
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            Vapp.showVappPaymentScreen(context, MY_PRODUCT, false);
          }
      }).show();
```

Only one product may be purchased at a time.  The SDK will enforce this however you should design your app so that it is obvious to the user that this is the case, e.g. by disabling purchasing options while one is in progress.

### Tracking Purchases

The SMS purchase may take some time (10's of minutes!), callbacks are provided with the VappProgressReceiver:
```
@Override public void onCreate(@Nullable Bundle savedInstanceState) {
  super.onCreate(savedInstanceState)
  VappProgressReceiver vappProgressReceiver = new VappProgressReceiver(context, listener);
  vappProgressReceiver.onCreate();
}

@Override public void onDestroy() {
    super.onDestroy();
    if (vappProgressReceiver != null) {
        vappProgressReceiver.onDestroy();
        vappProgressReceiver = null;
    }
}
```

### Progress Widget ###

You can make use of a purpose built progress view which allows you to display either the number of SMS's sent or the percentage completion as a progress bar and/or a numeric.  Just add the following to your layout file:

```
    <com.vasilitate.vapp.sdk.VappProgressWidget
        android:id="@+id/progress_widget"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>
```
To get the following progress view displayed:

![Progess Widget](/readmeresources/VappStoreProgress.png "Progess Widget")

You can set the 'hideCount' or 'hideProgressBar' attributes on this widget in the layout file if you only which to display the progress bar or numeric respectively.

After referencing the widget in the code you can set it as the listener for the VappProgressReceiver as follows:

```
    VappProgressWidget progressWidget = (VappProgressWidget) findViewById( R.id.progress_widget);
 
    vappProgressReceiver = new VappProgressReceiver(this, progressWidget);
    vappProgressReceiver.onCreate();
    
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
```
The widget also provides a VappCompletionListener listener to allow you to refresh the display once the purchase process has completed.

### Purchase Status ###

It is also possible to determine whether a product has been purchased using synchronous methods. Your app can then allow the user access to purchased content, if VAPP! has recorded the product as purchased.

```
boolean hasPaid = Vapp.isPaidFor(context, MY_PRODUCT));
boolean isPaying = Vapp.isBeingPaidFor(context, MY_PRODUCT);
```

### Cancelling Purchases ###
By default purchases are cancellable by the user. After clicking the cancel icon on the default Payment screen,
the user will be prompted to confirm the cancellation. If you wish to disable this behaviour, set the
cancellableProducts flag to false during Vapp.initialise(). It is also possible to manually cancel
a purchase with the following code (which will prompt the user):

```
Vapp.cancelVappPayment(context);
```

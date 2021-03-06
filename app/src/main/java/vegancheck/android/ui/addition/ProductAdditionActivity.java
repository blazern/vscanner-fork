package vegancheck.android.ui.addition;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import vegancheck.android.App;
import vegancheck.android.BarcodeToolkit;
import vegancheck.android.Product;
import vegancheck.android.R;
import vegancheck.android.network.ParcelableNameValuePair;
import vegancheck.android.network.http.HttpRequestResult;
import vegancheck.android.ui.BarcodeHttpActionActivity;
import vegancheck.android.ui.BarcodeHttpActionFragment;
import vegancheck.android.ui.CardboardActivityBase;
import vegancheck.android.ui.UIConstants;
import vegancheck.android.ui.scan.ScanActivity;

public class ProductAdditionActivity extends BarcodeHttpActionActivity {
    public ProductAdditionActivity() {
        super(
                App.getConfig().getServerUrl() + "tobasenew.php",
                R.string.product_addition_activity_submit_request_sent_toast,
                R.string.product_addition_activity_on_request_successfully_delivered,
                R.string.product_addition_activity_title);
        setFinalAction(new Runnable() {
            @Override
            public void run() {
                ScanActivity.startBy(ProductAdditionActivity.this);
            }
        });

        App.getLocationKeeper().requestLocationRenewal();
    }

    @Override
    protected final List<ParcelableNameValuePair> createPostParametersFor(final Object actionResult) {
        final List<ParcelableNameValuePair> postParameters = new ArrayList<ParcelableNameValuePair>();

        if (actionResult instanceof Product) {
            final Product product = (Product) actionResult;

            postParameters.add(new ParcelableNameValuePair("bcod", product.getBarcode()));
            postParameters.add(new ParcelableNameValuePair("name", product.getName()));
            postParameters.add(new ParcelableNameValuePair("companyname", product.getCompany()));

            // NOTE: Logical inversion with the 2 below parameters is intentional,
            // because server mistakenly uses negative naming convention for these 2.
            // (Like that: isNotVegan instead of isVegan.)
            postParameters.add(new ParcelableNameValuePair(
                    "veganstatus",
                    product.isVegan() ? "0" : "1"));
            postParameters.add(new ParcelableNameValuePair(
                    "vegetstatus",
                    product.isVegetarian() ? "0" : "1"));
            ////

            postParameters.add(new ParcelableNameValuePair("gmo", "0"));
            postParameters.add(new ParcelableNameValuePair("animals", product.wasTestedOnAnimals() ? "1" : "0"));
            postParameters.add(new ParcelableNameValuePair("comment", ""));

            postParameters.add(new ParcelableNameValuePair("user_client_app_identificator", App.getDeviceID()));
            postParameters.add(new ParcelableNameValuePair("user_client_platform_type_index", "1"));
            postParameters.add(new ParcelableNameValuePair("user_client_app_version", App.getAppVersion()));

            final Location lastLocation = App.getLocationKeeper().getLastLocation();
            if (lastLocation != null) {
                postParameters.add(
                        new ParcelableNameValuePair(
                                "report_longitude",
                                String.valueOf(lastLocation.getLongitude())));

                postParameters.add(
                        new ParcelableNameValuePair(
                                "report_latitude",
                                String.valueOf(lastLocation.getLatitude())));

                postParameters.add(
                        new ParcelableNameValuePair(
                                "report_location_accuracy",
                                String.valueOf(lastLocation.getAccuracy())));
            }
        } else {
            App.error(this, "(actionResult instanceof Product) == false!");
        }

        return postParameters;
    }

    @Override
    protected BarcodeHttpActionFragment createFragment() {
        return ProductAdditionFragment.createFor(getBarcode());
    }

    /**
     * @param barcode must be valid (i.e. BarcodeToolkit.isValid(barcode)==true)
     * @param context must not be null
     * @throws java.lang.IllegalArgumentException if any parameter is invalid
     */
    public static void startFor(final String barcode, final Context context) {
        if (!BarcodeToolkit.isValid(barcode)) {
            throw new IllegalArgumentException("barcode must be valid");
        } else if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        final Intent intent = new Intent(context, ProductAdditionActivity.class);
        intent.putExtra(BarcodeHttpActionActivity.BARCODE_EXTRA, barcode);
        context.startActivity(intent);
    }
}

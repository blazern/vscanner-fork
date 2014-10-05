package veganscanner.androclient;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends ActionBarActivity {
    MyTask myTask;
    String Barcode;
    String CommentTOSHOW = "";
    ProgressDialog progress;
    UserNetworkAction lastUserNetworkAction;
    private String comment = "";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_activity_layout);
    }

    public void onERRORButtonClick(View view) {
        lastUserNetworkAction = UserNetworkAction.REPORTING_ERROR;

        if (Barcode != null) {
            final DialogFragment errorReportDialog =
                    ErrorReportDialogFragment.create(new ErrorReportDialogFragment.Listener() {
                        @Override
                        public void onSendClicked(final String errorReportText) {
                            comment = errorReportText;
                            myTask = new MyTask();
                            progress = ProgressDialog.show(ProductActivity.this, "", "Соединение с базой", true);
                            myTask.execute();
                        }
                    });
            errorReportDialog.show(getSupportFragmentManager(), "dialog"); //TODO: magic word
        } else {
            Toast.makeText(this, "В начале отсканируйте товар", Toast.LENGTH_LONG).show();
        }
    }

    //ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ
    public void onINFButtonClick(View view) {
        if (Barcode != null) {
            Toast.makeText(this, CommentTOSHOW, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "В начале отсканируйте товар", Toast.LENGTH_LONG).show();
        }
    }

    public void onMyButtonClick(View view) {
        lastUserNetworkAction = UserNetworkAction.SENDING_BARCODE_DATA;
        IntentIntegrator scanIntegrator = new IntentIntegrator(this);
        scanIntegrator.initiateScan();
        Toast.makeText(this, "Поднесите камеру к штрих-коду", Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {
            Toast.makeText(this, "Получен штрих-код", Toast.LENGTH_SHORT).show();
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            TextView t = (TextView) findViewById(R.id.text_barcode);
            t.setText(scanContent);
            Barcode = scanContent;
            progress = ProgressDialog.show(this, "", "Соединение с базой", true);
            myTask = new MyTask();
            myTask.execute();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Не получен штрих-код", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private enum UserNetworkAction {SENDING_BARCODE_DATA, REPORTING_ERROR}

    class MyTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            byte[] resultb = null;
            String str = "";
            BufferedReader inBuffer = null;
            String url = "http://lumeria.ru/vscaner/";
            if (lastUserNetworkAction != UserNetworkAction.SENDING_BARCODE_DATA)//ЕСЛИ ВЫПОЛНЯМ POST ОТПРАВКИ ОШИБКИ
            {
                url = "http://lumeria.ru/vscaner/er.php";
            }
            String result = "fail";
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost request = new HttpPost(url);
                List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
                postParameters.add(new BasicNameValuePair("bcod", Barcode));
                if (lastUserNetworkAction != UserNetworkAction.SENDING_BARCODE_DATA) {
                    postParameters.add(new BasicNameValuePair("comment", comment));
                }
                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParameters, "UTF-8");

                request.setEntity(formEntity);
                httpClient.execute(request);
                HttpResponse response = httpClient.execute(request);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpURLConnection.HTTP_OK) {
                    resultb = EntityUtils.toByteArray(response.getEntity());
                    str = new String(resultb, "UTF-8");
                }

                result = str;
            } catch (Exception e) {
                // Do something about exceptions
                result = e.getMessage();
            } finally {
                if (inBuffer != null) {
                    try {
                        inBuffer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(String page) {
            progress.cancel();
            Boolean addbarcode = false;
            //ЕСЛИ POST ВЕРНУЛ "731" ТО ВЫЗЫВАЕМ ПРОЦЕДУРУ ДОБАВЛЕНИЯ В БАЗУ
            try {
                int b = Integer.parseInt(page);
                int a = 731;
                if (b == a) {
                    addbarcode = true;
                    final DialogFragment productNotFoundDialog =
                            ProductNotFoundDialogFragment.create(Barcode);
                    productNotFoundDialog.show(getSupportFragmentManager(), "dialog"); // TODO: magical word
                } else if (b == 0) {
                    addbarcode = true;
                    Toast.makeText(ProductActivity.this, "Ваше обращение принято! Орномное спасибо за участие в проекте!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
            }

            //ИНАЧЕ ПРОБУЕМ ОТОБРАЗИТЬ РЕЗУЛЬТАТ
            if (addbarcode == false) {
                try {
                    // TODO: whitespace adding - hack for correct evaluating of empty descriptions
                    String[] parts = (page + " ").split("§");
                    String vegantext;
                    String vegeteriatext;
                    String inanimals;
                    int j = Integer.parseInt(parts[1]); //ПРОВЕРКА НА ВЕГАНСТВО
                    if (j == 0) {
                        vegantext = "ДА";
                        vegeteriatext = "ДА";
                        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.product_activity_layout);
                        relativeLayout.setBackgroundResource(R.drawable.ok);
                    } else {
                        vegantext = "НЕТ";
                        ((TextView) findViewById(R.id.text_is_vegan)).setText(vegantext);
                        int j1 = Integer.parseInt(parts[2]);//ПРОВЕРКА НА ВЕГЕТАРИАНСТВО
                        if (j1 == 0) {
                            vegeteriatext = "ДА";
                            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.product_activity_layout);
                            relativeLayout.setBackgroundResource(R.drawable.normal);
                        } else {
                            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.product_activity_layout);
                            relativeLayout.setBackgroundResource(R.drawable.no);
                            vegeteriatext = "НЕТ";
                            ((TextView) findViewById(R.id.text_is_vegetarian)).setText(vegeteriatext);
                        }
                    }

                    int j3 = Integer.parseInt(parts[3]);
                    if (j3 == 0) {
                        inanimals = "НЕТ";
                    } else {
                        inanimals = "ДА";
                    }

                    ((TextView) findViewById(R.id.text_product_name)).setText(parts[0]);
                    ((TextView) findViewById(R.id.text_company_name)).setText(parts[5]);
                    ((TextView) findViewById(R.id.text_is_vegan)).setText(vegantext);
                    ((TextView) findViewById(R.id.text_is_vegetarian)).setText(vegeteriatext);
                    ((TextView) findViewById(R.id.text_was_tested_on_animals)).setText(inanimals);
                    // TODO: return showing of descriptions somehow
//                    CustomInf.setVisibility(View.VISIBLE);
                    CommentTOSHOW = parts[6];
                    if (j3 == 1) {
                        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.product_activity_layout);
                        relativeLayout.setBackgroundResource(R.drawable.no);
                        ((TextView) findViewById(R.id.text_was_tested_on_animals)).setText(inanimals);
                    }
                } catch (ArrayIndexOutOfBoundsException e) { // TODO: really? arrayoutofbound?
                    final DialogFragment internetUnavailableDialog =
                            InternetUnavailableDialogFragment.create(new Runnable() {
                                @Override
                                public void run() {
                                    myTask = new MyTask();
                                    myTask.execute();
                                }
                            });
                    internetUnavailableDialog.show(getSupportFragmentManager(), "dialog"); //TODO: magic word
                }
            }
        }
    }
}
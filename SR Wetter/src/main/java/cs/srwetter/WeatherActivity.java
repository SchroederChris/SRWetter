package cs.srwetter;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import de.devland.esperandro.Esperandro;

public class WeatherActivity extends Activity {
    private static final String URL = "http://www.sr-online.de/sronline/nachrichten/wetter/wetter_popup_SR_online100.html";
    private static final String IMG_BASE_URL = "http://www.sr-online.de";
    private static final String CONDITION_IDENTIFIER = "wetterlage";
    private static final String CONTENT_IDENTIFIER = "content";

    private static final int TODAY = 0;
    private static final int TOMORROW = 1;
    private static final int OUTLOOK = 2;

    private WebView conditionsView;
    private ImageView todayImageView;
    private WebView todayWebView;
    private ImageView tomorrowImageView;
    private WebView tomorrowWebView;
    private ImageView outlookImageView;
    private WebView outlookWebView;

    private WeatherPreferences preferences;
    private Picasso picasso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        if (preferences == null) {
            preferences = Esperandro.getPreferences(WeatherPreferences.class, this);
        }
        if (picasso == null) {
            picasso = Picasso.with(this);
        }

        initViews();
        loadContent();
    }

    private void initViews() {
        conditionsView = (WebView) findViewById(R.id.conditions);
        todayImageView = (ImageView) findViewById(R.id.todayImage);
        todayWebView = (WebView) findViewById(R.id.todayText);
        tomorrowImageView = (ImageView) findViewById(R.id.tomorrowImage);
        tomorrowWebView = (WebView) findViewById(R.id.tomorrowText);
        outlookImageView = (ImageView) findViewById(R.id.outlookImage);
        outlookWebView = (WebView) findViewById(R.id.outlookText);
    }

    private void loadContent() {
        DownloadWebpageTask task = new DownloadWebpageTask();
        task.execute(URL);
    }

    private void fillViews() {
        conditionsView.loadData(preferences.conditionsText(), "text/html", "utf-8");

        todayWebView.loadData(preferences.todayText(), "text/html", "utf-8");
        tomorrowWebView.loadData(preferences.tomorrowText(), "text/html", "utf-8");
        outlookWebView.loadData(preferences.outlookText(), "text/html", "utf-8");

        picasso.load(IMG_BASE_URL + preferences.todayImgSrc()).resize(
                preferences.todayImgWidth() * getResources().getInteger(R.integer.image_resize_factor),
                preferences.todayImgHeigth() * getResources().getInteger(R.integer.image_resize_factor)).into(todayImageView);
        picasso.load(IMG_BASE_URL + preferences.tomorrowImgSrc()).resize(
                preferences.tomorrowImgWidth() * getResources().getInteger(R.integer.image_resize_factor),
                preferences.tomorrowImgHeigth() * getResources().getInteger(R.integer.image_resize_factor)).into(tomorrowImageView);
        picasso.load(IMG_BASE_URL + preferences.outlookImgSrc()).resize(
                preferences.outlookImgWidth() * getResources().getInteger(R.integer.image_resize_factor),
                preferences.outlookImgHeigth() * getResources().getInteger(R.integer.image_resize_factor)).into(outlookImageView);
    }

    private void extractConditions(Document document) {
        Elements conditionsElements = document.getElementsByClass(CONDITION_IDENTIFIER);
        Element conditionsDiv = conditionsElements.first();
        Element conditionsTable = conditionsDiv.child(0);
        Element conditionsTbody = conditionsTable.child(0);
        Element conditionsTr = conditionsTbody.child(0);
        Element conditionsTd = conditionsTr.child(0);
        String conditions = conditionsTd.html();
        preferences.conditionsText(conditions);
    }

    private void extractDailyContent(Document document) {
        Elements contentElements = document.getElementsByClass(CONTENT_IDENTIFIER);
        Element contentDiv = contentElements.first();
        Element contentTable = contentDiv.child(0);
        Element contentTbody = contentTable.child(0);

        Element todayTrElement = contentTbody.child(TODAY);
        preferences.todayText(todayTrElement.child(2).html());
        preferences.todayImgSrc(getImgAttribute(todayTrElement, "src"));
        preferences.todayImgWidth(Integer.valueOf(getImgAttribute(todayTrElement, "width")));
        preferences.todayImgHeigth(Integer.valueOf(getImgAttribute(todayTrElement, "height")));

        Element tomorrowTrElement = contentTbody.child(TOMORROW);
        preferences.tomorrowText(tomorrowTrElement.child(2).html());
        preferences.tomorrowImgSrc(getImgAttribute(tomorrowTrElement, "src"));
        preferences.tomorrowImgWidth(Integer.valueOf(getImgAttribute(tomorrowTrElement, "width")));
        preferences.tomorrowImgHeigth(Integer.valueOf(getImgAttribute(tomorrowTrElement, "height")));

        Element outlookTrElement = contentTbody.child(OUTLOOK);
        preferences.outlookText(outlookTrElement.child(2).html());
        preferences.outlookImgSrc(getImgAttribute(outlookTrElement, "src"));
        preferences.outlookImgWidth(Integer.valueOf(getImgAttribute(outlookTrElement, "width")));
        preferences.outlookImgHeigth(Integer.valueOf(getImgAttribute(outlookTrElement, "height")));
    }

    private String getImgAttribute(Element todayTrElement, String attributeKey) {
        Element dayImgTd = todayTrElement.child(0);
        Element dayImg = dayImgTd.child(0);
        return dayImg.attr(attributeKey);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                loadContent();
                break;
        }
        return true;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private class DownloadWebpageTask extends AsyncTask<String, Void, Document> {
        @Override
        protected Document doInBackground(String... urls) {
            Document page = null;
            String url = urls[0];
            try {
                if (isOnline()) {
                    page = Jsoup.connect(url).get();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return page;
        }

        @Override
        protected void onPostExecute(Document document) {
            super.onPostExecute(document);

            if (document != null) {
                extractConditions(document);
                extractDailyContent(document);
            }

            fillViews();
        }
    }

}

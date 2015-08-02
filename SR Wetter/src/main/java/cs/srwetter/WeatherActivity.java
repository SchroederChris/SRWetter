package cs.srwetter;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.webkit.WebView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.devland.esperandro.Esperandro;

public class WeatherActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener {
    private static final String URL = "http://www.sr-online.de/sronline/nachrichten/wetter/wetter_popup_SR_online102.html";
    private static final String IMG_BASE_URL = "http://www.sr-online.de";
    private static final String CONDITION_IDENTIFIER = "wetterlage";
    private static final String CONTENT_IDENTIFIER = "content";

    private static final int TODAY = 0;
    private static final int TOMORROW = 1;
    private static final int OUTLOOK = 2;

    private static final long AUTO_REFRESH_INTERVAL = 60 * 60 * 1000;

    @InjectView(R.id.actionbar_toolbar)
    Toolbar toolbar;

    @InjectView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @InjectView(R.id.conditions)
    WebView conditionsView;

    @InjectView(R.id.todayImage)
    ImageView todayImageView;

    @InjectView(R.id.todayText)
    WebView todayWebView;

    @InjectView(R.id.tomorrowImage)
    ImageView tomorrowImageView;

    @InjectView(R.id.tomorrowText)
    WebView tomorrowWebView;

    @InjectView(R.id.outlookImage)
    ImageView outlookImageView;

    @InjectView(R.id.outlookText)
    WebView outlookWebView;

    private WeatherPreferences preferences;
    private Picasso picasso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        ButterKnife.inject(this);
        initSwipeRefreshLayout();

        preferences = Esperandro.getPreferences(WeatherPreferences.class, this);
        picasso = Picasso.with(this);

        if (preferences.updateTime() != null) {
            fillViews();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (System.currentTimeMillis() - preferences.lastRefresh() > AUTO_REFRESH_INTERVAL) {
            loadContent();
        }
    }

    private void initSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.material_design_indigo));
    }


    private void loadContent() {
        if (isOnline()) {
            DownloadWebpageTask task = new DownloadWebpageTask();
            swipeRefreshLayout.setRefreshing(true);
            task.execute(URL);
        }
    }

    private void fillViews() {
        toolbar.setTitle(preferences.updateTime());
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(10);
        }

        conditionsView.loadData(preferences.conditionsText(), "text/html; charset=UTF-8", null);

        todayWebView.loadData(preferences.todayText(), "text/html; charset=UTF-8", null);
        tomorrowWebView.loadData(preferences.tomorrowText(), "text/html; charset=UTF-8", null);
        outlookWebView.loadData(preferences.outlookText(), "text/html; charset=UTF-8", null);

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

    private void extractRefreshTime(Document document) {
        Element html = document.child(0);
        Element body = html.child(1);
        Element table = body.child(0);
        Element tbody = table.child(0);
        Element updateRow = tbody.child(1);
        Element updateField = updateRow.child(0);
        String updateTimeText = updateField.html();
        updateTimeText = updateTimeText.replace("&nbsp;", " ");
        preferences.updateTime(updateTimeText);
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


    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    public void onRefresh() {
        loadContent();
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
                extractRefreshTime(document);
                extractConditions(document);
                extractDailyContent(document);
            }

            fillViews();
            preferences.lastRefresh(System.currentTimeMillis());
            swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        protected void onCancelled() {
            swipeRefreshLayout.setRefreshing(false);
            super.onCancelled();
        }
    }

}

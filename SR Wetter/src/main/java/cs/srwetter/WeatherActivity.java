package cs.srwetter;

import android.app.Activity;
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

public class WeatherActivity extends Activity {
    private static final String URL = "http://www.sr-online.de/sronline/nachrichten/wetter/wetter_popup_SR_online100.html";
    private static final String CONDITION_IDENTIFIER = "wetterlage";
    private static final String CONTENT_IDENTIFIER = "content";

    private WebView conditionsView;
    private ImageView todayImageView;
    private WebView todayWebView;
    private ImageView tomorrowImageView;
    private WebView tomorrowWebView;
    private ImageView outlookImageView;
    private WebView outlookWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
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

    private void fillConditionsView(Document document) {
        Elements conditionsElements = document.getElementsByClass(CONDITION_IDENTIFIER);
        Element conditionsDiv = conditionsElements.first();
        Element conditionsTable = conditionsDiv.child(0);
        Element conditionsTbody = conditionsTable.child(0);
        Element conditionsTr = conditionsTbody.child(0);
        Element conditionsTd = conditionsTr.child(0);
        String conditions = conditionsTd.html();
        conditionsView.loadData(conditions, "text/html", "utf-8");
    }

    private void processContent(Document document) {
        Elements contentElements = document.getElementsByClass(CONTENT_IDENTIFIER);
        Element contentDiv = contentElements.first();
        Element contentTable = contentDiv.child(0);
        Element contentTbody = contentTable.child(0);

        processDayContent(contentTbody, 0, todayWebView, todayImageView);
        processDayContent(contentTbody, 1, tomorrowWebView, tomorrowImageView);
        processDayContent(contentTbody, 2, outlookWebView, outlookImageView);
    }

    private void processDayContent(Element contentTbody, int daysInFuture, WebView textWebView, ImageView imageView) {
        Element dayTrElement = contentTbody.child(daysInFuture);

        Element dayImgTd = dayTrElement.child(0);
        Element dayImg = dayImgTd.child(0);

        String dayImgSrc = dayImg.attr("src");
        String imgWidth = dayImg.attr("width");
        String imageHeight = dayImg.attr("height");

        int targetWidth = Integer.valueOf(imgWidth) * getResources().getInteger(R.integer.image_resize_factor);
        int targetHeight = Integer.valueOf(imageHeight) * getResources().getInteger(R.integer.image_resize_factor);
        Picasso.with(this).load("http://www.sr-online.de" + dayImgSrc).resize(targetWidth, targetHeight).into(imageView);

        Element dayTextElement = dayTrElement.child(2);
        String dayText = dayTextElement.html();
        textWebView.loadData(dayText, "text/html", "utf-8");
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

    private class DownloadWebpageTask extends AsyncTask<String, Void, Document> {
        @Override
        protected Document doInBackground(String... urls) {
            Document page = null;
            String url = urls[0];
            try {
                page = Jsoup.connect(url).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return page;
        }

        @Override
        protected void onPostExecute(Document document) {
            super.onPostExecute(document);

            fillConditionsView(document);
            processContent(document);
        }
    }

}

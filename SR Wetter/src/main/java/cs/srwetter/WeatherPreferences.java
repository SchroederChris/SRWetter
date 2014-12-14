package cs.srwetter;

import de.devland.esperandro.annotations.SharedPreferences;

@SharedPreferences
public interface WeatherPreferences {
    String updateTime();
    void updateTime(String updateTime);

    String conditionsText();
    void conditionsText(String conditionsText);


    String todayText();
    void todayText(String todayText);

    String todayImgSrc();
    void todayImgSrc(String todayImgSrc);

    int todayImgWidth();
    void todayImgWidth(int todayImgWidth);

    int todayImgHeigth();
    void todayImgHeigth(int todayImgHeigth);


    String tomorrowImgSrc();
    void tomorrowImgSrc(String tomorrowImgSrc);

    String tomorrowText();
    void tomorrowText(String tomorrowText);

    int tomorrowImgWidth();
    void tomorrowImgWidth(int tomorrowImgWidth);

    int tomorrowImgHeigth();
    void tomorrowImgHeigth(int tomorrowImgHeigth);


    String outlookText();
    void outlookText(String outlookText);

    String outlookImgSrc();
    void outlookImgSrc(String outlookImgSrc);

    int outlookImgWidth();
    void outlookImgWidth(int outlookImgWidth);

    int outlookImgHeigth();
    void outlookImgHeigth(int outlookImgHeigth);
}

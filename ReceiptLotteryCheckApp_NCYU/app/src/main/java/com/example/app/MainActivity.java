package com.example.app;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private int mYear, mMonth, mDay, input_year, input_month, input_day;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        //以下為選擇日期的月曆
        final TextView dateText = (TextView)findViewById(R.id.dataText);
        Button dateButton = (Button)findViewById(R.id.dataButton);
        dateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                mYear = c.get(Calendar.YEAR);
                mMonth = c.get(Calendar.MONTH);
                mDay = c.get(Calendar.DAY_OF_MONTH);
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        input_month = month+1;
                        input_year = year;
                        input_day = day;

                        String format = "您的發票日期為:"+ setDateFormat(year,month,day);
                        dateText.setText(format);

                        TextView dataW = (TextView) findViewById(R.id.dateWarning);
                        ColorStateList dataW_color = dataW.getTextColors();
                        changeToPrecaution(findViewById(R.id.precaution_button));//切回注意事項
                        //以下為確認日期是否正確(假如是在未來則顯示錯誤)
                        if (input_year < mYear ||
                                ((input_year == mYear) && ((input_month < mMonth+1) || ((input_month == mMonth+1) && (input_day <= mDay))))) {
                                Log.d("mytag", "correct_1");
                                //判斷是偶數月還是奇數月
                                switch (input_month % 2) {
                                    //奇數月
                                    case 1:
                                        //檢查是否已開獎
                                        if (((input_year < mYear) ||
                                                ((input_year == mYear) && ((mMonth+1>input_month+2)||((mMonth+1==input_month+2)&&(mDay>25)))))
                                                || (input_year == mYear-1 && input_month == 11 && (mMonth+1>1 || (mMonth+1==1 && mDay>25)))) {
                                            Log.d("mytag", "date_correct");
                                            set_en(true);
                                            dataW.setText("日期輸入正確");
                                            dataW.setTextColor(0xFF8BC34A);
                                        } else {
                                            //將底下的button都鎖起來，並且告知開獎日期
                                            set_en(false);
                                            String predict_str_0;
                                            if (input_month != 11) predict_str_0 = Integer.toString(input_year)+"年"+Integer.toString(input_month+2)+"月25日";
                                            else predict_str_0 = Integer.toString(input_year+1)+"年1月";
                                            dataW.setTextColor(0xFFE91E63);
                                            ((TextView) findViewById(R.id.dateWarning)).setText("該期將在"+ predict_str_0 +"開出");

                                        }
                                        break;
                                    //偶數月
                                    case 0:
                                        //檢查是否已開獎
                                        if (((input_year < mYear) ||
                                                ((input_year == mYear) && ((mMonth+1>input_month+1)||((mMonth+1==input_month+1)&&(mDay>25)))))
                                                || (input_year == mYear-1 && input_month == 12 && (mMonth+1>1 || (mMonth+1==1 && mDay>25)))) {
                                            Log.d("mytag", "date_correct");
                                            set_en(true);
                                            ((TextView) findViewById(R.id.dateWarning)).setText("日期輸入正確");
                                            dataW.setTextColor(0xFF8BC34A);
                                        } else {
                                            //將底下的button都鎖起來，並且告知開獎日期
                                            set_en(false);
                                            String predict_str_1;
                                            if (input_month != 11) predict_str_1 = Integer.toString(input_year)+"年"+Integer.toString(input_month+1)+"月25日";
                                            else predict_str_1 = Integer.toString(input_year+1)+"年1月";
                                            dataW.setTextColor(0xFFE91E63);
                                            ((TextView) findViewById(R.id.dateWarning)).setText("該期將在"+ predict_str_1 +"開出");
                                        }

                                        break;
                                }

                        } else {
                            //將底下的button都鎖起來，並且告知日期錯誤
                            set_en(false);
                            ((TextView) findViewById(R.id.dateWarning)).setText("請確認日期是否點選正確!");
                            dataW.setTextColor(0xFFE91E63);
                        }

                    }

                }, mYear,mMonth, mDay).show();
            }

        });
    };

    public void set_en(boolean b) {
        //假如日期錯誤，可以將下面的按鈕都鎖起來，或是打開
        findViewById(R.id.input_num).setEnabled(b);
        findViewById(R.id.searchButton).setEnabled(b);
        findViewById(R.id.bingo_info_button).setEnabled(b);
    }

    private String setDateFormat(int year,int monthOfYear,int dayOfMonth){
        //顯示輸入的日期
        return String.valueOf(year) + "-"
                + String.valueOf(monthOfYear + 1) + "-"
                + String.valueOf(dayOfMonth);
    }

boolean internet_b;

    public void search(View v) {
        //按下查詢鍵後進入此function
        TextView check_t = findViewById(R.id.numCheck_text);
        EditText t = findViewById(R.id.input_num);
        check_t.setTextSize(24);//還原預設的size
        char input_num[];
        //Log.d("mytag",t.getText().toString());
        input_num = t.getText().toString().toCharArray();//將input內容轉成charArray
        //以下為debug所用的，請忽略
        //Log.d("mytag", Character.toString(input_num[7]));
        //Log.d("mytag", Integer.toString(input_num.length));
        /*
        for (int i=0; i<input_num.length; i++) {
            Log.d("mytag", Character.toString(input_num[i]));//可刪
            Log.d("mytag", "*");//可刪
        }
         */

        boolean check_num = true;
        //檢查input是不是8個數字
        for (int i=0; i<input_num.length; i++) {
            if (!(input_num[i] >= '0' && input_num[i] <= '9' )) {
                check_num = false;
                break;
            }
        }
        if (!(check_num && input_num.length==8 )) {
            //Log.d("mytag", "num incorrect");
            catchInfo(input_year, input_month);//在這裡抓資訊是在測試是否有網路，無網路的話會將button關掉
            check_t.setText("數字輸入錯誤，請確認後重新輸入");
            check_t.setTextSize(14);
            check_t.setTextColor(0xFFE91E63);
        } else {
            catchInfo(input_year, input_month);//抓取該期的中獎資訊
            if (internet_b) check_info(input_num);//檢查是否中獎
        }
    }

    public void check_info(char input[]) {
        //開始兌獎
        //先將所有判斷都先預設為true
        boolean special_1 = true;//特別獎的判斷
        boolean special_2 = true;//特獎的判斷
        boolean bingo_b[][] = new boolean[5][3];// bingo_b[i][j] -> 第(i+1)獎的第(j+1)個判斷boolean
        for (int i=0; i<5; i++) {
            for (int j=0; j<3; j++) bingo_b[i][j] = true;
        }
        //以下為六獎的6個判斷boolean，3個是用頭獎來判斷，3個是用增開六獎來判斷
        boolean bingo_6_1 = true, bingo_6_2 = true, bingo_6_3 = true,
                bingo_6_4 = true, bingo_6_5 = true, bingo_6_6 = true;

        for (int i=0; i<8; i++) {
            //Log.d("mytag", "enter"+Integer.toString(i));
            if (special_1_str.charAt(i) != input[i]) special_1 = false;
            if (special_2_str.charAt(i) != input[i]) special_2 = false;
            if (bingo_1_1_str.charAt(i) != input[i]) bingo_b[0][0] = false;
            if (bingo_1_2_str.charAt(i) != input[i]) bingo_b[0][1] = false;
            if (bingo_1_3_str.charAt(i) != input[i]) bingo_b[0][2] = false;
            //兌二獎
            if (i>=1) {
                if (bingo_1_1_str.charAt(i) != input[i]) bingo_b[1][0] = false;
                if (bingo_1_2_str.charAt(i) != input[i]) bingo_b[1][1] = false;
                if (bingo_1_3_str.charAt(i) != input[i]) bingo_b[1][2] = false;
            }//兌三獎
            if (i>=2) {
                if (bingo_1_1_str.charAt(i) != input[i]) bingo_b[2][0] = false;
                if (bingo_1_2_str.charAt(i) != input[i]) bingo_b[2][1] = false;
                if (bingo_1_3_str.charAt(i) != input[i]) bingo_b[2][2] = false;
            }//兌四獎
            if (i>=3) {
                if (bingo_1_1_str.charAt(i) != input[i]) bingo_b[3][0] = false;
                if (bingo_1_2_str.charAt(i) != input[i]) bingo_b[3][1] = false;
                if (bingo_1_3_str.charAt(i) != input[i]) bingo_b[3][2] = false;
            }//兌五獎
            if (i>=4) {
                if (bingo_1_1_str.charAt(i) != input[i]) bingo_b[4][0] = false;
                if (bingo_1_2_str.charAt(i) != input[i]) bingo_b[4][1] = false;
                if (bingo_1_3_str.charAt(i) != input[i]) bingo_b[4][2] = false;
            }//兌六獎
            if (i >= 5) {
                switch (4 - six_count) {
                    case 1://不一定用到
                        if (bingo6_3[i-5] != input[i]) bingo_6_1 = false;
                    case 2://不一定用到
                        if (bingo6_2[i-5] != input[i]) bingo_6_2 = false;
                    case 3:
                        if (bingo6_1[i-5] != input[i]) bingo_6_3 = false;
                        if (bingo_1_3_str.charAt(i) != input[i]) bingo_6_4 = false;
                        if (bingo_1_2_str.charAt(i) != input[i]) bingo_6_5 = false;
                        if (bingo_1_1_str.charAt(i) != input[i]) bingo_6_6 = false;
                        break;
                }
            }
        }//顯示兌獎結果
        TextView check_t = findViewById(R.id.numCheck_text);
        check_t.setTextColor(0xFF8BC34A);//預設為綠色(中獎)
        if (special_1) {
            ((TextView)findViewById(R.id.numCheck_text)).setText("恭喜您中了特別獎 1,000萬元!!!");
        } else if (special_2) {
            ((TextView)findViewById(R.id.numCheck_text)).setText("恭喜您中了特獎 200萬元!!!");
        } else if (bingo_b[0][0] || bingo_b[0][1] || bingo_b[0][2]) {
            ((TextView)findViewById(R.id.numCheck_text)).setText("恭喜您中了頭獎 20萬元!!!");
        } else if (bingo_b[1][0] || bingo_b[1][1] || bingo_b[1][2]) {
            ((TextView)findViewById(R.id.numCheck_text)).setText("恭喜您中了二獎 4萬元!!!");
        } else if (bingo_b[2][0] || bingo_b[2][1] || bingo_b[2][2]) {
            ((TextView)findViewById(R.id.numCheck_text)).setText("恭喜您中了三獎 1萬元!!!");
        } else if (bingo_b[3][0] || bingo_b[3][1] || bingo_b[3][2]) {
            ((TextView)findViewById(R.id.numCheck_text)).setText("恭喜您中了四獎 4千元!!!");
        } else if (bingo_b[4][0]|| bingo_b[4][1] || bingo_b[4][2]) {
            ((TextView)findViewById(R.id.numCheck_text)).setText("恭喜您中了五獎 1千元!!!");
        } else {
            //判斷增開六獎的個數
            switch (six_count) {
                case 1:
                    if (bingo_6_3 || bingo_6_4 || bingo_6_5 || bingo_6_6) {
                        ((TextView)findViewById(R.id.numCheck_text)).setText("恭喜您中了六獎 200元!!!");
                    } else {
                        check_t.setTextColor(0xFFE91E63);
                        ((TextView)findViewById(R.id.numCheck_text)).setText("您未中獎~請再接再厲~~");
                    }
                    break;
                case 2:
                    if (bingo_6_2 || bingo_6_3 || bingo_6_4 || bingo_6_5 || bingo_6_6) {
                        ((TextView)findViewById(R.id.numCheck_text)).setText("恭喜您中了六獎 200元!!!");
                    } else {
                        check_t.setTextColor(0xFFE91E63);
                        ((TextView)findViewById(R.id.numCheck_text)).setText("您未中獎~請再接再厲~~");
                    }
                    break;
                case 3:
                    if (bingo_6_1 || bingo_6_2 || bingo_6_3 || bingo_6_4 || bingo_6_5 || bingo_6_6) {
                        ((TextView)findViewById(R.id.numCheck_text)).setText("恭喜您中了六獎 200元!!!");
                    } else {
                        check_t.setTextColor(0xFFE91E63);
                        ((TextView)findViewById(R.id.numCheck_text)).setText("您未中獎~請再接再厲~~");
                    }
                    break;
            }
        }
    }



    //以下為儲存特獎、特別獎、頭獎號碼的字串，bingo_6_str_p 作為在setBingoNum()輸出用的字串
    String special_1_str, special_2_str, bingo_1_1_str, bingo_1_2_str, bingo_1_3_str, bingo_6_str_p;
    //以下為儲存增開六獎的中獎號碼的Array
    char bingo6_1[] = new char[3];//增開1
    char bingo6_2[] = new char[3];//增開2(不一定有)
    char bingo6_3[] = new char[3];//增開3(不一定有)
    int six_count = 0; //紀錄增開6獎的總數:由於每年的數量都不一樣(最多3個，至少1個)

    public void catchInfo(int year, int month) {
        try {
            String month_str;
            if (month%2 == 0) month_str = Integer.toString(month-1);
            else month_str = Integer.toString(month);
            if (month != 11 && month!= 12) month_str = "0"+ month_str;
            Document doc = Jsoup.connect("https://www.etax.nat.gov.tw/etw-main/web/ETW183W2_"+Integer.toString(year-1911)+month_str+"/").get();
            Elements element = doc.select(".number");
            //Log.i("mytag",element.toString());
            Elements tds = element.select("td");
            //Log.i("mytag",tds.toString());

            for (int i=0; i<4; i++) {
                Element td = tds.get(i);
                String text = td.text();
                //Log.i("mytag", text);

                switch(i){
                    case 0:
                        special_1_str = text;
                        break;
                    case 1:
                        special_2_str = text;
                        break;
                    case 2:
                        String str_arr [] = text.split(" ");
                        bingo_1_1_str = str_arr[0];
                        bingo_1_2_str = str_arr[1];
                        bingo_1_3_str = str_arr[2];
                        break;
                    case 3:
                        bingo_6_str_p = text;
                        six_count = 0;
                        //假如增開6獎為3個，text.length()會為11個，14-11=3，他就能由上慢慢讀到下
                        switch (14 - text.length()) {
                            case 3:
                                for (int j=8, k=0; j<11; j++, k++) {
                                    bingo6_3[k] = text.charAt(j);
                                }
                                six_count ++;
                            case 7:
                                for (int j=4, k=0; j<7; j++, k++) {
                                    bingo6_2[k] = text.charAt(j);
                                }
                                six_count ++;
                            case 11:
                                for (int j=0; j<3; j++) {
                                    bingo6_1[j] = text.charAt(j);
                                }
                                six_count ++;
                                break;
                        }
                        break;
                }
                internet_b = true;

            }

        } catch (Exception e) {
            Log.i("mytag", e.toString());
            //做一個沒有連線的告訴 用TOAST&TextView
            Toast.makeText(this, "請先連接網際網路", Toast.LENGTH_SHORT).show();
            set_en(false);
            internet_b = false;
        }
    }

    public void changeToBingoInfo(View v) {
        //查看中獎號碼資訊時，需要將注意事項的button開啟但畫面Invisible
        setBingoNum();
        findViewById(R.id.rule_info).setEnabled(false);
        findViewById(R.id.rule_info).setVisibility(View.INVISIBLE);
        findViewById(R.id.bingo_info).setVisibility(View.VISIBLE);
        findViewById(R.id.bingo_info).setEnabled(true);
        findViewById(R.id.precaution_button).setEnabled(true);
        findViewById(R.id.bingo_info_button).setEnabled(false);
    }
    public void changeToPrecaution(View v) {
        //查看注意事項時，需要將中獎號碼資訊的button開啟但畫面Invisible
        findViewById(R.id.rule_info).setEnabled(true);
        findViewById(R.id.bingo_info).setVisibility(View.INVISIBLE);
        findViewById(R.id.rule_info).setVisibility(View.VISIBLE);
        findViewById(R.id.bingo_info).setEnabled(true);
        findViewById(R.id.precaution_button).setEnabled(false);
        findViewById(R.id.bingo_info_button).setEnabled(true);
    }

    public void setBingoNum() {
        //將中獎號碼資訊的textview set text 成抓取到的中獎號碼
        catchInfo(input_year, input_month);
        ((TextView) findViewById(R.id.special_1_t)).setText(special_1_str);
        ((TextView) findViewById(R.id.special_2_t)).setText(special_2_str);
        ((TextView) findViewById(R.id.bingo_1_1_t)).setText(bingo_1_1_str);
        ((TextView) findViewById(R.id.bingo_1_2_t)).setText(bingo_1_2_str);
        ((TextView) findViewById(R.id.bingo_1_3_t)).setText(bingo_1_3_str);
        ((TextView) findViewById(R.id.bingo_6_t)).setText(bingo_6_str_p);
    }

    public void launchWriterInfo(View v) {
        //進入作者資訊
        Intent i = new Intent(this, Writer_Info.class);
        startActivity(i);
    }

}

//以下為被淘汰掉的想法
/*
        //兌特別獎
        boolean special_1 = true;
        for (int i=0; i<8; i++)
            if (num1[i] != input[i]) special_1 = false;
        if (special_1) {

        }
        //兌特獎
        boolean special_2 = true;
        for (int i=0; i<8; i++)
            if (num2[i] != input[i]) special_2 = false;
        if (special_2) {

        }
        boolean bingo_1, bingo_2, bingo_3, bingo_4, bingo_5;
        //兌頭獎

        while (true) {
            bingo_1 = true;
            for (int i=0; i<8; i++)
                if (num3[i] != input[i]) bingo_1 = false;
            if (bingo_1) break;
            bingo_1 = true;
            for (int i=0; i<8; i++)
                if (num4[i] != input[i]) bingo_1 = false;
            if (bingo_1) break;
            bingo_1 = true;
            for (int i=0; i<8; i++)
                if (num5[i] != input[i]) bingo_1 = false;
            break;
        }
        if (bingo_1) {

        }
        //兌二獎
        while (true) {
            bingo_2 = true;
            for (int i=1; i<8; i++)
                if (num3[i] != input[i]) bingo_2 = false;
            if (bingo_2) break;
            bingo_2 = true;
            for (int i=1; i<8; i++)
                if (num4[i] != input[i]) bingo_2 = false;
            if (bingo_2) break;
            for (int i=1; i<8; i++)
                if (num5[i] != input[i]) bingo_2 = false;
            break;
        }
        if (bingo_2) {

        }
        //兌三獎
        while (true) {
            bingo_3 = true;
            for (int i=2; i<8; i++)
                if (num3[i] != input[i]) bingo_3 = false;
            if (bingo_3) break;
            bingo_3 = true;
            for (int i=2; i<8; i++)
                if (num4[i] != input[i]) bingo_3 = false;
            if (bingo_3) break;
            bingo_3 = true;
            for (int i=2; i<8; i++)
                if (num5[i] != input[i]) bingo_3 = false;
            break;
        }
        if (bingo_3) {

        }
        //兌四獎



         */
/*
        //兌六獎
        boolean bingo_6 = true;
        switch (4 - six_count) {
            case 1:
                for (int i=0; i<3; i++) {
                    if (num8[i] != input[5+i]) {
                        bingo_6 = false;
                    }
                }
                if (bingo_6) break;
            case 2:
                bingo_6 = true;
                for (int i=0; i<3; i++) {
                    if (num7[i] != input[5+i]) {
                        bingo_6 = false;
                    }
                }
                if (bingo_6) break;
            case 3:
                bingo_6 = true;
                for (int i=0; i<3; i++) {
                    if (num6[i] != input[5+i]) {
                        bingo_6 = false;
                    }
                }
                if (bingo_6) break;
                bingo_6 = true;
                for (int i=0; i<3; i++) {
                    if (num5[5+i] != input[5+i]) {
                        bingo_6 = false;
                    }
                }
                if (bingo_6) break;
                bingo_6 = true;
                for (int i=0; i<3; i++) {
                    if (num4[5+i] != input[5+i]) {
                        bingo_6 = false;
                    }
                }
                if (bingo_6) break;
                bingo_6 = true;
                for (int i=0; i<3; i++) {
                    if (num3[5+i] != input[5+i]) {
                        bingo_6 = false;
                    }
                }
                break;
        }
        //test
        if (bingo_6) ((TextView)findViewById(R.id.numCheck_text)).setText("OK");
        else ((TextView)findViewById(R.id.numCheck_text)).setText("NNo");


         */
package com.nauk.coinfolio.Activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.daimajia.swipe.SwipeLayout;
import com.db.chart.tooltip.Tooltip;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.nauk.coinfolio.DataManagers.CurrencyData.Currency;
import com.nauk.coinfolio.DataManagers.CurrencyData.CurrencyDataChart;
import com.nauk.coinfolio.DataManagers.CurrencyData.Transaction;
import com.nauk.coinfolio.DataManagers.DatabaseManager;
import com.nauk.coinfolio.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**Create a Parcelable**/

public class CurrencyDetailsActivity extends AppCompatActivity {

    private ViewFlipper viewFlipper;
    private LinearLayout transactionLayout;
    private LinearLayout chartLayout;
    private DatabaseManager databaseManager;
    //private String symbol;
    private Currency currency;
    private boolean hasBeenModified;
    private Tooltip tip;
    private int indexMax;
    private int indexMin;
    private final static int HOUR = 0;
    private final static int DAY = 1;
    private final static int WEEK = 2;
    private final static int MONTH = 3;
    private final static int YEAR = 4;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    viewFlipper.setDisplayedChild(0);
                    return true;
                case R.id.navigation_dashboard:
                    viewFlipper.setDisplayedChild(1);
                    return true;
                case R.id.navigation_notifications:
                    viewFlipper.setDisplayedChild(2);
                    return true;
            }
            return false;
        }
    };

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("update", hasBeenModified);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency_details);

        Intent intent = getIntent();

        //symbol = intent.getStringExtra("symbol");
        currency = intent.getParcelableExtra("currency");

        databaseManager = new DatabaseManager(this);

        viewFlipper = findViewById(R.id.vfCurrencyDetails);
        transactionLayout = findViewById(R.id.listTransactions);
        chartLayout = findViewById(R.id.chartsLayout);

        drawTransactionList();

        initializeButtons();

        if(currency.getHistoryMinutes().size() > 0)
        {
            drawPriceChart(DAY, 1);
            drawVolumeChart(DAY, 1);
        }
        else
        {
            /*TextView errorTextView = new TextView(this);
            errorTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 750));
            errorTextView.setText("Not enough data");
            errorTextView.setTag("chart_layout");
            errorTextView.setGravity(Gravity.CENTER);

            chartLayout.addView(errorTextView, 0);*/
        }

        setTitle(" " + currency.getName());
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);

        Bitmap result = Bitmap.createBitmap(150, 150, currency.getIcon().getConfig());

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(this, R.color.white));

        Canvas canvas = new Canvas(result);
        canvas.drawCircle(result.getHeight()/2, result.getWidth()/2, 75, paint);
        canvas.drawBitmap(Bitmap.createScaledBitmap(currency.getIcon(), 100, 100, false), result.getHeight()/2 - 50, result.getWidth()/2 - 50, null);

        getSupportActionBar().setIcon(new BitmapDrawable(Bitmap.createScaledBitmap(result, 120, 120, false)));

        BottomNavigationView navigation = findViewById(R.id.navigation_details);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        hasBeenModified = false;
    }

    private void initializeButtons()
    {
        LinearLayout buttonLayout = findViewById(R.id.layoutChartButtons);

        for(int i = 0; i < buttonLayout.getChildCount(); i++)
        {
            final Button button = (Button) buttonLayout.getChildAt(i);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buttonEvent(v);
                }
            });
        }
    }

    private void buttonEvent(View v)
    {
        v.setEnabled(false);
        v.setElevation(convertDpToPx(8));

        LinearLayout buttonLayout = (LinearLayout) v.getParent();

        for(int i = 0; i < buttonLayout.getChildCount(); i++)
        {
            Button button = (Button) buttonLayout.getChildAt(i);

            if(button != v)
            {
                button.setEnabled(true);
                button.setElevation(convertDpToPx(2));
            }
        }

        chartEvent((Button) v);
    }

    private float convertDpToPx(float dp)
    {
        return dp * this.getResources().getDisplayMetrics().density;
    }

    private void chartEvent(Button button)
    {
        findViewById(R.id.chartPriceView).setVisibility(View.GONE);
        findViewById(R.id.chartVolumeView).setVisibility(View.GONE);
        findViewById(R.id.progressLayoutChart).setVisibility(View.VISIBLE);

        String interval = button.getText().toString().substring(button.getText().toString().length()-2);

        switch (interval)
        {
            case "1h":
                drawPriceChart(HOUR, 1);
                drawVolumeChart(HOUR, 1);
                break;
            case "3h":
                drawPriceChart(HOUR, 3);
                drawVolumeChart(HOUR, 3);
                break;
            case "1d":
                drawPriceChart(DAY, 1);
                drawVolumeChart(DAY, 1);
                break;
            case "3d":
                currency.updateHistoryHours(this, new Currency.CurrencyCallBack() {
                    @Override
                    public void onSuccess(Currency currency) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                drawPriceChart(CurrencyDetailsActivity.DAY, 3);
                                drawVolumeChart(CurrencyDetailsActivity.DAY, 3);
                            }
                        });
                    }
                });
                break;
            case "1w":
                currency.updateHistoryHours(this, new Currency.CurrencyCallBack() {
                    @Override
                    public void onSuccess(Currency currency) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                drawPriceChart(CurrencyDetailsActivity.WEEK, 1);
                                drawVolumeChart(CurrencyDetailsActivity.WEEK, 1);
                            }
                        });
                    }
                });
                break;
            case "1M":
                currency.updateHistoryHours(this, new Currency.CurrencyCallBack() {
                    @Override
                    public void onSuccess(Currency currency) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                drawPriceChart(CurrencyDetailsActivity.MONTH, 1);
                                drawVolumeChart(CurrencyDetailsActivity.MONTH, 1);
                            }
                        });
                    }
                });
                break;
            case "3M":
                currency.updateHistoryDays(this, new Currency.CurrencyCallBack() {
                    @Override
                    public void onSuccess(Currency currency) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                drawPriceChart(CurrencyDetailsActivity.MONTH, 3);
                                drawVolumeChart(CurrencyDetailsActivity.MONTH, 3);
                            }
                        });
                    }
                });
                break;
            case "6M":
                currency.updateHistoryDays(this, new Currency.CurrencyCallBack() {
                    @Override
                    public void onSuccess(Currency currency) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                drawPriceChart(CurrencyDetailsActivity.MONTH, 6);
                                drawVolumeChart(CurrencyDetailsActivity.MONTH, 6);
                            }
                        });
                    }
                });
                break;
            case "1y":
                currency.updateHistoryDays(this, new Currency.CurrencyCallBack() {
                    @Override
                    public void onSuccess(Currency currency) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                drawPriceChart(CurrencyDetailsActivity.YEAR, 1);
                                drawVolumeChart(CurrencyDetailsActivity.YEAR, 1);
                            }
                        });
                    }
                });
                break;
        }
    }

    private void drawVolumeChart(int timeUnit, int amout)
    {
        final BarChart barChart = findViewById(R.id.chartVolumeView);

        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.setDrawMarkers(true);
        barChart.setDoubleTapToZoomEnabled(true);
        barChart.setPinchZoom(true);
        barChart.setScaleEnabled(false);
        barChart.setDragEnabled(true);
        barChart.getDescription().setEnabled(false);
        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.getXAxis().setEnabled(false);
        barChart.setViewPortOffsets(0, 0, 0, 0);
        barChart.setFitBars(true);

        barChart.setData(generateVolumeChartSet(timeUnit, amout));
        barChart.invalidate();

        findViewById(R.id.chartVolumeView).setVisibility(View.VISIBLE);
    }

    private void drawPriceChart(int timeUnit, int amout)
    {
        final LineChart lineChart = findViewById(R.id.chartPriceView);
        final BarChart barChart = findViewById(R.id.chartVolumeView);

        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(false);
        lineChart.setDrawMarkers(true);
        lineChart.setDoubleTapToZoomEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setScaleEnabled(false);
        lineChart.setDragEnabled(true);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getXAxis().setEnabled(false);
        lineChart.setViewPortOffsets(0, 0, 0, 0);

        lineChart.setData(generatePriceChartSet(timeUnit, amout));
        lineChart.getAxisLeft().setAxisMinValue(lineChart.getData().getYMin());

        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                updateFluctuation(lineChart.getData().getDataSets().get(0).getEntryForIndex(0).getY(), e.getY());
                int index = lineChart.getData().getDataSets().get(0).getEntryIndex(e);
                barChart.highlightValue(barChart.getData().getDataSets().get(0).getEntryForIndex(index).getX(), 0, index);
                //((TextView) findViewById(R.id.timestampHightlight)).setText(getDate());
            }

            @Override
            public void onNothingSelected() {

            }
        });

        lineChart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_UP)
                {
                    lineChart.highlightValue(null);
                    updateFluctuation(lineChart.getData().getDataSets().get(0).getEntryForIndex(0).getY(), lineChart.getData().getDataSets().get(0).getEntryForIndex(lineChart.getData().getDataSets().get(0).getEntryCount() - 1).getY());
                    barChart.highlightValues(null);
                }
                return false;
            }
        });

        updateFluctuation(lineChart.getData().getDataSets().get(0).getEntryForIndex(0).getY(), lineChart.getData().getDataSets().get(0).getEntryForIndex(lineChart.getData().getDataSets().get(0).getEntryCount() - 1).getY());

        findViewById(R.id.chartPriceView).setVisibility(View.VISIBLE);
        findViewById(R.id.progressLayoutChart).setVisibility(View.GONE);
    }

    private String getDate(long timeStamp){

        try{
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date netDate = (new Date(timeStamp));
            return sdf.format(netDate);
        }
        catch(Exception ex){
            return "xx";
        }
    }

    private BarData generateVolumeChartSet(int timeUnit, int amount)
    {
        BarDataSet dataSet;
        List<CurrencyDataChart> dataChartList = new ArrayList<>();
        ArrayList<BarEntry> values = new ArrayList<>();

        switch (timeUnit)
        {
            case HOUR:
                dataChartList = currency.getHistoryMinutes().subList(currency.getHistoryMinutes().size()-(60*amount), currency.getHistoryMinutes().size());
                break;
            case DAY:
                if(amount == 1)
                {
                    dataChartList = currency.getHistoryMinutes();
                }
                else
                {
                    dataChartList = currency.getHistoryHours().subList(currency.getHistoryHours().size()-(24*amount), currency.getHistoryHours().size());
                }
                break;
            case WEEK:
                dataChartList = currency.getHistoryHours().subList(currency.getHistoryHours().size()-168, currency.getHistoryHours().size());
                break;
            case MONTH:
                switch (amount)
                {
                    case 1:
                        dataChartList = currency.getHistoryHours();
                        break;
                    case 3:
                        dataChartList = currency.getHistoryDays().subList(currency.getHistoryDays().size()-93, currency.getHistoryDays().size());
                        break;
                    case 6:
                        dataChartList = currency.getHistoryDays().subList(currency.getHistoryDays().size()-186, currency.getHistoryDays().size());
                        break;
                }
                break;
            case YEAR:
                dataChartList = currency.getHistoryDays();
                break;
        }

        int offset = (int) Math.floor(dataChartList.size() / 200);

        if(offset < 1)
        {
            offset = 1;
        }

        for(int i = 0, j = 0; i < dataChartList.size(); i += offset, j++)
        {
            values.add(new BarEntry(j, (float) dataChartList.get(i).getVolumeFrom()));
        }

        dataSet = new BarDataSet(values, "Volume");
        dataSet.setDrawIcons(false);
        dataSet.setColor(Color.GRAY);
        dataSet.setDrawValues(false);
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(currency.getChartColor());

        return new BarData(dataSet);
    }

    private LineData generatePriceChartSet(int timeUnit, int amount)
    {
        LineDataSet dataSet;
        List<CurrencyDataChart> dataChartList = new ArrayList<>();
        ArrayList<Entry> values = new ArrayList<>();

        int counter = 0;
        Calendar calendar = Calendar.getInstance(Locale.FRANCE);
        String hour;
        String minute;
        String dayName = "";
        String dayNumber;
        String monthName = "";
        String monthNumber;
        int offset = 10;
        int pointFormat = HOUR;

        switch (timeUnit)
        {
            case HOUR:
                dataChartList = currency.getHistoryMinutes().subList(currency.getHistoryMinutes().size()-(60*amount), currency.getHistoryMinutes().size());
                offset = 10 * amount;
                pointFormat = HOUR;
                break;
            case DAY:
                if(amount == 1)
                {
                    dataChartList = currency.getHistoryMinutes();
                    offset = 10 * 24;
                    pointFormat = HOUR;
                }
                else
                {
                    dataChartList = currency.getHistoryHours().subList(currency.getHistoryHours().size()-(24*amount), currency.getHistoryHours().size());
                    offset = 24;
                    pointFormat = DAY;
                }
                break;
            case WEEK:
                dataChartList = currency.getHistoryHours().subList(currency.getHistoryHours().size()-168, currency.getHistoryHours().size());
                offset = 28;
                pointFormat = DAY;
                break;
            case MONTH:
                switch (amount)
                {
                    case 1:
                        dataChartList = currency.getHistoryHours();
                        Log.d("coinfolio", "1 month");
                        offset = 124;
                        pointFormat = MONTH;
                        break;
                    case 3:
                        dataChartList = currency.getHistoryDays().subList(currency.getHistoryDays().size()-93, currency.getHistoryDays().size());
                        offset = 15;
                        pointFormat = MONTH;
                        break;
                    case 6:
                        dataChartList = currency.getHistoryDays().subList(currency.getHistoryDays().size()-186, currency.getHistoryDays().size());
                        offset = 31;
                        pointFormat = MONTH;
                        break;
                }
                break;
            case YEAR:
                dataChartList = currency.getHistoryDays();
                offset = 30;
                pointFormat = YEAR;
                break;
        }

        /*for(int i = 0; i < dataChartList.size(); i++)
        {*/
            /*if(counter == offset)
            {
                calendar.setTimeInMillis(dataChartList.get(i).getTimestamp()*1000);

                switch (pointFormat)
                {
                    case HOUR:
                        hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
                        minute = String.valueOf(calendar.get(Calendar.MINUTE));

                        if(hour.length() < 2)
                        {
                            hour = "0" + hour;
                        }

                        if(minute.length() < 2)
                        {
                            minute = "0" + minute;
                        }

                        lineSet.addPoint(hour + ":" + minute, (float) dataChartList.get(i).getOpen());
                        break;
                    case DAY:
                        int dayIndex = calendar.get(Calendar.DAY_OF_WEEK)+1;

                        switch (dayIndex)
                        {
                            case Calendar.MONDAY:
                                dayName = "Mon";
                                break;
                            case Calendar.TUESDAY:
                                dayName = "Tue";
                                break;
                            case Calendar.WEDNESDAY:
                                dayName = "Wed";
                                break;
                            case Calendar.THURSDAY:
                                dayName = "Thu";
                                break;
                            case Calendar.FRIDAY:
                                dayName = "Fri";
                                break;
                            case Calendar.SATURDAY:
                                dayName = "Sat";
                                break;
                            case Calendar.SUNDAY:
                                dayName = "Sun";
                                break;
                        }

                        lineSet.addPoint(dayName, (float) dataChartList.get(i).getOpen());
                        break;
                    case MONTH:
                        dayNumber = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)+1);
                        monthNumber = String.valueOf(calendar.get(Calendar.MONTH)+1);

                        if(dayNumber.length() < 2)
                        {
                            dayNumber = '0' + dayNumber;
                        }

                        if(monthNumber.length() < 2)
                        {
                            monthNumber = '0' + monthNumber;
                        }

                        lineSet.addPoint(dayNumber + "/" + monthNumber, (float) dataChartList.get(i).getOpen());
                        break;
                    case YEAR:
                        int mb = calendar.get(Calendar.MONTH);

                        switch (mb)
                        {
                            case Calendar.JANUARY:
                                monthName = "Jan";
                                break;
                            case Calendar.FEBRUARY:
                                monthName = "Feb";
                                break;
                            case Calendar.MARCH:
                                monthName = "Mar";
                                break;
                            case Calendar.APRIL:
                                monthName = "Apr";
                                break;
                            case Calendar.MAY:
                                monthName = "May";
                                break;
                            case Calendar.JUNE:
                                monthName = "Jun";
                                break;
                            case Calendar.JULY:
                                monthName = "Jul";
                                break;
                            case Calendar.AUGUST:
                                monthName = "Aug";
                                break;
                            case Calendar.SEPTEMBER:
                                monthName = "Sep";
                                break;
                            case Calendar.OCTOBER:
                                monthName = "Oct";
                                break;
                            case Calendar.NOVEMBER:
                                monthName = "Nov";
                                break;
                            case Calendar.DECEMBER:
                                monthName = "Dec";
                                break;
                        }

                        lineSet.addPoint(monthName, (float) dataChartList.get(i).getOpen());
                        break;
                }
                counter = 0;
            }
            else
            {
                counter++;
                lineSet.addPoint("", (float) dataChartList.get(i).getOpen());
            }*/
            /*values.add(new Entry(i, (float) dataChartList.get(i).getOpen()));
        }*/

        int offsetRange = (int) Math.floor(dataChartList.size() / 200);

        if(offsetRange < 1)
        {
            offsetRange = 1;
        }

        for(int i = 0, j = 0; i < dataChartList.size(); i += offsetRange, j++)
        {
            values.add(new Entry(j, (float) dataChartList.get(i).getOpen()));
        }

        dataSet = new LineDataSet(values, "History");
        dataSet.setDrawIcons(false);
        dataSet.setColor(currency.getChartColor());
        dataSet.setLineWidth(1);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getColorWithAlpha(currency.getChartColor(), 0.5f));
        dataSet.setFormLineWidth(1);
        dataSet.setFormSize(15);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setHighlightEnabled(true);
        dataSet.setDrawHorizontalHighlightIndicator(false);
        dataSet.setHighLightColor(currency.getChartColor());

        Drawable fillDrawable = ContextCompat.getDrawable(this, R.drawable.linear_chart_gradient);
        fillDrawable.setColorFilter(getColorWithAlpha(currency.getChartColor(), 0.5f), PorterDuff.Mode.SRC_ATOP);
        //dataSet.setFillDrawable(fillDrawable);

        return new LineData(dataSet);
    }

    private void updateFluctuation(float start, float end)
    {
        float fluctuation = end - start;
        float percentageFluctuation = (float) (fluctuation / start * 100);

        if(percentageFluctuation < 0)
        {
            ((TextView) findViewById(R.id.txtViewPercentage)).setTextColor(getResources().getColor(R.color.red));
        }
        else
        {
            ((TextView) findViewById(R.id.txtViewPercentage)).setTextColor(getResources().getColor(R.color.green));
        }

        ((TextView) findViewById(R.id.txtViewPriceStart)).setText("$" + start);
        ((TextView) findViewById(R.id.txtViewPriceNow)).setText("$" + end);
        ((TextView) findViewById(R.id.txtViewPercentage)).setText(percentageFluctuation + "%");
    }

    /*private LineSet generatePriceChartSet(int timeUnit, int amount)
    {
        List<CurrencyDataChart> dataChartList = new ArrayList<>();
        LineSet lineSet = new LineSet();
        int counter = 0;
        Calendar calendar = Calendar.getInstance(Locale.FRANCE);
        String hour;
        String minute;
        String dayName = "";
        String dayNumber;
        String monthName = "";
        String monthNumber;
        int offset = 10;
        int pointFormat = HOUR;

        switch (timeUnit)
        {
            case HOUR:
                dataChartList = currency.getHistoryMinutes().subList(currency.getHistoryMinutes().size()-(60*amount), currency.getHistoryMinutes().size());
                offset = 10 * amount;
                pointFormat = HOUR;
                break;
            case DAY:
                if(amount == 1)
                {
                    dataChartList = currency.getHistoryMinutes();
                    offset = 10 * 24;
                    pointFormat = HOUR;
                }
                else
                {
                    dataChartList = currency.getHistoryHours().subList(currency.getHistoryHours().size()-(24*amount), currency.getHistoryHours().size());
                    offset = 24;
                    pointFormat = DAY;
                }
                break;
            case WEEK:
                dataChartList = currency.getHistoryHours().subList(currency.getHistoryHours().size()-168, currency.getHistoryHours().size());
                offset = 28;
                pointFormat = DAY;
                break;
            case MONTH:
                switch (amount)
                {
                    case 1:
                        dataChartList = currency.getHistoryHours();
                        offset = 124;
                        pointFormat = MONTH;
                        break;
                    case 3:
                        dataChartList = currency.getHistoryDays().subList(currency.getHistoryDays().size()-93, currency.getHistoryDays().size());
                        offset = 15;
                        pointFormat = MONTH;
                        break;
                    case 6:
                        dataChartList = currency.getHistoryDays().subList(currency.getHistoryDays().size()-186, currency.getHistoryDays().size());
                        offset = 31;
                        pointFormat = MONTH;
                        break;
                }
                break;
            case YEAR:
                dataChartList = currency.getHistoryDays();
                offset = 30;
                pointFormat = YEAR;
                break;
        }

        for(int i = 0; i < dataChartList.size(); i++)
        {
            if(counter == offset)
            {
                calendar.setTimeInMillis(dataChartList.get(i).getTimestamp()*1000);

                switch (pointFormat)
                {
                    case HOUR:
                        hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
                        minute = String.valueOf(calendar.get(Calendar.MINUTE));

                        if(hour.length() < 2)
                        {
                            hour = "0" + hour;
                        }

                        if(minute.length() < 2)
                        {
                            minute = "0" + minute;
                        }

                        lineSet.addPoint(hour + ":" + minute, (float) dataChartList.get(i).getOpen());
                        break;
                    case DAY:
                        int dayIndex = calendar.get(Calendar.DAY_OF_WEEK)+1;

                        switch (dayIndex)
                        {
                            case Calendar.MONDAY:
                                dayName = "Mon";
                                break;
                            case Calendar.TUESDAY:
                                dayName = "Tue";
                                break;
                            case Calendar.WEDNESDAY:
                                dayName = "Wed";
                                break;
                            case Calendar.THURSDAY:
                                dayName = "Thu";
                                break;
                            case Calendar.FRIDAY:
                                dayName = "Fri";
                                break;
                            case Calendar.SATURDAY:
                                dayName = "Sat";
                                break;
                            case Calendar.SUNDAY:
                                dayName = "Sun";
                                break;
                        }

                        lineSet.addPoint(dayName, (float) dataChartList.get(i).getOpen());
                        break;
                    case MONTH:
                        dayNumber = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)+1);
                        monthNumber = String.valueOf(calendar.get(Calendar.MONTH)+1);

                        if(dayNumber.length() < 2)
                        {
                            dayNumber = '0' + dayNumber;
                        }

                        if(monthNumber.length() < 2)
                        {
                            monthNumber = '0' + monthNumber;
                        }

                        lineSet.addPoint(dayNumber + "/" + monthNumber, (float) dataChartList.get(i).getOpen());
                        break;
                    case YEAR:
                        int mb = calendar.get(Calendar.MONTH);

                        switch (mb)
                        {
                            case Calendar.JANUARY:
                                monthName = "Jan";
                                break;
                            case Calendar.FEBRUARY:
                                monthName = "Feb";
                                break;
                            case Calendar.MARCH:
                                monthName = "Mar";
                                break;
                            case Calendar.APRIL:
                                monthName = "Apr";
                                break;
                            case Calendar.MAY:
                                monthName = "May";
                                break;
                            case Calendar.JUNE:
                                monthName = "Jun";
                                break;
                            case Calendar.JULY:
                                monthName = "Jul";
                                break;
                            case Calendar.AUGUST:
                                monthName = "Aug";
                                break;
                            case Calendar.SEPTEMBER:
                                monthName = "Sep";
                                break;
                            case Calendar.OCTOBER:
                                monthName = "Oct";
                                break;
                            case Calendar.NOVEMBER:
                                monthName = "Nov";
                                break;
                            case Calendar.DECEMBER:
                                monthName = "Dec";
                                break;
                        }

                        lineSet.addPoint(monthName, (float) dataChartList.get(i).getOpen());
                        break;
                }
                counter = 0;
            }
            else
            {
                counter++;
                lineSet.addPoint("", (float) dataChartList.get(i).getOpen());
            }
        }

        lineSet.setSmooth(true);
        lineSet.setThickness(3);
        lineSet.setFill(getColorWithAlpha(currency.getChartColor(), 0.5f));
        lineSet.setColor(currency.getChartColor());

        return lineSet;
    }*/

    private int getColorWithAlpha(int color, float ratio)
    {
        int transColor;
        int alpha = Math.round(Color.alpha(color) * ratio);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        transColor = Color.argb(alpha, r, g, b);

        return transColor;
    }

    private void drawTransactionList()
    {
        transactionLayout.removeAllViews();

        List<Transaction> transactionList = databaseManager.getCurrencyTransactions(currency.getSymbol());

        for(int i = 0; i < transactionList.size(); i++)
        {
            View view = LayoutInflater.from(this).inflate(R.layout.custom_transaction_row, null);
            TextView amountTxtView = view.findViewById(R.id.amountPurchased);
            TextView valueTxtView = view.findViewById(R.id.puchasedValue);
            TextView dateTxtView = view.findViewById(R.id.purchaseDate);

            LinearLayout deleteLayout = view.findViewById(R.id.deleteTransactionLayout);
            deleteLayout.setTag(transactionList.get(i).getTransactionId());

            deleteLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    databaseManager.deleteTransactionFromId(Integer.parseInt(view.getTag().toString()));
                    drawTransactionList();
                    hasBeenModified = true;
                }
            });

            amountTxtView.setText(transactionList.get(i).getAmount() + "");

            SwipeLayout swipeLayout =  view.findViewById(R.id.swipeLayout);

            //set show mode.
            swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);

            //add drag edge.(If the BottomView has 'layout_gravity' attribute, this line is unnecessary)
            swipeLayout.addDrag(SwipeLayout.DragEdge.Left, view.findViewById(R.id.bottom_wrapper));

            swipeLayout.addSwipeListener(new SwipeLayout.SwipeListener() {
                @Override
                public void onClose(SwipeLayout layout) {
                    //when the SurfaceView totally cover the BottomView.
                }

                @Override
                public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {
                    //you are swiping.
                }

                @Override
                public void onStartOpen(SwipeLayout layout) {

                }

                @Override
                public void onOpen(SwipeLayout layout) {
                    //when the BottomView totally show.
                }

                @Override
                public void onStartClose(SwipeLayout layout) {

                }

                @Override
                public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {
                    //when user's hand released.
                }
            });

            transactionLayout.addView(view);
        }

    }

}

package com.herbron.moodl.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.herbron.moodl.Activities.RecordTransactionFragments.BuyFragment;
import com.herbron.moodl.Activities.RecordTransactionFragments.SellFragment;
import com.herbron.moodl.CurrencyInfoUpdateNotifierInterface;
import com.herbron.moodl.CustomAdapters.PairRecordListAdapter;
import com.herbron.moodl.CustomLayouts.CustomRecordFragment;
import com.herbron.moodl.CustomLayouts.CustomViewPager;
import com.herbron.moodl.DataManagers.CurrencyData.Currency;
import com.herbron.moodl.DataManagers.CurrencyData.Transaction;
import com.herbron.moodl.DataManagers.ExchangeManager.Exchange;
import com.herbron.moodl.DataManagers.InfoAPIManagers.CryptocompareApiManager;
import com.herbron.moodl.DataManagers.DatabaseManager;
import com.herbron.moodl.DataManagers.InfoAPIManagers.Pair;
import com.herbron.moodl.DataNotifiers.CryptocompareNotifierInterface;
import com.herbron.moodl.CustomAdapters.CoinRecordListAdapter;
import com.herbron.moodl.CustomLayouts.CustomTabLayout;
import com.herbron.moodl.CustomAdapters.ExchangeRecordListAdapter;
import com.herbron.moodl.CustomAdapters.RecordTransactionPageAdapter;
import com.herbron.moodl.MoodlBox;
import com.herbron.moodl.DataNotifiers.MoodlboxNotifierInterface;
import com.herbron.moodl.Utils.PlaceholderUtils;
import com.herbron.moodl.R;

import java.util.ArrayList;
import java.util.List;

public class RecordTransactionActivity extends AppCompatActivity implements CurrencyInfoUpdateNotifierInterface, CryptocompareNotifierInterface {

    private Toolbar toolbar;
    private ImageView currencyIconImageView;

    private Currency currency;
    private Exchange exchange;
    private Pair pair;

    private CryptocompareApiManager cryptocompareApiManager;

    private AutoCompleteTextView coin_autoCompleteTextView;
    private AutoCompleteTextView exchange_autoCompleteTextView;
    private AutoCompleteTextView pair_autoCompleteTextView;

    private CustomTabLayout tabLayout;
    private CustomViewPager viewPager;

    private RecordTransactionPageAdapter pageAdapter;

    private Animation revealAnimation;
    private Animation dismissAnimation;

    private LinearLayout globalTabLayouts;

    private boolean isGlobalLayoutVisible;

    private Intent callingIntent;

    private TextWatcher coinTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            currencyIconImageView.setImageBitmap(null);
            exchange_autoCompleteTextView.setEnabled(false);
            exchange_autoCompleteTextView.setText("");

            ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(2).setEnabled(false);
            ((TextView) tabLayout.getTabAt(2).getCustomView()).setTextColor(getResources().getColor(R.color.separationColor));

            if(isGlobalLayoutVisible && globalTabLayouts.getAnimation().hasEnded())
            {
                globalTabLayouts.startAnimation(dismissAnimation);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_transaction);

        toolbar = findViewById(R.id.toolbar);

        currencyIconImageView = findViewById(R.id.currencyIconImageView);
        cryptocompareApiManager = CryptocompareApiManager.getInstance(this);

        cryptocompareApiManager.addListener(this);

        cryptocompareApiManager.updateExchangeList();

        coin_autoCompleteTextView = findViewById(R.id.coin_autoCompleteTextView);
        exchange_autoCompleteTextView = findViewById(R.id.exchange_autoCompleteTextView);
        pair_autoCompleteTextView = findViewById(R.id.pair_autoCompleteTextView);

        setSupportActionBar(toolbar);

        setupTabLayout();

        setupCoinAutoCompleteTextView();

        setupExchangeAutoCompleteTextView();

        setupPairAutoCompleteTextView();

        setupBackButton();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(callingIntent != null)
        {
            callingIntent.removeExtra("transactionId");
        }
    }

    private void checkCallingIntent()
    {
        callingIntent = getIntent();
        int transactionId = callingIntent.getIntExtra("transactionId", -1);

        if(transactionId != -1)
        {
            List<Exchange> exchangeList;
            List<Pair> pairList;
            DatabaseManager databaseManager = new DatabaseManager(getBaseContext());
            Transaction transaction = databaseManager.getCurrencyTransactionById(transactionId);
            List<Currency> denominationList = cryptocompareApiManager.getCurrenciesDenomination();

            boolean found = false;
            int index = 0;

            while(index < denominationList.size() && !found)
            {
                if(denominationList.get(index).getSymbol().equals(transaction.getSymbol()))
                {
                    currency = denominationList.get(index);
                    found = true;

                    currency.setListener(RecordTransactionActivity.this);
                    updateExchangeAdapter(currency.getSymbol());
                    exchange_autoCompleteTextView.setEnabled(true);
                    IconDownloaderTask iconDownloaderTask = new IconDownloaderTask();
                    iconDownloaderTask.execute();
                    coin_autoCompleteTextView.removeTextChangedListener(coinTextWatcher);
                    coin_autoCompleteTextView.setText(PlaceholderUtils.getDenomination(currency.getName(), currency.getSymbol(), getBaseContext()));
                    coin_autoCompleteTextView.setEnabled(false);

                    if(globalTabLayouts.getVisibility() == View.GONE)
                    {
                        globalTabLayouts.setVisibility(View.VISIBLE);
                    }

                    globalTabLayouts.startAnimation(revealAnimation);

                    isGlobalLayoutVisible = true;

                    updateCurrencyData();
                }

                index++;
            }

            found = false;
            index = 0;

            switch (transaction.getType())
            {
                case "b":
                    exchangeList = cryptocompareApiManager.getExchangeList(currency.getSymbol());

                    while(index < exchangeList.size() && !found)
                    {
                        if(exchangeList.get(index).getName().equals(transaction.getSource()))
                        {
                            exchange = exchangeList.get(index);

                            exchange_autoCompleteTextView.setText(exchange.getName());
                            exchange_autoCompleteTextView.setEnabled(true);

                            updateExchangeData();

                            updatePairAdapter();
                            found = true;
                        }

                        index++;
                    }

                    pairList = exchange.getPairsFor(currency.getSymbol());

                    found = false;
                    index = 0;

                    while(index < pairList.size() && !found)
                    {
                        if(pairList.get(index).contains(currency.getSymbol()) && pairList.get(index).contains(transaction.getSymPair()))
                        {
                            pair = pairList.get(index);

                            pair_autoCompleteTextView.setText(PlaceholderUtils.getPairString(pair.getFrom(), pair.getTo(), getBaseContext()));
                            pair_autoCompleteTextView.setEnabled(true);

                            ((BuyFragment) pageAdapter.getItem(0)).updatePair(pair);

                            //updatePairData();

                            found = true;
                        }

                        index++;
                    }

                    tabLayout.getTabAt(0).select();
                    ((TextView) tabLayout.getTabAt(0).getCustomView()).setTextColor(getResources().getColor(R.color.white));

                    break;
                case "s":
                    exchangeList = cryptocompareApiManager.getExchangeList(currency.getSymbol());

                    while(index < exchangeList.size() && !found)
                    {
                        if(exchangeList.get(index).getName().equals(transaction.getSource()))
                        {
                            exchange = exchangeList.get(index);

                            exchange_autoCompleteTextView.setText(exchange.getName());
                            exchange_autoCompleteTextView.setEnabled(true);

                            updateExchangeData();

                            updatePairAdapter();
                            found = true;
                        }

                        index++;
                    }

                    pairList = exchange.getPairsFor(currency.getSymbol());

                    found = false;
                    index = 0;

                    while(index < pairList.size() && !found)
                    {
                        if(pairList.get(index).contains(currency.getSymbol()) && pairList.get(index).contains(transaction.getSymPair()))
                        {
                            pair = pairList.get(index);

                            pair_autoCompleteTextView.setText(PlaceholderUtils.getPairString(pair.getFrom(), pair.getTo(), getBaseContext()));
                            pair_autoCompleteTextView.setEnabled(true);

                            ((SellFragment) pageAdapter.getItem(1)).updatePair(pair);

                            //updatePairData();

                            found = true;
                        }

                        index++;
                    }

                    tabLayout.getTabAt(1).select();
                    ((TextView) tabLayout.getTabAt(1).getCustomView()).setTextColor(getResources().getColor(R.color.white));
                    break;
                case "t":
                    tabLayout.getTabAt(2).select();
                    ((TextView) tabLayout.getTabAt(2).getCustomView()).setTextColor(getResources().getColor(R.color.white));
                    break;
            }
        }
    }

    public Currency getCurrency()
    {
        return currency;
    }

    private void setupTabLayout()
    {
        globalTabLayouts = findViewById(R.id.globalTabLayouts);

        tabLayout = findViewById(R.id.transactionsTabLayout);
        tabLayout.addTab(0, getResources().getString(R.string.buyText));
        tabLayout.addTab(1, getResources().getString(R.string.sellText));
        tabLayout.addTab(2, getResources().getString(R.string.transferText));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        viewPager = findViewById(R.id.transactionsViewPager);
        pageAdapter = new RecordTransactionPageAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(pageAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        viewPager.setPagingEnabled(false);
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        LinearLayout tabLayoutChildren = (LinearLayout) tabLayout.getChildAt(0);

        for(int i = 0; i < tabLayoutChildren.getChildCount(); i++)
        {
            tabLayoutChildren.getChildAt(i).setEnabled(false);
            ((TextView) tabLayout.getTabAt(i).getCustomView()).setTextColor(getResources().getColor(R.color.separationColor));
        }

        revealAnimation = AnimationUtils.loadAnimation(this, R.anim.reveal);
        dismissAnimation = AnimationUtils.loadAnimation(this, R.anim.dismiss);

        dismissAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isGlobalLayoutVisible = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void setupPairAutoCompleteTextView()
    {
        pair_autoCompleteTextView.setThreshold(0);
        pair_autoCompleteTextView.setTextColor(getResources().getColor(R.color.white));

        pair_autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(0).setEnabled(false);
                ((TextView) tabLayout.getTabAt(0).getCustomView()).setTextColor(getResources().getColor(R.color.separationColor));
                ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(1).setEnabled(false);
                ((TextView) tabLayout.getTabAt(1).getCustomView()).setTextColor(getResources().getColor(R.color.separationColor));
                tabLayout.getTabAt(2).select();

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        pair_autoCompleteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pair_autoCompleteTextView.showDropDown();
            }
        });

        pair_autoCompleteTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    pair_autoCompleteTextView.showDropDown();
                }
                else
                {
                    pair_autoCompleteTextView.dismissDropDown();
                }
            }
        });

        pair_autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                pair = (Pair) pair_autoCompleteTextView.getAdapter().getItem(position);

                pair_autoCompleteTextView.setText(PlaceholderUtils.getPairString(pair.getFrom(), pair.getTo(), getBaseContext()));
                toolbar.requestFocus();
                hideSoftKeyboard(RecordTransactionActivity.this);

                updatePairData();

                ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(0).setEnabled(true);
                ((TextView) tabLayout.getTabAt(0).getCustomView()).setTextColor(getResources().getColor(R.color.white));
                ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(1).setEnabled(true);
                ((TextView) tabLayout.getTabAt(1).getCustomView()).setTextColor(getResources().getColor(R.color.white));
                tabLayout.getTabAt(0).select();
            }
        });
    }

    private void setupExchangeAutoCompleteTextView()
    {
        exchange_autoCompleteTextView.setThreshold(0);
        exchange_autoCompleteTextView.setTextColor(getResources().getColor(R.color.white));

        exchange_autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                pair_autoCompleteTextView.setEnabled(false);
                pair_autoCompleteTextView.setText("");

                ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(0).setEnabled(false);
                ((TextView) tabLayout.getTabAt(0).getCustomView()).setTextColor(getResources().getColor(R.color.separationColor));
                ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(1).setEnabled(false);
                ((TextView) tabLayout.getTabAt(1).getCustomView()).setTextColor(getResources().getColor(R.color.separationColor));
                tabLayout.getTabAt(2).select();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        exchange_autoCompleteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exchange_autoCompleteTextView.showDropDown();
            }
        });

        exchange_autoCompleteTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    exchange_autoCompleteTextView.showDropDown();
                }
                else
                {
                    exchange_autoCompleteTextView.dismissDropDown();
                }
            }
        });

        exchange_autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                exchange = (Exchange) exchange_autoCompleteTextView.getAdapter().getItem(position);

                exchange_autoCompleteTextView.setText(exchange.getName());
                toolbar.requestFocus();
                hideSoftKeyboard(RecordTransactionActivity.this);

                updatePairAdapter();
                pair_autoCompleteTextView.setEnabled(true);

                updateExchangeData();
            }
        });
    }

    private void updatePairAdapter()
    {
        PairRecordListAdapter pairAdapter = new PairRecordListAdapter(this, android.R.layout.simple_list_item_1, new ArrayList<>(exchange.getPairsFor(currency.getSymbol())));

        pair_autoCompleteTextView.setAdapter(pairAdapter);
    }

    private void updateExchangeAdapter(String symbol)
    {
        ExchangeRecordListAdapter exchangeAdapter = new ExchangeRecordListAdapter(this, android.R.layout.simple_list_item_1, new ArrayList<>(cryptocompareApiManager.getExchangeList(symbol)));

        exchange_autoCompleteTextView.setAdapter(exchangeAdapter);
    }

    private void setupCoinAutoCompleteTextView()
    {
        CoinRecordListAdapter adapter = new CoinRecordListAdapter(getBaseContext(), R.layout.custom_summary_coin_row, new ArrayList<>(cryptocompareApiManager.getCurrenciesDenomination()));

        coin_autoCompleteTextView.setThreshold(0);
        coin_autoCompleteTextView.setAdapter(adapter);
        coin_autoCompleteTextView.setTextColor(getResources().getColor(R.color.white));
        coin_autoCompleteTextView.addTextChangedListener(coinTextWatcher);

        coin_autoCompleteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                coin_autoCompleteTextView.showDropDown();
            }
        });

        coin_autoCompleteTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    coin_autoCompleteTextView.showDropDown();
                }
                else
                {
                    coin_autoCompleteTextView.dismissDropDown();
                }
            }
        });

        coin_autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currency = (Currency) coin_autoCompleteTextView.getAdapter().getItem(position);

                coin_autoCompleteTextView.setText(PlaceholderUtils.getDenomination(currency.getName(), currency.getSymbol(), getBaseContext()));
                toolbar.requestFocus();
                hideSoftKeyboard(RecordTransactionActivity.this);

                currency.setListener(RecordTransactionActivity.this);

                updateExchangeAdapter(currency.getSymbol());
                exchange_autoCompleteTextView.setEnabled(true);

                IconDownloaderTask iconDownloaderTask = new IconDownloaderTask();
                iconDownloaderTask.execute();

                ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(0).setEnabled(false);
                ((TextView) tabLayout.getTabAt(0).getCustomView()).setTextColor(getResources().getColor(R.color.separationColor));
                ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(1).setEnabled(false);
                ((TextView) tabLayout.getTabAt(1).getCustomView()).setTextColor(getResources().getColor(R.color.separationColor));
                ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(2).setEnabled(true);
                ((TextView) tabLayout.getTabAt(2).getCustomView()).setTextColor(getResources().getColor(R.color.white));
                tabLayout.getTabAt(2).select();

                updateCurrencyData();

                if(globalTabLayouts.getVisibility() == View.GONE)
                {
                    globalTabLayouts.setVisibility(View.VISIBLE);
                }

                globalTabLayouts.startAnimation(revealAnimation);

                isGlobalLayoutVisible = true;
            }
        });
    }

    private void updateCurrencyData()
    {
        for(int i = 0; i < pageAdapter.getCount(); i++)
        {
            ((CustomRecordFragment) pageAdapter.getItem(i)).setCurrency(currency);
        }
    }

    private void updateExchangeData()
    {
        for(int i = 0; i < pageAdapter.getCount(); i++)
        {
            ((CustomRecordFragment) pageAdapter.getItem(i)).setExchange(exchange);
        }
    }

    private void updatePairData()
    {
        for(int i = 0; i < pageAdapter.getCount(); i++)
        {
            ((CustomRecordFragment) pageAdapter.getItem(i)).setPair(pair);
        }
    }

    @Override
    public void onTimestampPriceUpdated(String price) {
        //purchasedPriceEditText.setText(price);
    }

    @Override
    public void onHistoryDataUpdated() {

    }

    @Override
    public void onPriceUpdated(Currency currency) {

    }

    @Override
    public void onDetailsUpdated() {

    }

    @Override
    public void onExchangesUpdated() {
        checkCallingIntent();
    }

    private class IconDownloaderTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            String iconUrl = MoodlBox.getIconUrl(currency.getSymbol(), 500, cryptocompareApiManager);

            if(iconUrl != null)
            {
                MoodlBox.getBitmapFromURL(iconUrl, currency.getSymbol(), getResources(), getBaseContext(), new MoodlboxNotifierInterface() {
                    @Override
                    public void onBitmapDownloaded(Bitmap bitmapIcon) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                currencyIconImageView.setImageBitmap(bitmapIcon);
                            }
                        });

                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currencyIconImageView.setBackground(getResources().getDrawable(R.mipmap.ic_launcher_moodl));
                    }
                });
            }
            return null;
        }
    }

        /*if(transactionId != -1)
        {
            setTitle(PlaceholderUtils.getEditTransactionString(coin, getBaseContext()));

            DatabaseManager databaseManager = new DatabaseManager(this);
            Transaction transaction = databaseManager.getCurrencyTransactionById(transactionId);

            symbolTxtView.setText(transaction.getSymbol());
            amountTxtView.setText(String.valueOf(transaction.getAmount()));
            purchaseDate.setText(sdf.format(transaction.getTimestamp()));
            feesTxtView.setText(String.valueOf(transaction.getFees()));
        }
        else
        {
            setTitle(getString(R.string.new_transaction));

            purchaseDate.setText(sdf.format(calendar.getTime()));
            symbolTxtView.setText(symbol);
            feesTxtView.setText(String.valueOf(0));
        }

        currency = new Currency(coin, symbol);

        purchasedDateLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createDatePicker();
            }
        });

        purchaseDate.setKeyListener(null);

        purchaseDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createDatePicker();
            }
        });

        //initializeButtons();

        currency.getTimestampPrice(this, preferenceManager.getDefaultCurrency(), new Currency.PriceCallBack() {
            @Override
            public void onSuccess(String price) {
                purchasedPriceEditText.setText(price);
            }
        }, calendar.getTimeInMillis() / 1000);*/

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    private void setupBackButton()
    {
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}

package com.herbron.moodl.Activities.RecordTransactionFragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TimePicker;

import com.herbron.moodl.Activities.HomeActivity;
import com.herbron.moodl.CustomLayouts.CustomRecordFragment;
import com.herbron.moodl.DataManagers.CurrencyData.Currency;
import com.herbron.moodl.DataManagers.CurrencyData.Transaction;
import com.herbron.moodl.DataManagers.DatabaseManager;
import com.herbron.moodl.DataManagers.ExchangeManager.Exchange;
import com.herbron.moodl.DataManagers.InfoAPIManagers.Pair;
import com.herbron.moodl.DataManagers.PreferencesManager;
import com.herbron.moodl.Utils.PlaceholderUtils;
import com.herbron.moodl.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BuyFragment extends CustomRecordFragment {

    private TextInputEditText amoutEditText;
    private TextInputEditText buyPriceEditText;
    private TextInputEditText buyDateEditText;
    private TextInputEditText totalValueEditText;
    private TextInputEditText fees_editText;
    private TextInputEditText note_editText;
    private AppCompatButton saveBuyButton;
    private static Spinner feesCurrencySpinner;
    private static View view;
    private Switch deductHoldingsSwitch;

    private ArrayAdapter<String> currencyFeeAdapter;

    private SimpleDateFormat sdf;
    private Calendar calendar;
    private PreferencesManager preferenceManager;

    private static Context context;

    private static Currency fragmentCurrency;
    private static Exchange fragmentExchange;
    private static Pair fragmentPair;
    private List<String> symbolStrings;

    private int transactionId;
    private static Transaction transaction;

    private boolean isAmountLastUpdated;

    private TextWatcher amountTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            totalValueEditText.removeTextChangedListener(totalValueTextWatcher);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(isFieldCorrectlyFilled(buyPriceEditText, false) && isFieldCorrectlyFilled(amoutEditText, false))
            {
                if(Double.parseDouble(amoutEditText.getText().toString()) > 0)
                {
                    Double totalValue = Double.parseDouble(buyPriceEditText.getText().toString()) * Double.parseDouble(s.toString());
                    totalValueEditText.setText(String.format("%f", totalValue));
                }
                else
                {
                    totalValueEditText.setText("0");
                }
            }
            else
            {
                totalValueEditText.setText("");
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            totalValueEditText.addTextChangedListener(totalValueTextWatcher);
            isAmountLastUpdated = true;
        }
    };

    private TextWatcher totalValueTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            amoutEditText.removeTextChangedListener(amountTextWatcher);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(isFieldCorrectlyFilled(buyPriceEditText, false) && isFieldCorrectlyFilled(totalValueEditText, false))
            {
                if(Double.parseDouble(totalValueEditText.getText().toString()) > 0)
                {
                    Double amount = Double.parseDouble(s.toString()) / Double.parseDouble(buyPriceEditText.getText().toString());
                    amoutEditText.setText(String.format("%f", amount));
                }
                else
                {
                    amoutEditText.setText("0");
                }
            }
            else
            {
                amoutEditText.setText("");
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            amoutEditText.addTextChangedListener(amountTextWatcher);
            isAmountLastUpdated = false;
        }
    };

    private TextWatcher feesTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            totalValueEditText.removeTextChangedListener(totalValueTextWatcher);
            amoutEditText.removeTextChangedListener(amountTextWatcher);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if((isFieldCorrectlyFilled(amoutEditText, false) || isFieldCorrectlyFilled(totalValueEditText, false)) && isFieldCorrectlyFilled(buyPriceEditText, false))
            {
                double amount = Double.parseDouble(amoutEditText.getText().toString());
                double purchasePrice = Double.parseDouble(buyPriceEditText.getText().toString());
                double fees;
                double totalValue = Double.parseDouble(totalValueEditText.getText().toString());
                String feeCurrency;

                if(isAmountLastUpdated)
                {
                    totalValue = amount * purchasePrice;
                }
                else
                {
                    amount = totalValue / purchasePrice;
                }

                if(fees_editText.getText().toString().equals("0") || (start == 0 && before == 1 && count == 0))
                {
                    if(isAmountLastUpdated)
                    {
                        totalValueEditText.setText(String.valueOf(amount * purchasePrice));
                    }
                    else
                    {
                        amoutEditText.setText(String.valueOf(totalValue / purchasePrice));
                    }
                }
                else
                {

                    if(feesCurrencySpinner.getSelectedItemPosition() < 2)
                    {
                        feeCurrency = fragmentPair.getFrom();
                    }
                    else
                    {
                        feeCurrency = fragmentPair.getTo();
                    }

                    fees = getFees(feeCurrency, amount, purchasePrice);

                    if(feesCurrencySpinner.getSelectedItemPosition() % 2 == 0)
                    {
                        if(isAmountLastUpdated)
                        {
                            totalValueEditText.setText(String.valueOf(totalValue + fees));
                        }
                        else
                        {
                            amoutEditText.setText(String.valueOf(amount - (fees / purchasePrice)));
                        }
                    }
                    else
                    {
                        if(fragmentCurrency.getSymbol().equals(feeCurrency))
                        {
                            if(isAmountLastUpdated)
                            {
                                totalValueEditText.setText(String.valueOf(totalValue + (fees * purchasePrice)));
                            }
                            else
                            {
                                amoutEditText.setText(String.valueOf((totalValue / purchasePrice) - fees));
                            }
                        }
                        else
                        {
                            if(isAmountLastUpdated)
                            {
                                totalValueEditText.setText(String.valueOf(totalValue + fees));
                            }
                            else
                            {
                                amoutEditText.setText(String.valueOf((totalValue - fees) / purchasePrice));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            totalValueEditText.addTextChangedListener(totalValueTextWatcher);
            amoutEditText.addTextChangedListener(amountTextWatcher);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.record_transaction_fragment_buy, container, false);

        context = getActivity().getApplicationContext();

        calendar = Calendar.getInstance();
        sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.UK);

        preferenceManager = new PreferencesManager(getContext());

        initializeViewElements();

        return view;
    }

    private void checkCallingIntent()
    {
        Intent intent = getActivity().getIntent();
        transactionId = intent.getIntExtra("transactionId", -1);

        if(transactionId != -1)
        {
            DatabaseManager databaseManager = new DatabaseManager(context);
            transaction = databaseManager.getCurrencyTransactionById(transactionId);

            if(transaction.getType() == null || transaction.getType().equals("b"))
            {
                fillFields();
            }
        }
    }

    private void fillFields()
    {
        amoutEditText.setText(String.valueOf(transaction.getAmount()));
        buyPriceEditText.setText(String.valueOf(transaction.getPrice()));
        calendar.setTimeInMillis(transaction.getTimestamp());
        buyDateEditText.setText(sdf.format(calendar.getTime()));
        totalValueEditText.setText(String.valueOf(transaction.getAmount() * transaction.getPrice()));
        fees_editText.setText(String.valueOf(transaction.getFees()));
        note_editText.setText(transaction.getNote());
        deductHoldingsSwitch.setChecked(transaction.isDeducted());
    }

    private void initializeViewElements()
    {
        totalValueEditText = view.findViewById(R.id.totalValue_editText_buy);

        amoutEditText = view.findViewById(R.id.amount_editText_buy);

        buyPriceEditText = view.findViewById(R.id.buyPrice_editText);
        buyDateEditText = view.findViewById(R.id.buyDate_editText);
        buyDateEditText.setText(sdf.format(calendar.getTime()));

        buyDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDatePicker();
            }
        });
        feesCurrencySpinner = view.findViewById(R.id.feesCurrency_editText_buy);

        currencyFeeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, new ArrayList<>());
        currencyFeeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_black);
        feesCurrencySpinner.setAdapter(currencyFeeAdapter);

        deductHoldingsSwitch = view.findViewById(R.id.deductHoldingsBuy);

        if(fragmentPair != null)
        {
            updateAdapter();
        }

        saveBuyButton = view.findViewById(R.id.saveBuyButton);
        saveBuyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFieldCorrectlyFilled(amoutEditText, true) && isFieldCorrectlyFilled(buyPriceEditText, true) && isFieldCorrectlyFilled(totalValueEditText, true))
                {
                    double amount = Double.parseDouble(amoutEditText.getText().toString());
                    double purchasePrice = Double.parseDouble(buyPriceEditText.getText().toString());
                    double fees;
                    String feeCurrency;

                    if(feesCurrencySpinner.getSelectedItemPosition() < 1)
                    {
                        feeCurrency = fragmentPair.getFrom();
                    }
                    else
                    {
                        feeCurrency = fragmentPair.getTo();
                    }

                    fees = getFees(feeCurrency, amount, purchasePrice);

                    String note = note_editText.getText().toString();

                    DatabaseManager databaseManager = new DatabaseManager(getContext());

                    preferenceManager.setMustUpdateSummary(true);

                    if(transactionId == -1)
                    {
                        databaseManager.addTransaction(fragmentCurrency.getSymbol()
                                , amount
                                , calendar.getTime()
                                , purchasePrice
                                , fees
                                , note
                                , fragmentPair.getFrom().equals(fragmentCurrency.getSymbol()) ? fragmentPair.getTo() : fragmentPair.getFrom()
                                , feeCurrency
                                , ""
                                , fragmentExchange.getName()
                                , "b"
                                , feesCurrencySpinner.getSelectedItemPosition() % 2 == 0 ? "p" : "f"
                                , deductHoldingsSwitch.isChecked());

                        Intent intent = new Intent(getActivity(), HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                    }
                    else
                    {
                        databaseManager.updateTransactionWithId(transactionId
                                , amount
                                , calendar.getTime()
                                , purchasePrice
                                , fees
                                , note
                                , fragmentPair.getFrom().equals(fragmentCurrency.getSymbol()) ? fragmentPair.getTo() : fragmentPair.getFrom()
                                , feeCurrency
                                , ""
                                , fragmentExchange.getName()
                                ,"b"
                                , feesCurrencySpinner.getSelectedItemPosition() % 2 == 0 ? "p" : "f"
                                , deductHoldingsSwitch.isChecked());
                    }

                    getActivity().finish();
                }
            }
        });

        fees_editText = view.findViewById(R.id.fees_editText_buy);

        note_editText = view.findViewById(R.id.note_editText_buy);

        checkCallingIntent();

        setupTextWatchers();
    }

    private void setupTextWatchers()
    {
        totalValueEditText.addTextChangedListener(totalValueTextWatcher);
        amoutEditText.addTextChangedListener(amountTextWatcher);
        fees_editText.addTextChangedListener(feesTextWatcher);
    }

    private double getFees(String feeCurrency, double amount, double purchasedPrice)
    {
        double fees = 0;

        if(!fees_editText.getText().toString().equals(""))
        {
            fees = Double.parseDouble(fees_editText.getText().toString());

            if(feesCurrencySpinner.getSelectedItemPosition() % 2 == 0)
            {
                if(fragmentCurrency.getSymbol().equals(feeCurrency))
                {
                    fees = (100 * amount) / (100 + fees);
                }
                else
                {
                    double base = (100 * purchasedPrice * amount) / (100 + fees);

                    fees = purchasedPrice * amount - base;
                }
            }
        }

        return fees;
    }

    private boolean isFieldCorrectlyFilled(TextInputEditText editText, boolean displayError)
    {
        String purchasedPriceText = editText.getText().toString();
        double purchasedPrice;

        try {
            purchasedPrice = Double.parseDouble(purchasedPriceText);

            if(purchasedPrice < 0)
            {
                if(displayError) editText.setError(getResources().getString(R.string.field_negative));

                return false;
            }
        } catch (NumberFormatException e) {
            if(displayError) editText.setError(getResources().getString(R.string.field_nan));

            return false;
        }

        if(purchasedPriceText.equals(""))
        {
            if(displayError) editText.setError(getResources().getString(R.string.field_empty));

            return false;
        }

        return true;
    }

    private void updateAdapter()
    {
        symbolStrings = new ArrayList<>();
        symbolStrings.addAll(PlaceholderUtils.getFeeOptionsForSymbol(fragmentPair.getFrom(), getSecureContext()));
        symbolStrings.addAll(PlaceholderUtils.getFeeOptionsForSymbol(fragmentPair.getTo(), getSecureContext()));

        currencyFeeAdapter.clear();
        currencyFeeAdapter.addAll(symbolStrings);
        currencyFeeAdapter.notifyDataSetChanged();
    }

    private void createDatePicker()
    {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        buyDateEditText.setText(sdf.format(calendar.getTime()));
                        createTimePicker();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(Calendar.getInstance().getTimeInMillis());
        datePickerDialog.show();
    }

    private void createTimePicker()
    {
        new android.app.TimePickerDialog(
                getContext(),
                new android.app.TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        calendar.set(Calendar.MINUTE, minute);
                        buyDateEditText.setText(sdf.format(calendar.getTime()));

                        if(fragmentCurrency != null)
                        {
                            fragmentCurrency.getTimestampPrice(getContext(), fragmentCurrency.getSymbol().equals(fragmentPair.getFrom()) ? fragmentPair.getTo() : fragmentPair.getFrom(),calendar.getTimeInMillis() / 1000);
                        }
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        ).show();
    }

    private static Context getSecureContext()
    {
        return context;
    }

    @Override
    public void onCurrencyUpdated() {
        fragmentCurrency = currency;
    }

    @Override
    public void onExchangeUpdated() {
        fragmentExchange = exchange;
    }

    public void updatePair(Pair pair)
    {
        fragmentPair = pair;

        currencyFeeAdapter = new ArrayAdapter<String>(getSecureContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        currencyFeeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        feesCurrencySpinner.setAdapter(currencyFeeAdapter);

        symbolStrings = new ArrayList<>();
        symbolStrings.addAll(PlaceholderUtils.getFeeOptionsForSymbol(pair.getFrom(), getSecureContext()));
        symbolStrings.addAll(PlaceholderUtils.getFeeOptionsForSymbol(pair.getTo(), getSecureContext()));
        currencyFeeAdapter.addAll(symbolStrings);
        currencyFeeAdapter.notifyDataSetChanged();

        if(transaction != null)
        {
            if(transaction.getFeeCurrency().equals(fragmentPair.getFrom()))
            {
                if(transaction.getFeeFormat().equals("p"))
                {
                    feesCurrencySpinner.setSelection(0);
                }
                else
                {
                    feesCurrencySpinner.setSelection(1);
                }
            }
            else
            {
                if(transaction.getFeeFormat().equals("p"))
                {
                    feesCurrencySpinner.setSelection(2);
                }
                else
                {
                    feesCurrencySpinner.setSelection(3);
                }
            }
        }
    }

    @Override
    public void onPairUpdated() {
        fragmentPair = pair;

        fragmentCurrency.addOnTimestampPriceUpdatedListener(new Currency.OnTimestampPriceUpdatedListener() {
            @Override
            public void onTimeStampPriceUpdated(String price) {
                ((TextInputEditText) view.findViewById(R.id.buyPrice_editText)).setText(price);

                updatePair(fragmentPair);
            }
        });

        if(calendar == null)
        {
            calendar = Calendar.getInstance();
        }

        fragmentCurrency.getTimestampPrice(getSecureContext(), fragmentCurrency.getSymbol().equals(fragmentPair.getFrom()) ? fragmentPair.getTo() : fragmentPair.getFrom(), calendar.getTimeInMillis() / 1000);
    }

}

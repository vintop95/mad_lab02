package it.polito.mad1819.group17.deliveryapp.customer;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import it.polito.mad1819.group17.deliveryapp.common.orders.Order;
import it.polito.mad1819.group17.deliveryapp.common.orders.Rate;
import it.polito.mad1819.group17.deliveryapp.common.utils.ProgressBarHandler;
import it.polito.mad1819.group17.deliveryapp.customer.orders.OrderDetailsActivity;

public class RateActivity extends AppCompatActivity {

    private RatingBar rb_restaurant;
    private RatingBar rb_service;
    private RatingBar rb_food;
    private EditText input_comment;
    private Button btn_rate;

    private String inputOrderId;
    private Order inputOrder;

    private Rate rate;

    private ProgressBarHandler progressBar;

    private void locateViews() {
        rb_restaurant = findViewById(R.id.rb_restaurant);
        rb_service = findViewById(R.id.rb_service);
        rb_food = findViewById(R.id.rb_food);
        input_comment = findViewById(R.id.input_comment);
        btn_rate = findViewById(R.id.btn_rate);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate);

        locateViews();
        progressBar = new ProgressBarHandler(this);

        progressBar.show();
        inputOrderId = getIntent().getStringExtra("id");
        FirebaseDatabase.getInstance().getReference().child("customers").child(FirebaseAuth.getInstance().getUid()).child("orders").child(inputOrderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        inputOrder = dataSnapshot.getValue(Order.class);
                        progressBar.hide();
                        btn_rate.setEnabled(true);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        btn_rate.setOnClickListener(v -> storeRatesToFirebase());

    }


    private void storeRatesToFirebase() {
        if (inputOrder != null) {
            String restaurantRatePath = "/restaurant_rates/" + inputOrder.getRestaurant_id();
            String newPushedKey = FirebaseDatabase.getInstance().getReference(restaurantRatePath).push().getKey();
            restaurantRatePath += "/" + newPushedKey;

            String restaurateurOrderPath = "/restaurateurs/" + inputOrder.getRestaurant_id() + "/orders/" + inputOrder.getId();
            String customerOrderPath = "/customers/" + inputOrder.getCustomer_id() + "/orders/" + inputOrder.getId();

            HashMap<String, Object> ratesMap = new HashMap<>();
            rate = new Rate(FirebaseAuth.getInstance().getUid(), rb_restaurant.getRating(), rb_service.getRating(), input_comment.getText().toString());
            if (!rate.isEmpty()) {
                ratesMap.put(restaurantRatePath, rate);
                ratesMap.put(customerOrderPath + "/restaurant_rate", rate.getRestaurant_rate());
                ratesMap.put(customerOrderPath + "/service_rate", rate.getService_rate());
                ratesMap.put(customerOrderPath + "/comment", rate.getComment());
                setResult(OrderDetailsActivity.RATE_SENT, new Intent().putExtra("rate", rate));
            }


            if (!ratesMap.isEmpty()) {
                ratesMap.put(restaurateurOrderPath + "/rated", "yes");
                ratesMap.put(customerOrderPath + "/rated", "yes");
                FirebaseDatabase.getInstance().getReference().updateChildren(ratesMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        FirebaseDatabase.getInstance().getReference(/*dailyOffersPath*/"/restaurateurs/" + inputOrder.getRestaurant_id()).runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                                Integer number_of_restaurant_rates = mutableData.child("number_of_restaurant_rates").getValue(Integer.class);
                                Float total_restaurant_rate = mutableData.child("total_restaurant_rate").getValue(Float.class);
                                Integer number_of_service_rates = mutableData.child("number_of_service_rates").getValue(Integer.class);
                                Float total_service_rate = mutableData.child("total_service_rate").getValue(Float.class);

                                if (number_of_restaurant_rates == null)
                                    number_of_restaurant_rates = 0;
                                if (total_restaurant_rate == null)
                                    total_restaurant_rate = new Float(0);
                                if (number_of_service_rates == null)
                                    number_of_service_rates = 0;
                                if (total_service_rate == null)
                                    total_service_rate = new Float(0);

                                number_of_restaurant_rates += 1;
                                total_restaurant_rate += rate.getRestaurant_rate();

                                number_of_service_rates += 1;
                                total_service_rate += rate.getService_rate();

                                mutableData.child("number_of_restaurant_rates").setValue(number_of_restaurant_rates);
                                mutableData.child("total_restaurant_rate").setValue(total_restaurant_rate);
                                mutableData.child("number_of_service_rates").setValue(number_of_service_rates);
                                mutableData.child("total_service_rate").setValue(total_service_rate);

                                if (rb_food.getRating() != 0) {
                                    for (String itemId : inputOrder.getItem_itemDetails().keySet()) {


                                        MutableData currentIemRef = mutableData.child("daily_offers").child(itemId);
                                        MutableData currentNumberOfRatesRef = currentIemRef.child("number_of_rates");
                                        MutableData currentTotalRateRef = currentIemRef.child("total_rate");

                                        Integer number_of_rates = currentNumberOfRatesRef.getValue(Integer.class);
                                        Float total_rate = currentTotalRateRef.getValue(Float.class);
                                        if (number_of_rates == null || total_rate == null) {
                                            number_of_rates = new Integer(0);
                                            total_rate = new Float(0);
                                        }

                                        number_of_rates += 1;
                                        total_rate += new Float(rb_food.getRating());

                                        currentNumberOfRatesRef.setValue(number_of_rates);
                                        currentTotalRateRef.setValue(total_rate);
                                    }
                                }
                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {

                            }
                        });

                        Toast.makeText(getApplicationContext(), getString(R.string.feedback_sent), Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                setResult(OrderDetailsActivity.RATE_NOT_SENT);
                Toast.makeText(this, getString(R.string.feedback_empty_fields), Toast.LENGTH_SHORT).show();
            }

        } else {
            setResult(OrderDetailsActivity.RATE_NOT_SENT);
            Toast.makeText(this, getString(R.string.feedback_error), Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(OrderDetailsActivity.RATE_NOT_SENT);
        finish();
    }
}

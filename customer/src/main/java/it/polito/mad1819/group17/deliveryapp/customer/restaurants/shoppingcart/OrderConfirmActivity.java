package it.polito.mad1819.group17.deliveryapp.customer.restaurants.shoppingcart;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.security.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import it.polito.mad1819.group17.deliveryapp.common.dailyoffers.FoodModel;
import it.polito.mad1819.group17.deliveryapp.common.orders.Order;
import it.polito.mad1819.group17.deliveryapp.common.utils.CurrencyHelper;
import it.polito.mad1819.group17.deliveryapp.customer.R;
import it.polito.mad1819.group17.deliveryapp.customer.restaurants.dailyoffers.DailyMenuActivity;

public class OrderConfirmActivity extends AppCompatActivity {

    private TextView final_results;
    private EditText deliveryAddress_edit;
    private EditText deliveryHour_edit;
    private String restaurant_id;
    private String customer_id;
    private Intent intent;
    private Double totalprice;
    private Integer itemquantity;
    private HashMap<String,Integer> itemsMap;
    private Button btnConfirmOrder;
    private String deliveryAddress;
    private String phoneNumber;
    private String name;


    private ArrayList<String> keys;
    private ArrayList<Integer> values;

    ListView lst;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirm);

        final_results = findViewById(R.id.final_tv);
        btnConfirmOrder = findViewById(R.id.btn_confirm_order);
        intent = getIntent();

        customer_id = FirebaseAuth.getInstance().getUid();
        if(TextUtils.isEmpty(customer_id)){
            throw new IllegalStateException("customer_id is NULL!!!");
        }

        restaurant_id = intent.getStringExtra("restaurant_id");
        if (restaurant_id == null) throw new IllegalStateException("restaurant_id must not be null!");

        itemsMap = (HashMap<String, Integer>)intent.getSerializableExtra("itemsMap");
        retrieveData();
        keys = new ArrayList<String>(itemsMap.keySet());
        values = new ArrayList<Integer>(itemsMap.values());

        String[] names = keys.toArray(new String[values.size()]);
        Integer[] quantities = values.toArray(new Integer[keys.size()]);

        intent.putExtra("restaurant_id", restaurant_id);

        Log.d("elem_quantities", Integer.toString(quantities.length));
        Log.d("elem_names", Integer.toString(names.length));

        itemquantity = intent.getIntExtra("items_quantity",0);
        totalprice = intent.getDoubleExtra("items_tot_price",0);

          deliveryAddress_edit = (EditText) findViewById(R.id.deliveryAddress);
          deliveryHour_edit = (EditText) findViewById(R.id.deliveryHour);

        String finalResultString =
                String.format(Locale.getDefault(),
                        "Buying %d element(s) for the total price of: %s",
                        itemquantity, CurrencyHelper.getCurrency(totalprice)
                );

        final_results.setText(finalResultString);

        lst = (ListView) findViewById(R.id.listview_items);
        OrderConfirmAdapter orderConfirmAdapter = new OrderConfirmAdapter(names,quantities,this);
        lst.setAdapter(orderConfirmAdapter);

        btnConfirmOrder.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // TODO: complete filling order
                Order ord = new Order();
                HashMap<String,String> state_stateTime = new HashMap<String,String>();

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm");
                String current_timestamp = simpleDateFormat.format(new Date());


                String day = current_timestamp.split(" ")[0];
                String delivery_timestamp = day+" "+deliveryHour_edit.getText().toString();

                state_stateTime.put("state1",current_timestamp);
                DatabaseReference customerReference = FirebaseDatabase.getInstance().getReference()
                        .child("customers")
                        .child(customer_id);

                //Filling the order:
                ord.setState_stateTime(state_stateTime);
                ord.setSorting_field("state0_"+delivery_timestamp);
                ord.setCustomer_name(name);
                ord.setCustomer_phone(phoneNumber);
                //from query
                ord.setDelivery_timestamp(delivery_timestamp);
               // ord.setDelivery_address(deliveryAddress);
                ord.setDelivery_address(deliveryAddress_edit.getText().toString());
                ord.setItem_itemQuantity(itemsMap);

                //setId do not needed


                pushOrderToFirebase(ord);

                Toast.makeText(getApplicationContext(), "Order confirmed",Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    ////////////////// FIREBASE ORDER MGMT ////////////////////////////////////
    private final static String FIREBASE_ORDERS = "orders";

    public DatabaseReference getRestaurateurOrdersRef(){
        if(TextUtils.isEmpty(restaurant_id)){
            throw new IllegalStateException("restaurateur_id is NULL!!!");
        }

        return FirebaseDatabase.getInstance().getReference()
                .child("restaurateurs")
                .child(restaurant_id)
                .child(FIREBASE_ORDERS);
    }

    public DatabaseReference getCustomerOrdersRef(){
        if(TextUtils.isEmpty(customer_id)){
            throw new IllegalStateException("customer_id is NULL!!!");
        }

        return FirebaseDatabase.getInstance().getReference()
                .child("customers")
                .child(customer_id)
                .child(FIREBASE_ORDERS);
    }

    private static void handleCompletionListener
            (Context context, @Nullable DatabaseError err,
             @NonNull DatabaseReference ref, String msg){
        if(err != null){
            Log.e("FIREBASE_LOG",err.getMessage());
            Toast.makeText(context, err.getMessage(), Toast.LENGTH_LONG).show();
        }else{
            Log.d("FIREBASE_LOG", msg + " " + ref.toString());
        }
    }

    public void pushOrderToFirebase(Order ord){
        DatabaseReference newCustomerOrderRef = getCustomerOrdersRef().push();
        ord.setId(newCustomerOrderRef.getKey());
        newCustomerOrderRef.setValue(ord,
                (@Nullable DatabaseError err, @NonNull DatabaseReference ref)
                        -> handleCompletionListener(getApplicationContext(), err, ref, "Added")
        );

        DatabaseReference newRestaurateurOrderRef = getRestaurateurOrdersRef().child(ord.getId());
        moveFirebaseRecord(newCustomerOrderRef, newRestaurateurOrderRef);
    }

    public void moveFirebaseRecord(DatabaseReference fromPath, final DatabaseReference toPath)
    {
        fromPath.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                toPath.setValue(dataSnapshot.getValue(), (@Nullable DatabaseError err, @NonNull DatabaseReference ref)
                        -> handleCompletionListener(OrderConfirmActivity.this, err, ref, "Added"));
            }

            @Override
            public void onCancelled(DatabaseError firebaseError)
            {
                Log.e("FIREBASE_LOG", "Copy failed");
            }
        });
    }


    public void retrieveData(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            name = user.getDisplayName();
        } else {
            name = "";
        }

        DatabaseReference customerReference = FirebaseDatabase.getInstance().getReference()
                .child("customers")
                .child(customer_id);

        customerReference.child("address").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                deliveryAddress = (String) dataSnapshot.getValue(String.class);
                Log.d("retrieve", deliveryAddress);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        customerReference.child("phone").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                phoneNumber = (String) dataSnapshot.getValue(String.class);
                Log.d("retrieve", phoneNumber);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
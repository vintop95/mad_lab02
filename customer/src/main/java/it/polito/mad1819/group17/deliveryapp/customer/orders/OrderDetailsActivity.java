package it.polito.mad1819.group17.deliveryapp.customer.orders;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import it.polito.mad1819.group17.deliveryapp.common.orders.Order;
import it.polito.mad1819.group17.deliveryapp.common.orders.ShoppingItem;
import it.polito.mad1819.group17.deliveryapp.common.utils.CurrencyHelper;
import it.polito.mad1819.group17.deliveryapp.customer.R;

public class OrderDetailsActivity extends AppCompatActivity {

    public final static int STATE_CHANGED = 1;
    public final static int STATE_NOT_CHANGED = 0;

    private TextView txt_restaurant_name;
    private TextView txt_delivery_time;
    private TextView txt_delivery_date;
    private TextView txt_order_content;
    private TextView txt_customer_name;
    private TextView txt_customer_phone;
    private TextView txt_state_history;
    private TextView txt_delivery_address;
    private TextView txt_order_notes;
    private CardView card_deliveryman;
    private TextView txt_deliveryman_name;
    private TextView txt_deliveryman_phone;

    private Order inputOrder;

    private void showBackArrowOnToolbar() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void locateViews() {
        txt_restaurant_name = findViewById(R.id.txt_restaurant_name);
        txt_delivery_time = findViewById(R.id.txt_delivery_time);
        txt_delivery_date = findViewById(R.id.txt_delivery_date);
        txt_order_content = findViewById(R.id.txt_order_content);
        txt_customer_name = findViewById(R.id.txt_customer_name);
        txt_state_history = findViewById(R.id.txt_state_history);
        txt_delivery_address = findViewById(R.id.txt_delivery_address);
        txt_order_notes = findViewById(R.id.txt_order_notes);
        card_deliveryman = findViewById(R.id.card_deliveryman);
        txt_deliveryman_name = findViewById(R.id.txt_deliveryman_name);
        txt_deliveryman_phone = findViewById(R.id.txt_deliveryman_phone);
        txt_customer_phone = findViewById(R.id.txt_customer_phone);
    }

    private void feedViews(Order selectedOrder) {
        txt_restaurant_name.setText(selectedOrder.getRestaurant_name());
        txt_delivery_time.setText(selectedOrder.getDelivery_timestamp().split(" ")[1]);
        txt_delivery_date.setText(selectedOrder.getDelivery_timestamp().split(" ")[0]);
        txt_customer_name.setText(selectedOrder.getCustomer_name());
        txt_customer_phone.setText(Html.fromHtml("<u>" + selectedOrder.getCustomer_phone() + "<u/>"));
        txt_delivery_address.setText(selectedOrder.getDelivery_address());
        txt_order_notes.setText(selectedOrder.getNotes());

        String order_content = "";
        for (String item : selectedOrder.getItem_itemDetails().keySet()) {
            if (!order_content.equals(""))
                order_content += "\n";

            ShoppingItem shoppingItem = selectedOrder.getItem_itemDetails().get(item);
            order_content += "x" + shoppingItem.getQuantity()
                    + " " + item
                    + " - " + CurrencyHelper.getCurrency(shoppingItem.getPrice()*shoppingItem.getQuantity());
        }
        txt_order_content.setText(order_content);


        if (!TextUtils.isEmpty(selectedOrder.getDeliveryman_id())) {
            txt_deliveryman_name.setText(selectedOrder.getDeliveryman_name());
            txt_deliveryman_phone.setText(Html.fromHtml("<u>" + selectedOrder.getDeliveryman_phone() + "<u/>"));
            txt_deliveryman_phone.setOnClickListener(v -> {
                String phoneNumber = ((TextView) v).getText().toString();
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null)));
            });
        }

        txt_state_history.setText(selectedOrder.getStateHistoryToString());
    }


    public void adjustLayoutProgrammatically() {
        if (inputOrder.getCurrentState() != Order.STATE3) {
            card_deliveryman.setVisibility(View.GONE);
        } else {
            card_deliveryman.setVisibility(View.VISIBLE);
        }

        feedViews(inputOrder);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        showBackArrowOnToolbar();

        locateViews();

        txt_customer_phone.setOnClickListener(v -> {
            String phoneNumber = ((TextView) v).getText().toString();
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null)));
        });

        if (!TextUtils.isEmpty(getIntent().getStringExtra("id"))) {
            // we came here due to a tap on the notification so let us read the (updated) order from firebase
            FirebaseDatabase.getInstance().getReference("/restaurateurs/" + FirebaseAuth.getInstance().getUid() + "/orders/" + getIntent().getStringExtra("id"))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            inputOrder = dataSnapshot.getValue(Order.class);
                            adjustLayoutProgrammatically();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        } else {
            // we came here tapping on an order in the recycler view
            inputOrder = (Order) getIntent().getSerializableExtra("order");
            adjustLayoutProgrammatically();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


}
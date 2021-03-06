package it.polito.mad1819.group17.deliveryapp.restaurateur.profile;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.mad1819.group17.deliveryapp.common.Restaurateur;
import it.polito.mad1819.group17.deliveryapp.restaurateur.R;

public class EditProfileActivity extends AppCompatActivity {

    public static final int CAMERA_REQUEST = 0;
    public static final int GALLERY_REQUEST = 1;
    public static final int AUTOCOMPLETE_REQUEST = 2;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mRestaurateurDatabaseReference;
    private ValueEventListener mEditEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mFirebaseStorageReference;

    private Restaurateur current_user = new Restaurateur();
    private Toolbar toolbar;
    private Boolean image_changed = false;
    private ImageView image_user_photo;
    private EditText input_name;
    private EditText input_phone;
    private EditText input_mail;
    private EditText input_address;
    private Spinner input_restaurant_type;
    private Spinner input_free_day;
    private TextView input_working_time_opening;
    private TextView input_working_time_closing;
    private EditText input_bio;

    private String newAddress = null;

    private Intent intent;
    private Boolean firstAccess;
    private Boolean addressFlag = true;


    private void locateViews() {
        toolbar = findViewById(R.id.toolbar_edit);

        image_user_photo = findViewById(R.id.image_user_photo_sign_in);
        image_user_photo.setDrawingCacheEnabled(true);
        image_user_photo.buildDrawingCache();

        input_name = findViewById(R.id.input_name_sign_in);
        input_phone = findViewById(R.id.input_phone_sign_in);
        input_mail = findViewById(R.id.input_mail_sign_in);
        input_address = findViewById(R.id.input_address_sign_in);
        input_address.setKeyListener(null);
        input_restaurant_type = findViewById(R.id.input_restaurant_type_sign_in);
        input_free_day = findViewById(R.id.input_free_day_sign_in);
        input_working_time_opening = findViewById(R.id.input_working_time_opening_sign_in);
        input_working_time_closing = findViewById(R.id.input_working_time_closing_sign_in);
        input_bio = findViewById(R.id.input_bio_sign_in);
    }


    private void feedViews(Restaurateur restaurateur) {
        if (restaurateur != null) {
            if (!image_changed && restaurateur.getImage_path() != "") {
                Glide.with(image_user_photo.getContext()).load(restaurateur.getImage_path())
                        .into(image_user_photo);
            } else {
                Glide.with(image_user_photo.getContext()).clear(image_user_photo);
            }
            input_name.setText(restaurateur.getName());
            input_phone.setText(restaurateur.getPhone());
            input_mail.setText(restaurateur.getMail());
            input_address.setText(restaurateur.getAddress());

            //************** SET SPINNER ITEMS WITHOUT THE FIRST: "FAVOURITES"!
            CharSequence[] itemArray =
                    getResources().getTextArray(R.array.restaurant_types);
            List<CharSequence> itemList =
                    new ArrayList<CharSequence>(Arrays.asList(itemArray));
            itemList = itemList.subList(1,itemList.size()-1);

            ArrayAdapter adapter = new ArrayAdapter(this,
                    android.R.layout.simple_spinner_item,
                    itemList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            input_restaurant_type.setAdapter(adapter);
            //*************************************************
            String restaurant_type = restaurateur.getRestaurant_type();
            if (restaurant_type != null) {
                Integer index;
                try {
                    index = Integer.valueOf(restaurateur.getRestaurant_type());
                } catch (NumberFormatException e) {
                    index = 0;
                }

                // -1 because we removed the first category
                input_restaurant_type.setSelection(index-1);
            }

            String free_day = restaurateur.getFree_day();
            if (free_day != null) {
                Integer dayIndex;
                try {
                    dayIndex = Integer.valueOf(restaurateur.getFree_day());
                } catch (NumberFormatException e) {
                    dayIndex = 0;
                }

                input_free_day.setSelection(dayIndex);
            }

            String time_opening = restaurateur.getWorking_time_opening();
            if (time_opening != null)
                input_working_time_opening.setText(time_opening);

            String time_closing = restaurateur.getWorking_time_closing();
            if (time_closing != null)
                input_working_time_closing.setText(time_closing);
            if (restaurateur.getBio() != "")
                input_bio.setText(restaurateur.getBio());
            setCurrentUser(restaurateur, 0);
        } else {
            input_name.setText(mFirebaseAuth.getCurrentUser().getDisplayName());
            input_mail.setText(mFirebaseAuth.getCurrentUser().getEmail());
            setCurrentUser(null, 1);
        }
    }

    private void uploadImage() {
        Bitmap bitmap = ((BitmapDrawable) image_user_photo.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = mFirebaseStorageReference
                .child("profile_picture.jpg")
                .putBytes(data);

        uploadTask.addOnFailureListener((@NonNull Exception exception) -> {
            Log.e("FIREBASE_LOG", "Picture Upload Fail - EditProfileActivity");
            Toast.makeText(getApplicationContext(),
                    "Picture Upload Fail - EditProfileActivity",
                    Toast.LENGTH_LONG).show();
        }).addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) -> {
            mFirebaseStorageReference.child("profile_picture.jpg")
                    .getDownloadUrl()
                    .addOnSuccessListener((Uri uri) -> {
                        Uri downUri = uri;
                        Log.v("FIREBASE_LOG", "Picture Upload Success - EditProfileActivity");
                        mRestaurateurDatabaseReference.child(mFirebaseAuth.getUid())
                                .child("image_path").setValue(downUri.toString());
                    });
        });
    }

    private int saveProfile() {
        String id = mFirebaseAuth.getUid();
        String name = input_name.getText().toString();
        String phone = input_phone.getText().toString();
        String mail = input_mail.getText().toString();
        String address = input_address.getText().toString();
        String time_opening = input_working_time_opening.getText().toString();
        String time_closing = input_working_time_closing.getText().toString();
        Date date_timeOpening = null;
        Date date_timeClosing = null;
        String bio = input_bio.getText().toString();

        String free_day = Integer.toString(input_free_day.getSelectedItemPosition());
        int positionWithoutFavourites = input_restaurant_type.getSelectedItemPosition() + 1;
        String restaurant_type = Integer.toString(positionWithoutFavourites);

        if (name.isEmpty() ||
                phone.isEmpty() || !Patterns.PHONE.matcher(phone).matches() ||
                mail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mail).matches() ||
                address.isEmpty() ||
                restaurant_type.isEmpty() ||
                free_day.isEmpty() ||
                time_opening.equals("--:--") || time_closing.equals("--:--"))
            return -1;

        else if (!time_opening.equals("--:--") && !time_closing.equals("--:--")) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
            try {
                date_timeOpening = simpleDateFormat.parse(time_opening);
                date_timeClosing = simpleDateFormat.parse(time_closing);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (date_timeOpening.compareTo(date_timeClosing) >= 0)
                return 0;
            else {
                if (image_changed) uploadImage();

                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put("id", id);
                childUpdates.put("name", name);
                childUpdates.put("phone", phone);
                childUpdates.put("mail", mail);
                childUpdates.put("address", address);
                childUpdates.put("restaurant_type", restaurant_type);
                childUpdates.put("free_day", free_day);
                childUpdates.put("working_time_opening", time_opening);
                childUpdates.put("working_time_closing", time_closing);
                childUpdates.put("bio", bio);

                mRestaurateurDatabaseReference.child(id)
                        .updateChildren(childUpdates);

                if (mFirebaseAuth.getCurrentUser().getDisplayName() != name ||
                        mFirebaseAuth.getCurrentUser().getEmail() != mail) {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    mFirebaseAuth.getCurrentUser().updateProfile(profileUpdates);
                    mFirebaseAuth.getCurrentUser().updateEmail(mail);
                }

                return 1;
            }
        } else
            return -1;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Bitmap bitmapUserPhoto = null;

        if (requestCode == CAMERA_REQUEST || requestCode == GALLERY_REQUEST) {
            if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
                Bundle b = data.getExtras();
                if (b != null) bitmapUserPhoto = (Bitmap) b.get("data");
            } else if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
                try {
                    Uri targetUri = data.getData();
                    if (targetUri != null)
                        bitmapUserPhoto = BitmapFactory.decodeStream(
                                getContentResolver().openInputStream(targetUri));

                } catch (FileNotFoundException e) {
                    bitmapUserPhoto = null;
                    Toast.makeText(getApplicationContext(),
                            "FileNotFoundException: Error in setting image!",
                            Toast.LENGTH_LONG).show();
                }
            }

            if (bitmapUserPhoto != null) {
                image_changed = true;
                image_user_photo.setImageBitmap(bitmapUserPhoto);
                image_user_photo.setPadding(8, 8, 8, 8);
            }
        } else if (requestCode == AUTOCOMPLETE_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                newAddress = place.getAddress();
                input_address.setText(newAddress);
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Toast.makeText(this, "Error in retrieving the address :(", Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Address has not been selected.", Toast.LENGTH_LONG).show();
            }
            addressFlag = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        locateViews();

        toolbar = findViewById(R.id.toolbar_edit);
        showBackArrowOnToolbar();

        image_user_photo.setOnClickListener(v -> startPickPictureDialog());

        addOnFocusChangeListener(input_name);
        addOnFocusChangeListener(input_phone);
        addOnFocusChangeListener(input_address);
        addOnFocusChangeListener(input_mail);
        addOnFocusChangeListener(input_bio);

        Places.initialize(getApplicationContext(), "AIzaSyB7Tku5m9p0LVYU8k8-G7RB0DQoDXjvdSE");
        input_address.setOnClickListener(v -> {
            if (addressFlag) {
                addressFlag = false;
                List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.ADDRESS);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this);
                startActivityForResult(intent, AUTOCOMPLETE_REQUEST);
            }
        });

        addTimePickerOnClick(input_working_time_opening);
        addTimePickerOnClick(input_working_time_closing);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mRestaurateurDatabaseReference = mFirebaseDatabase.getReference().child("restaurateurs");

        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseStorageReference = mFirebaseStorage.getReference()
                .child(mFirebaseAuth.getUid()).child("images");
    }

    @Override
    public void onPause() {
        super.onPause();
        detachValueEventListener(mFirebaseAuth.getUid());
        Log.v("FIREBASE_LOG", "EventListener removed onPause - EditProfileActivity");
    }

    @Override
    public void onResume() {
        super.onResume();
        attachValueEventListener(mFirebaseAuth.getUid());
        Log.v("FIREBASE_LOG", "EventListener added onResume - EditProfileActivity");
    }

    private void attachValueEventListener(String userId) {
        if (userId == null) throw new IllegalArgumentException();

        if (mEditEventListener == null) {
            mEditEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Restaurateur restaurateur = dataSnapshot.getValue(Restaurateur.class);
                    if (restaurateur != null && newAddress != null)
                        restaurateur.setAddress(newAddress);
                    feedViews(restaurateur);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getApplicationContext(),
                            "Unable to retrieve restaurateur's information", Toast.LENGTH_LONG).show();
                }
            };
            mRestaurateurDatabaseReference.child(userId)
                    .addListenerForSingleValueEvent(mEditEventListener);
        }
    }

    private void detachValueEventListener(String userId) {
        if (mEditEventListener != null && userId != null) {
            mRestaurateurDatabaseReference.child(userId).removeEventListener(mEditEventListener);
            mEditEventListener = null;
        }
    }


    private void showBackArrowOnToolbar() {
        setSupportActionBar(toolbar);

        intent = getIntent();
        firstAccess = intent.getBooleanExtra("firstAccess", false);
        if (!firstAccess) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_edit_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.btn_save) {
            int result = saveProfile();
            if (result == 1) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.settings_changed), Toast.LENGTH_LONG).show();
                // startActivity(new Intent(EditProfileActivity.this, MainActivity.class));
                finish();
            } else if (result == -1)
                Toast.makeText(getApplicationContext(),
                        getString(R.string.fill_required_fields), Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getApplicationContext(),
                        getString(R.string.wrong_times), Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private void addTimePickerOnClick(View view) {
        view.setOnClickListener(v ->
        {
            TimePickerDialog timePickerDialog = new TimePickerDialog(EditProfileActivity.this,
                    (view1, hourOfDay, minute) -> {
                        String timestamp = "";

                        if (hourOfDay < 10)
                            timestamp += "0";
                        timestamp += hourOfDay + ":";

                        if (minute < 10)
                            timestamp += "0";
                        timestamp += minute;

                        ((TextView) view).setText(timestamp);
                    },
                    0,
                    0,
                    false);
            timePickerDialog.show();
        });
    }

    private void startPickPictureDialog() {
        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);

        myAlertDialog.setMessage(R.string.pick_picture_message);

        myAlertDialog.setPositiveButton(R.string.gallery,
                (dialog, which) -> {
                    Intent picFromGalleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    try {
                        startActivityForResult(picFromGalleryIntent, GALLERY_REQUEST);
                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(getApplicationContext(), R.string.gallery_not_found, Toast.LENGTH_LONG).show();
                    }
                }
        );

        myAlertDialog.setNegativeButton(R.string.camera,
                (dialog, which) -> {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    try {
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(takePictureIntent, CAMERA_REQUEST);
                        } else {
                            throw new android.content.ActivityNotFoundException();
                        }
                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(getApplicationContext(), R.string.camera_not_found,
                                Toast.LENGTH_LONG).show();
                    }
                }
        );

        myAlertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (firstAccess) {
            AuthUI.getInstance().signOut(this);
            finish();
        }
        else
            confirmOnBackPressed();
    }

    private boolean dataChanged() {
        if ((current_user.getName() == null && input_name.getText().toString() != null) ||
                (current_user.getName() != null && !current_user.getName().equals(input_name.getText().toString())))
            return true;

        if ((current_user.getPhone() == null && input_phone.getText().toString() != null) ||
                (current_user.getPhone() != null && !current_user.getPhone().equals(input_phone.getText().toString())))
            return true;

        if ((current_user.getMail() == null && input_mail.getText().toString() != null) ||
                (current_user.getMail() != null && !current_user.getMail().equals(input_mail.getText().toString())))
            return true;

        if ((current_user.getAddress() == null && input_address.getText().toString() != null) ||
                (current_user.getAddress() != null && !current_user.getAddress().equals(input_address.getText().toString())))
            return true;

        Long current_type = input_restaurant_type.getSelectedItemId() + 1; // +1 needed for avoiding "favourite" hidden category
        if ((current_user.getRestaurant_type() == null && !input_restaurant_type.getSelectedItem().toString().isEmpty()) ||
                (current_user.getRestaurant_type() != null && !current_user.getRestaurant_type().equals(current_type.toString())))
            return true;

        Long current_day = input_free_day.getSelectedItemId();
        if ((current_user.getFree_day() == null && !input_free_day.getSelectedItem().toString().isEmpty()) ||
                (current_user.getFree_day() != null && !current_user.getFree_day().equals(current_day.toString())))
            return true;

        if ((current_user.getWorking_time_opening() == null && !input_working_time_opening.getText().toString().equals("--:--")) ||
                (current_user.getWorking_time_opening() != null && !current_user.getWorking_time_opening().equals(input_working_time_opening.getText().toString())))
            return true;

        if ((current_user.getWorking_time_closing() == null && !input_working_time_closing.getText().toString().equals("--:--")) ||
                (current_user.getWorking_time_closing() != null && !current_user.getWorking_time_closing().equals(input_working_time_closing.getText().toString())))
            return true;

        return (current_user.getBio() == null && input_bio.getText().toString() != null) ||
                (current_user.getBio() != null && !current_user.getBio().equals(input_bio.getText().toString()));

    }

    private void confirmOnBackPressed() {
        // If no modification happened, then close without showing alert dialog.
        if (dataChanged()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.warning_title)
                    .setMessage(R.string.discard_msg)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                super.onBackPressed();
                            }
                    )
                    .setNegativeButton(android.R.string.no, (dialog, which) -> {
                                dialog.cancel();
                            }
                    )
                    .create()
                    .show();
        } else {
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void addOnFocusChangeListener(EditText editText) {
        editText.setOnFocusChangeListener((v, focus) -> {
            if (focus)
                ((EditText) v).setSelection(((EditText) v).getText().length(), 0);
        });
    }

    private void setCurrentUser(Restaurateur restaurateur, int flag) {
        if (flag == 0) {
            current_user.setName(restaurateur.getName());
            current_user.setPhone(restaurateur.getPhone());
            current_user.setMail(restaurateur.getMail());
            current_user.setAddress(restaurateur.getAddress());
            current_user.setRestaurant_type(restaurateur.getRestaurant_type());
            current_user.setFree_day(restaurateur.getFree_day());
            current_user.setWorking_time_opening(restaurateur.getWorking_time_opening());
            current_user.setWorking_time_closing(restaurateur.getWorking_time_closing());
            if (restaurateur.getBio() != null)
                current_user.setBio(restaurateur.getBio());
        }
        if (flag == 1 ) {
            current_user.setName(mFirebaseAuth.getCurrentUser().getDisplayName());
            current_user.setMail(mFirebaseAuth.getCurrentUser().getEmail());
        }
    }
}
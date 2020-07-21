package com.appmate.watchout.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.appmate.watchout.BuildConfig;
import com.appmate.watchout.R;
import com.appmate.watchout.model.Data;
import com.appmate.watchout.model.Location;
import com.github.ybq.android.spinkit.SpinKitView;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.type.LatLng;
import com.schibstedspain.leku.LocationPickerActivity;
import com.xw.repo.BubbleSeekBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.appmate.watchout.activity.SplashActivity.logoutUser;
import static com.appmate.watchout.activity.SplashActivity.mAuth;
import static com.appmate.watchout.util.AppUtil.hasPermissions;
import static com.appmate.watchout.util.AppUtil.isLocationEnabled;
import static com.appmate.watchout.util.Constants.GALLERY_REQUEST_CODE;
import static com.appmate.watchout.util.Constants.IMAGE_REQUEST_CODE;
import static com.appmate.watchout.util.Constants.LOCATION_PERMISSIONS;
import static com.appmate.watchout.util.Constants.MAP_BUTTON_REQUEST_CODE;
import static com.appmate.watchout.util.Constants.PERMISSIONS;
import static com.appmate.watchout.util.Constants.PERMISSION_ALL;
import static com.appmate.watchout.util.Constants.VIDEO_REQUEST_CODE;
import static com.appmate.watchout.util.Constants.currentLocation;
import static com.schibstedspain.leku.LocationPickerActivityKt.LATITUDE;
import static com.schibstedspain.leku.LocationPickerActivityKt.LOCATION_ADDRESS;
import static com.schibstedspain.leku.LocationPickerActivityKt.LONGITUDE;
import static com.schibstedspain.leku.LocationPickerActivityKt.ZIPCODE;

public class PostActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "PostActivity";
    private Context mContext;

    /*Base Layout*/
    private View menuLayout,loadingLayout;
    private ImageView btnMenu;
    private TextView activityTitle;
    private TextView tvUsername, tvEmail, btnMenuHome, btnMenuNewsFeed, btnMenuSettings, btnMenuHelpAboutUs, btnMenuLogout;
    private SpinKitView progressBar;
    /**/

    private EditText et_detail;
    private Button btn_camera, btn_video, btn_gallery;
    private ImageView iv_event;
    private VideoView vv_event;

    private Button map_button;

    /*Camera Gallery*/
    private String cameraFilePath;
    private Uri    mediaFilePath;

    /**/
    private BubbleSeekBar seek;
    private CheckBox cb_theft, cb_murder, cb_fire, cb_terror, cb_natural, cb_other;

    /*Data*/
    private Data postToCreate;
    /**/

    /*Testing*/
    private Button btn_saveEvent,btn_cancel;
    /**/
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseFirestore db;
    private DocumentReference dbRef;


    private String incidentType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_post);
        postToCreate = new Data();
        storage = FirebaseStorage.getInstance("gs://watch-out-7c380.appspot.com");
        storageRef = storage.getReference();
        db = FirebaseFirestore.getInstance();
        setupUI();
        setupTitleBar();
        setupMenu();
        setupProgressBar();
    }

    private void setupProgressBar() {
        progressBar =  findViewById(R.id.spin_kit);
        Sprite doubleBounce = new DoubleBounce();
        progressBar.setIndeterminateDrawable(doubleBounce);
    }

    public void showProgress(){
        loadingLayout.setVisibility(View.VISIBLE);
    }
    public void hideProgress(){
        loadingLayout.setVisibility(View.GONE);
    }
    public void setupUI() {
        menuLayout = findViewById(R.id.menuLayout);
        loadingLayout = findViewById(R.id.loadingLayout);

        et_detail = findViewById(R.id.et_detail);
        iv_event = findViewById(R.id.iv_event);
        vv_event = findViewById(R.id.vv_event);

        seek = findViewById(R.id.seek);

        cb_theft = findViewById(R.id.cb_theft);
        cb_murder = findViewById(R.id.cb_murder);
        cb_fire = findViewById(R.id.cb_fire);
        cb_terror = findViewById(R.id.cb_terror);
        cb_natural = findViewById(R.id.cb_natural);
        cb_other = findViewById(R.id.cb_other);

        cb_theft.setOnClickListener(this);
        cb_murder.setOnClickListener(this);
        cb_fire.setOnClickListener(this);
        cb_terror.setOnClickListener(this);
        cb_natural.setOnClickListener(this);
        cb_other.setOnClickListener(this);

        btn_camera = findViewById(R.id.btn_camera);
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPermissions(mContext, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(PostActivity.this, PERMISSIONS, PERMISSION_ALL);
                } else {
                    captureImage();
                }
            }
        });
        btn_gallery = findViewById(R.id.btn_gallery);
        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPermissions(mContext, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(PostActivity.this, PERMISSIONS, PERMISSION_ALL);
                }
                else {
                    imageFromGallery();
                }
            }
        });
        btn_video = findViewById(R.id.btn_video);
        btn_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPermissions(mContext, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(PostActivity.this, PERMISSIONS, PERMISSION_ALL);
                } else {
                    captureVideo();
                }
            }
        });

        map_button = findViewById(R.id.map_button);
        map_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPermissions(mContext, LOCATION_PERMISSIONS)) {
                    ActivityCompat.requestPermissions(PostActivity.this, PERMISSIONS, PERMISSION_ALL);
                }
                else if(isLocationEnabled(mContext)){
                    double  defaultLat = 31.5204;
                    double  defailtLng = 74.3587;
                    if(currentLocation != null){
                        defaultLat = currentLocation.getLatitude();
                        defailtLng = currentLocation.getLongitude();
                    }
                    Intent locationPickerIntent = new LocationPickerActivity.Builder()
                            .withLocation(defaultLat, defailtLng)
                            .withGeolocApiKey("AIzaSyDgIjrCXSxiH31ghL2fffio6Os7Y1X2JXQ")
                            .withSearchZone("en_PK")
//                        .withSearchZone(new SearchZoneRect(LatLng(26.525467, -18.910366), LatLng(43.906271, 5.394197)))
                            .withDefaultLocaleSearchZone()
                            .shouldReturnOkOnBackPressed()
//                        .withStreetHidden()
//                        .withCityHidden()
//                        .withZipCodeHidden()
//                        .withSatelliteViewHidden()
//                        .withGooglePlacesEnabled()
                            .withGoogleTimeZoneEnabled()
//                        .withVoiceSearchHidden()
//                        .withUnnamedRoadHidden()
                            .build(mContext);
                    startActivityForResult(locationPickerIntent, MAP_BUTTON_REQUEST_CODE);
                }
                // this is optional if you want to return RESULT_OK if you don't set the latitude/longitude and click back button
//                locationPickerIntent.putExtra("test", "this is a test");
            }
        });
        btn_saveEvent = findViewById(R.id.btn_saveEvent);
        btn_saveEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                uploadFile(cameraFilePath,"event1");
                createIncidentPost();
            }
        });
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }


    public void createIncidentPost() {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
        //Event Details
        postToCreate.setEvent(et_detail.getText().toString());

        postToCreate.setSeverity(getSeekBarValue());
        postToCreate.setType(incidentType);
        postToCreate.setId(uuid);
        //Location
        if(postToCreate.getLocation()==null){
            Toast.makeText(PostActivity.this, "Please provide location!",
                    Toast.LENGTH_SHORT).show();
        }
        //Attached Image/Video
        else if(cameraFilePath!=null){
            showProgress();
            uploadFile(cameraFilePath, ".jpg", uuid, postToCreate);
        }
        else Toast.makeText(PostActivity.this, "Please provide Image or Video!", Toast.LENGTH_SHORT).show();
//        postData(postToCreate);
    }

    private void postData(Data data) {

        Map<String, Object> map = new HashMap<>();
        map.put("id" , data.getId());
        map.put("eventName", data.getEvent());
        map.put("image", data.getImage());
        map.put("video", data.getVideo());
        map.put("location", data.getLocation());
        map.put("reportCount", data.getReportCount());
        map.put("alertCount", data.getAlertCount());
        map.put("severity", data.getSeverity());
        map.put("type", data.getType());
        map.put("email", mAuth.getCurrentUser().getEmail());
        map.put("userId", mAuth.getCurrentUser().getUid());
        map.put("userName", mAuth.getCurrentUser().getDisplayName());
        map.put("alerterList", new ArrayList<String>());
        map.put("reporterList", new ArrayList<String>());


        db.collection("events").document("post").update("posts",FieldValue.arrayUnion(map)).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                hideProgress();
                Toast.makeText(PostActivity.this, "Incident Posted Successfully!",
                        Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                hideProgress();
                Toast.makeText(PostActivity.this, "Updated Failed",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void uploadFile(String filePath, String fileType, String event, Data data) {

        StorageReference alertImages = storageRef.child("alertImages/" + event + fileType);
        InputStream stream = null;
        try {
            if(filePath.contains("media")){
                stream =  getContentResolver().openInputStream(mediaFilePath);
            }
            else{
                stream = new FileInputStream(new File(filePath.replace("file:", "")));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(stream != null){
            UploadTask uploadTask = alertImages.putStream(stream);

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    Toast.makeText(mContext, "Failure", Toast.LENGTH_SHORT).show();
                    hideProgress();

                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    // ...
                    // get the image Url of the file uploaded
                    alertImages.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // getting image uri and converting into string
                            System.out.println(uri.toString());
                            if(fileType.equalsIgnoreCase(".jpg")){
                                data.setImage(uri.toString());
                            }
                            else{
                                data.setVideo(uri.toString());
                            }
                            Toast.makeText(mContext, "Success", Toast.LENGTH_SHORT).show();
                            //Call Post Event To DB Here
                            postData(data);
                        }
                    });
                }
            })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
//                Toast.makeText(mContext,"Upload is " + progress + "% done",Toast.LENGTH_SHORT).show();
                        }
                    }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
//                System.out.println("Upload is paused");
                }
            });
        }
        else{
            Toast.makeText(mContext, "File Not Found", Toast.LENGTH_SHORT).show();
            hideProgress();
        }
    }
//    content://media/external/images/media/79445,file:///storage/emulated/0/DCIM/Camera/JPEG_20200719_203321_3591027173808915302.jpg,mContext.getContentResolver().openInputStream(data.getData())
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode) {
                case GALLERY_REQUEST_CODE:
                    //data.getData returns the content URI for the selected Image
                    Uri selectedImage = data.getData();
                    cameraFilePath = selectedImage.toString();
                    mediaFilePath = selectedImage;
                    iv_event.setVisibility(View.VISIBLE);
                    iv_event.setImageURI(Uri.parse(cameraFilePath));
//                    File outputFile = new File("YOUR DESIRED FILE PATH");
//
//                    InputStream stream = null;
//                    try {
//                        stream = new FileInputStream("/"+cameraFilePath);
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                    if(stream==null){
//                        Toast.makeText(mContext, String.valueOf("Still  Null"), Toast.LENGTH_SHORT).show();
//                    }
//                    else{
//                        Toast.makeText(mContext, String.valueOf("Success"), Toast.LENGTH_SHORT).show();
//                    }
                    break;
                case IMAGE_REQUEST_CODE:
                    iv_event.setVisibility(View.VISIBLE);
                    iv_event.setImageURI(Uri.parse(cameraFilePath));
                    break;
                case VIDEO_REQUEST_CODE:
                    vv_event.setVisibility(View.VISIBLE);
                    vv_event.setVideoURI(Uri.parse(cameraFilePath));
                    break;
                case MAP_BUTTON_REQUEST_CODE:
                    postToCreate.setLocation(getLatLng(data));
                    break;
            }

    }

    public Location getLatLng(Intent data) {
        double latitude = data.getDoubleExtra(LATITUDE, 0.0);
        Log.d("LATITUDE****", String.valueOf(latitude));
        double longitude = data.getDoubleExtra(LONGITUDE, 0.0);
        Log.d("LONGITUDE****", String.valueOf(longitude));
        String address = data.getStringExtra(LOCATION_ADDRESS);
        Log.d("ADDRESS****", address.toString());
        String postalcode = data.getStringExtra(ZIPCODE);
        Log.d("POSTALCODE****", postalcode.toString());
        return new Location(latitude, longitude);
    }

    public String getSeekBarValue() {
        return String.valueOf(seek.getProgress());
    }

    public void setCheckedIncidentType() {

    }

    public void setupTitleBar() {
        btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuLayout.getVisibility() == View.VISIBLE) {
                    menuLayout.setVisibility(View.GONE);
                } else {
                    menuLayout.setVisibility(View.VISIBLE);
                }
            }
        });
        activityTitle = findViewById(R.id.tv_bar_title);
        activityTitle.setText("Add Post");
    }

    public void setupMenu() {
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvUsername.setText(mAuth.getCurrentUser().getDisplayName());
        tvEmail.setText(mAuth.getCurrentUser().getEmail());
        btnMenuHome = findViewById(R.id.btnMenuHome);
        btnMenuHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostActivity.this.startActivity(new Intent(PostActivity.this, MainActivity.class));
                finish();
            }
        });
        btnMenuNewsFeed = findViewById(R.id.btnMenuNewsFeed);
        btnMenuNewsFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostActivity.this.startActivity(new Intent(PostActivity.this, NewsFeedActivity.class));
                finish();
            }
        });
        btnMenuSettings = findViewById(R.id.btnMenuSettings);
        btnMenuSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostActivity.this.startActivity(new Intent(PostActivity.this, SettingsActivity.class));
                finish();
            }
        });
        btnMenuHelpAboutUs = findViewById(R.id.btnMenuHelpAboutUs);
        btnMenuHelpAboutUs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostActivity.this.startActivity(new Intent(PostActivity.this, HelpContactActivity.class));
                finish();
            }
        });
        btnMenuLogout = findViewById(R.id.btnMenuLogout);
        btnMenuLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
                PostActivity.this.startActivity(new Intent(PostActivity.this, SignInActivity.class));
                finish();
            }
        });
    }

    public void imageFromGallery() {
        //Create an Intent with action as ACTION_PICK
        Intent intent = new Intent(Intent.ACTION_PICK);
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.setType("image/*");
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        // Launching the Intent
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", createImageFile("JPEG_", ".jpg")));
        startActivityForResult(intent, IMAGE_REQUEST_CODE);
    }

    private void captureVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", createVideoFile("MP4_", ".mp4")));
        startActivityForResult(intent, VIDEO_REQUEST_CODE);
    }


    private File createImageFile(String fileInitials, String suffix) {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = fileInitials + timeStamp + "_";
        //This is the directory in which the file will be created. This is the default location of Camera photos
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        storageDir.mkdirs();
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    suffix,         /* suffix */
                    storageDir      /* directory */
            );
            // Save a file: path for using again
            cameraFilePath = "file://" + image.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    private File createVideoFile(String fileInitials, String suffix) {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = fileInitials + timeStamp + "_";
        //This is the directory in which the file will be created. This is the default location of Camera photos
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        storageDir.mkdirs();
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    suffix,         /* suffix */
                    storageDir      /* directory */
            );
            // Save a file: path for using again
            cameraFilePath = "file://" + image.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cb_theft:
                cb_fire.setChecked(false);
                cb_murder.setChecked(false);
                cb_natural.setChecked(false);
                cb_other.setChecked(false);
                cb_terror.setChecked(false);
                incidentType = "Theft";
                break;
            case R.id.cb_fire:
                cb_theft.setChecked(false);
                cb_murder.setChecked(false);
                cb_natural.setChecked(false);
                cb_other.setChecked(false);
                cb_terror.setChecked(false);
                incidentType = "Fire";
                break;
            case R.id.cb_murder:
                cb_theft.setChecked(false);
                cb_fire.setChecked(false);
                cb_natural.setChecked(false);
                cb_other.setChecked(false);
                cb_terror.setChecked(false);
                incidentType = "Murder";
                break;
            case R.id.cb_natural:
                cb_theft.setChecked(false);
                cb_fire.setChecked(false);
                cb_murder.setChecked(false);
                cb_other.setChecked(false);
                cb_terror.setChecked(false);
                incidentType = "Natural";
                break;
            case R.id.cb_other:
                cb_theft.setChecked(false);
                cb_fire.setChecked(false);
                cb_murder.setChecked(false);
                cb_natural.setChecked(false);
                cb_terror.setChecked(false);
                incidentType = "Other";
                break;
            case R.id.cb_terror:
                cb_theft.setChecked(false);
                cb_fire.setChecked(false);
                cb_murder.setChecked(false);
                cb_natural.setChecked(false);
                cb_other.setChecked(false);
                incidentType = "Terror ";
                break;
        }
    }
}

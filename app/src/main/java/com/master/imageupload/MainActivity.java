package com.master.imageupload;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements PickImageDialogInterface {

    private Subscription mPicSubscription;
    private Uri mImageFileUri;
    private File mImageFile = null;
    private PickImageDialog mPickImageDialog;
    private ImageView mImageView;
    private ProgressBar mInsideProgressBar;
    private Button mUploadImage;
    /**
     * Permissions required to read and write contacts.
     */
    private static String[] permissionsCameraGallery = {Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};


    /**
     * Permissions required to read camera.
     */
    public static final int REQUEST_CAMERA = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mInsideProgressBar = findViewById(R.id.insidePB);
        mImageView = findViewById(R.id.uploadedImage);
        mUploadImage = findViewById(R.id.uploadImage);
        mUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO take image from gallery OR Camera and Upload to Server
                checkCameraAndStoragePermission();
            }
        });
    }

    private void showImagePickDialog() {

        if (mPickImageDialog == null) {
            mPickImageDialog = new PickImageDialog(this);
            mPickImageDialog.delegate = this;
            mPickImageDialog.showDialog();
        } else {
            mPickImageDialog.delegate = this;
            mPickImageDialog.showDialog();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mPickImageDialog == null) {
            mPickImageDialog = new PickImageDialog(this);
            mPickImageDialog.delegate = this;
            mPickImageDialog.resetFiles(mImageFileUri, mImageFile);
        }
        if (resultCode == Activity.RESULT_OK) {
            mPickImageDialog.onActivityResult(requestCode, data);

        } else {
            mPickImageDialog.onResultCancelled();
        }
    }

    @Override
    public void holdRecordingFile(Uri fileUri, File file) {
        this.mImageFileUri = fileUri;
        this.mImageFile = file;
    }

    @Override
    public void handleIntent(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void uploadPickedImage(File file) {
        mImageFile = file;
        uploadPhotoToServer(mImageFile, "images");
    }

    private void checkCameraAndStoragePermission() {

        if (Build.VERSION.SDK_INT >= 23) {
            if (!checkSDCardReadPermission(MainActivity.this)) {
                displaySDCardReadPermissionAlert(MainActivity.this);
            } else {
                showImagePickDialog();
            }
        } else {
            showImagePickDialog();
        }
    }


    //method checks for SD card read, write  permission granted OR not.
    public boolean checkSDCardReadPermission(Context thisActivity) {
        return !(ActivityCompat.checkSelfPermission(thisActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(thisActivity,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED);

    }

    public void displaySDCardReadPermissionAlert(Activity thisActivity) {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(thisActivity, permissionsCameraGallery, REQUEST_CAMERA);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkCameraAndStoragePermission();
                } else {
                    displayAlert(MainActivity.this, REQUEST_CAMERA);
                }
                break;
        }
    }

    public void displayAlert(final Activity context, final int position) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        if (position == REQUEST_CAMERA) {
            builder1.setTitle(context.getResources().getString(R.string.camera));
            builder1.setMessage(context.getResources().getString(R.string.camera_desc));
        }
        builder1.setCancelable(true);

        builder1.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        if (position == REQUEST_CAMERA) {
                            displaySDCardReadPermissionAlert(context);
                        }
                    }
                });

        builder1.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert1 = builder1.create();
        alert1.show();
    }


    private void uploadPhotoToServer(File mImageFile, String imageType) {
        mInsideProgressBar.setVisibility(View.VISIBLE);
        mUploadImage.setVisibility(View.GONE);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(imageType,
                mImageFile.getName(), RequestBody.create(MediaType.parse("image/*"), mImageFile));
        APIServices service = ServiceFactory.createRetrofitService(APIServices.class);
        mPicSubscription = service.uploadPhoto(APIConstants.IMAGE_UPLOAD_URL,
                filePart)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ImageUploadResponse>() {
                    @Override
                    public void onCompleted() {
                        mPicSubscription = null;
                        mInsideProgressBar.setVisibility(View.GONE);
                        mUploadImage.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mPicSubscription = null;

                    }

                    @Override
                    public void onNext(ImageUploadResponse imageResultModel) {
                        mInsideProgressBar.setVisibility(View.GONE);
                        mUploadImage.setVisibility(View.VISIBLE);
                        String imageUrl = imageResultModel.getResult().getImage();
                        Picasso.with(MainActivity.this).load(APIConstants.SERVER_URL + "containers/identificationImages/download/" + imageUrl)
                                .fit().centerCrop().into(mImageView);

                        Log.d("UPLOADED_IMAGE", imageUrl);
                        Toast.makeText(getApplicationContext(), "Uploaded Success", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPicSubscription != null) {
            mPicSubscription.unsubscribe();
        }
    }
}

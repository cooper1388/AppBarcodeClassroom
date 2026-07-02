package hn.uth.appbarcodeclassroom;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.IOException;

import hn.uth.appbarcodeclassroom.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 888;
    private ActivityMainBinding binding;
    private static final int REQUEST_IMAGE_CAPTURE = 999;
    private String imageFolder="";
    private Bitmap image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());


        //setContentView(R.layout.activity_main);
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.btnCamera.setOnClickListener(v -> {
            if (checkAndRequestPermission()) {
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takePicture.resolveActivity(getPackageManager()) != null){
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    }catch (Exception error){
                        Snackbar.make(v, R.string.error_take_picture, Snackbar.LENGTH_SHORT).show();
                    }
                    if(photoFile != null){
                        Uri photoUri = FileProvider.getUriForFile(this, "hn.uth.appbarcodeclassroom.fileprovider", photoFile);
                        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    }

                    startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
                }else{
                    Snackbar.make(v, R.string.no_camera_app, Snackbar.LENGTH_SHORT).show();
                }
            }else{
                Snackbar.make(v, R.string.permission_required, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            if(!"".equals(imageFolder) && imageFolder != null){

                File file = new File(imageFolder);
                if(file.exists()){
                    binding.imgPreview.setImageURI(Uri.fromFile(file));
                    image = BitmapFactory.decodeFile(file.getAbsolutePath());
                    procesarImagen();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void procesarImagen() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        InputImage inputImage = InputImage.fromBitmap(this.image, 0);

        scanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        Barcode barcode = barcodes.get(0);
                        String rawValue = barcode.getRawValue();


                        binding.etBarcodeFormat.setText(getString(barcode.getValueType() == 8 ? R.string.qr_code : R.string.unknown));
                        binding.etBarcodeRawValue.setText(rawValue);
                        binding.etBarcodeStatus.setText(getString(R.string.barcode_found));
                    } else {
                        binding.etBarcodeStatus.setText(getString(R.string.no_barcode_found));
                    }
                })
                .addOnFailureListener(e -> {
                    binding.etBarcodeStatus.setText(getString(R.string.error_processing_image));
                });
            Snackbar.make(binding.getRoot(), R.string.procesamiento_finalizado, Snackbar.LENGTH_SHORT).show();
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imageFolder = image.getAbsolutePath();

        return image;
    }

    private boolean checkAndRequestPermission() {
        int cameraPermission = checkSelfPermission(Manifest.permission.CAMERA);

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
            return false;
        }
        return true;

    }
}
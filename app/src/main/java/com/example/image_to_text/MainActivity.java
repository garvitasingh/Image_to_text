package com.example.image_to_text;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageButton cameraBt, detectBt, galleryBt;
    ImageView imgView;
    TextView textArea,imgText,txtText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        detectBt = (ImageButton) findViewById(R.id.detect);
        cameraBt = (ImageButton) findViewById(R.id.camera);
        galleryBt = (ImageButton) findViewById(R.id.gallery);

        imgView = findViewById(R.id.image);
        imgText=findViewById(R.id.imgtxt);
        txtText=findViewById(R.id.txt);
        textArea = findViewById(R.id.text);
        textArea.setMovementMethod(new ScrollingMovementMethod());

        detectBt.setOnClickListener(this);
        cameraBt.setOnClickListener(this);
        galleryBt.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.detect:
                try {
                    runTextRecognition();
                }catch (Exception e){
                    Toast.makeText(MainActivity.this, "Select Image", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.camera:
                askCameraPermissions();
                break;
            case R.id.gallery:
                galleryAddPic();
                break;
        }
    }

    ActivityResultLauncher<String> take_image=registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    imgView.setImageURI(uri);
                    imgText.setVisibility(View.GONE);
                    imgView.setVisibility(View.VISIBLE);
//                    uriString=uri.toString();
//                    try {
//                        Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    imageBitmap= ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver,uri));
                }
            }
    );

    ActivityResultLauncher cameraLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bundle bundle = result.getData().getExtras();
                Bitmap bitmap = (Bitmap) bundle.get("data");
                imgView.setImageBitmap(bitmap);
                imgText.setVisibility(View.GONE);
                imgView.setVisibility(View.VISIBLE);
            }
        }
    });

    final int CAMERA_PERM = 100;

    private void askCameraPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
            },CAMERA_PERM);
        }
        else {
            dispatchTakePictureIntent();
        }
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        }
        else {
            Toast.makeText(MainActivity.this, "There is no app that support this action",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            dispatchTakePictureIntent();
        }
        else{
            Toast.makeText(this, "Allow Camera", Toast.LENGTH_SHORT).show();
        }
    }


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (mediaScanIntent.resolveActivity(getPackageManager()) != null) {
            take_image.launch("image/*");
        }
    }

    // Text Detection
    private void runTextRecognition(){
        Bitmap bitmap = ((BitmapDrawable) imgView.getDrawable()).getBitmap();
        int rotationDegree = 0;

        InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        //for devanagari Lipi
//        TextRecognizer recognizer= TextRecognizer.getClient(new DevanagariTextRecognizerOptions.Builder().build());

        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                processTextRecognition(visionText);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "Failed to recognize",Toast.LENGTH_SHORT).show();
                                    }
                                });
    }

    private void processTextRecognition(Text visionText) {
        String resultText = visionText.getText();
        StringBuilder text = new StringBuilder();
        for (Text.TextBlock block : visionText.getTextBlocks()) {
//            String blockText = block.getText();
//            text.append(blockText+" \n");
//            Point[] blockCornerPoints = block.getCornerPoints();
//            Rect blockFrame = block.getBoundingBox();
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();
                text.append(lineText+" \n");
//                Point[] lineCornerPoints = line.getCornerPoints();
//                Rect lineFrame = line.getBoundingBox();
//                for (Text.Element element : line.getElements()) {
//                    String elementText = element.getText();
////                    text.append(elementText+" \n");
//                    Point[] elementCornerPoints = element.getCornerPoints();
//                    Rect elementFrame = element.getBoundingBox();
//                }
            }
        }
        List<Text.TextBlock> blocks = visionText.getTextBlocks();
        if (blocks.size() == 0){
            Toast.makeText(MainActivity.this, "No Text Detected",Toast.LENGTH_LONG).show();
        }
//
//        StringBuilder text = new StringBuilder();
//
//        for (int i = 0; i<blocks.size();i++){
//            List<Text.Line> lines = blocks.get(i).getLines();
//            for (int j = 0; j<lines.size();j++){
//                List<Text.Element> elements = lines.get(j).getElements();
//                for (int k = 0; k<elements.size();k++){
//                    text.append(elements.get(k).getText() + " ");
//                }
//            }
//        }
        txtText.setVisibility(View.GONE);
        textArea.setVisibility(View.VISIBLE);
        textArea.setText(text);
    }
}
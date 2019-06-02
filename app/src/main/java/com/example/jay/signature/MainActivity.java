package com.example.jay.signature;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    //public static final int  SIGNATURE_ACTIVITY = 1;

    private Button signatureBtn, cancel, clear, save;

    File file;
    Dialog dialog;
    Signatures mSignature;
    LinearLayout linearContent;
    View view;
    Bitmap bitmap;

    // Creating Separate Directory for Saving Generated Images;
    String DIRECTORY = Environment.getExternalStorageDirectory().getPath() + "/DigitalSignature/";
    String pic_name = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    String storePath = DIRECTORY + pic_name + ".png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signatureBtn = (Button)findViewById(R.id.signature_btn);

        signatureBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                         dialog_action();
                    }
                }
        );

        file = new File(DIRECTORY);
        if(!file.exists()){
            file.mkdir();
        }

        // Dialog Function
        dialog = new Dialog(MainActivity.this);
        // Removing the features of normal Dialogs
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_signature);
        dialog.setCancelable(true);

    }
    // Function for Digital Signature
    public void dialog_action(){
        cancel = dialog.findViewById(R.id.cancel);
        clear = dialog.findViewById(R.id.clear);
        save = dialog.findViewById(R.id.save);

        linearContent = dialog.findViewById(R.id.linearLayout);
        mSignature = new Signatures(getApplicationContext(), null);
        mSignature.setBackgroundColor(Color.WHITE);

        // Dynamically generating layout through Java Code
        linearContent.addView(mSignature, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        save.setEnabled(false);
        view = linearContent;

        clear.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSignature.clear();
                        save.setEnabled(false);
                    }
                }
        );

        save.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        view.setDrawingCacheEnabled(true);
                        mSignature.save(view, storePath);
                        dialog.dismiss();

                        Toast.makeText(getApplicationContext(), "Successfully Saved", Toast.LENGTH_SHORT).show();
                        // Calling the same class
                        recreate();

                    }
                }
        );

        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v("tag", "Panel Cancelled");
                dialog.dismiss();
                // Calling the same class
                recreate();
            }
        });

        dialog.show();
    }

    public class Signatures extends View {
        private static final String TAG = "Signatures";
        private static final float STROKE_WIDTH = 5f;
        private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
        private Paint paint = new Paint();
        private Path path = new Path();

        private float lastTouchX;
        private float lastTouchY;
        private final RectF dirtyRect = new RectF();

        public Signatures(Context context, AttributeSet attrs) {
            super(context, attrs);
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(STROKE_WIDTH);
        }

        @SuppressLint("WrongThread")
        public void save(View v, String storagePath){
            Log.d(TAG, "save: width : " + v.getWidth());
            Log.d(TAG, "save: Height : " + v.getHeight());

            if(bitmap == null){
                bitmap = Bitmap.createBitmap(linearContent.getWidth(), linearContent.getHeight(), Bitmap.Config.RGB_565);
            }

            Canvas canvas = new Canvas(bitmap);

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(storagePath);
                v.draw(canvas);

                // Convert the output file to Image such as .png
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void clear(){
            path.reset();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {

            canvas.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();
            save.setEnabled(true);

            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    return true;

                case MotionEvent.ACTION_MOVE:

                case MotionEvent.ACTION_UP:
                    resetDirtyRect(eventX, eventY);
                    int historySize = event.getHistorySize();
                    for(int i=0; i<historySize; i++){
                        float historicalX = event.getHistoricalX(i);
                        float historicalY = event.getHistoricalY(i);
                        expandDirtyRect(historicalX, historicalY);
                        path.lineTo(historicalX, historicalY);

                    }

                    path.lineTo(eventX, eventY);
                    break;

                default:
                    debug("Ignoret Touch Event: " + event.toString());
                    return false;

            }

            invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

            lastTouchX = eventX;
            lastTouchY = eventY;

            return true;
        }

        private void debug(String string) {
            Log.v("log_tag", string);
        }

        private void expandDirtyRect(float historicalX, float historicalY){

            if (historicalX < dirtyRect.left) {
                dirtyRect.left = historicalX;
            } else if (historicalX > dirtyRect.right) {
                dirtyRect.right = historicalX;
            }

            if (historicalY < dirtyRect.top) {
                dirtyRect.top = historicalY;
            } else if (historicalY > dirtyRect.bottom) {
                dirtyRect.bottom = historicalY;
            }
        }

        private void resetDirtyRect(float eventX, float eventY){
            dirtyRect.left = Math.min(lastTouchX, eventX);
            dirtyRect.right = Math.max(lastTouchX, eventX);
            dirtyRect.top = Math.min(lastTouchY, eventY);
            dirtyRect.bottom = Math.max(lastTouchY, eventY);
        }
    }

}

package comk.example.kcleung235.fyp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.provider.MediaStore;
import android.support.annotation.MainThread;
import android.support.constraint.solver.widgets.Rectangle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SizeF;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessPdfRenderer;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import java.awt.font.TextAttribute;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.format.TextStyle;
import java.util.ArrayList;

public class OcrActivity extends AppCompatActivity {

    Bitmap image;
    private TessBaseAPI mTess;
    String datapath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        //init image
        image = BitmapFactory.decodeResource(getResources(), R.drawable.underline);
//        ImageView srcImage = (ImageView) findViewById(R.id.srcImage);
//        srcImage.setImageBitmap(image);

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

        OpenCVLoader.initDebug();
        drawRect(image);

    }

    public void runOCR(View view){
        String OCRresult = null;
        mTess.setImage(image);
        OCRresult = mTess.getUTF8Text();
        TextView OCRTextView = (TextView) findViewById(R.id.OCRTextView);
        OCRTextView.setText(OCRresult);

    }

    public void drawRect(Bitmap src){
        // Pre processing
        // Sharpen image
        // Init TessApi
        Bitmap bitmapForTess = sharpenImage(src);
        mTess.setImage(bitmapForTess);

        // Init Mat for drawing
        Mat mat_with_rect = new Mat(bitmapForTess.getHeight(), bitmapForTess.getWidth(), Imgproc.COLOR_RGB2GRAY);
        Utils.bitmapToMat(bitmapForTess, mat_with_rect);

        // Init variables
        ArrayList<Rect> rectArrayList = mTess.getWords().getBoxRects();
        Point top_left3 = new Point(0,0);
        Point bottom_right3 = new Point(0,0);
        double[] x = new double[2];
        double[] y = new double[2];

        Rect rect1 = rectArrayList.get(0);
        int k = rect1.top;
        TextView OCRTextView = (TextView) findViewById(R.id.OCRTextView);
        OCRTextView.setText(String.valueOf(k));

        x[0] = rect1.left;
        x[1] = rect1.top + (3 * rect1.height()/4);
        y[0] = rect1.right;
        y[1] = rect1.bottom;

        int roiLeft = (int) x[0];
        int roiTop = (int) x[1];
        int roiRight = (int) y[0];
        int roiBottom = (int) y[1];

        top_left3.set(x);
        bottom_right3.set(y);
//        Imgproc.rectangle(mat_with_rect, top_left3, bottom_right3, new Scalar(0,0,0),8);

        Mat canny = mat_with_rect.adjustROI(roiTop,  roiBottom, roiLeft, roiRight);
        Imgproc.Canny(mat_with_rect, canny, 0, 0);
        Mat lines = new Mat ();
        Imgproc.HoughLinesP(canny, lines, 1, Math.PI/180, rect1.width()/2);


        for(int i = 0; i < lines.cols(); i++){
            double[] val= lines.get(0, i);
            Point pt1 = new Point(val[0], val[1]);
            Point pt2 = new Point(val[2], val[3]);
            Imgproc.line(canny, pt1, pt2, new Scalar(0, 0, 255), 10);
        }

        Bitmap bitmap_canny = Bitmap.createBitmap(canny.cols(), canny.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(canny, bitmap_canny);

        // Drawing process
//        for (Rect rects : rectArrayList ){
//            x[0] = rects.left;
//            x[1] = rects.top +(rects.height()*3/4);
//            y[0] = rects.right;
//            y[1] = rects.bottom;
//            top_left3.set(x);
//            bottom_right3.set(y);
//            Imgproc.rectangle(mat_with_rect, top_left3, bottom_right3, new Scalar(0,0,0),5);
//
//        }


        // Convert mat with drawn Rect to Bitmap
        Bitmap bitmap_with_rect = Bitmap.createBitmap(mat_with_rect.cols(), mat_with_rect.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat_with_rect, bitmap_with_rect);

        ImageView imageView = (ImageView) findViewById(R.id.rect);
        imageView.setImageBitmap(bitmap_canny);

    }

    public Bitmap sharpenImage(Bitmap src){

        Mat source = new Mat(src.getHeight(), src.getWidth(), CvType.CV_32F);
        Utils.bitmapToMat(src, source);
        Imgproc.cvtColor(source, source, Imgproc.COLOR_RGB2GRAY);

        Mat blurred_mat = new Mat(source.size(), CvType.CV_8UC1);
        Mat destination = new Mat(source.size(), CvType.CV_32F);

        //GaussianBlur
//        Size kSize = new Size(3,3);
//        Imgproc.GaussianBlur(source, blurred_mat, kSize,2, 2);

        Mat kernel = new Mat(3,3, CvType.CV_32F){
            {
                put(0,0,-1);
                put(0,1,-1);
                put(0,2,-1);

                put(1,0,-1);
                put(1,1,9);
                put(1,2,-1);

                put(2,0,-1);
                put(2,1,-1);
                put(2,2,-1);
            }
        };

        Imgproc.threshold(source, destination, 42, 255, Imgproc.THRESH_BINARY|Imgproc.THRESH_OTSU);

//        Imgproc.filter2D(blurred_mat, destination, -1, kernel);

//        destination = enhanceConstrast(destination);

        Bitmap result = Bitmap.createBitmap(destination.cols(), destination.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destination, result);

        return result;
    }

    public Mat enhanceConstrast(Mat src){

        Mat source = src;
        Mat destination = new Mat(source.size(), source.type());

        //sharpen processing
        Imgproc.equalizeHist(source, destination);

        Bitmap result = Bitmap.createBitmap(destination.cols(), destination.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destination, result);

        return  destination;
    }

    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }


            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

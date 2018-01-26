package comk.example.kcleung235.fyp;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class OcrActivity extends AppCompatActivity {

    Bitmap image;
    private TessBaseAPI mTess;
    String datapath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        final String underline = "UNDERLINE";
        final String bold = "BOLD";
        final String italic = "ITALIC";


        //init image
        image = BitmapFactory.decodeResource(getResources(), R.drawable.underline);
        ImageView srcImage = findViewById(R.id.srcImage);
        srcImage.setImageBitmap(image);

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

        OpenCVLoader.initDebug();


        Mat src = getMatFromBitmap(image);
        Mat mat_rect = getMatWithRects(image, bold);
        Mat mat_canny = getCannyMat(src);
        Mat mat_hough = getHoughLineMat(src);

        Bitmap test = getBitmapFromMat(mat_rect);
        displayBitmap(R.id.rect, test);

        Bitmap test2 = getBitmapFromMat(mat_canny);
        displayBitmap(R.id.canny, test2);

        Bitmap test3 = getBitmapFromMat(mat_hough);
        displayBitmap(R.id.hough, test3);

    }


    public Bitmap getBitmapFromMat(Mat src){
        Bitmap result = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, result);
        return result;
    }

    public Mat getMatFromBitmap(Bitmap src){
        Mat result = new Mat(src.getHeight(), src.getWidth(), CvType.CV_8SC3);
        Utils.bitmapToMat(src, result);
        return result;
    }

    public void displayBitmap(int view_id, Bitmap bitmap){
        ImageView imageView = findViewById(view_id);
        imageView.setImageBitmap(bitmap);
    }

    public Mat enhanceConstrast(Mat src){
        Mat destination = new Mat(src.size(), src.type());
        Imgproc.equalizeHist(src, destination);

        return  destination;
    }

    public Bitmap sharpenImage(Bitmap src){
        Mat source = getMatFromBitmap(src);
        Mat gray = new Mat ();
        Imgproc.cvtColor(source, gray, Imgproc.COLOR_RGB2GRAY, 4);


        Mat resultMat = new Mat();
        //GaussianBlur
        Size kSize = new Size(3,3);
        Imgproc.GaussianBlur(gray, resultMat, kSize,2, 2);

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
        Imgproc.filter2D(gray, resultMat, -1, kernel);

        Imgproc.threshold(gray, resultMat, 42, 255, Imgproc.THRESH_OTSU);

        resultMat = enhanceConstrast(gray);

        return getBitmapFromMat(resultMat);
    }

    public Mat getROI (Mat src, Rect rect){
        double[] x = new double[2];
        double[] y = new double[2];

        x[0] = rect.left;
        x[1] = rect.top + (3 * rect.height()/4);
        y[0] = rect.right;
        y[1] = rect.bottom;

        int roiLeft = (int) x[0];
        int roiTop = (int) x[1];
        int roiRight = (int) y[0];
        int roiBottom = (int) y[1];

        Range row_range = new Range(roiTop, roiBottom);
        Range col_range = new Range(roiLeft, roiRight);

        return  new Mat(src, row_range, col_range);
    }

    public Mat getMatWithRects(Bitmap src, String type){

        Bitmap bitmapForTess = sharpenImage(src);
        mTess.setImage(bitmapForTess);

        Mat result = getMatFromBitmap(bitmapForTess);

        ArrayList<Rect> rectArrayList = mTess.getWords().getBoxRects();
        Point top_left = new Point(0,0);
        Point bottom_right = new Point(0,0);
        double[] x = new double[2];
        double[] y = new double[2];

        for (Rect rects : rectArrayList ){
            if (type.equals("underline")){
                rects.top = rects.top + (rects.height()*3/4);
            }
            else if (type.equals("Bold")){}
            else {}
            x[0] = rects.left;
            x[1] = rects.top;
            y[0] = rects.right;
            y[1] = rects.bottom;
            top_left.set(x);
            bottom_right.set(y);
            Imgproc.rectangle(result, top_left, bottom_right, new Scalar(0,0,0),5);

        }

        return result;
    }

    public Mat getCannyMat(Mat src){
        Mat result = new Mat(src.size(), src.type());
        Imgproc.Canny(src, result, 80 ,100);
        return result;
    }

    public Mat getHoughLineMat(Mat src){
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY, 4);

        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 80 ,100);

        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI/180, 150, 70, 8);
        for(int i = 0; i < lines.rows(); i++) {
            for (int j = 0; j < lines.cols(); j++) {
                double[] data = lines.get(i, j);
                double x1 = data[0],
                       y1 = data[1],
                       x2 = data[2],
                       y2 = data[3];

                Point start = new Point(x1, y1);
                Point end = new Point(x2, y2);
                Imgproc.line(src, start, end, new Scalar(0), 4);
            }
        }

        return src;
    }

    public void runOCR(View view){
        String OCRresult;
        mTess.setImage(image);
        OCRresult = mTess.getUTF8Text();
        TextView OCRTextView = findViewById(R.id.OCRTextView);

        SpannableString content = new SpannableString(OCRresult);
        content.setSpan(new UnderlineSpan(), 0, OCRresult.length(), 0);
        char[] chars = new char[4];
        OCRresult.getChars(0,4,chars,0);
        String test = "";

        for(int i =0 ; i < chars.length ; i++){
            test += chars[i];
        }

        OCRTextView.setText(test);

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

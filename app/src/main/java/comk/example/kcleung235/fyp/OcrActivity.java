package comk.example.kcleung235.fyp;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.PageIterator;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class OcrActivity extends AppCompatActivity {

    Bitmap image;
    Bitmap normal_image;
    Bitmap italic_image;
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
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        image = BitmapFactory.decodeResource(getResources(), R.drawable.book_img, options);
        normal_image = BitmapFactory.decodeResource(getResources(), R.drawable.normal, options);
        italic_image = BitmapFactory.decodeResource(getResources(), R.drawable.italic_bk, options);
        ImageView srcImage = findViewById(R.id.srcImage);
        srcImage.setImageBitmap(image);

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

        OpenCVLoader.initDebug();


//        Mat src = getMatFromBitmap(image);
//        Mat mat_canny = getCannyMat(src);
//        Mat contourMat = charBoundingRect(src);
//
//        Bitmap test = getBitmapFromMat(contourMat);
//        displayBitmap(R.id.rect, test);
//
//        Bitmap test2 = getBitmapFromMat(mat_canny);
//        displayBitmap(R.id.canny, test2);

        calcF(image);

    }

    public Bitmap getBitmapFromMat(Mat src){
        Bitmap result = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, result);
        return result;
    }

    public Mat getMatFromBitmap(Bitmap src){
        Mat result = new Mat(src.getHeight(), src.getWidth(), CvType.CV_8UC3);
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
        Mat result = new Mat();
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY, 4);

        Imgproc.threshold(gray, gray, 127,250, Imgproc.THRESH_BINARY);
        double sigma = Math.sqrt(2.0);
        Mat gb = new Mat();
        Imgproc.GaussianBlur(gray, gb, new Size(3,3), sigma);
        Imgproc.Canny(gb, result, 100 ,250);

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
        mTess.setImage(image);
        String OCRresult = mTess.getUTF8Text();;

        String word;
        Rect word_postition;
        Mat mat = getMatFromBitmap(image);

        ResultIterator resultIterator = mTess.getResultIterator();
        do {
            word = resultIterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            word_postition = resultIterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD);

            mat.adjustROI(word_postition.top, word_postition.bottom, word_postition.left, word_postition.right);
            Mat threshold = new Mat();
            Imgproc.threshold(mat,threshold, 127, 250, Imgproc.THRESH_BINARY);



        } while (resultIterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD));

    }

    private Mat charBoundingRect(Mat src){

        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Mat canny = getCannyMat(src);
        Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        Mat result = new Mat(src.size(),src.type());
        src.assignTo(result);

        hierarchy.release();
        for ( int contourIdx=0; contourIdx < contours.size(); contourIdx++ ){
            int character_area_lower_thresh = 5;
            if (Imgproc.contourArea(contours.get(contourIdx)) > character_area_lower_thresh){
                // Minimum size allowed for consideration
                MatOfPoint2f approxCurve = new MatOfPoint2f();
                MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contourIdx).toArray());
                //Processing on mMOP2f1 which is in type MatOfPoint2f
                double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                //Convert back to MatOfPoint
                MatOfPoint points = new MatOfPoint(approxCurve.toArray());

                // Get bounding rect of contour
                org.opencv.core.Rect rect = Imgproc.boundingRect(points);

                if(rect.height > 10 && rect.width < 70){
                    Imgproc.rectangle(result, new Point(rect.x, rect.y),
                            new Point(rect.x + rect.width, rect.y + rect.height),
                            new Scalar(255, 0, 0, 255),
                            1);
                }
            }
        }
        return result;
    }

    public boolean underlineDetection(Bitmap image){
        boolean result = false;

        Mat check_underline_mat = new Mat();
        Mat src = getMatFromBitmap(image);
        src.assignTo(check_underline_mat);

        mTess.setImage(image);
        mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_WORD);

        ArrayList<Rect> rects = mTess.getWords().getBoxRects();
        Mat lines = new Mat();
        for( Rect r : rects ){
            check_underline_mat.adjustROI(r.top, r.bottom, r.left, r.right);
            lines = getHoughLineMat(check_underline_mat);
        }

        return result;
    }

    public void histOf(Bitmap bitmap){
        Mat src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC3);
        Utils.bitmapToMat(bitmap, src);

        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        List<Mat> graym = new ArrayList<Mat>();
        graym.add(gray);
        MatOfInt histSize = new MatOfInt(256);
        final MatOfFloat histRange = new MatOfFloat(0f, 256f);
        boolean accumulate = false;
        Mat hist = new Mat();
        Imgproc.calcHist(graym, new MatOfInt(0), new Mat(), hist, histSize, histRange, accumulate);

        double data[] = src.get(50,300);
        double data1[] = gray.get(50,300);

        String s = "";
        String t = "";
        int cast2;
        for (int i = 0; i<data.length; i++) {
            cast2 = (int) data[i];
            s += String.valueOf(cast2) + ",";
        }

        int cast;
        for(int j = 0; j<data1.length; j++){
            cast = (int) data1[j];
            t += String.valueOf(cast) + ",";
        }
        ImageView iv = findViewById(R.id.rect);
        iv.setImageBitmap(getBitmapFromMat(gray));
        TextView tv = findViewById(R.id.OCRTextView);
        tv.setText(s+"   "+t);
    }

    public void calcF(Bitmap src){
        Mat src_mat = new Mat(src.getHeight(), src.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, src_mat);

        Mat gray = new Mat();
        Imgproc.cvtColor(src_mat, gray, Imgproc.COLOR_RGB2GRAY);

        Mat blur = new Mat();
        Imgproc.GaussianBlur(gray, blur, new Size(3,3),0,0);

//        Mat big = new Mat();
//        Imgproc.resize(blur, big, new Size( src_mat.cols() * 15 , src_mat.rows() * 15 ));

        Mat blur2 = new Mat();
        Imgproc.bilateralFilter(gray, blur2, 3, 135,135);

//        Mat blur2_big = new Mat();
//        Imgproc.resize(blur2, blur2_big, new Size( src_mat.cols() * 15 , src_mat.rows() * 15 ));

        Mat black = new Mat();
        Imgproc.threshold( blur, black, 25, 255, Imgproc.THRESH_BINARY|Imgproc.THRESH_OTSU);

//        Mat black_big = new Mat();
//        Imgproc.resize(black, black_big, new Size( src_mat.cols() * 15 , src_mat.rows() * 15 ));


        ImageView iv = findViewById(R.id.rect);
        iv.setImageBitmap(getBitmapFromMat(blur));

        ImageView iv2 = findViewById(R.id.canny);
        iv2.setImageBitmap(getBitmapFromMat(blur2));

        ImageView iv3 = findViewById(R.id.gray);
        iv3.setImageBitmap(getBitmapFromMat(black));

        String t;
        mTess.setImage(getBitmapFromMat(src_mat));
        t = mTess.getUTF8Text();
        ResultIterator ri1 = mTess.getResultIterator();
        float k = ri1.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);

        mTess.setImage(getBitmapFromMat(blur));
        t = mTess.getUTF8Text();
        ResultIterator ri3 = mTess.getResultIterator();
        float bc = ri3.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);

        mTess.setImage(getBitmapFromMat(blur2));
        t = mTess.getUTF8Text();
        ResultIterator ri = mTess.getResultIterator();
        float i = ri.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);

        mTess.setImage(getBitmapFromMat(black));
        t = mTess.getUTF8Text();

        ResultIterator ri2 = mTess.getResultIterator();
        float j = ri2.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);

        double pixel_intensity[];
        int pix_int;
        float numberOfBlackPix = 0;
        for ( int row = 0 ; row < black.rows() ; row++ ){
            for ( int col = 0 ; col < black.cols() ; col++ ){
                pixel_intensity = black.get(row, col);
                pix_int = (int) pixel_intensity[0];
                if( pix_int == 0 ){
                    numberOfBlackPix ++;
                }
            }
        }
        float totalPix = blur.cols() * blur.rows() - numberOfBlackPix;
        float confidence = numberOfBlackPix/totalPix;

        TextView tv = findViewById(R.id.OCRTextView);
        tv.setText( String.valueOf(numberOfBlackPix) + " " + String.valueOf(confidence) +" "+ String.valueOf(k) + " " + String.valueOf(bc) + " " + String.valueOf(i) + " " + String.valueOf(j) );
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

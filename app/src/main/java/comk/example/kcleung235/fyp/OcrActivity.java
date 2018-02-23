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
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        image = BitmapFactory.decodeResource(getResources(), R.drawable.border, options);
        normal_image = BitmapFactory.decodeResource(getResources(), R.drawable.normal, options);
        italic_image = BitmapFactory.decodeResource(getResources(), R.drawable.italic_bk, options);
//        ImageView srcImage = findViewById(R.id.srcImage);
//        srcImage.setImageBitmap(image);

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

        OpenCVLoader.initDebug();

//        checkBold(getMatFromBitmap(image));
//
//        Mat trial = getMatFromBitmap(image);
//        Mat test = findCharBoundingRect(trial);
//        displayBitmap(R.id.testImage, getBitmapFromMat(test));

        borderRemoval(getMatFromBitmap(image));
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

    public Mat getMatWithRects(Bitmap src){
        mTess.setImage(src);
        Mat result = getMatFromBitmap(src);

        ArrayList<Rect> rectArrayList = mTess.getWords().getBoxRects();
        Point top_left = new Point(0,0);
        Point bottom_right = new Point(0,0);
        double[] x = new double[2];
        double[] y = new double[2];

        for (Rect rects : rectArrayList ){
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
        Imgproc.Canny(gray, edges, 100 ,250);

        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI/180, 150, 200, 5);
        for(int i = 0; i < lines.rows(); i++) {
            for (int j = 0; j < lines.cols(); j++) {
                double[] data = lines.get(i, j);
                double x1 = data[0],
                       y1 = data[1],
                       x2 = data[2],
                       y2 = data[3];

                Point start = new Point(x1, y1);
                Point end = new Point(x2, y2);
                Imgproc.line(src, start, end, new Scalar(0,0,255,255), 2);
            }
        }

        Mat big = new Mat();
        Imgproc.resize(src, big, new Size( src.cols() * 5 , src.rows() * 5 ));

        return big;
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

    private Mat findCharBoundingRect(Mat src){
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        Mat b_blur = new Mat();
        Mat dilate = new Mat();
        Mat canny = new Mat();

        Imgproc.bilateralFilter(gray, b_blur, 15, 60,60);

        Imgproc.Canny(b_blur, canny, 100, 200);

        Mat dj = new Mat();
        Mat element2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(40,10));
        Imgproc.dilate(canny, dj, element2);

        Mat dj2 = new Mat();
        Imgproc.dilate(dj, dj2, element2);

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(src.width(),src.height()/2));


//        ArrayList<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//        org.opencv.core.Rect rect;
//
//        Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );
//        List<Point> lp;
//        for( int i = 0; i< contours.size(); i++ ) {
//             lp = contours.get(i).toList();
//             Imgproc.floodFill(canny, new Mat(), lp.get(0), new Scalar(255, 255, 255, 255));
//             Imgproc.fillConvexPoly(canny, contours.get(i), new Scalar(255, 255, 255, 255));
//        }

        Mat result = dj.clone();
        return result;
    }

    public boolean underlineDetection(Bitmap image){
        boolean result = false;

        Mat check_underline_mat = getMatFromBitmap(image);

        mTess.setImage(image);
        mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_WORD);

        ArrayList<Rect> rects = mTess.getWords().getBoxRects();
        Mat lines = new Mat();
        for( Rect r : rects ){
            check_underline_mat.adjustROI(r.top, r.bottom, r.left, r.right);
            lines = getHoughLineMat(check_underline_mat);
        }

        if(lines.rows() != 0){
            result = true;
        }

        return result;
    }

    public void checkBold(Mat mat){
        Mat src_mat = mat;
        Mat gray_src_mat = new Mat();
        Imgproc.cvtColor(src_mat, gray_src_mat, Imgproc.COLOR_RGB2GRAY);

        ArrayList<Rect> rectArrayList = mTess.getWords().getBoxRects();
        Mat rect_scanning_region;
        double[] pixel_data;
        double black_pixel_count = 0;

        for ( int r = 0 ; r < gray_src_mat.rows() ; r++ ){
            for ( int c = 0 ; c < gray_src_mat.cols() ; c++ ){
                pixel_data = gray_src_mat.get( r , c );
                if ( pixel_data[0] == 0 ){
                    black_pixel_count++;
                }
            }
        }

        double normalised_count = 100 * ( black_pixel_count / (gray_src_mat.rows() * gray_src_mat.cols()) );
        TextView tv = findViewById(R.id.text);
        tv.setText( String.valueOf(black_pixel_count) + "   " + String.valueOf(gray_src_mat.rows() * gray_src_mat.cols()) + "   " + String.valueOf(normalised_count) );

//        double pixel_intensity[];
//        int pix_int;
//        float numberOfBlackPix = 0;
//        for ( int row = 0 ; row < black.rows() ; row++ ){
//            for ( int col = 0 ; col < black.cols() ; col++ ){
//                pixel_intensity = black.get(row, col);
//                pix_int = (int) pixel_intensity[0];
//                if( pix_int == 0 ){
//                    numberOfBlackPix ++;
//                }
//            }
//        }
//        float totalPix = blur.cols() * blur.rows() - numberOfBlackPix;
//        float confidence = numberOfBlackPix/totalPix;
//        ImageView iv = findViewById(R.id.testImage);
//        iv.setImageBitmap(getBitmapFromMat(black));
    }

    public Mat borderRemoval(Mat src){
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        Mat b_blur = new Mat();
        Mat dilate = new Mat();
        Mat canny = new Mat();

        Imgproc.bilateralFilter(gray, b_blur, 15, 60,60);

        Imgproc.Canny(b_blur, canny, 150, 200);

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(45,80));
        Imgproc.dilate(canny, dilate, element);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        double largest_area=0;
        org.opencv.core.Rect rect = new org.opencv.core.Rect();
        int index = 0;

        Imgproc.findContours(dilate, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );

        for( int i = 0; i< contours.size(); i++ ) {
            double a= Imgproc.contourArea( contours.get(i),false);
            if( a > largest_area ){
                largest_area = a;
                rect = Imgproc.boundingRect(contours.get(i));
                index = i;
            }
        }

        MatOfPoint2f origin = new MatOfPoint2f(contours.get(index).toArray());
        MatOfPoint2f approxCurve = new MatOfPoint2f();

        Imgproc.approxPolyDP(origin, approxCurve, 6.0, false);

        Point[] pts = approxCurve.toArray();

        for (int id = 0 ; id < pts.length ; id++ ){
            Imgproc.circle(src, pts[id], 10, new Scalar(255,255,0,255), 25);
        }

        RotatedRect test = Imgproc.minAreaRect(origin);

        Point[] vertices = new Point[4];
        test.points(vertices);

        Imgproc.line(src, vertices[1] , vertices[2], new Scalar(0,0,255,255), 25);

        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(contours.get(index), hull);

        MatOfInt4 conDefect = new MatOfInt4();
        Imgproc.convexityDefects(contours.get(index), hull, conDefect);

        int[] ints = conDefect.toArray();
        int start_index;
        int end_index;
        Point[] pts_array = contours.get(index).toArray();
        int init=0;
        double min_x_min_y = 0;
        double max_x_min_y = 0;
        double min_x_max_y = 0;
        double max_x_max_y = 0;
        Point top_left_point = new Point(),
              top_right_point = new Point(),
              bottom_left_point = new Point(),
              bottom_right_point = new Point();

        List<Point> vert = new ArrayList<>();

        for (int id = 0 ; id < ints.length ; id += 4 ){
            start_index = ints[id];
            end_index = ints[id+1];
            Imgproc.line(src, pts_array[start_index] , pts_array[end_index], new Scalar(0,255,0,255), 25);

            if ( init == 0 ){
                min_x_min_y = pts_array[start_index].x + pts_array[start_index].y;
                max_x_min_y = pts_array[start_index].x - pts_array[start_index].y;
                min_x_max_y = pts_array[start_index].y - pts_array[start_index].x;
                max_x_max_y = pts_array[start_index].x + pts_array[start_index].y;
                init = -1;
            }

            if ( (pts_array[start_index].x + pts_array[start_index].y) < min_x_min_y ){
                min_x_min_y = pts_array[start_index].x + pts_array[start_index].y;
                top_left_point = pts_array[start_index];
                vert.add(top_left_point);
            }

            if ( pts_array[start_index].x - pts_array[start_index].y > max_x_min_y ){
                max_x_min_y = pts_array[start_index].x - pts_array[start_index].y;
                top_right_point = pts_array[start_index];
                vert.add(top_right_point);
            }

            if ( pts_array[start_index].y - pts_array[start_index].x > min_x_max_y ){
                min_x_max_y = pts_array[start_index].y - pts_array[start_index].x;
                bottom_left_point = pts_array[start_index];
                vert.add(bottom_left_point);
            }

            if ( pts_array[start_index].x + pts_array[start_index].y > max_x_max_y ){
                max_x_max_y = pts_array[start_index].x + pts_array[start_index].y;
                bottom_right_point = pts_array[start_index];
                vert.add(bottom_right_point);
            }

        }
        Imgproc.circle(src, top_left_point, 20, new Scalar(255,0,0,255), 40);
        Imgproc.circle(src, top_right_point, 20, new Scalar(255,0,0,255), 40);
        Imgproc.circle(src, bottom_left_point, 20, new Scalar(255,0,0,255), 25);
        Imgproc.circle(src, bottom_right_point, 20, new Scalar(255,0,0,255), 25);

        Imgproc.drawContours(src, contours, index, new Scalar(255,0,255,255),25);

//        Mat transform;
//        MatOfPoint2f perspective = new MatOfPoint2f(top_left_point, top_right_point, bottom_left_point, bottom_right_point);
//        MatOfPoint2f dst = new MatOfPoint2f(vertices[0],vertices[1],vertices[2],vertices[3]);
//
//        Mat transformed = new Mat(src.size(), src.type());
//        transform = Imgproc.getPerspectiveTransform(perspective, dst);
//
//        Imgproc.warpPerspective(src, transformed, transform, src.size());

        displayBitmap(R.id.testImage, getBitmapFromMat(src));
//        displayBitmap(R.id.testImage2, getBitmapFromMat(transformed));

        String text_value = "";
        String text_value2 = "";

        for(int i = 0; i<4 ;i++){
            text_value += String.valueOf( (int)vert.get(i).x ) + ", " + String.valueOf( (int)vert.get(i).y ) + "\n";
            text_value2 += String.valueOf( (int)vertices[i].x ) + ", " + String.valueOf( (int)vertices[i].y ) + "\n";
        }

        TextView tv = findViewById(R.id.text);
        tv.setText( text_value + text_value2 );

        return src;
    }

    public float findConfidence(Bitmap image){
        float result;

        mTess.setImage(image);
        mTess.getUTF8Text();
        ResultIterator ri = mTess.getResultIterator();
        result = ri.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);

        return result;
    }

    public void compute_deskew(Bitmap image){
        Mat mat = getMatFromBitmap(image);
        Mat gray = new Mat();

        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);

        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,3));
        Mat erode = new Mat();
        Imgproc.erode(gray, erode, element);

        Mat white = new Mat();
        Core.findNonZero(erode, white);

        MatOfPoint mp = new MatOfPoint(white);
        MatOfPoint2f matOfPoint = new MatOfPoint2f(mp.toArray());

        RotatedRect rect = Imgproc.minAreaRect(matOfPoint);
        org.opencv.core.Rect r = Imgproc.boundingRect(mp);


        double angle = rect.angle;
        if (angle < -45.){
            angle += 90.;
        }

        Mat rot_mat = Imgproc.getRotationMatrix2D(rect.center, angle, 1);

        Mat rotated = new Mat();
        Imgproc.warpAffine(gray, rotated, rot_mat, mat.size(), Imgproc.INTER_CUBIC);

        Size box_size = rect.size;
        if (rect.angle < -45.) {
            double aux = box_size.width;
            box_size.width = box_size.height;
            box_size.height = aux;
        }

        Point[] vertices = new Point[4];
        rect.points(vertices);

        for (int i = 0 ; i < vertices.length ; i++ ) {
            Imgproc.line(erode, vertices[i], vertices[(i + 1) % 4], new Scalar(255, 255, 255, 255), 5);
        }

        Mat before_transform = new MatOfPoint2f(vertices[0],vertices[2],vertices[1],vertices[3]);
        Mat dst = new MatOfPoint2f(new Point(0, 0), new Point(r.width - 1, 0), new Point(0, r.height - 1), new Point(r.width - 1, r.height - 1));

        Mat crop = gray.submat(r);
        Mat result = new Mat();
        Mat transform = Imgproc.getPerspectiveTransform(before_transform, dst);
        Imgproc.warpPerspective(crop, result, transform, crop.size());

        displayBitmap(R.id.testImage2, getBitmapFromMat(crop));
        displayBitmap(R.id.testImage, getBitmapFromMat(result));
        displayBitmap(R.id.testImage3, getBitmapFromMat(rotated));
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

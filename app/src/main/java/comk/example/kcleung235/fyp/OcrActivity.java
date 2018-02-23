package comk.example.kcleung235.fyp;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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

        getStraightenedMat(getMatFromBitmap(image));
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

    public Mat getDilatedMat(Mat src){
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        Mat b_blur = new Mat();
        Imgproc.bilateralFilter(gray, b_blur, 15, 60,60);

        Mat canny = new Mat();
        Imgproc.Canny(b_blur, canny, 150, 200);

        Mat dilate = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(60,100));
        Imgproc.dilate(canny, dilate, element);
        
        return dilate;
    }
    
    public MatOfPoint getLargestContourMat(List<MatOfPoint> contour_list){
        MatOfPoint largestAreaContour = new MatOfPoint();
        
        double largest_area = 0;
        for( MatOfPoint contour_i : contour_list) {
            double area = Imgproc.contourArea( contour_i,false);
            if( area > largest_area ){
                largest_area = area;
                largestAreaContour = contour_i;
            }
        }
        
        return largestAreaContour;
    }

    public Mat getBorderRemovedMat(Mat src){
        Mat forRemoveMat = getStraightenedMat(src);
        Mat dilate = getDilatedMat(forRemoveMat);

        ArrayList<MatOfPoint> contour_list = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dilate, contour_list, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );

        MatOfPoint contourOfblob = getLargestContourMat(contour_list);

        org.opencv.core.Rect rect = Imgproc.boundingRect(contourOfblob);

        Mat result = src.submat(rect);
        return result;
    }

    public Mat getStraightenedMat(Mat src){
        Mat dilate = getDilatedMat(src);

        ArrayList<MatOfPoint> contour_list = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dilate, contour_list, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );

        MatOfPoint largestAreaContour = getLargestContourMat(contour_list);


        RotatedRect rotatedRect = Imgproc.minAreaRect(new MatOfPoint2f(largestAreaContour.toArray()));
        Point[] rect_vertices = new Point[4];
        rotatedRect.points(rect_vertices);

        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(largestAreaContour, hull);

        MatOfInt4 conDefect = new MatOfInt4();
        Imgproc.convexityDefects(largestAreaContour, hull, conDefect);

        int[] convexDefectArray = conDefect.toArray();
        Point[] cntPts = largestAreaContour.toArray();

        boolean init = false;
        double currentTL = 0,
               currentTR = 0,
               currentBL = 0,
               currentBR = 0;

        Point tl = new Point(),
              tr = new Point(),
              bl = new Point(),
              br = new Point();

        for (int id = 0 ; id < convexDefectArray.length ; id += 4){
            int i = convexDefectArray[id];

            if ( !init ){
                currentTL = cntPts[i].x + cntPts[i].y;
                currentTR = cntPts[i].x - cntPts[i].y;
                currentBL = cntPts[i].y - cntPts[i].x;
                currentBR = cntPts[i].x + cntPts[i].y;
                init = true;
            }

            if ( (cntPts[i].x + cntPts[i].y) < currentTL ){
                currentTL = cntPts[i].x + cntPts[i].y;
                tl = cntPts[i];
            }

            if ( cntPts[i].x - cntPts[i].y > currentTR ){
                currentTR = cntPts[i].x - cntPts[i].y;
                tr = cntPts[i];
            }

            if ( cntPts[i].y - cntPts[i].x > currentBL ){
                currentBL = cntPts[i].y - cntPts[i].x;
                bl = cntPts[i];
            }

            if ( cntPts[i].x + cntPts[i].y > currentBR ){
                currentBR = cntPts[i].x + cntPts[i].y;
                br = cntPts[i];
            }

        }

        if (bl.y > br.y) br.y = bl.y;
        else bl.y = br.y;

        MatOfPoint2f perspective = new MatOfPoint2f(tl, tr, bl, br);
        MatOfPoint2f dst = new MatOfPoint2f(rect_vertices[1],rect_vertices[2],rect_vertices[0],rect_vertices[3]);
        Mat transform = Imgproc.getPerspectiveTransform(perspective, dst);

        Mat transformed = new Mat(src.size(), src.type());
        Imgproc.warpPerspective(src, transformed, transform, src.size());

        displayBitmap(R.id.testImage, getBitmapFromMat(src));
        displayBitmap(R.id.testImage2, getBitmapFromMat(transformed));

        Mat dilate2 = getDilatedMat(transformed);
        displayBitmap(R.id.testImage3, getBitmapFromMat(dilate2));
        displayBitmap(R.id.testImage4, getBitmapFromMat(dilate));

        String text_value = "";
        String text_value2 = "";

        for(int i = 0; i<4 ;i++){
            text_value2 += String.valueOf( (int)rect_vertices[i].x ) + ", " + String.valueOf( (int)rect_vertices[i].y ) + "\n";
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

    public Mat getDeskewMat(Bitmap image){
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

        Mat crop = gray.submat(r);
        return crop;
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

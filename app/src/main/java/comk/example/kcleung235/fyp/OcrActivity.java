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
    private TessBaseAPI mTess;
    String datapath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        //init image
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        image = BitmapFactory.decodeResource(getResources(), R.drawable.border4, options);

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

        OpenCVLoader.initDebug();

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

    private Mat extractWordsBoundingRect(Mat src){
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        Mat b_blur = new Mat();
        Imgproc.bilateralFilter(gray, b_blur, 9, 10,10);

        Mat canny = new Mat();
        Imgproc.Canny(b_blur, canny, 30, 80);

        Mat closing = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,3));
        Imgproc.morphologyEx(canny, closing, Imgproc.MORPH_CLOSE, kernel, new Point(-1,-1), 3);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(closing, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        List<org.opencv.core.Rect> rectList = new ArrayList<>();

        for(int i = 0 ; i<contours.size()-1; i++) {
            org.opencv.core.Rect rect1;

            if (Imgproc.contourArea(contours.get(i))>50 && Imgproc.contourArea(contours.get(i))<8000) {
                rect1 = Imgproc.boundingRect(contours.get(i));

                if (rectList.isEmpty()){
                    rectList.add(rect1);
                }else {

                    for (int c = 0 ; c<rectList.size(); c++) {
                        org.opencv.core.Rect r = rectList.get(c);
                        int xDiff = Math.min(Math.abs(rect1.x - (r.x + r.width)),Math.abs(r.x - (rect1.x + rect1.width)));

                        if (Math.abs(rect1.y - r.y) < 10 && xDiff < 20) {
                            int newRectY = Math.min(r.y, rect1.y);
                            int newRectX = Math.min(r.x, rect1.x);
                            int newWidth = (int) Math.max(r.br().x, rect1.br().x) - newRectX;
                            int newHeight = (int) Math.max(r.br().y, rect1.br().y) - newRectY;
                            rect1 = new org.opencv.core.Rect(newRectX, newRectY, newWidth, newHeight);
                            rectList.remove(r);
                        }
                    }
                    rectList.add(rect1);
                }
            }
        }

        Mat result = src.clone();
        for (org.opencv.core.Rect r : rectList) {
            Imgproc.rectangle(src, r.tl(), r.br(), new Scalar(255,0,0,255),5);
        }
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
        Imgproc.bilateralFilter(gray, b_blur, 9, 10,10);


        Mat canny = new Mat();
        Imgproc.Canny(b_blur, canny, 30, 80);

        Mat closing = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,3));
        Imgproc.morphologyEx(canny, closing, Imgproc.MORPH_CLOSE, kernel, new Point(-1,-1), 3);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(closing, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        List<org.opencv.core.Rect> rectList = new ArrayList<>();

        for(int i = 0 ; i<contours.size()-1; i++) {
            org.opencv.core.Rect rect1;

            if (Imgproc.contourArea(contours.get(i))>50 && Imgproc.contourArea(contours.get(i))<8000) {
                rect1 = Imgproc.boundingRect(contours.get(i));

                if (rectList.isEmpty()){
                    rectList.add(rect1);
                }else {

                    for (int c = 0 ; c<rectList.size(); c++) {
                        org.opencv.core.Rect r = rectList.get(c);
                        int xDiff = Math.min(Math.abs(rect1.x - (r.x + r.width)),Math.abs(r.x - (rect1.x + rect1.width)));

                        if (Math.abs(rect1.y - r.y) < 10 && xDiff < 20) {
                                int newRectY = Math.min(r.y, rect1.y);
                                int newRectX = Math.min(r.x, rect1.x);
                                int newWidth = (int) Math.max(r.br().x, rect1.br().x) - newRectX;
                                int newHeight = (int) Math.max(r.br().y, rect1.br().y) - newRectY;
                                rect1 = new org.opencv.core.Rect(newRectX, newRectY, newWidth, newHeight);
                                rectList.remove(r);
                        }
                    }
                    rectList.add(rect1);
                }
            }
        }
        for (org.opencv.core.Rect r : rectList) {
            Imgproc.rectangle(src, r.tl(), r.br(), new Scalar(255,0,0,255),5);
        }

        Mat dilate = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10,80));
        Imgproc.dilate(canny, dilate, element);

        displayBitmap(R.id.testImage, getBitmapFromMat(src));

        Imgproc.findContours(dilate, contours,new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        MatOfPoint lgContour = getLargestContourMat(contours);
        double lgArea = Imgproc.contourArea(lgContour,false);
        boolean conti = true;
        Mat mor = new Mat();
        do{
            Imgproc.findContours(mor, contours,new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
            MatOfPoint lgContour2 = getLargestContourMat(contours);
            double temp = Imgproc.contourArea(lgContour2,false);

            if (temp < lgArea){
                lgArea = temp;
            }else {
                conti = false;
            }

        }while (conti);

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
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        Mat b_blur = new Mat();
        Imgproc.bilateralFilter(gray, b_blur, 9, 10,10);

        Mat canny = new Mat();
        Imgproc.Canny(b_blur, canny, 30, 80);

        Mat e = new Mat();
        Mat ee = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,1));
        Imgproc.erode(canny,e,ee);

        Mat dilate = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20,70));
        Imgproc.dilate(e, dilate, element);

        Mat element2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9,3));
        Imgproc.erode(dilate, dilate, element2);


        ArrayList<MatOfPoint> contour_list = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dilate, contour_list, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE );

        MatOfPoint textBlockContour = getLargestContourMat(contour_list);

        org.opencv.core.Rect rect = Imgproc.boundingRect(textBlockContour);

        Mat result = src.submat(rect);
        return result;
    }

    public Mat computeLargestBlob(Mat src){
        Mat gray = new Mat();
        if (src.channels()==1) {
            gray = src;
        }else{
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
        }

        Mat b_blur = new Mat();
        Imgproc.bilateralFilter(gray, b_blur, 9, 10,10);

        Mat canny = new Mat();
        Imgproc.Canny(b_blur, canny, 30, 80);

        Mat result = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,1));
        Imgproc.erode(canny, result, element);

        Mat element2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20,70));
        Imgproc.dilate(result, result, element2);

        Mat element3 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9,3));
        Imgproc.erode(result, result, element3);

        return result;
    }

    public Mat getStraightenedMat(Mat src){
        Mat mat = getDeskewMat(src);

        Mat textBlock = computeLargestBlob(mat);

        displayBitmap(R.id.testImage2, getBitmapFromMat(textBlock));

        ArrayList<MatOfPoint> contour_list = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(textBlock, contour_list, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );

        MatOfPoint largestAreaContour = getLargestContourMat(contour_list);

        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(largestAreaContour, hull);

        MatOfInt4 conDefect = new MatOfInt4();
        Imgproc.convexityDefects(largestAreaContour, hull, conDefect);

        int[] convexDefectArray = conDefect.toArray();
        Point[] cntPts = largestAreaContour.toArray();

        boolean init = false;
        double topLeftIndicator = 0,
               topRightIndicator = 0,
               bottomLeftIndicator = 0,
               bottomRightIndicator = 0;

        Point contourTopLeft = new Point(),
              contourTopRight = new Point(),
              contourBottomLeft = new Point(),
              contourBottomRight = new Point();

        for (int id = 0 ; id < convexDefectArray.length ; id += 4){
            int i = convexDefectArray[id];
            int j = convexDefectArray[id+1];

            if ( !init ){
                topLeftIndicator = cntPts[i].x + cntPts[i].y;
                topRightIndicator = cntPts[i].x - cntPts[i].y;
                bottomLeftIndicator = cntPts[i].y - cntPts[i].x;
                bottomRightIndicator = cntPts[i].x + cntPts[i].y;
                init = true;
            }

            if ( (cntPts[i].x + cntPts[i].y) < topLeftIndicator ){
                topLeftIndicator = cntPts[i].x + cntPts[i].y;
                contourTopLeft = cntPts[i];
            }

            if ( cntPts[i].x - cntPts[i].y > topRightIndicator ){
                topRightIndicator = cntPts[i].x - cntPts[i].y;
                contourTopRight = cntPts[i];
            }

            if ( cntPts[i].y - cntPts[i].x > bottomLeftIndicator ){
                bottomLeftIndicator = cntPts[i].y - cntPts[i].x;
                contourBottomLeft = cntPts[i];
            }

            if ( cntPts[i].x + cntPts[i].y > bottomRightIndicator ){
                bottomRightIndicator = cntPts[i].x + cntPts[i].y;
                contourBottomRight = cntPts[i];
            }

        }

        if (contourBottomLeft.y > contourBottomRight.y) {
            double tan = (contourBottomRight.y - contourTopLeft.y)/(contourBottomRight.x-contourTopLeft.x);

            double offsetX = (contourBottomLeft.y - contourBottomRight.y)/tan;
            contourBottomRight.x += offsetX;
            contourBottomRight.y = contourBottomLeft.y;
        }
        else { contourBottomLeft.y = contourBottomRight.y; }

        org.opencv.core.Rect contourBoundingRect = Imgproc.boundingRect(largestAreaContour);
        Point contourBoundingRectTR = new Point(contourBoundingRect.x + contourBoundingRect.width, contourBoundingRect.y);
        Point contourBoundingRectBL = new Point(contourBoundingRect.x, contourBoundingRect.y + contourBoundingRect.height);

        MatOfPoint2f originalPersepective = new MatOfPoint2f(contourTopLeft, contourTopRight, contourBottomLeft, contourBottomRight);
        MatOfPoint2f expectedPerspective = new MatOfPoint2f(contourBoundingRect.tl(),
                                                            contourBoundingRectTR ,
                                                            contourBoundingRectBL,
                                                            contourBoundingRect.br());

        Mat transformRatio = Imgproc.getPerspectiveTransform(originalPersepective, expectedPerspective);

        Mat transformedMat = new Mat(src.size(), src.type());
        Imgproc.warpPerspective(src, transformedMat, transformRatio, src.size());

        displayBitmap(R.id.testImage, getBitmapFromMat(transformedMat));

        return src;
    }


    public Mat getDeskewMat(Mat src){
        Mat borderRemovedMat = getBorderRemovedMat(src);
        Mat gray = new Mat();
        Imgproc.cvtColor(borderRemovedMat, gray, Imgproc.COLOR_RGB2GRAY);

        Mat smooth = new Mat();
        Imgproc.bilateralFilter(gray, smooth, 9,60,60);

        Mat canny = new Mat();
        Imgproc.Canny(smooth, canny, 30, 80);

        Mat closing = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,3));
        Imgproc.morphologyEx(canny, closing, Imgproc.MORPH_CLOSE, kernel, new Point(-1,-1), 3);


        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9,9));

        Mat erode = new Mat();
        Imgproc.morphologyEx(closing, erode, Imgproc.MORPH_ERODE, element, new Point(-1,-1), 1);

        Mat white = new Mat();
        Core.findNonZero(erode, white);

        MatOfPoint mp = new MatOfPoint(white);
        MatOfPoint2f matOfPoint = new MatOfPoint2f(mp.toArray());

        RotatedRect rect = Imgproc.minAreaRect(matOfPoint);
        Point[] points = new Point[4];
        rect.points(points);

        double angle = rect.angle;
        if (angle < -45.){
            angle += 90.;
        }

        Mat rot_mat = Imgproc.getRotationMatrix2D(rect.center, angle, 1);

        Mat rotated = new Mat();
        Imgproc.warpAffine(gray, rotated, rot_mat, borderRemovedMat.size(), Imgproc.INTER_CUBIC);

        Mat cropped = new Mat();

        displayBitmap(R.id.testImage, getBitmapFromMat(rotated));
        return rotated;
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

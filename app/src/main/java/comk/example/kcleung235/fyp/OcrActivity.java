package comk.example.kcleung235.fyp;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;
import android.text.style.UnderlineSpan;
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
        image = BitmapFactory.decodeResource(getResources(), R.drawable.border6, options);

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

        OpenCVLoader.initDebug();

        computeConnectedComponent(approxPolygon(getMatFromBitmap(image)));
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

    public Mat noiseRemovedBinarization(Mat src){
        Mat result = new Mat();
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(gray, result, 64, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        return result;
    }

    public Mat computeConnectedComponent(Mat src){
        Mat result = new Mat();
        Mat threshold = noiseRemovedBinarization(src);
        Mat white = Mat.ones(src.size(), CvType.CV_8UC1);

        Mat label = new Mat();
        Mat stat = new Mat();
        Mat centroid = new Mat();
        int labels = Imgproc.connectedComponentsWithStats(threshold, label, stat, centroid, 8, CvType.CV_16U);

        TextView tv = findViewById(R.id.text);
        white.setTo(new Scalar(255));

        for ( int r = 0 ; r < labels ; r++){
            double x = centroid.get(r, 0)[0];
            double y = centroid.get(r, 1)[0];
            Point centroidPoint = new Point(x, y);
            if( x > 30 && x < src.cols() - 30 ){
                if ( y > 30 && y < src.rows() - 30 ){
                    Imgproc.circle(src, centroidPoint, 5, new Scalar(255,0,0,255), 8);
                }
            }
        }


        displayBitmap(R.id.testImage, getBitmapFromMat(src));
        tv.setText(String.valueOf(centroid.get(0, 0)[0]));

        return result;
    }

    public Mat approxPolygon(Mat src){
        Mat srcClone = src.clone();

        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        Mat blur = new Mat();
        Imgproc.bilateralFilter(gray, blur, 5, 60, 60);

        Mat canny = new Mat();
        Imgproc.Canny(blur, canny, 50, 150, 3, false);

        Mat closing = new Mat();
        Mat closingElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 15));

        Imgproc.morphologyEx(canny, closing, Imgproc.MORPH_DILATE, closingElement);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(closing, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);

        ArrayList<MatOfPoint> approxPolys = new ArrayList<>();

        for ( MatOfPoint contour : contours ) {
            if ( Imgproc.boundingRect(contour).area() > src.size().area() * 0.1) {
                MatOfPoint2f curve = new MatOfPoint2f(contour.toArray());
                MatOfPoint2f polys = new MatOfPoint2f();
                Imgproc.approxPolyDP(curve, polys, Imgproc.arcLength(curve, true) * 0.1, true);
                approxPolys.add(new MatOfPoint(polys.toArray()));
            }
        }

        Mat finalResult = new Mat();

        for ( int id = 0 ; id < approxPolys.size() ; id++){
            if ( approxPolys.get(id).rows() == 4 ) {
                Point[] verticesOfPoly = new Point[4];

                for ( int row = 0 ; row < approxPolys.get(id).rows() ; row++ ){
                    verticesOfPoly[row] = new Point( approxPolys.get(id).get(row, 0) );
                }

                double topWidth = Math.abs( verticesOfPoly[0].x - verticesOfPoly[3].x );
                double bottomWidth = Math.abs( verticesOfPoly[1].x - verticesOfPoly[2].x );
                double meanWidth = (topWidth + bottomWidth) / 2;

                double leftHeight = Math.abs( verticesOfPoly[3].y - verticesOfPoly[2].y );
                double rightHeight = Math.abs( verticesOfPoly[1].y - verticesOfPoly[0].y );
                double meanHeight = (leftHeight + rightHeight) / 2;

                if ( meanWidth > (src.width() * 0.5)  &&  meanHeight > (src.height() * 0.5) ){
                    Imgproc.drawContours(src, approxPolys, id, new Scalar(0, 255, 0, 255), 5); // Drawing contour of the polygon

                    RotatedRect rotatedRect = Imgproc.minAreaRect(new MatOfPoint2f(verticesOfPoly));
                    Point[] rRectVertices = new Point[4];
                    rotatedRect.points(rRectVertices);

                    for ( int k = 0 ; k<4 ; k++ ){
                        Imgproc.line(src, rRectVertices[k], rRectVertices[(k+1) % 4], new Scalar(0, 0, 255, 255), 5);
                    } // Drawing lines

                    double angle = rotatedRect.angle;
                    if (angle < -45.) {
                        angle += 90.;
                    }

                    Mat rotated = new Mat();
                    Mat rot_mat = Imgproc.getRotationMatrix2D(rotatedRect.center, angle, 1);
                    Imgproc.warpAffine(srcClone, rotated, rot_mat, src.size(), Imgproc.INTER_CUBIC, Core.BORDER_DEFAULT, new Scalar(0));

//                    double a = Imgproc.contourArea(approxPolys.get(id));
//                    double p = Imgproc.arcLength(new MatOfPoint2f(approxPolys.get(id).toArray()), true);
//                    double width = (p + Math.sqrt(Math.pow(p,2) - (16 * a)))/4;
//                    double height = (p/2 - width);
//
//                    if (width > height){
//                        double temp = height;
//                        height = width;
//                        width = temp;
//                    } // Cal smaller rect size

                    double[] lengthArray = new double[4];
                    for ( int r = 0; r < rRectVertices.length ; r++){
                        double x1 = rRectVertices[r].x;
                        double y1 = rRectVertices[r].y;
                        double x2 = rRectVertices[(r+1) % rRectVertices.length].x;
                        double y2 = rRectVertices[(r+1) % rRectVertices.length].y;

                        double length = Math.sqrt(Math.pow((x1-x2),2) + Math.pow((y1-y2),2));
                        lengthArray[r] = length;
                    }
                    double max = lengthArray[0];
                    double min = lengthArray[0];
                    for ( int r = 0; r < rRectVertices.length ; r++ ) {
                        if(lengthArray[r] >= max){
                            max = lengthArray[r];
                        }
                        if(lengthArray[r] <= min){
                            min = lengthArray[r];
                        }
                    }

                    List<Mat> channelsOfRotated = new ArrayList<>();
                    Core.split(rotated, channelsOfRotated);

                    ArrayList<Mat> channelsOfResult = new ArrayList<>();

                    for ( int i = 0 ; i < channelsOfRotated.size() ; i++ ){
                        Mat result = new Mat();
                        Imgproc.getRectSubPix(channelsOfRotated.get(i), new Size(min, max), rotatedRect.center, result, channelsOfRotated.get(i).type());
                        channelsOfResult.add(result);
                    }
                    org.opencv.core.Rect boundingRect = Imgproc.boundingRect(approxPolys.get(id));

                    Imgproc.cvtColor(srcClone, srcClone, Imgproc.COLOR_RGB2GRAY);
                    Mat bRect = new Mat();
                    Imgproc.getRectSubPix(srcClone, boundingRect.size(), rotatedRect.center, bRect, srcClone.type());

                    Core.merge(channelsOfResult, finalResult);
                    displayBitmap(R.id.testImage2, getBitmapFromMat(bRect));
                    displayBitmap(R.id.testImage3, getBitmapFromMat(finalResult));

//                    MatOfPoint2f originalAffine = new MatOfPoint2f(verticesOfPoly[3], verticesOfPoly[0], verticesOfPoly[2], verticesOfPoly[1]);
//                    Point tr = new Point(verticesOfPoly[3].x + min -1, verticesOfPoly[3].y);
//                    Point bl = new Point( verticesOfPoly[3].x, verticesOfPoly[3].y + max -1);
//                    Point br = new Point( verticesOfPoly[3].x + min -1, verticesOfPoly[3].y + max -1 );
//                    MatOfPoint2f expectAffine = new MatOfPoint2f(verticesOfPoly[3], tr, bl, br);
//
//                    Mat affine = Imgproc.getPerspectiveTransform(originalAffine, expectAffine);
//                    Mat transformed = new Mat();
//                    Imgproc.warpPerspective(finalResult, transformed, affine, finalResult.size(),Imgproc.INTER_LANCZOS4, Core.BORDER_REPLICATE, new Scalar(0));
//                    displayBitmap(R.id.testImage4, getBitmapFromMat(transformed)); // Perspective transform // Perspective Transform
                }
            }
        }

        for ( int index = 0 ; index < contours.size() ; index++ ){
            if ( Imgproc.minAreaRect(new MatOfPoint2f(contours.get(index).toArray())).size.area() > src.size().area() * 0.1 ){
                Imgproc.drawContours(src, contours, index, new Scalar(255, 0 , 0, 255),2);
                double[] childIndex =  hierarchy.get(0, index);
                if ( childIndex[2] != -1 ){
                    Imgproc.drawContours(src, contours, (int)childIndex[2], new Scalar(0, 0 , 255, 255),2);
                }
            }
        } // drawing the contours

        return finalResult;
    }

    public void runOCR(Mat src){
        Mat smoothed = new Mat();
        Imgproc.GaussianBlur(src, smoothed, new Size(5, 5), 0);
        Mat thres = new Mat();
        Imgproc.threshold(src, thres, 64, 255, Imgproc.THRESH_OTSU);

        displayBitmap(R.id.testImage2, getBitmapFromMat(src));

        mTess.setImage(getBitmapFromMat(src));
        mTess.getUTF8Text();;

        ResultIterator resultIterator = mTess.getResultIterator();

        SpannableString whiteSpace = new SpannableString(" ");

        SpannableStringBuilder spannableParagraph = new SpannableStringBuilder();
        int count = 0;
        while (resultIterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD)){
            String wordString = resultIterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            SpannableString spannableWordString = new SpannableString(wordString);
            Rect wordBoundingRect = resultIterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            org.opencv.core.Rect wordOpencvRect = new org.opencv.core.Rect(new Point(wordBoundingRect.left, wordBoundingRect.top),
                                                                           new Point(wordBoundingRect.right, wordBoundingRect.bottom));

//            if( detectBoldStyle(src, wordBoundingRect) == true ){
//                spannableWordString.setSpan(new StyleSpan(Typeface.BOLD), 0, wordString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//            }
//
//            if( detectItalicStyle(src, wordBoundingRect) == true ){
//                spannableWordString.setSpan(new StyleSpan(Typeface.ITALIC), 0, wordString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//            }

//            int minLengthOfLine = (int)(wordOpencvRect.width * 0.8);
//            if( foundUnderLine(src.submat(wordOpencvRect), minLengthOfLine) ){
//                spannableWordString.setSpan(new UnderlineSpan(), 0, wordString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                Imgproc.rectangle(src, wordOpencvRect.tl(), wordOpencvRect.br(), new Scalar(255, 0, 0, 255), 5);
//            }

            if (count == 0 ){
                spannableWordString.setSpan(new LeadingMarginSpan.Standard(50, 0), 0, wordString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            Imgproc.rectangle(src, wordOpencvRect.tl(), wordOpencvRect.br(), new Scalar(0,0,255,255), 5);
            spannableParagraph.append(spannableWordString);
            spannableParagraph.append(whiteSpace);
            count++;
        }


        displayBitmap(R.id.testImage, getBitmapFromMat(src));

        TextView tv = findViewById(R.id.text);
        tv.setText(spannableParagraph);
    }

    public boolean foundUnderLine(Mat src, int minLengthOfLine){
        Boolean lineExist = true;
        Mat gray = src.clone();
        if (src.channels() > 1){
            Imgproc.cvtColor(gray, gray, Imgproc.COLOR_RGB2GRAY);
        }

        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 80 ,150);

        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI/180, 50, minLengthOfLine, 3);

        if ( lines.empty() != true ){
            for(int i = 0; i < lines.rows(); i++) {
                for (int j = 0; j < lines.cols(); j++) {
                    double[] lineCoordinates = lines.get(i, j);
                    double lineStartPointXCoodinate = lineCoordinates[0],
                            lineStartPointyCoodinate = lineCoordinates[1],
                            lineEndPointXCoodinate = lineCoordinates[2],
                            lineEndPointyCoodinate = lineCoordinates[3];

                    Point lineStartingPoint = new Point(lineStartPointXCoodinate, lineStartPointyCoodinate);
                    Point lineEndingPoint = new Point(lineEndPointXCoodinate, lineEndPointyCoodinate);
                    Imgproc.line(gray, lineStartingPoint, lineEndingPoint, new Scalar(0,255,255,255), 10);
                }
            }
            return lineExist;
        } else {
            lineExist = false;
        }

        return lineExist;
    }

    private ArrayList<RotatedRect> extractWordsBoundingRect(Mat src){
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        Mat b_blur = new Mat();
        Imgproc.bilateralFilter(gray, b_blur, 5, 30,30);

        Mat canny = new Mat();
        Imgproc.Canny(b_blur, canny, 30, 80);

        Mat closing = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7,3));
        Imgproc.morphologyEx(canny, closing, Imgproc.MORPH_CLOSE, kernel, new Point(-1,-1), 1);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(closing, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        ArrayList<RotatedRect> rectList = new ArrayList<>();

        for ( MatOfPoint contour : contours ){
            MatOfPoint2f contourPt2f = new MatOfPoint2f(contour.toArray());
            RotatedRect rect = Imgproc.minAreaRect(contourPt2f);
            rectList.add(rect);
        }
        for ( int i = 0 ; i < rectList.size() ; i++) {
            Point[] vertice = new Point[4];
            rectList.get(i).points(vertice);
            Imgproc.rectangle(src, vertice[1], vertice[3], new Scalar(255,0,0,255),5);
        }
        displayBitmap(R.id.testImage, getBitmapFromMat(src));

        return rectList;
    }

    public boolean checkBold(Mat imageMat, Rect roi){
        Boolean isBold = true;

        Mat src_mat = imageMat;
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
        return isBold;
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
        Imgproc.bilateralFilter(gray, b_blur, 9, 60,60);

        Mat canny = new Mat();
        Imgproc.Canny(b_blur, canny, 30, 80);

        Mat closing = new Mat();
        Mat closingKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,3));
        Imgproc.morphologyEx(canny, closing, Imgproc.MORPH_CLOSE, closingKernel, new Point(-1,-1), 1);

        Mat erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));
        Mat erode = new Mat();
        Imgproc.morphologyEx(closing, erode, Imgproc.MORPH_ERODE, erodeKernel, new Point(-1,-1), 1);

//        Mat erode = new Mat();
//        Mat erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,1));
//        Imgproc.erode(canny, erode, erodeKernel);

        Mat dilate = new Mat();
//        Mat aggressiveDilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25,2));
//        Imgproc.dilate(erode, dilate, aggressiveDilateKernel);
//
//        Imgproc.medianBlur(dilate, dilate, 5);
//
//        Mat erodeKernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1,7));
//        Imgproc.erode(dilate, dilate, erodeKernel2);
//
        Mat aggressiveDilateKernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25,80));
        Imgproc.dilate(erode, dilate, aggressiveDilateKernel2);
//
//        Mat erodeKernel_2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25,15));
//        Imgproc.erode(dilate, dilate, erodeKernel_2);


//
//        displayBitmap(R.id.testImage, getBitmapFromMat(erode));
        ArrayList<MatOfPoint> contour_list = new ArrayList<>();
        Imgproc.findContours(dilate, contour_list, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE );

        MatOfPoint textBlockContour = getLargestContourMat(contour_list);

        org.opencv.core.Rect rect = Imgproc.boundingRect(textBlockContour);

        Mat result = src.submat(rect);
//
//        displayBitmap(R.id.testImage4, getBitmapFromMat(result));
        return result;
    } // may be deprecated

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
    } // may be deprecated

    public Mat computeLargestBlob2(Mat src){
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

        Mat element3 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9,18));
        Imgproc.erode(result, result, element3, new Point(-1,-1),2);
        return result;
    }

    public Mat getStraightenedMat(Mat src){
        Mat mat = getDeskewMat(src);

        Mat textBlock = computeLargestBlob(mat);

        displayBitmap(R.id.testImage, getBitmapFromMat(textBlock));

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

            Imgproc.line(mat, cntPts[i], cntPts[j], new Scalar(255,255,0,255),5);


            Imgproc.circle(mat,  cntPts[i], 35, new Scalar(0,0,255,255),15 );

            if ( !init ){
                topLeftIndicator = cntPts[i].x + cntPts[i].y;
                topRightIndicator = cntPts[i].x - cntPts[i].y;
                bottomLeftIndicator = cntPts[i].y - cntPts[i].x;
                bottomRightIndicator = cntPts[i].x + cntPts[i].y;
                init = true;
            }

            if ( (cntPts[i].x + cntPts[i].y) <= topLeftIndicator ){
                topLeftIndicator = cntPts[i].x + cntPts[i].y;
                contourTopLeft = cntPts[i];
            }

            if ( cntPts[i].x - cntPts[i].y >= topRightIndicator ){
                topRightIndicator = cntPts[i].x - cntPts[i].y;
                contourTopRight = cntPts[i];
            }

            if ( cntPts[i].y - cntPts[i].x >= bottomLeftIndicator ){
                bottomLeftIndicator = cntPts[i].y - cntPts[i].x;
                contourBottomLeft = cntPts[i];
            }

            if ( cntPts[i].x + cntPts[i].y >= bottomRightIndicator ){
                bottomRightIndicator = cntPts[i].x + cntPts[i].y;
                contourBottomRight = cntPts[i];
            }

        }

//        if (contourBottomLeft.y > contourBottomRight.y) {
//            double tan = (contourBottomRight.x-contourTopLeft.x)/(contourBottomRight.y - contourTopLeft.y);
//
//            double offsetX = (contourBottomLeft.y - contourBottomRight.y) * tan;
//
//            contourBottomRight.x += offsetX;
//
//            contourBottomRight.y = contourBottomLeft.y;
//        }
//        else { contourBottomLeft.y = contourBottomRight.y; }

        org.opencv.core.Rect contourBoundingRect = Imgproc.boundingRect(largestAreaContour);

        Imgproc.circle(mat,  contourTopLeft, 15, new Scalar(0,255,0,255),15 );
        Imgproc.circle(mat,  contourTopRight, 15, new Scalar(0,255,0,255) ,15);
        Imgproc.circle(mat,  contourBottomLeft, 15, new Scalar(0,255,0,255) ,15);
        Imgproc.circle(mat,  contourBottomRight, 15, new Scalar(0,255,0,255) ,15);


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


        displayBitmap(R.id.testImage2, getBitmapFromMat(mat));

        return src;
    } // perspective transfrom

    public Mat getDeskewMat(Mat src){
        Mat borderRemovedMat = getBorderRemovedMat(src);
        Mat gray = new Mat();
        Imgproc.cvtColor(borderRemovedMat, gray, Imgproc.COLOR_RGB2GRAY);

        Mat smooth = new Mat();
        Imgproc.bilateralFilter(gray, smooth, 9,60,60);

        Mat canny = new Mat();
        Imgproc.Canny(smooth, canny, 30, 80);

        Mat closing = new Mat();
        Mat closingKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,3));
        Imgproc.morphologyEx(canny, closing, Imgproc.MORPH_CLOSE, closingKernel, new Point(-1,-1), 1);


        Mat erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));

        Mat erode = new Mat();
        Imgproc.morphologyEx(closing, erode, Imgproc.MORPH_ERODE, erodeKernel, new Point(-1,-1), 1);
//
//        displayBitmap(R.id.testImage3, getBitmapFromMat(erode));

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
        Imgproc.warpAffine(borderRemovedMat, rotated, rot_mat, borderRemovedMat.size(), Imgproc.INTER_CUBIC, Core.BORDER_WRAP, new Scalar(0));

        String test = "12345asd";
        SpannableString spannableString = new SpannableString(test);
        spannableString.setSpan(new LeadingMarginSpan.Standard(150, 0), 0, test.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString newSpannableString = new SpannableString("ssssssasjkbdkbkwbqdbksabckjasbckasjbckasbckjsabkcjabsckjasbkcjbaskjcbaskjcbaskjcbkasjbckjasbckasjbckjasbckjasbckjsabckjabckabskcjbaskjcbaskjcbkascbaksjbckjasbcksajbcjbacksj");

        SpannableStringBuilder sb = new SpannableStringBuilder();

        sb.append(spannableString);
        sb.append(newSpannableString);

        TextView tv = findViewById(R.id.text);
        tv.setText(sb);

        return rotated;
    } // deprecated deskew function

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

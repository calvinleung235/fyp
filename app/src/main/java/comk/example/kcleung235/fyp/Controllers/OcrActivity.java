package comk.example.kcleung235.fyp.Controllers;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;
import android.text.style.UnderlineSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
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

import comk.example.kcleung235.fyp.Models.DisplayTextProcessor;
import comk.example.kcleung235.fyp.Models.OcrString;
import comk.example.kcleung235.fyp.R;

public class OcrActivity extends AppCompatActivity{

    ImageView imageView;
    Button button;
    ProgressBar progressBar;

    Bitmap image;
    private TessBaseAPI mTess;
    String datapath = "";

    DisplayTextProcessor displayTextProcessor = new DisplayTextProcessor();
    SpannableStringBuilder titleText = new SpannableStringBuilder(" ");
    SpannableStringBuilder bodyText = new SpannableStringBuilder(" ");
    SpannableStringBuilder pageNumText = new SpannableStringBuilder(" ");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);
        button = findViewById(R.id.button);
        imageView = findViewById(R.id.testImage);
        progressBar = findViewById(R.id.progressBar);

        new BackgroundTask().execute();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OcrString ocrString = new OcrString(titleText, bodyText, pageNumText);
                Intent intent = new Intent(OcrActivity.this, ResultActivity.class);
                intent.putExtra("ocrString", ocrString);
                startActivity(intent);
            }
        });

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    class BackgroundTask extends AsyncTask<Void, Integer, Bitmap>{

        @Override
        protected Bitmap doInBackground(Void... params) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            image = BitmapFactory.decodeResource(getResources(), R.drawable.content, options);
            Bitmap cameraCapturedImage = BitmapFactory.decodeResource(getResources(), R.drawable.border7, options);
            publishProgress(0, 5);

            String language = "eng";
            datapath = getFilesDir()+ "/tesseract/";
            mTess = new TessBaseAPI();
            checkFile(new File(datapath + "tessdata/"));
            mTess.init(datapath, language);
            mTess.setVariable("language_model_penalty_non_dict_word", "1.0");
            mTess.setVariable("language_model_penalty_non_freq_dict_word", "1.0");
            OpenCVLoader.initDebug();
            publishProgress(5, 10);

            Mat para = computeTextBlockDeskew(approxPagePolygon(getMatFromBitmap(cameraCapturedImage)));
            Mat content = computeTextBlockDeskew(approxPagePolygon(getMatFromBitmap(image)));
            publishProgress(10, 40);

            ArrayList<SpannableString> contentString = runContentPageOCR(content, TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE);
            ArrayList<SpannableString> paragraphString = runOCR(para, TessBaseAPI.PageIteratorLevel.RIL_WORD);
            publishProgress(40, 70);

            titleText = displayTextProcessor.getTitleText(contentString, paragraphString);
            bodyText = displayTextProcessor.getBodyText(contentString, paragraphString);
            pageNumText = displayTextProcessor.getPageNumText(contentString, paragraphString);

            publishProgress(70, 100);
            return getBitmapFromMat(para);
        }

        @Override
        protected void onProgressUpdate(Integer... done) {
            for (int i = done[0]; i < done[1]; i++){
                try {
                    Thread.sleep(100);
                    progressBar.setProgress(i, true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (done[1] == 100){
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onPostExecute(Bitmap paraImage) {
            imageView.setImageBitmap(paraImage);
        }
    }


    public ArrayList<SpannableString> runOCR(Mat src, int pageIterateLevel){
        Mat smoothed = new Mat();
        Imgproc.GaussianBlur(src, smoothed, new Size(5, 5), 0,0);

        Mat gray = new Mat(src.size(), CvType.CV_32FC1);
        Imgproc.cvtColor(smoothed, gray, Imgproc.COLOR_RGB2GRAY);

        Mat thres = new Mat(gray.size(), CvType.CV_32FC1);
        Imgproc.threshold(gray, thres, 128, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        mTess.setImage(getBitmapFromMat(thres));
        mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        mTess.getUTF8Text();

        ArrayList<SpannableString> spannableParagraph = new ArrayList<>();
        ResultIterator resultIterator = mTess.getResultIterator();

        resultIterator.begin();
        Rect previousTextlineBoundingRect = null;
        double meanLeftIndent = 0;
        int count = 0;
        while ( resultIterator.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE) ) {
            Rect textlineRect = resultIterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE);
            if ((textlineRect.height() * textlineRect.width()) < 10 * 10) continue;

            if (previousTextlineBoundingRect != null){
                meanLeftIndent += textlineRect.left;
                count++;
            }
            previousTextlineBoundingRect = textlineRect;
        }

        meanLeftIndent = meanLeftIndent/count;

        resultIterator.begin();
        Rect previousWordBoundingRect = null;
        while (resultIterator.next(pageIterateLevel)){

            Rect wordRect = resultIterator.getBoundingRect(pageIterateLevel);
            if ((wordRect.height() * wordRect.width()) < 10 * 10){
                continue;
            }

            Point tl =new Point(wordRect.left, wordRect.top);
            Point br =new Point(wordRect.right, wordRect.bottom);
            Imgproc.rectangle(src, tl, br , new Scalar(0,0,255,255),5);

            String wordString = resultIterator.getUTF8Text(pageIterateLevel);
            SpannableString spannableString = new SpannableString(wordString);
            if (previousWordBoundingRect != null){
                if (previousWordBoundingRect.bottom < wordRect.top && wordRect.left > meanLeftIndent && wordRect.left < previousWordBoundingRect.right){
                    spannableString = new SpannableString("\n" + wordString);
                    spannableString.setSpan(new LeadingMarginSpan.Standard(80, 0), 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            org.opencv.core.Rect wordOpencvRect = new org.opencv.core.Rect(new Point(wordRect.left, wordRect.top), new Point(wordRect.right, wordRect.bottom));
            if( foundUnderLine(thres, wordOpencvRect) ){
                spannableString.setSpan(new UnderlineSpan(), 0, wordString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            spannableParagraph.add(spannableString);
            previousWordBoundingRect = wordRect;
        }
        return spannableParagraph;
    }

    public ArrayList<SpannableString> runContentPageOCR(Mat src, int pageIterateLevel){
        Mat smoothed = new Mat();
        Imgproc.GaussianBlur(src, smoothed, new Size(5, 5), 0,0);

        Mat gray = new Mat(src.size(), CvType.CV_32FC1);
        Imgproc.cvtColor(smoothed, gray, Imgproc.COLOR_RGB2GRAY);

        Mat thres = new Mat(gray.size(), CvType.CV_32FC1);
        Imgproc.threshold(gray, thres, 128, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        mTess.setImage(getBitmapFromMat(thres));
        mTess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        mTess.getUTF8Text();

        ArrayList<SpannableString> spannableParagraph = new ArrayList<>();
        ResultIterator resultIterator = mTess.getResultIterator();

        resultIterator.begin();
        while (resultIterator.next(pageIterateLevel)){
            Rect wordRect = resultIterator.getBoundingRect(pageIterateLevel);
            if ((wordRect.height() * wordRect.width()) < 10 * 10){
                continue;
            }
            String wordString = resultIterator.getUTF8Text(pageIterateLevel);
            SpannableString spannableString = new SpannableString(wordString);
            spannableParagraph.add(spannableString);
        }
        return spannableParagraph;
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

    public Mat approxPagePolygon(Mat src){
        Mat srcClone = src.clone();

        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        Mat blur = new Mat();
        Imgproc.bilateralFilter(gray, blur, 5, 80, 80);

        Mat canny = new Mat();
        Imgproc.Canny(blur, canny, 25, 50, 3, false);

        Mat closing = new Mat();
        Mat closingElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 15));
        Imgproc.morphologyEx(canny, closing, Imgproc.MORPH_DILATE, closingElement);

        ArrayList<MatOfPoint> contours = getContoursForApproxPolys(closing);
        for ( MatOfPoint contour : contours ) {
            if ( Imgproc.boundingRect(contour).area() > src.size().area() * 0.2) {
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                MatOfPoint2f polys = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour2f, polys, Imgproc.arcLength(contour2f, true) * 0.1, true);

                if ( polys.rows() == 4 ) {
                    RotatedRect rotatedRect = Imgproc.minAreaRect(contour2f);

                    Mat transformed = computePerspectiveTransform(srcClone, polys, rotatedRect, false);
                    Mat rotatedImage = computeRotation(transformed, rotatedRect);
                    cropImage(rotatedImage, rotatedRect, polys, false);
                    return rotatedImage;
                }
            }
        }
        return src;
    }

    private Mat computePerspectiveTransform(Mat src, MatOfPoint2f polys, RotatedRect respectiveRectShape, Boolean adjustRequired){
        Point[] verticesOfPoly = polys.toArray();
        Point[] respectivePoints = new Point[4];
        respectiveRectShape.points(respectivePoints);

        sortVertices(respectivePoints);
        sortVertices(verticesOfPoly);

        if( adjustRequired ) adjustCoordinates(verticesOfPoly);

        Mat transformed = new Mat();
        MatOfPoint2f originalPerspective = new MatOfPoint2f(verticesOfPoly[0], verticesOfPoly[1], verticesOfPoly[2], verticesOfPoly[3]);
        MatOfPoint2f expectedPerspective = new MatOfPoint2f(respectivePoints[0], respectivePoints[1], respectivePoints[2], respectivePoints[3]);

        Mat perspectiveTransformMat = Imgproc.getPerspectiveTransform(originalPerspective, expectedPerspective);
        Imgproc.warpPerspective(src, transformed, perspectiveTransformMat, src.size(),Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE, new Scalar(0));
        return transformed;
    }

    private double findMax( double[] arr) {
        double max = 0;

        for ( double i : arr){
            if (i >= max){
                max = i;
            }
        }
        return max;
    }

    private ArrayList<double[]> getXYDifferenceList(Point[] verticesOfPoly, Point[] respectivePoints) {
        double[] yD = new double[4];
        double[] xD = new double[4];

        ArrayList<double[]> differenceList = new ArrayList<>();

        for (int i = 0; i < 4 ;i++){
            yD[i] = Math.abs(verticesOfPoly[i].y - respectivePoints[i].y);
            xD[i] = Math.abs(verticesOfPoly[i].x - respectivePoints[i].x);
        }

        differenceList.add(xD);
        differenceList.add(yD);
        return differenceList;
    }

    private Mat computeRotation(Mat src, RotatedRect rotatedRect ){
        Mat rotated = new Mat();
        double angle = getRotationAngle(rotatedRect);
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(rotatedRect.center, angle, 1);
        Imgproc.warpAffine(src, rotated, rotationMatrix, src.size(), Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE, new Scalar(0));

        return rotated;
    }

    private void setRotatedRectProperSize (RotatedRect rotatedRect){
        if (rotatedRect.angle < -45.){
            double temp = rotatedRect.size.height;
            rotatedRect.size.height = rotatedRect.size.width;
            rotatedRect.size.width = temp;
        }
    }

    private void cropImage (Mat src, RotatedRect rotatedRect, MatOfPoint2f cropOject, boolean extendBord) {
        setRotatedRectProperSize(rotatedRect);

        Point[] verticesOfPoly = cropOject.toArray();
        Point[] respectivePoints = new Point[4];
        rotatedRect.points(respectivePoints);

        sortVertices(respectivePoints);
        sortVertices(verticesOfPoly);

        Size cropArea = new Size(rotatedRect.size.width, rotatedRect.size.height);

        if (extendBord){
            ArrayList<double[]> xyDifferenceList = getXYDifferenceList(verticesOfPoly, respectivePoints);
            double xMaxDiff = findMax(xyDifferenceList.get(0));
            double yMaxDiff = findMax(xyDifferenceList.get(1));
            cropArea.width += xMaxDiff;
            cropArea.height += yMaxDiff;
        }

        List<Mat> channelsOfTransformed = new ArrayList<>();
        Core.split(src, channelsOfTransformed);
        ArrayList<Mat> channelsOfResult = new ArrayList<>();
        for ( int i = 0 ; i < channelsOfTransformed.size() ; i++ ){
            Mat result = new Mat();
            Imgproc.getRectSubPix(channelsOfTransformed.get(i), cropArea, rotatedRect.center, result, channelsOfTransformed.get(i).type());
            channelsOfResult.add(result);
        }
        Core.merge(channelsOfResult, src);
    }

    @NonNull
    private ArrayList<MatOfPoint> getContoursForApproxPolys(Mat closing) {
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(closing, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
        return contours;
    }

    private double getRotationAngle(RotatedRect rotatedRect) {
        double angle = rotatedRect.angle;
        if (angle < -45.) {
            angle += 90.;
        }
        return angle;
    }
    
    private MatOfPoint getLargestContourMat(List<MatOfPoint> contour_list){
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

    private Mat computerTextBlockArea(Mat src){
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        Mat blur = new Mat();
        Imgproc.bilateralFilter(gray, blur, 9, 80, 80);

        Mat canny = new Mat();
        Imgproc.Canny(blur, canny, 50, 150, 3, false);

        Mat dilate = new Mat();
        Mat aggressiveDilationKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25,80));
        Imgproc.dilate(canny, dilate, aggressiveDilationKernel);

        return dilate;
    }

    private Mat computeTextBlockDeskew(Mat src){
        Mat textBlockArea = computerTextBlockArea(src);

        ArrayList<MatOfPoint> contour_list = new ArrayList<>();
        Imgproc.findContours(textBlockArea, contour_list, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );
        MatOfPoint largestContour = getLargestContourMat(contour_list);

        MatOfPoint2f poly = new MatOfPoint2f();
        MatOfPoint2f curve = new MatOfPoint2f(largestContour.toArray());
        Imgproc.approxPolyDP(curve, poly, Imgproc.arcLength(curve, true) * 0.25, true);

        if (poly.rows() == 4) {
            RotatedRect rotatedRect = Imgproc.minAreaRect(curve);
            Mat rt = computePerspectiveTransform(src, poly, rotatedRect, true);
            Mat tr = computeRotation(rt, rotatedRect);
            cropImage(tr, rotatedRect, poly, true);
            return tr;
        }

        return src.submat(Imgproc.boundingRect(largestContour));
    }

    private void sortVertices(Point[] vertices){
        quickSort(vertices, 0, vertices.length-1);

        if (vertices[0].x > vertices[1].x){
            Point temp = vertices[0];
            vertices[0] = vertices[1];
            vertices[1] = temp;
        }

        if (vertices[2].x > vertices[3].x){
            Point temp = vertices[2];
            vertices[2] = vertices[3];
            vertices[3] = temp;
        }
    }

    private void adjustCoordinates(Point[] verticesOfPoly){
        if (verticesOfPoly[0].y > verticesOfPoly[1].y) {
            double tan = (verticesOfPoly[1].x-verticesOfPoly[3].x)/(verticesOfPoly[1].y - verticesOfPoly[3].y);
            double offsetX = (verticesOfPoly[0].y - verticesOfPoly[1].y) * tan;
            verticesOfPoly[1].x += offsetX;
            verticesOfPoly[1].y = verticesOfPoly[0].y;
        }
        else {
            double tan = (verticesOfPoly[0].x-verticesOfPoly[2].x)/(verticesOfPoly[0].y - verticesOfPoly[2].y);
            double offsetX = (verticesOfPoly[1].y - verticesOfPoly[0].y) * tan;
            verticesOfPoly[0].x += offsetX;
            verticesOfPoly[0].y = verticesOfPoly[1].y;
        }

        if (verticesOfPoly[2].y > verticesOfPoly[3].y) {
            double tan = (verticesOfPoly[3].x-verticesOfPoly[1].x)/(verticesOfPoly[3].y - verticesOfPoly[1].y);
            double offsetX = (verticesOfPoly[2].y - verticesOfPoly[3].y) * tan;
            verticesOfPoly[3].x += offsetX;
            verticesOfPoly[3].y = verticesOfPoly[2].y;
        }
        else {
            double tan = (verticesOfPoly[2].x-verticesOfPoly[0].x)/(verticesOfPoly[2].y - verticesOfPoly[0].y);
            double offsetX = (verticesOfPoly[3].y - verticesOfPoly[2].y) * tan;
            verticesOfPoly[2].x += offsetX;
            verticesOfPoly[2].y = verticesOfPoly[3].y;
        }
    }

    public boolean foundUnderLine(Mat src, org.opencv.core.Rect wordOpencvRect){
        Mat gray = src.submat(wordOpencvRect).clone();
        if (src.channels() > 1){
            Imgproc.cvtColor(gray, gray, Imgproc.COLOR_RGB2GRAY);
        }

        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 80 ,150, 3, false);

        Mat lines = new Mat();
        int minLengthOfLine = (int)(wordOpencvRect.width * 0.8);
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI/180, 50, minLengthOfLine, 3);

        if ( !lines.empty() ){
            return true;
        } else {
            return false;
        }
    }

    private void quickSort(Point arr[], int low, int high) {
        if (low < high)
        {
        /* pi is partitioning index, arr[p] is now
           at right place */
            int pi = partition(arr, low, high);

            quickSort(arr, low, pi - 1);  // Before pi
            quickSort(arr, pi + 1, high); // After pi
        }
    }

    private int partition (Point arr[], int low, int high) {
        // pivot (Element to be placed at right position)
        double pivot = arr[high].y;

        int i = (low - 1);  // Index of smaller element

        for (int j = low; j <= high- 1; j++)
        {
            // If current element is smaller than or
            // equal to pivot
            if (arr[j].y <= pivot)
            {
                i++;    // increment index of smaller element
                Point temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        Point temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        return (i + 1);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home){
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

}

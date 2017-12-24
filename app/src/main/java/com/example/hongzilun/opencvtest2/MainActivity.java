package com.example.hongzilun.opencvtest2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.sql.Time;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
public class MainActivity extends AppCompatActivity {
    public static final String TAG="MainActivity";
    JavaCameraView javaCameraView;

    Button cameraChange, recordHand,resetHand ;
    Mat mRgba,mGray,imgCanny,imgHSV,mRgbaF,mRgbaT;

    boolean fingerState=false;//finger horizon or vertical ,1 mean horizon
    double fingerWidth=-1;
    boolean  isFont,isClicking;
    boolean hasfindHand=false,hasfindFinger=false;
    double iThreshold = 0;
    Point lastPoint=null,recordPoint=null;
    List<Point> historyPoint=null;
    List<Point> tryPoint=null;
    private Scalar               	mBlobColorHsv;
    private Scalar               	mBlobColorRgba;
    private ColorBlobDetector    	 detectHand,detectFinger;
    private Mat                  	mSpectrum;

    private boolean isTrackinghand = false,isTrackingfinger=false,toRecordhand=false,hasRecordHand=false,toGETdata=false;
    Rect myROI;
    Point startPoint=null;
    int clickTimes=0;
    long startTime=System.currentTimeMillis();
    long clickStartTime;
    private Size                 	SPECTRUM_SIZE;

    private Scalar               	CONTOUR_COLOR_WHITE;


    ColorBlobDetector initialDetect;
    boolean hasInit=false,isHasInit1=false;
    Mat lastRgb=null;
    Point currentPoint=null;
    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:{
                    javaCameraView.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }
            super.onManagerConnected(status);
        }
    };
    double bigdiff=0,countdifftimes=0;
    boolean hasbigdiff=false;
    private  SensorManager sensorManager;
    private  Sensor sensor;
    public  TextView sensorText,clickText;
    boolean sensorInil=false;
    boolean isFirst=true;
    private double  peakz=0;
    float   lasty=0,lastx,lastz;

    Point finger=null;
    String textStr="";
    float [] gravSensorVals=null;
    private boolean hasTrack=false;
    private boolean hasClick=false;
    static {


    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensorText=(TextView)findViewById(R.id.text1);
        clickText =(TextView)findViewById(R.id.text2);
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);


        sensor=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(sensor!=null){
            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    gravSensorVals = lowPass(sensorEvent.values.clone(), gravSensorVals);

                    if(toGETdata){

                        float resultx=Math.abs(sensorEvent.values[0]-gravSensorVals[0]) ;
                        float resultz=Math.abs(sensorEvent.values[2]-gravSensorVals[2]) ;
                        float resulty=Math.abs(sensorEvent.values[1]-gravSensorVals[1]) ;
                        if(hasClick){
                            //
                        }
                        else if(resultz<1.5&&resultz>0.75&&!hasClick){//0.088
                            /*
                            if(currentPoint!=null){
                                currentPoint.x+=50;
                                historyPoint.add(currentPoint);
                            }
                            int x=Math.abs((int)currentPoint.x-(int)myROI.tl().x);
                            int y=Math.abs((int)currentPoint.y-(int)myROI.tl().y);
                            int subwidth=(int)myROI.width/8;
                            int subheight=(int)myROI.height/3;
                            x=x/subwidth;
                            y=y/subheight;
                            // 0 3 first place  2,1
                            Log.d(TAG,"click position ("+x+","+y+","+(int)fingerWidth+")");
                            sensorText.setText("click position ("+x+","+y+","+(int)fingerWidth+")");
                            //clickTimes++;
                            Log.d(TAG,"flag1 "+clickTimes+" "+resultz);
                            */
                            hasClick=true;
                            clickStartTime=System.currentTimeMillis();
                            peakz=resultz;
                        }
                        //Log.d(TAG,"flag "+resultx+" "+resulty+" "+resultz);

                    }
                    sensorText.setText(textStr);
                    clickText.setText("click times :"+clickTimes);
                    //clickText.setText(textStr);


                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }

                public  float[] lowPass(float[] input,float[] output){
                    if(output == null) return  input;
                    float ALPHA=0.25f;
                    for ( int i=0; i<input.length; i++ ) {
                        output[i] = output[i] + ALPHA * (input[i] - output[i]);
                    }
                    return output;

                }
            },sensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
        isClicking=false;
        isFont=true;
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2(){
            @Override
            public void onCameraViewStarted(int width, int height) {

                mRgba = new Mat(height,width, CvType.CV_8UC4);
                mRgbaF = new Mat (height,width,CvType.CV_8UC4);
                mRgbaT = new Mat (height,width,CvType.CV_8UC4);
                mGray = new Mat(height,width, CvType.CV_8UC1);
                imgCanny = new Mat(height,width, CvType.CV_8UC1);
                imgHSV = new Mat (height,width,CvType.CV_8UC3);
                //mDetector = new ColorBlobDetector();
                detectFinger = new ColorBlobDetector();
                detectHand = new ColorBlobDetector();
                mSpectrum = new Mat();
                mBlobColorRgba = new Scalar(255);
                mBlobColorHsv = new Scalar(255);
                SPECTRUM_SIZE = new Size(200, 64);
                //CONTOUR_COLOR = new Scalar(255,0,0,255);
                CONTOUR_COLOR_WHITE = new Scalar(255,255,255,255);
                //hsvOffinger = new Scalar(255);
                //hsvOfhand = new Scalar(255);
                detectHand.setColorRadius(new Scalar(5,5,5));
                detectFinger.setColorRadius(new Scalar(15,15,90));

                historyPoint = new LinkedList<Point>();
                tryPoint = new LinkedList<Point>();
                //detectFinger.setHsvColor(new Scalar(6.0,90.0,165.0));
                //detectHand.setHsvColor(new Scalar(14.0,67.0,177.0));



            }

            @Override
            public void onCameraViewStopped() {
                mRgba.release();
                mRgbaF.release();
                mRgbaT.release();
                mGray.release();
                imgCanny.release();
                imgHSV.release();
                lastRgb.release();
                mSpectrum.release();
                hasInit=false;
                isHasInit1=false;
                lastRgb=null;

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                if(!javaCameraView.isEnabled()){
                    return null;
                }
                if(inputFrame==null){
                    return null;
                }

                mRgba = inputFrame.rgba();


                Log.d(TAG,"row:"+mRgba.rows()+"col:"+mRgba.cols());

                if(!isFont){
                    Core.transpose(mRgba, mRgbaT);
                    Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0.0D, 0.0D, 0);
                    Core.flip(mRgbaF, mRgba,1);

                }
                else{
                    Core.transpose(mRgba, mRgbaT);
                    Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0.0D, 0.0D, 0);
                    Core.flip(mRgbaF, mRgba,-1);
                }
                for(Point p : tryPoint){
                    Core.circle(mRgba,p,30,new Scalar(188,0,0));
                }
                if(isHasInit1){
                    myprocess3();

                    if(recordPoint!=null)
                        Core.circle(mRgba,recordPoint,30, new Scalar(255,255,255),Core.FILLED);
                    return mRgba;
                }
                else if(hasInit) {
                    isHasInit1 = true;
                    detectFinger.setHsvColor(new Scalar(240, 100, 80));
                    detectFinger.setColorRadius(new Scalar(15, 20, 60));
                    return mRgba;
                }
                else{
                    getInitHsv();
                    return myprocess2(mRgba);
                }



            }

        });
        javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);




        cameraChange = (Button) findViewById(R.id.button);
        cameraChange.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(isFont){
                    isFont=false;
                    if(javaCameraView!=null)
                        javaCameraView.disableView();
                    javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                    if(javaCameraView!=null)
                        javaCameraView.enableView();
                    Log.d(TAG,"change to back");
                }
                else{
                    isFont=true;
                    if(javaCameraView!=null)
                        javaCameraView.disableView();
                    javaCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                    if(javaCameraView!=null)
                        javaCameraView.enableView();
                    Log.d(TAG,"change to font");


                }
            }
        });


        recordHand = (Button) findViewById(R.id.button4);
        recordHand.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                toRecordhand=!toRecordhand;
                toGETdata=!toGETdata;
            }
        });

        resetHand = (Button) findViewById(R.id.button5);
        resetHand.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                hasRecordHand=false;
                //getInitHsv();
                hasInit=false;
                isHasInit1=false;
                bigdiff=0;
                hasbigdiff=false;
                countdifftimes=0;
                sensorInil=false;
                toGETdata=false;
                startTime=System.currentTimeMillis();
                lasty=0;
                isFirst=false;
                lastx=0;
                lastz=0;
                textStr="";
                historyPoint.clear();
                tryPoint.clear();
                tryPoint = new LinkedList<Point>();
                clickTimes = 0;
            }
        });




    }


    @Override
    protected  void onPause(){
        super.onPause();
        if(javaCameraView!=null)
            javaCameraView.disableView();


    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(javaCameraView!=null)
            javaCameraView.disableView();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!javaCameraView.isEnabled()){
            return false;
        }
        if(mRgba == null){
            return false;
        }
        if(isHasInit1)
            return false;
        int cols= mRgba.cols();
        int rows=mRgba.rows();
        /*
        int xOffset = (javaCameraView.getWidth()-cols)/2;
        int yOffset = (javaCameraView.getHeight()-rows)/2;
        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;
        */

        int x = (int)(event.getX()/javaCameraView.getWidth()*cols);
        int y = (int)(event.getY()/javaCameraView.getHeight()*rows);
        y-=100;

        tryPoint.add(new Point(x,y));

        int x1=Math.abs(x-(int)myROI.tl().x);
        int y1=Math.abs(y-(int)myROI.tl().y);
        int subwidth=(int)myROI.width/9;
        int subheight=(int)myROI.height/6;
        x1=x1/subwidth;
        y1=y1/subheight;
        //textStr="x="+x1+"  "+"y="+y1;

        Log.i(TAG,"Touch image coordinates :("+ x+ ","+y+")");

       // if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return super.onTouchEvent(event);


        if(isTrackingfinger){
            Scalar hsv=setHsvRgb(x,y,rows,cols,detectFinger);
            Log.d(TAG,"click position hsv"+hsv.toString());
           // hsvOffinger=hsv;
            hasfindFinger=true;
        }
        else if(isTrackinghand){
            Scalar hsv=setHsvRgb(x,y,rows,cols,detectHand);
           // hsvOfhand=hsv;
            hasfindHand=true;
        }
        return super.onTouchEvent(event);
    }
    public void getInitHsv(){
        int x=400,y=985;
        int rows=mRgba.rows(),cols=mRgba.cols();
        initialDetect=new ColorBlobDetector();
        initialDetect.setColorRadius(new Scalar(30,30,90));
        setHsvRgb(x,y,rows,cols,initialDetect);

    }
    public Scalar setHsvRgb(int x,int y,int rows,int cols,ColorBlobDetector detector){


        Log.i(TAG,"Touch image coordinates :("+ x+ ","+y+")");
        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return new Scalar(255);

        Rect touchedRect = new Rect();
        touchedRect.x = (x>5) ? x-5 : 0;
        touchedRect.y = (y>5) ? y-5 : 0;

        touchedRect.width = (x+5 < cols) ? x + 5 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+5 < rows) ? y + 5 - touchedRect.y : rows - touchedRect.y;

        //获取点击区域
        Mat touchedRegionRgba = mRgba.submat(touchedRect);
        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        //计算点击区域的hsv
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointcount = touchedRect.width * touchedRect.height;

        for(int i=0; i< mBlobColorHsv.val.length;i++){
            mBlobColorHsv.val[i] /= pointcount;
        }

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);
        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] + ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");
        Log.i(TAG, "Touched Hsv color: (" + mBlobColorHsv.val[0] + ", " + mBlobColorHsv.val[1] + ", " + mBlobColorHsv.val[2] + ", " + mBlobColorHsv.val[3] + ")");
        if(mBlobColorHsv!= null)
            detector.setHsvColor(mBlobColorHsv);
        Log.d(TAG,"click position hsv"+mBlobColorHsv.toString());
        Imgproc.resize(detector.getSpectrum(),mSpectrum,SPECTRUM_SIZE);

        //mIsColorSelected=true;
        touchedRegionHsv.release();
        touchedRegionRgba.release();
        return mBlobColorHsv;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor){
        Mat pointMatRgba = new Mat() ;
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
        return new Scalar(pointMatRgba.get(0, 0));
    }

    private  Mat matProcess(Mat mRgba,ColorBlobDetector detector){

        Imgproc.GaussianBlur(mRgba,mRgba,new org.opencv.core.Size(3,3),1,1);
        /*
        if(!mIsColorSelected)
            return mRgba;
            */
        /*
        List<MatOfPoint> contours = detector.getContours();
        //detector过滤处理
        detector.process(mRgba);
        */
        Scalar drawColor;
        if(detector==detectHand){
            drawColor=new Scalar(0,255,0);
        }
        else {
            drawColor=new Scalar(0,0,255);
        }
        detector.process(mRgba);
        List<MatOfPoint> contours = detector.getContours();
        Log.d(TAG, "Contours count: " + contours.size());
        if (contours.size() <= 0) {
            return mRgba;
        }


        RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(0)	.toArray()));

        double boundWidth = rect.size.width;
        double boundHeight = rect.size.height;
        boolean nofind=true;
        int boundPos = 0;
        //find maxsize
        double maxsize=0;
        for (int i = 0; i < contours.size(); i++) {
            rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
            Rect aRect = Imgproc.boundingRect(new MatOfPoint(contours.get(i).toArray()));

            if(hasInit){
                Point leftdown=aRect.tl();
                leftdown.y=leftdown.y+aRect.height;
                if(aRect.height*aRect.width<2000)//20000
                    continue;
                double length=myROI.br().x-leftdown.x+leftdown.y-myROI.tl().y;
                if (myROI.br().x-leftdown.x<aRect.width/3)
                    continue;
                if(leftdown.y-myROI.tl().y<aRect.height/3)
                    continue;
            }

            if (rect.size.width * rect.size.height > maxsize) {
                maxsize=rect.size.width * rect.size.height;
                boundPos = i;
                nofind=false;
            }
        }
        if(nofind){
            hasTrack=false;
            return mRgba;
        }

        rect=Imgproc.minAreaRect(new MatOfPoint2f(contours.get(boundPos).toArray()));
        Log.d(TAG,"area is:"+rect.size.width*rect.size.height);
        //if(rect.size.width*rect.size.height<50000)
           // return mRgba;
        Rect boundRect = Imgproc.boundingRect(new MatOfPoint(contours.get(boundPos).toArray()));

        if(!hasInit){
            myROI=boundRect;
            int y=0;
            if(myROI.tl().y>50){
                y=50;
            }
            Point tl=new Point(myROI.tl().x,myROI.tl().y-y);
            myROI = new Rect(tl,myROI.br());
            hasInit=true;
            startTime=System.currentTimeMillis();
        }

        //first
        Core.rectangle( mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR_WHITE, 2, 8, 0 );
        //textStr="area is "+boundRect.width*boundRect.height;
        double rect_width=boundRect.width,rect_height=boundRect.height;


        if(rect_height>=rect_width*2.3){
            fingerState=true;
            fingerWidth=rect_width;
        }
        else if(rect_width>rect_height*2){
            fingerState=true;
            fingerWidth=rect_height;
        }
        else
            fingerState=false;

        /*
        Log.d(TAG,
                " Row start ["+
                        (int) boundRect.tl().y + "] row end ["+
                        (int) boundRect.br().y+"] Col start ["+
                        (int) boundRect.tl().x+"] Col end ["+
                        (int) boundRect.br().x+"]");
        */

        double a = boundRect.br().y - boundRect.tl().y;
        a = a * 0.7;
        a = boundRect.tl().y + a;
        Log.d(TAG, " A ["+a+"] br y - tl y = ["+(boundRect.br().y - boundRect.tl().y)+"]");


        //second
        //Core.rectangle( mRgba, boundRect.tl(), boundRect.br(), drawColor, 2, 8, 0 );

        MatOfPoint2f pointMat = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(boundPos).toArray()), pointMat, 3, true);//逼近点集
        contours.set(boundPos, new MatOfPoint(pointMat.toArray()));

        MatOfInt hull = new MatOfInt();
        MatOfInt4 convexDefect = new MatOfInt4();
        Imgproc.convexHull(new MatOfPoint(contours.get(boundPos).toArray()), hull);//由点集找凸包

        if(hull.toArray().length < 4) return mRgba;

        Imgproc.convexityDefects(new MatOfPoint(contours.get(boundPos)	.toArray()), hull, convexDefect);
        List<MatOfPoint> hullPoints = new LinkedList<MatOfPoint>();
        List<Point> listPo = new LinkedList<Point>();
        for (int j = 0; j < hull.toList().size(); j++) {
            listPo.add(contours.get(boundPos).toList().get(hull.toList().get(j)));
        }

        MatOfPoint e = new MatOfPoint();
        e.fromList(listPo);
        hullPoints.add(e);

        List<MatOfPoint> defectPoints = new LinkedList<MatOfPoint>();
        List<Point> listPoDefect = new LinkedList<Point>();
        for (int j = 0; j < convexDefect.toList().size(); j = j+4) {
            Point farPoint = contours.get(boundPos).toList().get(convexDefect.toList().get(j+2));
            Integer depth = convexDefect.toList().get(j+3);
            if(depth > iThreshold && farPoint.y < a){
                listPoDefect.add(contours.get(boundPos).toList().get(convexDefect.toList().get(j+2)));
            }
            Log.d(TAG, "defects ["+j+"] " + convexDefect.toList().get(j+3));
        }

        MatOfPoint e2 = new MatOfPoint();
        e2.fromList(listPo);
        defectPoints.add(e2);

        Log.d(TAG, "hull: " + hull.toList());
        Log.d(TAG, "defects: " + convexDefect.toList());

       /*
        List<Point> fourPoint=findfourPoint(listPo);
        for(Point p :fourPoint){
            if(p!=null){
                Core.circle(mRgba, p, 40, new Scalar(0,0,255),Core.FILLED);
            }
        }
        */
        Imgproc.drawContours(mRgba,hullPoints, -1, drawColor, 3);
        //Imgproc.drawContours(mRgba, defectPoints, -1, drawColor, 3);
        if(!fingerState){
            fingerWidth=findWidth(listPo);
        }
        //textStr="fingerWidth :"+fingerWidth;
        for( Point p: listPo){
            Core.circle(mRgba, p, 20, new Scalar(215,111, 55),Core.FILLED);
            //findwidth(listPo);
        }
       Point thePoint=findleftdown(listPo);
        if(thePoint!=null){
            currentPoint=thePoint;
            finger=thePoint;
            if(!hasTrack){
                hasTrack=true;
                startPoint=thePoint;
                lastPoint=thePoint;
                Core.circle(mRgba, thePoint, 40, new Scalar(255,0,255),Core.FILLED);
            }
            else{
                if(lastPoint!=null&&PointDist(thePoint,lastPoint)>10){
                    hasTrack=false;
                }
                else{
                lastPoint=thePoint;
                }
            }
        }
        else if(thePoint==null){
            hasTrack=false;
        }
        if(!hasTrack&&hasClick){
            Long nowTime=System.currentTimeMillis();
            if((nowTime-clickStartTime)/1000>0.5)
                hasClick=false;
        }
        else if(hasClick){
            if(currentPoint!=null){
                currentPoint.x+=50;
                historyPoint.add(currentPoint);

                recordPoint=currentPoint;
                int x=Math.abs((int)currentPoint.x-(int)myROI.tl().x);
                int y=Math.abs((int)currentPoint.y-(int)myROI.tl().y);
                int subwidth=(int)myROI.width/100;
                int subheight=(int)myROI.height/100;
                x=x/subwidth;
                y=y/subheight;
                Log.d(TAG,"click position ("+x+","+y+")");
                if(x>=73&&x<=94&&y>=11&&y<=46)
                    textStr=textStr.concat("3");
                else if(x>=68&&x<=94&&y>=47&&y<=70)
                    textStr=textStr.concat("6");
                else if(x>=58&&x<=95&&y>=70&&y<=97)
                    textStr=textStr.concat("9");
                else if(x>=44&&x<=72&&y>=11&&y<=41)
                    textStr=textStr.concat("2");
                else if(x>=47&&x<=65&&y>=44&&y<=58)
                    textStr=textStr.concat("5");
                else if(x>=45&&x<=65&&y>=60&&y<=83)
                    textStr=textStr.concat("8");
                else if(x>=22&&x<=59&&y>=6&&y<=53)
                    textStr=textStr.concat("1");
                /*
                if((x>=14&&x<=19)&&(y>=2&&y<=4))
                    textStr=textStr.concat("7");
                else if((x>=14&&x<=19)&&(y>=4&&y<=7))
                    textStr=textStr.concat("8");
                else if((x>=14&&x<=19)&&(y>=8&&y<=13))
                    textStr=textStr.concat("9");
                else if((x>=9&&x<=13)&&(y>=1&&y<=3))
                    textStr=textStr.concat("4");
                else if((x>=9&&x<=13)&&(y>=4&&y<=7))
                    textStr=textStr.concat("5");
                else if((x>=9&&x<=13)&&(y>=8&&y<=15))
                    textStr=textStr.concat("6");
                else if((x<=8)&&(y>=1&&y<=3))
                    textStr=textStr.concat("1");
                else if((x<=8)&&(y>=4&&y<=7))
                    textStr=textStr.concat("2");
                else if((x<=8)&&(y>=8&&y<=15))
                    textStr=textStr.concat("3");
                */
            }
            clickTimes++;
            hasClick=false;
        }
        if(hasInit){

            return mRgba;
        }
        /*
        double minY=findMinY(listPo);
        if(Math.abs(minY-4.0)<2){
            //go and getfingerhsv
            Point fingerPoint=goFindFinger(minY,listPo);
            setHsvRgb((int)fingerPoint.x,(int)fingerPoint.y,mRgba.rows(),mRgba.cols(),detectFinger);
            setHsvRgb(400,985,mRgba.rows(),mRgba.cols(),detectHand);
            hasInit=true;
            Core.circle(mRgba,fingerPoint, 40, new Scalar(0,255,255),Core.FILLED);
            //go and gethandhsv
        }
        */
        if(isTrackinghand){
            //drawKeyboard(mRgba,fourPoint);
        }

        if(toRecordhand){
            toRecordhand=false;
            //记录下手背位置，并每次都显示位置
            //correctPoint=fourPoint;
            hasRecordHand=true;
        }


        //third


        //Imgproc.drawContours(mRgba,hullPoints,-1,new Scalar(0,255,0),Core.FILLED);


        int defectsTotal = (int) convexDefect.total();
        Log.d(TAG, "Defect total " + defectsTotal);

        /*this.numberOfFingers = listPoDefect.size();
        if(this.numberOfFingers > 5) this.numberOfFingers = 5;

        mHandler.post(mUpdateFingerCountResults);*/

        for(Point p : listPoDefect){
            //forth
            //Core.circle(mRgba, p, 6, new Scalar(255,0,255));
        }
        return mRgba;

    }
    private Point findbiggestChange(int times){
        if(lastRgb==null)
            return new Point(500,500);
        Point x1=myROI.tl(),y1=myROI.br();
        int width=(int)(y1.x-x1.x),height=(int)(y1.y-x1.y);
        //

        List<Rect> rectlist=new LinkedList<Rect>();

        for(int i=0;i<times;i++){
            Rect rect=new Rect((int)x1.x,(int)x1.y+height*i/times,width,height/times);
            rectlist.add(rect);
        }
        int rows=findminRect(rectlist);
        List<Rect> rectlist1=new LinkedList<Rect>();
        for(int i=0;i<times;i++){
            Rect rect=new Rect((int)x1.x+i*width/times,(int)x1.y+rows*height/times,width/times,height/times);
            rectlist1.add(rect);
        }
        int cols=findminRect(rectlist1);
        int tt=times*2;
        int xx=(int)x1.x+cols*width/times+width/tt,yy=(int)x1.y+rows*height/times+height/tt;
        return new Point(xx,yy);
    }
    private int findminRect(List<Rect> rectlist){
        double maxdiff=0;
        int index=0;
        for(int i=0;i<rectlist.size();i++){
            Mat mat1=mRgba.submat(rectlist.get(i)),mat2=lastRgb.submat(rectlist.get(i));
            double current=Core.norm(mat1,mat2);
            if(current>maxdiff){
                maxdiff=current;
                index=i;
            }
        }
        return index;
    }
    private double findWidth(List<Point> listPo){
        //find average height
        double height=0;
        for(Point p: listPo){
            height+=p.x;
        }
        height=height/listPo.size();
        Point left=null,right=null;
        double miny=10000,maxy=0;
        for(Point p : listPo){
            if(p.x<height)
                continue;
            if(p.y<miny){
                miny=p.y;
                left=p;
            }
            else  if(p.y>maxy){
                maxy=p.y;
                right=p;
            }
        }
        if(left!=null&&right!=null){
            Core.line(mRgba,left,right,new Scalar(10,100,200),20);
            return Math.sqrt(Math.pow(left.x-right.x,2)+Math.pow(left.y-right.y,2)) ;
        }
        else
            return 0.0;

    }
    private double PointDist(Point a,Point b){
       return Math.sqrt(Math.pow(a.x-b.x,2)+Math.pow(a.y-b.y,2)) ;
    }
    private  Mat myprocess1(Mat mRgba){
        //matProcess(mRgba,detectHand);
        if(lastRgb!=null){
            Mat lastMat=lastRgb.submat(myROI);
            Mat nowMat=mRgba.submat(myROI);
            Point x1=myROI.tl(),y1=myROI.br();
            /*
            int width=(int)(y1.x-x1.x),height=(int)(y1.y-x1.y);

            int colorR=50;
            Rect rect11=new Rect((int)x1.x,(int)x1.y,width/3,height/3);
            submatprocess(mRgba,rect11,new Scalar(colorR+60,255,0));

            Rect rect12=new Rect((int)x1.x+width/3,(int)x1.y,width/3,height/3);
            submatprocess(mRgba,rect12,new Scalar(colorR+60,255,0));

            Rect rect13=new Rect((int)x1.x+width*2/3,(int)x1.y,width/3,height/3);
            submatprocess(mRgba,rect13,new Scalar(colorR+120,255,0));

            int colorG=50;
            Rect rect21=new Rect((int)x1.x,(int)x1.y+height/3,width/3,height/3);
            submatprocess(mRgba,rect21,new Scalar(255,colorG,0));

            Rect rect22=new Rect((int)x1.x+width/3,(int)x1.y+height/3,width/3,height/3);
            submatprocess(mRgba,rect22,new Scalar(255,colorG+60,0));

            Rect rect23=new Rect((int)x1.x+width*2/3,(int)x1.y+height/3,width/3,height/3);
            submatprocess(mRgba,rect23,new Scalar(255,colorG+120,0));


            int colorB=50;
            Rect rect31=new Rect((int)x1.x,(int)x1.y+2*height/3,width/3,height/3);
            submatprocess(mRgba,rect31,new Scalar(0,255,colorB));

            Rect rect32=new Rect((int)x1.x+width/3,(int)x1.y+2*height/3,width/3,height/3);
            submatprocess(mRgba,rect32,new Scalar(0,255,colorB+50));

            Rect rect33=new Rect((int)x1.x+2*width/3,(int)x1.y+2*height/3,width/3,height/3);
            submatprocess(mRgba,rect33,new Scalar(0,255,colorB+100));

            */
            double diff=Core.norm(lastMat,nowMat);
            Log.d(TAG,"diff is :"+diff);
                long nowTime=System.currentTimeMillis();
            Log.d(TAG,"time is"+(nowTime-startTime)/1000);
            if((nowTime-startTime)/1000>2){
                Log.d(TAG,"time over");
                int width=(int)(y1.x-x1.x),height=(int)(y1.y-x1.y);
                int x=(int)x1.x+width/2,y=(int)x1.y+height/2;
                Point thePoint=new Point(x,y);
                setHsvRgb(x,y,mRgba.rows(),mRgba.cols(),detectFinger);
                recordPoint=thePoint;
                isHasInit1=true;
                hasbigdiff=true;
            }

        }
        Core.rectangle( mRgba, myROI.tl(), myROI.br(), CONTOUR_COLOR_WHITE, 2, 8, 0 );
        lastRgb=mRgba;


        return  mRgba;
    }
    private void submatprocess(Mat mRgba,Rect rect,Scalar scalar){
        if(lastRgb!=null){
            Mat mat1=mRgba.submat(rect),mat2=lastRgb.submat(rect);
            double diff=Core.norm(mat1,mat2);
            Log.d(TAG,"thediff is:"+diff);
            if(diff>12000){
                Core.rectangle(mRgba,rect.tl(),rect.br(),scalar);
            }
        }
    }
    private  Mat myprocess2(Mat mRgba){
        return matProcess(mRgba,initialDetect);
    }
    private Mat myprocess3(){
        //Core.rectangle( mRgba, myROI.tl(), myROI.br(), CONTOUR_COLOR_WHITE, 2, 8, 0 );

        matProcess(mRgba,detectFinger);

        Core.rectangle( mRgba, myROI.tl(), myROI.br(), new Scalar(88,88,88), 2, 8, 0 );
        //Core.circle(mRgba,recordPoint,80,new Scalar(255,255,44),Core.FILLED);
        return mRgba;
    }
    @Override
    protected  void onResume(){
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.d(TAG,"Opencv successfully loaded");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);


        }
        else{
            Log.d(TAG,"Opencv not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9,this,mLoaderCallBack);
        }


    }
    private Point findleftdown(List<Point> listPo){
        //minx maxy
        List<Point> minX=new LinkedList<Point>();
        double min=1000;
        for(Point p: listPo){
            if(p.x<min)
                min=p.x;
        }
        for (Point p :listPo){
            if(p.x==min)
                minX.add(p);
        }
        Point ainP=null;
        double max=0;
        for(Point p:minX){
            if(p.y>max){
                max=p.y;
                ainP=p;
            }
        }
        return  ainP;
    }
    private  List<Point> findfourPoint(List<Point> listPo){
        List<Point> result = new LinkedList<Point>();
        double minY=600;
        double maxX=0,minX=10000;
        Point leftdown=null,rightdown=null;
        for (Point p : listPo){
            if(p.y>minY&&p.x>maxX){
                maxX=p.x;
                rightdown=p;
            }
            else if(p.y>minY&&p.x<minX){
                minX=p.x;
                leftdown=p;
            }
        }
        if(rightdown==null||leftdown==null)
            return result;
        double width=(rightdown.x-leftdown.x)*1;
        rightdown=new Point (leftdown.x+width,leftdown.y);

        double height=1000;//find min
        Point leftup=null,rightup=null;
        for (Point p : listPo){
            if(p.y<minY&&p.x==maxX&&rightdown.y-p.y<height){
                rightup=p;
                height=rightdown.y-p.y;
            }
            else if(p.y<minY&&p.x==minX&&leftdown.y-p.y<height){
                leftup=p;
                height=leftdown.y-p.y;
            }
        }
        //height=height*0.9;
        leftup=new Point(leftdown.x,leftdown.y-height);
        rightup=new Point(rightdown.x,rightdown.y-height);
        result.add(leftup);
        result.add(rightup);
        result.add(leftdown);
        result.add(rightdown);
        int findnum=0;
        if(leftdown!=null)
            findnum++;
        if(leftup!=null)
            findnum++;
        if(rightdown!=null)
            findnum++;
        if(rightdown!=null)
            findnum++;
        Log.d(TAG,"findnum="+findnum);
        return result;
    }
    private  double findMinY(List<Point> listPo){
        double min=10000;
        for(Point p : listPo){
            if(p.y<min)
                min=p.y;
        }
        Log.d(TAG,"miny is:"+min);
        return min;
    }
    private Point goFindFinger(double minY,List<Point> listPo){
        int x=0,num=0;
        for(Point p: listPo){
            if(p.y==minY){
                x+=p.x;
                num++;
            }
        }
        if(num==0)
            return new Point(0,0);
        else
            return new Point(x/num,minY+300);
    }

    public  void drawKeyboard(Mat mat,List<Point> points){
        /*
        result.add(leftup);
        result.add(rightup);
        result.add(leftdown);
        result.add(rightdown);
        */
        int lineThick=20;
        if(points.toArray().length<4){
            return;
        }
        for(Point p : points){
            if (p==null)
                return ;
        }
        Point leftup=points.get(0);
        Point rightup=points.get(1);
        Point leftdown=points.get(2);
        Point rightdown=points.get(3);

        Core.line(mat,leftdown,rightdown,new Scalar(255,255,0),lineThick);
        Core.line(mat,leftup,rightup,new Scalar(255,255,0),lineThick);
        Core.line(mat,leftup,leftdown,new Scalar(255,255,0),lineThick);
        Core.line(mat,rightdown,rightup,new Scalar(255,255,0),lineThick);

        int height=(int)(leftdown.y-leftup.y);
        int width=(int)(leftdown.x-rightdown.x);
        height=height/3;
        width=width/3;
        Point x,y;
        x=new Point(leftdown.x,leftdown.y-height);
        y=new Point(rightdown.x,rightdown.y-height);
        Core.line(mat,x,y,new Scalar(255,255,0),lineThick);

        x=new Point(leftdown.x,leftdown.y-2*height);
        y=new Point(rightdown.x,rightdown.y-2*height);
        Core.line(mat,x,y,new Scalar(255,255,0),lineThick);

        x=new Point(leftdown.x-width,leftdown.y);
        y=new Point(leftup.x-width,leftup.y);
        Core.line(mat,x,y,new Scalar(255,255,0),lineThick);

        x=new Point(leftdown.x-2*width,leftdown.y);
        y=new Point(leftup.x-2*width,leftup.y);
        Core.line(mat,x,y,new Scalar(255,255,0),lineThick);

    }

    private void trackPoint(Point point){
        double movey=Math.abs(point.y-startPoint.y);
        double movex=Math.abs(point.x-startPoint.x);
        double x=point.x-myROI.tl().x,y=point.y-myROI.tl().y;
        if(movey>myROI.height*2/3){
            if(x<myROI.width/3)
                textStr=textStr.concat("a");
            else if(x>myROI.width/3&&x<myROI.width*2/3)
                textStr=textStr.concat("b");
            else if(x>myROI.width*2/3&&x<myROI.width)
                textStr=textStr.concat("c");

            startPoint=point;
        }
        else if (movex>myROI.width*2/3){
            if(y<myROI.height/3)
                textStr=textStr.concat("d");
            else if(y>myROI.height/3&&y<myROI.height*2/3)
                textStr=textStr.concat("e");
            else if(y>myROI.height*2/3&&y<myROI.height)
                textStr=textStr.concat("f");
            startPoint=point;
        }
    }






}





/*
 * Copyright (C) 2016 Player One
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package player.efis.common;

import java.util.Iterator;

import player.gles20.Line;
import player.gles20.PolyLine;
import player.gles20.Polygon;
import player.gles20.Square;
import player.gles20.Triangle;
import player.ulib.*;

import player.gles20.GLText;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;


/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class EFISRenderer //implements GLSurfaceView.Renderer
{
    private static final String TAG = "EFISRenderer";

    protected Triangle mTriangle;
    protected Square mSquare;
    protected Line mLine;
    protected PolyLine mPolyLine;
    protected Polygon mPolygon;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    protected final float[] mMVPMatrix = new float[16];
    protected final float[] mProjectionMatrix = new float[16];
    protected final float[] mViewMatrix = new float[16];
    protected final float[] mRotationMatrix = new float[16];
    protected final float[] mFdRotationMatrix = new float[16];  // for Flight Director
    protected final float[] mRmiRotationMatrix = new float[16]; // for RMI / Compass Rose

    private float mAngle;

    // OpenGL
    protected int pixW;
    protected int pixH;         // Width & Height of window in pixels
    protected int pixW2;
    protected int pixH2;       // Half Width & Height of window in pixels
    protected int pixM;               // The smallest dimension of pixH or pixM
    protected int pixM2;              // The smallest dimension of pixH2 or pixM2
    protected float zfloat;           // A Z to use for layering of ortho projected markings*/

    // Artificial Horizon
    protected float pitchInView;      // The degrees pitch to display above and below the lubber line
    private float pitch, roll;      // Pitch and roll in degrees
    public float pitchTranslation;    // Pitch amplified by 1/2 window pixels for use by glTranslate
    protected float rollRotation;     // Roll converted for glRotate
    // Airspeed Indicator
    protected float IASInView;        // The indicated units to display above the center line

    //private int   IASValue;       // Indicated Airspeed
    protected float IASValue;         // Indicated Airspeed
    protected float IASTranslation;   // Value amplified by 1/2 window pixels for use by glTranslate

    // The following should be read from a calibration file by an init routine
    private int IASMaxDisp;         // The highest speed to show on tape

    // Altimeter
    protected float MSLInView;        // The indicated units to display above the center line
    protected int MSLValue;           // Altitude above mean sea level, MSL
    protected float MSLTranslation;   // Value amplified by 1/2 window pixels for use by glTranslate
    private float baroPressure;     // Barometric pressure in in-Hg
    public int AGLValue;           // Altitude above ground, AGL

    // The following should be read from a calibration file by an init routine
    protected int MSLMinDisp;         // The lowest altitude to show on tape
    protected int MSLMaxDisp;         // The highest altitude to show on tape

    // VSI
    private float VSIInView;        // Vertical speed to display above the centerline
    private int VSIValue;           // Vertical speed in Feet/minute
    private float VSINeedleAngle;   // The angle to set the VSI needle

    //DI
    private float DIInView;         // The indicated units to display above the center line
    protected float DIValue;          // Altitude MSL
    private float SlipValue;        // was int
    private float BatteryPct;       // Battery usage
    private float GForceValue;      // G force
    private float GSValue;
    private float ROTValue;         // Rate Of Turn
    private float DITranslation;    // Value amplified by 1/2 window pixels for use by glTranslate

    // Geographic Coordinates
    protected float LatValue;        // Latitude
    protected float LonValue;        // Longitude

    //FPV - Flight Path Vector
    private float fpvX;            // Flight Path Vector X
    private float fpvY;            // Flight Path Vector Y

    //Flight Director
    protected float FDTranslation;           // = -6 / pitchInView  * pixM2;  // command 6 deg pitch up
    protected float FDRotation;              // = 20;  // command 20 deg roll

    // Onscreen elements
    protected boolean displayInfoPage;       // Display The Ancillary Information
    protected boolean displayFlightDirector; // Display Flight Director
    protected boolean displayRMI;            // Display RMI
    protected boolean displayHITS;           // Display the Highway In The Sky
    protected boolean displayDEM;            // Display the DEM terrain

    //3D map display
    protected boolean displayAirport;
    protected boolean displayAirspace;
    protected boolean displayTerrain;
    protected boolean displayTape;
    public boolean displayMirror;
    protected boolean displayFPV;

    protected boolean ServiceableDevice;  // Flag to indicate no faults
    protected boolean ServiceableAh;      // Flag to indicate AH failure
    protected boolean ServiceableAlt;     // Flag to indicate Altimeter failure
    protected boolean ServiceableAsi;     // Flag to indicate Airspeed failure
    protected boolean ServiceableDi;      // Flag to indicate DI failure
    protected boolean Calibrating;        // no longer used
    private String CalibratingMsg;      // no longer used

    private float mX, mY;                        // keypress location
    protected final float portraitOffset = 0.40f;  // the magic number for portrait offset

    //Demo Modes
    protected boolean bDemoMode;
    private String sDemoMsg;

    protected GLText glText;      // A GLText Instance
    protected Context context;    // Context (from Activity)

    // Colors
    protected float tapeShade = 0.600f; // grey
    protected float foreShade = 0.999f; // white
    protected float backShade = 0.001f; // black
    private float gamma = 1;
    protected float theta = 1;

    public enum layout_t
    {
        PORTRAIT,
        LANDSCAPE
    }

    public layout_t Layout = layout_t.LANDSCAPE;

    public EFISRenderer(Context context)
    {
        super();
        this.context = context;      // Save Specified Context

        // Initialisation of variables
        pitchTranslation = rollRotation = 0; // default object translation and rotation

        IASTranslation = 0;  // default IAS tape translation
        IASValue = 0;        // The default to show if no IAS calls come in
        MSLTranslation = 0;  // default MSL tape translation
        MSLValue = 0;        // The default to show if no MSL calls come in
        VSIValue = 0;        // The default vertical speed

        IASMaxDisp = 200;
        MSLMinDisp = -1000;
        MSLMaxDisp = 20000;

        VSIInView = 2000;
        displayFPV = true;
    }

    int FrameCounter = 0;
    protected float[] scratch1 = new float[16];  // moved to class scope
    protected float[] scratch2 = new float[16];  // moved to class scope
    protected float[] altMatrix = new float[16]; // moved to class scope
    protected float[] iasMatrix = new float[16]; // moved to class scope
    protected float[] fdMatrix = new float[16];  // moved to class scope
    protected float[] rmiMatrix = new float[16]; // moved to class scope

    //-------------------------------------------------------------------------
    //
    //
    public void setSpinnerParams()
    {
        // This code determines where the spinner control
        // elements are displayed. Used by WPT and ALT
        if (Layout == layout_t.LANDSCAPE) {
            // Landscape --------------
            lineAutoWptDetails = 0.00f;
            lineAncillaryDetails = -0.30f;

            if (fatFingerActive) {
                selWptDec = 0.75f * pixH2;
                selWptInc = 0.45f * pixH2;
                selAltDec = -0.45f * pixH2;
                selAltInc = -0.75f * pixH2;

                lineC = 0.2f;
                leftC = -0.55f;
                spinnerStep = 0.25f;
                spinnerTextScale = 2.0f;
            }
            else {
                // Top
                selWptDec = 0.90f * pixH2;
                selWptInc = 0.74f * pixH2;
                selAltDec = -0.74f * pixH2;
                selAltInc = -0.90f * pixH2;

                lineC = 0.50f;
                leftC = 0.6f;
                spinnerStep = 0.1f;
                spinnerTextScale = 1f;
            }
        }
        else {
            // Portrait ---------------
            lineAutoWptDetails = -0.60f;
            lineAncillaryDetails = -0.85f;

            if (fatFingerActive) {
                selWptDec = 0.7f * pixH2;
                selWptInc = 0.4f * pixH2;
                selAltDec = -0.4f * pixH2;
                selAltInc = -0.7f * pixH2;

                lineC = 0.15f;
                leftC = -0.75f;
                spinnerStep = 0.5f;
                spinnerTextScale = 2f;
            }
            else {
                selWptDec = -0.30f * pixH2;
                selWptInc = -0.41f * pixH2;
                selAltDec = -0.80f * pixH2;
                selAltInc = -0.91f * pixH2;

                lineC = -0.55f; //lineC = -0.90f;
                leftC = 0.6f;
                spinnerStep = 0.1f;
                spinnerTextScale = 1f;
            }
        }
    }


    //-------------------------------------------------------------------------
    // Utility method for debugging OpenGL calls. Provide the name of the call
    // just after making it:
    // <p>
    // <pre>
    // mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
    // MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
    // <p>
    // If the operation is not successful, the check throws an error.
    //
    // @param glOperation - Name of the OpenGL call to check.
    //
    public static void checkGlError(String glOperation)
    {
        /*  bugbug
        int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, glOperation + ": glError " + error);
			throw new RuntimeException(glOperation + ": glError " + error);
		}
		*/
        int error;
        if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    //-------------------------------------------------------------------------
    //  Returns the rotation angle of the triangle shape (mTriangle).
    //
    public float getAngle()
    {
        return mAngle;
    }

    //-------------------------------------------------------------------------
    //  Sets the rotation angle of the triangle shape (mTriangle).
    //
    public void setAngle(float angle)
    {
        mAngle = angle;
    }

    protected void renderCalibrate(float[] matrix)
    {
        String t = CalibratingMsg; //"Calibrating...";
        glText.begin(1.0f, 0f, 0f, 1.0f, matrix); // Red
        glText.setScale(5.0f);
        glText.drawCX(t, 0, 0);            // Draw  String
        glText.end();
    }

    protected void renderDemoMode(float[] matrix)
    {
        String s = sDemoMsg; //"demo mode";
        glText.begin(1.0f, 0f, 0f, 1.0f, matrix); // Red
        glText.setScale(9.0f);
        glText.drawCX(s, 0, 0);
        glText.end();
    }


    public void setDemoMode(boolean demo, String msg)
    {
        bDemoMode = demo;
        sDemoMsg = msg;
    }

    //-------------------------------------------------------------------------
    // Flight Director
    //

    protected float PPD_DIV = 30; // for landscape

    protected void renderFlightDirector(float[] matrix)
    {
        //int i;
        float z, pixPerDegree;

        z = zfloat;
        //pixPerDegree = pixM2 / pitchInView;
        pixPerDegree = pixM2 / PPD_DIV;

        // fwd triangles
        mTriangle.SetWidth(1);
        mTriangle.SetColor(theta * 1, theta * 0.5f, theta * 1, 1);  //purple
        mTriangle.SetVerts(0.0f * pixPerDegree, 0.0f * pixPerDegree, z,
                10.0f * pixPerDegree, -3.0f * pixPerDegree, z,
                12.0f * pixPerDegree, -2.0f * pixPerDegree, z);
        mTriangle.draw(matrix);
        mTriangle.SetVerts(0.0f * pixPerDegree, 0.0f * pixPerDegree, z,
                -12.0f * pixPerDegree, -2.0f * pixPerDegree, z,
                -10.0f * pixPerDegree, -3.0f * pixPerDegree, z);
        mTriangle.draw(matrix);

        // rear triangles
        mTriangle.SetColor(theta * 0.6f, theta * 0.3f, theta * 0.6f, 1);  //purple'ish
        mTriangle.SetVerts(10.0f * pixPerDegree, -3.0f * pixPerDegree, z,
                12.0f * pixPerDegree, -2.0f * pixPerDegree, z,
                12.0f * pixPerDegree, -3.0f * pixPerDegree, z);
        mTriangle.draw(matrix);
        mTriangle.SetVerts(-12.0f * pixPerDegree, -2.0f * pixPerDegree, z,
                -10.0f * pixPerDegree, -3.0f * pixPerDegree, z,
                -12.0f * pixPerDegree, -3.0f * pixPerDegree, z);
        mTriangle.draw(matrix);
    }

    public void setFlightDirector(boolean active, float pit, float rol)
    {
        displayFlightDirector = active;
        FDTranslation = -pit / pitchInView * pixM2;  // command 6 deg pitch up
        FDRotation = -rol;// = 20;  // command 20 deg roll
    }


    //-------------------------------------------------------------------------
    // Attitude Indicator
    //
    protected void renderFixedHorizonMarkers()
    {
        int i;
        float z, pixPerDegree, sinI, cosI;
        float _sinI, _cosI;

        z = zfloat;
        //pixPerDegree = pixM2 / pitchInView;
        pixPerDegree = pixM2 / PPD_DIV;

        // We might make this configurable in future
        // for now force it to false
        if (false) {
            // The lubber line - W style
            mPolyLine.SetColor(1, 1, 0, 1);
            mPolyLine.SetWidth(6);

            float[] vertPoly = {
                    // in counterclockwise order:
                    -6.0f * pixPerDegree, 0.0f, z,
                    -4.0f * pixPerDegree, 0.0f, z,
                    -2.0f * pixPerDegree, -2.0f * pixPerDegree, z,
                    0.0f * pixPerDegree, 0.0f, z,
                    2.0f * pixPerDegree, -2.0f * pixPerDegree, z,
                    4.0f * pixPerDegree, 0.0f, z,
                    6.0f * pixPerDegree, 0.0f, z
            };
            mPolyLine.VertexCount = 7;
            mPolyLine.SetVerts(vertPoly);
            mPolyLine.draw(mMVPMatrix);
        }
        else {
            // The lubber line - Flight Director style
            // side lines
            int B2 = 3;
            mLine.SetWidth(2 * B2);
            mLine.SetColor(1, 1, 0, 1);  // light yellow
            mLine.SetVerts(11.0f * pixPerDegree, B2, z,
                    15.0f * pixPerDegree, B2, z);
            mLine.draw(mMVPMatrix);
            mLine.SetVerts(-11.0f * pixPerDegree, B2, z,
                    -15.0f * pixPerDegree, B2, z);
            mLine.draw(mMVPMatrix);

            mLine.SetColor(0.6f, 0.6f, 0, 1);  // dark yellow
            mLine.SetVerts(11.0f * pixPerDegree, -B2, z,
                    15.0f * pixPerDegree, -B2, z);
            mLine.draw(mMVPMatrix);
            mLine.SetVerts(-11.0f * pixPerDegree, -B2, z,
                    -15.0f * pixPerDegree, -B2, z);
            mLine.draw(mMVPMatrix);

            // outer triangles
            mTriangle.SetWidth(1);
            mTriangle.SetColor(1, 1, 0, 1);
            mTriangle.SetVerts(0.0f * pixPerDegree, 0.0f * pixPerDegree, z,
                    6.0f * pixPerDegree, -3.0f * pixPerDegree, z,
                    10.0f * pixPerDegree, -3.0f * pixPerDegree, z);
            mTriangle.draw(mMVPMatrix);
            mTriangle.SetVerts(0.0f * pixPerDegree, 0.0f * pixPerDegree, z,
                    -10.0f * pixPerDegree, -3.0f * pixPerDegree, z,
                    -6.0f * pixPerDegree, -3.0f * pixPerDegree, z);
            mTriangle.draw(mMVPMatrix);

            // inner triangle
            mTriangle.SetColor(0.6f, 0.6f, 0, 1);
            mTriangle.SetVerts(0.0f * pixPerDegree, 0.0f * pixPerDegree, z,
                    4.0f * pixPerDegree, -3.0f * pixPerDegree, z,
                    6.0f * pixPerDegree, -3.0f * pixPerDegree, z);
            mTriangle.draw(mMVPMatrix);
            mTriangle.SetVerts(0.0f * pixPerDegree, 0.0f * pixPerDegree, z,
                    -6.0f * pixPerDegree, -3.0f * pixPerDegree, z,
                    -4.0f * pixPerDegree, -3.0f * pixPerDegree, z);
            mTriangle.draw(mMVPMatrix);

            // center triangle - optional
            //mTriangle.SetVerts(0.0f * pixPerDegree,  0.0f * pixPerDegree, z,
            //		-4.0f * pixPerDegree, -2.0f * pixPerDegree, z,
            //		 4.0f * pixPerDegree, -2.0f * pixPerDegree, z);
            //mTriangle.draw(mMVPMatrix);
        }

        // The fixed roll marker (roll circle marker radius is 15 degrees of pitch, with fixed markers on the outside)
        mTriangle.SetColor(0.9f, 0.9f, 0.0f, 1); //yellow
        mTriangle.SetVerts(0.035f * pixM2, 16.5f * pixPerDegree, z,
                -0.035f * pixM2, 16.5f * pixPerDegree, z,
                0.0f, 15f * pixPerDegree, z);
        mTriangle.draw(mMVPMatrix);

        //mLine.SetColor(0.5f, 0.5f, 0.5f, 1);  // grey
        mLine.SetColor(tapeShade, tapeShade, tapeShade, 1);  // grey

        mLine.SetWidth(2);
        // The lines
        for (i = 10; i <= 30; i = i + 10) {
            sinI = UTrig.isin(i);
            cosI = UTrig.icos(i);
            mLine.SetVerts(
                    15 * pixPerDegree * sinI, 15 * pixPerDegree * cosI, z,
                    16 * pixPerDegree * sinI, 16 * pixPerDegree * cosI, z
            );
            mLine.draw(mMVPMatrix);
            mLine.SetVerts(
                    15 * pixPerDegree * -sinI, 15 * pixPerDegree * cosI, z,
                    16 * pixPerDegree * -sinI, 16 * pixPerDegree * cosI, z
            );
            mLine.draw(mMVPMatrix);
        }
        // 45 - even though it is only one number, leave the loop
        // for consitency and possible changes
        for (i = 45; i <= 60; i = i + 15) {
            sinI = UTrig.isin(i);
            cosI = UTrig.icos(i);
            // The lines
            mLine.SetVerts(
                    15 * pixPerDegree * sinI, 15 * pixPerDegree * cosI, z,
                    16 * pixPerDegree * sinI, 16 * pixPerDegree * cosI, z
            );
            mLine.draw(mMVPMatrix);
            mLine.SetVerts(
                    15 * pixPerDegree * -sinI, 15 * pixPerDegree * cosI, z,
                    16 * pixPerDegree * -sinI, 16 * pixPerDegree * cosI, z
            );
            mLine.draw(mMVPMatrix);
        }
        // 30 and 60
        for (i = 30; i <= 60; i = i + 30) {
            sinI = UTrig.isin(i);
            cosI = UTrig.icos(i);

            mLine.SetVerts(
                    15 * pixPerDegree * sinI, 15 * pixPerDegree * cosI, z,
                    17 * pixPerDegree * sinI, 17 * pixPerDegree * cosI, z
            );
            mLine.draw(mMVPMatrix);
            mLine.SetVerts(
                    15 * pixPerDegree * -sinI, 15 * pixPerDegree * cosI, z,
                    17 * pixPerDegree * -sinI, 17 * pixPerDegree * cosI, z
            );
            mLine.draw(mMVPMatrix);
        }

        // The arc
        _sinI = 0;
        _cosI = 1;
        for (i = 10; i <= 60; i = i + 5) {
            sinI = UTrig.isin(i);
            cosI = UTrig.icos(i);

            mLine.SetVerts(
                    15 * pixPerDegree * _sinI, 15 * pixPerDegree * _cosI, z,
                    15 * pixPerDegree * sinI, 15 * pixPerDegree * cosI, z
            );
            mLine.draw(mMVPMatrix);
            mLine.SetVerts(
                    15 * pixPerDegree * -_sinI, 15 * pixPerDegree * _cosI, z,
                    15 * pixPerDegree * -sinI, 15 * pixPerDegree * cosI, z
            );
            mLine.draw(mMVPMatrix);
            _sinI = sinI;
            _cosI = cosI;
        }

    } //renderFixedHorizonMarkers

    protected void renderRollMarkers(float[] matrix)
    {
        float z, pixPerDegree;
        z = zfloat;
        //pixPerDegree = pixM2 / pitchInView;					// Put the markers in open space at zero pitch
        pixPerDegree = pixM2 / PPD_DIV;                            // Put the markers in open space at zero pitch

        mTriangle.SetColor(foreShade, foreShade, foreShade, 1);
        mTriangle.SetVerts(
                0.035f * pixM2, 13.5f * pixPerDegree, z,
                -0.035f * pixM2, 13.5f * pixPerDegree, z,
                0.0f, 15f * pixPerDegree, z);
        mTriangle.draw(matrix);
    }

    protected void renderPitchMarkers(float[] matrix)
    {
        int i;
        float innerTic, outerTic, z, pixPerDegree, iPix;
        //pixPerDegree = pixH / pitchInView;
        z = zfloat;

        //pixPerDegree = pixM / pitchInView;
        if (Layout == layout_t.LANDSCAPE) {
            pixPerDegree = pixM / pitchInView; //pixH
        }
        else {
            pixPerDegree = pixM / pitchInView * 100 / 60;
        }

        innerTic = 0.10f * pixW2;
        outerTic = 0.13f * pixW2;

        // top
        for (i = 90; i > 0; i = i - 10) {
            iPix = (float) i * pixPerDegree;
            String t = Integer.toString(i);
            {
                mPolyLine.SetColor(foreShade, foreShade, foreShade, 1); //white
                mPolyLine.SetWidth(2);
                float[] vertPoly = {
                        // in counterclockwise order:
                        -innerTic, iPix, z,
                        -outerTic, iPix, z,
                        -outerTic, iPix - 0.03f * pixW2, z
                };
                mPolyLine.VertexCount = 3;
                mPolyLine.SetVerts(vertPoly);
                mPolyLine.draw(matrix);
            }
            glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
            glText.setScale(2);
            glText.drawC(t, -0.2f * pixW2, iPix + glText.getCharHeight() / 2);
            glText.end();

            {
                mPolyLine.SetColor(foreShade, foreShade, foreShade, 1); //white
                mPolyLine.SetWidth(2);
                float[] vertPoly = {
                        // in counterclockwise order:
                        0.1f * pixW2, iPix, z,
                        outerTic, iPix, z,
                        outerTic, iPix - 0.03f * pixW2, z
                };
                mPolyLine.VertexCount = 3;
                mPolyLine.SetVerts(vertPoly);
                mPolyLine.draw(matrix);
            }
            glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
            glText.drawC(t, 0.2f * pixW2, iPix + glText.getCharHeight() / 2);
            glText.end();
        }

        mLine.SetColor(tapeShade, tapeShade, tapeShade, 1);  // white
        mLine.SetWidth(2);
        for (i = 9; i >= 6; i = i - 1) {
            iPix = (float) i * pixPerDegree;
            mLine.SetVerts(-0.03f * pixW2, iPix, z,
                    0.03f * pixW2, iPix, z);
            mLine.draw(matrix);
        }

        mLine.SetVerts(-0.1f * pixW2, 5.0f * pixPerDegree, z,
                0.1f * pixW2, 5.0f * pixPerDegree, z);
        mLine.draw(matrix);

        for (i = 4; i >= 1; i = i - 1) {
            iPix = (float) i * pixPerDegree;

            mLine.SetVerts(-0.03f * pixW2, iPix, z,
                    0.03f * pixW2, iPix, z);
            mLine.draw(matrix);
        }

        // horizon line - longer and thicker
        mLine.SetWidth(6); //4
        mLine.SetVerts(-0.95f * pixW2, 0.0f, z,
                0.95f * pixW2, 0.0f, z);
        mLine.draw(matrix);

        mLine.SetWidth(2);
        for (i = -1; i >= -4; i = i - 1) {
            iPix = (float) i * pixPerDegree;
            mLine.SetVerts(-0.03f * pixW2, iPix, z,
                    0.03f * pixW2, iPix, z);
            mLine.draw(matrix);
        }

        mLine.SetVerts(-0.1f * pixW2, -5.0f * pixPerDegree, z,
                0.1f * pixW2, -5.0f * pixPerDegree, z);
        mLine.draw(matrix);

        for (i = -6; i >= -9; i = i - 1) {
            iPix = (float) i * pixPerDegree;
            mLine.SetVerts(-0.03f * pixW2, iPix, z,
                    0.03f * pixW2, iPix, z);
            mLine.draw(matrix);
        }

        // bottom
        //for (i = -10; i>=-270; i=i-10) {
        for (i = -10; i >= -90; i = i - 10) {
            iPix = (float) i * pixPerDegree;
            String t = Integer.toString(i);

            {
                mPolyLine.SetColor(foreShade, foreShade, foreShade, 1); //white
                mPolyLine.SetWidth(2);
                float[] vertPoly = {
                        // in counterclockwise order:
                        -0.10f * pixW2, iPix, z,
                        -0.13f * pixW2, iPix, z,
                        -0.13f * pixW2, iPix + 0.03f * pixW2, z
                };

                mPolyLine.VertexCount = 3;
                mPolyLine.SetVerts(vertPoly);
                mPolyLine.draw(matrix);
            }
            glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
            glText.drawC(t, -0.2f * pixW2, iPix + glText.getCharHeight() / 2);
            glText.end();

            {
                mPolyLine.SetColor(foreShade, foreShade, foreShade, 1); //white
                mPolyLine.SetWidth(2);
                float[] vertPoly = {
                        0.10f * pixW2, iPix, z,
                        0.13f * pixW2, iPix, z,
                        0.13f * pixW2, iPix + 0.03f * pixW2, z
                };

                mPolyLine.VertexCount = 3;
                mPolyLine.SetVerts(vertPoly);
                mPolyLine.draw(matrix);
            }
            glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
            glText.drawC(t, 0.2f * pixW2, iPix + glText.getCharHeight() / 2);
            glText.end();
        }
    }

    protected void renderTerrain(float[] matrix)
    {
        float pixPitchViewMultiplier, pixOverWidth, z;

		/*!
            The ModelView has units of +/- 1 about the center.  In order to keep the gyro edges outside of the edges of
			the ViewPort, it is drawn wide to deal with affect of the aspect ratio scaling and the corners during roll

			The pitch range in degrees to be viewed must fit the ModelView units of 1. To accommodate this, the gyro must
			be ovesized, hence the multiplier 90/ pitchInView.
		 */

        pixPitchViewMultiplier = 90.0f / pitchInView * pixH;
        pixOverWidth = pixW2 * 1.80f; //1.42f;
        z = zfloat;


        //todo: make consistent with renderDEMSky
        // Earth
        // Level to -180 pitch
        //mSquare.SetColor(64f / 255f, 50f / 255f, 25f / 255f, 0); //brown
        // TODO: 2017-10-31 make parameterised
        //mSquare.SetColor(gamma*0.25f, gamma*0.20f, gamma*0.10f, 0); //brown
        mSquare.SetColor(gamma * 0.30f, gamma * 0.20f, gamma * 0.10f, 1); //brown

        mSquare.SetWidth(1);
        {
            float[] squarePoly = {
                    -pixOverWidth, -2.0f * pixPitchViewMultiplier, z,
                    pixOverWidth, -2.0f * pixPitchViewMultiplier, z,
                    pixOverWidth, 0.0f, z,
                    -pixOverWidth, 0.0f, z
            };
            mSquare.SetVerts(squarePoly);
            mSquare.draw(matrix);
        }

        // Sky
        // Level to 180 pitch
        //mSquare.SetColor(0f, 0f, 0.9f, 0); //blue
        // TODO: 2017-10-31 make parameterised
        //mSquare.SetColor(gamma*0.40f, gamma*0.40f, gamma*0.99f, 1); //blue
        mSquare.SetColor(gamma * 0.10f, gamma * 0.20f, gamma * 0.30f, 1); //blue
        mSquare.SetWidth(1);
        {
            float[] squarePoly = {
                    -pixOverWidth, 0.0f, z,
                    pixOverWidth, 0.0f, z,
                    pixOverWidth, 2.0f * pixPitchViewMultiplier, z,
                    -pixOverWidth, 2.0f * pixPitchViewMultiplier, z
            };
            mSquare.SetVerts(squarePoly);
            mSquare.draw(matrix);
        }

    }


    //-------------------------------------------------------------------------
    // Set the pitch angle
    //
    public void setPitch(float degrees)
    {
        pitch = -degrees;
        pitchTranslation = pitch / pitchInView * pixH;
    }

    //-------------------------------------------------------------------------
    // Set the roll angle
    //
    public void setRoll(float degrees)
    {
        roll = degrees;
        rollRotation = roll;
    }


    // TODO: use slide to position and get rid of the top/right vars
    //-------------------------------------------------------------------------
    // RadAlt Indicator (AGL)
    //
    protected void renderFixedRADALTMarkers(float[] matrix)
    {
        float z = zfloat;
        String t;

        float top = 0; //-0.7f * pixH2;
        float right = 0; //1.14f * pixM2;
        float left = right - 0.35f * pixM2;


        // The tapes are positioned left & right of the roll circle, occupying the space based
        // on the vertical dimension, from .6 to 1.0 pixM2.  This makes the basic display
        // square, leaving extra space outside the edges for terrain which can be clipped if required.

        // Radio Altimeter (AGL) Display

        // Do a dummy glText so that the Heights are correct for the masking box
        glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
        glText.setScale(2.5f);  //was 1.5
        glText.end();

        // Mask over the moving tape for the value display box
        mSquare.SetColor(backShade, backShade, backShade, 1); //black
        mSquare.SetWidth(2);
        {
            float[] squarePoly = {
                    right, top - glText.getCharHeight(), z,
                    right, top + glText.getCharHeight(), z,
                    left, top + glText.getCharHeight(), z,
                    left, top - glText.getCharHeight(), z,
            };
            mSquare.SetVerts(squarePoly);
            mSquare.draw(matrix);
        }

        // if we are below the preset, show the warning chevrons
        final float CevronAGL = 1000.0f;
        if (AGLValue < CevronAGL) {
            float slant = 0.06f * pixM2;
            float step = 0.04f * pixM2;
            float i;
            // moving yellow chevrons
            mLine.SetColor(0.4f, 0.4f, 0.0f, 0.5f); //yellow
            mLine.SetWidth(8); //4
            for (i = left; i < right - (float) AGLValue / CevronAGL * (right - left) - step; i = i + step) {
                mLine.SetVerts(
                        i, top + glText.getCharHeight(), z,
                        slant + i, top - glText.getCharHeight(), z
                );
                mLine.draw(matrix);
            }

            // left filler
            mLine.SetVerts(
                    left, top, z,
                    left + step / 2, top - glText.getCharHeight(), z
            );
            mLine.draw(matrix);

            // right filler
            mLine.SetVerts(
                    i, top + glText.getCharHeight(), z,
                    slant / 2 + i - 2, top, z
            );
            mLine.draw(matrix);

            /* this seems a little bit over the top
            // bar
            mLine.SetVerts(
                    spinnerStep + i, top + glText.getCharHeight(), z,
                    spinnerStep + i, top - glText.getCharHeight(), z
            );
            mLine.draw(matrix);
            */
        }

        int aglAlt = Math.round((float) this.AGLValue / 10) * 10;  // round to 10
        // draw the tape text in mixed sizes
        // to clearly show the thousands
        glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
        t = Integer.toString(aglAlt / 1000);
        float margin;

        // draw the thousands digits larger
        glText.setScale(3.5f);  //3  2.5
        if (aglAlt >= 1000) glText.draw(t, left + 0.03f * pixM2, top - glText.getCharHeight() / 2);
        if (aglAlt < 10000)
            margin = 0.6f * glText.getCharWidthMax(); // because of the differing sizes
        else
            margin = 1.1f * glText.getCharWidthMax();                    // we have to deal with the margin ourselves

        // draw the hundreds digits smaller
        t = String.format("%03.0f", (float) aglAlt % 1000);
        glText.setScale(2.5f); // was 2.5
        glText.draw(t, left + 0.03f * pixM2 + margin, top - glText.getCharHeight() / 2);
        glText.end();

        {
            mPolyLine.SetColor(foreShade, foreShade, foreShade, 1); //white
            mPolyLine.SetWidth(2);
            float[] vertPoly = {
                    right, top - glText.getCharHeight(), z,
                    right, top + glText.getCharHeight(), z,
                    left, top + glText.getCharHeight(), z,
                    left, top - glText.getCharHeight(), z,
                    right, top - glText.getCharHeight(), z
            };

            mPolyLine.VertexCount = 5;
            mPolyLine.SetVerts(vertPoly);
            mPolyLine.draw(matrix);
        }

    }

    // TODO: use slide to position and get rid of the top/right vars
    //-------------------------------------------------------------------------
    // Altimeter Indicator
    //
    protected void renderFixedALTMarkers(float[] matrix)
    {
        float z = zfloat;
        String t;

        float right = 0;//1.14f * pixM2;
        float left = right - 0.35f * pixM2;
        float apex = left - 0.05f * pixM2;

        // The tapes are positioned left & right of the roll circle, occupying the space based
        // on the vertical dimension, from .6 to 1.0 pixM2.  This makes the basic display
        // square, leaving extra space outside the edges for terrain which can be clipped if required.

        // Altimeter Display

        // Do a dummy glText so that the Heights are correct for the masking box
        glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
        glText.setScale(2.5f);  //was 1.5
        glText.end();

        // Mask over the moving tape for the value display box
        mSquare.SetColor(backShade, backShade, backShade, 1); //black
        mSquare.SetWidth(2);
        {
            float[] squarePoly = {
                    right, -glText.getCharHeight(), z,//+0.1f,
                    right, glText.getCharHeight(), z,//+0.1f,
                    left, glText.getCharHeight(), z,//+0.1f,
                    left, -glText.getCharHeight(), z,//+0.1f
            };
            mSquare.SetVerts(squarePoly);
            mSquare.draw(matrix);
        }

        int mslAlt = Math.round((float) this.MSLValue / 10) * 10;  // round to 10
        // draw the tape text in mixed sizes
        // to clearly show the thousands
        glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
        t = Integer.toString(mslAlt / 1000);
        float margin;

        // draw the thousands digits larger
        glText.setScale(3.5f);  //3  2.5
        if (mslAlt >= 1000) glText.draw(t, left + 0.03f * pixM2, -glText.getCharHeight() / 2);
        if (mslAlt < 10000)
            margin = 0.6f * glText.getCharWidthMax(); // because of the differing sizes
        else
            margin = 1.1f * glText.getCharWidthMax(); // we have to deal with the margin ourselves

        // draw the hundreds digits smaller
        t = String.format("%03.0f", (float) mslAlt % 1000);
        glText.setScale(2.5f); // was 2.5
        glText.draw(t, left + 0.03f * pixM2 + margin, -glText.getCharHeight() / 2);
        glText.end();

        mTriangle.SetColor(backShade, backShade, backShade, 1);  //black
        mTriangle.SetVerts(
                left, glText.getCharHeight() / 2, z,
                apex, 0.0f, z,
                left, -glText.getCharHeight() / 2, z
        );
        mTriangle.draw(mMVPMatrix);

        {
            mPolyLine.SetColor(foreShade, foreShade, foreShade, 1); //white
            mPolyLine.SetWidth(2);
            float[] vertPoly = {
                    right, -glText.getCharHeight(), z,
                    right, glText.getCharHeight(), z,
                    left, glText.getCharHeight(), z,
                    left, glText.getCharHeight() / 2, z,
                    apex, 0.0f, z,
                    left, -glText.getCharHeight() / 2, z,
                    left, -glText.getCharHeight(), z,
                    right, -glText.getCharHeight(), z
            };

            mPolyLine.VertexCount = 8;
            mPolyLine.SetVerts(vertPoly);
            mPolyLine.draw(matrix);
        }
    }

    protected void renderALTMarkers(float[] matrix)
    {
        int i, j;
        float innerTic, midTic, outerTic, z, pixPerUnit, iPix;

        //pixPerUnit = pixM2 / MSLInView; //b2 landscape
        pixPerUnit = pixH2 / MSLInView; //portrait
        z = zfloat;

        innerTic = 0.70f * pixM2;    // inner & outer are relative to the vertical scale line
        midTic = 0.75f * pixM2;
        outerTic = 0.80f * pixM2;

        // The numbers & tics for the tape
        for (i = MSLMaxDisp; i >= MSLMinDisp; i = i - 100) {
            // Ugly hack but is does significantly improve performance.
            if (i > MSLValue + 1.00 * MSLInView) continue;
            if (i < MSLValue - 1.50 * MSLInView) continue;

            iPix = (float) i * pixPerUnit;

            mLine.SetColor(tapeShade, tapeShade, tapeShade, 1);  // grey
            mLine.SetWidth(3);
            mLine.SetVerts(
                    innerTic, iPix, z,
                    outerTic, iPix, z
            );
            mLine.draw(matrix);

            // draw the tape text in mixed sizes
            // to clearly show the thousands
            glText.begin(tapeShade, tapeShade, tapeShade, 1.0f, matrix); // grey
            String t = Integer.toString(i / 1000);
            float margin;

            // draw the thousands digits larger
            glText.setScale(3.0f);
            //glText.setScale(4.0f, 2.5f);
            if (i >= 1000) glText.draw(t, outerTic, iPix - glText.getCharHeight() / 2);

            if (i < 10000)
                margin = 0.6f * glText.getCharWidthMax();  // because of the differing sizes
            else
                margin = 1.1f * glText.getCharWidthMax();            // we have to deal with the margin ourselves

            // draw the hundreds digits smaller
            t = String.format("%03.0f", (float) i % 1000);
            glText.setScale(2.0f); // was 1.5
            glText.draw(t, outerTic + margin, iPix - glText.getCharHeight() / 2);
            glText.end();

            for (j = i + 20; j < i + 90; j = j + 20) {
                iPix = (float) j * pixPerUnit;
                mLine.SetWidth(2);
                mLine.SetVerts(
                        innerTic, iPix, z,
                        midTic, iPix, z
                );
                mLine.draw(matrix);
            }
        }

        // The vertical scale bar
        mLine.SetVerts(
                innerTic, MSLMinDisp, z,
                innerTic, (MSLMaxDisp + 100) * pixPerUnit, z
        );
        mLine.draw(matrix);
    }

    //
    //Set the altimeter - ft
    //
    public void setALT(int feet)
    {
        MSLValue = feet;
        MSLTranslation = MSLValue / MSLInView * pixH2;
    }

    //
    //Set the barometric pressure
    //
    void setBaro(float milliBar)
    {
        baroPressure = milliBar;
    }


    void renderFixedVSIMarkers(float[] matrix)
    {
        int i, j;
        float innerTic, midTic, outerTic, z, pixPerUnit, iPix;

        pixPerUnit = 0.75f * pixM2 / VSIInView;
        z = zfloat;

        innerTic = 1.20f * pixM2;    // inner & outer are relative to the vertical scale line
        midTic = 1.23f * pixM2;
        outerTic = 1.26f * pixM2;

        for (i = 0; i <= VSIInView; i = i + 500) {

            iPix = (float) i * pixPerUnit;

            String t = Float.toString((float) i / 1000);

            mLine.SetColor(0.5f, 0.5f, 0.5f, 1);  // grey
            mLine.SetWidth(2);
            mLine.SetVerts(
                    innerTic, iPix, z,
                    outerTic, iPix, z
            );
            mLine.draw(matrix);

            glText.begin(tapeShade, tapeShade, tapeShade, 1, matrix); // white
            glText.setScale(1.5f); // was 1.2
            glText.draw(t, outerTic + glText.getCharWidthMax() / 2, iPix - glText.getCharHeight() / 2);
            glText.end();

            if (i < VSIInView) {
                for (j = i + 100; j < i + 500; j = j + 100) {
                    iPix = (float) j * pixPerUnit;
                    mLine.SetVerts(
                            innerTic, iPix, z,
                            midTic, iPix, z
                    );
                    mLine.draw(matrix);
                }
            }
        }

        // The vertical scale bar
        mLine.SetColor(tapeShade, tapeShade, tapeShade, 1);  // grey
        mLine.SetWidth(2);
        mLine.SetVerts(
                innerTic, -VSIInView, z,
                innerTic, (+VSIInView + 100) * pixPerUnit, z
        );
        mLine.draw(matrix);
    }


    //-------------------------------------------------------------------------
    // VSI Indicator
    //
    protected void renderVSIMarkers(float[] matrix)
    {
        int i;
        float z, pixPerUnit, innerTic; //, midTic, outerTic;

        pixPerUnit = 1.0f * pixM2 / VSIInView;
        z = zfloat;

        innerTic = 0.64f * pixM2;    // inner & outer are relative to the vertical scale line
        //outerTic = 0.70f * pixM2;
        //midTic = 0.67f * pixM2;


        // VSI box
        for (i = -2; i <= 2; i += 1) {
            mLine.SetColor(0.5f, 0.5f, 0.5f, 1);  // grey
            mLine.SetWidth(4);
            mLine.SetVerts(
                    0.64f * pixM2, i * 1000 * pixPerUnit, z,
                    0.70f * pixM2, i * 1000 * pixPerUnit, z
            );
            mLine.draw(matrix);

            if (i != 0) {
                String s = Integer.toString(Math.abs(i));
                glText.begin(tapeShade, tapeShade, tapeShade, 1.0f, matrix); // light grey
                glText.setScale(3.0f);   //1.75f
                glText.draw(s, innerTic - 1.5f * glText.getLength(s), i * 1000 * pixPerUnit - glText.getCharHeight() / 2);
                glText.end();
            }
        }

        // vertical speed  bar
        mLine.SetColor(0, 0.8f, 0, 1); // green
        mLine.SetWidth(16);
        mLine.SetVerts(
                0.67f * pixM2, 0.0f * pixH2, z,
                0.67f * pixM2, VSIValue * pixPerUnit, z
        );
        mLine.draw(matrix);
    }


    //
    //Set the VSI - ft
    //
    public void setVSI(int fpm)
    {
        VSIValue = fpm;
    }

    // TODO: use slide to position and get rid of the top/right vars
    //-------------------------------------------------------------------------
    // Airspeed Indicator
    //
    protected void renderFixedASIMarkers(float[] matrix)
    {
        float z = zfloat;
        String t;

        //float left = -1.10f * pixM2;
        //float right = -0.80f * pixM2;
        //float apex = -0.75f * pixM2;

        float left = 0;//-1.10f * pixM2;
        float right = left + 0.3f * pixM2;
        float apex = right + 0.05f * pixM2;


        // The tapes are positioned left & right of the roll circle, occupying the space based
        // on the vertical dimension, from .6 to 1.0 pixH2.  This makes the basic display
        // square, leaving extra space outside the edges for terrain which can be clipped if reqd.

        // Do a dummy glText so that the Heights are correct for the masking box
        glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
        glText.setScale(2.5f); // was 1.5
        glText.end();

        // Mask over the moving tape for the value display box
        mSquare.SetColor(backShade, backShade, backShade, 1); //black
        mSquare.SetWidth(2);
        {
            float[] squarePoly = {
                    left, -glText.getCharHeight(), z,
                    left, glText.getCharHeight(), z,
                    right, glText.getCharHeight(), z,
                    right, -glText.getCharHeight(), z,
            };
            mSquare.SetVerts(squarePoly);
            mSquare.draw(matrix);
        }

        mTriangle.SetColor(backShade, backShade, backShade, 1);  //black
        mTriangle.SetVerts(
                right, glText.getCharHeight() / 2, z,
                apex, 0.0f, z,
                right, -glText.getCharHeight() / 2, z
        );
        mTriangle.draw(mMVPMatrix);

        {
            mPolyLine.SetColor(foreShade, foreShade, foreShade, 1); //white
            mPolyLine.SetWidth(2);
            float[] vertPoly = {
                    left, -glText.getCharHeight(), z,
                    left, glText.getCharHeight(), z,
                    right, glText.getCharHeight(), z,
                    right, glText.getCharHeight() / 2, z,
                    apex, 0.0f, z,
                    right, -glText.getCharHeight() / 2, z,
                    right, -glText.getCharHeight(), z,
                    left, -glText.getCharHeight(), z
            };

            mPolyLine.VertexCount = 8;
            mPolyLine.SetVerts(vertPoly);
            mPolyLine.draw(matrix);
        }
        t = Integer.toString(Math.round(IASValue));
        glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix);     // white
        glText.setScale(3.5f);                            // was 2.5
        glText.drawC(t, left + 0.25f * pixM2, glText.getCharHeight() / 2);
        glText.end();
    }


    protected void renderASIMarkers(float[] matrix)
    {
        int i, j;
        float innerTic, midTic, outerTic; //, topTic, botTic;
        float z, pixPerUnit, iPix;

        z = zfloat;
        pixPerUnit = pixH2 / IASInView;

        innerTic = -0.70f * pixM2;    // inner & outer are relative to the vertical scale line
        outerTic = -0.80f * pixM2;
        midTic = -0.77f * pixM2;

        // The numbers & tics for the tape
        for (i = IASMaxDisp; i >= 0; i = i - 10) {
            iPix = (float) i * pixPerUnit;
            String t = Integer.toString(i);

            mLine.SetColor(tapeShade, tapeShade, tapeShade, 1);  // grey
            mLine.SetWidth(2);
            mLine.SetVerts(
                    innerTic, iPix, z,
                    outerTic, iPix, z
            );
            mLine.draw(matrix);

            glText.begin(tapeShade, tapeShade, tapeShade, 1.0f, matrix); // grey
            glText.setScale(2.5f); // was 2
            //glText.setScale(3.2f, 2f);  // screen ratio is 1.6 on Nexus 2 x 1.6 = 3.2
            glText.draw(t, outerTic - 1.5f * glText.getLength(t), iPix - glText.getCharHeight() / 2);
            glText.end();

            for (j = i + 2; j < i + 9; j = j + 2) {
                iPix = (float) j * pixPerUnit;
                mLine.SetVerts(
                        innerTic, iPix, z,
                        midTic, iPix, z
                );
                mLine.draw(matrix);
            }
        }

        // The vertical scale bar
        mLine.SetVerts(
                innerTic, 0, z,  // IASMinDisp - no longer used, set to 0
                innerTic, (IASMaxDisp + 100) * pixPerUnit, z
        );
        mLine.draw(matrix);

        // For monochrome display (displayTerrain false) do not use any color
        if (displayTerrain) {
            //
            // Special Vspeed markers
            //

            // Simplified V Speeds

            //glText.begin( 0.0f, 0.6f, 0.0f, 1.0f, matrix); // Green
            //glText.begin( 0.9f, 0.9f, 0.0f, 1.0f, matrix); // yellow
            //glText.begin( 0.0f, 0.9f, 0.9f, 1.0f, matrix); // cyan
            glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // grey
            glText.setScale(2.0f);    // was 1.5
            glText.draw(" Vx", innerTic, (float) AircraftData.Vx * pixPerUnit); // Vx
            glText.draw(" Vy", innerTic, (float) AircraftData.Vy * pixPerUnit); // Vy
            glText.draw(" Va", innerTic, (float) AircraftData.Va * pixPerUnit); // Va
            glText.end();


            // Tape markings for V speeds
            // Re use midTic ... maybe not such a good idea ...
            midTic = -0.75f * pixM2;                    // Put tape under the tics, w/ VsO-Vfe bar narrower than minor tics
            //midTic = -1.17f * pixM2;	// Put tape under the tics, w/ VsO-Vfe bar narrower than minor tics
            mSquare.SetColor(0, 0.5f, 0, 1);  // dark green arc
            mSquare.SetWidth(1);
            {
                float[] squarePoly = {
                        innerTic, (float) AircraftData.Vs1 * pixPerUnit, z,
                        innerTic, (float) AircraftData.Vno * pixPerUnit, z,
                        midTic, (float) AircraftData.Vno * pixPerUnit, z,
                        midTic, (float) AircraftData.Vs1 * pixPerUnit, z
                };
                mSquare.SetVerts(squarePoly);
                mSquare.draw(matrix);
            }
            mSquare.SetColor(0.5f, 0.5f, 0.5f, 1);  // white arc
            mSquare.SetWidth(1);
            {
                float[] squarePoly = {
                        innerTic, (float) AircraftData.Vs0 * pixPerUnit, z,
                        innerTic, (float) AircraftData.Vfe * pixPerUnit, z,
                        midTic, (float) AircraftData.Vfe * pixPerUnit, z,
                        midTic, (float) AircraftData.Vs0 * pixPerUnit, z
                };
                mSquare.SetVerts(squarePoly);
                mSquare.draw(matrix);
            }

            mSquare.SetColor(0.9f, 0.9f, 0, 1);  // yellow arc
            mSquare.SetWidth(1);
            {
                float[] squarePoly = {
                        innerTic, (float) AircraftData.Vno * pixPerUnit, z,
                        innerTic, (float) AircraftData.Vne * pixPerUnit, z,
                        midTic, (float) AircraftData.Vne * pixPerUnit, z,
                        midTic, (float) AircraftData.Vno * pixPerUnit, z
                };
                mSquare.SetVerts(squarePoly);
                mSquare.draw(matrix);
            }

            // Vne
            mSquare.SetColor(0.9f, 0, 0, 1);  // red
            mSquare.SetWidth(1);
            {
                float[] squarePoly = {
                        innerTic, (float) AircraftData.Vne * pixPerUnit, z,
                        innerTic, (float) (AircraftData.Vne + 1) * pixPerUnit, z,
                        outerTic, (float) (AircraftData.Vne + 1) * pixPerUnit, z,
                        outerTic, (float) AircraftData.Vne * pixPerUnit, z
                };
                mSquare.SetVerts(squarePoly);
                mSquare.draw(matrix);

                float[] squarePoly2 = {
                        innerTic, (float) AircraftData.Vne * pixPerUnit, z,
                        innerTic, (float) (IASMaxDisp + 10) * pixPerUnit, z,
                        midTic, (float) (IASMaxDisp + 10) * pixPerUnit, z,
                        midTic, (float) AircraftData.Vne * pixPerUnit, z
                };
                mSquare.SetVerts(squarePoly2);
                mSquare.draw(matrix);
            }
        }
    }

    //
    //Set the IAS indicator
    //
    public void setASI(float value)
    {
        IASValue = value;
        IASTranslation = IASValue / IASInView * pixH2;
    }

    //-------------------------------------------------------------------------
    // Direction Indicator
    //   Just a simple text box
    //
    protected void renderFixedDIMarkers(float[] matrix)
    {
        float z = zfloat;
        //String t;

        float top = 0.9f * pixH2;

        float left = -0.15f * pixM2;
        float right = 0.15f * pixM2;
        //float apex =   0.00f * pixM2;

        // The tapes are positioned left & right of the roll circle, occupying the space based
        // on the vertical dimension, from .6 to 1.0 pixH2.  This makes the basic display
        // square, leaving extra space outside the edges for terrain which can be clipped if reqd.

        // Do a dummy glText so that the Heights are correct for the masking box
        glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
        glText.setScale(2.5f); // was 1.5
        glText.end();

        // Mask over the moving tape for the value display box
        mSquare.SetColor(backShade, backShade, backShade, 1); //black
        mSquare.SetWidth(2);
        {
            float[] squarePoly = {
                    right, top - glText.getCharHeight(), z,
                    right, top + glText.getCharHeight(), z,
                    left, top + glText.getCharHeight(), z,
                    left, top - glText.getCharHeight(), z,
            };
            mSquare.SetVerts(squarePoly);
            mSquare.draw(matrix);
        }

        {
            mPolyLine.SetColor(foreShade, foreShade, foreShade, 1); //white
            mPolyLine.SetWidth(2);
            float[] vertPoly = {
                    right, top + glText.getCharHeight(), z,
                    left, top + glText.getCharHeight(), z,
                    left, top - glText.getCharHeight(), z,
                    right, top - glText.getCharHeight(), z,
                    right, top + glText.getCharHeight(), z,

                    // for some reason this causes a crash on restart if there are not 8 vertexes
                    // most probably a a bug in PolyLine
                    left, top + glText.getCharHeight(), z,
                    left, top - glText.getCharHeight(), z,
                    right, top - glText.getCharHeight(), z,

            };
            mPolyLine.VertexCount = 8;
            mPolyLine.SetVerts(vertPoly);
            mPolyLine.draw(matrix);
        }
    }


    //---------------------------------------------------------------------------
    // EFIS serviceability ... aka the Red X's
    //
    protected void renderUnserviceableDevice(float[] matrix)
    {
        renderUnserviceableAh(matrix);
        renderUnserviceableDi(matrix);
        renderUnserviceableAlt(matrix);
        renderUnserviceableAsi(matrix);
    }

    protected void renderUnserviceableAh(float[] matrix)
    {
        float z;
        z = zfloat;

        mLine.SetColor(1, 0, 0, 1);  // red
        mLine.SetWidth(20);

        mLine.SetVerts(
                -0.7f * pixM2, 0.8f * pixH2, z,
                0.7f * pixM2, -0.8f * pixH2, z
        );
        mLine.draw(matrix);
        mLine.SetVerts(
                0.7f * pixM2, 0.8f * pixH2, z,
                -0.7f * pixM2, -0.8f * pixH2, z
        );
        mLine.draw(matrix);
    }

    protected void renderUnserviceableDi(float[] matrix)
    {
        float z;
        z = zfloat;

        mLine.SetColor(1, 0, 0, 1);  // red
        mLine.SetWidth(8);

        mLine.SetVerts(
                -0.7f * pixM2, 0.95f * pixH2, z,
                0.7f * pixM2, 0.85f * pixH2, z
        );
        mLine.draw(matrix);
        mLine.SetVerts(
                0.7f * pixM2, 0.95f * pixH2, z,
                -0.7f * pixM2, 0.85f * pixH2, z
        );
        mLine.draw(matrix);
    }


    protected void renderUnserviceableAlt(float[] matrix)
    {
        float z;
        z = zfloat;

        mLine.SetColor(1, 0, 0, 1);  // red
        mLine.SetWidth(8);

        mLine.SetVerts(
                0.7f * pixM2, 0.8f * pixH2, z,
                1.0f * pixM2, -0.8f * pixH2, z
        );
        mLine.draw(matrix);
        mLine.SetVerts(
                1.0f * pixM2, 0.8f * pixH2, z,
                0.7f * pixM2, -0.8f * pixH2, z
        );
        mLine.draw(matrix);
    }

    protected void renderUnserviceableAsi(float[] matrix)
    {
        float z;
        z = zfloat;

        mLine.SetColor(1, 0, 0, 1);  // red
        mLine.SetWidth(8);

        mLine.SetVerts(
                -0.7f * pixM2, 0.8f * pixH2, z,
                -1.0f * pixM2, -0.8f * pixH2, z
        );
        mLine.draw(matrix);
        mLine.SetVerts(
                -1.0f * pixM2, 0.8f * pixH2, z,
                -0.7f * pixM2, -0.8f * pixH2, z
        );
        mLine.draw(matrix);
    }


    // Overall PFD serviceability
    public void setServiceableDevice()
    {
        ServiceableDevice = true;
    }

    public void setUnServiceableDevice()
    {
        ServiceableDevice = false;
    }

    // Artificial Horizon serviceability
    public void setServiceableAh()
    {
        ServiceableAh = true;
    }

    public void setUnServiceableAh()
    {
        ServiceableAh = false;
    }

    // Altimeter serviceability
    public void setServiceableAlt()
    {
        ServiceableAlt = true;
    }

    public void setUnServiceableAlt()
    {
        ServiceableAlt = false;
    }


    // Airspeed Indicator serviceability
    public void setServiceableAsi()
    {
        ServiceableAsi = true;
    }

    public void setUnServiceableAsi()
    {
        ServiceableAsi = false;
    }

    // Direction Indicaotor serviceability
    public void setServiceableDi()
    {
        ServiceableDi = true;
    }

    public void setUnServiceableDi()
    {
        ServiceableDi = false;
    }

    // Display control for FPV
    public void setDisplayFPV(boolean display)
    {
        displayFPV = display;
    }

    // Display control for Airports
    public void setDisplayAirport(boolean display)
    {
        displayAirport = display;
    }

    // Display control for Airspace
    public void setDisplayAirspace(boolean display)
    {
        displayAirspace = display;
    }

    protected void renderHDGValue(float[] matrix)
    {
        int rd = Math.round(DIValue);           // round to nearest integer
        String t = Integer.toString(rd);
        glText.begin(foreShade, foreShade, foreShade, 1, matrix);    // white
        glText.setScale(3.5f);  //was 2.5f
        glText.drawCX(t, 0, 0.9f * pixH2 - glText.getCharHeight() / 2);   // Draw String
        glText.end();
    }

    public void setHeading(float value)
    {
        DIValue = value;
    }


    //-------------------------------------------------------------------------
    // Slip ball
    //
    protected void renderSlipBall(float[] matrix)
    {
        float z;

        z = zfloat;

        float radius = 10 * pixM / 736;
        float x1 = SlipValue;
        float y1 = -0.9f * pixH2;


        // slip box
        mLine.SetColor(foreShade, foreShade, foreShade, 1);
        mLine.SetWidth(4);
        mLine.SetVerts(
                -0.07f * pixM2, y1 - 0.4f * glText.getCharHeight(), z,
                -0.07f * pixM2, y1 + 0.4f * glText.getCharHeight(), z
        );
        mLine.draw(matrix);

        mLine.SetVerts(
                0.07f * pixM2, y1 - 0.4f * glText.getCharHeight(), z,
                0.07f * pixM2, y1 + 0.4f * glText.getCharHeight(), z
        );
        mLine.draw(matrix);


        // slip ball
        mPolygon.SetWidth(1);
        mPolygon.SetColor(foreShade, foreShade, foreShade, 1); //white - always?
        {
            float[] vertPoly = {
                    // some issue with draworder to figure out.
                    x1 + 2.0f * radius, y1 + 0.8f * radius, z,
                    x1 + 0.8f * radius, y1 + 2.0f * radius, z,
                    x1 - 0.8f * radius, y1 + 2.0f * radius, z,
                    x1 - 2.0f * radius, y1 + 0.8f * radius, z,
                    x1 - 2.0f * radius, y1 - 0.8f * radius, z,
                    x1 - 0.8f * radius, y1 - 2.0f * radius, z,
                    x1 + 0.8f * radius, y1 - 2.0f * radius, z,
                    x1 + 2.0f * radius, y1 - 0.8f * radius, z
            };
            mPolygon.VertexCount = 8;
            mPolygon.SetVerts(vertPoly);
            mPolygon.draw(matrix);
        }
    }

    public void setSlip(float value)
    {
        SlipValue = value;
    }


    //-------------------------------------------------------------------------
    // Flight Path Vector
    //
    protected void renderFPV(float[] matrix)
    {
        float z, pixPerDegree;

        //pixPerDegree = pixM2 / pitchInView;
        pixPerDegree = pixM2 / PPD_DIV;
        z = zfloat;

        float radius = 10 * pixM / 736;  //12

        float x1 = fpvX * pixPerDegree;
        float y1 = fpvY * pixPerDegree;

        mPolyLine.SetWidth(3);
        mPolyLine.SetColor(backShade, foreShade, backShade, 1); //green
        {
            float[] vertPoly = {
                    // some issue with draworder to figger out.
                    x1 + 2.0f * radius, y1 + 0.8f * radius, z,
                    x1 + 0.8f * radius, y1 + 2.0f * radius, z,
                    x1 - 0.8f * radius, y1 + 2.0f * radius, z,
                    x1 - 2.0f * radius, y1 + 0.8f * radius, z,
                    x1 - 2.0f * radius, y1 - 0.8f * radius, z,
                    x1 - 0.8f * radius, y1 - 2.0f * radius, z,
                    x1 + 0.8f * radius, y1 - 2.0f * radius, z,
                    x1 + 2.0f * radius, y1 - 0.8f * radius, z,
                    x1 + 2.0f * radius, y1 + 0.8f * radius, z
            };
            mPolyLine.VertexCount = 9;
            mPolyLine.SetVerts(vertPoly);  //crash here
            mPolyLine.draw(matrix);
        }

        mLine.SetWidth(3);
        mLine.SetColor(backShade, foreShade, backShade, 1); //green
        mLine.SetVerts(
                x1 + 2.0f * radius, y1 + 0.0f * radius, z,
                x1 + 4.0f * radius, y1 + 0.0f * radius, z
        );
        mLine.draw(matrix);

        mLine.SetVerts(
                x1 - 2.0f * radius, y1 + 0.0f * radius, z,
                x1 - 4.0f * radius, y1 + 0.0f * radius, z
        );
        mLine.draw(matrix);

        mLine.SetVerts(
                x1 + 0.0f * radius, y1 + 2.0f * radius, z,
                x1 + 0.0f * radius, y1 + 4.0f * radius, z
        );
        mLine.draw(matrix);
    }

    public void setFPV(float x, float y)
    {
        fpvX = x;
        fpvY = y;
    }

    // This may be a differnt name?
    //-------------------------------------------------------------------------
    // Airports / Waypoints
    //

    //
    // Variables specific to render APT
    //
    protected final int MX_APT_SEEK_RNG = 99;
    protected int MX_NR_APT = 10;
    protected int AptSeekRange = 20; // start of with 20nm
    protected int Aptscounter = 0;
    protected int nrAptsFound;
    protected int nrAirspaceFound;
    protected void renderAPT(float[] matrix)
    {
    }

    //-------------------------------------------------------------------------
    // Synthetic Vision
    //
    private void __getColor(short c)
    {
        float red;
        float blue;
        float green;

        float r = 600;   //600;
        float r2 = r * 2;

        red = 0.0f;
        blue = 0.0f;
        green = (float) c / r;
        if (green > 1) {
            green = 1;
            red = (c - r) / r;
            if (red > 1) {
                red = 1;
                blue = (c - r2) / r;
                if (blue > 1) {
                    blue = 1;
                }
            }
        }
    }

    //-------------------------------------------------------------------------
    // DemGTOPO30 Sky.
    //
    protected void renderDEMSky(float[] matrix)
    {
        float pixPitchViewMultiplier, pixOverWidth, z;

        pixPitchViewMultiplier = 90.0f / pitchInView * pixH;
        pixOverWidth = pixW2 * 1.80f; //1.42f;
        z = zfloat;

        // Sky - simple
        // max: -0.05 to 180 pitch
        float overlap;  //= 0.05f;  // 0 - 0.05
        if (AGLValue > 0) overlap = 0.1f; //0.05f;
        else overlap = 0.0f;

        //mSquare.SetColor(0f, 0f, 0.9f, 1); //blue
        // // TODO: 2017-10-31 make parameterised
        mSquare.SetColor(gamma * 0.10f, gamma * 0.20f, gamma * 0.30f, 1); //blue

        mSquare.SetWidth(1);
        {
            float[] squarePoly = {
                    -pixOverWidth, -overlap * pixPitchViewMultiplier, z,
                    pixOverWidth, -overlap * pixPitchViewMultiplier, z,
                    pixOverWidth, 2.0f * pixPitchViewMultiplier, z,
                    -pixOverWidth, 2.0f * pixPitchViewMultiplier, z
            };
            mSquare.SetVerts(squarePoly);
            mSquare.draw(matrix);
        }
    }


    //-------------------------------------------------------------------------
    // Render the Digital Elevation Model (DEM).
    //
    // This is the meat and potatoes of the synthetic vision implementation
    // The loops are very performance intensive, therefore all the hardcoded
    // magic numbers
    //
    //protected void renderDEMTerrain(float[] matrix)
    //{
    //}


    // This is only good for debugging
    // It is very slow
    public void renderDEMBuffer(float[] matrix)
    {
        float z = zfloat;
        int x; // = 0;
        int y; // = 0;

        int maxx = DemGTOPO30.BUFX;
        int maxy = DemGTOPO30.BUFY;

        for (y = 0; y < maxy /*BUFY*/; y++) {
            for (x = 0; x < maxx /*BUFX*/; x++) {
                DemColor color = DemGTOPO30.getColor(DemGTOPO30.buff[x][y]);
                mLine.SetColor(color.red, color.green, color.blue, 1);  // rgb
                mLine.SetWidth(1);
                mLine.SetVerts(
                        x - maxx / 2, -y + pixH2 / 10, z,
                        x - maxx / 2 + 1, -y + pixH2 / 10, z
                );
                mLine.draw(matrix);
            }
        }
    }


    //-------------------------------------------------------------------------
    // Highway in the Sky (HITS)
    //
    protected void renderHITS(float[] matrix)
    {
        float z, pixPerDegree, x1, y1;
        float radius; // = pixM2 / 2; //5;
        float dme;
        float hitRelBrg;
        float obs;
        final float altMult = 10;

        pixPerDegree = pixM / pitchInView;
        z = zfloat;

        obs = mObsValue;

        for (float i = 0; i < mSelWptDme; i++) {
            //if (i < mSelWptDme - 6) continue;  // only show 6 gates
            // maybe rework this to only when we are "in" the HITS

            float hitLat = mWptSelLat + i / 60 * (float) Math.cos(Math.toRadians(obs - 180));  // this is not right, it must be the OBS setting
            float hitLon = mWptSelLon + i / 60 * (float) Math.sin(Math.toRadians(obs - 180));  // this is not right, it must be the OBS setting

            dme = UNavigation.calcDme(LatValue, LonValue, hitLat, hitLon);
            hitRelBrg = UNavigation.calcRelBrg(LatValue, LonValue, hitLat, hitLon, DIValue);  // the relative bearing to the hitpoint
            radius = 0.1f * pixM2 / dme;
            float skew = (float) Math.cos(Math.toRadians(hitRelBrg));    // to misquote William Shakespeare, this may be gilding the lily?

            x1 = hitRelBrg * pixPerDegree;
            y1 = (float) (-Math.toDegrees(UTrig.fastArcTan2(MSLValue - mAltSelValue, Unit.NauticalMile.toFeet(dme))) * pixPerDegree * altMult);

            // De-clutter the gates
            //
            //if ((Math.abs(x1) < 50) && (Math.abs(y1) < 50)) {
            //    if (i < mSelWptDme - 6) continue;  // only show 6 gates
            //}
            // This appears to be a better compromise.
            // Limit the gates to 6 only when close to the altitude
            if ((Math.abs(y1) < 5)) {
                if (i < mSelWptDme - 6) continue;  // only show 6 gates
            }


            mPolyLine.SetWidth(3);
            //mPolyLine.SetColor(0.8f, 0.4f, 0.8f, 1); // darker purple'ish
            mPolyLine.SetColor(0.0f, 0.8f, 0.8f, 1);   // darker cyan
            {
                float[] vertPoly = {
                        x1 - 3.0f * radius * skew, y1 - 2.0f * radius, z,
                        x1 + 3.0f * radius * skew, y1 - 2.0f * radius, z,
                        x1 + 3.0f * radius * skew, y1 + 2.0f * radius, z,
                        x1 - 3.0f * radius * skew, y1 + 2.0f * radius, z,
                        x1 - 3.0f * radius * skew, y1 - 2.0f * radius, z
                };
                mPolyLine.VertexCount = 5;
                mPolyLine.SetVerts(vertPoly);
                mPolyLine.draw(matrix);
            }
        }
    }


    //-------------------------------------------------------------------------
    // Radio Alitimeter (agl)
    //
    // There are two events that can change agl: setLatLon and setAlt
    // This function is called directly by them.
    //
    public void setAGL(int agl)
    {
        AGLValue = agl;
        if ((AGLValue <= 0) && (IASValue < AircraftData.Vx)) {        // was Vs0
            // Handle taxi as a special case
            MSLValue = 1 + (int) Unit.Meter.toFeet(DemGTOPO30.getElev(LatValue, LonValue));        // Add 1 extra ft to esure we "above the ground"
            AGLValue = 1;                                 // Just good form, it will get changed on the next update
        }

        /*
        if (DemGTOPO30.demDataValid) AGLValue = MSLValue - (int) (3.28084f * DemGTOPO30.getElev(LatValue, LonValue));
        else AGLValue = 0;

        if ((AGLValue < 0) && (IASValue < AircraftData.Vx)) {        // was Vs0
            // Handle taxi as a special case
            MSLValue = MSLValue + (-AGLValue) + 1;        // Add 1 extra ft to esure we "above the ground"
            AGLValue = 1;                                 // Just good form, it will get changed on the next update
        }

        if (AGLValue < 0) AGLValue = 0;                   // Clamp negative AGL
        */
    }

    //-------------------------------------------------------------------------
    // Geographic coordinates (lat, lon)
    // in decimal degrees
    //
    public void setLatLon(float lat, float lon)
    {
        LatValue = lat;
        LonValue = lon;
    }

    //-------------------------------------------------------------------------
    // Turn Indicator
    //
    protected void renderTurnMarkers(float[] matrix)
    {
        final float STD_RATE = 0.0524f; // = rate 1 = 3deg/s
        float z;

        z = zfloat;

        // rate of turn box
        mLine.SetColor(tapeShade, tapeShade, tapeShade, 1);  // grey
        mLine.SetWidth(4);
        mLine.SetVerts(
                -STD_RATE * 4 * pixM2, -0.8f * pixH2 - 10, z,
                -STD_RATE * 4 * pixM2, -0.8f * pixH2 + 10, z
        );
        mLine.draw(matrix);

        mLine.SetVerts(
                STD_RATE * 4 * pixM2, -0.8f * pixH2 - 10, z,
                STD_RATE * 4 * pixM2, -0.8f * pixH2 + 10, z
        );
        mLine.draw(matrix);

        // rate of turn bar
        mLine.SetColor(0, 0.8f, 0, 1); // green
        mLine.SetWidth(15);
        mLine.SetVerts(
                0, -0.8f * pixH2, z,
                ROTValue * 4 * pixM2, -0.8f * pixH2, z
        );
        mLine.draw(matrix);
    }


    public void setTurn(float rot)
    {
        ROTValue = rot;
    }

    //
    // Percentage battery remaining
    //
    protected void renderBatteryPct(float[] matrix)
    {
        String s = String.format("BAT %3.0f", BatteryPct * 100) + "%";
        if (BatteryPct > 0.1) glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
        else glText.begin(0.0f, foreShade, foreShade, 1.0f, matrix); // cyan

        glText.setScale(2.0f);                            //
        glText.draw(s, -0.97f * pixW2, (lineAncillaryDetails - 0.2f) * pixM2 - glText.getCharHeight() / 2); // as part of the ancillaray group
        glText.end();
    }

    public void setBatteryPct(float value)
    {
        BatteryPct = value;
    }

    protected void renderGForceValue(float[] matrix)
    {
        //float z, pixPerUnit;
        //pixPerUnit = pixH2 / DIInView;
        //z = zfloat;

        String t = String.format("G %03.1f", GForceValue);
        glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
        glText.setScale(3.0f);                            //
        glText.draw(t, -0.97f * pixW2, -0.9f * pixH2 - glText.getCharHeight() / 2);
        glText.end();
    }

    public void setGForce(float value)
    {
        GForceValue = value;
    }

    protected String mAutoWpt = "YSEN";

    public void setAutoWptValue(String wpt)
    {
        mAutoWpt = wpt;
    }

    private float mAutoWptBrg;

    protected void setAutoWptBrg(float brg)
    {
        mAutoWptBrg = brg;
    }

    private float mSelWptBrg;           // Selected waypoint Bearing
    protected float mSelWptRlb;           // Selected waypoint Relative bearing
    protected float mSelWptDme;           // Selected waypoint Dme distance (nm)

    private void setSelWptBrg(float brg)
    {
        mSelWptBrg = brg;
    }

    private void setSelWptDme(float dme)
    {
        mSelWptDme = dme;
    }

    private void setSelWptRelBrg(float rlb)
    {
        mSelWptRlb = rlb;
    }

    //
    // Display all the relevant auto wpt information with
    // A combo function to replace the individual ones
    //
    protected float lineAutoWptDetails;  // Auto Wpt - Set in onSurfaceChanged

    protected void renderAutoWptDetails(float[] matrix)
    {
        String s;

        glText.begin(theta * foreShade, theta * foreShade, theta * backShade, 1, matrix); // light yellow
        glText.setScale(2.0f);

        s = String.format("%s", mAutoWpt);
        glText.draw(s, -0.97f * pixW2, (lineAutoWptDetails - 0.0f) * pixM2 - glText.getCharHeight() / 2);

        s = String.format("BRG  %03.0f", mAutoWptBrg);
        glText.draw(s, -0.97f * pixW2, (lineAutoWptDetails - 0.1f) * pixM2 - glText.getCharHeight() / 2);

        s = String.format("DME %03.1f", mAutoWptDme);
        glText.draw(s, -0.97f * pixW2, (lineAutoWptDetails - 0.2f) * pixM2 - glText.getCharHeight() / 2);
        glText.end();
    }

    //-------------------------------------------------------------------------
    // Display all the relevant ancillary device information with
    // A combo function to replace the individual ones
    //
    protected float lineAncillaryDetails;  // Ancillary Details - Set in onSurfaceChanged

    protected void renderAncillaryDetails(float[] matrix)
    {
        String s;

        glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
        glText.setScale(2.0f);

        s = mGpsStatus; //String.format("%c%03.2f %c%03.2f",  (gps_lat < 0)?  'S':'N' , Math.abs(gps_lat), (gps_lon < 0)? 'W':'E' , Math.abs(gps_lon));
        glText.draw(s, -0.97f * pixW2, (lineAncillaryDetails - 0.3f) * pixM2 - glText.getCharHeight() / 2);

        s = String.format("RNG %d   #AP %d", AptSeekRange, nrAptsFound);
        glText.draw(s, -0.97f * pixW2, (lineAncillaryDetails - 0.4f) * pixM2 - glText.getCharHeight() / 2);

        ///*
        s = String.format("%c%03.2f %c%03.2f", (LatValue < 0) ? 'S' : 'N', Math.abs(LatValue), (LonValue < 0) ? 'W' : 'E', Math.abs(LonValue));
        glText.draw(s, -0.97f * pixW2, (lineAncillaryDetails - 0.5f) * pixM2 - glText.getCharHeight() / 2);
        //*/

        glText.end();
    }


    private String mGpsStatus; // = "GPS: 10 / 11";

    public void setGpsStatus(String gpsstatus)
    {
        mGpsStatus = gpsstatus;
    }


    public String mWptSelName = "YSEN";
    public String mWptSelComment = "Serpentine";
    public float mWptSelLat = -32.395000f;
    public float mWptSelLon = 115.871000f;
    public float mAltSelValue = 0;
    public String mAltSelName = "00000";
    protected float leftC = 0.6f;   // Selected Wpt
    protected float lineC;          // Selected Wpt - Set in onSurfaceChanged
    protected float selWptDec;      // = 0.90f * pixH2;
    protected float selWptInc;      // = 0.74f * pixH2;
    public float mObsValue;

    protected float spinnerStep = 0.10f;    // spacing between the spinner buttons
    protected float spinnerTextScale = 1;
    public boolean fatFingerActive = false;

    protected void renderSelWptValue(float[] matrix)
    {
        float z = zfloat;
        float size = spinnerStep * 0.2f; // 0.02f;

        // Draw the selecting triangle spinner buttons
        mTriangle.SetColor(foreShade, foreShade, foreShade, 1);  // gray
        for (int i = 0; i < 4; i++) {
            //float xPos = (leftC + (float) i / 10f);
            //float xPos = (leftC + i*spinnerStep);
            float xPos = (leftC + i * spinnerStep);

            mTriangle.SetVerts((xPos - size) * pixW2, selWptDec, z,  //0.02
                    (xPos + size) * pixW2, selWptDec, z,
                    (xPos + 0) * pixW2, selWptDec + 2 * size * pixM2, z);
            mTriangle.draw(matrix);

            mTriangle.SetVerts((xPos - size) * pixW2, selWptInc, z,  //0.02
                    (xPos + size) * pixW2, selWptInc, z,
                    (xPos + 0) * pixW2, selWptInc - 2 * size * pixM2, z);
            mTriangle.draw(matrix);

            // Draw the individual select characters
            if (mWptSelName != null) {
                glText.begin(foreShade, tapeShade, foreShade, 1.0f, matrix); //
                glText.setScale(3 * spinnerTextScale); //3f
                String s = String.format("%c", mWptSelName.charAt(i));
                glText.drawCX(s, xPos * pixW2, ((selWptInc + selWptDec) / 2) - (glText.getCharHeight() / 2));
                glText.end();
            }
        }

        // Calculate the relative bearing to the selected wpt
        float dme = 6080 * UNavigation.calcDme(LatValue, LonValue, mWptSelLat, mWptSelLon); // in ft
        float relBrg = UNavigation.calcRelBrg(LatValue, LonValue, mWptSelLat, mWptSelLon, DIValue);

        // Calculate how many degrees of pitch to command
        final float MAX_COMMAND = 15; // Garmin spec 15 deg pitch and 30 deg roll
        float deltaAlt = mAltSelValue - MSLValue;
        float commandPitch;
        if (deltaAlt > 0) commandPitch = (IASValue - AircraftData.Vy) / 5 * (deltaAlt / 1000);
        else commandPitch = (IASValue) / 5 * (deltaAlt / 1000);

        if (commandPitch > MAX_COMMAND) commandPitch = MAX_COMMAND;
        if (commandPitch < -MAX_COMMAND) commandPitch = -MAX_COMMAND;
        // if (IASValue < Vs0) commandPitch = -MAX_COMMAND; // Maybe handle a stall?

        // update the flight director data
        float commandRoll = relBrg;
        if (commandRoll > 30) commandRoll = 30;   //
        if (commandRoll < -30) commandRoll = -30;  //
        setFlightDirector(displayFlightDirector, commandPitch, commandRoll);

        // BRG
        float absBrg = UNavigation.calcAbsBrg(LatValue, LonValue, mWptSelLat, mWptSelLon);

        // Setting data in this renderer does not make much logical sense. This could be re-factored
        // Perhaps introduce a new function to explicitly handle "navigation"?
        setSelWptBrg(absBrg);
        setSelWptDme(dme / 6080);
        setSelWptRelBrg(relBrg);
    }

    private void dimScreen(float[] matrix, float alpha)
    {
        float z = zfloat;
        // Mask over the PFD for the input area
        mSquare.SetColor(backShade, backShade, backShade, alpha); //black xper .. 0.75f
        mSquare.SetWidth(2);
        {
            float[] squarePoly = {
                    -pixW2 + 5, +pixH2 - 5, z,
                    -pixW2 + 5, -pixH2 + 5, z,
                    +pixW2 - 5, -pixH2 + 5, z,
                    +pixW2 - 5, +pixH2 - 5, z,
            };
            mSquare.SetVerts(squarePoly);
            mSquare.draw(matrix);
        }
    }

    protected void renderSelWptDetails(float[] matrix)
    {
        float z = zfloat;
        String s;

        // This may need to be in a function
        if (fatFingerActive) {
            dimScreen(matrix, 0.75f);
        }

        //glText.begin(0.99f, 0.5f, 0.99f, 1, matrix); // purple -same as needle
        glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); //white
        // Name
        glText.setScale(2.1f * spinnerTextScale);
        s = mWptSelComment;
        glText.draw(s, leftC * pixW2, lineC * pixH2 - 0.5f * glText.getCharHeight());

        // BRG
        s = String.format("BRG  %03.0f", mSelWptBrg);
        glText.setScale(2.5f * spinnerTextScale);
        glText.draw(s, leftC * pixW2, lineC * pixH2 - 1.5f * glText.getCharHeight());

        // DME
        s = String.format("DME %03.1f", mSelWptDme);  // in nm
        glText.setScale(2.5f * spinnerTextScale);
        glText.draw(s, leftC * pixW2, lineC * pixH2 - 2.5f * glText.getCharHeight());
        glText.end();


        /* // Guide lines for fatfinger ... ?
        mLine.SetColor(0.9f, 0.9f, 0.9f, 0); //white
        mLine.SetVerts(leftC * pixW2 - 5, lineC * pixH2 + 0.75f*glText.getCharHeight(), z,
                               pixW2 - 5, lineC * pixH2 + 0.75f*glText.getCharHeight(), z);
        mLine.draw(matrix);
        mLine.SetVerts(leftC * pixW2 - 5, lineC * pixH2 - 2.70f*glText.getCharHeight(), z,
                               pixW2 - 5, lineC * pixH2 - 2.70f*glText.getCharHeight(), z);
        mLine.draw(matrix); */
    }


    protected float selAltInc; // = -0.90f * pixH2;
    protected float selAltDec; // = -0.74f * pixH2;

    protected void renderSelAltValue(float[] matrix)
    {
        float z;
        z = zfloat;
        float size = spinnerStep * 0.2f; // 0.02f;

        // Draw the selecting triangle spinner buttons
        mTriangle.SetColor(foreShade, foreShade, foreShade, 1);  // gray
        for (int i = 0; i < 3; i++) {
            //float xPos = (leftC + (float) i / 10f);
            float xPos = (leftC + i * spinnerStep);


            mTriangle.SetVerts((xPos - size) * pixW2, selAltDec, z,  //0.02
                    (xPos + size) * pixW2, selAltDec, z,
                    (xPos + 0.00f) * pixW2, selAltDec + 2 * size * pixM2, z);
            mTriangle.draw(matrix);

            mTriangle.SetVerts((xPos - size) * pixW2, selAltInc, z,  //0.02
                    (xPos + size) * pixW2, selAltInc, z,
                    (xPos + 0.00f) * pixW2, selAltInc - 2 * size * pixM2, z);
            mTriangle.draw(matrix);

            // Draw the individual select characters
            if (mAltSelName != null) {
                glText.begin(foreShade, tapeShade, foreShade, 1, matrix); //
                glText.setScale(3 * spinnerTextScale); //3f
                String s = String.format("%c", mAltSelName.charAt(i));
                glText.drawCX(s, xPos * pixW2, ((selAltInc + selAltDec) / 2) - glText.getCharHeight() / 2);
                glText.end();
            }
        }

        float xPos = (leftC + 2.6f / 10f);
        glText.begin(foreShade, tapeShade, foreShade, 1, matrix); //
        glText.setScale(2.2f);
        //String s = "X100 ft";
        String s = "F L";
        glText.draw(s, xPos * pixW2, -0.83f * pixH2 - glText.getCharHeight() / 2);
        glText.end();
    }

    public void setThemeDark()
    {
        tapeShade = 0.60f; // grey
        foreShade = 0.99f; // white
        backShade = 0.01f; // black
        gamma = 1;
        theta = 1;
        DemGTOPO30.setGamma(gamma);
    }

    public void setThemeLight()
    {
        tapeShade = 0.10f; // grey
        foreShade = 0.01f; // black
        backShade = 0.99f; // white
        gamma = 4.0f;//3.3f; //3
        theta = 0.6f;
        DemGTOPO30.setGamma(gamma);
    }

    //---------------------------------------------------------------------------
    // Handle the tap events
    //
    public void setActionDown(float x, float y)
    {
        mX = (x / pixW - 0.5f) * 2;
        mY = -(y / pixH - 0.5f) * 2;

        int pos = -1; // set to invalid / no selection
        int inc = 0;  //initialise to 0, also acts as a flag
        int ina = 0;  //initialise to 0, also acts as a flag
        char[] wpt = mWptSelName.toCharArray();
        char[] alt = mAltSelName.toCharArray();

        // fat finger mode
        if ((Math.abs(mY - lineC + 0.1f) < 0.105) && (mX > leftC)) {
            fatFingerActive = !fatFingerActive;
            setSpinnerParams();
        }

        // Determine if we are counting up or down?
        if (Math.abs(mY - selWptDec / pixH2) < 0.10) inc = -1;
        else if (Math.abs(mY - selWptInc / pixH2) < 0.10) inc = +1;

            // Determine if we are counting up or down?
            // altitude number
        else if (Math.abs(mY - selAltDec / pixH2) < 0.10) ina = -1;
        else if (Math.abs(mY - selAltInc / pixH2) < 0.10) ina = +1;

        // Determine which digit is changing
        for (int i = 0; i < 4; i++) {
            //float xPos = (leftC + (float) i / 10f);
            float xPos = (leftC + i * spinnerStep);

            if (Math.abs(mX - xPos) < spinnerStep / 2) {       //0.6, 0.7, 0.8, 0.9
                pos = i;
                break;
            }
        }

        //
        // Increment the appropriate digit if we got a valid tap (pos != -1)
        //
        if (pos > -1) {
            if (ina != 0) {
                alt[pos] += ina;
                if (alt[pos] < '0') alt[pos] = '9';
                if (alt[pos] > '9') alt[pos] = '0';
                if (alt[0] > '2') alt[pos] = '0';
                mAltSelName = String.valueOf(alt);
                mAltSelValue = Float.parseFloat(mAltSelName);
            }

            if (inc != 0) {
                wpt[pos] += inc;
                if (wpt[pos] < 'A') wpt[pos] = 'Z';
                if (wpt[pos] > 'Z') wpt[pos] = 'A';

                Iterator<Apt> it = Gpx.aptList.iterator();

                while (it.hasNext()) {
                    Apt currApt = it.next();

                    // Look for a perfect match
                    if (currApt.name.equals(String.valueOf(wpt))) {
                        mWptSelName = currApt.name;
                        mWptSelComment = currApt.cmt;
                        mWptSelLat = currApt.lat;
                        mWptSelLon = currApt.lon;
                        break;
                    }
                    // Look for a partial match
                    else if ((pos < 3) && currApt.name.startsWith(String.valueOf(wpt).substring(0, pos + 1))) {
                        // We found a partial match, fill in the rest for a first guess
                        for (int i = pos; i < 4; i++) wpt[i] = currApt.name.charAt(i);
                        mWptSelName = currApt.name;
                        mWptSelComment = currApt.cmt;
                        mWptSelLat = currApt.lat;
                        mWptSelLon = currApt.lon;
                        break;
                    }
                    // No match at all
                    else {
                        mWptSelName = String.valueOf(wpt);
                        mWptSelComment = " ";
                        mWptSelLat = 0;
                        mWptSelLon = 0;
                    }
                }
                // The selected waypoint has changed
                // Update the OBS as well
                mObsValue = UNavigation.calcAbsBrg(LatValue, LonValue, mWptSelLat, mWptSelLon);
            }
        }
    }


    //-------------------------------------------------------------------------
    //
    // Auto Waypoint handlers
    //
    //-------------------------------------------------------------------------
    private float mAutoWptDme;
    private float mAutoWptRlb;

    public void setAutoWptDme(float dme)
    {
        mAutoWptDme = dme;
    }

    private void renderAutoWptRlb(float[] matrix)
    {
        String t = String.format("RLB  %03.0f", mAutoWptRlb);
        glText.begin(foreShade, foreShade, foreShade, 1, matrix); // white
        glText.setScale(2.0f);                            //
        glText.draw(t, -0.97f * pixW2, -0.7f * pixM2 - glText.getCharHeight() / 2);            // Draw  String
        glText.end();
    }

    protected void setAutoWptRelBrg(float rlb)
    {
        mAutoWptRlb = rlb;
    }

    public void setPrefs(prefs_t pref, boolean value)
    {
        switch (pref) {
            case DEM:
                displayDEM = value;
                break;
            case TERRAIN:
                displayTerrain = value;
                break;
            case TAPE:
                displayTape = value;
                break;
            case MIRROR:
                displayMirror = value;
                break;
            case INFO_PAGE:
                displayInfoPage = value;
                break;
            case FLIGHT_DIRECTOR:
                displayFlightDirector = value;
                break;
            case REMOTE_INDICATOR:
                displayRMI = value;
                break;
            case HITS:
                displayHITS = value;
                break;
            case AIRPORT:
                displayAirport = value;
                break;
            case AIRSPACE:
                displayAirspace = value;
                break;
        }
    }

    //-----------------------------------------------------------------------------
    //
    //  Remote Indicator
    //
    //-----------------------------------------------------------------------------

    protected float roseScale = 0.34f; //0.30f; //0.33f; //0.5f

    protected void renderFixedCompassMarkers(float[] matrix)
    {
        //float tapeShade = 0.6f;
        int i;
        float z, sinI, cosI;
        float roseRadius = roseScale * pixM2;

        z = zfloat;

        mLine.SetWidth(2);  //3
        mLine.SetColor(foreShade, foreShade, foreShade, 1);
        for (i = 0; i <= 315; i = i + 45) {

            if (i % 90 == 0) mLine.SetWidth(4);
            else mLine.SetWidth(2);

            sinI = UTrig.isin((450 - i) % 360);
            cosI = UTrig.icos((450 - i) % 360);
            mLine.SetVerts(
                    1.005f * roseRadius * cosI, 1.005f * roseRadius * sinI, z,
                    1.120f * roseRadius * cosI, 1.120f * roseRadius * sinI, z
            );
            mLine.draw(matrix);
        }

        // Apex marker
        //float pixPerDegree;
        //pixPerDegree = pixM2 / PPD_DIV;

        mTriangle.SetColor(foreShade, foreShade, foreShade, 1);
        mTriangle.SetVerts(
                0.035f * pixM2, 1.120f * roseRadius, z,
                -0.035f * pixM2, 1.120f * roseRadius, z,
                0.0f, 1.00f * roseRadius, z);
        mTriangle.draw(matrix);


    }

    protected void renderCompassRose(float[] matrix)
    {
        //float tapeShade = 0.6f;
        int i, j;
        float z, sinI, cosI;
        String t;
        float roseRadius = roseScale * pixM2;

        z = zfloat;

        mLine.SetWidth(2);  //3
        mLine.SetColor(tapeShade, tapeShade, tapeShade, 1);  // grey

        // The rose degree tics
        for (i = 0; i <= 330; i = i + 30) {
            sinI = UTrig.isin((450 - i) % 360);
            cosI = UTrig.icos((450 - i) % 360);
            mLine.SetVerts(
                    0.9f * roseRadius * cosI, 0.9f * roseRadius * sinI, z,
                    1.0f * roseRadius * cosI, 1.0f * roseRadius * sinI, z
            );
            mLine.draw(matrix);

            glText.begin(tapeShade, tapeShade, tapeShade, 1.0f, matrix); // grey
            glText.setScale(1.5f);
            switch (i) {
                case 0:
                    t = "N";
                    glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
                    glText.setScale(2.0f);
                    break;
                case 30:
                    t = "3";
                    break;
                case 60:
                    t = "6";
                    break;
                case 90:
                    t = "E";
                    glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
                    glText.setScale(1.5f);
                    break;
                case 120:
                    t = "12";
                    break;
                case 150:
                    t = "15";
                    break;
                case 180:
                    t = "S";
                    glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
                    glText.setScale(1.5f);
                    break;
                case 220:
                    t = "21";
                    break;
                case 240:
                    t = "24";
                    break;
                case 270:
                    t = "W";
                    glText.begin(foreShade, foreShade, foreShade, 1.0f, matrix); // white
                    glText.setScale(1.5f);
                    break;
                case 300:
                    t = "30";
                    break;
                case 330:
                    t = "33";
                    break;
                default:
                    t = "";
                    break;
            }

            //glText.begin( tapeShade, tapeShade, tapeShade, 1.0f, matrix ); // white
            //glText.setScale(1.5f); // seems to have a weird effect here?
            glText.drawC(t, 0.75f * roseRadius * cosI, 0.75f * roseRadius * sinI, -i); // angleDeg=90-i, Use 360-DIValue for vertical text
            glText.end();
            for (j = 10; j <= 20; j = j + 10) {
                sinI = UTrig.isin((i + j));
                cosI = UTrig.icos((i + j));
                mLine.SetVerts(
                        0.93f * roseRadius * cosI, 0.93f * roseRadius * sinI, z,
                        1.00f * roseRadius * cosI, 1.00f * roseRadius * sinI, z
                );
                mLine.draw(matrix);
            }
            for (j = 5; j <= 25; j = j + 10) {
                sinI = UTrig.isin((i + j));
                cosI = UTrig.icos((i + j));
                mLine.SetVerts(
                        0.96f * roseRadius * cosI, 0.96f * roseRadius * sinI, z,
                        1.00f * roseRadius * cosI, 1.00f * roseRadius * sinI, z
                );
                mLine.draw(matrix);
            }
        }
    }

    //-------------------------------------------------------------------------
    // Render the two RMI needles
    //
    protected void renderBearing(float[] matrix)
    {
        float z, sinI, cosI, _sinI, _cosI;
        float roseRadius = roseScale * pixM2;

        z = zfloat;

        //
        // Bearing to Automatic Waypoint
        //
        mLine.SetWidth(5); //3);
        mLine.SetColor(theta * foreShade, theta * foreShade, theta * backShade, 1);  // needle yellow

        sinI = 0.9f * UTrig.isin(90 - (int) mAutoWptBrg);
        cosI = 0.9f * UTrig.icos(90 - (int) mAutoWptBrg);
        _sinI = 0.5f * UTrig.isin(90 - (int) mAutoWptBrg);
        _cosI = 0.5f * UTrig.icos(90 - (int) mAutoWptBrg);

        //tail
        mLine.SetVerts(
                -roseRadius * _cosI, -roseRadius * _sinI, z,
                -roseRadius * cosI, -roseRadius * sinI, z
        );
        mLine.draw(matrix);
        // head
        // point L
        _sinI = 0.80f * UTrig.isin(90 - (int) mAutoWptBrg + 6);
        _cosI = 0.80f * UTrig.icos(90 - (int) mAutoWptBrg + 6);
        mLine.SetVerts(
                roseRadius * _cosI, roseRadius * _sinI, z,
                roseRadius * cosI, roseRadius * sinI, z
        );
        mLine.draw(matrix);

        // parallel
        float parr = 0.40f;

        float __sinI = parr * UTrig.isin(90 - (int) mAutoWptBrg + 12);
        float __cosI = parr * UTrig.icos(90 - (int) mAutoWptBrg + 12);
        mLine.SetVerts(
                roseRadius * _cosI, roseRadius * _sinI, z,
                roseRadius * __cosI, roseRadius * __sinI, z
        );
        mLine.draw(matrix);

        // point R
        _sinI = 0.80f * UTrig.isin(90 - (int) mAutoWptBrg - 5);
        _cosI = 0.80f * UTrig.icos(90 - (int) mAutoWptBrg - 5);
        mLine.SetVerts(
                roseRadius * _cosI, roseRadius * _sinI, z,
                roseRadius * cosI, roseRadius * sinI, z
        );
        mLine.draw(matrix);

        // parallel
        __sinI = parr * UTrig.isin(90 - (int) mAutoWptBrg - 12);
        __cosI = parr * UTrig.icos(90 - (int) mAutoWptBrg - 12);
        mLine.SetVerts(
                roseRadius * _cosI, roseRadius * _sinI, z,
                roseRadius * __cosI, roseRadius * __sinI, z
        );
        mLine.draw(matrix);

        //
        // Bearing to Selected Waypoint
        //
        mLine.SetWidth(8); //6
        //mLine.SetColor(0, 0.7f, 0, 1);  // green
        mLine.SetColor(theta * 1, theta * 0.5f, theta * 1, 1); // purple'ish

        sinI = 0.9f * UTrig.isin(90 - (int) mSelWptBrg);
        cosI = 0.9f * UTrig.icos(90 - (int) mSelWptBrg);
        _sinI = 0.5f * UTrig.isin(90 - (int) mSelWptBrg);
        _cosI = 0.5f * UTrig.icos(90 - (int) mSelWptBrg);

        //tail
        mLine.SetVerts(
                -roseRadius * _cosI, -roseRadius * _sinI, z,
                -roseRadius * cosI, -roseRadius * sinI, z
        );
        mLine.draw(matrix);
        //head
        mLine.SetVerts(
                roseRadius * _cosI, roseRadius * _sinI, z,
                roseRadius * cosI, roseRadius * sinI, z
        );
        mLine.draw(matrix);

        mLine.SetWidth(6); //3
        // point
        _sinI = 0.80f * UTrig.isin(90 - (int) mSelWptBrg + 9); //6
        _cosI = 0.80f * UTrig.icos(90 - (int) mSelWptBrg + 9);
        mLine.SetVerts(
                roseRadius * _cosI, roseRadius * _sinI, z,
                roseRadius * cosI, roseRadius * sinI, z
        );
        mLine.draw(matrix);
        _sinI = 0.80f * UTrig.isin(90 - (int) mSelWptBrg - 8);  //5
        _cosI = 0.80f * UTrig.icos(90 - (int) mSelWptBrg - 8);
        mLine.SetVerts(
                roseRadius * _cosI, roseRadius * _sinI, z,
                roseRadius * cosI, roseRadius * sinI, z
        );
        mLine.draw(matrix);
    }


    protected void renderBearingTxt(float[] matrix)
    {
        float roseRadius = roseScale * pixM2;
        float scale = 1.6f;  // not sure why this is > 1.0 Does not really make sense
        // it is somehow related to which matrix it is drawn on

        //
        // Bearing to Selected Waypoint
        //
        //glText.begin(0.99f*theta, 0.5f*theta, 0.99f*theta, 1.0f, matrix); // purple'ish
        glText.begin(theta * foreShade, tapeShade * theta, theta * foreShade, 1.0f, matrix); // purple'ish
        glText.setScale(scale);
        glText.drawC(mWptSelName, 0, 0.12f * roseRadius, 0);
        glText.end();

        //
        // Bearing to Automatic Waypoint
        //
        glText.begin(0.7f * theta, 0.7f * theta, 0, 1.0f, matrix); // yellow
        glText.setScale(scale);
        glText.drawC(mAutoWpt, 0, -0.12f * roseRadius, 0);
        glText.end();
    }

    //-------------------------------------------------------------------------
    // MFD specific members
    //

    public float mMapZoom = 20; // Zoom multiplier for map. 1 (max out) to 200 (max in)

    //-------------------------------------------------------------------------
    // Render the Direct To bearing line
    //
    protected void renderDctTrack(float[] matrix)
    {
        float z, x1, y1; //sinI, cosI, _sinI, _cosI;
        z = zfloat;

        //
        // Direct Track to Selected Waypoint
        //
        mLine.SetWidth(20); //8
        //mLine.SetColor(0.5f, 0.250f, 0.5f, 0.125f); // purple'ish
        mLine.SetColor(0.45f, 0.45f, 0.10f, 0.125f); // yellow'ish

        x1 = mMapZoom * (mSelWptDme * UTrig.icos(90 - (int) mSelWptRlb));
        y1 = mMapZoom * (mSelWptDme * UTrig.isin(90 - (int) mSelWptRlb));
        mLine.SetVerts(
                0, 0, z,
                x1, y1, z
        );
        mLine.draw(matrix);
        // Skunk stripe
        mLine.SetWidth(4); //8
        mLine.SetColor(0.0f, 0.0f, 0.0f, 1); // black
        mLine.draw(matrix);

        //
        // Direct Track to Automatic Waypoint
        //
        /* Not sure I like this feature ...
        mLine.SetWidth(2); //8
        mLine.SetColor(0.7f, 0.7f, 0, 1.0f); // yellow

        x1 = mMapZoom * (mAutoWptDme * UTrig.icos(90-(int)mAutoWptRlb));
        y1 = mMapZoom * (mAutoWptDme * UTrig.isin(90-(int)mAutoWptRlb));
        mLine.SetVerts(
                0, 0, z,
                x1, y1, z
        );
        mLine.draw(matrix);
        */

    }

    //-------------------------------------------------------------------------
    // Render a little airplane symbol
    //
    protected void renderACSymbol(float[] matrix)
    {
        float z;
        z = zfloat;

        float wx = 0.10f * pixM2;
        float wy = 0.00f * pixM2;
        float wa = 0.025f * pixM2;
        float fa = 0.075f * pixM2;
        float ft = -0.10f * pixM2;
        int wid = 12;
        mLine.SetColor(foreShade, foreShade, foreShade, 1);

        // Use a loop to draw a "halo" around the plane as well
        for (int i = 0; i < 2; i++) {
            // Wings
            mLine.SetWidth(wid);

            mLine.SetVerts(
                    -wx, wy, z,
                    wx, wy, z
            );
            mLine.draw(matrix);
            // L
            mLine.SetVerts(
                    -wx, 0, z,
                    0, wa, z
            );
            mLine.draw(matrix);
            // R
            mLine.SetVerts(
                    wx, 0, z,
                    0, wa, z
            );
            mLine.draw(matrix);

            // Fuselage
            mLine.SetVerts(
                    0, fa, z,
                    0, ft, z
            );
            mLine.draw(matrix);

            // Tail
            mLine.SetVerts(
                    -wx / 2, ft, z,
                    wx / 2, ft, z
            );
            mLine.draw(matrix);

            wx = 0.09f * pixM2;
            wy = 0.00f * pixM2;
            wa = 0.020f * pixM2;
            fa = 0.065f * pixM2;
            ft = -0.10f * pixM2;
            wid = 6;
            mLine.SetColor(backShade, backShade, backShade, 1);
        }
    }

    //-------------------------------------------------------------------------
    // Render map scale ruler
    //
    protected void renderMapScale(float[] matrix)
    {
        float z;
        z = zfloat;

        float len = 20;//20;
        float x1 = mMapZoom * len;// * (5 * UTrig.icos(90-(int)90));

        while (x1 > pixW2 / 2) {
            if (len > 5) len = len - 5;
            else len = 1;

            x1 = mMapZoom * len;// * (5 * UTrig.icos(90-(int)90));
        }

        // Scale line
        mLine.SetWidth(1);
        mLine.SetColor(foreShade, foreShade, foreShade, 1);
        mLine.SetVerts(
                -0.95f * pixW2 + 0, -0.95f * pixH2, z,
                -0.95f * pixW2 + x1, -0.95f * pixH2, z
        );
        mLine.draw(matrix);
        mLine.SetVerts(
                -0.95f * pixW2 + 0, -0.95f * pixH2, z,
                -0.95f * pixW2 + 0, -0.92f * pixH2, z
        );
        mLine.draw(matrix);
        mLine.SetVerts(
                -0.95f * pixW2 + x1, -0.95f * pixH2, z,
                -0.95f * pixW2 + x1, -0.92f * pixH2, z
        );
        mLine.draw(matrix);

        String t = String.format("%3.0f nm", len);
        glText.begin(foreShade, foreShade, foreShade, 1, matrix); // White
        glText.setScale(1.5f);
        glText.draw(t, -0.90f * pixW2, -0.95f * pixH2);            // Draw  String
        glText.end();

        // leader line
        mLine.SetVerts(
                0, 0, z,
                0, x1, z
        );
        mLine.draw(matrix);
        mLine.SetVerts(
                -0.025f * pixM2, x1, z,
                +0.025f * pixM2, x1, z
        );
        mLine.draw(matrix);

    }

    //-------------------------------------------------------------------------
    // Map Zooming
    //
    public void setMapZoom(float zoom)
    {
        mMapZoom = zoom;
    }

    public void zoomIn()
    {
        if (mMapZoom < 5) mMapZoom += 1;
        else if (mMapZoom < 120) mMapZoom += 5;
    }

    public void zoomOut()
    {
        if (mMapZoom > 5) mMapZoom -= 5;
        else if (mMapZoom > 2) mMapZoom -= 1;
    }


}
//-------------
// END OF CLASS
//-------------


//-----------------------------------------------------------------------------
/*
Some leftover code fragments from the original c code.
This may still be useful one day

void GLPFD::renderDIMarkers()
{
	GLint i, j;
	GLfloat innerTic, midTic, outerTic, z, pixPerUnit, iPix;

	glLineWidth( 2 );
  pixPerUnit = pixH2/DIInView;
  z = zfloat;

  font = QFont("Fixed", 10, QFont::Normal);
	QFontMetrics fm = fontMetrics();

  innerTic = 0.80 * pixH2;	// inner & outer are relative to the vertical scale line
  outerTic = 0.90 * pixH2;
  midTic = 0.87 * pixH2;

	// The numbers & tics for the tape
	qglColor( QColor( "white" ) );

  for (i = 0; i < 360; i=i+30) {
    iPix = (float) i * pixPerUnit;
    t = QString( "%1" ).arg( i );

    glBegin(GL_LINE_STRIP);
      glVertex3f( iPix, innerTic, z);
      glVertex3f( iPix, outerTic, z);
    glEnd();
    QGLWidget::renderText (iPix - fm.width(t)/2 , outerTic  + fm.ascent() / 2 , z, t, font, 2000 );

    //for (j = i + 20; j < i+90; j=j+20) {
    for (j = i + 10; j < i+90; j=j+10) {
      iPix = (float) j * pixPerUnit;
			glBegin(GL_LINE_STRIP);
        glVertex3f( iPix, innerTic,z);
        glVertex3f( iPix, midTic,z);
			glEnd();
    }


    iPix = (float) (360-i) * pixPerUnit;
    t = QString( "%1" ).arg( i );
    glBegin(GL_LINE_STRIP);
      glVertex3f( -iPix, innerTic, z);
      glVertex3f( -iPix, outerTic, z);
    glEnd();
    QGLWidget::renderText (-iPix - fm.width(t)/2 , outerTic  + fm.ascent() / 2 , z, t, font, 2000 );

    //for (j = i + 20; j < i+90; j=j+20) {
    for (j = i + 10; j < i+90; j=j+10) {
      iPix = (float) j * pixPerUnit;
      glBegin(GL_LINE_STRIP);
        glVertex3f( -iPix, innerTic,z);
        glVertex3f( -iPix, midTic,z);
      glEnd();
    }
	}

  // The horizontal scale bar
	glBegin(GL_LINE_STRIP);
    glVertex3f(-180 * pixPerUnit , innerTic, z);
    glVertex3f(180 * pixPerUnit, innerTic, z);
  glEnd();
}


//
//	Set the indicator
//

void GLPFD::setHeading(int degrees)
{
  DIValue = degrees;
  while (DIValue < 0) DIValue += 360;
  DITranslation = DIValue / DIInView  * pixH2;
	updateGL();
}

void GLPFD::setSlip(int slip)
{
  SlipValue = slip;
  updateGL();
}


void GLPFD::setGForce(float gforce)
{
  GForceValue = gforce;
  updateGL();
}

void GLPFD::setGS(float gs)
{
  GSValue = gs;
  updateGL();
}

void GLPFD::setROT(float rot)
{
  ROTValue = rot;
  updateGL();
}

//
//	Set the bearing (to steer)
//

void GLPFD::setBearing(int degrees)
{
  baroPressure = degrees;
	updateGL();
}

*/



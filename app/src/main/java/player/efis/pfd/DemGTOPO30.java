
package player.efis.pfd;

// Standard imports
import android.content.Context;
import android.widget.Toast;

import java.io.*;


/*

a
 +------------------------------------+
 |            b                       |     a = demTopLeftLat,  demTopLeftLon
 |             +---------+            |     b = x0, y0
 |             |         |            |     c = lat0, lon0
 |             |  c +    |            |
 |             |         |            |
 |             +---------+            |
 |                                    |
 |             |< BUFX  >|            |
 |                                    |
 +------------------------------------+

 Note: BUFX should fit completely in the DEM tile
       Consider this when choosing size
*/


public class DemGTOPO30
{
    Context context;

    final static float DEM_HORIZON = 30; // nm

    final  int maxcol = 4800;
    final  int maxrow = 6000;

    static final int BUFX = 600;  //600;  //800 = 400nm square ie at least  200nm in each direction
    static final int BUFY = BUFX; // 400;
    static short buff[][] = new short[BUFX][BUFY];

    static float demTopLeftLat =  -10;
    static float demTopLeftLon = +100;

    static private int x0;   // center of the BUFX tile ??
    static private int y0;   // center of the BUFX tile

    public float lat0;
    public float lon0;


    public static boolean demDataValid = false;

    //-------------------------------------------------------------------------
    // Construct a new default loader with no flags set
    //
    public DemGTOPO30()
    {
    }

    public DemGTOPO30(Context context)
    {
        this.context = context;
    }


    public static short getElev(float lat, float lon)
    {
        // Do we return bad data and let the program continue
        // or do we wait for valid data ?
        //while (!demDataValid)
        //  ; // do nothing and wait for valid data

        // check if buff is valid ?

        // *60 -> min * 2 -> 30 arcsec = 1/2 a min
        int y = (int) (Math.abs(lat - demTopLeftLat) * 60 * 2) - y0;
        int x = (int) (Math.abs(lon - demTopLeftLon) * 60 * 2) - x0;

        if ((x < 0) || (y < 0) || (x >= BUFX) || ( y >= BUFY))
            //return 8888; //-8888;
            return -9999; //-8888;
        else return buff[x][y];

    }

    public void setBufferCenter(float lat, float lon)
    {
        lat0 = lat;
        lon0 = lon;
        x0 = (int) (Math.abs(lon0 - demTopLeftLon) * 60 *2) -  BUFX/2;
        y0 = (int) (Math.abs(lat0 - demTopLeftLat) * 60 *2) -  BUFY/2;
    }


    //-------------------------------------------------------------------------
    // use the lat lon to determine with region file is active
    //
    public String setDEMRegionFileName(float lat, float lon)
    {
        setBufferCenter(lat, lon);  // set the buffer tile as well

        demTopLeftLat =   90  - (int) (90 - lat) / 50 * 50;
        demTopLeftLon =  -180 + (int) (lon + 180) / 40 * 40;

        String s = String.format("%c%03d%c%02d", demTopLeftLon<0?'W':'E', (int)Math.abs(demTopLeftLon),
                                                 demTopLeftLat<0?'S':'N', (int)Math.abs(demTopLeftLat));
        //DemFilename = s;
        return s;

    }


    private void fillBuffer(short c)
    {
        for (int y = 0; y < BUFY; y++) {
            for (int x = 0; x < BUFX; x++) {
                buff[x][y] = c;  // fill in the buffer
            }
        }
    }

    private boolean isValidLocation(float lat, float lon)
    {
        if (lat + lon == 0) return false;
        if (Math.abs(lat) > 90)  return false;
        if (Math.abs(lon) > 180)  return false;

        return true;
    }


    public void loadDemBuffer(float lat, float lon)
    {
        if (isValidLocation(lat, lon)) {
            Toast.makeText(context, "DEM terrain loading", Toast.LENGTH_SHORT).show();
            demDataValid = false;
            String DemFilename = setDEMRegionFileName(lat, lon);
            setBufferCenter(lat, lon);
            fillBuffer((short) 0);

            try {
                InputStream inp = context.getAssets().open("terrain/" + DemFilename + ".DEM");
                DataInputStream demFile = new DataInputStream(inp);

                final int NUM_BYTES_IN_SHORT = 2;
                short c;
                int x, y;
                int x1, y1, x2, y2;


                // buffer wholly in tile
                x1 = x0;
                x2 = x0 + BUFX;
                y1 = y0;
                y2 = y0 + BUFY;

                // Handle borders
                // Buffer west and north
                if (x0 < 0) x1 = 0;
                if (y0 < 0) y1 = 0;

                // Buffer east and south
                if (x0 + BUFX > maxcol) x2 = maxcol;
                if (y0 + BUFY > maxrow) y2 = maxrow;


                demFile.skipBytes(NUM_BYTES_IN_SHORT * (maxcol * y1));
                demFile.skipBytes(NUM_BYTES_IN_SHORT * (x1));
                for (y = y1; y < y2; y++) {
                    for (x = x1; x < x2; x++) {
                        c = demFile.readShort();
                        // deliberately avoid 0
                        if (c > 0) {
                            buff[x - x1][y - y1] = c;  // fill in the buffer
                        }
                    }
                    demFile.skipBytes(NUM_BYTES_IN_SHORT * (maxcol - x2));
                    demFile.skipBytes(NUM_BYTES_IN_SHORT * (x1));
                }
                demFile.close();
                demDataValid = true;
            }
            catch (IOException e) {
                Toast.makeText(context, "DEM file error: " + DemFilename, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }


}

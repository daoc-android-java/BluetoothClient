package daoc.btclient;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.UUID;

import daoc.msg.SerialBmp;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

/*
 * Atención: este código casi no tiene control de errores
 */
public class MainActivity extends Activity implements Camera.PreviewCallback {
	private static final UUID SRV_UUID = UUID.fromString("00000000-1111-2222-3333-444444444444");
	private final String HTC_ADDR = "38:E7:D8:A1:58:79";
	private final String LG_ADDR = "74:A7:22:5D:35:61";
	private final static int SERIAL_JPEG_QUALITY = 10;
	private final static int INNER_JPEG_QUALITY = 100;
	
	private BluetoothAdapter btAdapter;
	private BluetoothDevice srvDevice;
	private BluetoothSocket socket;
	
	private Camera cam;
	private ImageView preview;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		preview = (ImageView) findViewById(R.id.iv1);
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
        for(BluetoothDevice device : btAdapter.getBondedDevices()) {
            if (device.getAddress().equals(LG_ADDR) || device.getAddress().equals(HTC_ADDR)) {
            	srvDevice = device;
            	break;
            }
        }
	}

	@Override
	protected void onStart() {
		super.onStart();
        try {
            cam = Camera.open();    
            List<Size> prevSizes = cam.getParameters().getSupportedPreviewSizes();
            Size size = prevSizes.get(prevSizes.size() - 1);//el �ltimo es el m�s peque�o
            Parameters param = cam.getParameters();
            param.setPreviewSize(size.width, size.height);
            cam.setParameters(param);
    		cam.setPreviewCallback(this);
            cam.startPreview();
            if(srvDevice == null) {
            	Toast.makeText(this, "ERROR: NO puede conectarse", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Oprima el botón para conectarse", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "NO se puede activar la cámara", Toast.LENGTH_LONG).show();
        }        
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
    	desconectar(null);
        if (cam != null) {
        	cam.stopPreview();
        	cam.setPreviewCallback(null);
        	cam.release();
        	cam = null;
        }  
	}
   
	public void conectar(View v) {
		try {
	        desconectar(v);
			socket = srvDevice.createRfcommSocketToServiceRecord(SRV_UUID);
			socket.connect();
			Toast.makeText(this, "Conectado al servidor", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "ERROR: NO puede conectarse", Toast.LENGTH_LONG).show();
		}
	}    
    
	public void desconectar(View v) {
		try {
			if(socket != null) {
				socket.close();
				socket = null;
			}
			Toast.makeText(this, "Desconectado del servidor", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "ERROR: al desconectarse", Toast.LENGTH_LONG).show();
		}
	}    

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
	    Parameters param = camera.getParameters();
	    int width = param.getPreviewSize().width;
	    int height = param.getPreviewSize().height;
	    
    	Bitmap bmp = yuv2bmp(data, width, height);
    	bmp = rotarBmp(bmp, 90);
    	
//    	// Para pruebas solamente
//    	SerialBmp sb = new SerialBmp(width, height, bmp2barr(bmp));
//    	bmp.recycle();bmp = null;
//    	bmp = BitmapFactory.decodeByteArray(sb.barr, 0, sb.barr.length);
    	
    	preview.setImageBitmap( bmp );
    	
    	if(socket != null) {
        	try {
        		SerialBmp sbmp = new SerialBmp(width, height, bmp2barr(bmp));
            	ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            	out.writeObject(sbmp);
            	out.flush();
            	//Log.d("PREVIEW", "Enviado: " + sbmp.barr.length);
			} catch (Exception e) {
				e.printStackTrace();
				desconectar(null);
				//Log.d("PREVIEW", "NO se envía ");
			}
    	}
	}
	
    private Bitmap yuv2bmp(byte[] data, int width, int height) {	    
	    //Transforma: ImageFormat.NV21 o ImageFormat.YUY2
	    //(camera.getParameters().getPreviewFormat())
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), INNER_JPEG_QUALITY, baos);
        byte[] jdata = baos.toByteArray();
//        BitmapFactory.Options bmpFactOpt = new BitmapFactory.Options();
//        bmpFactOpt.inPreferredConfig = Bitmap.Config.RGB_565;
//        return BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bmpFactOpt);
        return BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
    }
    
    private byte[] bmp2barr(Bitmap bmp) {
		ByteArrayOutputStream barrout = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.JPEG, SERIAL_JPEG_QUALITY, barrout);
        return barrout.toByteArray();
    }
        
	private Bitmap rotarBmp(Bitmap bmp, float angulo) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angulo);
        return  Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);		
	}       

	private Bitmap scaleBmp(Bitmap bmp, int width, int height) {
        return  Bitmap.createScaledBitmap(bmp, width, height, true);	
	}
	
}

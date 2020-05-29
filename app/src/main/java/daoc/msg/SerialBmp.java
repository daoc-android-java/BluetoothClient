package daoc.msg;

import java.io.Serializable;

public class SerialBmp implements Serializable {
	private static final long serialVersionUID = -2700475582718533764L;

	public int width;
	public int height;
	public byte[] barr;
	
	public SerialBmp(int width, int height, byte[] barr) {
		this.width = width;
		this.height = height;
		this.barr = barr;
	}
}

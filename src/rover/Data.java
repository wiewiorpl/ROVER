package rover;

import java.io.Serializable;
import java.util.List;


public class Data implements Serializable {

	private static final long serialVersionUID = 5844257548890635939L;

	public String c; // Command
	public List <String> m; //Measurements

	public String getC() {return c;}
	public void setC(String c) {this.c = c;}
	public List<String> getM() {return m;}
	public void setM(List<String> m) {this.m = m;}
	
}
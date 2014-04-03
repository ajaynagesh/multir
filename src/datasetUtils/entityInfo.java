package datasetUtils;

public class entityInfo {
	protected String guid;
	protected String name;
	protected String [] types;
	
	public entityInfo(String guid, String name, String[] types) {
		// TODO Auto-generated constructor stub
		this.guid = guid;
		this.name = name;
		this.types = types;
	}
	
	public String toString(){
		String toret = name;
//		String toret = name + ":(";
//		
//		for(String t : types){
//			toret += t + ",";
//		}
//		toret += ")";
		
		return toret;
	}
}

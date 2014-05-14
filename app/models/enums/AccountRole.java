package models.enums;

import java.util.LinkedHashMap;
import java.util.Map;

public enum AccountRole {
	
	STUDENT("Student"),
	TUTOR("Dozent"),
	ADMIN("Administrator");

	private String displayName;
	
	private AccountRole(String displayName)  {
		this.displayName = displayName;
	}
	
	public String getDisplayName() {
		return this.displayName;
	}
	
	public static AccountRole findByOrdinal(int ordinal){
        for (AccountRole AccountRoleType : AccountRole.values()) {
            if(AccountRoleType.ordinal() == ordinal){
            	return AccountRoleType;
            }
        }
        return null;
	}
	
    public static Map<String, String> selectOptions(){
        LinkedHashMap<String, String> vals = new LinkedHashMap<String, String>();
        for (AccountRole AccountRoleType : AccountRole.values()) {
            vals.put(String.valueOf(AccountRoleType.ordinal()), AccountRoleType.getDisplayName());
        }
        return vals;
    }
}
